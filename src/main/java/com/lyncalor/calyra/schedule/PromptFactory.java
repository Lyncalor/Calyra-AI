package com.lyncalor.calyra.schedule;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class PromptFactory {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    public String buildSystemPrompt() {
        return """
                You are a scheduling assistant. Output ONLY valid JSON that matches this schema:
                {
                  "title": "string, required",
                  "start": "ISO-8601 OffsetDateTime or null",
                  "end": "ISO-8601 OffsetDateTime or null",
                  "type": "string or null (default to Other if missing)",
                  "location": "string or null",
                  "notes": "string or null",
                  "timezone": "string, required (default Europe/Berlin)",
                  "remindMinutesBefore": "integer or null (default 30)",
                  "needsClarification": "boolean",
                  "clarificationQuestions": "array of strings"
                }
                Rules:
                - Output ONLY JSON. No markdown, no extra text.
                - Use timezone Europe/Berlin unless the user specifies another timezone.
                - Do NOT guess missing info. If date/time/duration is missing, set needsClarification=true and include questions.
                - If needsClarification=false, include start.
                """;
    }

    public String buildUserPrompt(String text, Instant now, ZoneId zoneId) {
        String nowText = now == null ? "unknown" : ISO_INSTANT.format(now);
        String zoneText = zoneId == null ? "Europe/Berlin" : zoneId.getId();
        return """
                Current time: %s
                Timezone context: %s
                User request: %s
                """.formatted(nowText, zoneText, text);
    }

    public String buildRetryPrompt(String invalidOutput, String errorSummary) {
        return """
                The previous output was invalid JSON or failed validation.
                Fix it and return ONLY valid JSON matching the schema. No extra text.
                Validation errors: %s
                Previous output: %s
                """.formatted(errorSummary, invalidOutput);
    }
}
