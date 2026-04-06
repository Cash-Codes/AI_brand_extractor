package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.*;
import com.brandextractor.domain.model.*;

import java.util.List;

/**
 * Package-private utility that stamps source metadata and evidence counts
 * onto an {@link ExtractionResult} returned by the AI adapter.
 *
 * <p>The AI adapter does not know the input type or source URL; those are
 * stamped here after analysis, together with evidence-derived counters and
 * post-extraction validation issues.
 */
final class ResultEnricher {

    private ResultEnricher() {}

    static ExtractionResult enrich(
            ExtractionResult base,
            ExtractionInputType inputType,
            String originalSource,
            String resolvedSource,
            List<ValidationIssue> validationIssues,
            List<Evidence> evidence) {

        return new ExtractionResult(
                base.requestId(),
                inputType,
                originalSource,
                resolvedSource,
                base.brandProfile(),
                base.colors(),
                base.assets(),
                base.links(),
                base.confidence(),
                base.warnings(),
                validationIssues,
                countText(evidence),
                countImages(evidence),
                countOcrBlocks(evidence),
                hasScreenshot(evidence),
                evidence);
    }

    private static int countText(List<Evidence> evidence) {
        return (int) evidence.stream()
                .filter(e -> e instanceof WebsiteEvidence || e instanceof TextEvidence)
                .count();
    }

    private static int countImages(List<Evidence> evidence) {
        return (int) evidence.stream()
                .filter(e -> e instanceof FlyerEvidence
                          || e instanceof ScreenshotEvidence
                          || e instanceof VisualEvidence)
                .count();
    }

    private static int countOcrBlocks(List<Evidence> evidence) {
        return evidence.stream()
                .filter(OcrEvidence.class::isInstance)
                .map(OcrEvidence.class::cast)
                .mapToInt(o -> o.blocks().size())
                .sum();
    }

    private static boolean hasScreenshot(List<Evidence> evidence) {
        return evidence.stream().anyMatch(ScreenshotEvidence.class::isInstance);
    }
}
