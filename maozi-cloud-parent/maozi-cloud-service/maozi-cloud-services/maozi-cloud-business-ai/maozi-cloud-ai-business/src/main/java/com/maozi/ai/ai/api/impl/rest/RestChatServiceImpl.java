package com.maozi.ai.ai.api.impl.rest;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.maozi.ai.ai.api.ChatConversationRecordService;
import com.maozi.ai.ai.api.impl.ChatServiceImpl;
import com.maozi.ai.ai.api.rest.RestChatService;
import com.maozi.ai.ai.config.ChatMemoryRepository;
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
import com.maozi.service.api.annotation.RestService;
import com.maozi.system.config.api.rpc.RpcConfigService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI对话服务 REST 实现
 * <p>
 * 基于 Spring AI 与阿里云 DashScope 实现 AI 对话能力，包括：
 * <ul>
 *   <li>流式对话（chat）：支持纯文本与多模态图片输入，以 SSE 流式返回 AI 回复</li>
 *   <li>文生图（generateImage）：根据用户描述生成图片</li>
 *   <li>对话消息的查询、停止、增量获取与删除</li>
 * </ul>
 * 对话进行中的状态通过 Redis 标记维护，用于支持停止对话与页面状态展示。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
@RequiredArgsConstructor
public class RestChatServiceImpl extends ChatServiceImpl implements RestChatService {

    private static final int DEFAULT_IMAGE_COUNT = 1;

    private static final long CHAT_STOP_KEY_EXPIRE_SECONDS = 60L;

    /** 文生图默认图片尺寸（宽与高相同） */
    private static final int DEFAULT_IMAGE_SIZE = 1024;

    /** 视觉模型（图生文）对话温度参数 */
    private static final double VISION_MODEL_TEMPERATURE = 0.7D;

    /** 对话客户端，用于与 AI 模型交互 */
    private final ChatClient chatClient;

    /** 对话记忆，用于保存和读取会话上下文 */
    private final ChatMemory chatMemory;

    /** 图片模型，用于文生图 */
    private final ImageModel imageModel;

    /** 对话记忆存储，直接操作底层会话消息数据 */
    private final ChatMemoryRepository chatMemoryRepository;

    /** 图生文（视觉理解）模型名称 */
    @Value("${spring.ai.dashscope.chat.options.text_image_model:}")
    private String imageModelName;

    /** 系统配置 RPC 服务，用于获取提示词配置 */
    @RemoteResource
    private RpcConfigService rpcConfigService;

    /** 会话记录服务，用于校验会话是否存在 */
    @Resource(name = "chatConversationRecordServiceImpl")
    private ChatConversationRecordService  chatConversationRecordService;

    /** 对话进行中标记 key 前缀：key 存在表示该会话正在对话，删除即停止对话 */
    public static final String AI_CHAT_STOP_KEY = RedisUtil.REDIS_KEY_PREFIX + "chat:status:";

    /** 文生图进行中标记 key 前缀：key 被删除表示文生图请求已被停止 */
    public static final String AI_CHAT_GENERATE_IMAGE_STOP_KEY =
            RedisUtil.REDIS_KEY_PREFIX + "chat:generate:image:status:";

    /**
     * AI流式对话
     * <p>
     * 校验会话后构建用户消息（支持附带 png/jpg/jpeg/gif 图片进行多模态对话），
     * 有图片时切换为图生文模型并启用多模态；
     * 根据配置 key 从系统配置服务获取提示词作为系统消息。
     * 以 SSE 流式返回 AI 回复，通过 Redis 标记支持中途停止对话，
     * 客户端断开连接时将已生成的部分回复写入会话记忆。
     * </p>
     *
     * @param param 对话参数（会话 ID、消息内容、图片列表、提示词配置）
     * @return AI 回复的分片流，最后追加一条对话完成（FINISH）消息
     * @throws BusinessResultException 会话不存在、图片类型不受支持或提示词配置获取失败时抛出
     */
    @Override
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
                    chatMemory.add(conversationIdFinal,new AssistantMessage(aiChatOutputMessage.toString()));
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
     * AI文生图
     * <p>
     * 将用户描述写入会话记忆后调用图片模型生成图片（尺寸与数量未传时取默认值），
     * 生成期间通过 Redis 标记维护进行中状态（按生成数量放大过期时间兜底）。
     * 若生成期间用户调用了停止接口，则不保存结果直接返回空图片列表；
     * 正常完成后将生成结果（回复与图片）写入会话记忆并返回图片地址。
     * </p>
     *
     * @param param 文生图参数（会话 ID、消息内容、生成数量、图片宽高）
     * @return 生成结果，包含提示消息与图片地址列表
     */
    @Override
    @SneakyThrows
    public AbstractBaseResult<ChatGenerateImageResult> generateImage(ChatGenerateImageParam param) {

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

        try{

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
            for(String image : images){
                medias.add(new Media(MimeTypeUtils.IMAGE_PNG, new URI(image)));
            }

            // 文生图标记已被删除，说明生成期间用户调用了停止接口，不保存结果
            if(!redisClient.hasKey(chatGenerateImageStopLockKey)){
                return ResultUtil.success(new ChatGenerateImageResult(null,Set.of()));
            }

            // 生成结果（回复与图片）写入会话记忆
            String responseMessage = "图片生成完成 ...";
            chatMemory.add(conversationId, new AssistantMessage(
                    responseMessage, CollectionUtil.newHashMap(), CollectionUtil.newArrayList(), medias));

            return ResultUtil.success(new ChatGenerateImageResult(responseMessage, images));

        }finally {

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
     * 查询会话的用户对话记录列表
     * <p>
     * 返回会话中的用户与 AI 对话消息（仅对话记录，不含系统消息），
     * 同时通过对话进行中标记返回当前会话是否正在对话中。
     * </p>
     *
     * @param conversationId 会话 ID
     * @return 对话消息列表及会话对话中状态
     */
    @Override
    public AbstractBaseResult<ChatListResult> list(String conversationId) {

        // 是否对话中：对话进行中标记存在即正在对话
        Boolean isLocked = RedisUtil.getRedisClient().hasKey(AI_CHAT_STOP_KEY + conversationId);

        List<Message> messages = chatMemoryRepository.findRecordByConversationId(conversationId);
        if(CollectionUtil.isEmpty(messages)){
            return ResultUtil.success(new ChatListResult(
                    isLocked,
                    CollectionUtil.newArrayList())
            );
        }

        return ResultUtil.success(new ChatListResult(isLocked,convertMessageList(messages)));

    }


    /**
     * 停止会话的进行中对话或文生图
     * <p>
     * 删除对话与文生图的进行中标记：
     * 流式对话通道因标记被删除而结束，
     * 文生图生成期间检测到标记被删除则不保存结果。
     * </p>
     *
     * @param conversationId 会话 ID
     * @return 空结果
     */
    @Override
    public AbstractBaseResult<Void> stop(String conversationId) {
        RedisUtil.getRedisClient().delete(
                CollectionUtil.newArrayList(
                        AI_CHAT_STOP_KEY + conversationId,
                        AI_CHAT_GENERATE_IMAGE_STOP_KEY + conversationId
                )
        );
        return ResultUtil.success();
    }

    /**
     * 查询会话中指定时间之后的消息列表
     * <p>
     * 仅返回写入时间晚于 createTime 的用户与 AI 对话消息，用于页面增量拉取新消息。
     * </p>
     *
     * @param conversationId 会话 ID
     * @param createTime 时间毫秒值，查询该时间之后（不含等于）写入的消息
     * @return 增量消息列表
     */
    @Override
    public AbstractBaseResult<List<ChatItemResult>> getAfterMessages(String conversationId, Long createTime) {

        List<Message> messages = chatMemoryRepository.findRecordAfterTimestamp(conversationId, createTime);
        if(ObjectUtil.isNullEmpty(messages)){
            return ResultUtil.success(CollectionUtil.newArrayList());
        }

        return ResultUtil.success(convertMessageList(messages));

    }

    /**
     * 删除会话中的指定消息
     * <p>
     * 从会话记忆中删除指定 ID 的消息（含正文与顺序、记录索引）。
     * </p>
     *
     * @param conversationId 会话 ID
     * @param messageId 消息 ID
     * @return 空结果
     */
    @Override
    public AbstractBaseResult<Void> removeMessage(String conversationId, String messageId) {
        chatMemoryRepository.deleteByMessageId(conversationId, messageId);
        return ResultUtil.success();
    }

}
