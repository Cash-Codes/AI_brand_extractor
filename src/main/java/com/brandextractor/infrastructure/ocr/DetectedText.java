package com.brandextractor.infrastructure.ocr;

/**
 * A single text region as returned by an {@link OcrClient}, before domain mapping.
 *
 * @param text        the recognised text
 * @param boundingBox pixel-coordinate bounding box; {@code null} when the provider
 *                    does not return spatial data
 * @param confidence  per-region recognition confidence in {@code [0.0, 1.0]}
 */
public record DetectedText(String text, PixelBoundingBox boundingBox, double confidence) {}
