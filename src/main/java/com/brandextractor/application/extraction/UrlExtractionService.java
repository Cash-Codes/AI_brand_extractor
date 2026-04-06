package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.evidence.ScreenshotEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
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
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UrlExtractionService implements UrlExtractionUseCase {

    private static final Logger log = LoggerFactory.getLogger(UrlExtractionService.class);
    private static final Pattern HTTP_SCHEME =
            Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE);

    private final WebsiteIngestionPort    websiteIngestionPort;
    private final ScreenshotPort          screenshotPort;
    private final VisualAnalysisPort      visualAnalysisPort;
    private final AIAnalysisPort          aiAnalysisPort;
    private final ExtractionValidationPort validationPort;

    @Override
    public ExtractionResult extract(String url) {
        validateUrl(url);

        List<Evidence> evidence = new ArrayList<>();

        // 1. Parse the page for structured HTML signals
        WebsiteEvidence website = websiteIngestionPort.ingest(url);
        evidence.add(website);

        // 2. Capture screenshot (no-op until a real client is wired)
        Optional<ScreenshotEvidence> screenshot = screenshotPort.capture(url);
        screenshot.ifPresent(evidence::add);

        // 3. Visual enrichment from screenshot bytes (best-effort)
        screenshot.ifPresent(s -> tryVisualAnalysis(s.imageBytes(), s.mimeType(), evidence));

        // 4. Analyse all collected evidence with the AI model
        ExtractionResult base = aiAnalysisPort.analyse(List.copyOf(evidence));

        // 5. Post-extraction validation
        var issues = validationPort.validate(base);

        // 6. Stamp correct source metadata and evidence counts
        return ResultEnricher.enrich(
                base,
                ExtractionInputType.URL,
                url,
                website.resolvedUrl(),
                issues,
                List.copyOf(evidence));
    }

    // -------------------------------------------------------------------------

    private static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ExtractionException("URL must not be blank.");
        }
        if (!HTTP_SCHEME.matcher(url).matches()) {
            throw new ExtractionException(
                    "URL must begin with http:// or https://: " + url);
        }
    }

    private void tryVisualAnalysis(byte[] imageBytes, String mimeType, List<Evidence> evidence) {
        try {
            var visual = visualAnalysisPort.analyse(imageBytes, mimeType);
            if (visual != null) evidence.add(visual);
        } catch (Exception e) {
            log.debug("Visual analysis skipped for URL flow: {}", e.getMessage());
        }
    }
}
