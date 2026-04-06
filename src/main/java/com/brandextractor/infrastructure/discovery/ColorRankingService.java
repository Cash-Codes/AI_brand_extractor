package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.ColorCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts and ranks colour candidates from website and flyer evidence.
 *
 * <h3>Signal sources and base scores</h3>
 * <ul>
 *   <li>Flyer dominant colours (position 0) — 0.90 (top colour in physically printed material)</li>
 *   <li>Flyer dominant colours (positions 1–4) — 0.85 descending by rank</li>
 *   <li>CSS inline/style-tag colours (website) — 0.70 (structural design tokens)</li>
 * </ul>
 *
 * <p>When the same hex value appears in multiple sources the highest score wins.
 * Colours that look like near-white ({@code lightness ≥ 95%}) or near-black
 * ({@code lightness ≤ 5%}) are penalised by −0.20 because they are usually
 * background/text colours rather than brand colours.
 */
@Component
class ColorRankingService {

    private static final int MAX_COLORS = 20;

    List<ColorCandidate> discover(List<Evidence> evidence) {
        Map<String, ColorCandidate> byHex = new LinkedHashMap<>();

        for (Evidence e : evidence) {
            switch (e) {
                case FlyerEvidence f   -> collectFromFlyer(f, byHex);
                case WebsiteEvidence w -> collectFromWebsite(w, byHex);
                default                -> { }
            }
        }

        return byHex.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(MAX_COLORS)
                .toList();
    }

    // -------------------------------------------------------------------------

    private void collectFromFlyer(FlyerEvidence f, Map<String, ColorCandidate> acc) {
        List<String> colors = f.dominantColors();
        for (int i = 0; i < colors.size(); i++) {
            String hex   = colors.get(i);
            double base  = i == 0 ? 0.90 : Math.max(0.70, 0.87 - i * 0.04);
            double score = penalise(base, hex);
            String rationale = i == 0
                    ? "Most frequent colour in flyer image (dominant)"
                    : "Flyer dominant colour rank " + (i + 1);
            offer(acc, hex, score, rationale, f.id());
        }
    }

    private void collectFromWebsite(WebsiteEvidence w, Map<String, ColorCandidate> acc) {
        List<String> colors = w.cssColorCandidates();
        for (int i = 0; i < colors.size(); i++) {
            String hex = colors.get(i);
            // Position 0–1: theme-color / CSS brand vars — highest CSS signal
            // Position 2+: utility stylesheet colours — decay quickly
            double base = i == 0 ? 0.90
                        : i == 1 ? 0.82
                        : Math.max(0.45, 0.78 - i * 0.03);
            double score = penalise(base, hex);
            String rationale = i < 2
                    ? "Brand colour signal from SVG logo, OG image, theme-color, or CSS custom property"
                    : "CSS colour from website styles (position " + (i + 1) + ")";
            offer(acc, hex, score, rationale, w.id());
        }
    }

    private static void offer(Map<String, ColorCandidate> acc,
                               String hex, double score, String rationale, String ref) {
        if (hex == null || hex.isBlank()) return;
        String key = hex.toUpperCase();
        acc.merge(key,
                new ColorCandidate(key, score, rationale, List.of(ref)),
                (existing, incoming) -> existing.score() >= incoming.score() ? existing : incoming);
    }

    /**
     * Penalises colours that are structural scaffolding rather than brand palette colours.
     *
     * <ul>
     *   <li><b>Near-white</b> (lightness ≥ 0.75) or <b>near-black</b> (lightness ≤ 0.05): −0.25.
     *       Catches light utility colors including Tailwind pastels such as
     *       {@code pink-200} (#FEB2B2, L≈0.85) and {@code orange-200} (#FBD38D, L≈0.77)
     *       which appear in CSS utility stylesheets but are not brand colours.</li>
     *   <li><b>Low chroma</b> (max−min &lt; 38/255 ≈ 0.15): −0.20. Colours with very little
     *       saturation are almost always neutral backgrounds or text, not brand accents.</li>
     * </ul>
     *
     * <p>Penalties are applied independently and can stack.
     */
    static double penalise(double base, String hex) {
        if (hex == null || hex.length() < 7) return base;
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            double lightness = (max + min) / 510.0;
            double chroma    = (max - min) / 255.0;

            double score = base;
            // Near-white (≥0.75) or near-black (≤0.05) — background/text scaffolding
            if (lightness >= 0.75 || lightness <= 0.05) score -= 0.25;
            // Very low chroma — near-neutral, almost certainly not a brand accent
            if (chroma < 0.15)                          score -= 0.20;
            return Math.max(0.0, score);
        } catch (NumberFormatException ignored) { }
        return base;
    }
}
