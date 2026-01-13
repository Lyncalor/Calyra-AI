package com.lyncalor.calyra.memory;

import com.lyncalor.calyra.schedule.ScheduleDraft;
import com.lyncalor.calyra.schedule.ScheduleParserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "telegram.enabled=false",
        "notion.enabled=false",
        "llm.enabled=true",
        "llm.api-key=test",
        "working-memory.enabled=true",
        "working-memory.ttl-minutes=30",
        "qdrant.enabled=false"
})
class ClarificationServiceTest {

    private static final Instant NOW = Instant.parse("2025-01-01T10:00:00Z");

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        Clock testClock() {
            return Clock.fixed(NOW, ZoneId.of("UTC"));
        }
    }

    @Autowired
    private ClarificationService clarificationService;

    @Autowired
    private PendingDraftRepository repository;

    @MockBean
    private ScheduleParserService scheduleParserService;

    @Test
    void newMessageNeedingClarificationCreatesPendingDraft() {
        ScheduleDraft draft = new ScheduleDraft(
                "Meet",
                null,
                null,
                null,
                null,
                null,
                "Europe/Berlin",
                30,
                true,
                List.of("What time?")
        );
        when(scheduleParserService.parse(any(), any(), any())).thenReturn(draft);

        ClarificationResult result = clarificationService.handleIncomingMessage(123L, "Meet tomorrow");

        assertThat(result.reply()).contains("What time?");
        PendingDraft pending = repository.findByChatId(123L).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
        assertThat(pending.getExpiresAt()).isEqualTo(NOW.plusSeconds(1800));
    }

    @Test
    void replyCompletesDraftAndMarksReady() {
        PendingDraft existing = new PendingDraft();
        existing.setChatId(222L);
        existing.setRawInitialText("Meet tomorrow");
        existing.setDraftJson("{}");
        existing.setStatus(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
        existing.setCreatedAt(NOW.minusSeconds(60));
        existing.setUpdatedAt(NOW.minusSeconds(60));
        existing.setExpiresAt(NOW.plusSeconds(1800));
        repository.save(existing);

        ScheduleDraft complete = new ScheduleDraft(
                "Meet",
                OffsetDateTime.ofInstant(NOW, ZoneId.of("Europe/Berlin")),
                null,
                "Other",
                null,
                null,
                "Europe/Berlin",
                30,
                false,
                List.of()
        );
        when(scheduleParserService.parse(any(), any(), any())).thenReturn(complete);

        ClarificationResult result = clarificationService.handleIncomingMessage(222L, "At 3pm");

        assertThat(result.reply()).contains("Parsed schedule");
        PendingDraft pending = repository.findByChatId(222L).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(PendingDraftStatus.READY);
    }

    @Test
    void expiredDraftIsResetAndReprocessed() {
        PendingDraft existing = new PendingDraft();
        existing.setChatId(333L);
        existing.setRawInitialText("Old request");
        existing.setDraftJson("{}");
        existing.setStatus(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
        existing.setCreatedAt(NOW.minusSeconds(4000));
        existing.setUpdatedAt(NOW.minusSeconds(4000));
        existing.setExpiresAt(NOW.minusSeconds(10));
        repository.save(existing);

        ScheduleDraft draft = new ScheduleDraft(
                "New",
                null,
                null,
                null,
                null,
                null,
                "Europe/Berlin",
                30,
                true,
                List.of("Which day?")
        );
        when(scheduleParserService.parse(any(), any(), any())).thenReturn(draft);

        ClarificationResult result = clarificationService.handleIncomingMessage(333L, "New request");

        assertThat(result.reply()).contains("Previous draft expired");
        PendingDraft pending = repository.findByChatId(333L).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
    }
}
