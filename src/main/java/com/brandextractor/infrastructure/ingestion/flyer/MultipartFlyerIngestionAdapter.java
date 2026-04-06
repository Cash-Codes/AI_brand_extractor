package com.brandextractor.infrastructure.ingestion.flyer;

import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.ports.FlyerIngestionPort;
import com.brandextractor.support.error.ExtractionException;
import com.brandextractor.support.util.MimeTypeUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class MultipartFlyerIngestionAdapter implements FlyerIngestionPort {

    static final long MAX_FILE_BYTES       = 10L * 1024 * 1024; // 10 MB
    static final int  MAX_DOMINANT_COLORS  = 5;
    static final int  MAX_SAMPLE_PIXELS    = 10_000;

    // Quantise each channel into 8 buckets (256 / 32 = 8)
    private static final int QUANTIZE_STEP = 32;

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
     * Samples the image on a grid and returns up to {@value #MAX_DOMINANT_COLORS} hex colours
     * ordered by frequency (most frequent first). Each channel is quantised to 8 buckets so
     * near-identical shades collapse into a single representative colour.
     *
     * <p>Fully-transparent pixels (alpha &lt; 128) are skipped.
     */
    List<String> extractDominantColors(BufferedImage image) {
        int step = Math.max(1,
                (int) Math.sqrt((long) image.getWidth() * image.getHeight() / MAX_SAMPLE_PIXELS));

        Map<Integer, Integer> counts = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y += step) {
            for (int x = 0; x < image.getWidth(); x += step) {
                int argb  = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) continue;                      // ignore transparent
                int quantized = quantizeRgb(argb & 0x00FFFFFF);
                counts.merge(quantized, 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(MAX_DOMINANT_COLORS)
                .map(e -> String.format("#%06X", e.getKey()))
                .toList();
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

    private static int quantizeRgb(int rgb) {
        int r = ((rgb >> 16) & 0xFF) / QUANTIZE_STEP * QUANTIZE_STEP;
        int g = ((rgb >>  8) & 0xFF) / QUANTIZE_STEP * QUANTIZE_STEP;
        int b = ( rgb        & 0xFF) / QUANTIZE_STEP * QUANTIZE_STEP;
        return (r << 16) | (g << 8) | b;
    }
}
