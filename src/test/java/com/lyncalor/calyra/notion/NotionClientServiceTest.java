package com.lyncalor.calyra.notion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.NotionProperties;
import com.lyncalor.calyra.schedule.ScheduleDraft;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class NotionClientServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createSimpleEntrySendsHeadersAndPayload() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        server.start();

        NotionProperties properties = new NotionProperties();
        properties.setEnabled(true);
        properties.setToken("test-token");
        properties.setDatabaseId("db-123");
        properties.setNotionVersion("2022-06-28");
        properties.setApiBaseUrl(server.url("/").toString());

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                .defaultHeader("Notion-Version", properties.getNotionVersion())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        NotionClientService service = new NotionClientService(restClient, properties, new NotionPayloadBuilder());

        boolean result = service.createSimpleEntry("Title", "Raw message", "telegram");

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        server.shutdown();

        assertThat(result).isTrue();
        assertThat(request.getPath()).isEqualTo("/v1/pages");
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-token");
        assertThat(request.getHeader("Notion-Version")).isEqualTo("2022-06-28");
        assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).contains(MediaType.APPLICATION_JSON_VALUE);

        JsonNode json = mapper.readTree(request.getBody().readUtf8());
        assertThat(json.at("/parent/database_id").asText()).isEqualTo("db-123");
        assertThat(json.at("/properties/Name/title/0/text/content").asText()).isEqualTo("Title");
        assertThat(json.at("/properties/Source/select/name").asText()).isEqualTo("telegram");
        assertThat(json.at("/properties/Raw/rich_text/0/text/content").asText()).isEqualTo("Raw message");
    }

    @Test
    void createScheduleEntryReturnsId() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":\"page-123\"}")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        server.start();

        NotionProperties properties = new NotionProperties();
        properties.setEnabled(true);
        properties.setToken("test-token");
        properties.setDatabaseId("db-123");
        properties.setNotionVersion("2022-06-28");
        properties.setApiBaseUrl(server.url("/").toString());

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                .defaultHeader("Notion-Version", properties.getNotionVersion())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        NotionClientService service = new NotionClientService(restClient, properties, new NotionPayloadBuilder());
        ScheduleDraft draft = new ScheduleDraft(
                "Title",
                OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                null,
                "Meeting",
                "Office",
                null,
                "Europe/Berlin",
                30,
                false,
                java.util.List.of()
        );

        Optional<String> result = service.createScheduleEntry(draft, "Raw message", "telegram");

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        server.shutdown();

        assertThat(result).contains("page-123");
        assertThat(request.getPath()).isEqualTo("/v1/pages");
    }
}
