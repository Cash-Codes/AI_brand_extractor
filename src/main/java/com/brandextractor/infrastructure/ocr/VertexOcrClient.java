package com.brandextractor.infrastructure.ocr;

import com.brandextractor.infrastructure.ai.vertex.VertexAiExtractionProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Vertex AI Gemini implementation of {@link OcrClient}.
 *
 * <p>Sends the image to Gemini as a multimodal prompt and requests a structured JSON
 * response listing all visible text regions with pixel bounding boxes and confidence.
 *
 * <p>OCR failures are non-fatal: any {@link IOException} or parse error is logged at
 * WARN level and an empty {@link OcrClientResponse} is returned so the extraction
 * pipeline can continue with other evidence sources.
 */
@Component
@ConditionalOnProperty(name = "vertexai.enabled", havingValue = "true")
public class VertexOcrClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(VertexOcrClient.class);

    private static final int MAX_OCR_TOKENS = 4096;

    private static final String OCR_PROMPT =
            "Extract every piece of text visible in this image. " +
            "For each distinct text region return its exact text content, " +
            "its bounding box in pixels (x = left edge, y = top edge, width, height), " +
            "and your confidence in the transcription. " +
            "imageWidth and imageHeight must be the actual pixel dimensions of the image. " +
            "Return ONLY the JSON object — no markdown fences.";

    @Lazy
    @Autowired
    private VertexAI vertexAI;

    private final VertexAiExtractionProperties props;
    private final ObjectMapper                 objectMapper;

    public VertexOcrClient(VertexAiExtractionProperties props, ObjectMapper objectMapper) {
        this.props        = props;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // OcrClient
    // -------------------------------------------------------------------------

    @Override
    public OcrClientResponse extract(byte[] imageBytes, String mimeType) {
        try {
            GenerativeModel model = new GenerativeModel(props.getModelId(), vertexAI)
                    .withGenerationConfig(buildOcrGenerationConfig());

            var content = ContentMaker.fromMultiModalData(
                    PartMaker.fromMimeTypeAndData(mimeType, imageBytes),
                    OCR_PROMPT);

            GenerateContentResponse response = model.generateContent(content);
            String rawJson = ResponseHandler.getText(response);
            log.debug("OCR raw response ({} chars): {}", rawJson.length(), rawJson);

            return parseOcrResponse(rawJson, imageBytes, mimeType);

        } catch (IOException e) {
            log.warn("OCR Vertex AI call failed — continuing without OCR: {}", e.getMessage());
            return OcrClientResponse.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    private OcrClientResponse parseOcrResponse(String json, byte[] imageBytes, String mimeType) {
        try {
            JsonNode root = objectMapper.readTree(json);

            // Prefer Gemini-reported dimensions; fall back to header parse
            int imageWidth  = root.path("imageWidth").asInt(0);
            int imageHeight = root.path("imageHeight").asInt(0);
            if (imageWidth <= 0 || imageHeight <= 0) {
                int[] dims = parseImageDimensions(imageBytes, mimeType);
                imageWidth  = dims[0];
                imageHeight = dims[1];
            }

            List<DetectedText> regions = new ArrayList<>();
            JsonNode regionsNode = root.path("regions");
            if (regionsNode.isArray()) {
                for (JsonNode r : regionsNode) {
                    String text = r.path("text").asText("").strip();
                    if (text.isBlank()) continue;

                    double confidence = r.path("confidence").asDouble(1.0);

                    int x = r.path("x").asInt(0);
                    int y = r.path("y").asInt(0);
                    int w = r.path("width").asInt(0);
                    int h = r.path("height").asInt(0);
                    // Only populate the bounding box when Gemini returned actual coordinates
                    PixelBoundingBox bbox = (x > 0 || y > 0 || w > 0 || h > 0)
                            ? new PixelBoundingBox(x, y, w, h)
                            : null;

                    regions.add(new DetectedText(text, bbox, confidence));
                }
            }

            log.debug("OCR extracted {} text region(s) from {}×{} image",
                    regions.size(), imageWidth, imageHeight);
            return new OcrClientResponse(List.copyOf(regions), imageWidth, imageHeight);

        } catch (Exception e) {
            log.warn("Failed to parse OCR response — continuing without OCR: {}", e.getMessage());
            return OcrClientResponse.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Generation config
    // -------------------------------------------------------------------------

    private static GenerationConfig buildOcrGenerationConfig() {
        Schema regionSchema = Schema.newBuilder()
                .setType(Type.OBJECT)
                .setDescription("A single text region in the image")
                .putProperties("text",       Schema.newBuilder().setType(Type.STRING)
                        .setDescription("The exact text content").build())
                .putProperties("x",          Schema.newBuilder().setType(Type.INTEGER)
                        .setDescription("Left edge of the bounding box in pixels").build())
                .putProperties("y",          Schema.newBuilder().setType(Type.INTEGER)
                        .setDescription("Top edge of the bounding box in pixels").build())
                .putProperties("width",      Schema.newBuilder().setType(Type.INTEGER)
                        .setDescription("Width of the bounding box in pixels").build())
                .putProperties("height",     Schema.newBuilder().setType(Type.INTEGER)
                        .setDescription("Height of the bounding box in pixels").build())
                .putProperties("confidence", Schema.newBuilder().setType(Type.NUMBER)
                        .setDescription("Transcription confidence in [0.0, 1.0]").build())
                .addRequired("text")
                .addRequired("confidence")
                .build();

        Schema ocrSchema = Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("imageWidth",  Schema.newBuilder().setType(Type.INTEGER)
                        .setDescription("Image width in pixels").build())
                .putProperties("imageHeight", Schema.newBuilder().setType(Type.INTEGER)
                        .setDescription("Image height in pixels").build())
                .putProperties("regions",     Schema.newBuilder()
                        .setType(Type.ARRAY)
                        .setDescription("All visible text regions in reading order")
                        .setItems(regionSchema)
                        .build())
                .addRequired("imageWidth")
                .addRequired("imageHeight")
                .addRequired("regions")
                .build();

        return GenerationConfig.newBuilder()
                .setResponseMimeType("application/json")
                .setResponseSchema(ocrSchema)
                .setTemperature(0.0f)
                .setMaxOutputTokens(MAX_OCR_TOKENS)
                .build();
    }

    // -------------------------------------------------------------------------
    // Image dimension helper — avoids full BufferedImage decode for large files
    // -------------------------------------------------------------------------

    /**
     * Reads image dimensions from the raw bytes without fully decoding the image.
     *
     * <p>For PNG, the dimensions are read directly from the IHDR chunk at bytes 16–23.
     * For other formats, {@link ImageIO}'s reader metadata is used.
     *
     * @return {@code int[]{width, height}}, or {@code {0, 0}} on failure
     */
    static int[] parseImageDimensions(byte[] bytes, String mimeType) {
        // PNG IHDR: signature(8) + chunk-length(4) + "IHDR"(4) + width(4) + height(4)
        if ("image/png".equals(mimeType) && bytes.length >= 24) {
            int w = readInt32BE(bytes, 16);
            int h = readInt32BE(bytes, 20);
            if (w > 0 && h > 0) return new int[]{w, h};
        }
        // JPEG / other: use ImageIO metadata (header only, no pixel decode)
        try (ImageInputStream stream = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream, true, true);
                    return new int[]{reader.getWidth(0), reader.getHeight(0)};
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception ignored) { }
        return new int[]{0, 0};
    }

    private static int readInt32BE(byte[] b, int offset) {
        return ((b[offset]     & 0xFF) << 24)
             | ((b[offset + 1] & 0xFF) << 16)
             | ((b[offset + 2] & 0xFF) << 8)
             |  (b[offset + 3] & 0xFF);
    }
}
