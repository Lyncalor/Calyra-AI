package com.lyncalor.calyra.vector;

import java.time.Instant;
import java.util.Map;

public record VectorEvent(
        String id,
        long chatId,
        String notionPageId,
        String type,
        Instant startTs,
        String title,
        String textForEmbedding,
        Map<String, Object> payload
) {
    public VectorEvent {
        if (payload == null) {
            payload = Map.of();
        }
    }
}
