package com.lyncalor.calyra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "working-memory")
public class WorkingMemoryProperties {

    private boolean enabled = true;
    private int ttlMinutes = 30;
    private int maxPendingPerChat = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public int getMaxPendingPerChat() {
        return maxPendingPerChat;
    }

    public void setMaxPendingPerChat(int maxPendingPerChat) {
        this.maxPendingPerChat = maxPendingPerChat;
    }
}
