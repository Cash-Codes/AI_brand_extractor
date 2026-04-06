package com.brandextractor.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UrlExtractionRequest(

        @NotBlank(message = "url must not be blank")
        @Pattern(regexp = "^https?://.*", message = "url must start with http:// or https://")
        String url) {}
