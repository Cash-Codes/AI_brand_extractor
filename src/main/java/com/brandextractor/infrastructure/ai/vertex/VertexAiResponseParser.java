package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.infrastructure.ai.client.AiExtractionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses and validates a raw JSON string from Vertex AI into a typed
 * {@link AiExtractionResponse}.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Required fields must be present and non-null: {@code brandName}, {@code tagline},
 *       {@code summary}, {@code primaryColor}, {@code overallConfidence}.</li>
 *   <li>All confidence values must be in {@code [0.0, 1.0]}.</li>
 *   <li>Hex colour fields (when present) must match {@code #[0-9A-Fa-f]{6}}.</li>
 * </ul>
 *
 * <p>Throws {@link MalformedAiResponseException} on any validation failure so that
 * {@link VertexAiExtractionClient} can retry.
 */
@Component
public class VertexAiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(VertexAiResponseParser.class);
    private static final java.util.regex.Pattern HEX_COLOR =
            java.util.regex.Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final ObjectMapper objectMapper;

    public VertexAiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses {@code rawJson} into an {@link AiExtractionResponse}.
     *
     * @throws MalformedAiResponseException if JSON is invalid or schema constraints are violated
     */
    public AiExtractionResponse parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new MalformedAiResponseException("AI returned an empty response");
        }

        JsonNode root;
        try {
            // Strip accidental markdown fences the model may have added despite instructions
            String json = stripMarkdownFences(rawJson);
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new MalformedAiResponseException("AI response is not valid JSON: " + e.getMessage(), e);
        }

        String brandName         = requireString(root, "brandName");
        double brandNameConf     = requireConfidence(root, "brandNameConfidence");
        String tagline           = requireString(root, "tagline");
        double taglineConf       = requireConfidence(root, "taglineConfidence");
        String summary           = requireString(root, "summary");
        double summaryConf       = requireConfidence(root, "summaryConfidence");
        List<String> toneKeywords = parseStringArray(root, "toneKeywords");
        String primaryColor      = optionalHexColor(root, "primaryColor");
        String secondaryColor    = optionalHexColor(root, "secondaryColor");
        String textColor         = optionalHexColor(root, "textColor");
        String logoUrl           = optionalString(root, "logoUrl");
        String heroImageUrl      = optionalString(root, "heroImageUrl");
        Map<String, String> links = parseContactLinks(root);
        double overallConf       = requireConfidence(root, "overallConfidence");
        List<String> warnings    = parseStringArray(root, "warnings");

        if (toneKeywords.size() > 5) {
            log.warn("Model returned {} toneKeywords; trimming to 5", toneKeywords.size());
            toneKeywords = toneKeywords.subList(0, 5);
        }

        return new AiExtractionResponse(
                brandName, brandNameConf,
                tagline, taglineConf,
                summary, summaryConf,
                toneKeywords,
                primaryColor, secondaryColor, textColor,
                logoUrl, heroImageUrl,
                links, overallConf, warnings);
    }

    // -------------------------------------------------------------------------
    // Field extractors
    // -------------------------------------------------------------------------

    private static String requireString(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new MalformedAiResponseException("Required field '" + field + "' is missing or blank");
        }
        return node.asText().strip();
    }

    private static double requireConfidence(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isNumber()) {
            throw new MalformedAiResponseException("Required numeric field '" + field + "' is missing");
        }
        double v = node.asDouble();
        if (v < 0.0 || v > 1.0) {
            throw new MalformedAiResponseException(
                    "Confidence field '" + field + "' value " + v + " is outside [0.0, 1.0]");
        }
        return v;
    }

    private static String optionalHexColor(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual()) return null;
        String hex = node.asText().strip().toUpperCase();
        if (hex.isBlank() || hex.equals("NULL") || hex.equals("NONE")
                || hex.equals("N/A") || hex.equals("UNKNOWN")) return null;
        if (!HEX_COLOR.matcher(hex).matches()) {
            log.warn("Field '{}' value '{}' is not valid #RRGGBB — ignoring", field, hex);
            return null;
        }
        return hex;
    }

    private static String optionalString(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual()) return null;
        String v = node.asText().strip();
        return v.isBlank() ? null : v;
    }

    private static List<String> parseStringArray(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().strip());
            }
        }
        return result;
    }

    private static Map<String, String> parseContactLinks(JsonNode root) {
        JsonNode node = root.get("contactLinks");
        if (node == null || node.isNull() || !node.isObject()) return Map.of();
        Map<String, String> links = new HashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual() && !entry.getValue().asText().isBlank()) {
                links.put(entry.getKey(), entry.getValue().asText().strip());
            }
        });
        return links;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Strips optional markdown code fences ({@code ```json ... ```} or {@code ``` ... ```})
     * that Gemini sometimes adds despite being told not to.
     */
    static String stripMarkdownFences(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) trimmed = trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3).stripTrailing();
        }
        return trimmed;
    }
}
