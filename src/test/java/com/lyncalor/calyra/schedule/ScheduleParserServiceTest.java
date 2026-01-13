package com.lyncalor.calyra.schedule;

import com.lyncalor.calyra.config.LlmProperties;
import com.lyncalor.calyra.llm.LlmClientService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleParserServiceTest {

    @Test
    void retriesOnceAfterInvalidJson() {
        List<String> responses = new ArrayList<>();
        responses.add("{bad json");
        responses.add("""
                {
                  "title": "Meeting",
                  "start": "2025-01-01T10:00:00+01:00",
                  "end": null,
                  "type": "Other",
                  "location": null,
                  "notes": null,
                  "timezone": "Europe/Berlin",
                  "remindMinutesBefore": 30,
                  "needsClarification": false,
                  "clarificationQuestions": []
                }
                """);

        LlmClientService client = new StubLlmClientService(responses);
        ScheduleParserService service = new ScheduleParserService(client, new PromptFactory());

        ScheduleDraft draft = service.parse("tomorrow 10am meeting", Instant.parse("2025-01-01T08:00:00Z"),
                ZoneId.of("Europe/Berlin"));

        assertThat(draft.title()).isEqualTo("Meeting");
    }

    private static class StubLlmClientService extends LlmClientService {

        private final List<String> responses;
        private int index = 0;

        StubLlmClientService(List<String> responses) {
            super(RestClient.builder().baseUrl("http://localhost").build(), new LlmProperties());
            this.responses = responses;
        }

        @Override
        public String completeJson(String systemPrompt, String userPrompt) {
            if (index >= responses.size()) {
                return responses.get(responses.size() - 1);
            }
            return responses.get(index++);
        }
    }
}
