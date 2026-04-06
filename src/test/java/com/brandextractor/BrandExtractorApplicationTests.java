package com.brandextractor;

import com.google.cloud.vertexai.VertexAI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class BrandExtractorApplicationTests {

    @MockBean
    VertexAI vertexAI;

    @Test
    void contextLoads() {
    }
}
