package com.brandextractor.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contact and social media links discovered in the source. " +
                      "All fields are null when not found.")
public record ContactLinksDto(

        @Schema(description = "Primary website URL",
                example = "https://www.acmestudio.com")
        String website,

        @Schema(description = "Instagram profile URL",
                example = "https://www.instagram.com/acmestudio")
        String instagram,

        @Schema(description = "LinkedIn page URL",
                example = "https://www.linkedin.com/company/acmestudio")
        String linkedin,

        @Schema(description = "Contact email address (mailto: URI)",
                example = "mailto:hello@acmestudio.com")
        String email,

        @Schema(description = "X (Twitter) profile URL",
                example = "https://twitter.com/acmestudio")
        String twitter,

        @Schema(description = "Facebook page URL",
                example = "https://www.facebook.com/acmestudio")
        String facebook,

        @Schema(description = "TikTok profile URL",
                example = "https://www.tiktok.com/@acmestudio")
        String tiktok,

        @Schema(description = "YouTube channel URL",
                example = "https://www.youtube.com/@acmestudio")
        String youtube,

        @Schema(description = "Phone number (tel: URI)",
                example = "tel:+15551234567")
        String phone) {}
