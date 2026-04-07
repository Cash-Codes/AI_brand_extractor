package com.brandextractor.infrastructure.web.dto.evidence;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Evidence collected from an uploaded image file — " +
                      "dimensions, file metadata, and grid-sampled dominant colours.")
public record FlyerEvidenceDto(

        @Schema(description = "Unique evidence ID, e.g. `f-1`", example = "f-1")
        String id,

        @Schema(description = "Always `FLYER` for this evidence type", example = "FLYER")
        String sourceType,

        @Schema(description = "Original filename or label supplied by the caller",
                example = "acme-summer-poster.png")
        String sourceReference,

        @Schema(description = "MIME type detected from magic bytes (not file extension)",
                example = "image/png")
        String mimeType,

        @Schema(description = "Image width in pixels", example = "1200")
        int width,

        @Schema(description = "Image height in pixels", example = "1800")
        int height,

        @Schema(description = "File size in bytes", example = "524288")
        long sizeBytes,

        @Schema(description = "Dominant colours sampled from a grid of the image, " +
                              "normalised to uppercase #RRGGBB",
                example = "[\"#E40043\", \"#FFFFFF\", \"#1A1A1A\"]")
        List<String> dominantColors) implements EvidenceDto {}
