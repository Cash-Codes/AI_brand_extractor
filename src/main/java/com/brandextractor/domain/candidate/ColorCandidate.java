package com.brandextractor.domain.candidate;

import java.util.List;

public record ColorCandidate(
        String hex,
        double score,
        String rationale,
        List<String> evidenceRefs) {}
