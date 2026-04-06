package com.brandextractor.domain.ports;

import com.brandextractor.domain.evidence.ScreenshotEvidence;

import java.util.Optional;

public interface ScreenshotPort {
    Optional<ScreenshotEvidence> capture(String url);
}
