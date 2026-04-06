package com.brandextractor.domain.model;

import com.brandextractor.domain.candidate.*;

import java.util.List;

/**
 * Container for all brand-signal candidates discovered from a set of evidence.
 * Candidates are unranked raw signals; domain rules apply ranking and normalisation
 * before they are assembled into an {@link ExtractionResult}.
 */
public record ExtractionCandidates(
        List<BrandNameCandidate>    brandNames,
        List<TaglineCandidate>      taglines,
        List<SummaryCandidate>      summaries,
        List<ColorCandidate>        colors,
        List<AssetCandidate>        assets,
        List<LinkCandidate>         links,
        List<ToneKeywordCandidate>  toneKeywords) {}
