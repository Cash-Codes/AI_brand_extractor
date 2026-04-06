package com.brandextractor.domain.evidence;

public record ColorEvidence(
        String id,
        String sourceType,
        String sourceReference,
        String hexValue,
        double frequency) implements Evidence {}
