package com.lyncalor.calyra.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.QdrantProperties;
import com.lyncalor.calyra.embedding.FakeEmbeddingService;
import com.lyncalor.calyra.vector.VectorEvent;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantVectorMemoryStoreTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void ensuresCollectionAndUpsertsPoints() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        server.start();

        QdrantProperties properties = new QdrantProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(server.url("/").toString());
        properties.setCollectionName("calyra_test");
        properties.setVectorSize(3);
        properties.setDistance("Cosine");

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();

        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        FakeEmbeddingService embeddingService = new FakeEmbeddingService(3);

        QdrantVectorMemoryStore store = new QdrantVectorMemoryStore(restClient, properties, embeddingService, clock);
        store.ensureCollectionExists();

        VectorEvent event = new VectorEvent(
                "evt-1",
                42L,
                null,
                "meeting",
                Instant.parse("2025-01-01T10:00:00Z"),
                "Team sync",
                "Team sync meeting",
                Map.of("source", "test")
        );

        store.upsertEvent(event);

        RecordedRequest checkRequest = server.takeRequest();
        RecordedRequest createRequest = server.takeRequest();
        RecordedRequest upsertRequest = server.takeRequest();
        server.shutdown();

        assertThat(checkRequest.getMethod()).isEqualTo("GET");
        assertThat(checkRequest.getPath()).isEqualTo("/collections/calyra_test");

        assertThat(createRequest.getMethod()).isEqualTo("PUT");
        assertThat(createRequest.getPath()).isEqualTo("/collections/calyra_test");
        JsonNode createJson = mapper.readTree(createRequest.getBody().readUtf8());
        assertThat(createJson.at("/vectors/size").asInt()).isEqualTo(3);
        assertThat(createJson.at("/vectors/distance").asText()).isEqualTo("Cosine");

        assertThat(upsertRequest.getMethod()).isEqualTo("PUT");
        assertThat(upsertRequest.getPath()).isEqualTo("/collections/calyra_test/points?wait=true");
        JsonNode upsertJson = mapper.readTree(upsertRequest.getBody().readUtf8());
        assertThat(upsertJson.at("/points/0/id").asText()).isEqualTo("evt-1");
        assertThat(upsertJson.at("/points/0/vector")).hasSize(3);
        assertThat(upsertJson.at("/points/0/payload/chatId").asLong()).isEqualTo(42L);
        assertThat(upsertJson.at("/points/0/payload/title").asText()).isEqualTo("Team sync");
        assertThat(upsertJson.at("/points/0/payload/type").asText()).isEqualTo("meeting");
        assertThat(upsertJson.at("/points/0/payload/startTs").asLong()).isEqualTo(1735725600L);
        assertThat(upsertJson.at("/points/0/payload/createdAt").asLong()).isEqualTo(1735689600L);
    }
}
