package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.ColorCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ColorRankingServiceTest {

    private ColorRankingService service;

    @BeforeEach
    void setUp() { service = new ColorRankingService(); }

    // -------------------------------------------------------------------------
    // Source priority
    // -------------------------------------------------------------------------

    @Test
    void flyerDominantColorScoresHigherThanCssColor() {
        List<Evidence> evidence = List.of(
                flyer(List.of("#FF6600")),
                website(List.of("#FF6600")));

        List<ColorCandidate> colors = service.discover(evidence);

        // Same hex, flyer wins — dedup keeps highest score
        assertThat(colors).hasSize(1);
        assertThat(colors.get(0).score()).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void flyerTopColorHasHighestBaseScore() {
        List<Evidence> evidence = List.of(flyer(List.of("#AA1111", "#BB2222", "#CC3333")));

        List<ColorCandidate> colors = service.discover(evidence);

        assertThat(colors.get(0).hex()).isEqualTo("#AA1111");
        assertThat(colors.get(0).score()).isEqualTo(0.90);
    }

    @Test
    void cssColorsCollectedFromWebsite() {
        List<Evidence> evidence = List.of(website(List.of("#3B82F6", "#1E40AF")));

        List<ColorCandidate> colors = service.discover(evidence);

        assertThat(colors).extracting(ColorCandidate::hex)
                .contains("#3B82F6", "#1E40AF");
    }

    // -------------------------------------------------------------------------
    // Deduplication
    // -------------------------------------------------------------------------

    @Test
    void duplicatesDeduplicatedKeepingHigherScore() {
        // Same hex in flyer (0.90) and website (0.70) — flyer score wins
        List<Evidence> evidence = List.of(
                flyer(List.of("#FF0000")),
                website(List.of("#FF0000")));

        List<ColorCandidate> colors = service.discover(evidence);

        assertThat(colors.stream().filter(c -> c.hex().equals("#FF0000")).count()).isEqualTo(1);
        assertThat(colors.get(0).score()).isGreaterThanOrEqualTo(0.85);
    }

    // -------------------------------------------------------------------------
    // Penalty for near-white / near-black
    // -------------------------------------------------------------------------

    @Test
    void nearWhiteIsPenalised() {
        // #FFFFFF: lightness=1.0 (≥0.75 → -0.25) + chroma=0 (<0.15 → -0.20) = 0.45
        double white   = ColorRankingService.penalise(0.90, "#FFFFFF");
        double midtone = ColorRankingService.penalise(0.90, "#FF6600");

        assertThat(white).isLessThan(midtone);
        assertThat(white).isCloseTo(0.45, within(1e-9));
    }

    @Test
    void nearBlackIsPenalised() {
        // #000000: lightness=0 (≤0.05 → -0.25) + chroma=0 (<0.15 → -0.20) = 0.45
        double black   = ColorRankingService.penalise(0.90, "#000000");
        double midtone = ColorRankingService.penalise(0.90, "#336699");

        assertThat(black).isLessThan(midtone);
        assertThat(black).isCloseTo(0.45, within(1e-9));
    }

    @Test
    void midtoneColorNotPenalised() {
        // #3B82F6: lightness≈0.598, chroma≈0.733 — no penalty
        double score = ColorRankingService.penalise(0.85, "#3B82F6");
        assertThat(score).isEqualTo(0.85);
    }

    @Test
    void penaltyDoesNotGoNegative() {
        double score = ColorRankingService.penalise(0.10, "#FFFFFF");
        assertThat(score).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void tailwindPastelPinkIsPenalised() {
        // #FEB2B2 (Tailwind pink-200): lightness≈0.847 (≥0.75 → -0.25), chroma≈0.298 → -0.25 total
        double pastel = ColorRankingService.penalise(0.70, "#FEB2B2");
        double brand  = ColorRankingService.penalise(0.70, "#E40043"); // CRUK pink

        assertThat(pastel).isLessThan(brand);
        assertThat(pastel).isCloseTo(0.45, within(1e-9));
        assertThat(brand).isEqualTo(0.70);
    }

    @Test
    void tailwindPastelOrangeIsPenalised() {
        // #FBD38D (Tailwind orange-200): lightness≈0.769 (≥0.75 → -0.25), chroma≈0.431 → -0.25 total
        double pastel = ColorRankingService.penalise(0.70, "#FBD38D");
        double brand  = ColorRankingService.penalise(0.70, "#FF6B00");

        assertThat(pastel).isLessThan(brand);
        assertThat(pastel).isCloseTo(0.45, within(1e-9));
    }

    @Test
    void tailwindUtilityGrayIsPenalised() {
        // #EDF2F7 (Tailwind gray-100): both penalties
        double utility = ColorRankingService.penalise(0.70, "#EDF2F7");
        double brand   = ColorRankingService.penalise(0.70, "#E40043");

        assertThat(utility).isLessThan(brand);
        assertThat(brand).isEqualTo(0.70);
    }

    @Test
    void lowChromaNeutralIsPenalised() {
        // #AAAAAA: lightness≈0.667 (no near-white/black), chroma=0 → -0.20 only
        double neutral = ColorRankingService.penalise(0.70, "#AAAAAA");
        double accent  = ColorRankingService.penalise(0.70, "#E40043");

        assertThat(neutral).isCloseTo(0.50, within(1e-9));
        assertThat(accent).isEqualTo(0.70);
    }

    // -------------------------------------------------------------------------
    // Ordering and limits
    // -------------------------------------------------------------------------

    @Test
    void colorsOrderedByScoreDescending() {
        List<Evidence> evidence = List.of(
                flyer(List.of("#AA1111", "#BB2222", "#CC3333")),
                website(List.of("#DD4444", "#EE5555")));

        List<ColorCandidate> colors = service.discover(evidence);

        for (int i = 0; i < colors.size() - 1; i++) {
            assertThat(colors.get(i).score())
                    .isGreaterThanOrEqualTo(colors.get(i + 1).score());
        }
    }

    @Test
    void candidatesContainEvidenceRefAndRationale() {
        FlyerEvidence f = flyer(List.of("#AA1111"));
        List<ColorCandidate> colors = service.discover(List.<Evidence>of(f));

        assertThat(colors.get(0).evidenceRefs()).contains(f.id());
        assertThat(colors.get(0).rationale()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static FlyerEvidence flyer(List<String> colors) {
        return new FlyerEvidence("f-test", "FLYER", "upload", "image/png",
                800, 600, 10_000L, colors, 1.0, Instant.now());
    }

    private static WebsiteEvidence website(List<String> cssColors) {
        return new WebsiteEvidence(
                "w-test", "WEBSITE", "https://example.com", "https://example.com/",
                null, null, "", List.of(),
                null, List.of(), List.of(), cssColors,
                null, null, null, null, null, null,
                1.0, Instant.now());
    }
}
