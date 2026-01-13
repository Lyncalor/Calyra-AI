package com.lyncalor.calyra.preference;

public record TypePreferencePatch(
        Integer typicalDurationMinutes,
        String defaultLocation,
        Integer defaultRemindMinutes
) {
}
