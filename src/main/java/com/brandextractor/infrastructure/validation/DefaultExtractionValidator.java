package com.brandextractor.infrastructure.validation;

import com.brandextractor.domain.model.*;
import com.brandextractor.domain.model.ValidationIssue.Severity;
import com.brandextractor.domain.ports.ExtractionValidationPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates a normalised {@link ExtractionResult} against structural business rules.
 *
 * <p>This validator is intended to run <em>after</em> {@link DefaultExtractionNormalizer}
 * so that it operates on clean, trimmed values. Rules:
 * <ul>
 *   <li>{@code MISSING_BRAND_NAME} (ERROR) — brand name absent or blank.</li>
 *   <li>{@code MISSING_PRIMARY_COLOR} (WARNING) — primary colour is {@code null}.</li>
 *   <li>{@code LOW_CONFIDENCE} (WARNING) — overall confidence below
 *       {@value #MIN_CONFIDENCE}.</li>
 *   <li>{@code INVALID_COLOR_FORMAT} (WARNING) — a non-null colour value does not
 *       match {@code #RRGGBB}. Belt-and-suspenders check; normally cleared by the
 *       normaliser first.</li>
 *   <li>{@code DUPLICATE_COLOR_ROLES} (WARNING) — two distinct colour roles share
 *       the same hex value.</li>
 * </ul>
 */
@Component
public class DefaultExtractionValidator implements ExtractionValidationPort {

    static final double MIN_CONFIDENCE = 0.30;

    private static final Pattern HEX_COLOR =
            Pattern.compile("^#[0-9A-Fa-f]{6}$");

    @Override
    public List<ValidationIssue> validate(ExtractionResult result) {
        List<ValidationIssue> issues = new ArrayList<>();

        checkBrandName(result, issues);
        checkPrimaryColor(result, issues);
        checkConfidence(result, issues);
        checkColorFormats(result, issues);
        checkDistinctColorRoles(result, issues);

        return List.copyOf(issues);
    }

    // -------------------------------------------------------------------------

    private static void checkBrandName(ExtractionResult result,
                                        List<ValidationIssue> issues) {
        var brandName = result.brandProfile() != null
                ? result.brandProfile().brandName() : null;
        if (brandName == null || brandName.value() == null || brandName.value().isBlank()) {
            issues.add(new ValidationIssue(
                    "MISSING_BRAND_NAME",
                    "Brand name could not be extracted from the provided input.",
                    Severity.ERROR));
        }
    }

    private static void checkPrimaryColor(ExtractionResult result,
                                           List<ValidationIssue> issues) {
        if (result.colors() == null || result.colors().primary() == null) {
            issues.add(new ValidationIssue(
                    "MISSING_PRIMARY_COLOR",
                    "Primary brand colour could not be identified.",
                    Severity.WARNING));
        }
    }

    private static void checkConfidence(ExtractionResult result,
                                         List<ValidationIssue> issues) {
        if (result.confidence() != null && result.confidence().overall() < MIN_CONFIDENCE) {
            issues.add(new ValidationIssue(
                    "LOW_CONFIDENCE",
                    "Overall extraction confidence (%.0f%%) is below the acceptable threshold (%.0f%%)."
                            .formatted(
                                    result.confidence().overall() * 100,
                                    MIN_CONFIDENCE * 100),
                    Severity.WARNING));
        }
    }

    private static void checkColorFormats(ExtractionResult result,
                                           List<ValidationIssue> issues) {
        if (result.colors() == null) return;
        checkColorFormat(result.colors().primary(),   "Primary colour",   issues);
        checkColorFormat(result.colors().secondary(), "Secondary colour", issues);
        checkColorFormat(result.colors().text(),      "Text colour",      issues);
    }

    private static void checkColorFormat(ColorValue color, String label,
                                          List<ValidationIssue> issues) {
        if (color == null) return;
        if (color.value() == null || !HEX_COLOR.matcher(color.value()).matches()) {
            issues.add(new ValidationIssue(
                    "INVALID_COLOR_FORMAT",
                    label + " value '" + color.value() +
                    "' is not a valid #RRGGBB hex colour.",
                    Severity.WARNING));
        }
    }

    private static void checkDistinctColorRoles(ExtractionResult result,
                                                  List<ValidationIssue> issues) {
        if (result.colors() == null) return;
        String primary   = hexOf(result.colors().primary());
        String secondary = hexOf(result.colors().secondary());
        String text      = hexOf(result.colors().text());

        if (primary != null && primary.equalsIgnoreCase(secondary)) {
            issues.add(new ValidationIssue(
                    "DUPLICATE_COLOR_ROLES",
                    "Primary and secondary colours are identical (" + primary + ").",
                    Severity.WARNING));
        }
        if (primary != null && primary.equalsIgnoreCase(text)) {
            issues.add(new ValidationIssue(
                    "DUPLICATE_COLOR_ROLES",
                    "Primary and text colours are identical (" + primary + ").",
                    Severity.WARNING));
        }
    }

    private static String hexOf(ColorValue color) {
        return color != null ? color.value() : null;
    }
}
