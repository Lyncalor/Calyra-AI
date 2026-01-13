package com.lyncalor.calyra.preference;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PrefsCommandServiceTest {

    @Test
    void setRemindParsesMinutes() {
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PrefsCommandService service = new PrefsCommandService(preferenceService);

        Optional<String> reply = service.handle(10L, "/prefs set remind 20");

        ArgumentCaptor<UserPreferencePatch> captor = ArgumentCaptor.forClass(UserPreferencePatch.class);
        Mockito.verify(preferenceService).updateUserPreference(Mockito.eq(10L), captor.capture());
        assertThat(captor.getValue().defaultRemindMinutes()).isEqualTo(20);
        assertThat(reply).isPresent();
    }

    @Test
    void setTypeLocationParsesValue() {
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PrefsCommandService service = new PrefsCommandService(preferenceService);

        Optional<String> reply = service.handle(10L, "/prefs set type Meeting location \"TUM Library\"");

        ArgumentCaptor<TypePreferencePatch> captor = ArgumentCaptor.forClass(TypePreferencePatch.class);
        Mockito.verify(preferenceService).updateTypePreference(Mockito.eq(10L), Mockito.eq("Meeting"), captor.capture());
        assertThat(captor.getValue().defaultLocation()).isEqualTo("TUM Library");
        assertThat(reply).isPresent();
    }

    @Test
    void resetRequiresYes() {
        PreferenceService preferenceService = Mockito.mock(PreferenceService.class);
        PrefsCommandService service = new PrefsCommandService(preferenceService);

        Optional<String> reply = service.handle(10L, "/prefs reset");

        Mockito.verify(preferenceService, Mockito.never()).resetPreferences(Mockito.anyLong());
        assertThat(reply).isPresent();

        service.handle(10L, "/prefs reset YES");
        Mockito.verify(preferenceService).resetPreferences(10L);
    }
}
