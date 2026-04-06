package com.brandextractor.infrastructure.ai.client;

import com.brandextractor.domain.candidate.*;
import com.brandextractor.domain.evidence.Evidence;

import java.util.List;

/**
 * Typed payload sent to a {@link BrandExtractionAiClient}.
 *
 * <p>Contains the normalised evidence and pre-discovered candidates so the AI model
 * can refine and rank rather than work from raw HTML alone.
 */
public record AiExtractionRequest(
        List<Evidence>            evidence,
        List<BrandNameCandidate>  brandNameCandidates,
        List<TaglineCandidate>    taglineCandidates,
        List<SummaryCandidate>    summaryCandidates,
        List<ColorCandidate>      colorCandidates,
        List<AssetCandidate>      assetCandidates,
        List<LinkCandidate>       linkCandidates) {}
