package com.lyncalor.calyra.preference;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final TypePreferenceRepository typePreferenceRepository;
    private final Clock clock;

    public PreferenceService(UserPreferenceRepository userPreferenceRepository,
                             TypePreferenceRepository typePreferenceRepository,
                             Clock clock) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.typePreferenceRepository = typePreferenceRepository;
        this.clock = clock;
    }

    public UserPreference getOrCreateUserPreference(long chatId) {
        return userPreferenceRepository.findByChatId(chatId)
                .orElseGet(() -> {
                    UserPreference preference = new UserPreference();
                    preference.setChatId(chatId);
                    preference.setUpdatedAt(clock.instant());
                    return userPreferenceRepository.save(preference);
                });
    }

    public Optional<TypePreference> getTypePreference(long chatId, String type) {
        if (type == null || type.isBlank()) {
            return Optional.empty();
        }
        return typePreferenceRepository.findByChatIdAndType(chatId, type);
    }

    public List<TypePreference> getAllTypePreferences(long chatId) {
        return typePreferenceRepository.findAllByChatId(chatId);
    }

    public UserPreference updateUserPreference(long chatId, UserPreferencePatch patch) {
        UserPreference preference = getOrCreateUserPreference(chatId);
        if (patch.defaultRemindMinutes() != null) {
            preference.setDefaultRemindMinutes(patch.defaultRemindMinutes());
        }
        if (patch.defaultTimezone() != null && !patch.defaultTimezone().isBlank()) {
            preference.setDefaultTimezone(patch.defaultTimezone());
        }
        if (patch.workingHoursStart() != null) {
            preference.setWorkingHoursStart(patch.workingHoursStart());
        }
        if (patch.workingHoursEnd() != null) {
            preference.setWorkingHoursEnd(patch.workingHoursEnd());
        }
        preference.setUpdatedAt(clock.instant());
        return userPreferenceRepository.save(preference);
    }

    public TypePreference updateTypePreference(long chatId, String type, TypePreferencePatch patch) {
        TypePreference preference = typePreferenceRepository.findByChatIdAndType(chatId, type)
                .orElseGet(() -> {
                    TypePreference created = new TypePreference();
                    created.setChatId(chatId);
                    created.setType(type);
                    return created;
                });
        if (patch.typicalDurationMinutes() != null) {
            preference.setTypicalDurationMinutes(patch.typicalDurationMinutes());
        }
        if (patch.defaultLocation() != null && !patch.defaultLocation().isBlank()) {
            preference.setDefaultLocation(patch.defaultLocation());
        }
        if (patch.defaultRemindMinutes() != null) {
            preference.setDefaultRemindMinutes(patch.defaultRemindMinutes());
        }
        preference.setUpdatedAt(clock.instant());
        return typePreferenceRepository.save(preference);
    }

    public void resetPreferences(long chatId) {
        userPreferenceRepository.findByChatId(chatId).ifPresent(userPreferenceRepository::delete);
        typePreferenceRepository.deleteAllByChatId(chatId);
    }
}
