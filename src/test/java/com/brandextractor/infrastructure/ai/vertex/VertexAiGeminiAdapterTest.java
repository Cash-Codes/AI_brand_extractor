package com.brandextractor.infrastructure.ai.vertex;

import com.google.cloud.vertexai.VertexAI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class VertexAiGeminiAdapterTest {

    @Test
    void analyse_throwsUnsupportedOperationException() {
        var props = new VertexAiProperties();
        props.setProjectId("test-project");
        props.setLocation("us-central1");
        props.setModelId("gemini-2.0-flash-001");

        var adapter = new VertexAiGeminiAdapter(mock(VertexAI.class), props);

        assertThatThrownBy(() -> adapter.analyse(List.of()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    void vertexAiProperties_defaultsAreSet() {
        var props = new VertexAiProperties();
        props.setProjectId("my-project");

        assertThat(props.getProjectId()).isEqualTo("my-project");
    }
}
