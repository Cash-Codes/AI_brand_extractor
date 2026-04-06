package com.brandextractor.domain.ports;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.ExtractionResult;

import java.util.List;

public interface AIAnalysisPort {
    ExtractionResult analyse(List<Evidence> evidence);
}
