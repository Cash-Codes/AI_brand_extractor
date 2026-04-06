package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.ExtractionInputType;
import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.ports.*;
import com.brandextractor.support.error.ExtractionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileExtractionService implements FileExtractionUseCase {

    private static final Logger log = LoggerFactory.getLogger(FileExtractionService.class);
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final FlyerIngestionPort          flyerIngestionPort;
    private final OcrPort                     ocrPort;
    private final VisualAnalysisPort          visualAnalysisPort;
    private final AIAnalysisPort              aiAnalysisPort;
    private final ExtractionNormalizationPort normalizationPort;
    private final ExtractionValidationPort    validationPort;

    @Override
    public ExtractionResult extract(byte[] imageBytes, String mimeType, String sourceLabel) {
        validateInput(imageBytes, mimeType);

        List<Evidence> evidence = new ArrayList<>();

        // 1. Extract flyer metadata: dimensions, dominant colours, MIME type
        evidence.add(flyerIngestionPort.ingest(imageBytes, mimeType, sourceLabel));

        // 2. Extract text blocks with bounding-box metadata
        evidence.add(ocrPort.extractText(imageBytes, mimeType));

        // 3. Visual enrichment from image bytes (best-effort)
        tryVisualAnalysis(imageBytes, mimeType, evidence);

        // 4. Analyse all collected evidence with the AI model
        ExtractionResult base = aiAnalysisPort.analyse(List.copyOf(evidence));

        // 5. Normalise AI output: trim text, sanitise URLs, detect conflicts
        ExtractionResult normalised = normalizationPort.normalize(base);

        // 6. Post-extraction validation
        var issues = validationPort.validate(normalised);

        // 7. Stamp correct source metadata and evidence counts
        return ResultEnricher.enrich(
                normalised,
                ExtractionInputType.FILE,
                sourceLabel,
                null,
                issues,
                List.copyOf(evidence));
    }

    // -------------------------------------------------------------------------

    private static void validateInput(byte[] imageBytes, String mimeType) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ExtractionException("Image bytes must not be empty.");
        }
        if (imageBytes.length > MAX_FILE_BYTES) {
            throw new ExtractionException(
                    "File size %d bytes exceeds the maximum of %d bytes (10 MB)."
                            .formatted(imageBytes.length, MAX_FILE_BYTES));
        }
        if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw new ExtractionException(
                    "Unsupported MIME type '%s'. Only image/jpeg and image/png are accepted."
                            .formatted(mimeType));
        }
    }

    private void tryVisualAnalysis(byte[] imageBytes, String mimeType, List<Evidence> evidence) {
        try {
            var visual = visualAnalysisPort.analyse(imageBytes, mimeType);
            if (visual != null) evidence.add(visual);
        } catch (Exception e) {
            log.debug("Visual analysis skipped for file flow: {}", e.getMessage());
        }
    }
}
