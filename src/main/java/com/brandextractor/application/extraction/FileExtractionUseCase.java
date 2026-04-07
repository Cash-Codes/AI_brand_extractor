package com.brandextractor.application.extraction;

import com.brandextractor.domain.model.ExtractionResult;

public interface FileExtractionUseCase {
    ExtractionResult extract(byte[] imageBytes, String mimeType, String sourceLabel);
}
