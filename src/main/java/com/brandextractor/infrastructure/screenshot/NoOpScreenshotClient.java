package com.brandextractor.infrastructure.screenshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Placeholder {@link ScreenshotClient} that always skips capture.
 *
 * <p>Replace this bean with a real implementation (Playwright, Puppeteer sidecar,
 * remote screenshot API, etc.) to enable screenshot evidence in extractions.
 * No browser automation dependency is required while this placeholder is active.
 */
@Component
public class NoOpScreenshotClient implements ScreenshotClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpScreenshotClient.class);

    @Override
    public Optional<CapturedImage> capture(String url) {
        log.debug("Screenshot capture skipped for {} — NoOpScreenshotClient active. " +
                  "Replace with a real ScreenshotClient to enable screenshots.", url);
        return Optional.empty();
    }
}
