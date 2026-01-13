package com.lyncalor.calyra.suggestion;

import java.util.List;

public record SuggestionResult(
        ScheduleSuggestions suggestions,
        List<Suggestion<?>> suggestionList
) {
    public static SuggestionResult empty() {
        return new SuggestionResult(ScheduleSuggestions.empty(), List.of());
    }
}
