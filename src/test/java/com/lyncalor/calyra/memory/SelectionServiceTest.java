package com.lyncalor.calyra.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.RetrievalProperties;
import com.lyncalor.calyra.vector.VectorCandidate;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "llm.enabled=false",
        "working-memory.enabled=false",
        "telegram.enabled=false",
        "notion.enabled=false",
        "qdrant.enabled=false"
})
class SelectionServiceTest {

    @Autowired
    private PendingSelectionRepository repository;

    @Test
    void storesSelectionAndResolvesChoice() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.setMaxCandidates(2);
        properties.setSelectionTtlMinutes(30);
        properties.setMinScore(0.9);
        properties.setMinMargin(0.2);

        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        VectorMemoryStore store = new VectorMemoryStore() {
            @Override
            public void upsertEvent(com.lyncalor.calyra.vector.VectorEvent event) {
            }

            @Override
            public List<VectorCandidate> search(String queryText, long chatId, int limit, Duration lookback) {
                return List.of(
                        new VectorCandidate("id-1", 0.5, "Meeting A", null, "meeting", null, Map.of()),
                        new VectorCandidate("id-2", 0.4, "Meeting B", null, "meeting", null, Map.of())
                );
            }
        };

        SelectionService service = new SelectionService(repository, store, properties, clock, objectMapper);

        String reply = service.findCandidates(10L, "meeting");

        assertThat(reply).contains("Reply with a number");
        assertThat(repository.findByChatId(10L)).isPresent();

        String selectionReply = service.handleSelectionReply(10L, "1").orElse("");

        assertThat(selectionReply).contains("Selected event");
        assertThat(repository.findByChatId(10L)).isEmpty();
    }
}
