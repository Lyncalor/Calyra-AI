package com.lyncalor.calyra.preference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "llm.enabled=false",
        "working-memory.enabled=false",
        "telegram.enabled=false",
        "notion.enabled=false",
        "qdrant.enabled=false"
})
@Import(PreferenceServiceTest.ClockConfig.class)
class PreferenceServiceTest {

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private TypePreferenceRepository typePreferenceRepository;

    @Autowired
    private Clock clock;

    @Test
    void updatesAndResetsPreferences() {
        PreferenceService service = new PreferenceService(userPreferenceRepository, typePreferenceRepository, clock);

        service.updateUserPreference(10L, new UserPreferencePatch(20, null, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        service.updateTypePreference(10L, "Meeting", new TypePreferencePatch(45, "Office", 15));

        assertThat(userPreferenceRepository.findByChatId(10L)).isPresent();
        assertThat(typePreferenceRepository.findAllByChatId(10L)).hasSize(1);

        service.resetPreferences(10L);

        assertThat(userPreferenceRepository.findByChatId(10L)).isEmpty();
        assertThat(typePreferenceRepository.findAllByChatId(10L)).isEmpty();
    }

    static class ClockConfig {
        @Bean
        Clock testClock() {
            return Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
