package com.lyncalor.calyra.suggestion;

import com.lyncalor.calyra.config.SuggestionProperties;
import com.lyncalor.calyra.preference.PreferenceService;
import com.lyncalor.calyra.preference.UserPreference;
import com.lyncalor.calyra.schedule.ScheduleDraft;
import com.lyncalor.calyra.vector.VectorCandidate;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "suggestion", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "qdrant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SuggestionEngine {

    private final VectorMemoryStore vectorMemoryStore;
    private final SuggestionProperties properties;
    private final PreferenceService preferenceService;
    private final Clock clock;

    public SuggestionEngine(VectorMemoryStore vectorMemoryStore,
                            SuggestionProperties properties,
                            PreferenceService preferenceService,
                            Clock clock) {
        this.vectorMemoryStore = vectorMemoryStore;
        this.properties = properties;
        this.preferenceService = preferenceService;
        this.clock = clock;
    }

    public SuggestionResult suggest(long chatId, String queryText, ScheduleDraft draft) {
        Instant now = clock.instant();
        List<Suggestion<?>> suggestions = new ArrayList<>();
        ScheduleSuggestions scheduleSuggestions = buildPreferenceSuggestions(chatId, draft, suggestions);

        List<VectorCandidate> candidates = vectorMemoryStore.search(
                queryText,
                chatId,
                properties.getMaxCandidatesForStats(),
                Duration.ofDays(properties.getLookbackDays())
        );
        if (!candidates.isEmpty()) {
            scheduleSuggestions = mergeSuggestions(scheduleSuggestions,
                    buildVectorSuggestions(candidates, draft, now, suggestions));
        }
        if (suggestions.isEmpty()) {
            return SuggestionResult.empty();
        }
        return new SuggestionResult(scheduleSuggestions, suggestions);
    }

    private ScheduleSuggestions buildPreferenceSuggestions(long chatId,
                                                           ScheduleDraft draft,
                                                           List<Suggestion<?>> suggestions) {
        UserPreference userPreference = preferenceService.getOrCreateUserPreference(chatId);
        Optional<com.lyncalor.calyra.preference.TypePreference> typePreference =
                preferenceService.getTypePreference(chatId, draft.type());

        Optional<Duration> duration = Optional.empty();
        Optional<String> location = Optional.empty();
        Optional<Integer> reminder = Optional.empty();

        if (draft.end() == null && draft.start() != null && typePreference.isPresent()
                && typePreference.get().getTypicalDurationMinutes() != null) {
            duration = Optional.of(Duration.ofMinutes(typePreference.get().getTypicalDurationMinutes()));
            suggestions.add(new Suggestion<>("duration", duration.get(), 0.95, "Based on your preferences."));
        }
        if ((draft.location() == null || draft.location().isBlank()) && typePreference.isPresent()
                && typePreference.get().getDefaultLocation() != null) {
            location = Optional.of(typePreference.get().getDefaultLocation());
            suggestions.add(new Suggestion<>("location", location.get(), 0.95, "Based on your " + draft.type() + " preferences."));
        }
        if (draft.remindMinutesBefore() == null || draft.remindMinutesBefore() == 30) {
            if (typePreference.isPresent() && typePreference.get().getDefaultRemindMinutes() != null) {
                reminder = Optional.of(typePreference.get().getDefaultRemindMinutes());
                suggestions.add(new Suggestion<>("reminder", reminder.get(), 0.95,
                        "Based on your " + draft.type() + " preferences."));
            } else if (userPreference.getDefaultRemindMinutes() != null) {
                reminder = Optional.of(userPreference.getDefaultRemindMinutes());
                suggestions.add(new Suggestion<>("reminder", reminder.get(), 0.9, "Based on your preferences."));
            }
        }
        // Type preferences only apply when a type is already known.

        return new ScheduleSuggestions(duration, location, reminder, Optional.empty(), Optional.empty());
    }

    private ScheduleSuggestions buildVectorSuggestions(List<VectorCandidate> candidates,
                                                       ScheduleDraft draft,
                                                       Instant now,
                                                       List<Suggestion<?>> suggestions) {
        Optional<Duration> duration = Optional.empty();
        Optional<String> location = Optional.empty();
        Optional<Integer> reminder = Optional.empty();
        Optional<String> type = Optional.empty();
        Optional<LocalTime> typicalStart = Optional.empty();

        if (draft.end() == null && draft.start() != null) {
            duration = computeDurationSuggestion(candidates, now, suggestions);
        }
        if (draft.location() == null || draft.location().isBlank()) {
            location = computeStringSuggestion(candidates, "location", now, "Location", suggestions);
        }
        if (draft.remindMinutesBefore() == null || draft.remindMinutesBefore() == 30) {
            reminder = computeIntSuggestion(candidates, "remindMinutesBefore", now, "Reminder", suggestions);
        }
        if (draft.type() == null || draft.type().isBlank() || "Other".equalsIgnoreCase(draft.type())) {
            type = computeStringSuggestion(candidates, "type", now, "Type", suggestions);
        }
        typicalStart = computeTypicalStart(candidates, now, suggestions);

        return new ScheduleSuggestions(duration, location, reminder, type, typicalStart);
    }

    private ScheduleSuggestions mergeSuggestions(ScheduleSuggestions base, ScheduleSuggestions fromVector) {
        Optional<Duration> duration = base.duration().isPresent() ? base.duration() : fromVector.duration();
        Optional<String> location = base.location().isPresent() ? base.location() : fromVector.location();
        Optional<Integer> reminder = base.remindMinutesBefore().isPresent() ? base.remindMinutesBefore() : fromVector.remindMinutesBefore();
        Optional<String> type = base.type().isPresent() ? base.type() : fromVector.type();
        Optional<LocalTime> typicalStart = base.typicalStartTime().isPresent() ? base.typicalStartTime() : fromVector.typicalStartTime();
        return new ScheduleSuggestions(duration, location, reminder, type, typicalStart);
    }

    private Optional<Duration> computeDurationSuggestion(List<VectorCandidate> candidates,
                                                         Instant now,
                                                         List<Suggestion<?>> suggestions) {
        Map<Long, Double> weights = new HashMap<>();
        int sample = 0;
        for (VectorCandidate candidate : candidates) {
            Instant start = toInstant(candidate.payload().get("startTs"));
            Instant end = toInstant(candidate.payload().get("endTs"));
            if (start == null || end == null || end.isBefore(start)) {
                continue;
            }
            long minutes = Duration.between(start, end).toMinutes();
            if (minutes <= 0) {
                continue;
            }
            weights.merge(minutes, weight(start, now), Double::sum);
            sample++;
        }
        if (weights.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<Long, Double> best = weights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (best == null) {
            return Optional.empty();
        }
        double confidence = best.getValue() / Math.max(1.0, sample);
        if (confidence >= properties.getMinConfidenceSuggest()) {
            suggestions.add(new Suggestion<>("duration", Duration.ofMinutes(best.getKey()),
                    confidence, rationale(sample)));
        }
        return Optional.of(Duration.ofMinutes(best.getKey()));
    }

    private Optional<String> computeStringSuggestion(List<VectorCandidate> candidates,
                                                     String key,
                                                     Instant now,
                                                     String label,
                                                     List<Suggestion<?>> suggestions) {
        Map<String, Double> weights = new HashMap<>();
        int sample = 0;
        for (VectorCandidate candidate : candidates) {
            Object value = candidate.payload().get(key);
            if (value == null) {
                continue;
            }
            String text = value.toString().trim();
            if (text.isBlank()) {
                continue;
            }
            weights.merge(text, weight(candidate.startTs(), now), Double::sum);
            sample++;
        }
        return buildTopStringSuggestion(weights, sample, label, suggestions);
    }

    private Optional<Integer> computeIntSuggestion(List<VectorCandidate> candidates,
                                                   String key,
                                                   Instant now,
                                                   String label,
                                                   List<Suggestion<?>> suggestions) {
        Map<Integer, Double> weights = new HashMap<>();
        int sample = 0;
        for (VectorCandidate candidate : candidates) {
            Object value = candidate.payload().get(key);
            if (value == null) {
                continue;
            }
            Integer intValue = parseInt(value);
            if (intValue == null) {
                continue;
            }
            weights.merge(intValue, weight(candidate.startTs(), now), Double::sum);
            sample++;
        }
        if (weights.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<Integer, Double> best = weights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (best == null) {
            return Optional.empty();
        }
        double confidence = best.getValue() / Math.max(1.0, sample);
        if (confidence >= properties.getMinConfidenceSuggest()) {
            suggestions.add(new Suggestion<>(label.toLowerCase(), best.getKey(), confidence, rationale(sample)));
        }
        return Optional.of(best.getKey());
    }

    private Optional<String> buildTopStringSuggestion(Map<String, Double> weights,
                                                      int sample,
                                                      String label,
                                                      List<Suggestion<?>> suggestions) {
        if (weights.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<String, Double> best = weights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (best == null) {
            return Optional.empty();
        }
        double confidence = best.getValue() / Math.max(1.0, sample);
        if (confidence >= properties.getMinConfidenceSuggest()) {
            suggestions.add(new Suggestion<>(label.toLowerCase(), best.getKey(), confidence, rationale(sample)));
        }
        return Optional.of(best.getKey());
    }

    private Optional<LocalTime> computeTypicalStart(List<VectorCandidate> candidates,
                                                    Instant now,
                                                    List<Suggestion<?>> suggestions) {
        Map<LocalTime, Double> weights = new HashMap<>();
        int sample = 0;
        for (VectorCandidate candidate : candidates) {
            Instant start = candidate.startTs();
            if (start == null) {
                continue;
            }
            LocalTime time = start.atZone(java.time.ZoneId.of("Europe/Berlin")).toLocalTime().withSecond(0).withNano(0);
            weights.merge(time, weight(start, now), Double::sum);
            sample++;
        }
        if (weights.isEmpty()) {
            return Optional.empty();
        }
        Map.Entry<LocalTime, Double> best = weights.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .orElse(null);
        if (best == null) {
            return Optional.empty();
        }
        double confidence = best.getValue() / Math.max(1.0, sample);
        if (confidence >= properties.getMinConfidenceSuggest()) {
            suggestions.add(new Suggestion<>("typicalStartTime", best.getKey(), confidence, rationale(sample)));
        }
        return Optional.of(best.getKey());
    }

    private double weight(Instant start, Instant now) {
        if (start == null || now == null) {
            return 1.0;
        }
        long days = Duration.between(start, now).toDays();
        if (days < 0) {
            days = 0;
        }
        double factor = Math.max(0.2, 1.0 - (days / (double) properties.getLookbackDays()));
        return factor;
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        try {
            long epoch = Long.parseLong(value.toString());
            return Instant.ofEpochSecond(epoch);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(Object value) {
        try {
            return Integer.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String rationale(int sample) {
        return "Based on your last " + sample + " similar events.";
    }
}
