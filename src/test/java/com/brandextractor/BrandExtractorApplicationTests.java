package com.brandextractor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// vertexai.enabled defaults to false → MockBrandExtractionAiClient is active,
// no GCP credentials or project-id required for the context to load.
@SpringBootTest
class BrandExtractorApplicationTests {

    @Test
    void contextLoads() {
    }
}
