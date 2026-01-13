package com.lyncalor.calyra.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.SuggestionProperties;
import com.lyncalor.calyra.config.WorkingMemoryProperties;
import com.lyncalor.calyra.schedule.ScheduleDraft;
import com.lyncalor.calyra.schedule.ScheduleParserService;
import com.lyncalor.calyra.suggestion.ScheduleSuggestions;
import com.lyncalor.calyra.suggestion.Suggestion;
import com.lyncalor.calyra.suggestion.SuggestionEngine;
import com.lyncalor.calyra.suggestion.SuggestionResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@DataJpaTest(properties = {
        "llm.enabled=false",
        "working-memory.enabled=true",
        "telegram.enabled=false",
        "notion.enabled=false",
        "qdrant.enabled=false"
})
class ClarificationSuggestionFlowTest {

    @Autowired
    private PendingDraftRepository pendingDraftRepository;

    @Autowired
    private DedupeEntryRepository dedupeEntryRepository;

    @Test
    void acceptAppliesSuggestionsAndFinalizes() {
        ScheduleParserService parserService = Mockito.mock(ScheduleParserService.class);
        SuggestionEngine suggestionEngine = Mockito.mock(SuggestionEngine.class);

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
        Mockito.when(parserService.parse(any(), any(), any())).thenReturn(draft);

        ScheduleSuggestions suggestions = new ScheduleSuggestions(
                Optional.of(Duration.ofMinutes(60)),
                Optional.of("Office"),
                Optional.of(15),
                Optional.of("Meeting"),
                Optional.empty()
        );
        SuggestionResult suggestionResult = new SuggestionResult(
                suggestions,
                List.of(new Suggestion<>("duration", Duration.ofMinutes(60), 0.9, "Based on history"))
        );
        Mockito.when(suggestionEngine.suggest(any(), any(), any())).thenReturn(suggestionResult);

        WorkingMemoryProperties memoryProperties = new WorkingMemoryProperties();
        memoryProperties.setTtlMinutes(30);

        SuggestionProperties suggestionProperties = new SuggestionProperties();
        suggestionProperties.setConfirmationRequired(true);

        Clock clock = Clock.fixed(Instant.parse("2025-01-10T09:00:00Z"), ZoneOffset.UTC);

        ClarificationService service = new ClarificationService(
                pendingDraftRepository,
                dedupeEntryRepository,
                parserService,
                memoryProperties,
                clock,
                new ObjectMapper().findAndRegisterModules(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(suggestionEngine),
                suggestionProperties,
                Optional.empty()
        );

        ClarificationResult first = service.handleIncomingMessage(10L, "Schedule meeting");

        assertThat(first.reply()).contains("Suggested defaults");
        PendingDraft pending = pendingDraftRepository.findByChatId(10L).orElseThrow();
        assertThat(pending.getSuggestedJson()).isNotBlank();

        ClarificationResult accepted = service.handleIncomingMessage(10L, "accept");

        assertThat(accepted.reply()).contains("Parsed schedule");
        assertThat(dedupeEntryRepository.findAll()).isNotEmpty();
    }
}
