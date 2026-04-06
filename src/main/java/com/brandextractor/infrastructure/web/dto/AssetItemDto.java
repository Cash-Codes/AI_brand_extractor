package com.brandextractor.infrastructure.web.dto;

import java.util.List;

public record AssetItemDto(
        String url,
        String role,
        double confidence,
        int width,
        int height,
        String mimeType,
        List<String> evidenceRefs) {}
