package com.lyncalor.calyra.telegram;

import com.lyncalor.calyra.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.stream.Collectors;

public class TgNotionAgentBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TgNotionAgentBot.class);

    private final TelegramProperties properties;
    private final TelegramMessageService messageService;

    public TgNotionAgentBot(TelegramProperties properties, TelegramMessageService messageService) {
        this.properties = properties;
        this.messageService = messageService;
    }

    @Override
    public String getBotUsername() {
        return properties.getBotUsername() == null ? "" : properties.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return properties.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage()) {
            return;
        }
        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }
        String text = message.getText();
        logIncoming(message, text);

        String replyText = messageService.buildReplyText(text, message.getChatId());
        if (replyText == null) {
            return;
        }
        SendMessage reply = SendMessage.builder()
                .chatId(String.valueOf(message.getChatId()))
                .text(replyText)
                .build();
        try {
            execute(reply);
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram reply to chatId={}", message.getChatId(), e);
        }
    }

    public List<String> getAllowedUpdates() {
        List<String> allowed = properties.getPollingAllowedUpdates();
        if (allowed == null || allowed.isEmpty()) {
            return null;
        }
        List<String> filtered = allowed.stream()
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.toList());
        return filtered.isEmpty() ? null : filtered;
    }

    private void logIncoming(Message message, String text) {
        String username = message.getFrom() == null ? null : message.getFrom().getUserName();
        String safeText = text;
        if (safeText != null && safeText.length() > 200) {
            safeText = safeText.substring(0, 200) + "...";
        }
        log.info("Telegram message chatId={} username={} messageId={} text=\"{}\"",
                message.getChatId(),
                username,
                message.getMessageId(),
                safeText);
    }
}
