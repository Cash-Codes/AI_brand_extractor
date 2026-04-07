package com.brandextractor.infrastructure.web.dto.evidence;

/**
 * API projection of {@link com.brandextractor.domain.evidence.BoundingBox}.
 * Coordinates are normalised to {@code [0.0, 1.0]} relative to image dimensions.
 */
public record BoundingBoxDto(double x, double y, double width, double height) {}
