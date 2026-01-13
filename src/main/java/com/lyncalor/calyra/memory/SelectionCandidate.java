package com.lyncalor.calyra.memory;

import java.time.Instant;

public record SelectionCandidate(
        String id,
        String title,
        Instant startTs,
        String type,
        String notionPageId
) {
}
