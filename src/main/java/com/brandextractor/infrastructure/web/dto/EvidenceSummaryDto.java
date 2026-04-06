package com.brandextractor.infrastructure.web.dto;

public record EvidenceSummaryDto(
        int textEvidenceCount,
        int imageEvidenceCount,
        int ocrBlockCount,
        boolean usedScreenshot) {}
