package com.brandextractor.infrastructure.screenshot;

import com.brandextractor.domain.evidence.ScreenshotEvidence;
import com.brandextractor.domain.ports.ScreenshotPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoOpScreenshotAdapter implements ScreenshotPort {

    @Override
    public Optional<ScreenshotEvidence> capture(String url) {
        return Optional.empty();
    }
}
