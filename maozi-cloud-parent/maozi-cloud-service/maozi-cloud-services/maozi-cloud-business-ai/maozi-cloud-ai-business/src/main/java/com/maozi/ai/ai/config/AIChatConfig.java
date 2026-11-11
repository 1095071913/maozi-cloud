package com.maozi.ai.ai.config;

import com.maozi.ai.ai.function.call.OauthFunctionCall;
import com.maozi.ai.ai.function.call.SystemFunctionCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 对话配置
 * <p>
 * 基于 Spring AI 构建 ChatClient，装配对话记忆、系统工具等组件。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/15 20:12
 */
@Configuration
public class AIChatConfig {

    private static final int CHAT_MEMORY_MAX_MESSAGES = 200;

//    @Bean
//    public Advisor loggerAdvisor() {
//        return new SimpleLoggerAdvisor();
//    }

    /**
     * 构建对话记忆
     * <p>
     * 基于自定义对话记忆存储构建窗口式对话记忆，每个会话最多保留 200 条消息。
     * </p>
     *
     * @param chatMemoryRepository 对话记忆存储
     * @return 窗口式对话记忆
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(CHAT_MEMORY_MAX_MESSAGES)
                .build();
    }

    /**
     * 构建对话记忆 Advisor
     * <p>
     * 将对话记忆包装为对话客户端的消息增强器，对话时自动读取与写入会话上下文。
     * </p>
     *
     * @param chatMemory 对话记忆
     * @return 对话记忆 Advisor
     */
    @Bean
    public Advisor chatMemoryAdvisor(ChatMemory chatMemory){
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * 构建对话客户端
     * <p>
     * 在 Spring AI 自动配置的构建器上默认装配对话记忆 Advisor，
     * 并注册系统与 OAuth 两组工具函数供 AI 对话调用。
     * </p>
     *
     * @param chatClientBuilder Spring AI 自动配置的对话客户端构建器
     * @param chatMemoryAdvisor 对话记忆 Advisor
     * @param systemFunctionCall 系统工具函数
     * @param oauthFunctionCall OAuth 工具函数
     * @return 对话客户端
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, Advisor chatMemoryAdvisor,
                                 SystemFunctionCall systemFunctionCall,
                                 OauthFunctionCall oauthFunctionCall) {
        return chatClientBuilder
                .defaultAdvisors(chatMemoryAdvisor)
                .defaultTools(systemFunctionCall,oauthFunctionCall)
                .build();
    }

}
