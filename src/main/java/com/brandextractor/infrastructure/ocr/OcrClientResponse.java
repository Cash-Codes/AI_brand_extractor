package com.brandextractor.infrastructure.ocr;

import java.util.List;

/**
 * Raw OCR result returned by an {@link OcrClient}, before normalisation.
 *
 * <p>Image dimensions are included so the {@link OcrPortAdapter} can convert
 * pixel-coordinate bounding boxes to normalised {@code [0.0, 1.0]} domain coordinates
 * without re-decoding the image.
 *
 * @param regions    detected text regions in reading order
 * @param imageWidth  source image width in pixels; 0 when unavailable
 * @param imageHeight source image height in pixels; 0 when unavailable
 */
public record OcrClientResponse(List<DetectedText> regions, int imageWidth, int imageHeight) {

    /** Convenience factory for an empty result (e.g. from a placeholder or failed call). */
    public static OcrClientResponse empty() {
        return new OcrClientResponse(List.of(), 0, 0);
    }
}
