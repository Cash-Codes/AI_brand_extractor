package com.brandextractor.infrastructure.ai.vertex;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "vertexai")
@Data
@Validated
public class VertexAiProperties {

    @NotBlank
    private String projectId;

    @NotBlank
    private String location = "us-central1";

    @NotBlank
    private String modelId = "gemini-2.0-flash-001";
}
