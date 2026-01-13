package com.lyncalor.calyra.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.memory.ClarificationResult;
import com.lyncalor.calyra.memory.ClarificationService;
import com.lyncalor.calyra.memory.PendingDraft;
import com.lyncalor.calyra.memory.SelectionService;
import com.lyncalor.calyra.notion.NotionClient;
import com.lyncalor.calyra.preference.PrefsCommandService;
import com.lyncalor.calyra.schedule.ScheduleDraft;
import com.lyncalor.calyra.schedule.ScheduleParserService;
import com.lyncalor.calyra.suggestion.SuggestionEngine;
import com.lyncalor.calyra.suggestion.SuggestionRenderHelper;
import com.lyncalor.calyra.suggestion.SuggestionResult;
import com.lyncalor.calyra.vector.VectorEvent;
import com.lyncalor.calyra.vector.VectorMemoryStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelegramMessageService {

    private final Optional<NotionClient> notionClient;
    private final Optional<ScheduleParserService> scheduleParserService;
    private final Optional<ClarificationService> clarificationService;
    private final Optional<VectorMemoryStore> vectorMemoryStore;
    private final Optional<SelectionService> selectionService;
    private final Optional<SuggestionEngine> suggestionEngine;
    private final Optional<PrefsCommandService> prefsCommandService;
    private final ObjectMapper objectMapper;
    private final SuggestionRenderHelper suggestionRenderHelper = new SuggestionRenderHelper();

    public TelegramMessageService(Optional<NotionClient> notionClient,
                                  Optional<ScheduleParserService> scheduleParserService,
                                  Optional<ClarificationService> clarificationService,
                                  Optional<VectorMemoryStore> vectorMemoryStore,
                                  Optional<SelectionService> selectionService,
                                  Optional<SuggestionEngine> suggestionEngine,
                                  Optional<PrefsCommandService> prefsCommandService,
                                  ObjectMapper objectMapper) {
        this.notionClient = notionClient;
        this.scheduleParserService = scheduleParserService;
        this.clarificationService = clarificationService;
        this.vectorMemoryStore = vectorMemoryStore;
        this.selectionService = selectionService;
        this.suggestionEngine = suggestionEngine;
        this.prefsCommandService = prefsCommandService;
        this.objectMapper = objectMapper;
    }

    public String buildReplyText(String text, Long chatId) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("/start")) {
            return "Welcome! Send me a message and I will echo it back.";
        }
        if (prefsCommandService.isPresent()) {
            Optional<String> prefsReply = prefsCommandService.get().handle(chatId, trimmed);
            if (prefsReply.isPresent()) {
                return prefsReply.get();
            }
        }
        if (trimmed.startsWith("/notion_test")) {
            return handleNotionTest(chatId);
        }
        if (trimmed.startsWith("/parse")) {
            return handleParse(trimmed, chatId);
        }
        if (trimmed.startsWith("/suggest")) {
            return handleSuggest(trimmed, chatId);
        }
        if (trimmed.startsWith("/find")) {
            return handleFind(trimmed, chatId);
        }
        if (trimmed.startsWith("/qdrant_test")) {
            return handleQdrantTest(chatId);
        }
        if (trimmed.startsWith("/reset")) {
            return handleReset(chatId);
        }
        if (trimmed.startsWith("/status")) {
            return handleStatus(chatId);
        }
        if (selectionService.isPresent()) {
            Optional<String> selectionReply = selectionService.get().handleSelectionReply(chatId, trimmed);
            if (selectionReply.isPresent()) {
                return selectionReply.get();
            }
        }
        if (shouldTriggerSearch(trimmed)) {
            return selectionService.isPresent()
                    ? selectionService.get().findCandidates(chatId, trimmed)
                    : "Vector search is disabled.";
        }
        return handleClarificationFlow(text, chatId);
    }

    private String handleNotionTest(Long chatId) {
        if (notionClient.isEmpty()) {
            return "Notion integration is disabled.";
        }
        String title = "TG Notion Agent Test";
        String raw = "Notion test from chatId=" + chatId + " at " + Instant.now();
        boolean success = notionClient.get().createSimpleEntry(title, raw, "telegram");
        if (success) {
            return "Notion test created.";
        }
        return "Notion test failed. Check server logs for details.";
    }

    private String handleParse(String trimmed, Long chatId) {
        if (scheduleParserService.isEmpty()) {
            return "LLM parsing is disabled.";
        }
        String payload = trimmed.replaceFirst("^/parse\\s*", "");
        if (payload.isBlank()) {
            return "Usage: /parse <your text>";
        }
        try {
            ScheduleDraft draft = scheduleParserService.get()
                    .parse(payload, Instant.now(), ZoneId.of("Europe/Berlin"));
            if (draft.needsClarification()) {
                if (draft.clarificationQuestions().isEmpty()) {
                    return "I need more details to schedule this.";
                }
                return "Please clarify:\n- " + String.join("\n- ", draft.clarificationQuestions());
            }
            String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(draft);
            if (pretty.length() > 1200) {
                pretty = pretty.substring(0, 1200) + "...";
            }
            return pretty;
        } catch (JsonProcessingException e) {
            return "Failed to format parsed schedule.";
        } catch (RuntimeException e) {
            return "Failed to parse schedule. Try adding more detail.";
        }
    }

    private String handleQdrantTest(Long chatId) {
        if (vectorMemoryStore.isEmpty()) {
            return "Qdrant integration is disabled.";
        }
        VectorEvent event = new VectorEvent(
                UUID.randomUUID().toString(),
                chatId,
                null,
                "test",
                Instant.now(),
                "Qdrant Test",
                "Qdrant test event",
                Map.of("source", "telegram")
        );
        try {
            vectorMemoryStore.get().upsertEvent(event);
            return "Qdrant test stored.";
        } catch (RuntimeException e) {
            return "Qdrant test failed. Check server logs.";
        }
    }

    private String handleSuggest(String trimmed, Long chatId) {
        if (scheduleParserService.isEmpty() || suggestionEngine.isEmpty()) {
            return "Suggestions are disabled.";
        }
        String payload = trimmed.replaceFirst("^/suggest\\s*", "");
        if (payload.isBlank()) {
            return "Usage: /suggest <text>";
        }
        try {
            ScheduleDraft draft = scheduleParserService.get()
                    .parse(payload, Instant.now(), ZoneId.of("Europe/Berlin"));
            SuggestionResult result = suggestionEngine.get().suggest(chatId, payload, draft);
            if (result.suggestionList().isEmpty()) {
                return "No suggestions found.";
            }
            return suggestionRenderHelper.render(result.suggestionList());
        } catch (RuntimeException e) {
            return "Unable to generate suggestions. Try again later.";
        }
    }

    private String handleFind(String trimmed, Long chatId) {
        if (selectionService.isEmpty()) {
            return "Vector search is disabled.";
        }
        String payload = trimmed.replaceFirst("^/find\\s*", "");
        if (payload.isBlank()) {
            return "Usage: /find <query>";
        }
        return selectionService.get().findCandidates(chatId, payload);
    }

    private String handleReset(Long chatId) {
        if (clarificationService.isEmpty()) {
            return "Working memory is disabled.";
        }
        return clarificationService.get().reset(chatId);
    }

    private String handleStatus(Long chatId) {
        if (clarificationService.isEmpty()) {
            return "Working memory is disabled.";
        }
        Optional<PendingDraft> pending = clarificationService.get().getActiveDraft(chatId);
        if (pending.isEmpty()) {
            return "No active pending draft.";
        }
        return "Pending draft expires at " + pending.get().getExpiresAt();
    }

    private String handleClarificationFlow(String text, Long chatId) {
        if (clarificationService.isEmpty()) {
            return "You said: " + text;
        }
        try {
            ClarificationResult result = clarificationService.get().handleIncomingMessage(chatId, text);
            return result.reply();
        } catch (RuntimeException e) {
            return "Sorry, I couldn't process that. Please try again.";
        }
    }

    private boolean shouldTriggerSearch(String text) {
        String trimmed = text.toLowerCase();
        return trimmed.startsWith("cancel ")
                || trimmed.startsWith("move ")
                || trimmed.startsWith("reschedule ")
                || trimmed.startsWith("update ")
                || trimmed.startsWith("change ")
                || trimmed.startsWith("取消")
                || trimmed.startsWith("改到")
                || trimmed.startsWith("挪到")
                || trimmed.startsWith("改成");
    }
}
