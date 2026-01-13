package com.lyncalor.calyra.notion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.NotionProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotionPayloadBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildsPayloadWithCustomPropertyNames() throws Exception {
        NotionProperties properties = new NotionProperties();
        properties.setDatabaseId("db-123");
        properties.setPropertyNameTitle("Title");
        properties.setPropertyNameSource("Origin");
        properties.setPropertyNameRaw("Message");
        properties.setPropertyTypeSource(NotionProperties.SourcePropertyType.RICH_TEXT);

        NotionPayloadBuilder builder = new NotionPayloadBuilder();
        Map<String, Object> payload = builder.buildCreatePagePayload(properties, "Hello", "Raw body", "telegram");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(payload));
        assertThat(json.at("/parent/database_id").asText()).isEqualTo("db-123");
        assertThat(json.at("/properties/Title/title/0/text/content").asText()).isEqualTo("Hello");
        assertThat(json.at("/properties/Origin/rich_text/0/text/content").asText()).isEqualTo("telegram");
        assertThat(json.at("/properties/Message/rich_text/0/text/content").asText()).isEqualTo("Raw body");
    }
}
