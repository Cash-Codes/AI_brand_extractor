package com.brandextractor.infrastructure.ingestion.flyer;

import com.brandextractor.domain.evidence.FlyerEvidence;
import com.brandextractor.support.error.ExtractionException;
import com.brandextractor.support.util.MimeTypeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultipartFlyerIngestionAdapterTest {

    private MultipartFlyerIngestionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MultipartFlyerIngestionAdapter(new MimeTypeUtils());
    }

    // -------------------------------------------------------------------------
    // Happy-path ingestion
    // -------------------------------------------------------------------------

    @Test
    void ingest_validPng_returnsFlyerEvidence() throws IOException {
        byte[] png = solidPng(100, 80, 0xFF0000); // pure red

        FlyerEvidence ev = adapter.ingest(png, "image/png", "logo.png");

        assertThat(ev.id()).isNotBlank();
        assertThat(ev.sourceType()).isEqualTo("FLYER");
        assertThat(ev.sourceReference()).isEqualTo("logo.png");
        assertThat(ev.mimeType()).isEqualTo("image/png");
        assertThat(ev.width()).isEqualTo(100);
        assertThat(ev.height()).isEqualTo(80);
        assertThat(ev.sizeBytes()).isEqualTo(png.length);
        assertThat(ev.confidence()).isEqualTo(1.0);
        assertThat(ev.extractedAt()).isNotNull();
    }

    @Test
    void ingest_validJpeg_returnsFlyerEvidence() throws IOException {
        byte[] jpeg = solidJpeg(120, 90, 0x0000FF); // pure blue

        FlyerEvidence ev = adapter.ingest(jpeg, "image/jpeg", "banner.jpg");

        assertThat(ev.mimeType()).isEqualTo("image/jpeg");
        assertThat(ev.width()).isEqualTo(120);
        assertThat(ev.height()).isEqualTo(90);
    }

    @Test
    void ingest_nullSourceLabel_usesDefaultReference() throws IOException {
        FlyerEvidence ev = adapter.ingest(solidPng(10, 10, 0x000000), "image/png", null);

        assertThat(ev.sourceReference()).isEqualTo("upload");
    }

    @Test
    void ingest_usesMagicBytesNotDeclaredMimeType() throws IOException {
        // Declare "image/jpeg" but pass actual PNG bytes — adapter should trust the bytes
        byte[] png = solidPng(10, 10, 0xFFFFFF);

        FlyerEvidence ev = adapter.ingest(png, "image/jpeg", "file");

        assertThat(ev.mimeType()).isEqualTo("image/png");
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    void ingest_fileTooLarge_throwsExtractionException() {
        byte[] tooBig = new byte[(int) MultipartFlyerIngestionAdapter.MAX_FILE_BYTES + 1];

        assertThatThrownBy(() -> adapter.ingest(tooBig, "image/png", "big.png"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void ingest_unsupportedFormat_throwsExtractionException() {
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};

        assertThatThrownBy(() -> adapter.ingest(garbage, "image/gif", "anim.gif"))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("Unsupported image format");
    }

    // -------------------------------------------------------------------------
    // Dominant colour extraction
    // -------------------------------------------------------------------------

    @Test
    void extractDominantColors_solidRed_returnsSingleRedBucket() throws IOException {
        BufferedImage image = solidImage(50, 50, 0xFF0000);

        var colors = adapter.extractDominantColors(image);

        assertThat(colors).isNotEmpty();
        // Pure red 0xFF0000 quantised with step 32 → #E00000
        assertThat(colors.get(0)).matches("#[0-9A-F]{6}");
        assertThat(colors.get(0)).startsWith("#E"); // red channel dominant
    }

    @Test
    void extractDominantColors_returnsAtMostMaxColors() throws IOException {
        // Build an image with many different colours
        int size = 200;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, (x * 7 + y * 13) & 0xFFFFFF);
            }
        }

        var colors = adapter.extractDominantColors(image);

        assertThat(colors).hasSizeLessThanOrEqualTo(MultipartFlyerIngestionAdapter.MAX_DOMINANT_COLORS);
    }

    @Test
    void extractDominantColors_mostFrequentColorFirst() throws IOException {
        // 3/4 of pixels red, 1/4 blue
        int w = 100, h = 100;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(x, y, x < 75 ? 0xFF0000 : 0x0000FF);
            }
        }

        var colors = adapter.extractDominantColors(image);

        assertThat(colors).hasSizeGreaterThanOrEqualTo(2);
        // Red bucket should appear before blue bucket
        int redIdx  = indexOfColorStartingWith(colors, "#E");  // red
        int blueIdx = indexOfColorStartingWith(colors, "#0");  // blue
        assertThat(redIdx).isLessThan(blueIdx);
    }

    @Test
    void extractDominantColors_transparentPixelsSkipped() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        // All pixels fully transparent
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                image.setRGB(x, y, 0x00FF0000); // alpha=0, red=255
            }
        }

        var colors = adapter.extractDominantColors(image);

        assertThat(colors).isEmpty();
    }

    @Test
    void extractDominantColors_hexValuesAreUppercaseAndSixDigit() throws IOException {
        BufferedImage image = solidImage(20, 20, 0x1A2B3C);

        var colors = adapter.extractDominantColors(image);

        assertThat(colors).isNotEmpty();
        assertThat(colors.get(0)).matches("#[0-9A-F]{6}");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static byte[] solidPng(int w, int h, int rgb) throws IOException {
        return encode(solidImage(w, h, rgb), "PNG");
    }

    private static byte[] solidJpeg(int w, int h, int rgb) throws IOException {
        return encode(solidImage(w, h, rgb), "JPEG");
    }

    private static BufferedImage solidImage(int w, int h, int rgb) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    private static int indexOfColorStartingWith(java.util.List<String> colors, String prefix) {
        for (int i = 0; i < colors.size(); i++) {
            if (colors.get(i).startsWith(prefix)) return i;
        }
        return Integer.MAX_VALUE;
    }
}
