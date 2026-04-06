package com.brandextractor.integration;

import com.brandextractor.domain.candidate.ColorCandidate;
import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.brandextractor.infrastructure.ai.client.BrandExtractionAiClient;
import com.brandextractor.infrastructure.ocr.OcrClient;
import com.brandextractor.infrastructure.screenshot.ScreenshotClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class UrlExtractionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandExtractionAiClient aiClient;

    @MockitoBean
    private OcrClient ocrClient;

    @MockitoBean
    private ScreenshotClient screenshotClient;

    private static HttpServer fixtureServer;
    private static String acmeUrl;
    private static String minimalUrl;

    @BeforeAll
    static void startFixtureServer() throws Exception {
        byte[] acmeHtml = Files.readAllBytes(
                Path.of("src/test/resources/fixtures/acme-studio.html"));
        byte[] minimalHtml = Files.readAllBytes(
                Path.of("src/test/resources/fixtures/minimal-page.html"));

        fixtureServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        fixtureServer.createContext("/acme", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, acmeHtml.length);
            exchange.getResponseBody().write(acmeHtml);
            exchange.getResponseBody().close();
        });
        fixtureServer.createContext("/minimal", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, minimalHtml.length);
            exchange.getResponseBody().write(minimalHtml);
            exchange.getResponseBody().close();
        });
        fixtureServer.start();
        int port = fixtureServer.getAddress().getPort();
        acmeUrl   = "http://localhost:" + port + "/acme";
        minimalUrl = "http://localhost:" + port + "/minimal";
    }

    @AfterAll
    static void stopFixtureServer() {
        if (fixtureServer != null) fixtureServer.stop(0);
    }

    @BeforeEach
    void setUpDefaults() {
        when(screenshotClient.capture(anyString())).thenReturn(Optional.empty());
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    void extractUrl_fullPipeline_returns200WithBrandProfile() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inputType").value("URL"))
                .andExpect(jsonPath("$.brandProfile.brandName").value("Acme Studio"))
                .andExpect(jsonPath("$.brandProfile.brandNameConfidence").value(0.94))
                .andExpect(jsonPath("$.brandProfile.tagline").value("Crafting brands that endure"))
                .andExpect(jsonPath("$.colors.primary.value").value("#1A2B3C"))
                .andExpect(jsonPath("$.colors.secondary.value").value("#FF6600"))
                .andExpect(jsonPath("$.confidence.overall").value(0.88))
                .andExpect(jsonPath("$.evidence").doesNotExist());
    }

    @Test
    void extractUrl_sourceMetadata_isPopulated() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source.original").value(acmeUrl))
                .andExpect(jsonPath("$.source.resolved").isNotEmpty());
    }

    @Test
    void extractUrl_evidenceSummary_countsWebsiteEvidence() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceSummary.textEvidenceCount").value(1))
                .andExpect(jsonPath("$.evidenceSummary.imageEvidenceCount").value(0))
                .andExpect(jsonPath("$.evidenceSummary.usedScreenshot").value(false));
    }

    @Test
    void extractUrl_withIncludeEvidence_returnsEvidencePayload() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .param("include", "evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.evidence[0].type").value("WEBSITE"))
                .andExpect(jsonPath("$.evidence[0].ogSiteName").value("Acme Studio"));
    }

    @Test
    void extractUrl_aiClientReceivesCandidatesFromHtml() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk());

        ArgumentCaptor<AiExtractionRequest> captor = ArgumentCaptor.forClass(AiExtractionRequest.class);
        verify(aiClient).extract(captor.capture());
        AiExtractionRequest request = captor.getValue();

        // DeterministicCandidateDiscovery should pick up brand name from og:site_name / headings
        assertThat(request.brandNameCandidates()).isNotEmpty();
        // Evidence list should contain exactly one WebsiteEvidence entry
        assertThat(request.evidence()).hasSize(1);
    }

    @Test
    void extractUrl_colorCandidatesFromCss_passedToAiClient() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk());

        ArgumentCaptor<AiExtractionRequest> captor = ArgumentCaptor.forClass(AiExtractionRequest.class);
        verify(aiClient).extract(captor.capture());

        // acme-studio.html has #1E3A8A (navy, --primary) and #FF6600 (orange, .cta) in its <style>
        List<String> hexValues = captor.getValue().colorCandidates()
                .stream()
                .map(ColorCandidate::hex)
                .toList();
        assertThat(hexValues).contains("#1E3A8A", "#FF6600");
    }

    @Test
    void extractUrl_socialLinks_areDiscoveredAndPassedToAiClient() throws Exception {
        when(aiClient.extract(any())).thenReturn(goldenAiResponse());

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk());

        ArgumentCaptor<AiExtractionRequest> captor = ArgumentCaptor.forClass(AiExtractionRequest.class);
        verify(aiClient).extract(captor.capture());

        List<String> hrefs = captor.getValue().linkCandidates()
                .stream()
                .map(c -> c.href())
                .toList();
        assertThat(hrefs).anyMatch(h -> h.contains("instagram.com"));
    }

    // =========================================================================
    // Normalisation applied end-to-end
    // =========================================================================

    @Test
    void extractUrl_aiReturnsLowercaseHex_normalizerUppercasesIt() throws Exception {
        when(aiClient.extract(any())).thenReturn(new AiExtractionResponse(
                "Acme Studio", 0.90,
                "A tagline", 0.85,
                "A summary.", 0.80,
                List.of("bold"),
                "#1a2b3c", null, null,   // lowercase — normalizer should uppercase
                null, null,
                Map.of(),
                0.85, List.of()));

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colors.primary.value").value("#1A2B3C"));
    }

    // =========================================================================
    // Validation errors
    // =========================================================================

    @Test
    void extractUrl_withBlankUrl_returns400WithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void extractUrl_withFtpScheme_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void extractUrl_unreachableHost_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"http://localhost:1/nowhere\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Extraction Failed"))
                .andExpect(jsonPath("$.status").value(422));
    }

    // =========================================================================
    // Error propagation
    // =========================================================================

    @Test
    void extractUrl_aiThrowsAiProviderException_returns502() throws Exception {
        when(aiClient.extract(any()))
                .thenThrow(new com.brandextractor.support.error.AiProviderException("Gemini quota exceeded"));

        mockMvc.perform(post("/api/v1/extractions/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "%s"}
                                """.formatted(acmeUrl)))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("AI Provider Error"))
                .andExpect(jsonPath("$.status").value(502));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static AiExtractionResponse goldenAiResponse() {
        return new AiExtractionResponse(
                "Acme Studio", 0.94,
                "Crafting brands that endure", 0.87,
                "A full-service branding agency.", 0.91,
                List.of("bold", "minimal", "modern"),
                "#1A2B3C", "#FF6600", null,
                "https://acme.com/logo.png", null,
                Map.of("instagram", "https://instagram.com/acmestudio"),
                0.88, List.of());
    }
}
