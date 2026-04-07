package com.brandextractor.infrastructure.web.dto.evidence;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Visual analysis evidence produced by Gemini multimodal — " +
                      "scene labels and dominant theme detected from the image.")
public record VisualEvidenceDto(

        @Schema(description = "Unique evidence ID, e.g. `vis-1`", example = "vis-1")
        String id,

        @Schema(description = "Always `VISUAL` for this evidence type", example = "VISUAL")
        String sourceType,

        @Schema(description = "Reference to the source image (filename or URL)",
                example = "acme-summer-poster.png")
        String sourceReference,

        @Schema(description = "Scene and object labels identified by Gemini",
                example = "[\"minimalist design\", \"typography\", \"brand logo\"]")
        List<String> detectedLabels,

        @Schema(description = "Single dominant visual theme summarising the image aesthetic",
                example = "Clean, bold typography on a white background with a navy accent")
        String dominantTheme) implements EvidenceDto {}
