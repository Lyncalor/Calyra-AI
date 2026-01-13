package com.lyncalor.calyra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    public enum Provider {
        FAKE,
        OPENAI
    }

    private Provider provider = Provider.FAKE;
    private String model = "text-embedding-3-small";

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
