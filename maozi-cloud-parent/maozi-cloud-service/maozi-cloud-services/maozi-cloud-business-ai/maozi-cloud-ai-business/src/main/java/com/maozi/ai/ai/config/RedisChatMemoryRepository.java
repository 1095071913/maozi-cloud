package com.maozi.ai.ai.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maozi.ai.ai.constant.ChatMessageConstant;
import com.maozi.common.CollectionUtil;
import com.maozi.common.JacksonUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.redis.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 基于Redis的对话记忆存储实现
 * <p>
 * 每个会话在 Redis 中使用三个 key 存储：
 * <ul>
 *   <li>{@code chat:memory:{conversationId}}：HASH，field = 消息雪花 ID，value = 消息 JSON</li>
 *   <li>{@code chat:memory:{conversationId}:order}：ZSET，全部消息的顺序索引，score = 写入时刻毫秒时间戳</li>
 *   <li>{@code chat:memory:{conversationId}:record}：ZSET，仅 USER / ASSISTANT 消息（用户对话记录）的顺序索引</li>
 * </ul>
 * 消息以 {@link ChatMessage} 结构序列化为 JSON 存储，读取时按消息类型反序列化为对应的 Spring AI 消息对象。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/16 20:06
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /** Redis 客户端，用于读写会话记忆的消息主体与索引 key */
    @Resource
    private StringRedisTemplate redisClient;

    /** 会话记忆消息主体 key 前缀 */
    private static final String AI_CHAT_MEMORY_KEY = RedisUtil.REDIS_KEY_PREFIX + "chat:memory:";

    /** 消息顺序索引 key 后缀：ZSET，member = 消息雪花 ID，score = 写入时刻毫秒时间戳 */
    private static final String AI_CHAT_MEMORY_ORDER_KEY_SUFFIX = ":order";

    /** 用户对话记录索引 key 后缀：ZSET，member = 消息雪花 ID，score = 写入时刻毫秒时间戳，仅记录 USER / ASSISTANT 消息 */
    private static final String AI_CHAT_MEMORY_RECORD_KEY_SUFFIX = ":record";

    /**
     * 查询全部会话 ID
     * <p>
     * 通过 SCAN 遍历会话记忆主体 key 并去除前缀得到会话 ID，跳过 :order 与 :record 索引 key。
     * </p>
     *
     * @return 会话 ID 列表
     */
    @NotNull
    @Override
    public List<String> findConversationIds() {

        String aiChatMemoryKey = AI_CHAT_MEMORY_KEY + "*";

        List<String> conversationIds = CollectionUtil.newArrayList();
        try (Cursor<String> cursor = redisClient.scan(ScanOptions.scanOptions().match(aiChatMemoryKey).count(1000).build())) {
            cursor.forEachRemaining(key -> {
                // 跳过顺序索引与对话记录索引 key，仅保留消息主体 key
                if (key.endsWith(AI_CHAT_MEMORY_ORDER_KEY_SUFFIX) || key.endsWith(AI_CHAT_MEMORY_RECORD_KEY_SUFFIX)) {
                    return;
                }
                conversationIds.add(StrUtil.replace(key, AI_CHAT_MEMORY_KEY, ""));
            });
        }

        return conversationIds;

    }

    /**
     * 查询会话的全部消息（含系统、工具等所有类型，按写入时间正序）
     *
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    @NotNull
    @Override
    public List<Message> findByConversationId(@NotNull String conversationId) {
        return findMessageByIndexSuffix(conversationId, AI_CHAT_MEMORY_ORDER_KEY_SUFFIX, Double.NEGATIVE_INFINITY);
    }

    /**
     * 查询会话的用户对话记录（仅 USER / ASSISTANT 消息，按时间正序）
     *
     * @param conversationId 会话 ID
     * @return 对话记录消息列表
     */
    @NotNull
    @Override
    public List<Message> findRecordByConversationId(@NotNull String conversationId) {
        return findMessageByIndexSuffix(conversationId, AI_CHAT_MEMORY_RECORD_KEY_SUFFIX, Double.NEGATIVE_INFINITY);
    }

    /**
     * 查询会话中写入时间晚于指定时间戳的用户对话消息（仅 USER / ASSISTANT 消息，按时间正序）
     *
     * @param conversationId 会话 ID
     * @param timestamp 写入时刻毫秒时间戳（不包含等于该时间戳的消息）
     * @return 对话记录消息列表
     */
    @NotNull
    @Override
    public List<Message> findRecordAfterTimestamp(@NotNull String conversationId, Long timestamp) {
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        return findMessageByIndexSuffix(conversationId, AI_CHAT_MEMORY_RECORD_KEY_SUFFIX, timestamp + 1);
    }

    /**
     * 按索引后缀与最小 score 查询会话消息的通用方法
     * <p>
     * 先从指定 ZSET 索引中按 score 范围（minScore 至正无穷）取出消息 ID，
     * 再从消息主体 HASH 中批量取出消息 JSON 并反序列化为消息对象。
     * </p>
     *
     * @param conversationId 会话 ID
     * @param indexKeySuffix 索引 key 后缀（:order 全部消息 / :record 用户对话记录）
     * @param minScore 最小写入时刻毫秒时间戳（包含，查询 score 大于等于该值的消息，调用方通过传入 timestamp + 1 实现"晚于 timestamp"语义）
     * @return 消息列表
     */
    private List<Message> findMessageByIndexSuffix(String conversationId, String indexKeySuffix, double minScore) {

        String aiChatMemoryKey = AI_CHAT_MEMORY_KEY + conversationId;

        // 从索引 ZSET 中按 score 范围取出消息 ID
        Set<String> messageIds = redisClient.boundZSetOps(aiChatMemoryKey + indexKeySuffix).rangeByScore(minScore, Double.POSITIVE_INFINITY);
        if(ObjectUtil.isNullEmpty(messageIds)){
            return CollectionUtil.newArrayList();
        }

        // 从消息主体 HASH 中批量取出消息 JSON
        List<String> messages = redisClient.<String, String>boundHashOps(aiChatMemoryKey).multiGet(messageIds);
        if(ObjectUtil.isNullEmpty(messages)){
            return CollectionUtil.newArrayList();
        }

        return messages.stream()
                .filter(Objects::nonNull)
                .map(this::jsonToMessage)
                .toList();

    }

    /**
     * 全量保存会话消息
     * <p>
     * 先清空该会话原有数据，再整体重写：为无 ID 的消息生成雪花 ID 与写入时间戳写入消息元数据，
     * 消息 JSON 写入主体 HASH，同时维护全部消息顺序索引与用户对话记录索引。
     * </p>
     *
     * @param conversationId 会话 ID
     * @param messages 消息列表
     */
    @Override
    public void saveAll(@NotNull String conversationId, @NotNull List<Message> messages) {

        String aiChatMemoryKey = AI_CHAT_MEMORY_KEY + conversationId;

        // 全量重写：先删除该会话原有数据
        deleteByConversationId(conversationId);

        Map<String, String> messageMap = CollectionUtil.newHashMap();
        Set<ZSetOperations.TypedTuple<String>> orderTuples = CollectionUtil.newHashSet();
        Set<ZSetOperations.TypedTuple<String>> recordTuples = CollectionUtil.newHashSet();
        messages.forEach(message -> {

            Map<String, Object> metadata = message.getMetadata();
            String messageId = (String) metadata.get(ChatMessageConstant.ID);
            String createTime = (String) metadata.get(ChatMessageConstant.CREATE_TIME);

            // 首次写入的消息：生成雪花 ID 并记录写入时间戳到消息元数据
            if(ObjectUtil.isNullEmpty(messageId)){
                createTime = String.valueOf(System.currentTimeMillis());
                messageId = String.valueOf(IdWorker.getId());
                metadata.put(ChatMessageConstant.ID, messageId);
                metadata.put(ChatMessageConstant.CREATE_TIME, createTime);
            }

            messageMap.put(messageId, Objects.requireNonNull(messageToJson(message)));
            orderTuples.add(ZSetOperations.TypedTuple.of(messageId, Double.parseDouble(createTime)));

            MessageType messageType = message.getMessageType();

            // 用户对话记录索引仅收集 USER / ASSISTANT 消息
            if (MessageType.USER == messageType || MessageType.ASSISTANT == messageType) {
                recordTuples.add(ZSetOperations.TypedTuple.of(messageId, Double.parseDouble(createTime)));
            }

        });

        // 消息主体与两个索引分别写入
        BoundHashOperations<String, String, String> hashOps = redisClient.boundHashOps(aiChatMemoryKey);
        hashOps.putAll(messageMap);

        redisClient.boundZSetOps(aiChatMemoryKey + AI_CHAT_MEMORY_ORDER_KEY_SUFFIX).add(orderTuples);

        if(!recordTuples.isEmpty()){
            redisClient.boundZSetOps(aiChatMemoryKey + AI_CHAT_MEMORY_RECORD_KEY_SUFFIX).add(recordTuples);
        }

    }

    /**
     * 删除会话的全部对话记忆（消息主体与两个索引 key）
     *
     * @param conversationId 会话 ID
     */
    @Override
    public void deleteByConversationId(@NotNull String conversationId) {
        String aiChatMemoryKey = AI_CHAT_MEMORY_KEY + conversationId;
        redisClient.delete(List.of(aiChatMemoryKey, aiChatMemoryKey + AI_CHAT_MEMORY_ORDER_KEY_SUFFIX, aiChatMemoryKey + AI_CHAT_MEMORY_RECORD_KEY_SUFFIX));
    }

    /**
     * 删除会话中的指定消息（消息主体与两个索引中的对应记录）
     *
     * @param conversationId 会话 ID
     * @param messageId 消息 ID
     */
    @Override
    public void deleteByMessageId(@NotNull String conversationId, @NotNull String messageId) {
        String aiChatMemoryKey = AI_CHAT_MEMORY_KEY + conversationId;
        redisClient.<String, String>boundHashOps(aiChatMemoryKey).delete(messageId);
        redisClient.boundZSetOps(aiChatMemoryKey + AI_CHAT_MEMORY_ORDER_KEY_SUFFIX).remove(messageId);
        redisClient.boundZSetOps(aiChatMemoryKey + AI_CHAT_MEMORY_RECORD_KEY_SUFFIX).remove(messageId);
    }

    /**
     * 将消息对象序列化为 JSON
     * <p>
     * 复制为 {@link ChatMessage} 存储结构：消息类型转为小写字符串值，
     * 并按消息类型补充工具调用、工具响应等特有属性。
     * </p>
     *
     * @param message 消息对象
     * @return 消息 JSON
     */
    public String messageToJson(Message message) {

        ChatMessage chatMessage = CglibUtil.copy(message, ChatMessage.class);
        chatMessage.setMessageType(message.getMessageType().getValue().toLowerCase());
        chatMessage.setTextContent(message.getText());

        // AI 消息补充工具调用信息
        if(message instanceof AssistantMessage assistantMessage){
            chatMessage.setToolCalls(assistantMessage.getToolCalls());
        }

        // 工具响应消息补充工具响应信息
        if(message instanceof ToolResponseMessage toolResponseMessage){
            chatMessage.setToolResponses(toolResponseMessage.getResponses());
        }

        return JacksonUtil.objectToJson(chatMessage);

    }

    /**
     * 将 JSON 反序列化为消息对象
     * <p>
     * 根据消息类型构建对应的 Spring AI 消息：
     * SYSTEM / USER / ASSISTANT / TOOL，其中 USER 与 ASSISTANT 消息会还原媒体与元数据等信息。
     * </p>
     *
     * @param json 消息 JSON
     * @return 消息对象
     * @throws RuntimeException 消息数据非法（反序列化为空或消息类型缺失、无法识别）时抛出
     */
    public Message jsonToMessage(String json){

        ChatMessage chatMessage = JacksonUtil.jsonToObject(json, ChatMessage.class);
        if(ObjectUtil.isNullEmpty(chatMessage)){
            throw new RuntimeException("message data conversion error");
        }

        String messageTypeString = chatMessage.getMessageType();
        if(ObjectUtil.isNullEmpty(messageTypeString)){
            throw new RuntimeException("messageType is null");
        }

        // 按消息类型还原为对应的消息对象
        MessageType messageType = MessageType.fromValue(messageTypeString);
        switch (messageType) {
            case SYSTEM -> {
                return new SystemMessage(chatMessage.getTextContent());
            }
            case USER ->  {
                return UserMessage.builder()
                        .text(chatMessage.getTextContent())
                        .metadata(chatMessage.getMetadata())
                        .media(chatMessage.getMedia())
                        .build();
            }
            case ASSISTANT -> {
                return new AssistantMessage(chatMessage.getTextContent(), chatMessage.getMetadata(), chatMessage.getToolCalls(),chatMessage.getMedia());
            }
            case TOOL -> {
                return new ToolResponseMessage(chatMessage.getToolResponses(), chatMessage.getMetadata());
            }
            default -> throw new BusinessResultException("message data conversion error");
        }

    }

}
