package com.lyncalor.calyra.preference;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "preference-learning")
public class PreferenceLearningProperties {

    public enum UpdateStrategy {
        MEDIAN,
        MEAN,
        MODE
    }

    private boolean enabled = false;
    private int minSamples = 3;
    private int lookbackDays = 180;
    private UpdateStrategy updateStrategy = UpdateStrategy.MEDIAN;

    @AssertTrue(message = "preference-learning settings must be valid")
    public boolean isValidConfig() {
        return minSamples > 0 && lookbackDays > 0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinSamples() {
        return minSamples;
    }

    public void setMinSamples(int minSamples) {
        this.minSamples = minSamples;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public UpdateStrategy getUpdateStrategy() {
        return updateStrategy;
    }

    public void setUpdateStrategy(UpdateStrategy updateStrategy) {
        this.updateStrategy = updateStrategy;
    }
}
