package com.brandextractor.infrastructure.ai.client;

import java.util.List;

/**
 * Structured response returned by a {@link BrandExtractionAiClient}.
 *
 * <p>All fields directly correspond to properties in the JSON schema enforced in
 * {@link com.brandextractor.infrastructure.ai.vertex.VertexAiPromptFactory}.
 *
 * @param brandName           chosen brand name
 * @param brandNameConfidence confidence in [0.0, 1.0]
 * @param tagline             chosen tagline
 * @param taglineConfidence   confidence in [0.0, 1.0]
 * @param summary             one-paragraph brand summary
 * @param summaryConfidence   confidence in [0.0, 1.0]
 * @param toneKeywords        up to 5 tone/personality keywords
 * @param primaryColor        hex of the primary brand colour
 * @param secondaryColor      hex of the secondary brand colour (nullable)
 * @param textColor           hex of the primary text colour (nullable)
 * @param logoUrl             URL of the best logo candidate (nullable)
 * @param heroImageUrl        URL of the best hero image (nullable)
 * @param contactLinks        key=platform, value=url (e.g. "instagram" → url)
 * @param overallConfidence   overall extraction confidence in [0.0, 1.0]
 * @param warnings            any warnings the model wants to surface
 */
public record AiExtractionResponse(
        String       brandName,
        double       brandNameConfidence,
        String       tagline,
        double       taglineConfidence,
        String       summary,
        double       summaryConfidence,
        List<String> toneKeywords,
        String       primaryColor,
        String       secondaryColor,
        String       textColor,
        String       logoUrl,
        String       heroImageUrl,
        java.util.Map<String, String> contactLinks,
        double       overallConfidence,
        List<String> warnings) {}
