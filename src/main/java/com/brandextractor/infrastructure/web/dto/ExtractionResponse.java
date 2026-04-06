package com.brandextractor.infrastructure.web.dto;

import com.brandextractor.infrastructure.web.dto.evidence.EvidenceDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

// Class-level NON_NULL is intentional: Java records don't support field-level Jackson
// annotations on components. Only `evidence` should ever be null (opt-in via ?include=evidence).
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Brand profile extracted from the submitted URL or image file")
public record ExtractionResponse(

        @Schema(description = "Unique identifier for this extraction request",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID requestId,

        @Schema(description = "Input type that produced this result",
                allowableValues = {"URL", "FILE"},
                example = "URL")
        String inputType,

        @Schema(description = "Source URL or filename, both as supplied and as resolved after redirects")
        SourceDto source,

        @Schema(description = "AI-extracted brand identity fields with per-field confidence scores")
        BrandProfileDto brandProfile,

        @Schema(description = "Primary, secondary, and text colour values in #RRGGBB format")
        ColorSelectionDto colors,

        @Schema(description = "Logo and hero image candidates identified from the source")
        AssetSelectionDto assets,

        @Schema(description = "Social and contact links discovered in the source")
        ContactLinksDto links,

        @Schema(description = "Overall extraction confidence aggregated across all AI-extracted fields")
        ConfidenceDto confidence,

        @Schema(description = "Non-blocking warnings emitted during normalisation or by the AI model. " +
                              "Absent when empty.",
                example = "[\"Brand name was truncated from 120 to 100 characters.\"]")
        List<String> warnings,

        @Schema(description = "Post-extraction validation issues. Absent when empty.",
                example = "[\"Primary brand colour could not be identified.\"]")
        List<String> validationIssues,

        @Schema(description = "Counts of evidence items used during extraction")
        EvidenceSummaryDto evidenceSummary,

        @Schema(description = "Raw extraction evidence. Only present when ?include=evidence is passed.")
        List<EvidenceDto> evidence) {}   // null when ?include=evidence not present
