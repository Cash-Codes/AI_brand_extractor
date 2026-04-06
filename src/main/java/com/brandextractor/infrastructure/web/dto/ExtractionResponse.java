package com.brandextractor.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

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
        EvidenceSummaryDto evidenceSummary) {}
