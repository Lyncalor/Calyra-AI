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
class PendingDraftRepositoryTest {

    @Autowired
    private PendingDraftRepository repository;

    @Test
    void saveAndFindByChatId() {
        PendingDraft draft = new PendingDraft();
        draft.setChatId(999L);
        draft.setRawInitialText("Initial");
        draft.setDraftJson("{}");
        draft.setStatus(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
        draft.setCreatedAt(Instant.now());
        draft.setUpdatedAt(Instant.now());
        draft.setExpiresAt(Instant.now().plusSeconds(60));
        repository.save(draft);

        PendingDraft loaded = repository.findByChatId(999L).orElseThrow();

        assertThat(loaded.getChatId()).isEqualTo(999L);
        assertThat(loaded.getStatus()).isEqualTo(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
    }
}
