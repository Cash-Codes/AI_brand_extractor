package com.brandextractor.application.extraction;

import com.brandextractor.domain.model.ExtractionResult;
import org.springframework.stereotype.Service;

@Service
public class FileExtractionService implements FileExtractionUseCase {

    @Override
    public ExtractionResult extract(byte[] imageBytes, String mimeType, String sourceLabel) {
        throw new UnsupportedOperationException("File extraction not yet implemented");
    }
}
