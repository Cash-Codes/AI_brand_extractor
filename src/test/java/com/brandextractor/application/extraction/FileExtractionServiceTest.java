package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.evidence.VisualEvidence;
import com.brandextractor.domain.model.*;
import com.brandextractor.domain.ports.*;
import com.brandextractor.support.error.AiProviderException;
import com.brandextractor.support.error.ExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileExtractionServiceTest {

    private FlyerIngestionPort       flyerIngestionPort;
    private OcrPort                  ocrPort;
    private VisualAnalysisPort       visualAnalysisPort;
    private AIAnalysisPort           aiAnalysisPort;
    private ExtractionValidationPort validationPort;
    private FileExtractionService    service;

    @BeforeEach
    void setUp() {
        flyerIngestionPort = mock(FlyerIngestionPort.class);
        ocrPort            = mock(OcrPort.class);
        visualAnalysisPort = mock(VisualAnalysisPort.class);
        aiAnalysisPort     = mock(AIAnalysisPort.class);
        validationPort     = mock(ExtractionValidationPort.class);
        service = new FileExtractionService(
                flyerIngestionPort, ocrPort, visualAnalysisPort, aiAnalysisPort, validationPort);

        when(validationPort.validate(any())).thenReturn(List.of());
    }

    // -------------------------------------------------------------------------
    // Input validation
    // -------------------------------------------------------------------------

    @Test
    void throwsForNullBytes() {
        assertThatThrownBy(() -> service.extract(null, "image/png", "f.png"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void throwsForEmptyBytes() {
        assertThatThrownBy(() -> service.extract(new byte[0], "image/png", "f.png"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void throwsForOversizedFile() {
        byte[] big = new byte[10 * 1024 * 1024 + 1]; // 10 MB + 1 byte
        assertThatThrownBy(() -> service.extract(big, "image/png", "f.png"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void throwsForUnsupportedMimeType() {
        assertThatThrownBy(() -> service.extract(new byte[]{1}, "image/gif", "f.gif"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("image/gif");
    }

    @Test
    void throwsForNullMimeType() {
        assertThatThrownBy(() -> service.extract(new byte[]{1}, null, "f.png"))
                .isInstanceOf(ExtractionException.class);
    }

    @Test
    void acceptsJpeg() {
        stubPortsForHappyPath();
        // should not throw
        service.extract(new byte[]{1}, "image/jpeg", "photo.jpg");
    }

    @Test
    void acceptsPng() {
        stubPortsForHappyPath();
        // should not throw
        service.extract(new byte[]{1}, "image/png", "logo.png");
    }

    // -------------------------------------------------------------------------
    // Evidence assembly
    // -------------------------------------------------------------------------

    @Test
    void ingestsFlyerAndOcrThenPassesBothToAi() {
        var flyer = stubFlyerEvidence();
        var ocr   = stubOcrEvidence();
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(flyer);
        when(ocrPort.extractText(any(), any())).thenReturn(ocr);
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract(new byte[]{1, 2, 3}, "image/png", "logo.png");

        verify(flyerIngestionPort).ingest(new byte[]{1, 2, 3}, "image/png", "logo.png");
        verify(ocrPort).extractText(new byte[]{1, 2, 3}, "image/png");
        verify(aiAnalysisPort).analyse(argThat(list ->
                list.contains(flyer) && list.contains(ocr)));
    }

    @Test
    void flyerEvidenceComesBeforeOcrEvidenceInList() {
        var flyer = stubFlyerEvidence();
        var ocr   = stubOcrEvidence();
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(flyer);
        when(ocrPort.extractText(any(), any())).thenReturn(ocr);
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract(new byte[]{1}, "image/png", null);

        verify(aiAnalysisPort).analyse(argThat(list ->
                list.indexOf(flyer) < list.indexOf(ocr)));
    }

    @Test
    void includesVisualEvidenceWhenAnalysisSucceeds() {
        var visual = stubVisualEvidence();
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(stubFlyerEvidence());
        when(ocrPort.extractText(any(), any())).thenReturn(stubOcrEvidence());
        when(visualAnalysisPort.analyse(any(), any())).thenReturn(visual);
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract(new byte[]{1}, "image/png", "logo.png");

        verify(aiAnalysisPort).analyse(argThat(list -> list.contains(visual)));
    }

    @Test
    void skipsVisualEvidenceWhenAnalysisThrows() {
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(stubFlyerEvidence());
        when(ocrPort.extractText(any(), any())).thenReturn(stubOcrEvidence());
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException("not impl"));
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        // must not throw
        service.extract(new byte[]{1}, "image/png", "logo.png");

        verify(aiAnalysisPort).analyse(argThat(list ->
                list.stream().noneMatch(VisualEvidence.class::isInstance)));
    }

    // -------------------------------------------------------------------------
    // Result metadata
    // -------------------------------------------------------------------------

    @Test
    void resultHasInputTypeFile() {
        stubPortsForHappyPath();

        var result = service.extract(new byte[]{1}, "image/png", "logo.png");

        assertThat(result.inputType()).isEqualTo(ExtractionInputType.FILE);
    }

    @Test
    void resultHasOriginalSourceFromSourceLabel() {
        stubPortsForHappyPath();

        var result = service.extract(new byte[]{1}, "image/png", "brand-logo.png");

        assertThat(result.originalSource()).isEqualTo("brand-logo.png");
    }

    @Test
    void resolvedSourceIsNullForFileFlow() {
        stubPortsForHappyPath();

        var result = service.extract(new byte[]{1}, "image/png", "logo.png");

        assertThat(result.resolvedSource()).isNull();
    }

    @Test
    void imageEvidenceCountIncludesFlyerEvidence() {
        stubPortsForHappyPath();

        var result = service.extract(new byte[]{1}, "image/png", "logo.png");

        assertThat(result.imageEvidenceCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void usedScreenshotAlwaysFalseForFileFlow() {
        stubPortsForHappyPath();

        var result = service.extract(new byte[]{1}, "image/png", "logo.png");

        assertThat(result.usedScreenshot()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    void validationIssuesAreIncludedInResult() {
        var issue = new ValidationIssue(
                "MISSING_BRAND_NAME", "No brand name", ValidationIssue.Severity.ERROR);
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(stubFlyerEvidence());
        when(ocrPort.extractText(any(), any())).thenReturn(stubOcrEvidence());
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());
        when(validationPort.validate(any())).thenReturn(List.of(issue));

        var result = service.extract(new byte[]{1}, "image/png", "logo.png");

        assertThat(result.validationIssues()).containsExactly(issue);
    }

    // -------------------------------------------------------------------------
    // Failure paths
    // -------------------------------------------------------------------------

    @Test
    void propagatesAiProviderException() {
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(stubFlyerEvidence());
        when(ocrPort.extractText(any(), any())).thenReturn(stubOcrEvidence());
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException());
        when(aiAnalysisPort.analyse(anyList())).thenThrow(new AiProviderException("AI unavailable"));

        assertThatThrownBy(() -> service.extract(new byte[]{1}, "image/png", "logo.png"))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    void propagatesIngestionException() {
        when(flyerIngestionPort.ingest(any(), any(), any()))
                .thenThrow(new ExtractionException("Corrupt image"));

        assertThatThrownBy(() -> service.extract(new byte[]{1}, "image/png", "bad.png"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("Corrupt image");
    }

    @Test
    void passesSourceLabelToFlyerIngestionPort() {
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(stubFlyerEvidence());
        when(ocrPort.extractText(any(), any())).thenReturn(stubOcrEvidence());
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());

        service.extract(new byte[]{1}, "image/jpeg", "campaign-flyer.jpg");

        verify(flyerIngestionPort).ingest(any(), eq("image/jpeg"), eq("campaign-flyer.jpg"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void stubPortsForHappyPath() {
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(stubFlyerEvidence());
        when(ocrPort.extractText(any(), any())).thenReturn(stubOcrEvidence());
        when(visualAnalysisPort.analyse(any(), any())).thenThrow(new UnsupportedOperationException());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(stubResult());
    }

    private static FlyerEvidence stubFlyerEvidence() {
        return new FlyerEvidence(
                "f-1", "FLYER", "logo.png", "image/png", 100, 80, 2048L,
                List.of("#FF0000"), 1.0, Instant.now());
    }

    private static OcrEvidence stubOcrEvidence() {
        return new OcrEvidence(
                "o-1", "IMAGE", "logo.png", List.of(), 0.9, Instant.now());
    }

    private static VisualEvidence stubVisualEvidence() {
        return new VisualEvidence(
                "v-1", "IMAGE", "image/png",
                List.of("logo"), "bold", 0.80, Instant.now());
    }

    private static ExtractionResult stubResult() {
        return new ExtractionResult(
                UUID.randomUUID(),
                ExtractionInputType.FILE, null, null,
                null, null, null, null,
                new ConfidenceScore(0.75),
                List.of(), List.of(),
                0, 0, 0, false,
                List.of());
    }
}
