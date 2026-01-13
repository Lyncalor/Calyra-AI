package com.lyncalor.calyra.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.LlmProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

public class LlmClientService {

    private final RestClient restClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmClientService(RestClient restClient, LlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public String completeJson(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", 0,
                "messages", new Object[]{
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                }
        );
        try {
            String response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractContent(response);
        } catch (RestClientResponseException e) {
            String bodyText = e.getResponseBodyAsString();
            throw new LlmRequestException("LLM request failed with status " + e.getRawStatusCode(), bodyText);
        } catch (RestClientException e) {
            throw new LlmRequestException("LLM request failed", e.getMessage());
        }
    }

    private String extractContent(String response) {
        if (response == null || response.isBlank()) {
            throw new LlmRequestException("LLM response was empty", null);
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.at("/choices/0/message/content");
            if (content.isMissingNode() || content.isNull()) {
                throw new LlmRequestException("LLM response missing content", response);
            }
            return content.asText();
        } catch (Exception e) {
            throw new LlmRequestException("LLM response parsing failed", response);
        }
    }
}
