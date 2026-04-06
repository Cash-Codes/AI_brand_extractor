package com.brandextractor.domain.candidate;

import java.util.List;

public record BrandNameCandidate(
        String value,
        double score,
        String rationale,
        List<String> evidenceRefs) {}
