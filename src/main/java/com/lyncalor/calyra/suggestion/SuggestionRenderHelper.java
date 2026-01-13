package com.lyncalor.calyra.suggestion;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SuggestionRenderHelper {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public String render(List<Suggestion<?>> suggestions) {
        if (suggestions.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("Suggested defaults:\n");
        for (Suggestion<?> suggestion : suggestions) {
            builder.append("- ")
                    .append(suggestion.fieldName())
                    .append(": ")
                    .append(formatValue(suggestion.suggestedValue()))
                    .append(" (")
                    .append(String.format("%.0f%%", suggestion.confidence() * 100))
                    .append(")")
                    .append(" — ")
                    .append(suggestion.rationale())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "n/a";
        }
        if (value instanceof Duration duration) {
            long minutes = duration.toMinutes();
            return minutes + " minutes";
        }
        if (value instanceof LocalTime time) {
            return TIME_FORMAT.format(time);
        }
        return value.toString();
    }
}
