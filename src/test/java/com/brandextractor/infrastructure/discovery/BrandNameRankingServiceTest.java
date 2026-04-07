package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.BrandNameCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.evidence.TextBlock;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrandNameRankingServiceTest {

    private BrandNameRankingService service;

    @BeforeEach
    void setUp() { service = new BrandNameRankingService(); }

    // -------------------------------------------------------------------------
    // Signal priority
    // -------------------------------------------------------------------------

    @Test
    void ogSiteNameScoresHighest() {
        List<Evidence> evidence = List.of(website()
                .ogSiteName("Acme Studio")
                .title("Acme Studio | Design")
                .build());

        List<BrandNameCandidate> candidates = service.discover(evidence);

        assertThat(candidates.get(0).value()).isEqualTo("Acme Studio");
        assertThat(candidates.get(0).score()).isEqualTo(0.90);
    }

    @Test
    void firstH1ScoresAboveTitle() {
        List<Evidence> evidence = List.of(website()
                .headings(List.of("Acme Studio"))
                .title("Different Title")
                .build());

        List<BrandNameCandidate> candidates = service.discover(evidence);

        BrandNameCandidate h1    = candidateWithValue(candidates, "Acme Studio");
        BrandNameCandidate title = candidateWithValue(candidates, "Different Title");
        assertThat(h1.score()).isGreaterThan(title.score());
    }

    @Test
    void titleFirstSegmentStrippedAtSeparator() {
        List<Evidence> evidence = List.of(website()
                .title("Acme Studio | Design Agency")
                .build());

        List<BrandNameCandidate> candidates = service.discover(evidence);

        assertThat(candidates).anyMatch(c -> c.value().equals("Acme Studio"));
        assertThat(candidates).noneMatch(c -> c.value().contains("|"));
    }

    @Test
    void ocrFirstBlockShortTextIsCandidate() {
        OcrEvidence ocr = new OcrEvidence("o1", "IMAGE", "img",
                List.of(new TextBlock("ACME", null, 0.95)), 0.95, Instant.now());

        List<BrandNameCandidate> candidates = service.discover(List.<Evidence>of(ocr));

        assertThat(candidates).anyMatch(c -> c.value().equals("ACME") && c.score() == 0.65);
    }

    @Test
    void ocrFirstBlockLongerThan40CharsIgnored() {
        OcrEvidence ocr = new OcrEvidence("o1", "IMAGE", "img",
                List.of(new TextBlock("A".repeat(41), null, 0.9)), 0.9, Instant.now());

        assertThat(service.discover(List.<Evidence>of(ocr))).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Deduplication
    // -------------------------------------------------------------------------

    @Test
    void duplicatesDeduplicatedKeepingHighestScore() {
        List<Evidence> evidence = List.of(website()
                .ogSiteName("Acme Studio")          // score 0.90
                .headings(List.of("Acme Studio"))   // score 0.80 — same value
                .build());

        List<BrandNameCandidate> candidates = service.discover(evidence);

        long count = candidates.stream()
                .filter(c -> c.value().equalsIgnoreCase("Acme Studio")).count();
        assertThat(count).isEqualTo(1);
        assertThat(candidateWithValue(candidates, "Acme Studio").score()).isEqualTo(0.90);
    }

    @Test
    void deduplicationIsCaseInsensitive() {
        List<Evidence> evidence = List.of(website()
                .ogSiteName("Acme Studio")
                .headings(List.of("acme studio"))
                .build());

        List<BrandNameCandidate> candidates = service.discover(evidence);

        long count = candidates.stream()
                .filter(c -> c.value().equalsIgnoreCase("Acme Studio")).count();
        assertThat(count).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Ordering
    // -------------------------------------------------------------------------

    @Test
    void candidatesOrderedByScoreDescending() {
        List<Evidence> evidence = List.of(website()
                .ogSiteName("SiteNameBrand")
                .title("TitleBrand | page")
                .headings(List.of("H1Brand"))
                .build());

        List<BrandNameCandidate> candidates = service.discover(evidence);

        for (int i = 0; i < candidates.size() - 1; i++) {
            assertThat(candidates.get(i).score())
                    .isGreaterThanOrEqualTo(candidates.get(i + 1).score());
        }
    }

    // -------------------------------------------------------------------------
    // Evidence references and rationale
    // -------------------------------------------------------------------------

    @Test
    void candidatesReferenceSourceEvidenceId() {
        WebsiteEvidence w = website().ogSiteName("Acme").build();
        List<BrandNameCandidate> candidates = service.discover(List.<Evidence>of(w));

        assertThat(candidates.get(0).evidenceRefs()).contains(w.id());
    }

    @Test
    void candidatesIncludeNonBlankRationale() {
        List<BrandNameCandidate> candidates =
                service.discover(List.<Evidence>of(website().ogSiteName("Acme").build()));

        assertThat(candidates.get(0).rationale()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // firstSegment helper
    // -------------------------------------------------------------------------

    @Test
    void firstSegment_splitsOnPipe() {
        assertThat(BrandNameRankingService.firstSegment("Acme | Tagline")).isEqualTo("Acme");
    }

    @Test
    void firstSegment_splitsOnEnDash() {
        assertThat(BrandNameRankingService.firstSegment("Acme – Design")).isEqualTo("Acme");
    }

    @Test
    void firstSegment_splitsOnHyphen() {
        assertThat(BrandNameRankingService.firstSegment("Acme - Design")).isEqualTo("Acme");
    }

    @Test
    void firstSegment_returnsWholeStringWhenNoSeparator() {
        assertThat(BrandNameRankingService.firstSegment("Acme Studio")).isEqualTo("Acme Studio");
    }

    @Test
    void firstSegment_returnsNullForNull() {
        assertThat(BrandNameRankingService.firstSegment(null)).isNull();
    }

    @Test
    void firstSegment_returnsNullForBlank() {
        assertThat(BrandNameRankingService.firstSegment("   ")).isNull();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static BrandNameCandidate candidateWithValue(List<BrandNameCandidate> list, String value) {
        return list.stream().filter(c -> c.value().equals(value)).findFirst().orElseThrow();
    }

    private static WebsiteBuilder website() { return new WebsiteBuilder(); }

    private static class WebsiteBuilder {
        private String ogSiteName;
        private String title;
        private List<String> headings = List.of();

        WebsiteBuilder ogSiteName(String v)     { this.ogSiteName = v; return this; }
        WebsiteBuilder title(String v)          { this.title = v; return this; }
        WebsiteBuilder headings(List<String> v) { this.headings = v; return this; }

        WebsiteEvidence build() {
            return new WebsiteEvidence(
                    "w-test", "WEBSITE", "https://example.com", "https://example.com/",
                    title, null, "", headings,
                    null, List.of(), List.of(), List.of(),
                    null, null, null, ogSiteName, null, null,
                    1.0, Instant.now());
        }
    }
}
