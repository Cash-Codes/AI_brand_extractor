package com.brandextractor.infrastructure.web.dto;

import java.util.List;

public record BrandProfileDto(
        String brandName,
        double brandNameConfidence,
        String tagline,
        double taglineConfidence,
        String summary,
        double summaryConfidence,
        List<String> toneKeywords) {}
