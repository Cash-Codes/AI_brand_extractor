package com.brandextractor.domain.evidence;

import java.time.Instant;
import java.util.List;

public record WebsiteEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String title,
        String metaDescription,
        String resolvedUrl,
        List<String> headings,
        String htmlSnippet,
        double confidence,
        Instant extractedAt) implements Evidence {}
