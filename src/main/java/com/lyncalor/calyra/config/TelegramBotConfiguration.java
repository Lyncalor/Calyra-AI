package com.lyncalor.calyra.config;

import com.lyncalor.calyra.telegram.TelegramMessageService;
import com.lyncalor.calyra.telegram.TgNotionAgentBot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TgNotionAgentBot tgNotionAgentBot(TelegramProperties properties, TelegramMessageService messageService) {
        return new TgNotionAgentBot(properties, messageService);
    }
}
