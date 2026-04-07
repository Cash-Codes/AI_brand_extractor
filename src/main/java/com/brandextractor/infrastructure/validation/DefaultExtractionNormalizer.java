package com.brandextractor.infrastructure.validation;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.*;
import com.brandextractor.domain.ports.ExtractionNormalizationPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Deterministic post-AI normaliser that cleans up the raw {@link ExtractionResult}
 * before validation and response mapping.
 *
 * <h2>Rules applied in order</h2>
 * <ol>
 *   <li><b>Text normalisation</b> — trim whitespace; null-out blank optionals;
 *       truncate values that exceed maximum character lengths.</li>
 *   <li><b>Colour validity</b> — values that do not match {@code #RRGGBB} are
 *       cleared and a warning is appended.</li>
 *   <li><b>Asset URL safety</b> — URLs with non-{@code http(s)} schemes (e.g.
 *       {@code data:}, {@code javascript:}) are removed.</li>
 *   <li><b>AI / evidence conflict</b> — when the extracted brand name differs
 *       substantially from the strongest evidence signal ({@code og:site_name}
 *       then first heading), an {@link ExtractionWarning} is appended and the
 *       overall confidence is reduced by {@value #CONFLICT_PENALTY}.</li>
 *   <li><b>Confidence bounding</b> — the overall confidence score is clamped to
 *       {@code [0.0, 1.0]} after any penalty has been applied.</li>
 * </ol>
 */
@Component
public class DefaultExtractionNormalizer implements ExtractionNormalizationPort {

    static final int    MAX_BRAND_NAME_LENGTH = 100;
    static final int    MAX_TAGLINE_LENGTH    = 200;
    static final int    MAX_SUMMARY_LENGTH    = 2000;
    static final double CONFLICT_PENALTY      = 0.15;

    private static final Pattern HEX_COLOR =
            Pattern.compile("^#[0-9A-Fa-f]{6}$");

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    @Override
    public ExtractionResult normalize(ExtractionResult result) {
        List<ExtractionWarning> warnings = new ArrayList<>(
                result.warnings() != null ? result.warnings() : List.of());

        BrandProfile   profile = normalizeBrandProfile(result.brandProfile(), warnings);
        ColorSelection colors  = normalizeColors(result.colors(), warnings);
        AssetSelection assets  = sanitizeAssets(result.assets(), warnings);

        double confidence = result.confidence() != null ? result.confidence().overall() : 0.0;
        confidence = detectConflict(profile, result.evidence(), confidence, warnings);
        confidence = Math.max(0.0, Math.min(1.0, confidence));

        return new ExtractionResult(
                result.requestId(),
                result.inputType(),
                result.originalSource(),
                result.resolvedSource(),
                profile, colors, assets,
                result.links(),
                new ConfidenceScore(confidence),
                List.copyOf(warnings),
                result.validationIssues(),   // filled later by ExtractionValidationPort
                result.textEvidenceCount(),
                result.imageEvidenceCount(),
                result.ocrBlockCount(),
                result.usedScreenshot(),
                result.evidence());
    }

    // -------------------------------------------------------------------------
    // Brand profile
    // -------------------------------------------------------------------------

    private static BrandProfile normalizeBrandProfile(BrandProfile profile,
                                                       List<ExtractionWarning> warnings) {
        if (profile == null) return null;
        return new BrandProfile(
                normalizeTextField(profile.brandName(), "Brand name",
                        MAX_BRAND_NAME_LENGTH, warnings),
                normalizeTextField(profile.tagline(), "Tagline",
                        MAX_TAGLINE_LENGTH, warnings),
                normalizeTextField(profile.summary(), "Summary",
                        MAX_SUMMARY_LENGTH, warnings),
                profile.toneKeywords() != null ? profile.toneKeywords() : List.of());
    }

    /**
     * Trims and caps a {@code Confident<String>} field.
     *
     * <ul>
     *   <li>If the field or its value is {@code null} → returns {@code null}.</li>
     *   <li>If the trimmed value is blank → returns {@code null}.</li>
     *   <li>If the trimmed value exceeds {@code maxLength} → truncates and adds a
     *       warning.</li>
     * </ul>
     */
    private static Confident<String> normalizeTextField(Confident<String> field,
                                                         String label,
                                                         int maxLength,
                                                         List<ExtractionWarning> warnings) {
        if (field == null || field.value() == null) return null;
        String value = field.value().trim();
        if (value.isBlank()) return null;
        if (value.length() > maxLength) {
            warnings.add(new ExtractionWarning(
                    label + " was truncated from " + field.value().length() +
                    " to " + maxLength + " characters."));
            value = value.substring(0, maxLength);
        }
        return new Confident<>(value, field.confidence());
    }

    // -------------------------------------------------------------------------
    // Colours
    // -------------------------------------------------------------------------

    private static ColorSelection normalizeColors(ColorSelection colors,
                                                   List<ExtractionWarning> warnings) {
        if (colors == null) return null;
        return new ColorSelection(
                normalizeColor(colors.primary(),   "Primary colour",   warnings),
                normalizeColor(colors.secondary(), "Secondary colour", warnings),
                normalizeColor(colors.text(),      "Text colour",      warnings));
    }

    /**
     * Validates hex format; normalises case to upper. Nulls the field and adds a
     * warning when the format is invalid.
     */
    private static ColorValue normalizeColor(ColorValue color, String label,
                                              List<ExtractionWarning> warnings) {
        if (color == null) return null;
        String raw = color.value();
        if (raw == null || !HEX_COLOR.matcher(raw).matches()) {
            warnings.add(new ExtractionWarning(
                    label + " value '" + raw +
                    "' is not a valid #RRGGBB hex colour and was removed."));
            return null;
        }
        String upper = raw.toUpperCase(Locale.ROOT);
        return upper.equals(raw)
                ? color
                : new ColorValue(upper, color.confidence(), color.evidenceRefs());
    }

    // -------------------------------------------------------------------------
    // Asset URLs
    // -------------------------------------------------------------------------

    private static AssetSelection sanitizeAssets(AssetSelection assets,
                                                  List<ExtractionWarning> warnings) {
        if (assets == null) return null;
        return new AssetSelection(
                sanitizeAssetList(assets.logos(),      warnings),
                sanitizeAssetList(assets.heroImages(), warnings));
    }

    private static List<AssetItem> sanitizeAssetList(List<AssetItem> items,
                                                      List<ExtractionWarning> warnings) {
        if (items == null || items.isEmpty()) return List.of();
        List<AssetItem> safe = new ArrayList<>();
        for (AssetItem item : items) {
            if (isSafeUrl(item.url())) {
                safe.add(item);
            } else {
                warnings.add(new ExtractionWarning(
                        "Asset URL '" + item.url() +
                        "' was removed because it uses an unsafe scheme."));
            }
        }
        return List.copyOf(safe);
    }

    private static boolean isSafeUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    // -------------------------------------------------------------------------
    // AI / evidence conflict detection
    // -------------------------------------------------------------------------

    /**
     * Compares the AI-extracted brand name against the strongest evidence signal
     * available in the result's evidence list.
     *
     * <p>A <em>conflict</em> is declared when neither name is a substring of the
     * other (case-insensitive). On conflict a warning is added and
     * {@value #CONFLICT_PENALTY} is subtracted from the confidence score.
     */
    private static double detectConflict(BrandProfile profile,
                                          List<Evidence> evidence,
                                          double confidence,
                                          List<ExtractionWarning> warnings) {
        if (profile == null || profile.brandName() == null) return confidence;
        String aiName = profile.brandName().value();
        if (aiName == null || aiName.isBlank()) return confidence;

        String evidenceName = bestEvidenceBrandName(evidence);
        if (evidenceName == null) return confidence;

        String normAi  = aiName.toLowerCase(Locale.ROOT).strip();
        String normEvi = evidenceName.toLowerCase(Locale.ROOT).strip();

        if (!normAi.contains(normEvi) && !normEvi.contains(normAi)) {
            warnings.add(new ExtractionWarning(
                    "Extracted brand name '" + aiName +
                    "' may conflict with the site name '" + evidenceName +
                    "' found in evidence; confidence reduced."));
            return confidence - CONFLICT_PENALTY;
        }
        return confidence;
    }

    /**
     * Returns the best available brand name from evidence:
     * {@code og:site_name} first, then the first heading, then {@code null}.
     */
    private static String bestEvidenceBrandName(List<Evidence> evidence) {
        if (evidence == null) return null;
        return evidence.stream()
                .filter(WebsiteEvidence.class::isInstance)
                .map(WebsiteEvidence.class::cast)
                .map(w -> {
                    if (w.ogSiteName() != null && !w.ogSiteName().isBlank())
                        return w.ogSiteName().trim();
                    if (w.headings() != null && !w.headings().isEmpty())
                        return w.headings().get(0).trim();
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse(null);
    }
}
