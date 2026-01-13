package com.lyncalor.calyra.embedding;

import com.lyncalor.calyra.config.EmbeddingProperties;
import com.lyncalor.calyra.config.LlmProperties;

import java.util.List;

public class OpenAiEmbeddingService implements EmbeddingService {

    private final EmbeddingProperties properties;
    private final LlmProperties llmProperties;

    public OpenAiEmbeddingService(EmbeddingProperties properties, LlmProperties llmProperties) {
        this.properties = properties;
        this.llmProperties = llmProperties;
    }

    @Override
    public int dimension() {
        throw new UnsupportedOperationException("OpenAI embeddings are not implemented yet");
    }

    @Override
    public List<Float> embed(String text) {
        throw new UnsupportedOperationException("OpenAI embeddings are not implemented yet");
    }
}
