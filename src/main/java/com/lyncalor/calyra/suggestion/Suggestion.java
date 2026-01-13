package com.lyncalor.calyra.suggestion;

public record Suggestion<T>(
        String fieldName,
        T suggestedValue,
        double confidence,
        String rationale
) {
}
