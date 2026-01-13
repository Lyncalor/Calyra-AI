package com.lyncalor.calyra.notion;

import com.lyncalor.calyra.config.NotionProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotionPayloadBuilder {

    public Map<String, Object> buildCreatePagePayload(NotionProperties properties,
                                                      String title,
                                                      String rawText,
                                                      String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("parent", Map.of("database_id", properties.getDatabaseId()));

        Map<String, Object> props = new LinkedHashMap<>();
        props.put(properties.getPropertyNameTitle(), titleProperty(title));

        if (source != null && !source.isBlank() && properties.getPropertyNameSource() != null
                && !properties.getPropertyNameSource().isBlank()) {
            props.put(properties.getPropertyNameSource(), sourceProperty(properties, source));
        }
        if (rawText != null && !rawText.isBlank() && properties.getPropertyNameRaw() != null
                && !properties.getPropertyNameRaw().isBlank()) {
            props.put(properties.getPropertyNameRaw(), richTextProperty(rawText));
        }

        payload.put("properties", props);
        return payload;
    }

    public Map<String, Object> buildCreatePagePayload(NotionProperties properties,
                                                      com.lyncalor.calyra.schedule.ScheduleDraft draft,
                                                      String rawText,
                                                      String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("parent", Map.of("database_id", properties.getDatabaseId()));

        Map<String, Object> props = new LinkedHashMap<>();
        props.put(properties.getPropertyNameTitle(), titleProperty(draft.title()));

        if (source != null && !source.isBlank() && properties.getPropertyNameSource() != null
                && !properties.getPropertyNameSource().isBlank()) {
            props.put(properties.getPropertyNameSource(), sourceProperty(properties, source));
        }
        if (rawText != null && !rawText.isBlank() && properties.getPropertyNameRaw() != null
                && !properties.getPropertyNameRaw().isBlank()) {
            props.put(properties.getPropertyNameRaw(), richTextProperty(rawText));
        }
        if (draft.start() != null && properties.getPropertyNameStart() != null
                && !properties.getPropertyNameStart().isBlank()) {
            props.put(properties.getPropertyNameStart(), dateProperty(draft.start(), draft.end()));
        }
        if (draft.end() != null && properties.getPropertyNameEnd() != null
                && !properties.getPropertyNameEnd().isBlank()) {
            props.put(properties.getPropertyNameEnd(), dateProperty(draft.end(), null));
        }
        if (draft.type() != null && !draft.type().isBlank() && properties.getPropertyNameType() != null
                && !properties.getPropertyNameType().isBlank()) {
            props.put(properties.getPropertyNameType(), Map.of("select", Map.of("name", draft.type())));
        }
        if (draft.location() != null && !draft.location().isBlank() && properties.getPropertyNameLocation() != null
                && !properties.getPropertyNameLocation().isBlank()) {
            props.put(properties.getPropertyNameLocation(), richTextProperty(draft.location()));
        }
        if (draft.remindMinutesBefore() != null && properties.getPropertyNameReminder() != null
                && !properties.getPropertyNameReminder().isBlank()) {
            props.put(properties.getPropertyNameReminder(), Map.of("number", draft.remindMinutesBefore()));
        }

        payload.put("properties", props);
        return payload;
    }

    private Map<String, Object> titleProperty(String title) {
        return Map.of("title", List.of(textObject(title)));
    }

    private Map<String, Object> sourceProperty(NotionProperties properties, String source) {
        if (properties.getPropertyTypeSource() == NotionProperties.SourcePropertyType.RICH_TEXT) {
            return richTextProperty(source);
        }
        return Map.of("select", Map.of("name", source));
    }

    private Map<String, Object> richTextProperty(String value) {
        return Map.of("rich_text", List.of(textObject(value)));
    }

    private Map<String, Object> dateProperty(java.time.OffsetDateTime start, java.time.OffsetDateTime end) {
        Map<String, Object> date = new LinkedHashMap<>();
        date.put("start", start.toString());
        if (end != null) {
            date.put("end", end.toString());
        }
        return Map.of("date", date);
    }

    private Map<String, Object> textObject(String value) {
        return Map.of("text", Map.of("content", value));
    }
}
