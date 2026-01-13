package com.lyncalor.calyra.preference;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PrefsCommandService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final PreferenceService preferenceService;

    public PrefsCommandService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    public Optional<String> handle(long chatId, String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("/prefs")) {
            return Optional.empty();
        }
        if (trimmed.equals("/prefs")) {
            return Optional.of(renderPreferences(chatId));
        }
        if (trimmed.startsWith("/prefs reset")) {
            return Optional.of(handleReset(chatId, trimmed));
        }
        if (trimmed.startsWith("/prefs set")) {
            return Optional.of(handleSet(chatId, trimmed));
        }
        return Optional.of(helpText());
    }

    private String handleReset(long chatId, String text) {
        if (text.trim().equalsIgnoreCase("/prefs reset YES")) {
            preferenceService.resetPreferences(chatId);
            return "Preferences reset.";
        }
        return "Confirm reset with: /prefs reset YES";
    }

    private String handleSet(long chatId, String text) {
        String remaining = text.substring("/prefs set".length()).trim();
        if (remaining.startsWith("remind")) {
            String value = remaining.substring("remind".length()).trim();
            Integer minutes = parseInt(value);
            if (minutes == null) {
                return helpText();
            }
            preferenceService.updateUserPreference(chatId, new UserPreferencePatch(minutes, null, null, null));
            return "Default reminder set to " + minutes + " minutes.";
        }
        if (remaining.startsWith("hours")) {
            String value = remaining.substring("hours".length()).trim();
            String[] parts = value.split("-");
            if (parts.length != 2) {
                return helpText();
            }
            LocalTime start = parseTime(parts[0].trim());
            LocalTime end = parseTime(parts[1].trim());
            if (start == null || end == null) {
                return helpText();
            }
            preferenceService.updateUserPreference(chatId, new UserPreferencePatch(null, null, start, end));
            return "Working hours set to " + TIME_FORMAT.format(start) + "-" + TIME_FORMAT.format(end) + ".";
        }
        if (remaining.startsWith("type")) {
            String typePart = remaining.substring("type".length()).trim();
            return handleTypePreference(chatId, typePart);
        }
        return helpText();
    }

    private String handleTypePreference(long chatId, String input) {
        String[] tokens = input.split("\\s+");
        if (tokens.length < 3) {
            return helpText();
        }
        String type = tokens[0];
        String field = tokens[1].toLowerCase(Locale.ROOT);
        String value = input.substring(input.indexOf(field) + field.length()).trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        TypePreferencePatch patch = switch (field) {
            case "duration" -> new TypePreferencePatch(parseInt(value), null, null);
            case "location" -> new TypePreferencePatch(null, value, null);
            case "remind" -> new TypePreferencePatch(null, null, parseInt(value));
            default -> null;
        };
        if (patch == null) {
            return helpText();
        }
        preferenceService.updateTypePreference(chatId, type, patch);
        return "Type preferences updated for " + type + ".";
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text.trim(), TIME_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private String renderPreferences(long chatId) {
        UserPreference user = preferenceService.getOrCreateUserPreference(chatId);
        List<TypePreference> types = preferenceService.getAllTypePreferences(chatId);
        StringBuilder builder = new StringBuilder("Preferences:\n");
        builder.append("Default reminder: ").append(valueOrDash(user.getDefaultRemindMinutes())).append('\n');
        builder.append("Timezone: ").append(valueOrDash(user.getDefaultTimezone())).append('\n');
        builder.append("Working hours: ").append(formatHours(user.getWorkingHoursStart(), user.getWorkingHoursEnd()))
                .append('\n');
        if (types.isEmpty()) {
            builder.append("Type defaults: none");
            return builder.toString();
        }
        builder.append("Type defaults:\n");
        for (TypePreference type : types) {
            builder.append("- ").append(type.getType()).append(": ");
            builder.append("duration=").append(valueOrDash(type.getTypicalDurationMinutes())).append(", ");
            builder.append("location=").append(valueOrDash(type.getDefaultLocation())).append(", ");
            builder.append("remind=").append(valueOrDash(type.getDefaultRemindMinutes())).append('\n');
        }
        return builder.toString().trim();
    }

    private String formatHours(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return "-";
        }
        return TIME_FORMAT.format(start) + "-" + TIME_FORMAT.format(end);
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String helpText() {
        return """
                Usage:
                /prefs
                /prefs set remind <minutes>
                /prefs set hours HH:mm-HH:mm
                /prefs set type <Type> duration <minutes>
                /prefs set type <Type> location \"Place\"
                /prefs set type <Type> remind <minutes>
                /prefs reset YES
                """.trim();
    }
}
