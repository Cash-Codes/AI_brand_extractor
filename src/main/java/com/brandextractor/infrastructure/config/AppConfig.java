package com.brandextractor.infrastructure.config;

import com.brandextractor.infrastructure.ai.vertex.VertexAiExtractionProperties;
import com.google.cloud.vertexai.VertexAI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(VertexAiExtractionProperties.class)
public class AppConfig {

    private final VertexAiExtractionProperties props;

    public AppConfig(VertexAiExtractionProperties props) {
        this.props = props;
    }

    @Bean
    @Lazy
    public VertexAI vertexAI() throws IOException {
        return new VertexAI(props.getProjectId(), props.getLocation());
    }
}
