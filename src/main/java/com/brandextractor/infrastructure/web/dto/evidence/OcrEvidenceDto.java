package com.brandextractor.infrastructure.web.dto.evidence;

import java.util.List;

public record OcrEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        List<String> textBlocks) implements EvidenceDto {}
