package com.brandextractor.infrastructure.web.dto.evidence;

import java.util.List;

public record WebsiteEvidenceDto(
        String id,
        String sourceType,
        String sourceReference,
        String resolvedUrl,
        String title,
        String metaDescription,
        String visibleText,
        List<String> headings,
        String faviconUrl,
        List<String> imageUrls,
        List<String> socialLinks,
        List<String> cssColorCandidates,
        String ogTitle,
        String ogDescription,
        String ogImage,
        String ogSiteName,
        String twitterCard,
        String twitterImage) implements EvidenceDto {}
