package com.brandextractor.domain.model;

import java.util.List;

public record AssetItem(
        String url,
        AssetRole role,
        double confidence,
        int width,
        int height,
        String mimeType,
        List<String> evidenceRefs) {}
