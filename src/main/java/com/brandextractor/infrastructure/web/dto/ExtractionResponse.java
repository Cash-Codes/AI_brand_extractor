package com.brandextractor.infrastructure.web.dto;

import com.brandextractor.infrastructure.web.dto.evidence.EvidenceDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExtractionResponse(
        UUID requestId,
        String inputType,
        SourceDto source,
        BrandProfileDto brandProfile,
        ColorSelectionDto colors,
        AssetSelectionDto assets,
        ContactLinksDto links,
        ConfidenceDto confidence,
        List<String> warnings,
        List<String> validationIssues,
        EvidenceSummaryDto evidenceSummary,
        List<EvidenceDto> evidence) {}   // null when ?include=evidence not present
