package com.brandextractor.infrastructure.screenshot;

/**
 * Raw image data returned by a {@link ScreenshotClient} before it is converted
 * into a domain {@link com.brandextractor.domain.evidence.ScreenshotEvidence}.
 */
public record CapturedImage(
        byte[] bytes,
        String mimeType,
        int width,
        int height) {}
