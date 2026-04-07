package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.AssetCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.AssetRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts logo and hero-image asset candidates from website and flyer evidence.
 *
 * <h3>Logo signals</h3>
 * <ul>
 *   <li>Favicon URL                               — PRIMARY_LOGO  0.80</li>
 *   <li>og:image (when path contains logo/brand)  — PRIMARY_LOGO  0.85</li>
 *   <li>Image URLs whose path contains "logo"     — PRIMARY_LOGO  0.90</li>
 *   <li>Image URLs whose path contains "brand"    — SECONDARY_LOGO 0.75</li>
 *   <li>The flyer image itself                    — PRIMARY_LOGO  0.80</li>
 * </ul>
 *
 * <h3>Hero-image signals</h3>
 * <ul>
 *   <li>og:image (general)                        — HERO_IMAGE    0.75</li>
 *   <li>Twitter card image                        — HERO_IMAGE    0.65</li>
 *   <li>First non-logo image URL                  — HERO_IMAGE    0.55</li>
 * </ul>
 */
@Component
class AssetRankingService {

    List<AssetCandidate> discover(List<Evidence> evidence) {
        List<AssetCandidate> results = new ArrayList<>();

        for (Evidence e : evidence) {
            switch (e) {
                case WebsiteEvidence w -> collectFromWebsite(w, results);
                case FlyerEvidence f   -> collectFromFlyer(f, results);
                default                -> { }
            }
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    // -------------------------------------------------------------------------

    private void collectFromWebsite(WebsiteEvidence w, List<AssetCandidate> out) {
        String ref = w.id();

        // Favicon
        if (w.faviconUrl() != null && !w.faviconUrl().isBlank()) {
            out.add(asset(w.faviconUrl(), AssetRole.PRIMARY_LOGO, 0.80,
                    "Favicon is the most compact brand mark", ref));
        }

        // og:image — logo path → PRIMARY_LOGO, otherwise → HERO_IMAGE
        if (w.ogImage() != null && !w.ogImage().isBlank()) {
            if (containsLogoHint(w.ogImage())) {
                out.add(asset(w.ogImage(), AssetRole.PRIMARY_LOGO, 0.85,
                        "og:image path contains logo hint", ref));
            } else {
                out.add(asset(w.ogImage(), AssetRole.HERO_IMAGE, 0.75,
                        "og:image is the canonical social sharing image", ref));
            }
        }

        // Twitter card image
        if (w.twitterImage() != null && !w.twitterImage().isBlank()) {
            out.add(asset(w.twitterImage(), AssetRole.HERO_IMAGE, 0.65,
                    "Twitter card image is a supplementary hero asset", ref));
        }

        // Image URLs
        boolean heroAssigned = false;
        for (String url : w.imageUrls()) {
            if (url == null || url.isBlank()) continue;
            String lower = url.toLowerCase();
            if (lower.contains("logo")) {
                out.add(asset(url, AssetRole.PRIMARY_LOGO, 0.90,
                        "Image URL path contains 'logo'", ref));
            } else if (lower.contains("brand")) {
                out.add(asset(url, AssetRole.SECONDARY_LOGO, 0.75,
                        "Image URL path contains 'brand'", ref));
            } else if (!heroAssigned) {
                out.add(asset(url, AssetRole.HERO_IMAGE, 0.55,
                        "First non-logo image is a hero-image candidate", ref));
                heroAssigned = true;
            }
        }
    }

    private void collectFromFlyer(FlyerEvidence f, List<AssetCandidate> out) {
        out.add(new AssetCandidate(
                f.sourceReference(),
                AssetRole.PRIMARY_LOGO,
                0.80,
                "Uploaded flyer is a primary brand asset",
                List.of(f.id()),
                f.width(),
                f.height(),
                f.mimeType()));
    }

    // -------------------------------------------------------------------------

    private static AssetCandidate asset(String url, AssetRole role, double score,
                                        String rationale, String ref) {
        return new AssetCandidate(url, role, score, rationale, List.of(ref), 0, 0, null);
    }

    static boolean containsLogoHint(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("logo") || lower.contains("brand") || lower.contains("icon");
    }
}
