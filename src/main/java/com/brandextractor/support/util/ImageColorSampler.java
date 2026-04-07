package com.brandextractor.support.util;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid-samples a {@link BufferedImage} and returns the most frequent quantised colours.
 *
 * <p>Each RGB channel is quantised to 8 buckets ({@value QUANTIZE_STEP} per step) so that
 * near-identical shades collapse into a single representative hex value. Fully-transparent
 * pixels (alpha &lt; 128) are skipped, making it safe to use on PNG logos with transparency.
 */
public final class ImageColorSampler {

    public  static final int MAX_DOMINANT_COLORS = 5;
    private static final int MAX_SAMPLE_PIXELS   = 10_000;
    private static final int QUANTIZE_STEP       = 32;

    private ImageColorSampler() {}

    /**
     * Returns up to {@value MAX_DOMINANT_COLORS} dominant hex colours ordered by
     * frequency (most frequent first).
     */
    public static List<String> dominantColors(BufferedImage image) {
        int step = Math.max(1,
                (int) Math.sqrt((long) image.getWidth() * image.getHeight() / MAX_SAMPLE_PIXELS));

        Map<Integer, Integer> counts = new HashMap<>();

        for (int y = 0; y < image.getHeight(); y += step) {
            for (int x = 0; x < image.getWidth(); x += step) {
                int argb  = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) continue;
                int quantized = quantizeRgb(argb & 0x00FFFFFF);
                counts.merge(quantized, 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(MAX_DOMINANT_COLORS)
                .map(e -> String.format("#%06X", e.getKey()))
                .toList();
    }

    private static int quantizeRgb(int rgb) {
        int r = ((rgb >> 16) & 0xFF) / QUANTIZE_STEP * QUANTIZE_STEP;
        int g = ((rgb >>  8) & 0xFF) / QUANTIZE_STEP * QUANTIZE_STEP;
        int b = ( rgb        & 0xFF) / QUANTIZE_STEP * QUANTIZE_STEP;
        return (r << 16) | (g << 8) | b;
    }
}
