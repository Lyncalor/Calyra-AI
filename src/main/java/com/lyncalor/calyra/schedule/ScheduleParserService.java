package com.lyncalor.calyra.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyncalor.calyra.llm.LlmClientService;
import com.lyncalor.calyra.llm.LlmRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

public class ScheduleParserService {

    private final LlmClientService llmClientService;
    private final PromptFactory promptFactory;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ScheduleParserService(LlmClientService llmClientService, PromptFactory promptFactory) {
        this.llmClientService = llmClientService;
        this.promptFactory = promptFactory;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public ScheduleDraft parse(String text, Instant now, ZoneId zoneId) {
        String systemPrompt = promptFactory.buildSystemPrompt();
        String userPrompt = promptFactory.buildUserPrompt(text, now, zoneId);
        String raw = llmClientService.completeJson(systemPrompt, userPrompt);
        try {
            ScheduleDraft draft = parseAndValidate(raw);
            return draft;
        } catch (RuntimeException firstFailure) {
            String errorSummary = summarizeError(firstFailure);
            String retryPrompt = promptFactory.buildRetryPrompt(raw, errorSummary);
            try {
                String retryRaw = llmClientService.completeJson(systemPrompt, retryPrompt);
                return parseAndValidate(retryRaw);
            } catch (RuntimeException retryFailure) {
                throw new ScheduleParsingException("Unable to parse schedule request. Please refine your input.");
            }
        }
    }

    private ScheduleDraft parseAndValidate(String json) {
        try {
            ScheduleDraft draft = objectMapper.readValue(json, ScheduleDraft.class);
            Set<ConstraintViolation<ScheduleDraft>> violations = validator.validate(draft);
            if (!violations.isEmpty()) {
                String message = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining("; "));
                throw new ScheduleParsingException(message);
            }
            return draft;
        } catch (JsonProcessingException e) {
            throw new ScheduleParsingException("Invalid JSON: " + e.getOriginalMessage());
        }
    }

    private String summarizeError(RuntimeException failure) {
        if (failure instanceof ScheduleParsingException) {
            return failure.getMessage();
        }
        if (failure instanceof LlmRequestException) {
            return "LLM request failed";
        }
        return "Unknown parsing error";
    }
}
