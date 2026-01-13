package com.lyncalor.calyra.preference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "llm.enabled=false",
        "working-memory.enabled=false",
        "telegram.enabled=false",
        "notion.enabled=false",
        "qdrant.enabled=false"
})
class UserPreferenceRepositoryTest {

    @Autowired
    private UserPreferenceRepository repository;

    @Test
    void findByChatIdWorks() {
        UserPreference preference = new UserPreference();
        preference.setChatId(101L);
        preference.setUpdatedAt(Instant.now());
        repository.save(preference);

        assertThat(repository.findByChatId(101L)).isPresent();
    }

    @Test
    void chatIdIsUnique() {
        UserPreference first = new UserPreference();
        first.setChatId(202L);
        first.setUpdatedAt(Instant.now());
        repository.save(first);

        UserPreference second = new UserPreference();
        second.setChatId(202L);
        second.setUpdatedAt(Instant.now());

        assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
