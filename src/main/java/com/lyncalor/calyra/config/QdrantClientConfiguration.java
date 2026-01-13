package com.lyncalor.calyra.config;

import com.lyncalor.calyra.embedding.EmbeddingService;
import com.lyncalor.calyra.qdrant.QdrantVectorMemoryStore;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
public class QdrantClientConfiguration {

    @Bean
    @Qualifier("qdrantRestClient")
    @ConditionalOnProperty(prefix = "qdrant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestClient qdrantRestClient(QdrantProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "qdrant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public VectorMemoryStore vectorMemoryStore(@Qualifier("qdrantRestClient") RestClient restClient,
                                               QdrantProperties properties,
                                               EmbeddingService embeddingService,
                                               Clock clock) {
        return new QdrantVectorMemoryStore(restClient, properties, embeddingService, clock);
    }
}
