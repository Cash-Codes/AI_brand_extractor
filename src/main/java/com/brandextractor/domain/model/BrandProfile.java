package com.brandextractor.domain.model;

import java.util.List;

public record BrandProfile(
        Confident<String> brandName,
        Confident<String> tagline,
        Confident<String> summary,
        List<String> toneKeywords) {}
