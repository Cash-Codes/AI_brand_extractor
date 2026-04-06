package com.brandextractor;

import com.google.cloud.vertexai.VertexAI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BrandExtractorApplicationTests {

    @MockitoBean
    VertexAI vertexAI;

    @Test
    void contextLoads() {
    }
}
