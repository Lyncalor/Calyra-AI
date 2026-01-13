package com.lyncalor.calyra.preference;

import com.lyncalor.calyra.schedule.ScheduleDraft;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "preference-learning", name = "enabled", havingValue = "true")
public class PreferenceLearningService {

    private final PreferenceLearningProperties properties;
    private final EventStatRepository eventStatRepository;
    private final PreferenceService preferenceService;
    private final Clock clock;

    public PreferenceLearningService(PreferenceLearningProperties properties,
                                     EventStatRepository eventStatRepository,
                                     PreferenceService preferenceService,
                                     Clock clock) {
        this.properties = properties;
        this.eventStatRepository = eventStatRepository;
        this.preferenceService = preferenceService;
        this.clock = clock;
    }

    public void learnFromEvent(long chatId, ScheduleDraft draft) {
        if (draft == null || draft.title() == null) {
            return;
        }
        EventStat stat = new EventStat();
        stat.setChatId(chatId);
        stat.setType(draft.type());
        if (draft.start() != null && draft.end() != null) {
            stat.setDurationMinutes((int) Duration.between(draft.start(), draft.end()).toMinutes());
        }
        stat.setLocation(draft.location());
        stat.setRemindMinutes(draft.remindMinutesBefore());
        stat.setCreatedAt(clock.instant());
        eventStatRepository.save(stat);

        Instant cutoff = clock.instant().minus(Duration.ofDays(properties.getLookbackDays()));
        if (draft.type() != null && !draft.type().isBlank()) {
            List<EventStat> typeStats = eventStatRepository
                    .findByChatIdAndTypeAndCreatedAtAfter(chatId, draft.type(), cutoff);
            if (typeStats.size() >= properties.getMinSamples()) {
                updateTypePreference(chatId, draft.type(), typeStats);
            }
        }
        List<EventStat> allStats = eventStatRepository.findByChatIdAndCreatedAtAfter(chatId, cutoff);
        if (allStats.size() >= properties.getMinSamples()) {
            updateUserPreference(chatId, allStats);
        }
    }

    private void updateTypePreference(long chatId, String type, List<EventStat> stats) {
        Integer duration = aggregateInt(stats, EventStat::getDurationMinutes);
        String location = aggregateString(stats, EventStat::getLocation);
        Integer remind = aggregateInt(stats, EventStat::getRemindMinutes);
        preferenceService.updateTypePreference(chatId, type, new TypePreferencePatch(duration, location, remind));
    }

    private void updateUserPreference(long chatId, List<EventStat> stats) {
        Integer remind = aggregateInt(stats, EventStat::getRemindMinutes);
        if (remind != null) {
            preferenceService.updateUserPreference(chatId, new UserPreferencePatch(remind, null, null, null));
        }
    }

    private Integer aggregateInt(List<EventStat> stats, Function<EventStat, Integer> extractor) {
        List<Integer> values = stats.stream()
                .map(extractor)
                .filter(value -> value != null && value > 0)
                .toList();
        if (values.size() < properties.getMinSamples()) {
            return null;
        }
        return switch (properties.getUpdateStrategy()) {
            case MEAN -> (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
            case MODE -> mode(values).orElse(null);
            case MEDIAN -> median(values);
        };
    }

    private String aggregateString(List<EventStat> stats, Function<EventStat, String> extractor) {
        List<String> values = stats.stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .toList();
        if (values.size() < properties.getMinSamples()) {
            return null;
        }
        return mode(values).orElse(null);
    }

    private Integer median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(mid);
        }
        return (sorted.get(mid - 1) + sorted.get(mid)) / 2;
    }

    private <T> Optional<T> mode(List<T> values) {
        Map<T, Long> counts = values.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }
}
