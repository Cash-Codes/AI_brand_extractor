package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.LinkCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LinkRankingServiceTest {

    private LinkRankingService service;

    @BeforeEach
    void setUp() { service = new LinkRankingService(); }

    // -------------------------------------------------------------------------
    // Platform detection
    // -------------------------------------------------------------------------

    @Test
    void instagramLinkDetectedCorrectly() {
        List<Evidence> evidence = List.of(website(List.of("https://instagram.com/acme")));

        List<LinkCandidate> links = service.discover(evidence);

        assertThat(links.get(0).platform()).isEqualTo("instagram");
        assertThat(links.get(0).score()).isEqualTo(0.90);
    }

    @Test
    void linkedinLinkDetectedCorrectly() {
        assertPlatform("https://linkedin.com/company/acme", "linkedin", 0.88);
    }

    @Test
    void twitterLinkDetectedCorrectly() {
        assertPlatform("https://twitter.com/acme", "twitter", 0.80);
    }

    @Test
    void xDotComDetectedAsTwitter() {
        assertPlatform("https://x.com/acme", "twitter", 0.80);
    }

    @Test
    void facebookLinkDetectedCorrectly() {
        assertPlatform("https://facebook.com/acme", "facebook", 0.78);
    }

    @Test
    void tiktokLinkDetectedCorrectly() {
        assertPlatform("https://tiktok.com/@acme", "tiktok", 0.75);
    }

    @Test
    void youtubeLinkDetectedCorrectly() {
        assertPlatform("https://youtube.com/acme", "youtube", 0.72);
    }

    @Test
    void mailtoLinkDetectedAsEmail() {
        assertPlatform("mailto:hello@acme.com", "email", 0.70);
    }

    @Test
    void telLinkDetectedAsPhone() {
        assertPlatform("tel:+15551234567", "phone", 0.65);
    }

    // -------------------------------------------------------------------------
    // Ordering
    // -------------------------------------------------------------------------

    @Test
    void linksOrderedByScoreDescending() {
        List<Evidence> evidence = List.of(website(List.of(
                "https://instagram.com/acme",
                "https://youtube.com/acme",
                "mailto:hi@acme.com",
                "tel:+15550000")));

        List<LinkCandidate> links = service.discover(evidence);

        for (int i = 0; i < links.size() - 1; i++) {
            assertThat(links.get(i).score())
                    .isGreaterThanOrEqualTo(links.get(i + 1).score());
        }
    }

    // -------------------------------------------------------------------------
    // Deduplication
    // -------------------------------------------------------------------------

    @Test
    void duplicateHrefsDeduplicatedAcrossEvidenceItems() {
        WebsiteEvidence w1 = website(List.of("https://instagram.com/acme"));
        WebsiteEvidence w2 = website(List.of("https://instagram.com/acme", "https://twitter.com/acme"));

        List<LinkCandidate> links = service.discover(List.<Evidence>of(w1, w2));

        long count = links.stream()
                .filter(l -> l.href().equals("https://instagram.com/acme")).count();
        assertThat(count).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Evidence references
    // -------------------------------------------------------------------------

    @Test
    void linkCandidateContainsSourceEvidenceId() {
        WebsiteEvidence w = website(List.of("https://instagram.com/acme"));
        List<LinkCandidate> links = service.discover(List.<Evidence>of(w));

        assertThat(links.get(0).evidenceRefs()).contains(w.id());
    }

    // -------------------------------------------------------------------------
    // detectPlatform and scoreFor helpers
    // -------------------------------------------------------------------------

    @Test
    void detectPlatform_unknownDomainReturnsUnknown() {
        assertThat(LinkRankingService.detectPlatform("https://example.com/page")).isEqualTo("unknown");
    }

    @Test
    void detectPlatform_nullReturnsUnknown() {
        assertThat(LinkRankingService.detectPlatform(null)).isEqualTo("unknown");
    }

    @Test
    void scoreFor_unknownDomainReturnsFallback() {
        assertThat(LinkRankingService.scoreFor("https://unknown-social.io/user")).isEqualTo(0.50);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertPlatform(String href, String expectedPlatform, double expectedScore) {
        List<Evidence> evidence = List.of(website(List.of(href)));
        List<LinkCandidate> links = service.discover(evidence);

        assertThat(links).hasSize(1);
        assertThat(links.get(0).platform()).isEqualTo(expectedPlatform);
        assertThat(links.get(0).score()).isEqualTo(expectedScore);
    }

    private static WebsiteEvidence website(List<String> socialLinks) {
        return new WebsiteEvidence(
                "w-" + socialLinks.hashCode(), "WEBSITE",
                "https://example.com", "https://example.com/",
                null, null, "", List.of(),
                null, List.of(), socialLinks, List.of(),
                null, null, null, null, null, null,
                1.0, Instant.now());
    }
}
