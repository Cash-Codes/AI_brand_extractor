package com.brandextractor.application.extraction;

import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.domain.evidence.OcrEvidence;
import com.brandextractor.domain.model.ExtractionResult;
import com.brandextractor.domain.ports.AIAnalysisPort;
import com.brandextractor.domain.ports.FlyerIngestionPort;
import com.brandextractor.domain.ports.OcrPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class FileExtractionServiceTest {

    private FlyerIngestionPort flyerIngestionPort;
    private OcrPort ocrPort;
    private AIAnalysisPort aiAnalysisPort;
    private FileExtractionService service;

    @BeforeEach
    void setUp() {
        flyerIngestionPort = mock(FlyerIngestionPort.class);
        ocrPort            = mock(OcrPort.class);
        aiAnalysisPort     = mock(AIAnalysisPort.class);
        service = new FileExtractionService(flyerIngestionPort, ocrPort, aiAnalysisPort);
    }

    @Test
    void ingestsFlyerAndOcrThenPassesBothToAi() {
        var flyerEvidence = stubFlyerEvidence();
        var ocrEvidence   = stubOcrEvidence();
        var expected      = mock(ExtractionResult.class);

        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(flyerEvidence);
        when(ocrPort.extractText(any(), any())).thenReturn(ocrEvidence);
        when(aiAnalysisPort.analyse(anyList())).thenReturn(expected);

        var result = service.extract(new byte[]{1, 2, 3}, "image/png", "logo.png");

        assertThat(result).isSameAs(expected);
        verify(flyerIngestionPort).ingest(new byte[]{1, 2, 3}, "image/png", "logo.png");
        verify(ocrPort).extractText(new byte[]{1, 2, 3}, "image/png");
        verify(aiAnalysisPort).analyse(List.of(flyerEvidence, ocrEvidence));
    }

    @Test
    void flyerEvidenceComesBeforeOcrEvidenceInList() {
        var flyerEvidence = stubFlyerEvidence();
        var ocrEvidence   = stubOcrEvidence();

        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(flyerEvidence);
        when(ocrPort.extractText(any(), any())).thenReturn(ocrEvidence);
        when(aiAnalysisPort.analyse(anyList())).thenReturn(mock(ExtractionResult.class));

        service.extract(new byte[0], "image/png", null);

        verify(aiAnalysisPort).analyse(List.of(flyerEvidence, ocrEvidence));
    }

    @Test
    void passesSourceLabelToFlyerIngestionPort() {
        when(flyerIngestionPort.ingest(any(), any(), any())).thenReturn(stubFlyerEvidence());
        when(ocrPort.extractText(any(), any())).thenReturn(stubOcrEvidence());
        when(aiAnalysisPort.analyse(anyList())).thenReturn(mock(ExtractionResult.class));

        service.extract(new byte[0], "image/jpeg", "campaign-flyer.jpg");

        verify(flyerIngestionPort).ingest(any(), eq("image/jpeg"), eq("campaign-flyer.jpg"));
    }

    // -------------------------------------------------------------------------

    private static FlyerEvidence stubFlyerEvidence() {
        return new FlyerEvidence(
                "f-1", "FLYER", "logo.png", "image/png", 100, 80, 2048L,
                List.of("#FF0000"), 1.0, Instant.now());
    }

    private static OcrEvidence stubOcrEvidence() {
        return new OcrEvidence(
                "o-1", "IMAGE", "logo.png", List.of(), 0.9, Instant.now());
    }
}
