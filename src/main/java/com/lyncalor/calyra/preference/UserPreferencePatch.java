package com.lyncalor.calyra.preference;

import java.time.LocalTime;

public record UserPreferencePatch(
        Integer defaultRemindMinutes,
        String defaultTimezone,
        LocalTime workingHoursStart,
        LocalTime workingHoursEnd
) {
}
