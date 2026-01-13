package com.lyncalor.calyra.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.notion.NotionClient;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramMessageServiceTest {

    @Test
    void startCommandReturnsWelcome() {
        TelegramMessageService service = new TelegramMessageService(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), new ObjectMapper());

        String response = service.buildReplyText("/start", 10L);

        assertThat(response).contains("Welcome");
    }

    @Test
    void nonStartTextIsEchoed() {
        TelegramMessageService service = new TelegramMessageService(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), new ObjectMapper());

        String response = service.buildReplyText("hello there", 10L);

        assertThat(response).isEqualTo("You said: hello there");
    }

    @Test
    void notionTestUsesClientWhenAvailable() {
        NotionClient client = new NotionClient() {
            @Override
            public java.util.Optional<String> createScheduleEntry(com.lyncalor.calyra.schedule.ScheduleDraft draft,
                                                                  String rawText,
                                                                  String source) {
                return java.util.Optional.of("page");
            }

            @Override
            public boolean createSimpleEntry(String title, String rawText, String source) {
                return true;
            }
        };
        TelegramMessageService service = new TelegramMessageService(Optional.of(client), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), new ObjectMapper());

        String response = service.buildReplyText("/notion_test", 42L);

        assertThat(response).isEqualTo("Notion test created.");
    }
}
