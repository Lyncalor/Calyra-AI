package com.lyncalor.calyra.vector;

import java.time.Instant;
import java.util.Map;

public record VectorCandidate(
        String id,
        double score,
        String title,
        Instant startTs,
        String type,
        String notionPageId,
        Map<String, Object> payload
) {
    public VectorCandidate {
        if (payload == null) {
            payload = Map.of();
        }
    }
}
