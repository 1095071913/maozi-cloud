package com.maozi.ai.ai.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maozi.ai.ai.api.ChatConversationRecordService;
import com.maozi.ai.ai.domain.ChatConversationRecordDo;
import com.maozi.ai.ai.mapper.ChatConversationRecordMapper;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.service.api.impl.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * AI交互会话服务实现类
 * <p>实现AI交互会话相关的业务逻辑，提供会话存在性校验能力；
 * 会话的保存/更新与分页列表查询由 REST 层实现类
 * RestChatConversationRecordImpl 提供并复用本类的通用数据访问能力。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class ChatConversationRecordServiceImpl extends BaseServiceImpl<ChatConversationRecordMapper, ChatConversationRecordDo>
		implements ChatConversationRecordService {

	/** 资源名称，用于异常提示信息 */
	private static final String RESOURCE_NAME = "AI聊天会话记录";

	/**
	 * 获取资源名称
	 *
	 * @return 资源名称字符串
	 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}


	/**
	 * 校验会话是否存在
	 * <p>
	 * 按会话 ID 统计记录数，会话不存在时抛出数据不存在的业务异常。
	 * </p>
	 *
	 * @param conversationId 会话 ID
	 * @throws BusinessResultException 会话不存在时抛出
	 */
	@Override
	public void has(Long conversationId) {
		LambdaQueryWrapper<ChatConversationRecordDo> wrapper = Wrappers.lambdaQuery();
		wrapper.eq(ChatConversationRecordDo::getId, conversationId);
		checkHas(wrapper);
	}

}
