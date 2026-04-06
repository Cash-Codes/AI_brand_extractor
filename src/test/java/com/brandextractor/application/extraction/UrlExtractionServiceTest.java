package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.ScreenshotEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.ports.AIAnalysisPort;
import com.brandextractor.domain.ports.ScreenshotPort;
import com.brandextractor.domain.ports.WebsiteIngestionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class UrlExtractionServiceTest {

    private WebsiteIngestionPort websiteIngestionPort;
    private ScreenshotPort screenshotPort;
    private AIAnalysisPort aiAnalysisPort;
    private UrlExtractionService service;

    @BeforeEach
    void setUp() {
        websiteIngestionPort = mock(WebsiteIngestionPort.class);
        screenshotPort       = mock(ScreenshotPort.class);
        aiAnalysisPort       = mock(AIAnalysisPort.class);
        service = new UrlExtractionService(websiteIngestionPort, screenshotPort, aiAnalysisPort);
    }

    @Test
    void ingestsWebsiteAndPassesEvidenceToAi() {
        var websiteEvidence = stubWebsiteEvidence();
        var expectedResult  = mock(ExtractionResult.class);
        when(websiteIngestionPort.ingest("https://example.com")).thenReturn(websiteEvidence);
        when(screenshotPort.capture("https://example.com")).thenReturn(Optional.empty());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(expectedResult);

        var result = service.extract("https://example.com");

        assertThat(result).isSameAs(expectedResult);
        verify(websiteIngestionPort).ingest("https://example.com");
        verify(aiAnalysisPort).analyse(List.of(websiteEvidence));
    }

    @Test
    void includesScreenshotEvidenceWhenClientReturnsOne() {
        var websiteEvidence    = stubWebsiteEvidence();
        var screenshotEvidence = stubScreenshotEvidence();
        var expectedResult     = mock(ExtractionResult.class);
        when(websiteIngestionPort.ingest("https://example.com")).thenReturn(websiteEvidence);
        when(screenshotPort.capture("https://example.com")).thenReturn(Optional.of(screenshotEvidence));
        when(aiAnalysisPort.analyse(anyList())).thenReturn(expectedResult);

        service.extract("https://example.com");

        verify(aiAnalysisPort).analyse(List.of(websiteEvidence, screenshotEvidence));
    }

    @Test
    void omitsScreenshotFromEvidenceWhenClientReturnsEmpty() {
        var websiteEvidence = stubWebsiteEvidence();
        when(websiteIngestionPort.ingest("https://example.com")).thenReturn(websiteEvidence);
        when(screenshotPort.capture("https://example.com")).thenReturn(Optional.empty());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(mock(ExtractionResult.class));

        service.extract("https://example.com");

        verify(aiAnalysisPort).analyse(List.of(websiteEvidence));
    }

    // -------------------------------------------------------------------------

    private static WebsiteEvidence stubWebsiteEvidence() {
        return new WebsiteEvidence(
                "w-1", "WEBSITE", "https://example.com", "https://example.com/",
                null, null, "", List.of(), null, List.of(), List.of(), List.of(),
                null, null, null, null, null, null,
                1.0, Instant.now());
    }

    private static ScreenshotEvidence stubScreenshotEvidence() {
        return new ScreenshotEvidence(
                "s-1", "SCREENSHOT", "https://example.com",
                new byte[0], "image/png", 1280, 800,
                1.0, Instant.now());
    }
}
