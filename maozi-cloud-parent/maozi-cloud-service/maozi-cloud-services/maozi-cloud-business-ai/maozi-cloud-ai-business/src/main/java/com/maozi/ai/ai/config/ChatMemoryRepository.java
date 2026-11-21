package com.maozi.ai.ai.config;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 对话记忆存储扩展接口
 * <p>
 * 在 Spring AI 的 ChatMemoryRepository 基础上扩展：
 * 支持按消息 ID 删除消息、查询用户对话记录以及按时间增量查询对话消息。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/23
 */
public interface ChatMemoryRepository extends org.springframework.ai.chat.memory.ChatMemoryRepository {

    /**
     * 删除会话中的指定消息
     *
     * @param conversationId 会话 ID
     * @param messageId 消息 ID
     */
    void deleteByMessageId(String conversationId, String messageId);

    /**
     * 查询会话的用户对话记录（仅 USER / ASSISTANT 消息，按时间正序）
     *
     * @param conversationId 会话 ID
     * @return 对话记录消息列表
     */
    List<Message> findRecordByConversationId(String conversationId);

    /**
     * 查询会话中写入时间晚于指定时间戳的用户对话消息
     * （仅 USER / ASSISTANT 消息，按时间正序）
     *
     * @param conversationId 会话 ID
     * @param timestamp 写入时刻毫秒时间戳（不包含等于该时间戳的消息）
     * @return 对话记录消息列表
     */
    List<Message> findRecordAfterTimestamp(String conversationId, Long timestamp);

}
