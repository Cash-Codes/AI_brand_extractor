package com.brandextractor.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    void extractionResult_canBeConstructed() {
        var brandProfile = new BrandProfile("Acme", "Bold digital", "A studio.", List.of("modern"));
        var colorValue   = new ColorValue("#0F172A", 0.93, List.of("color-1"));
        var colors       = new ColorSelection(colorValue, colorValue, colorValue);
        var assetItem    = new AssetItem("https://example.com/logo.svg", AssetRole.PRIMARY_LOGO,
                                         0.94, 280, 96, "image/svg+xml", List.of("img-3"));
        var assets       = new AssetSelection(List.of(assetItem), List.of());
        var links        = new ContactLinks("https://example.com", null, null, null);
        var confidence   = new ConfidenceScore(0.88);
        var warning      = new ExtractionWarning("No secondary color found.");
        var issue        = new ValidationIssue("MISSING_LOGO", "No logo detected.", ValidationIssue.Severity.WARNING);

        var result = new ExtractionResult(
                UUID.randomUUID(), ExtractionInputType.URL,
                "https://example.com", "https://example.com/",
                brandProfile, colors, assets, links, confidence,
                List.of(warning), List.of(issue), 8, 12, 0, true);

        assertThat(result.inputType()).isEqualTo(ExtractionInputType.URL);
        assertThat(result.brandProfile().brandName()).isEqualTo("Acme");
        assertThat(result.colors().primary().value()).isEqualTo("#0F172A");
        assertThat(result.assets().logos()).hasSize(1);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.validationIssues().get(0).severity()).isEqualTo(ValidationIssue.Severity.WARNING);
    }
}
