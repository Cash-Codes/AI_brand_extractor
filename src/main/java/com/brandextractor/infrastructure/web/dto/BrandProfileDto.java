package com.brandextractor.infrastructure.web.dto;

import java.util.List;

public record BrandProfileDto(
        String brandName,
        String tagline,
        String summary,
        List<String> toneKeywords) {}
