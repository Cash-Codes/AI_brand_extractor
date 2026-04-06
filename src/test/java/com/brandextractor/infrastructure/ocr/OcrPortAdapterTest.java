package com.brandextractor.infrastructure.ocr;

import com.brandextractor.domain.evidence.OcrEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OcrPortAdapterTest {

    private OcrClient client;
    private OcrPortAdapter adapter;

    @BeforeEach
    void setUp() {
        client  = mock(OcrClient.class);
        adapter = new OcrPortAdapter(client);
    }

    // -------------------------------------------------------------------------
    // Structured evidence
    // -------------------------------------------------------------------------

    @Test
    void mapsDetectedTextToTextBlocks() {
        when(client.extract(any(), anyString())).thenReturn(new OcrClientResponse(
                List.of(new DetectedText("ACME", new PixelBoundingBox(10, 20, 100, 40), 0.95)),
                400, 200));

        OcrEvidence ev = adapter.extractText(new byte[]{1}, "image/png");

        assertThat(ev.blocks()).hasSize(1);
        assertThat(ev.blocks().get(0).text()).isEqualTo("ACME");
        assertThat(ev.blocks().get(0).confidence()).isEqualTo(0.95);
    }

    @Test
    void preservesReadingOrderOfBlocks() {
        when(client.extract(any(), anyString())).thenReturn(new OcrClientResponse(
                List.of(
                        new DetectedText("First",  null, 0.90),
                        new DetectedText("Second", null, 0.85),
                        new DetectedText("Third",  null, 0.80)),
                100, 100));

        OcrEvidence ev = adapter.extractText(new byte[0], "image/jpeg");

        assertThat(ev.blocks()).extracting(b -> b.text())
                .containsExactly("First", "Second", "Third");
    }

    // -------------------------------------------------------------------------
    // Bounding-box normalisation
    // -------------------------------------------------------------------------

    @Test
    void normalisesPixelBoundingBoxToFractionalCoordinates() {
        when(client.extract(any(), anyString())).thenReturn(new OcrClientResponse(
                List.of(new DetectedText("Logo", new PixelBoundingBox(40, 60, 200, 80), 1.0)),
                400, 300));

        var box = adapter.extractText(new byte[0], "image/png").blocks().get(0).boundingBox();

        assertThat(box).isNotNull();
        assertThat(box.x())      .isCloseTo(0.10, within(1e-9)); // 40/400
        assertThat(box.y())      .isCloseTo(0.20, within(1e-9)); // 60/300
        assertThat(box.width())  .isCloseTo(0.50, within(1e-9)); // 200/400
        assertThat(box.height()) .isCloseTo(0.2666, within(1e-4)); // 80/300
    }

    @Test
    void boundingBoxIsNullWhenClientProvidesNone() {
        when(client.extract(any(), anyString())).thenReturn(new OcrClientResponse(
                List.of(new DetectedText("NoBox", null, 0.9)),
                200, 200));

        var box = adapter.extractText(new byte[0], "image/png").blocks().get(0).boundingBox();

        assertThat(box).isNull();
    }

    @Test
    void boundingBoxIsNullWhenImageDimensionsAreZero() {
        // Provider returned a box but no image dimensions
        when(client.extract(any(), anyString())).thenReturn(new OcrClientResponse(
                List.of(new DetectedText("Text", new PixelBoundingBox(10, 10, 50, 20), 0.9)),
                0, 0));

        var box = adapter.extractText(new byte[0], "image/png").blocks().get(0).boundingBox();

        assertThat(box).isNull();
    }

    @Test
    void normalisedCoordinatesAreClampedToUnitRange() {
        // Edge case: bounding box that exactly fills the image
        when(client.extract(any(), anyString())).thenReturn(new OcrClientResponse(
                List.of(new DetectedText("Full", new PixelBoundingBox(0, 0, 800, 600), 1.0)),
                800, 600));

        var box = adapter.extractText(new byte[0], "image/png").blocks().get(0).boundingBox();

        assertThat(box.x())      .isEqualTo(0.0);
        assertThat(box.y())      .isEqualTo(0.0);
        assertThat(box.width())  .isEqualTo(1.0);
        assertThat(box.height()) .isEqualTo(1.0);
    }

    // -------------------------------------------------------------------------
    // Overall confidence
    // -------------------------------------------------------------------------

    @Test
    void confidenceIsMeanOfBlockConfidences() {
        when(client.extract(any(), anyString())).thenReturn(new OcrClientResponse(
                List.of(
                        new DetectedText("A", null, 0.80),
                        new DetectedText("B", null, 0.90),
                        new DetectedText("C", null, 1.00)),
                100, 100));

        OcrEvidence ev = adapter.extractText(new byte[0], "image/png");

        assertThat(ev.confidence()).isCloseTo(0.90, within(1e-9));
    }

    @Test
    void confidenceIsOneWhenNoBlocksReturned() {
        when(client.extract(any(), anyString())).thenReturn(OcrClientResponse.empty());

        OcrEvidence ev = adapter.extractText(new byte[0], "image/png");

        assertThat(ev.blocks()).isEmpty();
        assertThat(ev.confidence()).isEqualTo(1.0);
    }

    // -------------------------------------------------------------------------
    // Evidence provenance
    // -------------------------------------------------------------------------

    @Test
    void evidenceHasUniqueIdAndTimestamp() {
        when(client.extract(any(), anyString())).thenReturn(OcrClientResponse.empty());

        OcrEvidence ev1 = adapter.extractText(new byte[0], "image/png");
        OcrEvidence ev2 = adapter.extractText(new byte[0], "image/png");

        assertThat(ev1.id()).isNotBlank();
        assertThat(ev2.id()).isNotBlank();
        assertThat(ev1.id()).isNotEqualTo(ev2.id());
        assertThat(ev1.extractedAt()).isNotNull();
    }

    @Test
    void sourceTypeIsImage() {
        when(client.extract(any(), anyString())).thenReturn(OcrClientResponse.empty());

        OcrEvidence ev = adapter.extractText(new byte[0], "image/jpeg");

        assertThat(ev.sourceType()).isEqualTo("IMAGE");
    }

    @Test
    void sourceReferenceIsTheMimeType() {
        when(client.extract(any(), anyString())).thenReturn(OcrClientResponse.empty());

        OcrEvidence ev = adapter.extractText(new byte[0], "image/jpeg");

        assertThat(ev.sourceReference()).isEqualTo("image/jpeg");
    }

}
