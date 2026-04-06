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
            String hex   = colors.get(i);
            double base  = Math.max(0.50, 0.72 - i * 0.01);
            double score = penalise(base, hex);
            String rationale = "CSS colour from website styles (position " + (i + 1) + ")";
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
     * Penalises near-white and near-black colours by −0.20 — they are structural
     * scaffolding rather than brand palette colours.
     */
    static double penalise(double base, String hex) {
        if (hex == null || hex.length() < 7) return base;
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            double lightness = (Math.max(r, Math.max(g, b)) + Math.min(r, Math.min(g, b))) / 510.0;
            if (lightness >= 0.95 || lightness <= 0.05) return Math.max(0.0, base - 0.20);
        } catch (NumberFormatException ignored) { }
        return base;
    }
}
