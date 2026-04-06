package com.brandextractor.domain.ports;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.ExtractionCandidates;

import java.util.List;

public interface CandidateDiscoveryPort {

    /**
     * Analyses the collected evidence and produces typed brand-signal candidates
     * for each extraction dimension (name, tagline, summary, colours, assets, links,
     * tone keywords).  The returned candidates are unranked; ranking and normalisation
     * are applied downstream by the domain rules layer.
     */
    ExtractionCandidates discover(List<Evidence> evidence);
}
