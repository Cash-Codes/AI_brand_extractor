package com.brandextractor.infrastructure.screenshot;

import java.util.Optional;

/**
 * Infrastructure-level abstraction for capturing screenshots of web pages.
 *
 * <p>This is the boundary to swap: replace {@link NoOpScreenshotClient} with a real
 * implementation (e.g. Playwright, Puppeteer via Node sidecar, or a remote screenshot
 * API) without touching the domain or the adapter.
 */
public interface ScreenshotClient {

    /**
     * Captures a full-page screenshot of the given URL.
     *
     * @param url the fully-qualified URL to capture
     * @return the captured image, or empty if capture is unavailable or fails gracefully
     */
    Optional<CapturedImage> capture(String url);
}
