package com.lyncalor.calyra.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private boolean enabled = true;
    private String botToken;
    private String botUsername;
    private List<String> pollingAllowedUpdates = new ArrayList<>();

    @AssertTrue(message = "telegram.bot-token must be set when telegram.enabled=true")
    public boolean isBotTokenValid() {
        if (!enabled) {
            return true;
        }
        return botToken != null && !botToken.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

    public List<String> getPollingAllowedUpdates() {
        return pollingAllowedUpdates;
    }

    public void setPollingAllowedUpdates(List<String> pollingAllowedUpdates) {
        this.pollingAllowedUpdates = pollingAllowedUpdates == null ? new ArrayList<>() : pollingAllowedUpdates;
    }
}
