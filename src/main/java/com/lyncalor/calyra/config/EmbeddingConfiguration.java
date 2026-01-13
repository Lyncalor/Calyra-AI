package com.lyncalor.calyra.config;

import com.lyncalor.calyra.embedding.EmbeddingService;
import com.lyncalor.calyra.embedding.FakeEmbeddingService;
import com.lyncalor.calyra.embedding.OpenAiEmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "fake", matchIfMissing = true)
    public EmbeddingService fakeEmbeddingService(QdrantProperties qdrantProperties) {
        return new FakeEmbeddingService(qdrantProperties.getVectorSize());
    }

    @Bean
    @ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "openai")
    public EmbeddingService openAiEmbeddingService(EmbeddingProperties embeddingProperties,
                                                   LlmProperties llmProperties) {
        return new OpenAiEmbeddingService(embeddingProperties, llmProperties);
    }
}
