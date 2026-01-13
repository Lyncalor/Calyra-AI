package com.lyncalor.calyra.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.config.SuggestionProperties;
import com.lyncalor.calyra.config.WorkingMemoryProperties;
import com.lyncalor.calyra.notion.NotionClient;
import com.lyncalor.calyra.preference.PreferenceLearningService;
import com.lyncalor.calyra.schedule.ScheduleDraft;
import com.lyncalor.calyra.schedule.ScheduleParserService;
import com.lyncalor.calyra.suggestion.ScheduleSuggestions;
import com.lyncalor.calyra.suggestion.SuggestionEngine;
import com.lyncalor.calyra.suggestion.SuggestionRenderHelper;
import com.lyncalor.calyra.suggestion.SuggestionResult;
import com.lyncalor.calyra.vector.VectorEvent;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "working-memory", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClarificationService {

    private static final Logger log = LoggerFactory.getLogger(ClarificationService.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");

    private final PendingDraftRepository repository;
    private final DedupeEntryRepository dedupeRepository;
    private final ScheduleParserService parserService;
    private final WorkingMemoryProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Optional<VectorMemoryStore> vectorMemoryStore;
    private final Optional<NotionClient> notionClient;
    private final Optional<SuggestionEngine> suggestionEngine;
    private final SuggestionProperties suggestionProperties;
    private final Optional<PreferenceLearningService> preferenceLearningService;
    private final SuggestionRenderHelper suggestionRenderHelper = new SuggestionRenderHelper();
    private final Validator validator;

    public ClarificationService(PendingDraftRepository repository,
                                DedupeEntryRepository dedupeRepository,
                                ScheduleParserService parserService,
                                WorkingMemoryProperties properties,
                                Clock clock,
                                ObjectMapper objectMapper,
                                Optional<VectorMemoryStore> vectorMemoryStore,
                                Optional<NotionClient> notionClient,
                                Optional<SuggestionEngine> suggestionEngine,
                                SuggestionProperties suggestionProperties,
                                Optional<PreferenceLearningService> preferenceLearningService) {
        this.repository = repository;
        this.dedupeRepository = dedupeRepository;
        this.parserService = parserService;
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.vectorMemoryStore = vectorMemoryStore;
        this.notionClient = notionClient;
        this.suggestionEngine = suggestionEngine;
        this.suggestionProperties = suggestionProperties;
        this.preferenceLearningService = preferenceLearningService;
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public Optional<PendingDraft> getActiveDraft(long chatId) {
        return repository.findByChatId(chatId)
                .filter(draft -> draft.getStatus() == PendingDraftStatus.WAITING_FOR_CLARIFICATION)
                .filter(draft -> !isExpired(draft, clock.instant()));
    }

    public ClarificationResult handleIncomingMessage(long chatId, String text) {
        Instant now = clock.instant();
        Optional<PendingDraft> existing = repository.findByChatId(chatId);
        if (existing.isPresent()) {
            PendingDraft draft = existing.get();
            if (draft.getStatus() == PendingDraftStatus.WAITING_FOR_CLARIFICATION && !isExpired(draft, now)) {
                return handleClarificationReply(draft, text, now);
            }
            if (isExpired(draft, now)) {
                markExpired(draft, now);
                ClarificationResult result = handleNewRequest(chatId, text, now, draft);
                return new ClarificationResult("Previous draft expired and was reset.\n" + result.reply());
            }
        }
        return handleNewRequest(chatId, text, now, existing.orElse(null));
    }

    public String reset(long chatId) {
        Optional<PendingDraft> existing = repository.findByChatId(chatId);
        if (existing.isEmpty()) {
            return "No pending draft to reset.";
        }
        PendingDraft draft = existing.get();
        Instant now = clock.instant();
        draft.setStatus(PendingDraftStatus.EXPIRED);
        draft.setUpdatedAt(now);
        draft.setExpiresAt(now);
        repository.save(draft);
        return "Pending draft cleared.";
    }

    private ClarificationResult handleNewRequest(long chatId, String text, Instant now, PendingDraft existing) {
        ScheduleDraft draft = parserService.parse(text, now, DEFAULT_ZONE);
        if (draft.needsClarification()) {
            PendingDraft pending = existing == null ? new PendingDraft() : existing;
            pending.setChatId(chatId);
            pending.setRawInitialText(text);
            pending.setDraftJson(writeJson(draft));
            pending.setSuggestedJson(null);
            pending.setStatus(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
            if (pending.getCreatedAt() == null) {
                pending.setCreatedAt(now);
            }
            pending.setUpdatedAt(now);
            pending.setExpiresAt(now.plusSeconds(properties.getTtlMinutes() * 60L));
            repository.save(pending);
            return new ClarificationResult(buildSuggestionsResponse(pending, draft, text, true));
        }
        if (shouldSuggestForCompleteDraft(draft)) {
            PendingDraft pending = existing == null ? new PendingDraft() : existing;
            pending.setChatId(chatId);
            pending.setRawInitialText(text);
            pending.setDraftJson(writeJson(draft));
            pending.setStatus(PendingDraftStatus.WAITING_FOR_CLARIFICATION);
            if (pending.getCreatedAt() == null) {
                pending.setCreatedAt(now);
            }
            pending.setUpdatedAt(now);
            pending.setExpiresAt(now.plusSeconds(properties.getTtlMinutes() * 60L));
            repository.save(pending);
            return new ClarificationResult(buildSuggestionsResponse(pending, draft, text, false));
        }
        return finalizeDraft(chatId, text, draft, null);
    }

    private ClarificationResult handleClarificationReply(PendingDraft existing, String userReply, Instant now) {
        if (isAccept(userReply) && hasSuggestions(existing)) {
            ScheduleDraft draft = readJson(existing.getDraftJson());
            if (draft == null) {
                existing.setSuggestedJson(null);
                existing.setUpdatedAt(now);
                repository.save(existing);
                return new ClarificationResult("Unable to apply suggestions. Please restate your request.");
            }
            ScheduleDraft applied = applySuggestions(draft, readSuggestions(existing));
            if (isValidDraft(applied)) {
                existing.setDraftJson(writeJson(applied));
                existing.setSuggestedJson(null);
                existing.setStatus(PendingDraftStatus.READY);
                existing.setUpdatedAt(now);
                existing.setExpiresAt(now);
                repository.save(existing);
                return finalizeDraft(existing.getChatId(), existing.getRawInitialText(), applied, existing);
            }
            existing.setSuggestedJson(null);
            existing.setUpdatedAt(now);
            repository.save(existing);
            return new ClarificationResult(formatQuestions(draft));
        }
        if (isReject(userReply) && hasSuggestions(existing)) {
            existing.setSuggestedJson(null);
            existing.setUpdatedAt(now);
            repository.save(existing);
            ScheduleDraft draft = readJson(existing.getDraftJson());
            if (draft == null) {
                return new ClarificationResult("Please restate your request.");
            }
            if (draft != null && !draft.needsClarification()) {
                existing.setStatus(PendingDraftStatus.READY);
                existing.setExpiresAt(now);
                repository.save(existing);
                return finalizeDraft(existing.getChatId(), existing.getRawInitialText(), draft, existing);
            }
            return new ClarificationResult(formatQuestions(draft));
        }
        ScheduleDraft previous = readJson(existing.getDraftJson());
        String combined = buildCombinedPrompt(existing.getRawInitialText(),
                previous == null ? "" : String.join("; ", previous.clarificationQuestions()),
                userReply);
        ScheduleDraft draft = parserService.parse(combined, now, DEFAULT_ZONE);
        if (draft.needsClarification()) {
            existing.setDraftJson(writeJson(draft));
            existing.setSuggestedJson(null);
            existing.setUpdatedAt(now);
            existing.setExpiresAt(now.plusSeconds(properties.getTtlMinutes() * 60L));
            repository.save(existing);
            return new ClarificationResult(buildSuggestionsResponse(existing, draft, combined, true));
        }
        existing.setDraftJson(writeJson(draft));
        existing.setSuggestedJson(null);
        existing.setStatus(PendingDraftStatus.READY);
        existing.setUpdatedAt(now);
        existing.setExpiresAt(now);
        repository.save(existing);
        return finalizeDraft(existing.getChatId(), existing.getRawInitialText(), draft, existing);
    }

    private ClarificationResult finalizeDraft(long chatId, String rawText, ScheduleDraft draft, PendingDraft existing) {
        String dedupeKey = dedupeKey(chatId, draft);
        Optional<DedupeEntry> dedupe = dedupeRepository.findByChatIdAndDedupeKey(chatId, dedupeKey)
                .filter(entry -> !isExpired(entry, clock.instant()));
        if (dedupe.isPresent()) {
            return new ClarificationResult("Looks like this already exists:\n" + dedupe.get().getSummary());
        }

        String notionPageId = null;
        if (notionClient.isPresent()) {
            notionPageId = notionClient.get().createScheduleEntry(draft, rawText, "telegram").orElse(null);
        }

        String summary = formatSummary(draft);
        DedupeEntry entry = new DedupeEntry();
        entry.setChatId(chatId);
        entry.setDedupeKey(dedupeKey);
        entry.setCreatedAt(clock.instant());
        entry.setExpiresAt(clock.instant().plusSeconds(300));
        entry.setNotionPageId(notionPageId);
        entry.setSummary(summary);
        dedupeRepository.save(entry);

        upsertVectorEvent(chatId, rawText, draft, notionPageId);

        if (notionPageId != null) {
            preferenceLearningService.ifPresent(service -> service.learnFromEvent(chatId, draft));
        }

        if (existing != null) {
            existing.setSuggestedJson(null);
            existing.setUpdatedAt(clock.instant());
            repository.save(existing);
        }

        if (notionClient.isPresent() && notionPageId == null) {
            return new ClarificationResult(summary + "\nNotion create failed.");
        }
        return new ClarificationResult(summary);
    }

    private void upsertVectorEvent(long chatId, String rawText, ScheduleDraft draft, String notionPageId) {
        if (vectorMemoryStore.isEmpty()) {
            return;
        }
        VectorEvent event = new VectorEvent(
                UUID.randomUUID().toString(),
                chatId,
                notionPageId,
                draft.type(),
                draft.start() == null ? null : draft.start().toInstant(),
                draft.title(),
                buildTextForEmbedding(draft),
                buildPayload(rawText, draft)
        );
        try {
            vectorMemoryStore.get().upsertEvent(event);
        } catch (RuntimeException e) {
            log.warn("Vector memory upsert failed for chatId={}", chatId, e);
        }
    }

    private Map<String, Object> buildPayload(String rawText, ScheduleDraft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rawInitialText", rawText);
        payload.put("timezone", draft.timezone());
        if (draft.location() != null) {
            payload.put("location", draft.location());
        }
        if (draft.remindMinutesBefore() != null) {
            payload.put("remindMinutesBefore", draft.remindMinutesBefore());
        }
        if (draft.end() != null) {
            payload.put("endTs", draft.end().toInstant().getEpochSecond());
        }
        return payload;
    }

    private String buildTextForEmbedding(ScheduleDraft draft) {
        List<String> parts = new ArrayList<>();
        if (draft.title() != null) {
            parts.add(draft.title());
        }
        if (draft.notes() != null) {
            parts.add(draft.notes());
        }
        if (draft.location() != null) {
            parts.add(draft.location());
        }
        if (draft.type() != null) {
            parts.add(draft.type());
        }
        return String.join(" ", parts);
    }

    private String buildSuggestionsResponse(PendingDraft pending, ScheduleDraft draft, String rawText, boolean includeQuestions) {
        if (suggestionEngine.isEmpty() || !suggestionProperties.isEnabled()) {
            return includeQuestions ? formatQuestions(draft) : formatSummary(draft);
        }
        SuggestionResult suggestionResult = suggestionEngine.get().suggest(pending.getChatId(), rawText, draft);
        if (suggestionResult.suggestionList().isEmpty()) {
            return includeQuestions ? formatQuestions(draft) : formatSummary(draft);
        }
        pending.setSuggestedJson(writeSuggestions(suggestionResult.suggestions()));
        repository.save(pending);
        String suggestionsText = suggestionRenderHelper.render(suggestionResult.suggestionList());
        if (suggestionProperties.isConfirmationRequired()) {
            if (includeQuestions) {
                return suggestionsText + "\nReply 'accept' to use these defaults, or answer the questions below.\n"
                        + formatQuestions(draft);
            }
            return suggestionsText + "\nReply 'accept' to use these defaults, or reply 'reject' to keep current values.";
        }
        return includeQuestions ? suggestionsText + "\n" + formatQuestions(draft) : suggestionsText;
    }

    private boolean shouldSuggestForCompleteDraft(ScheduleDraft draft) {
        if (suggestionEngine.isEmpty() || !suggestionProperties.isEnabled()) {
            return false;
        }
        return draft.start() != null && draft.end() == null;
    }

    private boolean hasSuggestions(PendingDraft draft) {
        return draft.getSuggestedJson() != null && !draft.getSuggestedJson().isBlank();
    }

    private ScheduleSuggestions readSuggestions(PendingDraft draft) {
        if (draft.getSuggestedJson() == null || draft.getSuggestedJson().isBlank()) {
            return ScheduleSuggestions.empty();
        }
        try {
            return objectMapper.readValue(draft.getSuggestedJson(), ScheduleSuggestions.class);
        } catch (JsonProcessingException e) {
            return ScheduleSuggestions.empty();
        }
    }

    private String writeSuggestions(ScheduleSuggestions suggestions) {
        try {
            return objectMapper.writeValueAsString(suggestions);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private ScheduleDraft applySuggestions(ScheduleDraft draft, ScheduleSuggestions suggestions) {
        if (draft == null) {
            return null;
        }
        java.time.OffsetDateTime start = draft.start();
        java.time.OffsetDateTime end = draft.end();
        if (end == null && start != null && suggestions.duration().isPresent()) {
            end = start.plus(suggestions.duration().get());
        }
        String location = draft.location();
        if ((location == null || location.isBlank()) && suggestions.location().isPresent()) {
            location = suggestions.location().get();
        }
        Integer reminder = draft.remindMinutesBefore();
        if ((reminder == null || reminder == 30) && suggestions.remindMinutesBefore().isPresent()) {
            reminder = suggestions.remindMinutesBefore().get();
        }
        String type = draft.type();
        if ((type == null || type.isBlank() || "Other".equalsIgnoreCase(type)) && suggestions.type().isPresent()) {
            type = suggestions.type().get();
        }
        boolean needsClarification = draft.needsClarification();
        if (start != null) {
            needsClarification = false;
        }
        return new ScheduleDraft(
                draft.title(),
                start,
                end,
                type,
                location,
                draft.notes(),
                draft.timezone(),
                reminder,
                needsClarification,
                draft.clarificationQuestions()
        );
    }

    private boolean isValidDraft(ScheduleDraft draft) {
        if (draft == null) {
            return false;
        }
        return validator.validate(draft).isEmpty();
    }

    private boolean isAccept(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase();
        return normalized.equals("accept")
                || normalized.equals("use defaults")
                || normalized.equals("采用")
                || normalized.equals("确认");
    }

    private boolean isReject(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase();
        return normalized.equals("reject")
                || normalized.equals("no")
                || normalized.equals("不用");
    }

    private String dedupeKey(long chatId, ScheduleDraft draft) {
        String base = chatId + "|" + draft.title() + "|" + (draft.start() == null ? "" : draft.start().toString());
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(base.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private boolean isExpired(DedupeEntry entry, Instant now) {
        return entry.getExpiresAt() != null && now.isAfter(entry.getExpiresAt());
    }

    private boolean isExpired(PendingDraft draft, Instant now) {
        return draft.getExpiresAt() != null && now.isAfter(draft.getExpiresAt());
    }

    private void markExpired(PendingDraft draft, Instant now) {
        draft.setStatus(PendingDraftStatus.EXPIRED);
        draft.setUpdatedAt(now);
        draft.setExpiresAt(now);
        repository.save(draft);
    }

    private String writeJson(ScheduleDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private ScheduleDraft readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ScheduleDraft.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String buildCombinedPrompt(String originalText, String questions, String answer) {
        return "Original request: " + originalText + "\n"
                + "Clarification questions: " + questions + "\n"
                + "User answer: " + answer;
    }

    private String formatQuestions(ScheduleDraft draft) {
        if (draft.clarificationQuestions().isEmpty()) {
            return "Please provide more details to schedule this.";
        }
        StringBuilder builder = new StringBuilder("Please clarify:\n");
        for (int i = 0; i < draft.clarificationQuestions().size(); i++) {
            builder.append(i + 1).append(") ").append(draft.clarificationQuestions().get(i)).append('\n');
        }
        return builder.toString().trim();
    }

    private String formatSummary(ScheduleDraft draft) {
        StringBuilder summary = new StringBuilder("Parsed schedule:\n");
        summary.append("Title: ").append(draft.title()).append('\n');
        summary.append("Start: ").append(draft.start()).append('\n');
        summary.append("End: ").append(draft.end()).append('\n');
        summary.append("Type: ").append(draft.type()).append('\n');
        summary.append("Timezone: ").append(draft.timezone());
        return summary.toString();
    }
}
