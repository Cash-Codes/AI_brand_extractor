package com.brandextractor.domain.rules;

import com.brandextractor.domain.candidate.ColorCandidate;
import com.brandextractor.domain.model.ColorSelection;

import java.util.List;

public interface ColorNormalisationRule {
    ColorSelection normalise(List<ColorCandidate> candidates);
}
