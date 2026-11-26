package com.tokenflash.ai.config;

import cn.hutool.core.util.StrUtil;
import com.tokenflash.ai.repository.RedisChatMemoryStore;
import com.tokenflash.ai.service.ConsultantService;
import com.tokenflash.ai.tools.ReservationTool;
import com.tokenflash.ai.tools.ShopTool;
import com.tokenflash.ai.tools.TokenPackTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reference wiring for the course assistant, enabled only in the local profile. */
@Profile("consultant-local")
@Configuration
public class AiConfig {

    @Value("${tokenflash.ai.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${tokenflash.ai.api-key:}")
    private String apiKey;

    @Value("${tokenflash.ai.model-name:deepseek-chat}")
    private String modelName;

    @Value("${tokenflash.ai.log-requests:false}")
    private boolean logRequests;

    @Value("${tokenflash.ai.log-responses:false}")
    private boolean logResponses;

    @Bean
    public StreamingChatModel streamingChatModel() {
        String effectiveKey = StrUtil.isBlank(apiKey) ? "placeholder-api-key" : apiKey;
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(effectiveKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(RedisChatMemoryStore redisChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    @Bean
    public ConsultantService consultantService(
            StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            ShopTool shopTool,
            TokenPackTool voucherTool,
            ReservationTool reservationTool
    ) {
        return AiServices.builder(ConsultantService.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(shopTool, voucherTool, reservationTool)
                .build();
    }
}
