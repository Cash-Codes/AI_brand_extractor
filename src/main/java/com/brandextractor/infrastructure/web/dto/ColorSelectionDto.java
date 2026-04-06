package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The three colour roles extracted for the brand")
public record ColorSelectionDto(

        @Schema(description = "Dominant brand colour used for primary UI elements and backgrounds")
        ColorValueDto primary,

        @Schema(description = "Accent colour that complements the primary. Null when not identified.")
        ColorValueDto secondary,

        @Schema(description = "Primary text/foreground colour. Null when not identified.")
        ColorValueDto text) {}
