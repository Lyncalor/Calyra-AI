package com.lyncalor.calyra.config;

import com.lyncalor.calyra.llm.LlmClientService;
import com.lyncalor.calyra.schedule.PromptFactory;
import com.lyncalor.calyra.schedule.ScheduleParserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmClientConfiguration {

    @Bean
    @Qualifier("llmRestClient")
    @ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestClient llmRestClient(LlmProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LlmClientService llmClientService(@Qualifier("llmRestClient") RestClient restClient,
                                             LlmProperties properties) {
        return new LlmClientService(restClient, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ScheduleParserService scheduleParserService(LlmClientService llmClientService,
                                                       PromptFactory promptFactory) {
        return new ScheduleParserService(llmClientService, promptFactory);
    }
}
