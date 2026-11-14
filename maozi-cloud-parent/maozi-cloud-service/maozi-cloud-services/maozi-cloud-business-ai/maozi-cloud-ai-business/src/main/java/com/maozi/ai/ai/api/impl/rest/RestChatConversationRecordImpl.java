package com.maozi.ai.ai.api.impl.rest;

import com.maozi.ai.ai.api.impl.ChatConversationRecordServiceImpl;
import com.maozi.ai.ai.api.rest.RestChatConversationRecord;
import com.maozi.ai.ai.param.ChatConversationRecordListParam;
import com.maozi.ai.ai.result.ChatConversationRecordListResult;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;

/**
 * AI会话管理服务 REST 实现
 *
 * @author maozi
 * @date 2026/11/14
 */
@RestService
public class RestChatConversationRecordImpl extends BaseServiceResultImpl<ChatConversationRecordServiceImpl, Void>
        implements RestChatConversationRecord {

    /** 查询当前用户的会话分页列表 */
    @Override
    public AbstractBaseResult<PageResult<ChatConversationRecordListResult>> listResult(
            PageParam<ChatConversationRecordListParam> pageParam) {
        return ResultUtil.success(service.adminList(pageParam));
    }

    /** 创建会话，以首条消息作为会话标题 */
    @Override
    public AbstractBaseResult<Long> createResult(RequestParam<String> param) {
        return ResultUtil.success(service.adminSave(param));
    }

    /** 更新会话标题 */
    @Override
    public AbstractBaseResult<Void> updateTitleResult(Long conversationId, RequestParam<String> param) {
        service.adminUpdateTitle(conversationId, param);
        return ResultUtil.success();
    }

    /** 删除会话，同时清空该会话的对话记忆 */
    @Override
    public AbstractBaseResult<Void> removeResult(String conversationId) {
        service.adminRemove(conversationId);
        return ResultUtil.success();
    }

}
