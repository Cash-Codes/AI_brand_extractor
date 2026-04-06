package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Counts of evidence items collected and used during extraction")
public record EvidenceSummaryDto(

        @Schema(description = "Number of text-based evidence items (e.g. website HTML signals)",
                example = "1")
        int textEvidenceCount,

        @Schema(description = "Number of image-based evidence items (flyer, screenshot, visual analysis)",
                example = "2")
        int imageEvidenceCount,

        @Schema(description = "Total number of OCR text blocks extracted from images",
                example = "14")
        int ocrBlockCount,

        @Schema(description = "Whether a screenshot was captured and used as evidence",
                example = "false")
        boolean usedScreenshot) {}
