package com.lyncalor.calyra.memory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "llm.enabled=false",
        "working-memory.enabled=false",
        "telegram.enabled=false",
        "notion.enabled=false",
        "qdrant.enabled=false"
})
class PendingSelectionRepositoryTest {

    @Autowired
    private PendingSelectionRepository repository;

    @Test
    void saveAndFindByChatId() {
        PendingSelection selection = new PendingSelection();
        selection.setChatId(321L);
        selection.setCandidatesJson("[]");
        selection.setCreatedAt(Instant.now());
        selection.setUpdatedAt(Instant.now());
        selection.setExpiresAt(Instant.now().plusSeconds(60));
        repository.save(selection);

        PendingSelection loaded = repository.findByChatId(321L).orElseThrow();

        assertThat(loaded.getChatId()).isEqualTo(321L);
    }
}
