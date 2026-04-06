package com.brandextractor.infrastructure.screenshot;

import com.brandextractor.domain.evidence.ScreenshotEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreenshotPortAdapterTest {

    private ScreenshotClient client;
    private ScreenshotPortAdapter adapter;

    @BeforeEach
    void setUp() {
        client = mock(ScreenshotClient.class);
        adapter = new ScreenshotPortAdapter(client);
    }

    @Test
    void returnsEmptyWhenClientReturnsEmpty() {
        when(client.capture("https://example.com")).thenReturn(Optional.empty());

        assertThat(adapter.capture("https://example.com")).isEmpty();
    }

    @Test
    void mapsClientImageToScreenshotEvidence() {
        byte[] bytes = new byte[]{1, 2, 3};
        when(client.capture("https://example.com"))
                .thenReturn(Optional.of(new CapturedImage(bytes, "image/png", 1280, 800)));

        Optional<ScreenshotEvidence> result = adapter.capture("https://example.com");

        assertThat(result).isPresent();
        ScreenshotEvidence ev = result.get();
        assertThat(ev.id()).isNotBlank();
        assertThat(ev.sourceType()).isEqualTo("SCREENSHOT");
        assertThat(ev.sourceReference()).isEqualTo("https://example.com");
        assertThat(ev.imageBytes()).isEqualTo(bytes);
        assertThat(ev.mimeType()).isEqualTo("image/png");
        assertThat(ev.width()).isEqualTo(1280);
        assertThat(ev.height()).isEqualTo(800);
        assertThat(ev.confidence()).isEqualTo(1.0);
        assertThat(ev.extractedAt()).isNotNull();
    }

    @Test
    void eachCallProducesAUniqueId() {
        byte[] bytes = new byte[0];
        when(client.capture("https://example.com"))
                .thenReturn(Optional.of(new CapturedImage(bytes, "image/png", 100, 100)));

        String id1 = adapter.capture("https://example.com").map(ScreenshotEvidence::id).orElseThrow();
        String id2 = adapter.capture("https://example.com").map(ScreenshotEvidence::id).orElseThrow();

        assertThat(id1).isNotEqualTo(id2);
    }
}
