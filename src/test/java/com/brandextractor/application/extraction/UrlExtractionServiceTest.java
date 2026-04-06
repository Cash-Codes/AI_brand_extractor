package com.brandextractor.application.extraction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlExtractionServiceTest {

    @Test
    void extract_throwsUnsupportedOperationException() {
        var service = new UrlExtractionService();

        assertThatThrownBy(() -> service.extract("https://example.com"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }
}
