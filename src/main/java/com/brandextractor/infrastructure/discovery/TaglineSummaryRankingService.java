package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.SummaryCandidate;
import com.brandextractor.domain.candidate.TaglineCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.evidence.TextBlock;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts tagline and summary candidates from website and OCR evidence.
 *
 * <h3>Tagline signals</h3>
 * <ul>
 *   <li>og:description — 0.85 (intended one-liner)</li>
 *   <li>meta description — 0.75 (often tagline-length)</li>
 *   <li>Second H1 or first H2 — 0.70</li>
 *   <li>OCR block 2–4, 15–120 chars — 0.60 (sub-heading region)</li>
 * </ul>
 *
 * <h3>Summary signals</h3>
 * <ul>
 *   <li>Visible text (first 500 chars) — 0.65</li>
 *   <li>Concatenated OCR blocks — 0.55</li>
 * </ul>
 */
@Component
class TaglineSummaryRankingService {

    private static final int TAGLINE_MIN =  10;
    private static final int TAGLINE_MAX = 160;
    private static final int SUMMARY_MIN =  40;
    private static final int SUMMARY_MAX = 500;

    // -------------------------------------------------------------------------

    List<TaglineCandidate> discoverTaglines(List<Evidence> evidence) {
        List<TaglineCandidate> results = new ArrayList<>();

        for (Evidence e : evidence) {
            switch (e) {
                case WebsiteEvidence w -> collectTaglinesFromWebsite(w, results);
                case OcrEvidence o     -> collectTaglinesFromOcr(o, results);
                default                -> { }
            }
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    List<SummaryCandidate> discoverSummaries(List<Evidence> evidence) {
        List<SummaryCandidate> results = new ArrayList<>();

        for (Evidence e : evidence) {
            switch (e) {
                case WebsiteEvidence w -> collectSummariesFromWebsite(w, results);
                case OcrEvidence o     -> collectSummariesFromOcr(o, results);
                default                -> { }
            }
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Tagline collection
    // -------------------------------------------------------------------------

    private void collectTaglinesFromWebsite(WebsiteEvidence w, List<TaglineCandidate> out) {
        String ref = w.id();

        addTagline(out, w.ogDescription(), 0.85,
                "og:description is the intended page one-liner", ref);
        addTagline(out, w.metaDescription(), 0.75,
                "meta description is often tagline-length", ref);

        List<String> headings = w.headings();
        if (headings.size() >= 2) {
            addTagline(out, headings.get(1), 0.70,
                    "Second heading is frequently a brand sub-tagline", ref);
        } else if (!headings.isEmpty()) {
            // Only one heading: try it as a tagline if not already the brand name candidate
            addTagline(out, headings.get(0), 0.60,
                    "Sole heading used as tagline fallback", ref);
        }
    }

    private void collectTaglinesFromOcr(OcrEvidence o, List<TaglineCandidate> out) {
        List<TextBlock> blocks = o.blocks();
        // Blocks 1–3 (0-indexed) are typically sub-headings on a flyer
        for (int i = 1; i < Math.min(4, blocks.size()); i++) {
            String text = blocks.get(i).text();
            if (text != null && text.length() >= TAGLINE_MIN && text.length() <= TAGLINE_MAX) {
                out.add(new TaglineCandidate(text.strip(), 0.60,
                        "OCR block " + (i + 1) + " is in the sub-heading region", List.of(o.id())));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Summary collection
    // -------------------------------------------------------------------------

    private void collectSummariesFromWebsite(WebsiteEvidence w, List<SummaryCandidate> out) {
        if (w.visibleText() != null && w.visibleText().length() >= SUMMARY_MIN) {
            String excerpt = w.visibleText().length() > SUMMARY_MAX
                    ? w.visibleText().substring(0, SUMMARY_MAX)
                    : w.visibleText();
            out.add(new SummaryCandidate(excerpt.strip(), 0.65,
                    "Visible page text provides the richest body copy for summarisation",
                    List.of(w.id())));
        }
    }

    private void collectSummariesFromOcr(OcrEvidence o, List<SummaryCandidate> out) {
        if (o.blocks().isEmpty()) return;
        String joined = o.blocks().stream()
                .map(TextBlock::text)
                .filter(t -> t != null && !t.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        if (joined.length() >= SUMMARY_MIN) {
            out.add(new SummaryCandidate(joined, 0.55,
                    "Concatenated OCR blocks form the full flyer text", List.of(o.id())));
        }
    }

    // -------------------------------------------------------------------------

    private static void addTagline(List<TaglineCandidate> out, String value,
                                   double score, String rationale, String ref) {
        if (value == null || value.isBlank()) return;
        String v = value.strip();
        if (v.length() < TAGLINE_MIN || v.length() > TAGLINE_MAX) return;
        out.add(new TaglineCandidate(v, score, rationale, List.of(ref)));
    }
}
