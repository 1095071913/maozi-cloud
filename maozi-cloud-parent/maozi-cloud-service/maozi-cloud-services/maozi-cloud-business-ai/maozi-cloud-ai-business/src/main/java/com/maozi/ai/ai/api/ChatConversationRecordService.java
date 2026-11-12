package com.maozi.ai.ai.api;

import com.maozi.common.result.error.exception.BusinessResultException;

/**
 * AI交互会话业务接口
 * <p>
 * 定义AI交互会话领域对内暴露的业务方法契约，
 * 供本模块内部其他业务（如AI对话流程）调用。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public interface ChatConversationRecordService {

    /**
     * 校验会话是否存在
     *
     * @param conversationId 会话 ID
     * @throws BusinessResultException 会话不存在时抛出
     */
    void has(Long conversationId);

}
