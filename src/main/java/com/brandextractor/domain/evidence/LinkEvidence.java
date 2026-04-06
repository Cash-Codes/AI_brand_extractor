package com.brandextractor.domain.evidence;

public record LinkEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String href,
        String rel,
        String context) implements Evidence {}
