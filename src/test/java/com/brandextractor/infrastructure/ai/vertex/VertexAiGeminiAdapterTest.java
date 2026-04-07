package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.*;
import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.brandextractor.infrastructure.ai.client.BrandExtractionAiClient;
import com.brandextractor.infrastructure.discovery.DeterministicCandidateDiscoveryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VertexAiGeminiAdapterTest {

    private BrandExtractionAiClient               aiClient;
    private DeterministicCandidateDiscoveryAdapter candidateDiscovery;
    private VertexAiGeminiAdapter                 adapter;

    @BeforeEach
    void setUp() {
        aiClient          = mock(BrandExtractionAiClient.class);
        candidateDiscovery = mock(DeterministicCandidateDiscoveryAdapter.class);
        adapter = new VertexAiGeminiAdapter(aiClient, candidateDiscovery);

        when(candidateDiscovery.discover(any()))
                .thenReturn(new ExtractionCandidates(
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void delegatesToAiClientWithEvidenceAndCandidates() {
        when(aiClient.extract(any())).thenReturn(fullResponse());

        adapter.analyse(List.<Evidence>of(websiteEvidence()));

        verify(aiClient).extract(any(AiExtractionRequest.class));
    }

    @Test
    void passesEvidenceListToCandidateDiscovery() {
        when(aiClient.extract(any())).thenReturn(fullResponse());
        List<Evidence> evidence = List.<Evidence>of(websiteEvidence());

        adapter.analyse(evidence);

        verify(candidateDiscovery).discover(evidence);
    }

    @Test
    void mapsAiResponseToBrandProfile() {
        when(aiClient.extract(any())).thenReturn(fullResponse());

        ExtractionResult result = adapter.analyse(List.<Evidence>of(websiteEvidence()));

        assertThat(result.brandProfile().brandName().value()).isEqualTo("Acme Studio");
        assertThat(result.brandProfile().brandName().confidence()).isEqualTo(0.92);
        assertThat(result.brandProfile().tagline().value()).isEqualTo("Crafting brands");
        assertThat(result.brandProfile().summary().value()).isEqualTo("A great studio.");
        assertThat(result.brandProfile().toneKeywords()).containsExactly("bold", "modern");
    }

    @Test
    void mapsAiResponseToColorSelection() {
        when(aiClient.extract(any())).thenReturn(fullResponse());

        ExtractionResult result = adapter.analyse(List.<Evidence>of(websiteEvidence()));

        assertThat(result.colors().primary().value()).isEqualTo("#1A2B3C");
        assertThat(result.colors().secondary().value()).isEqualTo("#FF6600");
    }

    @Test
    void nullSecondaryColorProducesNullColorValue() {
        var response = new AiExtractionResponse(
                "Acme", 0.9, "Tagline", 0.8, "Summary here.", 0.7,
                List.of(), "#FF0000", null, null,
                null, null, Map.of(), 0.85, List.of());
        when(aiClient.extract(any())).thenReturn(response);

        ExtractionResult result = adapter.analyse(List.<Evidence>of(websiteEvidence()));

        assertThat(result.colors().secondary()).isNull();
    }

    @Test
    void mapsAiResponseToContactLinks() {
        var response = new AiExtractionResponse(
                "Acme", 0.9, "Tagline", 0.8, "Summary here.", 0.7,
                List.of(), "#FF0000", null, null, null, null,
                Map.of("instagram", "https://instagram.com/acme",
                       "email",     "mailto:hello@acme.com"),
                0.85, List.of());
        when(aiClient.extract(any())).thenReturn(response);

        ExtractionResult result = adapter.analyse(List.<Evidence>of(websiteEvidence()));

        assertThat(result.links().instagram()).isEqualTo("https://instagram.com/acme");
        assertThat(result.links().email()).isEqualTo("mailto:hello@acme.com");
    }

    @Test
    void mapsWarningsToExtractionWarnings() {
        var response = new AiExtractionResponse(
                "Acme", 0.9, "Tagline", 0.8, "Summary here.", 0.7,
                List.of(), "#FF0000", null, null, null, null,
                Map.of(), 0.85, List.of("Low confidence on tagline"));
        when(aiClient.extract(any())).thenReturn(response);

        ExtractionResult result = adapter.analyse(List.<Evidence>of(websiteEvidence()));

        assertThat(result.warnings()).extracting(ExtractionWarning::message)
                .containsExactly("Low confidence on tagline");
    }

    @Test
    void resultContainsOriginalEvidence() {
        when(aiClient.extract(any())).thenReturn(fullResponse());
        var evidence = List.<Evidence>of(websiteEvidence());

        ExtractionResult result = adapter.analyse(evidence);

        assertThat(result.evidence()).isEqualTo(evidence);
    }

    @Test
    void resultHasNonNullRequestId() {
        when(aiClient.extract(any())).thenReturn(fullResponse());

        ExtractionResult result = adapter.analyse(List.<Evidence>of(websiteEvidence()));

        assertThat(result.requestId()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AiExtractionResponse fullResponse() {
        return new AiExtractionResponse(
                "Acme Studio", 0.92,
                "Crafting brands", 0.85,
                "A great studio.", 0.80,
                List.of("bold", "modern"),
                "#1A2B3C", "#FF6600", "#222222",
                "https://acme.com/logo.png", "https://acme.com/hero.jpg",
                Map.of("instagram", "https://instagram.com/acme"),
                0.88, List.of());
    }

    private static WebsiteEvidence websiteEvidence() {
        return new WebsiteEvidence(
                "w1", "WEBSITE", "https://acme.com", "https://acme.com/",
                null, null, "", List.of(), null, List.of(), List.of(), List.of(),
                null, null, null, null, null, null, 1.0, Instant.now());
    }
}
