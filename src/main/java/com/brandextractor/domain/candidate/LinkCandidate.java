package com.brandextractor.domain.candidate;

import java.util.List;

public record LinkCandidate(
        String href,
        String platform,
        double score,
        List<String> evidenceRefs) {}
