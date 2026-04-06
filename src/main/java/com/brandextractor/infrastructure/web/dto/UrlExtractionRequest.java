package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request body for URL-based brand extraction")
public record UrlExtractionRequest(

        @Schema(
                description = "Publicly reachable URL to extract brand data from. " +
                              "Must begin with http:// or https://.",
                example = "https://www.acmestudio.com")
        @NotBlank(message = "url must not be blank")
        @Pattern(regexp = "^https?://.*", message = "url must start with http:// or https://")
        String url) {}
