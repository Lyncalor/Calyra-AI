package com.lyncalor.calyra.suggestion;

import com.lyncalor.calyra.config.SuggestionProperties;
import com.lyncalor.calyra.schedule.ScheduleDraft;
import com.lyncalor.calyra.preference.PreferenceService;
import com.lyncalor.calyra.preference.UserPreference;
import com.lyncalor.calyra.vector.VectorCandidate;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SuggestionEngineTest {

    @Test
    void suggestsDurationAndLocation() {
        SuggestionProperties properties = new SuggestionProperties();
        properties.setMinConfidenceSuggest(0.3);
        properties.setMaxCandidatesForStats(5);
        properties.setLookbackDays(180);

        Instant now = Instant.parse("2025-01-10T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        VectorMemoryStore store = new VectorMemoryStore() {
            @Override
            public void upsertEvent(com.lyncalor.calyra.vector.VectorEvent event) {
            }

            @Override
            public List<VectorCandidate> search(String queryText, long chatId, int limit, Duration lookback) {
                return List.of(
                        new VectorCandidate("1", 0.5, "A", Instant.parse("2025-01-08T10:00:00Z"), "meeting", null,
                                Map.of("startTs", 1736330400L, "endTs", 1736334000L, "location", "Office")),
                        new VectorCandidate("2", 0.4, "B", Instant.parse("2025-01-05T10:00:00Z"), "meeting", null,
                                Map.of("startTs", 1736071200L, "endTs", 1736074800L, "location", "Office"))
                );
            }
        };

        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        UserPreference userPreference = new UserPreference();
        userPreference.setChatId(10L);
        Mockito.when(preferenceService.getOrCreateUserPreference(10L)).thenReturn(userPreference);
        Mockito.when(preferenceService.getTypePreference(Mockito.anyLong(), Mockito.any()))
                .thenReturn(java.util.Optional.empty());

        SuggestionEngine engine = new SuggestionEngine(store, properties, preferenceService, clock);
        ScheduleDraft draft = new ScheduleDraft(
                "Title",
                Instant.parse("2025-01-10T10:00:00Z").atOffset(ZoneOffset.UTC),
                null,
                "Other",
                null,
                null,
                "Europe/Berlin",
                30,
                true,
                List.of("When?")
        );

        SuggestionResult result = engine.suggest(10L, "meeting", draft);

        assertThat(result.suggestionList()).isNotEmpty();
        assertThat(result.suggestions().duration()).isPresent();
        assertThat(result.suggestions().location()).contains("Office");
    }
}
