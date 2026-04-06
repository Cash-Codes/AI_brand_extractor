package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.SummaryCandidate;
import com.brandextractor.domain.candidate.TaglineCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.evidence.TextBlock;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaglineSummaryRankingServiceTest {

    private TaglineSummaryRankingService service;

    @BeforeEach
    void setUp() { service = new TaglineSummaryRankingService(); }

    // -------------------------------------------------------------------------
    // Tagline – signal priority
    // -------------------------------------------------------------------------

    @Test
    void ogDescriptionScoresHighestTagline() {
        List<Evidence> evidence = List.of(website()
                .ogDescription("Crafting digital brands since 2010")
                .metaDescription("Alternative description")
                .build());

        List<TaglineCandidate> taglines = service.discoverTaglines(evidence);

        assertThat(taglines.get(0).value()).isEqualTo("Crafting digital brands since 2010");
        assertThat(taglines.get(0).score()).isEqualTo(0.85);
    }

    @Test
    void metaDescriptionScoresAboveHeading() {
        List<Evidence> evidence = List.of(website()
                .metaDescription("We build brands that last")
                .headings(List.of("Welcome", "Explore our work"))
                .build());

        List<TaglineCandidate> taglines = service.discoverTaglines(evidence);

        TaglineCandidate meta    = taglineWithValue(taglines, "We build brands that last");
        TaglineCandidate heading = taglineWithValue(taglines, "Explore our work");
        assertThat(meta.score()).isGreaterThan(heading.score());
    }

    @Test
    void secondHeadingUsedAsTagline() {
        List<Evidence> evidence = List.of(website()
                .headings(List.of("Acme Studio", "Where creativity meets craft"))
                .build());

        List<TaglineCandidate> taglines = service.discoverTaglines(evidence);

        assertThat(taglines).anyMatch(t -> t.value().equals("Where creativity meets craft"));
    }

    @Test
    void taglineTooShortIgnored() {
        List<Evidence> evidence = List.of(website().ogDescription("Short").build());

        assertThat(service.discoverTaglines(evidence)).isEmpty();
    }

    @Test
    void taglineTooLongIgnored() {
        List<Evidence> evidence = List.of(website()
                .ogDescription("x".repeat(161))
                .build());

        assertThat(service.discoverTaglines(evidence)).isEmpty();
    }

    @Test
    void ocrSubHeadingBlocksAreTaglineCandidates() {
        OcrEvidence ocr = new OcrEvidence("o1", "IMAGE", "img", List.of(
                new TextBlock("BRAND NAME",             null, 0.98),
                new TextBlock("Your creative partner",  null, 0.90),
                new TextBlock("Award-winning agency",   null, 0.88)),
                0.92, Instant.now());

        List<TaglineCandidate> taglines = service.discoverTaglines(List.<Evidence>of(ocr));

        assertThat(taglines).anyMatch(t -> t.value().equals("Your creative partner"));
        assertThat(taglines).anyMatch(t -> t.value().equals("Award-winning agency"));
    }

    @Test
    void firstOcrBlockNotUsedAsTagline() {
        OcrEvidence ocr = new OcrEvidence("o1", "IMAGE", "img", List.of(
                new TextBlock("ACME STUDIO", null, 0.98)),
                0.98, Instant.now());

        assertThat(service.discoverTaglines(List.<Evidence>of(ocr))).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Tagline – ordering and evidence refs
    // -------------------------------------------------------------------------

    @Test
    void taglinesOrderedByScoreDescending() {
        List<Evidence> evidence = List.of(website()
                .ogDescription("The og description tagline here")
                .metaDescription("The meta description tagline here")
                .headings(List.of("H1 Heading", "The second heading tagline here"))
                .build());

        List<TaglineCandidate> taglines = service.discoverTaglines(evidence);

        for (int i = 0; i < taglines.size() - 1; i++) {
            assertThat(taglines.get(i).score())
                    .isGreaterThanOrEqualTo(taglines.get(i + 1).score());
        }
    }

    @Test
    void taglineCandidateContainsEvidenceRef() {
        WebsiteEvidence w = website().ogDescription("Crafting brands since 2010").build();
        List<TaglineCandidate> taglines = service.discoverTaglines(List.<Evidence>of(w));

        assertThat(taglines.get(0).evidenceRefs()).contains(w.id());
    }

    // -------------------------------------------------------------------------
    // Summary – signals
    // -------------------------------------------------------------------------

    @Test
    void visibleTextProducesSummaryCandidate() {
        List<Evidence> evidence = List.of(website()
                .visibleText("We are a brand design agency with over 20 years of experience building memorable identities.")
                .build());

        List<SummaryCandidate> summaries = service.discoverSummaries(evidence);

        assertThat(summaries).isNotEmpty();
        assertThat(summaries.get(0).score()).isEqualTo(0.65);
    }

    @Test
    void visibleTextTruncatedTo500CharsForSummary() {
        List<Evidence> evidence = List.of(website()
                .visibleText("x".repeat(600))
                .build());

        List<SummaryCandidate> summaries = service.discoverSummaries(evidence);

        assertThat(summaries.get(0).value()).hasSizeLessThanOrEqualTo(500);
    }

    @Test
    void ocrBlocksJoinedForSummary() {
        OcrEvidence ocr = new OcrEvidence("o1", "IMAGE", "img", List.of(
                new TextBlock("ACME STUDIO",                  null, 0.98),
                new TextBlock("Your creative partner",        null, 0.90),
                new TextBlock("info@acme.com  +1 555 0100",   null, 0.85)),
                0.91, Instant.now());

        List<SummaryCandidate> summaries = service.discoverSummaries(List.<Evidence>of(ocr));

        assertThat(summaries).isNotEmpty();
        assertThat(summaries.get(0).value()).contains("ACME STUDIO");
        assertThat(summaries.get(0).value()).contains("Your creative partner");
    }

    @Test
    void shortVisibleTextBelowMinIgnored() {
        List<Evidence> evidence = List.of(website().visibleText("Too short").build());

        assertThat(service.discoverSummaries(evidence)).isEmpty();
    }

    @Test
    void websiteSummaryScoresAboveOcrSummary() {
        WebsiteEvidence w = website()
                .visibleText("A long enough visible text body for summary extraction here.")
                .build();
        OcrEvidence ocr = new OcrEvidence("o1", "IMAGE", "img", List.of(
                new TextBlock("Flyer text one",   null, 0.9),
                new TextBlock("Flyer text two",   null, 0.9),
                new TextBlock("Flyer text three", null, 0.9)),
                0.9, Instant.now());

        List<SummaryCandidate> summaries = service.discoverSummaries(List.<Evidence>of(w, ocr));

        assertThat(summaries.get(0).score()).isGreaterThan(summaries.get(1).score());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static TaglineCandidate taglineWithValue(List<TaglineCandidate> list, String value) {
        return list.stream().filter(t -> t.value().equals(value)).findFirst().orElseThrow();
    }

    private static WebsiteBuilder website() { return new WebsiteBuilder(); }

    private static class WebsiteBuilder {
        private String ogDescription;
        private String metaDescription;
        private String visibleText = "";
        private List<String> headings = List.of();

        WebsiteBuilder ogDescription(String v)   { this.ogDescription = v; return this; }
        WebsiteBuilder metaDescription(String v) { this.metaDescription = v; return this; }
        WebsiteBuilder visibleText(String v)     { this.visibleText = v; return this; }
        WebsiteBuilder headings(List<String> v)  { this.headings = v; return this; }

        WebsiteEvidence build() {
            return new WebsiteEvidence(
                    "w-test", "WEBSITE", "https://example.com", "https://example.com/",
                    null, metaDescription, visibleText, headings,
                    null, List.of(), List.of(), List.of(),
                    null, ogDescription, null, null, null, null,
                    1.0, Instant.now());
        }
    }
}
