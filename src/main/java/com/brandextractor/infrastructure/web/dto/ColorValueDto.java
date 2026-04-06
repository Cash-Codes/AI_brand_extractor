package com.brandextractor.infrastructure.web.dto;

import java.util.List;

public record ColorValueDto(
        String value,
        double confidence,
        List<String> evidenceRefs) {}
