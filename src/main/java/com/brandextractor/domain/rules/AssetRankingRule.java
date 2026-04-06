package com.brandextractor.domain.rules;

import com.brandextractor.domain.candidate.AssetCandidate;
import com.brandextractor.domain.model.AssetSelection;

import java.util.List;

public interface AssetRankingRule {
    AssetSelection rank(List<AssetCandidate> candidates);
}
