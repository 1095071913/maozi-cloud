package com.maozi.ai.ai.api.impl.rest;

import com.maozi.ai.ai.api.impl.ChatServiceImpl;
import com.maozi.ai.ai.api.rest.RestChatService;
import com.maozi.ai.ai.param.ChatGenerateImageParam;
import com.maozi.ai.ai.param.ChatParam;
import com.maozi.ai.ai.vo.ChatGenerateImageResult;
import com.maozi.ai.ai.vo.ChatItemResult;
import com.maozi.ai.ai.vo.ChatListResult;
import com.maozi.ai.ai.vo.ChatResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.service.api.annotation.RestService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;

import java.util.List;

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
    public Flux<AbstractBaseResult<ChatResult>> restChat(ChatParam param) {
        return chat(param);
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
    public AbstractBaseResult<ChatGenerateImageResult> restGenerateImage(ChatGenerateImageParam param) {
        return ResultUtil.success(generateImage(param));
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
    public AbstractBaseResult<ChatListResult> restList(String conversationId) {
        return ResultUtil.success(list(conversationId));
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
    public AbstractBaseResult<Void> restStop(String conversationId) {
        stop(conversationId);
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
    public AbstractBaseResult<List<ChatItemResult>> restGetAfterMessages(String conversationId, Long createTime) {
        return ResultUtil.success(getAfterMessages(conversationId, createTime));
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
    public AbstractBaseResult<Void> restRemoveMessage(String conversationId, String messageId) {
        chatMemoryRepository.deleteByMessageId(conversationId, messageId);
        return ResultUtil.success();
    }

}
