package com.brandextractor.infrastructure.ai.vertex;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "vertexai")
@Data
@Validated
public class VertexAiExtractionProperties {

    @NotBlank
    private String projectId;

    @NotBlank
    private String location = "us-central1";

    @NotBlank
    private String modelId = "gemini-2.0-flash-001";

    /** Temperature for generation — 0.0 = fully deterministic. */
    @Min(0) @Max(2)
    private float temperature = 0.0f;

    /** Maximum tokens to generate in a single response. */
    @Positive
    private int maxOutputTokens = 2048;

    /**
     * Maximum number of times to retry parsing if the model returns malformed JSON.
     * Each retry re-sends the request with a reminder to fix the schema.
     */
    @Min(0) @Max(5)
    private int maxParseRetries = 2;
}
