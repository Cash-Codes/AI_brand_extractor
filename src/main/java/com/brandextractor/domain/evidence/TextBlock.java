package com.brandextractor.domain.evidence;

/**
 * A single text region detected by an OCR engine.
 *
 * @param text        the recognised text content
 * @param boundingBox normalised position within the source image;
 *                    {@code null} when the OCR provider does not return spatial data
 * @param confidence  per-block recognition confidence in {@code [0.0, 1.0]}
 */
public record TextBlock(String text, BoundingBox boundingBox, double confidence) {}
