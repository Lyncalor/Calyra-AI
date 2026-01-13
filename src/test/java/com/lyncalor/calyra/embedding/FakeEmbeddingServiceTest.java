package com.lyncalor.calyra.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FakeEmbeddingServiceTest {

    @Test
    void returnsDeterministicVector() {
        FakeEmbeddingService service = new FakeEmbeddingService(8);

        List<Float> first = service.embed("hello world");
        List<Float> second = service.embed("hello world");

        assertThat(first).containsExactlyElementsOf(second);
        assertThat(first).hasSize(8);
    }

    @Test
    void exposesDimension() {
        FakeEmbeddingService service = new FakeEmbeddingService(4);

        assertThat(service.dimension()).isEqualTo(4);
        assertThat(service.embed("test")).hasSize(4);
    }
}
