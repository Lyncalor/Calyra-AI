package com.lyncalor.calyra.vector;

import com.lyncalor.calyra.config.RetrievalProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VectorRerankerTest {

    @Test
    void confidentMatchRequiresMargin() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.setMinScore(0.3);
        properties.setMinMargin(0.05);
        VectorReranker reranker = new VectorReranker();

        List<VectorCandidate> candidates = List.of(
                new VectorCandidate("1", 0.5, "Alpha", null, null, null, Map.of()),
                new VectorCandidate("2", 0.48, "Beta", null, null, null, Map.of())
        );

        VectorCandidate confident = reranker.confidentMatch(candidates, properties);

        assertThat(confident).isNull();
    }

    @Test
    void rerankAddsRecencyBonus() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.setDefaultLookbackDays(10);
        VectorReranker reranker = new VectorReranker();

        Instant now = Instant.parse("2025-01-10T00:00:00Z");
        List<VectorCandidate> candidates = List.of(
                new VectorCandidate("1", 0.4, "Alpha", Instant.parse("2025-01-09T00:00:00Z"), null, null, Map.of()),
                new VectorCandidate("2", 0.4, "Beta", Instant.parse("2024-12-01T00:00:00Z"), null, null, Map.of())
        );

        List<VectorCandidate> reranked = reranker.rerank(candidates, "alpha", now, properties);

        assertThat(reranked.get(0).id()).isEqualTo("1");
        assertThat(reranked.get(0).score()).isGreaterThan(reranked.get(1).score());
    }
}
