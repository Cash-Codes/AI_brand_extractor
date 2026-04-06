package com.brandextractor.infrastructure.ai.vertex;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vertexai")
@Data
public class VertexAiExtractionProperties {

    /**
     * Whether Vertex AI integration is active. Set to {@code true} only when
     * {@code VERTEXAI_PROJECT_ID} and application credentials are configured.
     * Defaults to {@code false} — mock responses are returned instead.
     */
    private boolean enabled = false;

    /** GCP project that hosts the Vertex AI endpoint. Required when {@code enabled=true}. */
    private String projectId = "";

    private String location = "us-central1";

    private String modelId = "gemini-2.0-flash-001";

    /** Temperature for generation — 0.0 = fully deterministic. */
    @Min(0) @Max(2)
    private float temperature = 0.0f;

    /** Maximum tokens to generate in a single response. */
    @Positive
    private int maxOutputTokens = 4096;

    /**
     * Maximum number of times to retry parsing if the model returns malformed JSON.
     * Each retry re-sends the request with a reminder to fix the schema.
     */
    @Min(0) @Max(5)
    private int maxParseRetries = 2;
}
