package com.brandextractor.domain.evidence;

import java.time.Instant;
import java.util.List;

public record WebsiteEvidence(
        String id,
        String sourceType,
        String sourceReference,          // original requested URL
        String resolvedUrl,              // final URL after redirects

        // --- page content ---
        String title,                    // <title> tag
        String metaDescription,          // <meta name="description">
        String visibleText,              // truncated visible body text (no markup)
        List<String> headings,           // h1–h3 text in document order

        // --- assets ---
        String faviconUrl,
        List<String> imageUrls,          // src/data-src from <img> tags

        // --- social & contact links ---
        List<String> socialLinks,        // href for known platforms + mailto: + tel:

        // --- colours ---
        List<String> cssColorCandidates, // normalised hex from inline styles + <style>

        // --- Open Graph ---
        String ogTitle,
        String ogDescription,
        String ogImage,
        String ogSiteName,

        // --- Twitter Card ---
        String twitterCard,
        String twitterImage,

        // --- provenance ---
        double confidence,
        Instant extractedAt) implements Evidence {}
