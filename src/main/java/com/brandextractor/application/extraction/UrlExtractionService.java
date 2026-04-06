package com.brandextractor.application.extraction;

import com.brandextractor.domain.model.ExtractionResult;
import org.springframework.stereotype.Service;

@Service
public class UrlExtractionService implements UrlExtractionUseCase {

    @Override
    public ExtractionResult extract(String url) {
        throw new UnsupportedOperationException("URL extraction not yet implemented");
    }
}
