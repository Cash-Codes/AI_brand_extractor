package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Source identifiers for the extraction input")
public record SourceDto(

        @Schema(description = "URL or filename exactly as submitted by the caller",
                example = "https://www.acmestudio.com")
        String original,

        @Schema(description = "Final URL after HTTP redirects, or null for file inputs",
                example = "https://www.acmestudio.com/")
        String resolved) {}
