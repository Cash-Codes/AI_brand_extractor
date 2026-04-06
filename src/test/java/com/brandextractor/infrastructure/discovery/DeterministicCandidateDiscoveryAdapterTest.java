package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.ExtractionCandidates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicCandidateDiscoveryAdapterTest {

    private DeterministicCandidateDiscoveryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DeterministicCandidateDiscoveryAdapter(
                new BrandNameRankingService(),
                new TaglineSummaryRankingService(),
                new ColorRankingService(),
                new AssetRankingService(),
                new LinkRankingService());
    }

    @Test
    void populatesAllDimensionsFromWebsiteEvidence() {
        List<Evidence> evidence = List.<Evidence>of(new WebsiteEvidence(
                "w1", "WEBSITE", "https://acme.com", "https://acme.com/",
                "Acme Studio | Design", "We craft memorable brands",
                "Acme Studio builds digital products for forward-thinking companies worldwide.",
                List.of("Acme Studio", "Our Work"),
                "https://acme.com/favicon.ico",
                List.of("https://acme.com/logo.png", "https://acme.com/hero.jpg"),
                List.of("https://instagram.com/acme", "mailto:hello@acme.com"),
                List.of("#1A2B3C", "#FF6600"),
                "Acme Studio", "We craft memorable brands",
                "https://acme.com/og-logo.png", "Acme Studio",
                "summary_large_image", "https://acme.com/tw.jpg",
                1.0, Instant.now()));

        ExtractionCandidates candidates = adapter.discover(evidence);

        assertThat(candidates.brandNames()).isNotEmpty();
        assertThat(candidates.taglines()).isNotEmpty();
        assertThat(candidates.summaries()).isNotEmpty();
        assertThat(candidates.colors()).isNotEmpty();
        assertThat(candidates.assets()).isNotEmpty();
        assertThat(candidates.links()).isNotEmpty();
        assertThat(candidates.toneKeywords()).isEmpty(); // reserved for AI
    }

    @Test
    void returnsEmptyCandidatesForEmptyEvidence() {
        ExtractionCandidates candidates = adapter.discover(List.of());

        assertThat(candidates.brandNames()).isEmpty();
        assertThat(candidates.taglines()).isEmpty();
        assertThat(candidates.summaries()).isEmpty();
        assertThat(candidates.colors()).isEmpty();
        assertThat(candidates.assets()).isEmpty();
        assertThat(candidates.links()).isEmpty();
    }

    @Test
    void topBrandNameCandidateIsOgSiteName() {
        List<Evidence> evidence = List.<Evidence>of(new WebsiteEvidence(
                "w1", "WEBSITE", "https://acme.com", "https://acme.com/",
                "Acme | page", null, "", List.of(),
                null, List.of(), List.of(), List.of(),
                null, null, null, "Acme Corp",
                null, null, 1.0, Instant.now()));

        ExtractionCandidates candidates = adapter.discover(evidence);

        assertThat(candidates.brandNames().get(0).value()).isEqualTo("Acme Corp");
    }
}
