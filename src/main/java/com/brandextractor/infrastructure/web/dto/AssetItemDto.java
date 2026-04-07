package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A single image asset identified from the source")
public record AssetItemDto(

        @Schema(description = "Absolute https URL of the asset",
                example = "https://www.acmestudio.com/assets/logo.png")
        String url,

        @Schema(description = "Semantic role of this asset",
                allowableValues = {"PRIMARY_LOGO", "ALTERNATE_LOGO", "HERO_IMAGE", "ICON", "OTHER"},
                example = "PRIMARY_LOGO")
        String role,

        @Schema(description = "AI confidence this is the correct asset for the given role, in [0.0, 1.0]",
                example = "0.90")
        double confidence,

        @Schema(description = "Image width in pixels, or 0 when not determined",
                example = "400")
        int width,

        @Schema(description = "Image height in pixels, or 0 when not determined",
                example = "100")
        int height,

        @Schema(description = "MIME type of the asset. Null when not determined.",
                example = "image/png")
        String mimeType,

        @Schema(description = "IDs of the evidence items this asset was sourced from",
                example = "[\"w-1\"]")
        List<String> evidenceRefs) {}
