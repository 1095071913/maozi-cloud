package com.maozi.ai.ai.api.impl.rest;

import com.maozi.ai.ai.api.impl.ChatConversationRecordServiceImpl;
import com.maozi.ai.ai.api.rest.RestChatConversationRecord;
import com.maozi.ai.ai.param.ChatConversationRecordListParam;
import com.maozi.ai.ai.vo.ChatConversationRecordListResult;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.service.api.annotation.RestService;
import com.maozi.service.api.impl.result.BaseServiceResultImpl;

/**
 * AI会话管理服务 REST 实现
 * <p>
 * 实现当前用户的会话分页列表查询、会话创建（标题取首条消息前 20 字）、
 * 会话标题更新以及会话删除（同时清空该会话的对话记忆）。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/16 21:49
 */
@RestService
public class RestChatConversationRecordImpl extends BaseServiceResultImpl<ChatConversationRecordServiceImpl, Void>
        implements RestChatConversationRecord {

    /**
     * 查询当前用户的会话分页列表
     * <p>
     * 按当前登录用户过滤会话记录并分页查询，同时填充关联数据。
     * </p>
     *
     * @param pageParam 分页查询参数
     * @return 会话分页列表
     */
    @Override
    public AbstractBaseResult<PageResult<ChatConversationRecordListResult>> restList(
            PageParam<ChatConversationRecordListParam> pageParam) {
        return ResultUtil.success(service.list(pageParam));
    }

    /**
     * 创建会话
     * <p>
     * 以用户传入的首条消息作为标题（超过 20 字截断前 20 字并追加省略号），
     * 会话归属当前登录用户。
     * </p>
     *
     * @param param 携带首条消息内容的请求参数
     * @return 新建会话 ID
     * @throws BusinessResultException 消息内容为空时抛出
     */
    @Override
    public AbstractBaseResult<Long> restCreate(RequestParam<String> param) {
        return ResultUtil.success(service.save(param));
    }

    /**
     * 更新会话标题
     * <p>
     * 标题不能为空且不能超过 20 字符。
     * </p>
     *
     * @param conversationId 会话 ID
     * @param param 携带新标题的请求参数
     * @return 空结果
     * @throws BusinessResultException 标题为空或长度超过 20 字符时抛出
     */
    @Override
    public AbstractBaseResult<Void> restUpdateTitle(Long conversationId, RequestParam<String> param) {
        service.updateTitle(conversationId, param);
        return ResultUtil.success();
    }

    /**
     * 删除会话
     * <p>
     * 删除会话记录的同时清空该会话的对话记忆。
     * </p>
     *
     * @param conversationId 会话 ID
     * @return 空结果
     */
    @Override
    public AbstractBaseResult<Void> restRemove(String conversationId) {
        service.remove(conversationId);
        return ResultUtil.success();
    }

}
