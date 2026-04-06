package com.brandextractor.domain.evidence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceTest {

    private static final Instant NOW = Instant.parse("2026-04-06T00:00:00Z");

    @Test
    void websiteEvidence_implementsEvidence() {
        Evidence evidence = new WebsiteEvidence(
                "id-1", "WEBSITE", "https://example.com", "https://example.com/",
                "Acme Studio", "A design studio", "Acme Studio builds digital products.",
                List.of("H1: Acme"), "https://example.com/favicon.ico",
                List.of("https://example.com/logo.png"), List.of(), List.of(),
                null, null, null, null, null, null,
                0.95, NOW);

        assertThat(evidence.id()).isEqualTo("id-1");
        assertThat(evidence.sourceType()).isEqualTo("WEBSITE");
        assertThat(evidence.confidence()).isEqualTo(0.95);
        assertThat(evidence.extractedAt()).isEqualTo(NOW);
        assertThat(evidence).isInstanceOf(WebsiteEvidence.class);
    }

    @Test
    void colorEvidence_holdsHexAndFrequency() {
        var evidence = new ColorEvidence("color-1", "CSS", "styles.css", "#0F172A", 0.42, 0.88, NOW);

        assertThat(evidence.hexValue()).isEqualTo("#0F172A");
        assertThat(evidence.frequency()).isEqualTo(0.42);
        assertThat(evidence.confidence()).isEqualTo(0.88);
    }

    @Test
    void ocrEvidence_holdsTextBlocks() {
        var evidence = new OcrEvidence("ocr-1", "IMAGE", "flyer.png", List.of("ACME", "Studio"), 0.91, NOW);

        assertThat(evidence.textBlocks()).containsExactly("ACME", "Studio");
        assertThat(evidence.confidence()).isEqualTo(0.91);
    }

    @Test
    void sealedInterface_patternMatch_exhaustive() {
        Evidence e = new TextEvidence("t-1", "HTML", "index.html", "Acme Studio", "heading", 0.85, NOW);

        // This switch must compile — if a new Evidence subtype is added without a case, it fails
        String type = switch (e) {
            case WebsiteEvidence we    -> "website";
            case FlyerEvidence fe      -> "flyer";
            case OcrEvidence oe        -> "ocr";
            case VisualEvidence ve     -> "visual";
            case ColorEvidence ce      -> "color";
            case LinkEvidence le       -> "link";
            case TextEvidence te       -> "text";
            case ScreenshotEvidence se -> "screenshot";
        };

        assertThat(type).isEqualTo("text");
    }
}
