package com.brandextractor.infrastructure.ocr;

import com.brandextractor.domain.evidence.BoundingBox;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.evidence.TextBlock;
import com.brandextractor.domain.ports.OcrPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adapts the domain {@link OcrPort} to the infrastructure {@link OcrClient}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Delegates extraction to the active {@link OcrClient} implementation.</li>
 *   <li>Normalises pixel-coordinate bounding boxes to {@code [0.0, 1.0]} domain
 *       coordinates using the image dimensions reported by the client.</li>
 *   <li>Computes overall evidence confidence as the mean of per-block confidences
 *       (falls back to {@code 1.0} for empty results).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OcrPortAdapter implements OcrPort {

    private final OcrClient client;

    @Override
    public OcrEvidence extractText(byte[] imageBytes, String mimeType) {
        OcrClientResponse response = client.extract(imageBytes, mimeType);

        List<TextBlock> blocks = response.regions().stream()
                .map(r -> new TextBlock(
                        r.text(),
                        normalise(r.boundingBox(), response.imageWidth(), response.imageHeight()),
                        r.confidence()))
                .toList();

        return new OcrEvidence(
                UUID.randomUUID().toString(),
                "IMAGE",
                mimeType,
                blocks,
                meanConfidence(blocks),
                Instant.now());
    }

    // -------------------------------------------------------------------------
    // Normalisation helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a pixel bounding box to normalised {@code [0.0, 1.0]} coordinates.
     * Returns {@code null} when the box or image dimensions are unavailable.
     */
    private static BoundingBox normalise(PixelBoundingBox px, int imageWidth, int imageHeight) {
        if (px == null || imageWidth <= 0 || imageHeight <= 0) return null;
        return new BoundingBox(
                (double) px.x()      / imageWidth,
                (double) px.y()      / imageHeight,
                (double) px.width()  / imageWidth,
                (double) px.height() / imageHeight);
    }

    private static double meanConfidence(List<TextBlock> blocks) {
        if (blocks.isEmpty()) return 1.0;
        return blocks.stream()
                .mapToDouble(TextBlock::confidence)
                .average()
                .orElse(1.0);
    }
}
