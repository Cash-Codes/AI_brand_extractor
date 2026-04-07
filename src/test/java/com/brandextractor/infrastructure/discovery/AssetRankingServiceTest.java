package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.AssetCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.AssetRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssetRankingServiceTest {

    private AssetRankingService service;

    @BeforeEach
    void setUp() { service = new AssetRankingService(); }

    // -------------------------------------------------------------------------
    // Logo candidates
    // -------------------------------------------------------------------------

    @Test
    void imageUrlWithLogoInPathIsPrimaryLogo() {
        List<Evidence> evidence = List.of(website()
                .imageUrls(List.of("https://example.com/images/logo.png"))
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        AssetCandidate logo = assetWithUrl(assets, "https://example.com/images/logo.png");
        assertThat(logo.role()).isEqualTo(AssetRole.PRIMARY_LOGO);
        assertThat(logo.score()).isEqualTo(0.90);
    }

    @Test
    void imageUrlWithBrandInPathIsSecondaryLogo() {
        List<Evidence> evidence = List.of(website()
                .imageUrls(List.of("https://example.com/brand/mark.svg"))
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        AssetCandidate logo = assetWithUrl(assets, "https://example.com/brand/mark.svg");
        assertThat(logo.role()).isEqualTo(AssetRole.SECONDARY_LOGO);
    }

    @Test
    void faviconIsPrimaryLogoCandidate() {
        List<Evidence> evidence = List.of(website()
                .faviconUrl("https://example.com/favicon.ico")
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        AssetCandidate favicon = assetWithUrl(assets, "https://example.com/favicon.ico");
        assertThat(favicon.role()).isEqualTo(AssetRole.PRIMARY_LOGO);
        assertThat(favicon.score()).isEqualTo(0.80);
    }

    @Test
    void ogImageWithLogoHintIsPrimaryLogo() {
        List<Evidence> evidence = List.of(website()
                .ogImage("https://example.com/logo-og.png")
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        AssetCandidate og = assetWithUrl(assets, "https://example.com/logo-og.png");
        assertThat(og.role()).isEqualTo(AssetRole.PRIMARY_LOGO);
        assertThat(og.score()).isEqualTo(0.85);
    }

    // -------------------------------------------------------------------------
    // Hero-image candidates
    // -------------------------------------------------------------------------

    @Test
    void ogImageWithoutLogoHintIsHeroImage() {
        List<Evidence> evidence = List.of(website()
                .ogImage("https://example.com/hero-banner.jpg")
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        AssetCandidate og = assetWithUrl(assets, "https://example.com/hero-banner.jpg");
        assertThat(og.role()).isEqualTo(AssetRole.HERO_IMAGE);
        assertThat(og.score()).isEqualTo(0.75);
    }

    @Test
    void twitterImageIsHeroImageCandidate() {
        List<Evidence> evidence = List.of(website()
                .twitterImage("https://example.com/tw.jpg")
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        AssetCandidate tw = assetWithUrl(assets, "https://example.com/tw.jpg");
        assertThat(tw.role()).isEqualTo(AssetRole.HERO_IMAGE);
        assertThat(tw.score()).isEqualTo(0.65);
    }

    @Test
    void firstNonLogoImageIsHeroCandidate() {
        List<Evidence> evidence = List.of(website()
                .imageUrls(List.of(
                        "https://example.com/team.jpg",
                        "https://example.com/office.jpg"))
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        // First non-logo image should be the hero
        AssetCandidate hero = assetWithUrl(assets, "https://example.com/team.jpg");
        assertThat(hero.role()).isEqualTo(AssetRole.HERO_IMAGE);
        // Second non-logo image should not be added
        assertThat(assets).noneMatch(a -> a.url().equals("https://example.com/office.jpg")
                && a.role() == AssetRole.HERO_IMAGE);
    }

    // -------------------------------------------------------------------------
    // Flyer
    // -------------------------------------------------------------------------

    @Test
    void flyerImageIsPrimaryLogoCandidate() {
        FlyerEvidence flyer = new FlyerEvidence(
                "f1", "FLYER", "campaign.png", "image/png", 1200, 800, 50_000L,
                List.of(), 1.0, Instant.now());

        List<AssetCandidate> assets = service.discover(List.<Evidence>of(flyer));

        assertThat(assets).hasSize(1);
        AssetCandidate asset = assets.get(0);
        assertThat(asset.url()).isEqualTo("campaign.png");
        assertThat(asset.role()).isEqualTo(AssetRole.PRIMARY_LOGO);
        assertThat(asset.width()).isEqualTo(1200);
        assertThat(asset.height()).isEqualTo(800);
        assertThat(asset.mimeType()).isEqualTo("image/png");
    }

    // -------------------------------------------------------------------------
    // Ordering and evidence refs
    // -------------------------------------------------------------------------

    @Test
    void assetsOrderedByScoreDescending() {
        List<Evidence> evidence = List.of(website()
                .faviconUrl("https://example.com/favicon.ico")
                .ogImage("https://example.com/hero.jpg")
                .imageUrls(List.of("https://example.com/logo.png"))
                .build());

        List<AssetCandidate> assets = service.discover(evidence);

        for (int i = 0; i < assets.size() - 1; i++) {
            assertThat(assets.get(i).score())
                    .isGreaterThanOrEqualTo(assets.get(i + 1).score());
        }
    }

    @Test
    void assetCandidateContainsEvidenceRefAndRationale() {
        WebsiteEvidence w = website().faviconUrl("https://example.com/favicon.ico").build();
        List<AssetCandidate> assets = service.discover(List.<Evidence>of(w));

        AssetCandidate favicon = assetWithUrl(assets, "https://example.com/favicon.ico");
        assertThat(favicon.evidenceRefs()).contains(w.id());
        assertThat(favicon.rationale()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // containsLogoHint helper
    // -------------------------------------------------------------------------

    @Test
    void containsLogoHint_trueForLogoKeyword() {
        assertThat(AssetRankingService.containsLogoHint("https://cdn.example.com/logo.png")).isTrue();
    }

    @Test
    void containsLogoHint_trueForIconKeyword() {
        assertThat(AssetRankingService.containsLogoHint("https://example.com/icon-192.png")).isTrue();
    }

    @Test
    void containsLogoHint_falseForGenericUrl() {
        assertThat(AssetRankingService.containsLogoHint("https://example.com/hero.jpg")).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AssetCandidate assetWithUrl(List<AssetCandidate> list, String url) {
        return list.stream().filter(a -> a.url().equals(url)).findFirst().orElseThrow(
                () -> new AssertionError("No asset with URL: " + url + " in " + list));
    }

    private static WebsiteBuilder website() { return new WebsiteBuilder(); }

    private static class WebsiteBuilder {
        private String faviconUrl;
        private String ogImage;
        private String twitterImage;
        private List<String> imageUrls = List.of();

        WebsiteBuilder faviconUrl(String v)      { this.faviconUrl = v; return this; }
        WebsiteBuilder ogImage(String v)         { this.ogImage = v; return this; }
        WebsiteBuilder twitterImage(String v)    { this.twitterImage = v; return this; }
        WebsiteBuilder imageUrls(List<String> v) { this.imageUrls = v; return this; }

        WebsiteEvidence build() {
            return new WebsiteEvidence(
                    "w-test", "WEBSITE", "https://example.com", "https://example.com/",
                    null, null, "", List.of(),
                    faviconUrl, imageUrls, List.of(), List.of(),
                    null, null, ogImage, null, null, twitterImage,
                    1.0, Instant.now());
        }
    }
}
