package com.brandextractor.infrastructure.validation;

import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.brandextractor.infrastructure.validation.DefaultExtractionNormalizer.*;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultExtractionNormalizerTest {

    private DefaultExtractionNormalizer normalizer;

    @BeforeEach
    void setUp() { normalizer = new DefaultExtractionNormalizer(); }

    // =========================================================================
    // Text normalisation — brand name
    // =========================================================================

    @Nested
    class BrandName {

        @Test
        void trimmedLeadingAndTrailingWhitespace() {
            var result = resultWithBrandName("  Acme  ");
            assertThat(normalizer.normalize(result).brandProfile().brandName().value())
                    .isEqualTo("Acme");
        }

        @Test
        void blankValueBecomesNull() {
            var result = resultWithBrandName("   ");
            assertThat(normalizer.normalize(result).brandProfile().brandName()).isNull();
        }

        @Test
        void nullFieldStaysNull() {
            var profile = new BrandProfile(null, null, null, List.of());
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().brandName()).isNull();
        }

        @Test
        void valueWithinLimitUnchanged() {
            String name = "A".repeat(MAX_BRAND_NAME_LENGTH);
            var result = resultWithBrandName(name);
            assertThat(normalizer.normalize(result).brandProfile().brandName().value())
                    .isEqualTo(name);
        }

        @Test
        void valueExceedingLimitTruncated() {
            String long_ = "B".repeat(MAX_BRAND_NAME_LENGTH + 10);
            var result = resultWithBrandName(long_);
            var normalized = normalizer.normalize(result);
            assertThat(normalized.brandProfile().brandName().value())
                    .hasSize(MAX_BRAND_NAME_LENGTH);
        }

        @Test
        void truncationAddsWarning() {
            String long_ = "C".repeat(MAX_BRAND_NAME_LENGTH + 5);
            var result = resultWithBrandName(long_);
            assertThat(normalizer.normalize(result).warnings())
                    .anyMatch(w -> w.message().contains("Brand name") &&
                                   w.message().contains("truncated"));
        }

        @Test
        void confidencePreservedAfterTrim() {
            var field  = new Confident<>("  Acme  ", 0.87);
            var profile = new BrandProfile(field, null, null, List.of());
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().brandName().confidence())
                    .isEqualTo(0.87);
        }
    }

    // =========================================================================
    // Text normalisation — tagline
    // =========================================================================

    @Nested
    class Tagline {

        @Test
        void blankTaglineBecomesNull() {
            var profile = profileWith("Acme", "  ", null);
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().tagline()).isNull();
        }

        @Test
        void taglineExceedingLimitTruncated() {
            String long_ = "T".repeat(MAX_TAGLINE_LENGTH + 1);
            var profile = profileWith("Acme", long_, null);
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().tagline().value())
                    .hasSize(MAX_TAGLINE_LENGTH);
        }

        @Test
        void taglineTruncationAddsWarning() {
            var profile = profileWith("Acme", "T".repeat(MAX_TAGLINE_LENGTH + 1), null);
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).warnings())
                    .anyMatch(w -> w.message().contains("Tagline") &&
                                   w.message().contains("truncated"));
        }
    }

    // =========================================================================
    // Text normalisation — summary
    // =========================================================================

    @Nested
    class Summary {

        @Test
        void blankSummaryBecomesNull() {
            var profile = profileWith("Acme", "A tagline", "  ");
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().summary()).isNull();
        }

        @Test
        void summaryExceedingLimitTruncated() {
            String long_ = "S".repeat(MAX_SUMMARY_LENGTH + 1);
            var profile = profileWith("Acme", null, long_);
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().summary().value())
                    .hasSize(MAX_SUMMARY_LENGTH);
        }
    }

    // =========================================================================
    // Colour validity
    // =========================================================================

    @Nested
    class ColorValidity {

        @Test
        void validHexPassesThrough() {
            var result = baseResult(validProfile(), colorsWith("#1A2B3C", null, null), List.of());
            assertThat(normalizer.normalize(result).colors().primary().value())
                    .isEqualTo("#1A2B3C");
        }

        @Test
        void lowercaseHexNormalisedToUpperCase() {
            var result = baseResult(validProfile(), colorsWith("#aabbcc", null, null), List.of());
            assertThat(normalizer.normalize(result).colors().primary().value())
                    .isEqualTo("#AABBCC");
        }

        @Test
        void alreadyUpperCaseReturnsSameObject() {
            var primary = new ColorValue("#FF0000", 0.9, List.of());
            var colors  = new ColorSelection(primary, null, null);
            var result  = baseResult(validProfile(), colors, List.of());
            // same instance — no copy needed when already uppercase
            assertThat(normalizer.normalize(result).colors().primary())
                    .isSameAs(primary);
        }

        @Test
        void invalidHexBecomesNull() {
            var result = baseResult(validProfile(), colorsWith("red", null, null), List.of());
            assertThat(normalizer.normalize(result).colors().primary()).isNull();
        }

        @Test
        void invalidHexAddsWarning() {
            var result = baseResult(validProfile(), colorsWith("red", null, null), List.of());
            assertThat(normalizer.normalize(result).warnings())
                    .anyMatch(w -> w.message().contains("Primary colour") &&
                                   w.message().contains("red") &&
                                   w.message().contains("removed"));
        }

        @Test
        void invalidSecondaryColorBecomesNull() {
            var result = baseResult(validProfile(), colorsWith("#FF0000", "notahex", null), List.of());
            assertThat(normalizer.normalize(result).colors().secondary()).isNull();
        }

        @Test
        void invalidTextColorBecomesNull() {
            var result = baseResult(validProfile(), colorsWith("#FF0000", null, "#ZZZZZZ"), List.of());
            assertThat(normalizer.normalize(result).colors().text()).isNull();
        }

        @Test
        void nullColorStaysNull() {
            var result = baseResult(validProfile(), colorsWith(null, null, null), List.of());
            assertThat(normalizer.normalize(result).colors().primary()).isNull();
        }

        @Test
        void nullColorsSelectionStaysNull() {
            var result = baseResult(validProfile(), null, List.of());
            assertThat(normalizer.normalize(result).colors()).isNull();
        }
    }

    // =========================================================================
    // Asset URL safety
    // =========================================================================

    @Nested
    class AssetUrlSafety {

        @Test
        void httpsUrlKept() {
            var result = resultWithLogoUrl("https://acme.com/logo.png");
            assertThat(normalizer.normalize(result).assets().logos()).hasSize(1);
        }

        @Test
        void httpUrlKept() {
            var result = resultWithLogoUrl("http://acme.com/logo.png");
            assertThat(normalizer.normalize(result).assets().logos()).hasSize(1);
        }

        @Test
        void dataUrlRemoved() {
            var result = resultWithLogoUrl("data:image/png;base64,abc123");
            assertThat(normalizer.normalize(result).assets().logos()).isEmpty();
        }

        @Test
        void dataUrlAddsWarning() {
            var result = resultWithLogoUrl("data:image/png;base64,abc123");
            assertThat(normalizer.normalize(result).warnings())
                    .anyMatch(w -> w.message().contains("data:image/png;base64,abc123") &&
                                   w.message().contains("unsafe scheme"));
        }

        @Test
        void javascriptUrlRemoved() {
            var result = resultWithLogoUrl("javascript:void(0)");
            assertThat(normalizer.normalize(result).assets().logos()).isEmpty();
        }

        @Test
        void relativeUrlRemoved() {
            var result = resultWithLogoUrl("/images/logo.png");
            assertThat(normalizer.normalize(result).assets().logos()).isEmpty();
        }

        @Test
        void safeUrlPreservesAssetDetails() {
            var item   = new AssetItem("https://acme.com/logo.png", AssetRole.PRIMARY_LOGO,
                    0.9, 200, 50, "image/png", List.of("w1"));
            var assets = new AssetSelection(List.of(item), List.of());
            var result = baseResult(validProfile(), validColors(), List.of());
            var withAssets = withAssets(result, assets);
            var normalized = normalizer.normalize(withAssets);
            assertThat(normalized.assets().logos()).containsExactly(item);
        }

        @Test
        void nullAssetsSelectionStaysNull() {
            var result = baseResult(validProfile(), validColors(), List.of());
            assertThat(normalizer.normalize(result).assets()).isNull();
        }

        @Test
        void mixedSafeAndUnsafeUrlsFiltered() {
            var logos = List.of(
                    new AssetItem("https://acme.com/logo.png", AssetRole.PRIMARY_LOGO, 0.9, 0, 0, null, List.of()),
                    new AssetItem("data:image/png;base64,x",   AssetRole.PRIMARY_LOGO, 0.5, 0, 0, null, List.of()));
            var assets = new AssetSelection(logos, List.of());
            var result = withAssets(baseResult(validProfile(), validColors(), List.of()), assets);
            assertThat(normalizer.normalize(result).assets().logos()).hasSize(1);
        }
    }

    // =========================================================================
    // AI / evidence conflict detection
    // =========================================================================

    @Nested
    class ConflictDetection {

        @Test
        void noConflictWhenBrandNameMatchesOgSiteName() {
            var evidence = List.of(websiteWith("Acme Studio", List.of()));
            var result   = baseResult(profileWith("Acme Studio", null, null), validColors(), evidence);
            double originalConfidence = result.confidence().overall();

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(originalConfidence);
        }

        @Test
        void noConflictWhenAiNameContainsEvidenceName() {
            // e.g. og:site_name = "Acme", AI = "Acme Studio" → not a conflict
            var evidence = List.of(websiteWith("Acme", List.of()));
            var result   = baseResult(profileWith("Acme Studio", null, null), validColors(), evidence);
            double originalConfidence = result.confidence().overall();

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(originalConfidence);
        }

        @Test
        void noConflictWhenEvidenceNameContainsAiName() {
            // e.g. og:site_name = "Acme Corp", AI = "Acme" → not a conflict
            var evidence = List.of(websiteWith("Acme Corp", List.of()));
            var result   = baseResult(profileWith("Acme", null, null), validColors(), evidence);
            double originalConfidence = result.confidence().overall();

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(originalConfidence);
        }

        @Test
        void confidenceReducedWhenBrandNameConflictsWithOgSiteName() {
            var evidence = List.of(websiteWith("Acme Corp", List.of()));
            var result   = baseResult(profileWith("Foo Studio", null, null), validColors(), evidence);

            double normalized = normalizer.normalize(result).confidence().overall();
            assertThat(normalized).isLessThan(result.confidence().overall());
        }

        @Test
        void confidencePenaltyIsExactAmount() {
            double start   = 0.80;
            var evidence = List.of(websiteWith("Acme Corp", List.of()));
            var result   = resultWithConfidence(start, "Foo Studio", evidence);

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(start - CONFLICT_PENALTY);
        }

        @Test
        void conflictAddsWarning() {
            var evidence = List.of(websiteWith("Acme Corp", List.of()));
            var result   = baseResult(profileWith("Foo Studio", null, null), validColors(), evidence);

            assertThat(normalizer.normalize(result).warnings())
                    .anyMatch(w -> w.message().contains("Foo Studio") &&
                                   w.message().contains("Acme Corp"));
        }

        @Test
        void noConflictCheckWhenNoWebsiteEvidence() {
            var evidence = List.of(flyerEvidence());
            var result   = baseResult(profileWith("Foo Studio", null, null), validColors(), evidence);
            double originalConfidence = result.confidence().overall();

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(originalConfidence);
        }

        @Test
        void fallsBackToFirstHeadingWhenOgSiteNameAbsent() {
            var evidence = List.of(websiteWithHeadings(null, List.of("Acme Corp", "Services")));
            var result   = baseResult(profileWith("Foo Studio", null, null), validColors(), evidence);

            assertThat(normalizer.normalize(result).warnings())
                    .anyMatch(w -> w.message().contains("Acme Corp"));
        }

        @Test
        void noConflictWhenBrandNameIsNull() {
            var profile  = new BrandProfile(null, null, null, List.of());
            var evidence = List.of(websiteWith("Acme Corp", List.of()));
            var result   = baseResult(profile, validColors(), evidence);
            double originalConfidence = result.confidence().overall();

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(originalConfidence);
        }

        @Test
        void conflictIsCaseInsensitive() {
            // "acme studio" vs "ACME STUDIO" → no conflict (same normalised)
            var evidence = List.of(websiteWith("ACME STUDIO", List.of()));
            var result   = baseResult(profileWith("acme studio", null, null), validColors(), evidence);
            double originalConfidence = result.confidence().overall();

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(originalConfidence);
        }
    }

    // =========================================================================
    // Confidence bounding
    // =========================================================================

    @Nested
    class ConfidenceBounding {

        @Test
        void confidenceNotReducedBelowZero() {
            // start low, strong conflict → must not go negative
            double start   = 0.10;
            var evidence = List.of(websiteWith("Totally Different Corp", List.of()));
            var result   = resultWithConfidence(start, "Foo Studio", evidence);

            assertThat(normalizer.normalize(result).confidence().overall())
                    .isGreaterThanOrEqualTo(0.0);
        }

        @Test
        void normalConfidenceUnaffectedWhenNoConflict() {
            var result = baseResult(validProfile(), validColors(), List.of());
            assertThat(normalizer.normalize(result).confidence().overall())
                    .isEqualTo(result.confidence().overall());
        }
    }

    // =========================================================================
    // Optional field handling
    // =========================================================================

    @Nested
    class OptionalFields {

        @Test
        void nullLinksPreservedAsNull() {
            var result = baseResult(validProfile(), validColors(), List.of());
            assertThat(normalizer.normalize(result).links()).isNull();
        }

        @Test
        void nullAssetsPreservedAsNull() {
            var result = baseResult(validProfile(), validColors(), List.of());
            assertThat(normalizer.normalize(result).assets()).isNull();
        }

        @Test
        void nullTaglinePreservedAsNull() {
            var profile = profileWith("Acme", null, null);
            var result  = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().tagline()).isNull();
        }

        @Test
        void existingWarningsPreserved() {
            var existing = List.of(new ExtractionWarning("Pre-existing warning from AI."));
            var result   = resultWithWarnings(existing);
            assertThat(normalizer.normalize(result).warnings())
                    .contains(new ExtractionWarning("Pre-existing warning from AI."));
        }

        @Test
        void toneKeywordsPreserved() {
            var profile = new BrandProfile(
                    new Confident<>("Acme", 0.9), null, null,
                    List.of("bold", "modern"));
            var result = baseResult(profile, validColors(), List.of());
            assertThat(normalizer.normalize(result).brandProfile().toneKeywords())
                    .containsExactly("bold", "modern");
        }
    }

    // =========================================================================
    // Immutability / pass-through of untouched fields
    // =========================================================================

    @Nested
    class PassThrough {

        @Test
        void requestIdPreserved() {
            UUID id     = UUID.randomUUID();
            var result = baseResultWithId(id);
            assertThat(normalizer.normalize(result).requestId()).isEqualTo(id);
        }

        @Test
        void evidenceListPreserved() {
            var evidence = List.of(websiteWith("Acme", List.of()));
            var result   = baseResult(validProfile(), validColors(), evidence);
            assertThat(normalizer.normalize(result).evidence()).isEqualTo(evidence);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static BrandProfile validProfile() {
        return profileWith("Acme Studio", "Design for brands", "We build brands.");
    }

    private static BrandProfile profileWith(String name, String tagline, String summary) {
        return new BrandProfile(
                name    != null ? new Confident<>(name,    0.90) : null,
                tagline != null ? new Confident<>(tagline, 0.80) : null,
                summary != null ? new Confident<>(summary, 0.75) : null,
                List.of());
    }

    private static ColorSelection validColors() {
        return colorsWith("#FF0000", null, null);
    }

    private static ColorSelection colorsWith(String primary, String secondary, String text) {
        return new ColorSelection(
                primary   != null ? new ColorValue(primary,   1.0, List.of()) : null,
                secondary != null ? new ColorValue(secondary, 1.0, List.of()) : null,
                text      != null ? new ColorValue(text,      1.0, List.of()) : null);
    }

    private static ExtractionResult baseResult(BrandProfile profile, ColorSelection colors,
                                                List<? extends com.brandextractor.domain.evidence.Evidence> evidence) {
        return new ExtractionResult(
                UUID.randomUUID(), ExtractionInputType.URL, null, null,
                profile, colors, null, null,
                new ConfidenceScore(0.80),
                List.of(), List.of(),
                0, 0, 0, false,
                List.copyOf(evidence));
    }

    private static ExtractionResult baseResultWithId(UUID id) {
        return new ExtractionResult(
                id, ExtractionInputType.URL, null, null,
                validProfile(), validColors(), null, null,
                new ConfidenceScore(0.80),
                List.of(), List.of(),
                0, 0, 0, false,
                List.of());
    }

    private static ExtractionResult resultWithBrandName(String name) {
        return baseResult(
                new BrandProfile(new Confident<>(name, 0.9), null, null, List.of()),
                validColors(), List.of());
    }

    private static ExtractionResult resultWithLogoUrl(String url) {
        var assets = new AssetSelection(
                List.of(new AssetItem(url, AssetRole.PRIMARY_LOGO, 0.9, 0, 0, null, List.of())),
                List.of());
        return withAssets(baseResult(validProfile(), validColors(), List.of()), assets);
    }

    private static ExtractionResult resultWithWarnings(List<ExtractionWarning> warnings) {
        return new ExtractionResult(
                UUID.randomUUID(), ExtractionInputType.URL, null, null,
                validProfile(), validColors(), null, null,
                new ConfidenceScore(0.80),
                warnings, List.of(),
                0, 0, 0, false,
                List.of());
    }

    private static ExtractionResult resultWithConfidence(double confidence, String brandName,
                                                          List<? extends com.brandextractor.domain.evidence.Evidence> evidence) {
        return new ExtractionResult(
                UUID.randomUUID(), ExtractionInputType.URL, null, null,
                profileWith(brandName, null, null), validColors(), null, null,
                new ConfidenceScore(confidence),
                List.of(), List.of(),
                0, 0, 0, false,
                List.copyOf(evidence));
    }

    private static ExtractionResult withAssets(ExtractionResult base, AssetSelection assets) {
        return new ExtractionResult(
                base.requestId(), base.inputType(), base.originalSource(), base.resolvedSource(),
                base.brandProfile(), base.colors(), assets, base.links(),
                base.confidence(), base.warnings(), base.validationIssues(),
                base.textEvidenceCount(), base.imageEvidenceCount(),
                base.ocrBlockCount(), base.usedScreenshot(),
                base.evidence());
    }

    private static WebsiteEvidence websiteWith(String ogSiteName, List<String> headings) {
        return new WebsiteEvidence(
                "w1", "WEBSITE", "https://acme.com", "https://acme.com/",
                null, null, "", headings, null, List.of(), List.of(), List.of(),
                null, null, null, ogSiteName, null, null,
                1.0, Instant.now());
    }

    private static WebsiteEvidence websiteWithHeadings(String ogSiteName, List<String> headings) {
        return websiteWith(ogSiteName, headings);
    }

    private static FlyerEvidence flyerEvidence() {
        return new FlyerEvidence(
                "f1", "FLYER", "logo.png", "image/png", 100, 80, 2048L,
                List.of("#FF0000"), 1.0, Instant.now());
    }
}
