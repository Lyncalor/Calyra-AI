package com.lyncalor.calyra.qdrant;

import com.lyncalor.calyra.config.QdrantProperties;
import com.lyncalor.calyra.embedding.EmbeddingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.vector.VectorCandidate;
import com.lyncalor.calyra.vector.VectorEvent;
import com.lyncalor.calyra.vector.VectorMemoryException;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QdrantVectorMemoryStore implements VectorMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorMemoryStore.class);

    private final RestClient restClient;
    private final QdrantProperties properties;
    private final EmbeddingService embeddingService;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QdrantVectorMemoryStore(RestClient restClient,
                                   QdrantProperties properties,
                                   EmbeddingService embeddingService,
                                   Clock clock) {
        this.restClient = restClient;
        this.properties = properties;
        this.embeddingService = embeddingService;
        this.clock = clock;
    }

    @PostConstruct
    void init() {
        ensureCollectionExists();
    }

    void ensureCollectionExists() {
        try {
            restClient.get()
                    .uri("/collections/{name}", properties.getCollectionName())
                    .retrieve()
                    .toBodilessEntity();
            return;
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() != 404) {
                log.error("Qdrant collection check failed status={}", e.getRawStatusCode());
                throw new VectorMemoryException("Qdrant collection check failed", e);
            }
        } catch (RestClientException e) {
            log.error("Qdrant is not reachable", e);
            throw new VectorMemoryException("Qdrant is not reachable", e);
        }

        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", properties.getVectorSize(),
                        "distance", properties.getDistance()
                )
        );
        try {
            restClient.put()
                    .uri("/collections/{name}", properties.getCollectionName())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Qdrant collection creation failed", e);
            throw new VectorMemoryException("Qdrant collection creation failed", e);
        }
    }

    @Override
    public void upsertEvent(VectorEvent event) {
        List<Float> vector = embeddingService.embed(event.textForEmbedding());
        if (vector.size() != properties.getVectorSize()) {
            throw new VectorMemoryException("Embedding dimension mismatch", null);
        }

        Map<String, Object> payload = new LinkedHashMap<>(event.payload());
        payload.put("chatId", event.chatId());
        if (event.notionPageId() != null) {
            payload.put("notionPageId", event.notionPageId());
        }
        if (event.type() != null) {
            payload.put("type", event.type());
        }
        if (event.startTs() != null) {
            payload.put("startTs", event.startTs().getEpochSecond());
        }
        payload.put("title", event.title());
        payload.put("createdAt", clock.instant().getEpochSecond());

        Map<String, Object> point = new LinkedHashMap<>();
        point.put("id", event.id());
        point.put("vector", vector);
        point.put("payload", payload);

        Map<String, Object> body = Map.of("points", List.of(point));

        try {
            restClient.put()
                    .uri("/collections/{name}/points?wait=true", properties.getCollectionName())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Qdrant upsert failed status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new VectorMemoryException("Qdrant upsert failed", e);
        } catch (RestClientException e) {
            log.warn("Qdrant upsert failed", e);
            throw new VectorMemoryException("Qdrant upsert failed", e);
        }
    }

    @Override
    public List<VectorCandidate> search(String queryText, long chatId, int limit, Duration lookback) {
        List<Float> vector = embeddingService.embed(queryText);
        if (vector.size() != properties.getVectorSize()) {
            throw new VectorMemoryException("Embedding dimension mismatch", null);
        }
        Map<String, Object> filter = buildFilter(chatId, lookback);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", vector);
        body.put("limit", limit);
        body.put("with_payload", true);
        body.put("filter", filter);

        try {
            String response = restClient.post()
                    .uri("/collections/{name}/points/search", properties.getCollectionName())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseCandidates(response);
        } catch (RestClientResponseException e) {
            log.warn("Qdrant search failed status={} body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new VectorMemoryException("Qdrant search failed", e);
        } catch (RestClientException e) {
            log.warn("Qdrant search failed", e);
            throw new VectorMemoryException("Qdrant search failed", e);
        }
    }

    private Map<String, Object> buildFilter(long chatId, Duration lookback) {
        List<Map<String, Object>> must = new ArrayList<>();
        must.add(Map.of("key", "chatId", "match", Map.of("value", chatId)));
        if (lookback != null) {
            long gte = clock.instant().minus(lookback).getEpochSecond();
            must.add(Map.of("key", "startTs", "range", Map.of("gte", gte)));
        }
        return Map.of("must", must);
    }

    private List<VectorCandidate> parseCandidates(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        List<VectorCandidate> candidates = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("result");
            if (!result.isArray()) {
                return List.of();
            }
            for (JsonNode item : result) {
                String id = item.path("id").asText(null);
                double score = item.path("score").asDouble(0.0);
                JsonNode payload = item.path("payload");
                String title = payload.path("title").asText(null);
                String type = payload.path("type").asText(null);
                String notionPageId = payload.path("notionPageId").asText(null);
                Instant startTs = null;
                if (payload.hasNonNull("startTs")) {
                    startTs = Instant.ofEpochSecond(payload.get("startTs").asLong());
                }
                Map<String, Object> payloadMap = objectMapper.convertValue(payload, Map.class);
                candidates.add(new VectorCandidate(id, score, title, startTs, type, notionPageId, payloadMap));
            }
            return candidates;
        } catch (Exception e) {
            log.warn("Qdrant search response parse failed", e);
            return List.of();
        }
    }
}
