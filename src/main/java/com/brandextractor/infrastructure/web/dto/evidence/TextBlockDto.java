package com.brandextractor.infrastructure.web.dto.evidence;

/**
 * API projection of {@link com.brandextractor.domain.evidence.TextBlock}.
 * {@code boundingBox} is {@code null} when spatial data was not provided by the OCR engine.
 */
public record TextBlockDto(String text, BoundingBoxDto boundingBox, double confidence) {}
