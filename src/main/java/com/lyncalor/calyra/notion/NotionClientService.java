package com.lyncalor.calyra.notion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.NotionProperties;
import com.lyncalor.calyra.schedule.ScheduleDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.Map;

public class NotionClientService implements NotionClient {

    private static final Logger log = LoggerFactory.getLogger(NotionClientService.class);

    private final RestClient restClient;
    private final NotionProperties properties;
    private final NotionPayloadBuilder payloadBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotionClientService(RestClient restClient, NotionProperties properties, NotionPayloadBuilder payloadBuilder) {
        this.restClient = restClient;
        this.properties = properties;
        this.payloadBuilder = payloadBuilder;
    }

    @Override
    public Optional<String> createScheduleEntry(ScheduleDraft draft, String rawText, String source) {
        Map<String, Object> payload = payloadBuilder.buildCreatePagePayload(properties, draft, rawText, source);
        try {
            String response = restClient.post()
                    .uri("/v1/pages")
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return extractId(response);
        } catch (RestClientResponseException e) {
            log.warn("Notion API error status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Notion API call failed", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean createSimpleEntry(String title, String rawText, String source) {
        Map<String, Object> payload = payloadBuilder.buildCreatePagePayload(properties, title, rawText, source);
        try {
            restClient.post()
                    .uri("/v1/pages")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            log.warn("Notion API error status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (RestClientException e) {
            log.warn("Notion API call failed", e);
            return false;
        }
    }

    private Optional<String> extractId(String response) {
        if (response == null || response.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode idNode = root.get("id");
            if (idNode == null || idNode.isNull()) {
                return Optional.empty();
            }
            return Optional.ofNullable(idNode.asText(null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
