package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.domain.candidate.*;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.evidence.TextBlock;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.AssetRole;
import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VertexAiPromptFactoryTest {

    private VertexAiPromptFactory factory;

    @BeforeEach
    void setUp() { factory = new VertexAiPromptFactory(); }

    // -------------------------------------------------------------------------
    // System instruction
    // -------------------------------------------------------------------------

    @Test
    void systemInstruction_containsJsonRule() {
        String instruction = factory.buildSystemInstruction();
        assertThat(instruction).contains("Return ONLY valid JSON");
    }

    @Test
    void systemInstruction_containsHexRule() {
        String instruction = factory.buildSystemInstruction();
        assertThat(instruction).contains("#RRGGBB");
    }

    @Test
    void systemInstruction_isNotBlank() {
        assertThat(factory.buildSystemInstruction()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // User content — evidence sections
    // -------------------------------------------------------------------------

    @Test
    void userContent_includesWebsiteTitle() {
        var request = requestWith(List.of(website()));
        assertThat(factory.buildUserContent(request)).contains("Acme Studio | Design");
    }

    @Test
    void userContent_includesOgSiteName() {
        var request = requestWith(List.of(website()));
        assertThat(factory.buildUserContent(request)).contains("Acme Corp");
    }

    @Test
    void userContent_includesOcrBlocks() {
        OcrEvidence ocr = new OcrEvidence("o1", "IMAGE", "flyer.png",
                List.of(new TextBlock("ACME STUDIO", null, 0.98),
                        new TextBlock("Your creative partner", null, 0.90)),
                0.94, Instant.now());
        var request = requestWith(List.of(ocr));

        String content = factory.buildUserContent(request);
        assertThat(content).contains("ACME STUDIO");
        assertThat(content).contains("Your creative partner");
    }

    @Test
    void userContent_includesBrandNameCandidates() {
        var candidates = List.of(
                new BrandNameCandidate("Acme Corp", 0.90, "og:site_name", List.of("w1")));
        var request = new AiExtractionRequest(
                List.of(), candidates, List.of(), List.of(), List.of(), List.of(), List.of());

        String content = factory.buildUserContent(request);
        assertThat(content).contains("Acme Corp");
        assertThat(content).contains("0.90");
    }

    @Test
    void userContent_includesColorCandidates() {
        var colors = List.of(new ColorCandidate("#1A2B3C", 0.88, "Top flyer colour", List.of("f1")));
        var request = new AiExtractionRequest(
                List.of(), List.of(), List.of(), List.of(), colors, List.of(), List.of());

        assertThat(factory.buildUserContent(request)).contains("#1A2B3C");
    }

    @Test
    void userContent_includesAssetCandidates() {
        var assets = List.of(new AssetCandidate(
                "https://acme.com/logo.png", AssetRole.PRIMARY_LOGO, 0.90,
                "URL contains logo", List.of("w1"), 0, 0, null));
        var request = new AiExtractionRequest(
                List.of(), List.of(), List.of(), List.of(), List.of(), assets, List.of());

        String content = factory.buildUserContent(request);
        assertThat(content).contains("https://acme.com/logo.png");
        assertThat(content).contains("PRIMARY_LOGO");
    }

    @Test
    void userContent_includesLinkCandidates() {
        var links = List.of(new LinkCandidate(
                "https://instagram.com/acme", "instagram", 0.90, List.of("w1")));
        var request = new AiExtractionRequest(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), links);

        assertThat(factory.buildUserContent(request)).contains("instagram");
    }

    @Test
    void userContent_includesTaskSection() {
        var request = requestWith(List.of());
        assertThat(factory.buildUserContent(request)).contains("## Task");
    }

    // -------------------------------------------------------------------------
    // Generation config
    // -------------------------------------------------------------------------

    @Test
    void generationConfig_setsJsonMimeType() {
        var props = props(0.0f, 2048, 2);
        var config = factory.buildGenerationConfig(props);
        assertThat(config.getResponseMimeType()).isEqualTo("application/json");
    }

    @Test
    void generationConfig_setsTemperatureAndTokens() {
        var props = props(0.2f, 1024, 1);
        var config = factory.buildGenerationConfig(props);
        assertThat(config.getTemperature()).isEqualTo(0.2f);
        assertThat(config.getMaxOutputTokens()).isEqualTo(1024);
    }

    @Test
    void generationConfig_hasResponseSchema() {
        var config = factory.buildGenerationConfig(props(0.0f, 2048, 2));
        assertThat(config.hasResponseSchema()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Response schema
    // -------------------------------------------------------------------------

    @Test
    void responseSchema_requiresBrandName() {
        var schema = factory.buildResponseSchema();
        assertThat(schema.getRequiredList()).contains("brandName");
    }

    @Test
    void responseSchema_requiresPrimaryColor() {
        var schema = factory.buildResponseSchema();
        assertThat(schema.getRequiredList()).contains("primaryColor");
    }

    @Test
    void responseSchema_hasContactLinksProperty() {
        var schema = factory.buildResponseSchema();
        assertThat(schema.getPropertiesMap()).containsKey("contactLinks");
    }

    @Test
    void responseSchema_hasToneKeywordsAsArray() {
        var schema = factory.buildResponseSchema();
        var toneSchema = schema.getPropertiesMap().get("toneKeywords");
        assertThat(toneSchema).isNotNull();
        assertThat(toneSchema.getTypeValue())
                .isEqualTo(com.google.cloud.vertexai.api.Type.ARRAY.getNumber());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AiExtractionRequest requestWith(
            java.util.List<com.brandextractor.domain.evidence.Evidence> evidence) {
        return new AiExtractionRequest(
                evidence, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static WebsiteEvidence website() {
        return new WebsiteEvidence(
                "w1", "WEBSITE", "https://acme.com", "https://acme.com/",
                "Acme Studio | Design", "We craft brands",
                "Acme Studio builds things.",
                List.of("Acme Studio", "Our Work"),
                "https://acme.com/favicon.ico",
                List.of("https://acme.com/logo.png"),
                List.of("https://instagram.com/acme"),
                List.of("#1A2B3C"),
                "Acme Studio", "We craft brands",
                "https://acme.com/og.png", "Acme Corp",
                "summary_large_image", null,
                1.0, Instant.now());
    }

    private static VertexAiExtractionProperties props(float temp, int maxTokens, int retries) {
        var p = new VertexAiExtractionProperties();
        p.setProjectId("test-project");
        p.setTemperature(temp);
        p.setMaxOutputTokens(maxTokens);
        p.setMaxParseRetries(retries);
        return p;
    }
}
