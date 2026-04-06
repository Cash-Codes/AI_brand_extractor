package com.brandextractor.domain.model;

import java.util.List;

public record BrandProfile(
        String brandName,
        String tagline,
        String summary,
        List<String> toneKeywords) {}
