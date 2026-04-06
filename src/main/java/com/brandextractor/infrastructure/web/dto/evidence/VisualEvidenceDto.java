package com.brandextractor.infrastructure.web.dto.evidence;

import java.util.List;

public record VisualEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        List<String> detectedLabels,
        String dominantTheme) implements EvidenceDto {}
