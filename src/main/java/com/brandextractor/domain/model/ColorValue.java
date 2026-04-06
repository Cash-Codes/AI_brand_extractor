package com.brandextractor.domain.model;

import java.util.List;

public record ColorValue(
        String value,
        double confidence,
        List<String> evidenceRefs) {}
