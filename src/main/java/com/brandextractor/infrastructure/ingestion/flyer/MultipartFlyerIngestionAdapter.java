package com.brandextractor.infrastructure.ingestion.flyer;

import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.ports.FlyerIngestionPort;
import com.brandextractor.support.error.ExtractionException;
import com.brandextractor.support.util.ImageColorSampler;
import com.brandextractor.support.util.MimeTypeUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class MultipartFlyerIngestionAdapter implements FlyerIngestionPort {

    static final long MAX_FILE_BYTES       = 10L * 1024 * 1024; // 10 MB
    static final int  MAX_DOMINANT_COLORS  = 5;
    static final int  MAX_SAMPLE_PIXELS    = 10_000;

    private static final Set<String> SUPPORTED_TYPES = Set.of("image/jpeg", "image/png");

    private final MimeTypeUtils mimeTypeUtils;

    public MultipartFlyerIngestionAdapter(MimeTypeUtils mimeTypeUtils) {
        this.mimeTypeUtils = mimeTypeUtils;
    }

    // -------------------------------------------------------------------------
    // Port interface
    // -------------------------------------------------------------------------

    @Override
    public FlyerEvidence ingest(byte[] imageBytes, String mimeType, String sourceLabel) {
        validateSize(imageBytes);
        String verified = validateAndDetectMimeType(imageBytes);

        BufferedImage image = decode(imageBytes);

        return new FlyerEvidence(
                UUID.randomUUID().toString(),
                "FLYER",
                sourceLabel != null ? sourceLabel : "upload",
                verified,
                image.getWidth(),
                image.getHeight(),
                imageBytes.length,
                extractDominantColors(image),
                1.0,
                Instant.now());
    }

    // -------------------------------------------------------------------------
    // Package-private for unit testing
    // -------------------------------------------------------------------------

    BufferedImage decode(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new ExtractionException("Could not decode image — format unrecognised by ImageIO");
            }
            return img;
        } catch (IOException e) {
            throw new ExtractionException("Failed to decode image bytes", e);
        }
    }

    /**
     * Samples the image and returns up to {@value #MAX_DOMINANT_COLORS} dominant hex colours.
     * Delegates to {@link ImageColorSampler}.
     */
    List<String> extractDominantColors(BufferedImage image) {
        return ImageColorSampler.dominantColors(image);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateSize(byte[] bytes) {
        if (bytes.length > MAX_FILE_BYTES) {
            throw new ExtractionException(
                    "Image size %,d bytes exceeds the %,d-byte limit".formatted(
                            bytes.length, MAX_FILE_BYTES));
        }
    }

    private String validateAndDetectMimeType(byte[] bytes) {
        String detected = mimeTypeUtils.detectMimeType(bytes);
        if (!SUPPORTED_TYPES.contains(detected)) {
            throw new ExtractionException(
                    "Unsupported image format '%s' — only JPEG and PNG are accepted".formatted(detected));
        }
        return detected;
    }

}
