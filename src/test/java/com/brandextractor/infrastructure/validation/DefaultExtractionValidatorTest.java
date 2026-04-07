package com.brandextractor.infrastructure.validation;

import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultExtractionValidatorTest {

    private DefaultExtractionValidator validator;

    @BeforeEach
    void setUp() { validator = new DefaultExtractionValidator(); }

    @Test
    void noIssuesForWellFormedResult() {
        var result = resultWith(
                brandProfile("Acme", "Tagline", "Summary"),
                colorSelection("#FF0000"),
                0.80);

        assertThat(validator.validate(result)).isEmpty();
    }

    @Test
    void errorWhenBrandNameIsNull() {
        var profile = new BrandProfile(null, null, null, List.of());
        var result  = resultWith(profile, colorSelection("#FF0000"), 0.80);

        var issues = validator.validate(result);

        assertThat(issues).anyMatch(i ->
                i.code().equals("MISSING_BRAND_NAME") &&
                i.severity() == ValidationIssue.Severity.ERROR);
    }

    @Test
    void errorWhenBrandNameValueIsBlank() {
        var profile = new BrandProfile(new Confident<>("  ", 0.9), null, null, List.of());
        var result  = resultWith(profile, colorSelection("#FF0000"), 0.80);

        var issues = validator.validate(result);

        assertThat(issues).anyMatch(i -> i.code().equals("MISSING_BRAND_NAME"));
    }

    @Test
    void warningWhenPrimaryColorIsNull() {
        var result = resultWith(
                brandProfile("Acme", null, null),
                new ColorSelection(null, null, null),
                0.80);

        var issues = validator.validate(result);

        assertThat(issues).anyMatch(i ->
                i.code().equals("MISSING_PRIMARY_COLOR") &&
                i.severity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    void warningWhenColorsIsNull() {
        var result = resultWith(brandProfile("Acme", null, null), null, 0.80);

        var issues = validator.validate(result);

        assertThat(issues).anyMatch(i -> i.code().equals("MISSING_PRIMARY_COLOR"));
    }

    @Test
    void warningWhenConfidenceBelowThreshold() {
        var result = resultWith(
                brandProfile("Acme", "Tagline", "Summary"),
                colorSelection("#FF0000"),
                0.20);

        var issues = validator.validate(result);

        assertThat(issues).anyMatch(i ->
                i.code().equals("LOW_CONFIDENCE") &&
                i.severity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    void noLowConfidenceWarningAtExactThreshold() {
        var result = resultWith(
                brandProfile("Acme", "Tagline", "Summary"),
                colorSelection("#FF0000"),
                0.30);

        var issues = validator.validate(result);

        assertThat(issues).noneMatch(i -> i.code().equals("LOW_CONFIDENCE"));
    }

    @Test
    void multipleIssuesReturnedWhenSeveralRulesFail() {
        var profile = new BrandProfile(new Confident<>("", 0.5), null, null, List.of());
        var result  = resultWith(profile, new ColorSelection(null, null, null), 0.10);

        var issues = validator.validate(result);

        assertThat(issues).hasSizeGreaterThanOrEqualTo(3); // brand, color, confidence
    }

    // -------------------------------------------------------------------------
    // INVALID_COLOR_FORMAT
    // -------------------------------------------------------------------------

    @Test
    void warningWhenPrimaryColorHasInvalidHexFormat() {
        var colors = new ColorSelection(new ColorValue("red", 1.0, List.of()), null, null);
        var result = resultWith(brandProfile("Acme", null, null), colors, 0.80);

        var issues = validator.validate(result);

        assertThat(issues).anyMatch(i ->
                i.code().equals("INVALID_COLOR_FORMAT") &&
                i.severity() == ValidationIssue.Severity.WARNING &&
                i.message().contains("red"));
    }

    @Test
    void warningWhenSecondaryColorHasInvalidHexFormat() {
        var colors = new ColorSelection(
                new ColorValue("#FF0000", 1.0, List.of()),
                new ColorValue("notahex", 1.0, List.of()),
                null);
        var result = resultWith(brandProfile("Acme", null, null), colors, 0.80);

        assertThat(validator.validate(result))
                .anyMatch(i -> i.code().equals("INVALID_COLOR_FORMAT") &&
                               i.message().contains("notahex"));
    }

    @Test
    void noColorFormatWarningForValidHex() {
        var result = resultWith(brandProfile("Acme", null, null), colorSelection("#1A2B3C"), 0.80);

        assertThat(validator.validate(result))
                .noneMatch(i -> i.code().equals("INVALID_COLOR_FORMAT"));
    }

    @Test
    void noColorFormatWarningForNullColor() {
        var result = resultWith(brandProfile("Acme", null, null),
                new ColorSelection(null, null, null), 0.80);

        assertThat(validator.validate(result))
                .noneMatch(i -> i.code().equals("INVALID_COLOR_FORMAT"));
    }

    // -------------------------------------------------------------------------
    // DUPLICATE_COLOR_ROLES
    // -------------------------------------------------------------------------

    @Test
    void warningWhenPrimaryAndSecondaryColorAreIdentical() {
        var colors = new ColorSelection(
                new ColorValue("#FF0000", 1.0, List.of()),
                new ColorValue("#FF0000", 1.0, List.of()),
                null);
        var result = resultWith(brandProfile("Acme", null, null), colors, 0.80);

        assertThat(validator.validate(result))
                .anyMatch(i -> i.code().equals("DUPLICATE_COLOR_ROLES") &&
                               i.severity() == ValidationIssue.Severity.WARNING);
    }

    @Test
    void warningWhenPrimaryAndTextColorAreIdentical() {
        var colors = new ColorSelection(
                new ColorValue("#FF0000", 1.0, List.of()),
                null,
                new ColorValue("#FF0000", 1.0, List.of()));
        var result = resultWith(brandProfile("Acme", null, null), colors, 0.80);

        assertThat(validator.validate(result))
                .anyMatch(i -> i.code().equals("DUPLICATE_COLOR_ROLES"));
    }

    @Test
    void noDuplicateWarningWhenAllColorsDistinct() {
        var colors = new ColorSelection(
                new ColorValue("#FF0000", 1.0, List.of()),
                new ColorValue("#00FF00", 1.0, List.of()),
                new ColorValue("#0000FF", 1.0, List.of()));
        var result = resultWith(brandProfile("Acme", null, null), colors, 0.80);

        assertThat(validator.validate(result))
                .noneMatch(i -> i.code().equals("DUPLICATE_COLOR_ROLES"));
    }

    @Test
    void noDuplicateWarningWhenSecondaryAndTextSameButPrimaryDifferent() {
        // rule only checks primary vs secondary and primary vs text
        var colors = new ColorSelection(
                new ColorValue("#FF0000", 1.0, List.of()),
                new ColorValue("#0000FF", 1.0, List.of()),
                new ColorValue("#0000FF", 1.0, List.of()));
        var result = resultWith(brandProfile("Acme", null, null), colors, 0.80);

        assertThat(validator.validate(result))
                .noneMatch(i -> i.code().equals("DUPLICATE_COLOR_ROLES"));
    }

    @Test
    void duplicateCheckIsCaseInsensitive() {
        var colors = new ColorSelection(
                new ColorValue("#ff0000", 1.0, List.of()),
                new ColorValue("#FF0000", 1.0, List.of()),
                null);
        var result = resultWith(brandProfile("Acme", null, null), colors, 0.80);

        assertThat(validator.validate(result))
                .anyMatch(i -> i.code().equals("DUPLICATE_COLOR_ROLES"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static BrandProfile brandProfile(String name, String tagline, String summary) {
        return new BrandProfile(
                name != null    ? new Confident<>(name, 0.9)    : null,
                tagline != null ? new Confident<>(tagline, 0.8)  : null,
                summary != null ? new Confident<>(summary, 0.75) : null,
                List.of());
    }

    private static ColorSelection colorSelection(String hex) {
        return new ColorSelection(new ColorValue(hex, 1.0, List.of()), null, null);
    }

    private static ExtractionResult resultWith(BrandProfile profile, ColorSelection colors, double confidence) {
        return new ExtractionResult(
                UUID.randomUUID(),
                ExtractionInputType.URL, "https://acme.com", "https://acme.com/",
                profile, colors, null, null,
                new ConfidenceScore(confidence),
                List.of(), List.of(),
                1, 0, 0, false,
                List.of(new WebsiteEvidence(
                        "w-1", "WEBSITE", "https://acme.com", "https://acme.com/",
                        null, null, "", List.of(), null, List.of(), List.of(), List.of(),
                        null, null, null, null, null, null, 1.0, Instant.now())));
    }
}
