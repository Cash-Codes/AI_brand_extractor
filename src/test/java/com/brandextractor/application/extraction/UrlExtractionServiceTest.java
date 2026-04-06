package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.ScreenshotEvidence;
import com.brandextractor.domain.evidence.VisualEvidence;
import com.brandextractor.domain.evidence.WebsiteEvidence;
import com.brandextractor.domain.model.*;
import com.brandextractor.domain.ports.*;
import com.brandextractor.support.error.AiProviderException;
import com.brandextractor.support.error.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UrlExtractionServiceTest {

    private WebsiteIngestionPort        websiteIngestionPort;
    private ScreenshotPort              screenshotPort;
    private VisualAnalysisPort          visualAnalysisPort;
    private AIAnalysisPort              aiAnalysisPort;
    private ExtractionNormalizationPort normalizationPort;
    private ExtractionValidationPort    validationPort;
    private UrlExtractionService        service;

    @BeforeEach
    void setUp() {
        websiteIngestionPort = mock(WebsiteIngestionPort.class);
        screenshotPort       = mock(ScreenshotPort.class);
        visualAnalysisPort   = mock(VisualAnalysisPort.class);
        aiAnalysisPort       = mock(AIAnalysisPort.class);
        normalizationPort    = mock(ExtractionNormalizationPort.class);
        validationPort       = mock(ExtractionValidationPort.class);
        service = new UrlExtractionService(
                websiteIngestionPort, screenshotPort, visualAnalysisPort,
                aiAnalysisPort, normalizationPort, validationPort);

        // defaults
        when(screenshotPort.capture(any())).thenReturn(Optional.empty());
        when(normalizationPort.normalize(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationPort.validate(any())).thenReturn(List.of());
    }

    // -------------------------------------------------------------------------
    // URL validation
    // -------------------------------------------------------------------------

    @Test
    void throwsExtractionExceptionForNullUrl() {
        assertThatThrownBy(() -> service.extract(null))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void throwsExtractionExceptionForBlankUrl() {
        assertThatThrownBy(() -> service.extract("  "))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void throwsExtractionExceptionForNonHttpUrl() {
        assertThatThrownBy(() -> service.extract("ftp://example.com"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("http");
    }

    // -------------------------------------------------------------------------
    // Evidence assembly
    // -------------------------------------------------------------------------

    @Test
    void ingestsWebsiteAndPassesEvidenceToAi() {
        var website  = stubWebsiteEvidence();
        var expected = stubResult();
        when(websiteIngestionPort.ingest("https://example.com")).thenReturn(website);
        when(aiAnalysisPort.analyse(anyList())).thenReturn(expected);

        service.extract("https://example.com");

        verify(websiteIngestionPort).ingest("https://example.com");
        verify(aiAnalysisPort).analyse(argThat(list -> list.contains(website)));
    }

    @Test
    void includesScreenshotEvidenceWhenClientReturnsOne() {
        var website    = stubWebsiteEvidence();
        var screenshot = stubScreenshotEvidence();
        when(websiteIngestionPort.ingest(any())).thenReturn(website);
        when(screenshotPort.capture(any())).thenReturn(Optional.of(screenshot));
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract("https://example.com");

        verify(aiAnalysisPort).analyse(argThat(list ->
                list.contains(website) && list.contains(screenshot)));
    }

    @Test
    void omitsScreenshotWhenClientReturnsEmpty() {
        var website = stubWebsiteEvidence();
        when(websiteIngestionPort.ingest(any())).thenReturn(website);
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract("https://example.com");

        verify(aiAnalysisPort).analyse(argThat(list ->
                list.size() == 1 && list.contains(website)));
    }

    @Test
    void includesVisualEvidenceWhenScreenshotPresentAndAnalysisSucceeds() {
        var screenshot = stubScreenshotEvidence();
        var visual     = stubVisualEvidence();
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(screenshotPort.capture(any())).thenReturn(Optional.of(screenshot));
        when(visualAnalysisPort.analyse(any(), any())).thenReturn(visual);
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract("https://example.com");

        verify(aiAnalysisPort).analyse(argThat(list -> list.contains(visual)));
    }

    @Test
    void skipsVisualAnalysisWhenItThrows() {
        var screenshot = stubScreenshotEvidence();
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(screenshotPort.capture(any())).thenReturn(Optional.of(screenshot));
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException("not impl"));
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        // must not throw
        service.extract("https://example.com");

        verify(aiAnalysisPort).analyse(argThat(list ->
                list.stream().noneMatch(VisualEvidence.class::isInstance)));
    }

    @Test
    void doesNotCallVisualAnalysisWhenNoScreenshot() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract("https://example.com");

        verifyNoInteractions(visualAnalysisPort);
    }

    // -------------------------------------------------------------------------
    // Result metadata
    // -------------------------------------------------------------------------

    @Test
    void resultHasInputTypeUrl() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        var result = service.extract("https://example.com");

        assertThat(result.inputType()).isEqualTo(ExtractionInputType.URL);
    }

    @Test
    void resultHasOriginalSourceEqualToInputUrl() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        var result = service.extract("https://example.com");

        assertThat(result.originalSource()).isEqualTo("https://example.com");
    }

    @Test
    void resultHasResolvedSourceFromWebsiteEvidence() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        var result = service.extract("https://example.com");

        assertThat(result.resolvedSource()).isEqualTo("https://example.com/");
    }

    @Test
    void textEvidenceCountReflectsWebsiteEvidence() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        var result = service.extract("https://example.com");

        assertThat(result.textEvidenceCount()).isEqualTo(1);
    }

    @Test
    void imageEvidenceCountIncludesScreenshot() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(screenshotPort.capture(any())).thenReturn(Optional.of(stubScreenshotEvidence()));
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        var result = service.extract("https://example.com");

        assertThat(result.imageEvidenceCount()).isEqualTo(1);
        assertThat(result.usedScreenshot()).isTrue();
    }

    @Test
    void usedScreenshotFalseWhenNoScreenshot() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        var result = service.extract("https://example.com");

        assertThat(result.usedScreenshot()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    void validationIssuesFromPortAreIncludedInResult() {
        var issue = new ValidationIssue("MISSING_BRAND_NAME", "Brand name missing", ValidationIssue.Severity.ERROR);
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());
        when(validationPort.validate(any())).thenReturn(List.of(issue));

        var result = service.extract("https://example.com");

        assertThat(result.validationIssues()).containsExactly(issue);
    }

    @Test
    void emptyValidationIssuesWhenPortReturnsNone() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        var result = service.extract("https://example.com");

        assertThat(result.validationIssues()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Failure paths
    // -------------------------------------------------------------------------

    @Test
    void propagatesAiProviderExceptionWithoutWrapping() {
        when(websiteIngestionPort.ingest(any())).thenReturn(stubWebsiteEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenThrow(new AiProviderException("AI down"));

        assertThatThrownBy(() -> service.extract("https://example.com"))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    void propagatesIngestionExceptionWithoutWrapping() {
        when(websiteIngestionPort.ingest(any())).thenThrow(new ExtractionException("DNS failure"));

        assertThatThrownBy(() -> service.extract("https://example.com"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("DNS failure");
    }

    // -------------------------------------------------------------------------
    // Helpers
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

    private static VisualEvidence stubVisualEvidence() {
        return new VisualEvidence(
                "v-1", "IMAGE", "image/png",
                List.of("logo", "text"), "minimal", 0.85, Instant.now());
    }

    private static ExtractionResult stubResult() {
        return new ExtractionResult(
                UUID.randomUUID(),
                ExtractionInputType.URL, null, null,
                null, null, null, null,
                new ConfidenceScore(0.80),
                List.of(), List.of(),
                0, 0, 0, false,
                List.of());
    }
}
