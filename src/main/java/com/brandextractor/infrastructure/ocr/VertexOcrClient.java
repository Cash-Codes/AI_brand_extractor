package com.brandextractor.infrastructure.ocr;

import com.brandextractor.infrastructure.ai.vertex.VertexAiExtractionProperties;
import com.google.cloud.vertexai.VertexAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Vertex AI–backed {@link OcrClient} using Gemini's multimodal text extraction.
 *
 * <h2>Integration path</h2>
 * <p>When implemented, this client will:
 * <ol>
 *   <li>Encode the image as a base-64 inline part.</li>
 *   <li>Send a structured prompt to {@code gemini-2.0-flash-001} (or the configured model)
 *       requesting JSON output with text blocks and bounding-box vertices.</li>
 *   <li>Parse the response into {@link DetectedText} records with
 *       {@link PixelBoundingBox} coordinates.</li>
 *   <li>Return an {@link OcrClientResponse} including image dimensions decoded from
 *       the image bytes so the adapter can normalise coordinates.</li>
 * </ol>
 *
 * <p>Until the implementation is complete, this client returns an empty response and
 * logs at {@code INFO} level so the pipeline continues without OCR data.
 *
 * <h2>To activate</h2>
 * <p>Set {@code vertexai.project-id}, {@code vertexai.location}, and
 * {@code vertexai.model-id} in {@code application.properties} and replace the
 * placeholder body below with the real Gemini call.
 */
@Component
public class VertexOcrClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(VertexOcrClient.class);

    private final VertexAI vertexAI;
    private final VertexAiExtractionProperties props;

    public VertexOcrClient(@Lazy @Autowired VertexAI vertexAI, VertexAiExtractionProperties props) {
        this.vertexAI = vertexAI;
        this.props    = props;
    }

    @Override
    public OcrClientResponse extract(byte[] imageBytes, String mimeType) {
        // TODO: implement Gemini multimodal OCR call
        //
        // Sketch:
        //   var model = new GenerativeModel(props.getModelId(), vertexAI);
        //   var imagePart = PartMaker.fromMimeTypeAndData(mimeType, imageBytes);
        //   var textPart  = PartMaker.fromString(OCR_PROMPT);
        //   var response  = model.generateContent(ContentMaker.fromMultiModalData(imagePart, textPart));
        //   return parseResponse(response, imageBytes);
        //
        log.info("VertexOcrClient is not yet implemented — returning empty OCR response. " +
                 "Model={} project={}", props.getModelId(), props.getProjectId());
        return OcrClientResponse.empty();
    }
}
