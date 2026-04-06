package com.brandextractor.infrastructure.discovery;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.ExtractionCandidates;
import com.brandextractor.domain.ports.CandidateDiscoveryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic, rule-based implementation of {@link CandidateDiscoveryPort}.
 *
 * <p>Delegates each dimension to a focused ranking service and assembles the
 * results into an {@link ExtractionCandidates} container. No AI calls are made;
 * all logic is pure signal extraction and scoring.
 */
@Component
@RequiredArgsConstructor
public class DeterministicCandidateDiscoveryAdapter implements CandidateDiscoveryPort {

    private final BrandNameRankingService       brandNameService;
    private final TaglineSummaryRankingService  taglineSummaryService;
    private final ColorRankingService           colorService;
    private final AssetRankingService           assetService;
    private final LinkRankingService            linkService;

    @Override
    public ExtractionCandidates discover(List<Evidence> evidence) {
        return new ExtractionCandidates(
                brandNameService.discover(evidence),
                taglineSummaryService.discoverTaglines(evidence),
                taglineSummaryService.discoverSummaries(evidence),
                colorService.discover(evidence),
                assetService.discover(evidence),
                linkService.discover(evidence),
                List.of());   // tone keywords: reserved for AI analysis
    }
}
