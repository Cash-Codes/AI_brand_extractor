package com.brandextractor.infrastructure.vision;

import com.brandextractor.domain.evidence.VisualEvidence;
import com.brandextractor.domain.ports.VisualAnalysisPort;
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

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Vertex AI Gemini implementation of {@link VisualAnalysisPort}.
 *
 * <p>Sends the image to Gemini multimodal and asks for brand-relevant visual signals:
 * descriptive labels (industry, aesthetic, mood, quality tier) and a dominant visual theme.
 *
 * <p>Failures are non-fatal: any exception is propagated to the caller, which wraps the
 * call in a try/catch so the extraction pipeline continues without visual evidence.
 */
@Component
@ConditionalOnProperty(name = "vertexai.enabled", havingValue = "true")
public class GeminiVisualAnalysisAdapter implements VisualAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisualAnalysisAdapter.class);

    private static final int MAX_LABELS = 15;

    private static final String VISUAL_PROMPT =
            "You are a brand analyst examining an image. Analyse it for brand identity signals:\n" +
            "- labels: 5–15 short descriptive terms covering industry/category, visual style, " +
            "  mood/tone, quality tier, typography style, and key visual elements " +
            "  (e.g. \"luxury\", \"minimalist\", \"bold typography\", \"dark palette\", \"tech\", " +
            "  \"handcrafted\", \"editorial\", \"vibrant\").\n" +
            "- dominantTheme: one sentence describing the overall visual brand identity.\n" +
            "- confidence: your confidence in the analysis [0.0–1.0].\n" +
            "Return ONLY the JSON object — no markdown fences.";

    @Lazy
    @Autowired
    private VertexAI vertexAI;

    private final VertexAiExtractionProperties props;
    private final ObjectMapper                 objectMapper;

    public GeminiVisualAnalysisAdapter(VertexAiExtractionProperties props,
                                       ObjectMapper objectMapper) {
        this.props        = props;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // VisualAnalysisPort
    // -------------------------------------------------------------------------

    @Override
    public VisualEvidence analyse(byte[] imageBytes, String mimeType) {
        try {
            GenerativeModel model = new GenerativeModel(props.getModelId(), vertexAI)
                    .withGenerationConfig(buildVisualConfig());

            var content = ContentMaker.fromMultiModalData(
                    PartMaker.fromMimeTypeAndData(mimeType, imageBytes),
                    VISUAL_PROMPT);

            GenerateContentResponse response = model.generateContent(content);
            String rawJson = ResponseHandler.getText(response);
            log.debug("Visual analysis raw response: {}", rawJson);

            return parseResponse(rawJson, mimeType);

        } catch (IOException e) {
            log.warn("Visual analysis Gemini call failed: {}", e.getMessage());
            throw new RuntimeException("Visual analysis failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    private VisualEvidence parseResponse(String json, String mimeType) {
        try {
            JsonNode root = objectMapper.readTree(json);

            List<String> labels = new ArrayList<>();
            JsonNode labelsNode = root.path("labels");
            if (labelsNode.isArray()) {
                for (JsonNode l : labelsNode) {
                    String label = l.asText("").strip();
                    if (!label.isBlank()) labels.add(label);
                    if (labels.size() >= MAX_LABELS) break;
                }
            }

            String dominantTheme = root.path("dominantTheme").asText(null);
            if (dominantTheme != null && dominantTheme.isBlank()) dominantTheme = null;

            double confidence = root.path("confidence").asDouble(0.8);
            confidence = Math.max(0.0, Math.min(1.0, confidence));

            log.debug("Visual analysis: {} labels, theme=\"{}\"", labels.size(), dominantTheme);

            return new VisualEvidence(
                    UUID.randomUUID().toString(),
                    "IMAGE_VISUAL",
                    mimeType,
                    List.copyOf(labels),
                    dominantTheme,
                    confidence,
                    Instant.now());

        } catch (Exception e) {
            log.warn("Failed to parse visual analysis response: {}", e.getMessage());
            return new VisualEvidence(
                    UUID.randomUUID().toString(),
                    "IMAGE_VISUAL",
                    mimeType,
                    List.of(),
                    null,
                    0.0,
                    Instant.now());
        }
    }

    // -------------------------------------------------------------------------
    // Generation config
    // -------------------------------------------------------------------------

    private static GenerationConfig buildVisualConfig() {
        Schema responseSchema = Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("labels", Schema.newBuilder()
                        .setType(Type.ARRAY)
                        .setDescription("5–15 brand-relevant descriptive labels")
                        .setItems(Schema.newBuilder().setType(Type.STRING).build())
                        .build())
                .putProperties("dominantTheme", Schema.newBuilder()
                        .setType(Type.STRING)
                        .setDescription("One-sentence summary of the visual brand identity")
                        .build())
                .putProperties("confidence", Schema.newBuilder()
                        .setType(Type.NUMBER)
                        .setDescription("Confidence in the analysis [0.0–1.0]")
                        .build())
                .addRequired("labels")
                .addRequired("dominantTheme")
                .addRequired("confidence")
                .build();

        return GenerationConfig.newBuilder()
                .setResponseMimeType("application/json")
                .setResponseSchema(responseSchema)
                .setTemperature(0.2f)
                .setMaxOutputTokens(512)
                .build();
    }
}
