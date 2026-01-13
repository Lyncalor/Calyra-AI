package com.lyncalor.calyra.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "retrieval")
public class RetrievalProperties {

    private double minScore = 0.35;
    private double minMargin = 0.05;
    private int defaultLookbackDays = 180;
    private int maxCandidates = 5;
    private int selectionTtlMinutes = 30;

    @AssertTrue(message = "retrieval settings must be positive")
    public boolean isValidConfig() {
        return minScore >= 0.0
                && minMargin >= 0.0
                && defaultLookbackDays > 0
                && maxCandidates > 0
                && selectionTtlMinutes > 0;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public double getMinMargin() {
        return minMargin;
    }

    public void setMinMargin(double minMargin) {
        this.minMargin = minMargin;
    }

    public int getDefaultLookbackDays() {
        return defaultLookbackDays;
    }

    public void setDefaultLookbackDays(int defaultLookbackDays) {
        this.defaultLookbackDays = defaultLookbackDays;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public int getSelectionTtlMinutes() {
        return selectionTtlMinutes;
    }

    public void setSelectionTtlMinutes(int selectionTtlMinutes) {
        this.selectionTtlMinutes = selectionTtlMinutes;
    }
}
