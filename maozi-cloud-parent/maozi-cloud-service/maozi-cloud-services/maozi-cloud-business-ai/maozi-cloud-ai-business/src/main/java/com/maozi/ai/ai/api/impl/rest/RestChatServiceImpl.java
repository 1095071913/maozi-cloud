package com.maozi.ai.ai.api.impl.rest;

import com.maozi.ai.ai.api.impl.ChatServiceImpl;
import com.maozi.ai.ai.api.rest.RestChatService;
import com.maozi.ai.ai.param.ChatGenerateImageParam;
import com.maozi.ai.ai.result.ChatGenerateImageResult;
import com.maozi.ai.ai.result.ChatItemResult;
import com.maozi.ai.ai.result.ChatListResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;

/**
 * AI对话服务 REST 实现
 * <p>
 * 基于 Spring AI 与阿里云 DashScope 实现流式对话与文生图，
 * 对话进行中状态通过 Redis 标记维护，以支持停止对话与页面状态展示。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@RestService
@RequiredArgsConstructor
public class RestChatServiceImpl extends ChatServiceImpl implements RestChatService {

    /** AI文生图 */
    @Override
    @SneakyThrows
    public AbstractBaseResult<ChatGenerateImageResult> generateImageResult(ChatGenerateImageParam param) {
        return ResultUtil.success(super.adminGenerateImage(param));
    }

    /** 查询会话的对话消息列表 */
    @Override
    public AbstractBaseResult<ChatListResult> listResult(String conversationId) {
        return ResultUtil.success(super.adminList(conversationId));
    }


    /** 停止会话进行中的对话或文生图 */
    @Override
    public AbstractBaseResult<Void> stopResult(String conversationId) {
        super.adminStop(conversationId);
        return ResultUtil.success();
    }

    /** 查询会话中指定时间之后的增量消息列表 */
    @Override
    public AbstractBaseResult<List<ChatItemResult>> getAfterMessagesResult(String conversationId, Long createTime) {
        return ResultUtil.success(super.adminGetAfterMessages(conversationId, createTime));
    }

    /** 删除会话中的指定消息 */
    @Override
    public AbstractBaseResult<Void> removeMessageResult(String conversationId, String messageId) {
        chatMemoryRepository.deleteByMessageId(conversationId, messageId);
        return ResultUtil.success();
    }

}
