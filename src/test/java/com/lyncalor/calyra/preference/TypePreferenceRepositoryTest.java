package com.lyncalor.calyra.preference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "llm.enabled=false",
        "working-memory.enabled=false",
        "telegram.enabled=false",
        "notion.enabled=false",
        "qdrant.enabled=false"
})
class TypePreferenceRepositoryTest {

    @Autowired
    private TypePreferenceRepository repository;

    @Test
    void chatIdAndTypeAreUnique() {
        TypePreference first = new TypePreference();
        first.setChatId(101L);
        first.setType("Meeting");
        first.setUpdatedAt(Instant.now());
        repository.save(first);

        TypePreference second = new TypePreference();
        second.setChatId(101L);
        second.setType("Meeting");
        second.setUpdatedAt(Instant.now());

        assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
