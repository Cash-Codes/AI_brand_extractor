package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.ports.AIAnalysisPort;
import com.brandextractor.domain.ports.FlyerIngestionPort;
import com.brandextractor.domain.ports.OcrPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileExtractionService implements FileExtractionUseCase {

    private final FlyerIngestionPort flyerIngestionPort;
    private final OcrPort ocrPort;
    private final AIAnalysisPort aiAnalysisPort;

    @Override
    public ExtractionResult extract(byte[] imageBytes, String mimeType, String sourceLabel) {
        List<Evidence> evidence = new ArrayList<>();

        // 1. Extract flyer metadata: dimensions, dominant colours, MIME type
        evidence.add(flyerIngestionPort.ingest(imageBytes, mimeType, sourceLabel));

        // 2. Extract text blocks with bounding-box metadata
        evidence.add(ocrPort.extractText(imageBytes, mimeType));

        // 3. Analyse collected evidence with the AI model
        return aiAnalysisPort.analyse(List.copyOf(evidence));
    }
}
