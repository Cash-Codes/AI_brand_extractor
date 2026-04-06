package com.brandextractor.infrastructure.screenshot;

import com.brandextractor.domain.evidence.ScreenshotEvidence;
import com.brandextractor.domain.ports.ScreenshotPort;
import java.util.Optional;

/**
 * @deprecated Superseded by {@link ScreenshotPortAdapter} + {@link NoOpScreenshotClient}.
 *             Kept for reference; not registered as a Spring bean.
 */
@Deprecated
public class NoOpScreenshotAdapter implements ScreenshotPort {

    @Override
    public Optional<ScreenshotEvidence> capture(String url) {
        return Optional.empty();
    }
}
