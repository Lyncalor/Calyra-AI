package com.lyncalor.calyra.suggestion;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

public record ScheduleSuggestions(
        Optional<Duration> duration,
        Optional<String> location,
        Optional<Integer> remindMinutesBefore,
        Optional<String> type,
        Optional<LocalTime> typicalStartTime
) {
    public static ScheduleSuggestions empty() {
        return new ScheduleSuggestions(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
