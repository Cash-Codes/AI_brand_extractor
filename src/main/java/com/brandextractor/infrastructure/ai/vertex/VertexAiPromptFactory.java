package com.brandextractor.infrastructure.ai.vertex;

import com.brandextractor.domain.candidate.*;
import com.brandextractor.domain.evidence.*;
import com.brandextractor.infrastructure.ai.client.AiExtractionRequest;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the prompt payload sent to Vertex AI Gemini for brand extraction.
 *
 * <p>Two outputs:
 * <ul>
 *   <li>{@link #buildSystemInstruction()} — plain text that primes Gemini to act as a
 *       structured brand extraction engine and always return valid JSON.</li>
 *   <li>{@link #buildUserContent(AiExtractionRequest)} — the structured evidence digest
 *       appended to each user turn.</li>
 *   <li>{@link #buildGenerationConfig(VertexAiExtractionProperties)} — enforces
 *       {@code application/json} output and the response schema via Gemini's
 *       controlled generation feature.</li>
 * </ul>
 */
@Component
public class VertexAiPromptFactory {

    // -------------------------------------------------------------------------
    // System instruction
    // -------------------------------------------------------------------------

    public String buildSystemInstruction() {
        return """
                You are a brand extraction engine. Your task is to analyse the provided brand \
                evidence and candidate signals, and return a strictly structured JSON object that \
                conforms exactly to the response schema.

                Rules:
                - Return ONLY valid JSON. Do not add explanations, markdown fences, or any text \
                  outside the JSON object.
                - All confidence values must be numbers between 0.0 and 1.0.
                - Hex colours must be in the format #RRGGBB (6 digits, uppercase).
                - If a field has no reliable signal, use null rather than guessing.
                - toneKeywords must contain between 0 and 5 items.
                - Prefer signals with higher candidate scores when resolving conflicts.
                """;
    }

    // -------------------------------------------------------------------------
    // User content
    // -------------------------------------------------------------------------

    public String buildUserContent(AiExtractionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Evidence\n\n");
        appendEvidence(sb, request.evidence());

        sb.append("\n## Brand-name candidates (ranked)\n\n");
        appendBrandNames(sb, request.brandNameCandidates());

        sb.append("\n## Tagline candidates (ranked)\n\n");
        appendTaglines(sb, request.taglineCandidates());

        sb.append("\n## Summary candidates (ranked)\n\n");
        appendSummaries(sb, request.summaryCandidates());

        sb.append("\n## Colour candidates (ranked)\n\n");
        appendColors(sb, request.colorCandidates());

        sb.append("\n## Asset candidates (ranked)\n\n");
        appendAssets(sb, request.assetCandidates());

        sb.append("\n## Link candidates (ranked)\n\n");
        appendLinks(sb, request.linkCandidates());

        sb.append("""

                ## Task
                Using the evidence and candidates above, return a single JSON object that selects \
                the best brand name, tagline, summary, colours, logo, hero image, and contact links. \
                Include a confidence score for each selection and an overall confidence score.
                """);

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Generation config with controlled-generation schema
    // -------------------------------------------------------------------------

    public GenerationConfig buildGenerationConfig(VertexAiExtractionProperties props) {
        return GenerationConfig.newBuilder()
                .setTemperature(props.getTemperature())
                .setMaxOutputTokens(props.getMaxOutputTokens())
                .setResponseMimeType("application/json")
                .setResponseSchema(buildResponseSchema())
                .build();
    }

    // -------------------------------------------------------------------------
    // Response schema
    // -------------------------------------------------------------------------

    Schema buildResponseSchema() {
        return Schema.newBuilder()
                .setType(Type.OBJECT)
                .putProperties("brandName",           stringSchema("Chosen brand name"))
                .putProperties("brandNameConfidence", numberSchema("Confidence for brand name [0,1]"))
                .putProperties("tagline",             stringSchema("Chosen tagline"))
                .putProperties("taglineConfidence",   numberSchema("Confidence for tagline [0,1]"))
                .putProperties("summary",             stringSchema("One-paragraph brand summary"))
                .putProperties("summaryConfidence",   numberSchema("Confidence for summary [0,1]"))
                .putProperties("toneKeywords",        stringArraySchema("Up to 5 brand tone/personality keywords"))
                .putProperties("primaryColor",        stringSchema("Primary brand colour as #RRGGBB"))
                .putProperties("secondaryColor",      stringSchema("Secondary brand colour as #RRGGBB or null"))
                .putProperties("textColor",           stringSchema("Primary text colour as #RRGGBB or null"))
                .putProperties("logoUrl",             stringSchema("URL of the best logo candidate or null"))
                .putProperties("heroImageUrl",        stringSchema("URL of the best hero image or null"))
                .putProperties("contactLinks",        contactLinksSchema())
                .putProperties("overallConfidence",   numberSchema("Overall extraction confidence [0,1]"))
                .putProperties("warnings",            stringArraySchema("Any extraction warnings"))
                .addRequired("brandName")
                .addRequired("brandNameConfidence")
                .addRequired("tagline")
                .addRequired("taglineConfidence")
                .addRequired("summary")
                .addRequired("summaryConfidence")
                .addRequired("toneKeywords")
                .addRequired("primaryColor")
                .addRequired("overallConfidence")
                .addRequired("warnings")
                .build();
    }

    // -------------------------------------------------------------------------
    // Evidence formatting
    // -------------------------------------------------------------------------

    private void appendEvidence(StringBuilder sb, List<Evidence> evidence) {
        for (Evidence e : evidence) {
            switch (e) {
                case WebsiteEvidence w -> {
                    sb.append("### Website: ").append(w.resolvedUrl()).append("\n");
                    append(sb, "Title", w.title());
                    append(sb, "Meta description", w.metaDescription());
                    append(sb, "OG site name", w.ogSiteName());
                    append(sb, "OG description", w.ogDescription());
                    if (!w.headings().isEmpty())
                        sb.append("Headings: ").append(String.join(" | ", w.headings())).append("\n");
                    if (w.visibleText() != null && !w.visibleText().isBlank()) {
                        String excerpt = w.visibleText().length() > 500
                                ? w.visibleText().substring(0, 500) + "…"
                                : w.visibleText();
                        sb.append("Visible text: ").append(excerpt).append("\n");
                    }
                }
                case OcrEvidence o -> {
                    sb.append("### OCR from: ").append(o.sourceReference()).append("\n");
                    o.blocks().forEach(b -> sb.append("  - ").append(b.text())
                            .append(" (conf=").append(String.format("%.2f", b.confidence())).append(")\n"));
                }
                case FlyerEvidence f -> {
                    sb.append("### Flyer: ").append(f.sourceReference())
                      .append(" (").append(f.width()).append("×").append(f.height()).append(")\n");
                    sb.append("Dominant colours: ")
                      .append(String.join(", ", f.dominantColors())).append("\n");
                }
                default -> { }
            }
        }
    }

    private void appendBrandNames(StringBuilder sb, List<BrandNameCandidate> candidates) {
        candidates.forEach(c -> sb.append(String.format("  %.2f  \"%s\"  — %s  [%s]\n",
                c.score(), c.value(), c.rationale(), String.join(",", c.evidenceRefs()))));
    }

    private void appendTaglines(StringBuilder sb, List<TaglineCandidate> candidates) {
        candidates.forEach(c -> sb.append(String.format("  %.2f  \"%s\"  — %s\n",
                c.score(), c.value(), c.rationale())));
    }

    private void appendSummaries(StringBuilder sb, List<SummaryCandidate> candidates) {
        candidates.stream().limit(3).forEach(c -> sb.append(String.format("  %.2f  %s\n",
                c.score(), c.value().length() > 300 ? c.value().substring(0, 300) + "…" : c.value())));
    }

    private void appendColors(StringBuilder sb, List<ColorCandidate> candidates) {
        candidates.stream().limit(10).forEach(c -> sb.append(String.format("  %.2f  %s  — %s\n",
                c.score(), c.hex(), c.rationale())));
    }

    private void appendAssets(StringBuilder sb, List<AssetCandidate> candidates) {
        candidates.stream().limit(10).forEach(c -> sb.append(String.format("  %.2f  [%s]  %s  — %s\n",
                c.score(), c.role(), c.url(), c.rationale())));
    }

    private void appendLinks(StringBuilder sb, List<LinkCandidate> candidates) {
        candidates.forEach(c -> sb.append(String.format("  %.2f  %s  %s\n",
                c.score(), c.platform(), c.href())));
    }

    // -------------------------------------------------------------------------
    // Schema helpers
    // -------------------------------------------------------------------------

    private static Schema stringSchema(String description) {
        return Schema.newBuilder().setType(Type.STRING).setDescription(description).build();
    }

    private static Schema numberSchema(String description) {
        return Schema.newBuilder().setType(Type.NUMBER).setDescription(description).build();
    }

    private static Schema stringArraySchema(String description) {
        return Schema.newBuilder()
                .setType(Type.ARRAY)
                .setDescription(description)
                .setItems(Schema.newBuilder().setType(Type.STRING).build())
                .build();
    }

    private static Schema contactLinksSchema() {
        return Schema.newBuilder()
                .setType(Type.OBJECT)
                .setDescription("Platform → URL map for contact/social links")
                .build();
    }

    // -------------------------------------------------------------------------

    private static void append(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) sb.append(label).append(": ").append(value).append("\n");
    }
}
