package com.brandextractor.infrastructure.web.dto.evidence;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "OCR evidence extracted from an image by Gemini multimodal — " +
                      "structured text blocks with normalised bounding boxes.")
public record OcrEvidenceDto(

        @Schema(description = "Unique evidence ID, e.g. `ocr-1`", example = "ocr-1")
        String id,

        @Schema(description = "Always `OCR` for this evidence type", example = "OCR")
        String sourceType,

        @Schema(description = "Reference to the source image (filename or URL)",
                example = "acme-summer-poster.png")
        String sourceReference,

        @Schema(description = "Text blocks detected by OCR, each with text content, " +
                              "bounding box, and confidence score")
        List<TextBlockDto> blocks) implements EvidenceDto {}
