package com.brandextractor.domain.ports;

import com.brandextractor.domain.model.ExtractionResult;

public interface ExtractionNormalizationPort {

    /**
     * Normalises an {@link ExtractionResult} produced by the AI adapter.
     *
     * <p>Normalisation is non-destructive from the caller's perspective — a new
     * result is always returned; the original is never mutated. The returned
     * result may have:
     * <ul>
     *   <li>Text fields trimmed, blank optionals set to {@code null}, and values
     *       capped at documented maximum lengths.</li>
     *   <li>Colour values validated and cleared when they do not match
     *       {@code #RRGGBB} format.</li>
     *   <li>Asset URLs sanitised; entries with non-{@code http(s)} schemes are
     *       removed.</li>
     *   <li>Additional {@link com.brandextractor.domain.model.ExtractionWarning}
     *       entries appended when values were modified or dropped.</li>
     *   <li>A reduced {@link com.brandextractor.domain.model.ConfidenceScore}
     *       when the AI output conflicts with strong evidence signals.</li>
     * </ul>
     *
     * @param result a raw result straight from the AI adapter
     * @return a new, normalised result ready for post-extraction validation
     */
    ExtractionResult normalize(ExtractionResult result);
}
