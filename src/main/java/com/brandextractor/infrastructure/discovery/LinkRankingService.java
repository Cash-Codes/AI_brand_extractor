package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.candidate.LinkCandidate;
import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts social and contact link candidates from website evidence.
 *
 * <p>Each social platform has a fixed base score reflecting its importance as a brand
 * presence signal. mailto: and tel: links are contact signals scored slightly lower.
 * Duplicate hrefs are suppressed.
 */
@Component
class LinkRankingService {

    /** Platform keyword → base score (higher = more brand-signal weight). */
    private static final Map<String, Double> PLATFORM_SCORES = Map.ofEntries(
            Map.entry("instagram.com",  0.90),
            Map.entry("linkedin.com",   0.88),
            Map.entry("twitter.com",    0.80),
            Map.entry("x.com",          0.80),
            Map.entry("facebook.com",   0.78),
            Map.entry("tiktok.com",     0.75),
            Map.entry("youtube.com",    0.72),
            Map.entry("mailto:",        0.70),
            Map.entry("tel:",           0.65)
    );

    List<LinkCandidate> discover(List<Evidence> evidence) {
        List<LinkCandidate> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Evidence e : evidence) {
            if (e instanceof WebsiteEvidence w) {
                for (String href : w.socialLinks()) {
                    if (href == null || href.isBlank() || !seen.add(href)) continue;
                    results.add(new LinkCandidate(
                            href,
                            detectPlatform(href),
                            scoreFor(href),
                            List.of(w.id())));
                }
            }
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    // -------------------------------------------------------------------------

    static String detectPlatform(String href) {
        if (href == null) return "unknown";
        String lower = href.toLowerCase();
        if (lower.startsWith("mailto:")) return "email";
        if (lower.startsWith("tel:"))    return "phone";
        for (String key : PLATFORM_SCORES.keySet()) {
            if (lower.contains(key)) return platformName(key);
        }
        return "unknown";
    }

    static double scoreFor(String href) {
        if (href == null) return 0.0;
        String lower = href.toLowerCase();
        for (Map.Entry<String, Double> entry : PLATFORM_SCORES.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return 0.50;
    }

    private static String platformName(String key) {
        return switch (key) {
            case "instagram.com" -> "instagram";
            case "linkedin.com"  -> "linkedin";
            case "twitter.com"   -> "twitter";
            case "x.com"         -> "twitter";
            case "facebook.com"  -> "facebook";
            case "tiktok.com"    -> "tiktok";
            case "youtube.com"   -> "youtube";
            case "mailto:"       -> "email";
            case "tel:"          -> "phone";
            default              -> key;
        };
    }
}
