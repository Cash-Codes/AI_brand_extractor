package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "AI-extracted brand identity, with per-field confidence scores")
public record BrandProfileDto(

        @Schema(description = "Primary brand name. Max 100 characters.",
                example = "Acme Studio")
        String brandName,

        @Schema(description = "AI confidence in the brand name, in the range [0.0, 1.0]",
                example = "0.94")
        double brandNameConfidence,

        @Schema(description = "Brand tagline or strapline. Max 200 characters. Null when not found.",
                example = "Crafting brands that endure")
        String tagline,

        @Schema(description = "AI confidence in the tagline, in the range [0.0, 1.0]",
                example = "0.87")
        double taglineConfidence,

        @Schema(description = "Short brand summary. Max 2000 characters. Null when not found.",
                example = "Acme Studio is a full-service branding agency specialising in visual identity.")
        String summary,

        @Schema(description = "AI confidence in the summary, in the range [0.0, 1.0]",
                example = "0.91")
        double summaryConfidence,

        @Schema(description = "Up to five tone/style keywords describing the brand voice",
                example = "[\"bold\", \"minimal\", \"modern\"]")
        List<String> toneKeywords) {}
