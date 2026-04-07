package com.brandextractor.domain.evidence;

/**
 * Normalised bounding box for a detected region within an image.
 *
 * <p>All coordinates are in the range {@code [0.0, 1.0]}, relative to the image
 * width and height, so downstream consumers can scale to any resolution.
 *
 * @param x      left edge of the box (0 = left of image)
 * @param y      top edge of the box  (0 = top of image)
 * @param width  horizontal extent
 * @param height vertical extent
 */
public record BoundingBox(double x, double y, double width, double height) {}
