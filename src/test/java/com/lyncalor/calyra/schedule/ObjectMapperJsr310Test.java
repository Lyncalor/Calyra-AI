package com.lyncalor.calyra.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "telegram.enabled=false",
        "notion.enabled=false",
        "llm.enabled=false",
        "working-memory.enabled=false",
        "qdrant.enabled=false"
})
class ObjectMapperJsr310Test {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void parsesOffsetDateTime() throws Exception {
        String json = """
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
                """;

        ScheduleDraft draft = objectMapper.readValue(json, ScheduleDraft.class);

        assertThat(draft.start()).isEqualTo(OffsetDateTime.parse("2025-01-01T10:00:00+01:00"));
    }
}
