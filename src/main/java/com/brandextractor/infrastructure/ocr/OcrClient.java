package com.brandextractor.infrastructure.ocr;

/**
 * Infrastructure-level abstraction for OCR text extraction.
 *
 * <p>This is the swap boundary: replace {@link VertexOcrClient} with any provider
 * (Cloud Vision API, Tesseract sidecar, AWS Textract, etc.) without touching the
 * domain or the {@link OcrPortAdapter}.
 *
 * <p>Implementations return raw pixel-coordinate results; normalisation to domain
 * coordinates is handled by the adapter.
 */
public interface OcrClient {

    /**
     * Extracts text regions from the supplied image.
     *
     * @param imageBytes raw image bytes (JPEG or PNG)
     * @param mimeType   MIME type of the image (e.g. {@code "image/png"})
     * @return the extraction result; never {@code null}
     */
    OcrClientResponse extract(byte[] imageBytes, String mimeType);
}
