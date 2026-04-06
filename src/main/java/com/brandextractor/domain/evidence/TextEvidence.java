package com.brandextractor.domain.evidence;

public record TextEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String text,
        String textType) implements Evidence {}
