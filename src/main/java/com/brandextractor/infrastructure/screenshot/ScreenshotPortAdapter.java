package com.brandextractor.infrastructure.screenshot;

import com.brandextractor.domain.evidence.ScreenshotEvidence;
import com.brandextractor.domain.ports.ScreenshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapts the domain {@link ScreenshotPort} to the infrastructure {@link ScreenshotClient}.
 *
 * <p>Converts a {@link CapturedImage} into a {@link ScreenshotEvidence} domain record,
 * assigning a UUID, provenance metadata, and a confidence of 1.0 for a successful capture.
 */
@Component
@RequiredArgsConstructor
public class ScreenshotPortAdapter implements ScreenshotPort {

    private final ScreenshotClient client;

    @Override
    public Optional<ScreenshotEvidence> capture(String url) {
        return client.capture(url).map(image -> new ScreenshotEvidence(
                UUID.randomUUID().toString(),
                "SCREENSHOT",
                url,
                image.bytes(),
                image.mimeType(),
                image.width(),
                image.height(),
                1.0,
                Instant.now()));
    }
}
