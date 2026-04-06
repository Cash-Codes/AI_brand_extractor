package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VertexAiResponseParserTest {

    private VertexAiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new VertexAiResponseParser(new ObjectMapper());
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void parsesCompleteValidResponse() {
        String json = """
                {
                  "brandName": "Acme Studio",
                  "brandNameConfidence": 0.92,
                  "tagline": "Crafting brands that last",
                  "taglineConfidence": 0.85,
                  "summary": "Acme Studio is a full-service brand design agency.",
                  "summaryConfidence": 0.88,
                  "toneKeywords": ["bold", "modern", "minimal"],
                  "primaryColor": "#1A2B3C",
                  "secondaryColor": "#FF6600",
                  "textColor": "#222222",
                  "logoUrl": "https://acme.com/logo.png",
                  "heroImageUrl": "https://acme.com/hero.jpg",
                  "contactLinks": {"instagram": "https://instagram.com/acme"},
                  "overallConfidence": 0.90,
                  "warnings": []
                }
                """;

        AiExtractionResponse r = parser.parse(json);

        assertThat(r.brandName()).isEqualTo("Acme Studio");
        assertThat(r.brandNameConfidence()).isEqualTo(0.92);
        assertThat(r.tagline()).isEqualTo("Crafting brands that last");
        assertThat(r.summary()).isEqualTo("Acme Studio is a full-service brand design agency.");
        assertThat(r.toneKeywords()).containsExactly("bold", "modern", "minimal");
        assertThat(r.primaryColor()).isEqualTo("#1A2B3C");
        assertThat(r.secondaryColor()).isEqualTo("#FF6600");
        assertThat(r.textColor()).isEqualTo("#222222");
        assertThat(r.logoUrl()).isEqualTo("https://acme.com/logo.png");
        assertThat(r.heroImageUrl()).isEqualTo("https://acme.com/hero.jpg");
        assertThat(r.contactLinks()).containsEntry("instagram", "https://instagram.com/acme");
        assertThat(r.overallConfidence()).isEqualTo(0.90);
        assertThat(r.warnings()).isEmpty();
    }

    @Test
    void parsesResponseWithNullOptionalFields() {
        String json = """
                {
                  "brandName": "Acme",
                  "brandNameConfidence": 0.80,
                  "tagline": "Minimal brand for modern times",
                  "taglineConfidence": 0.75,
                  "summary": "Acme creates minimal brand identities.",
                  "summaryConfidence": 0.70,
                  "toneKeywords": [],
                  "primaryColor": "#FF0000",
                  "secondaryColor": null,
                  "textColor": null,
                  "logoUrl": null,
                  "heroImageUrl": null,
                  "contactLinks": {},
                  "overallConfidence": 0.78,
                  "warnings": ["No social links found"]
                }
                """;

        AiExtractionResponse r = parser.parse(json);

        assertThat(r.secondaryColor()).isNull();
        assertThat(r.textColor()).isNull();
        assertThat(r.logoUrl()).isNull();
        assertThat(r.heroImageUrl()).isNull();
        assertThat(r.toneKeywords()).isEmpty();
        assertThat(r.warnings()).containsExactly("No social links found");
    }

    @Test
    void hexColoursNormalisedToUpperCase() {
        String json = minimalJson("\"primaryColor\": \"#aabbcc\"");

        AiExtractionResponse r = parser.parse(json);

        assertThat(r.primaryColor()).isEqualTo("#AABBCC");
    }

    @Test
    void toneKeywordsTrimmedToFiveMaximum() {
        String json = minimalJson(
                "\"primaryColor\": \"#FF0000\"",
                "\"toneKeywords\": [\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]");

        AiExtractionResponse r = parser.parse(json);

        assertThat(r.toneKeywords()).hasSize(5);
    }

    @Test
    void stripsMarkdownFencesBeforeParsing() {
        String json = "```json\n" + validJsonBody() + "\n```";

        AiExtractionResponse r = parser.parse(json);

        assertThat(r.brandName()).isEqualTo("Acme Studio");
    }

    @Test
    void stripsMarkdownFencesWithoutLanguageTag() {
        String json = "```\n" + validJsonBody() + "\n```";

        AiExtractionResponse r = parser.parse(json);

        assertThat(r.brandName()).isEqualTo("Acme Studio");
    }

    // -------------------------------------------------------------------------
    // Validation failures → MalformedAiResponseException
    // -------------------------------------------------------------------------

    @Test
    void throwsOnEmptyInput() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void throwsOnNullInput() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(MalformedAiResponseException.class);
    }

    @Test
    void throwsOnInvalidJson() {
        assertThatThrownBy(() -> parser.parse("{not valid json"))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void throwsWhenBrandNameMissing() {
        String json = validJsonWithout("brandName");
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("brandName");
    }

    @Test
    void throwsWhenTaglineMissing() {
        String json = validJsonWithout("tagline");
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("tagline");
    }

    @Test
    void throwsWhenPrimaryColorMissing() {
        String json = validJsonWithout("primaryColor");
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("primaryColor");
    }

    @Test
    void throwsWhenConfidenceOutOfRange() {
        String json = minimalJson("\"brandNameConfidence\": 1.5");
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("brandNameConfidence");
    }

    @Test
    void throwsWhenPrimaryColorNotHexFormat() {
        String json = minimalJson("\"primaryColor\": \"red\"");
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("primaryColor");
    }

    @Test
    void throwsWhenOverallConfidenceNegative() {
        String json = minimalJson("\"overallConfidence\": -0.1");
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(MalformedAiResponseException.class)
                .hasMessageContaining("overallConfidence");
    }

    // -------------------------------------------------------------------------
    // stripMarkdownFences helper
    // -------------------------------------------------------------------------

    @Test
    void stripMarkdownFences_noOpForPlainJson() {
        String plain = "{\"key\":\"value\"}";
        assertThat(VertexAiResponseParser.stripMarkdownFences(plain)).isEqualTo(plain);
    }

    @Test
    void stripMarkdownFences_removesJsonFence() {
        String fenced = "```json\n{\"key\":\"value\"}\n```";
        assertThat(VertexAiResponseParser.stripMarkdownFences(fenced)).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void stripMarkdownFences_removesPlainFence() {
        String fenced = "```\n{\"key\":\"value\"}\n```";
        assertThat(VertexAiResponseParser.stripMarkdownFences(fenced)).isEqualTo("{\"key\":\"value\"}");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns a valid JSON body string (no fences). */
    private static String validJsonBody() {
        return """
                {
                  "brandName": "Acme Studio",
                  "brandNameConfidence": 0.90,
                  "tagline": "Crafting brands",
                  "taglineConfidence": 0.85,
                  "summary": "A design studio.",
                  "summaryConfidence": 0.80,
                  "toneKeywords": ["bold"],
                  "primaryColor": "#1A2B3C",
                  "secondaryColor": null,
                  "textColor": null,
                  "logoUrl": null,
                  "heroImageUrl": null,
                  "contactLinks": {},
                  "overallConfidence": 0.88,
                  "warnings": []
                }""";
    }

    /**
     * Returns a minimal valid JSON with one or more fields overridden.
     * Each {@code override} must be a {@code "field": value} JSON fragment
     * that replaces the default value for that field.
     */
    private static String minimalJson(String... overrides) {
        // Build a base object and apply overrides by key replacement
        var base = new java.util.LinkedHashMap<String, Object>();
        base.put("brandName",           "Acme");
        base.put("brandNameConfidence", 0.80);
        base.put("tagline",             "A tagline long enough to pass validation here");
        base.put("taglineConfidence",   0.75);
        base.put("summary",             "A longer summary text that meets the minimum length.");
        base.put("summaryConfidence",   0.70);
        base.put("toneKeywords",        List.of());
        base.put("primaryColor",        "#FF0000");
        base.put("secondaryColor",      null);
        base.put("textColor",           null);
        base.put("logoUrl",             null);
        base.put("heroImageUrl",        null);
        base.put("contactLinks",        new java.util.HashMap<>());
        base.put("overallConfidence",   0.78);
        base.put("warnings",            List.of());

        try {
            ObjectMapper m = new ObjectMapper();
            // Start with base, then parse overrides to merge
            var node = (com.fasterxml.jackson.databind.node.ObjectNode) m.valueToTree(base);
            for (String override : overrides) {
                var overrideNode = (com.fasterxml.jackson.databind.node.ObjectNode)
                        m.readTree("{" + override + "}");
                overrideNode.fields().forEachRemaining(e -> node.set(e.getKey(), e.getValue()));
            }
            return m.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns a valid JSON string with the named field removed. */
    private static String validJsonWithout(String fieldToRemove) {
        try {
            ObjectMapper m = new ObjectMapper();
            var node = (com.fasterxml.jackson.databind.node.ObjectNode) m.readTree(validJsonBody());
            node.remove(fieldToRemove);
            return m.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
