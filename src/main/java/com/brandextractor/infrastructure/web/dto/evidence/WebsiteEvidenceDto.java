package com.brandextractor.infrastructure.web.dto.evidence;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Evidence collected from parsing a website's HTML — " +
                      "titles, headings, OG tags, CSS colour candidates, and social links.")
public record WebsiteEvidenceDto(

        @Schema(description = "Unique evidence ID, e.g. `w-1`", example = "w-1")
        String id,

        @Schema(description = "Always `WEBSITE` for this evidence type", example = "WEBSITE")
        String sourceType,

        @Schema(description = "Original URL submitted by the caller",
                example = "https://www.acmestudio.com")
        String sourceReference,

        @Schema(description = "Final URL after HTTP redirects",
                example = "https://www.acmestudio.com/")
        String resolvedUrl,

        @Schema(description = "Contents of the HTML <title> element",
                example = "Acme Studio — Crafting brands that endure")
        String title,

        @Schema(description = "Contents of <meta name='description'>",
                example = "A full-service branding agency.")
        String metaDescription,

        @Schema(description = "Visible body text, truncated to 3000 characters",
                example = "We are a full-service branding agency. Bold. Minimal. Modern.")
        String visibleText,

        @Schema(description = "H1–H3 headings extracted from the page, up to 20",
                example = "[\"Acme Studio\", \"Crafting brands that endure\"]")
        List<String> headings,

        @Schema(description = "URL of the site's favicon",
                example = "https://www.acmestudio.com/favicon.ico")
        String faviconUrl,

        @Schema(description = "Absolute image URLs found in the page, up to 30",
                example = "[\"https://www.acmestudio.com/assets/logo.png\"]")
        List<String> imageUrls,

        @Schema(description = "Social, mailto:, and tel: links discovered in the page, up to 20",
                example = "[\"https://www.instagram.com/acmestudio\", \"mailto:hello@acmestudio.com\"]")
        List<String> socialLinks,

        @Schema(description = "Chromatic CSS colour candidates extracted from stylesheets and inline " +
                              "styles, normalised to uppercase #RRGGBB, up to 30",
                example = "[\"#1E3A8A\", \"#FF6600\"]")
        List<String> cssColorCandidates,

        @Schema(description = "og:title value", example = "Acme Studio — Crafting brands that endure")
        String ogTitle,

        @Schema(description = "og:description value", example = "A full-service branding agency.")
        String ogDescription,

        @Schema(description = "og:image URL", example = "https://www.acmestudio.com/og-image.jpg")
        String ogImage,

        @Schema(description = "og:site_name value", example = "Acme Studio")
        String ogSiteName,

        @Schema(description = "twitter:card value", example = "summary_large_image")
        String twitterCard,

        @Schema(description = "twitter:image URL", example = "https://www.acmestudio.com/tw.png")
        String twitterImage) implements EvidenceDto {}
