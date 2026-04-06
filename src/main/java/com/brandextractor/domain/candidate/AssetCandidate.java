package com.brandextractor.domain.candidate;

import com.brandextractor.domain.model.AssetRole;

import java.util.List;

public record AssetCandidate(
        String url,
        AssetRole role,
        double score,
        String rationale,
        List<String> evidenceRefs,
        int width,
        int height,
        String mimeType) {}
