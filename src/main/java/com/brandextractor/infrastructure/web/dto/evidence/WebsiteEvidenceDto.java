package com.brandextractor.infrastructure.web.dto.evidence;

import java.util.List;

public record WebsiteEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String title,
        String metaDescription,
        String resolvedUrl,
        List<String> headings,
        String htmlSnippet) implements EvidenceDto {}
