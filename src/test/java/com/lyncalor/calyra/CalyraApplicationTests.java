package com.lyncalor.calyra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "telegram.enabled=false",
        "notion.enabled=false",
        "llm.enabled=false",
        "working-memory.enabled=false",
        "qdrant.enabled=false"
})
class CalyraApplicationTests {

	@Test
	void contextLoads() {
	}

}
