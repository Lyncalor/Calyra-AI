package com.lyncalor.calyra.vector;

import com.lyncalor.calyra.config.RetrievalProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class VectorReranker {

    public List<VectorCandidate> rerank(List<VectorCandidate> candidates,
                                        String queryText,
                                        Instant now,
                                        RetrievalProperties properties) {
        List<String> tokens = tokenize(queryText);
        List<VectorCandidate> reranked = new ArrayList<>(candidates.size());
        for (VectorCandidate candidate : candidates) {
            double score = candidate.score();
            score += recencyBonus(candidate.startTs(), now, properties.getDefaultLookbackDays());
            score += keywordBonus(candidate.title(), tokens);
            reranked.add(new VectorCandidate(
                    candidate.id(),
                    score,
                    candidate.title(),
                    candidate.startTs(),
                    candidate.type(),
                    candidate.notionPageId(),
                    candidate.payload()
            ));
        }
        reranked.sort(Comparator.comparingDouble(VectorCandidate::score).reversed());
        return reranked;
    }

    public VectorCandidate confidentMatch(List<VectorCandidate> candidates, RetrievalProperties properties) {
        if (candidates.isEmpty()) {
            return null;
        }
        VectorCandidate top1 = candidates.get(0);
        if (top1.score() < properties.getMinScore()) {
            return null;
        }
        if (candidates.size() == 1) {
            return top1;
        }
        VectorCandidate top2 = candidates.get(1);
        if ((top1.score() - top2.score()) >= properties.getMinMargin()) {
            return top1;
        }
        return null;
    }

    private double recencyBonus(Instant startTs, Instant now, int lookbackDays) {
        if (startTs == null || now == null || lookbackDays <= 0) {
            return 0.0;
        }
        long ageDays = Duration.between(startTs, now).toDays();
        if (ageDays < 0) {
            ageDays = 0;
        }
        double factor = Math.max(0.0, (lookbackDays - ageDays) / (double) lookbackDays);
        return factor * 0.05;
    }

    private double keywordBonus(String title, List<String> tokens) {
        if (title == null || tokens.isEmpty()) {
            return 0.0;
        }
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String token : tokens) {
            if (lowerTitle.contains(token)) {
                matches++;
            }
        }
        return Math.min(0.08, matches * 0.02);
    }

    private List<String> tokenize(String queryText) {
        if (queryText == null) {
            return List.of();
        }
        String[] parts = queryText.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 3) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
