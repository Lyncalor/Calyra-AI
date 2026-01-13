package com.lyncalor.calyra.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.RetrievalProperties;
import com.lyncalor.calyra.vector.VectorCandidate;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import com.lyncalor.calyra.vector.VectorReranker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "qdrant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SelectionService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PendingSelectionRepository repository;
    private final VectorMemoryStore vectorMemoryStore;
    private final RetrievalProperties retrievalProperties;
    private final VectorReranker reranker;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public SelectionService(PendingSelectionRepository repository,
                            VectorMemoryStore vectorMemoryStore,
                            RetrievalProperties retrievalProperties,
                            Clock clock,
                            ObjectMapper objectMapper) {
        this.repository = repository;
        this.vectorMemoryStore = vectorMemoryStore;
        this.retrievalProperties = retrievalProperties;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.reranker = new VectorReranker();
    }

    public Optional<String> handleSelectionReply(long chatId, String text) {
        if (text == null) {
            return Optional.empty();
        }
        Optional<PendingSelection> pending = repository.findByChatId(chatId);
        if (pending.isEmpty()) {
            return Optional.empty();
        }
        PendingSelection selection = pending.get();
        Instant now = clock.instant();
        if (isExpired(selection, now)) {
            repository.delete(selection);
            return Optional.of("Selection expired. Please run /find again.");
        }
        Integer choice = parseChoice(text.trim());
        if (choice == null) {
            return Optional.empty();
        }
        List<SelectionCandidate> candidates = readCandidates(selection.getCandidatesJson());
        if (choice < 1 || choice > candidates.size()) {
            return Optional.of("Please reply with a number between 1 and " + candidates.size() + ".");
        }
        SelectionCandidate chosen = candidates.get(choice - 1);
        repository.delete(selection);
        return Optional.of(formatSummary(chosen));
    }

    public String findCandidates(long chatId, String queryText) {
        Duration lookback = Duration.ofDays(retrievalProperties.getDefaultLookbackDays());
        List<VectorCandidate> candidates = vectorMemoryStore.search(
                queryText,
                chatId,
                retrievalProperties.getMaxCandidates(),
                lookback
        );
        if (candidates.isEmpty()) {
            return "No matches found.";
        }
        List<VectorCandidate> reranked = reranker.rerank(candidates, queryText, clock.instant(), retrievalProperties);
        VectorCandidate confident = reranker.confidentMatch(reranked, retrievalProperties);
        if (confident != null) {
            return formatSummary(toSelectionCandidate(confident));
        }
        PendingSelection selection = new PendingSelection();
        selection.setChatId(chatId);
        selection.setCandidatesJson(writeCandidates(reranked));
        Instant now = clock.instant();
        selection.setCreatedAt(now);
        selection.setUpdatedAt(now);
        selection.setExpiresAt(now.plusSeconds(retrievalProperties.getSelectionTtlMinutes() * 60L));
        repository.save(selection);
        return formatChoices(reranked);
    }

    public Optional<PendingSelection> getActiveSelection(long chatId) {
        return repository.findByChatId(chatId)
                .filter(selection -> !isExpired(selection, clock.instant()));
    }

    private boolean isExpired(PendingSelection selection, Instant now) {
        return selection.getExpiresAt() != null && now.isAfter(selection.getExpiresAt());
    }

    private Integer parseChoice(String text) {
        if (!text.matches("^\\d{1,2}$")) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private String writeCandidates(List<VectorCandidate> candidates) {
        List<SelectionCandidate> selectionCandidates = new ArrayList<>();
        for (VectorCandidate candidate : candidates) {
            selectionCandidates.add(toSelectionCandidate(candidate));
        }
        try {
            return objectMapper.writeValueAsString(selectionCandidates);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<SelectionCandidate> readCandidates(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SelectionCandidate.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private SelectionCandidate toSelectionCandidate(VectorCandidate candidate) {
        String title = candidate.title() == null ? "Untitled" : candidate.title();
        return new SelectionCandidate(
                candidate.id(),
                title,
                candidate.startTs(),
                candidate.type(),
                candidate.notionPageId()
        );
    }

    private String formatChoices(List<VectorCandidate> candidates) {
        StringBuilder builder = new StringBuilder("I found these possible matches. Reply with a number:\n");
        int count = Math.min(candidates.size(), retrievalProperties.getMaxCandidates());
        for (int i = 0; i < count; i++) {
            SelectionCandidate candidate = toSelectionCandidate(candidates.get(i));
            builder.append(i + 1).append(") ").append(candidate.title());
            if (candidate.startTs() != null) {
                String ts = DATE_TIME.format(candidate.startTs().atZone(DEFAULT_ZONE));
                builder.append(" @ ").append(ts);
            }
            if (candidate.type() != null) {
                builder.append(" [").append(candidate.type()).append("]");
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    private String formatSummary(SelectionCandidate candidate) {
        StringBuilder builder = new StringBuilder("Selected event:\n");
        builder.append("Title: ").append(candidate.title()).append('\n');
        if (candidate.startTs() != null) {
            builder.append("Start: ").append(DATE_TIME.format(candidate.startTs().atZone(DEFAULT_ZONE))).append('\n');
        }
        if (candidate.type() != null) {
            builder.append("Type: ").append(candidate.type()).append('\n');
        }
        builder.append("Id: ").append(shortId(candidate.id()));
        return builder.toString();
    }

    private String shortId(String id) {
        if (id == null) {
            return "unknown";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }
}
