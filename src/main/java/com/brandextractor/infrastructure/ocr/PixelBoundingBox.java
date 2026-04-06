package com.brandextractor.infrastructure.ocr;

/**
 * Bounding box in absolute pixel coordinates as returned by an OCR provider.
 * Converted to normalised {@link com.brandextractor.domain.evidence.BoundingBox}
 * by {@link OcrPortAdapter} before entering the domain.
 *
 * @param x      left edge in pixels
 * @param y      top edge in pixels
 * @param width  horizontal extent in pixels
 * @param height vertical extent in pixels
 */
public record PixelBoundingBox(int x, int y, int width, int height) {}
