package com.brandextractor.application.extraction;

import com.brandextractor.domain.model.ExtractionResult;

public interface UrlExtractionUseCase {
    ExtractionResult extract(String url);
}
