package com.lyncalor.calyra.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "suggestion")
public class SuggestionProperties {

    private boolean enabled = true;
    private double minConfidenceAutoApply = 0.85;
    private double minConfidenceSuggest = 0.55;
    private int maxCandidatesForStats = 10;
    private int lookbackDays = 180;
    private boolean confirmationRequired = true;

    @AssertTrue(message = "suggestion settings must be valid")
    public boolean isValidConfig() {
        return minConfidenceAutoApply >= 0.0
                && minConfidenceSuggest >= 0.0
                && maxCandidatesForStats > 0
                && lookbackDays > 0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getMinConfidenceAutoApply() {
        return minConfidenceAutoApply;
    }

    public void setMinConfidenceAutoApply(double minConfidenceAutoApply) {
        this.minConfidenceAutoApply = minConfidenceAutoApply;
    }

    public double getMinConfidenceSuggest() {
        return minConfidenceSuggest;
    }

    public void setMinConfidenceSuggest(double minConfidenceSuggest) {
        this.minConfidenceSuggest = minConfidenceSuggest;
    }

    public int getMaxCandidatesForStats() {
        return maxCandidatesForStats;
    }

    public void setMaxCandidatesForStats(int maxCandidatesForStats) {
        this.maxCandidatesForStats = maxCandidatesForStats;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
