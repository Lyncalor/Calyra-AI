package com.lyncalor.calyra.vector;

public interface VectorMemoryStore {

    void upsertEvent(VectorEvent event);

    java.util.List<VectorCandidate> search(String queryText, long chatId, int limit, java.time.Duration lookback);
}
