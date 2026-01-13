package com.lyncalor.calyra.config;

import com.lyncalor.calyra.notion.NotionClient;
import com.lyncalor.calyra.notion.NotionClientService;
import com.lyncalor.calyra.notion.NotionPayloadBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class NotionClientConfiguration {

    @Bean
    @Qualifier("notionRestClient")
    @ConditionalOnProperty(prefix = "notion", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestClient notionRestClient(NotionProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                .defaultHeader("Notion-Version", properties.getNotionVersion())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "notion", name = "enabled", havingValue = "true", matchIfMissing = true)
    public NotionClient notionClient(@Qualifier("notionRestClient") RestClient restClient,
                                     NotionProperties properties,
                                     NotionPayloadBuilder payloadBuilder) {
        return new NotionClientService(restClient, properties, payloadBuilder);
    }
}
