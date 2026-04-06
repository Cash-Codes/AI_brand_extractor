package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.Evidence;
import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.ports.AIAnalysisPort;
import com.brandextractor.domain.ports.ScreenshotPort;
import com.brandextractor.domain.ports.WebsiteIngestionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlExtractionService implements UrlExtractionUseCase {

    private final WebsiteIngestionPort websiteIngestionPort;
    private final ScreenshotPort screenshotPort;
    private final AIAnalysisPort aiAnalysisPort;

    @Override
    public ExtractionResult extract(String url) {
        List<Evidence> evidence = new ArrayList<>();

        // 1. Parse the page for structured HTML signals
        evidence.add(websiteIngestionPort.ingest(url));

        // 2. Capture a screenshot for visual analysis (no-op until a real client is wired)
        screenshotPort.capture(url).ifPresent(evidence::add);

        // 3. Analyse all collected evidence with the AI model
        return aiAnalysisPort.analyse(List.copyOf(evidence));
    }
}
