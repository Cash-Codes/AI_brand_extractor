package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.BrandNameCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts brand-name candidates from website and OCR evidence.
 *
 * <p>Signal priority (highest first):
 * <ol>
 *   <li>og:site_name  — 0.90 (authoritative brand identity field)</li>
 *   <li>First H1      — 0.80 (primary page heading)</li>
 *   <li>og:title      — 0.75 (often "Brand – tagline"; use first segment)</li>
 *   <li>Page title    — 0.70 (first segment before " | " or " – ")</li>
 *   <li>OCR first block ≤ 40 chars — 0.65 (prominent flyer text)</li>
 * </ol>
 *
 * <p>Candidates are deduplicated by normalised value (lowercase trimmed); when the same
 * value appears in multiple signals the highest score is kept.
 */
@Component
class BrandNameRankingService {

    private static final int MAX_BRAND_NAME_LENGTH = 80;

    List<BrandNameCandidate> discover(List<Evidence> evidence) {
        Map<String, BrandNameCandidate> byNormalized = new LinkedHashMap<>();

        for (Evidence e : evidence) {
            switch (e) {
                case WebsiteEvidence w -> collectFromWebsite(w, byNormalized);
                case OcrEvidence o     -> collectFromOcr(o, byNormalized);
                default                -> { /* other evidence types not used */ }
            }
        }

        return byNormalized.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    // -------------------------------------------------------------------------

    private void collectFromWebsite(WebsiteEvidence w, Map<String, BrandNameCandidate> acc) {
        String ref = w.id();

        offer(acc, w.ogSiteName(), 0.90,
                "og:site_name is the authoritative brand identity field", ref);

        if (!w.headings().isEmpty()) {
            offer(acc, w.headings().get(0), 0.80,
                    "First H1 is the primary page heading", ref);
        }

        offer(acc, firstSegment(w.ogTitle()), 0.75,
                "First segment of og:title", ref);

        offer(acc, firstSegment(w.title()), 0.70,
                "First segment of page <title>", ref);
    }

    private void collectFromOcr(OcrEvidence o, Map<String, BrandNameCandidate> acc) {
        if (o.blocks().isEmpty()) return;
        String text = o.blocks().get(0).text();
        if (text != null && text.length() <= 40) {
            offer(acc, text, 0.65,
                    "First OCR block is short — likely a prominent brand name", o.id());
        }
    }

    private static void offer(Map<String, BrandNameCandidate> acc,
                               String value, double score, String rationale, String ref) {
        if (value == null || value.isBlank() || value.length() > MAX_BRAND_NAME_LENGTH) return;
        String key = value.toLowerCase().strip();
        acc.merge(key, new BrandNameCandidate(value.strip(), score, rationale, List.of(ref)),
                (existing, incoming) -> existing.score() >= incoming.score() ? existing : incoming);
    }

    /**
     * Returns the portion of a string before the first occurrence of common
     * title separators ({@code |}, {@code –}, {@code -}, {@code :}).
     * Returns {@code null} if the input is null or blank.
     */
    static String firstSegment(String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.split("[|–\\-:]", 2);
        String segment = parts[0].strip();
        return segment.isBlank() ? null : segment;
    }
}
