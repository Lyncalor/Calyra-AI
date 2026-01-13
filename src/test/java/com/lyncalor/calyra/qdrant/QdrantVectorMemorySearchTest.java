package com.lyncalor.calyra.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.QdrantProperties;
import com.lyncalor.calyra.embedding.FakeEmbeddingService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantVectorMemorySearchTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void searchIncludesChatFilterAndPayload() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"result\":[]}"));
        server.start();

        QdrantProperties properties = new QdrantProperties();
        properties.setBaseUrl(server.url("/").toString());
        properties.setCollectionName("calyra_test");
        properties.setVectorSize(3);

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
        FakeEmbeddingService embeddingService = new FakeEmbeddingService(3);
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

        QdrantVectorMemoryStore store = new QdrantVectorMemoryStore(restClient, properties, embeddingService, clock);
        store.search("test query", 77L, 5, Duration.ofDays(30));

        RecordedRequest request = server.takeRequest();
        server.shutdown();

        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/collections/calyra_test/points/search");
        JsonNode body = mapper.readTree(request.getBody().readUtf8());
        assertThat(body.get("limit").asInt()).isEqualTo(5);
        assertThat(body.get("with_payload").asBoolean()).isTrue();
        assertThat(body.at("/filter/must/0/key").asText()).isEqualTo("chatId");
        assertThat(body.at("/filter/must/0/match/value").asLong()).isEqualTo(77L);
        assertThat(body.at("/filter/must/1/key").asText()).isEqualTo("startTs");
    }
}
