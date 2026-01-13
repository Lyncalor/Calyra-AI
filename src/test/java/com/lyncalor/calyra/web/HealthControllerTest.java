package com.lyncalor.calyra.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "telegram.enabled=false",
                "notion.enabled=false",
                "llm.enabled=false",
                "working-memory.enabled=false",
                "qdrant.enabled=false"
        })
class HealthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthReturnsOk() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("ok");
    }
}
