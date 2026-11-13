package com.maozi.ai.ai.api.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.maozi.ai.ai.api.ChatConversationRecordService;
import com.maozi.ai.ai.api.ChatService;
import com.maozi.ai.ai.config.ChatMemoryRepository;
import com.maozi.ai.ai.constant.ChatMessageConstant;
import com.maozi.ai.ai.enums.ChatMessageType;
import com.maozi.ai.ai.enums.ChatType;
import com.maozi.ai.ai.param.ChatGenerateImageParam;
import com.maozi.ai.ai.param.ChatParam;
import com.maozi.ai.ai.vo.ChatGenerateImageResult;
import com.maozi.ai.ai.vo.ChatItemResult;
import com.maozi.ai.ai.vo.ChatListResult;
import com.maozi.ai.ai.vo.ChatResult;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.redis.utils.RedisUtil;
import com.maozi.service.api.annotation.RemoteResource;
import com.maozi.system.config.api.rpc.RpcConfigService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI对话服务基础实现
 * <p>
 * 提供文生图、会话消息列表查询、增量消息查询、停止对话等能力，
 * 以及对话消息到前端展示结果的通用转换，供 REST 层实现类继承使用。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/16 10:05
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /** 文生图默认生成图片数量 */
    protected static final int DEFAULT_IMAGE_COUNT = 1;

    /** 文生图默认图片尺寸（宽与高相同） */
    protected static final int DEFAULT_IMAGE_SIZE = 1024;

    /** 视觉模型（图生文）对话温度参数 */
    protected static final double VISION_MODEL_TEMPERATURE = 0.7D;

    /** 对话进行中标记的过期时间（秒），防止异常导致标记残留 */
    protected static final long CHAT_STOP_KEY_EXPIRE_SECONDS = 60L;

    /** 对话进行中标记 key 前缀：key 存在表示该会话正在对话，删除即停止对话 */
    protected static final String AI_CHAT_STOP_KEY = RedisUtil.REDIS_KEY_PREFIX + "chat:status:";

    /** 文生图进行中标记 key 前缀：key 被删除表示文生图请求已被停止 */
    protected static final String AI_CHAT_GENERATE_IMAGE_STOP_KEY = RedisUtil.REDIS_KEY_PREFIX + "chat:generate:image:status:";

    /** 图片模型，用于文生图 */
    @Resource
    protected ImageModel imageModel;

    /** 对话记忆，用于保存和读取会话上下文 */
    @Resource
    protected ChatMemory chatMemory;

    /** 对话客户端，用于与 AI 模型交互 */
    @Resource
    protected ChatClient chatClient;

    /** 系统配置 RPC 服务，用于获取提示词配置 */
    @RemoteResource
    protected RpcConfigService rpcConfigService;

    /** 对话记忆存储，直接操作底层会话消息数据 */
    @Resource
    protected ChatMemoryRepository chatMemoryRepository;

    /** 会话记录服务，用于校验会话是否存在 */
    @Resource(name = "chatConversationRecordServiceImpl")
    protected ChatConversationRecordService chatConversationRecordService;

    /** 图生文（视觉理解）模型名称 */
    @Value("${spring.ai.dashscope.chat.options.text_image_model:}")
    protected String imageModelName;

    @SneakyThrows
    public Flux<AbstractBaseResult<ChatResult>> chat(ChatParam param) {

        Long conversationId = param.getConversationId();

        // 校验会话是否存在
        chatConversationRecordService.has(conversationId);

        // 构建用户消息
        UserMessage.Builder userMessageBuilder = UserMessage.builder().text(param.getMessage());

        // 携带图片时构建多模态消息
        List<String> images = param.getImages();
        boolean hasImage = ObjectUtil.isNotNullEmpty(images);
        if(hasImage){
            List<Media> medias = CollectionUtil.newArrayList();
            for(String image : images){
                // 根据图片后缀解析对应的 MIME 类型
                MimeType mimeType;
                if(image.endsWith(".png")){
                    mimeType = MimeTypeUtils.IMAGE_PNG;
                }else if(image.endsWith(".jpg") || image.endsWith(".jpeg")){
                    mimeType = MimeTypeUtils.IMAGE_JPEG;
                }else if(image.endsWith(".gif")){
                    mimeType = MimeTypeUtils.IMAGE_GIF;
                }else{
                    throw new BusinessResultException("不支持该图片类型");
                }
                medias.add(new Media(mimeType, new URI(image)));
            }
            userMessageBuilder.media(medias);
        }

        UserMessage userMessage = userMessageBuilder.build();
        ChatClient.ChatClientRequestSpec chatClientRequest;
        if(hasImage){

            // 设置消息格式为图片
            userMessage.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

            // 有图片时使用图生文（视觉理解）模型，启用多模态与高分辨率图片处理
            Prompt chatPrompt = new Prompt(userMessage,
                    DashScopeChatOptions.builder()
                            .withModel(imageModelName)        // 使用视觉模型
                            .withMultiModel(true)             // 启用多模态
                            .withVlHighResolutionImages(true) // 启用高分辨率图片处理
                            .withTemperature(VISION_MODEL_TEMPERATURE)
                            .build());

            chatClientRequest = chatClient.prompt(chatPrompt);

        }else{
            // 纯文本对话使用默认模型
            chatClientRequest = chatClient.prompt(new Prompt(userMessage));
        }

        // 配置了提示词 key 时，从系统配置服务获取提示词内容作为系统消息
        String promptConfig = param.getPromptConfig();
        if(ObjectUtil.isNotNullEmpty(promptConfig)){
            promptConfig = rpcConfigService.rpcGet(promptConfig).getResultDataThrowError();
            if(ObjectUtil.isNotNullEmpty(promptConfig)){
                chatClientRequest.system(promptConfig);
            }
        }

        String chatStopLockKey = AI_CHAT_STOP_KEY + conversationId;
        StringRedisTemplate redisClient = RedisUtil.getRedisClient();
        ValueOperations<String, String> stringValueOperations = redisClient.opsForValue();

        // 累积 AI 已输出的内容，用于客户端断开时保存部分回复
        StringBuilder aiChatOutputMessage = new StringBuilder();
        final String conversationIdFinal = conversationId.toString();
        return chatClientRequest

                // 携带会话 ID 参数，供对话记忆 Advisor 定位当前会话并读写上下文消息
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationIdFinal))

                .stream()
                .chatResponse()

                // 请求前处理：写入对话进行中标记
                // （60 秒过期兜底，防止异常导致标记残留）
                .doFirst(() -> stringValueOperations.setIfAbsent(
                        chatStopLockKey, Boolean.TRUE.toString(), CHAT_STOP_KEY_EXPIRE_SECONDS, TimeUnit.SECONDS))

                // 异常时处理：清除对话进行中标记
                .doOnError(throwable -> redisClient.delete(chatStopLockKey))

                // 请求正常结束后处理：清除对话进行中标记
                .doOnComplete(() -> redisClient.delete(chatStopLockKey))

                // 对话进行中标记被删除（调用 stop 停止对话）时结束对话流通道
                .takeWhile(chatResponse -> redisClient.hasKey(chatStopLockKey))

                // 客户端取消订阅（断开连接）后执行：
                // 将已生成的部分回复写入会话记忆，并清除对话进行中标记
                .doOnCancel(() -> {
                    chatMemory.add(conversationIdFinal, new AssistantMessage(aiChatOutputMessage.toString()));
                    redisClient.delete(chatStopLockKey);
                })

                // 每次获取到流数据后的处理：累积回复内容并向下发送分片
                .map(chatResponse -> {

                    Generation result = chatResponse.getResult();
                    String aiChat = result == null ? "" : result.getOutput().getText();

                    aiChatOutputMessage.append(aiChat);

                    return (AbstractBaseResult<ChatResult>) ResultUtil.success(new ChatResult(ChatType.OUTPUT, aiChat));

                })

                // 追加完成消息，告诉前端对话流通道已结束 可以关闭连接
                .concatWith(Flux.just(ResultUtil.success(ChatResult.builder().type(ChatType.FINISH).build())));

    }

    /**
     * 将会话记忆中的消息对象转换为前端展示的消息结果列表
     * <p>
     * 从消息元数据中取出消息 ID 与创建时间，
     * 并收集用户消息与 AI 消息携带的媒体（图片）地址。
     * </p>
     *
     * @param messages 会话记忆中的消息列表
     * @return 前端展示的消息结果列表
     */
    protected List<ChatItemResult> convertMessageList(List<Message> messages){

        return messages.stream()
                .map(message -> {

                    Map<String, Object> metadata = message.getMetadata();
                    String id = metadata.get(ChatMessageConstant.ID).toString();
                    LocalDateTime createTime = Instant.ofEpochMilli(Long.parseLong(metadata.get(ChatMessageConstant.CREATE_TIME).toString()))
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    List<String> images = CollectionUtil.newArrayList();
                    if(message instanceof UserMessage userMessage){
                        userMessage.getMedia().forEach(media -> images.add(media.getData().toString()));
                    }

                    if(message instanceof AssistantMessage assistantMessage){
                        assistantMessage.getMedia().forEach(media -> images.add(media.getData().toString()));
                    }

                    ChatMessageType chatMessageType = message.getMessageType() == MessageType.ASSISTANT ? ChatMessageType.AI : ChatMessageType.USER;
                    return new ChatItemResult(id, chatMessageType, message.getText(), images, createTime);

                })

                .toList();

    }

    /**
     * AI文生图
     * <p>
     * 写入对话与文生图进行中标记后执行生成，生成期间标记被删除则丢弃结果；
     * 用户输入与生成结果均写入会话记忆，结束后清除标记。
     * </p>
     *
     * @param param 文生图参数
     * @return 提示消息与生成图片的地址列表（被停止时图片列表为空）
     */
    @SneakyThrows
    public ChatGenerateImageResult generateImage(ChatGenerateImageParam param) {

        StringRedisTemplate redisClient = RedisUtil.getRedisClient();
        ValueOperations<String, String> stringValueOperations = redisClient.opsForValue();

        String conversationId = param.getConversationId().toString();

        String chatStopLockKey = AI_CHAT_STOP_KEY + conversationId;
        String chatGenerateImageStopLockKey = AI_CHAT_GENERATE_IMAGE_STOP_KEY + conversationId;

        int count = ObjectUtil.isNotNullEmpty(param.getCount()) ? param.getCount() : DEFAULT_IMAGE_COUNT;

        // 写入对话与文生图进行中标记，过期时间按生成数量放大，防止异常导致标记残留
        stringValueOperations.setIfAbsent(chatStopLockKey, Boolean.TRUE.toString(),
                CHAT_STOP_KEY_EXPIRE_SECONDS * count, TimeUnit.SECONDS);
        stringValueOperations.setIfAbsent(chatGenerateImageStopLockKey, Boolean.TRUE.toString(),
                CHAT_STOP_KEY_EXPIRE_SECONDS * count, TimeUnit.SECONDS);

        try {

            // 用户输入写入会话记忆
            String message = param.getMessage();
            chatMemory.add(conversationId, new UserMessage(message));

            // 构建图片生成参数，未指定时使用默认值
            Integer height = param.getHeight();
            Integer width = param.getWidth();
            ImageOptions options = ImageOptionsBuilder.builder()
                    .height(ObjectUtil.isNotNullEmpty(height) ? height : DEFAULT_IMAGE_SIZE)
                    .width(ObjectUtil.isNotNullEmpty(width) ? width : DEFAULT_IMAGE_SIZE)
                    .N(ObjectUtil.isNotNullEmpty(count) ? count : 1)
                    .build();

            // 调用图片模型生成图片并收集图片地址
            ImageResponse response = imageModel.call(new ImagePrompt(message, options));
            Set<String> images = response.getResults().stream()
                    .map(result -> result.getOutput().getUrl()).collect(Collectors.toSet());

            List<Media> medias = CollectionUtil.newArrayList();
            for (String image : images) {
                medias.add(new Media(MimeTypeUtils.IMAGE_PNG, new URI(image)));
            }

            // 文生图标记已被删除，说明生成期间用户调用了停止接口，不保存结果
            if (!redisClient.hasKey(chatGenerateImageStopLockKey)) {
                return new ChatGenerateImageResult(null, Set.of());
            }

            // 生成结果（回复与图片）写入会话记忆
            String responseMessage = "图片生成完成 ...";
            chatMemory.add(conversationId, new AssistantMessage(
                    responseMessage, CollectionUtil.newHashMap(), CollectionUtil.newArrayList(), medias));

            return new ChatGenerateImageResult(responseMessage, images);

        } finally {

            // 无论成功与否，最后清除对话与文生图进行中标记
            RedisUtil.getRedisClient().delete(
                    CollectionUtil.newArrayList(
                            chatStopLockKey,
                            chatGenerateImageStopLockKey
                    )
            );

        }
    }

    /**
     * 查询会话的对话消息列表
     *
     * @param conversationId 会话 ID
     * @return 会话是否正在对话中以及该会话的对话消息列表
     */
    public ChatListResult list(String conversationId) {

        // 是否对话中：对话进行中标记存在即正在对话
        Boolean isLocked = RedisUtil.getRedisClient().hasKey(AI_CHAT_STOP_KEY + conversationId);

        List<Message> messages = chatMemoryRepository.findRecordByConversationId(conversationId);
        if(CollectionUtil.isEmpty(messages)){
            return new ChatListResult(
                    isLocked,
                    CollectionUtil.newArrayList()
            );
        }

        return new ChatListResult(
                isLocked,
                convertMessageList(messages)
        );

    }

    /**
     * 停止会话进行中的对话或文生图（删除进行中标记）
     *
     * @param conversationId 会话 ID
     */
    public void stop(String conversationId) {
        RedisUtil.getRedisClient().delete(
                CollectionUtil.newArrayList(
                        AI_CHAT_STOP_KEY + conversationId,
                        AI_CHAT_GENERATE_IMAGE_STOP_KEY + conversationId
                )
        );
    }

    /**
     * 查询会话中指定时间之后的消息列表
     *
     * @param conversationId 会话 ID
     * @param createTime 时间毫秒值，查询写入时间晚于该值的消息
     * @return 该时间之后写入的增量消息列表
     */
    public List<ChatItemResult> getAfterMessages(String conversationId, Long createTime) {

        List<Message> messages = chatMemoryRepository.findRecordAfterTimestamp(conversationId, createTime);
        if(ObjectUtil.isNullEmpty(messages)){
            return CollectionUtil.newArrayList();
        }

        return convertMessageList(messages);

    }

}
