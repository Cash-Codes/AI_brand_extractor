package com.brandextractor.domain.candidate;

import java.util.List;

public record SummaryCandidate(
        String value,
        double score,
        String rationale,
        List<String> evidenceRefs) {}
