package com.maozi.ai.ai.api.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.ai.ai.api.ChatConversationRecordService;
import com.maozi.ai.ai.domain.ChatConversationRecordDO;
import com.maozi.ai.ai.mapper.ChatConversationRecordMapper;
import com.maozi.ai.ai.param.ChatConversationRecordListParam;
import com.maozi.ai.ai.result.ChatConversationRecordListResult;
import com.maozi.base.param.PageParam;
import com.maozi.base.param.RequestParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ObjectUtil;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.service.api.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI交互会话服务实现类
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
@RequiredArgsConstructor
public class ChatConversationRecordServiceImpl extends BaseServiceImpl<ChatConversationRecordMapper, ChatConversationRecordDO>
		implements ChatConversationRecordService {

	/** 资源名称，用于异常提示信息 */
	public static final String RESOURCE_NAME = "AI聊天会话记录";

	/** 会话标题最大长度 */
	private static final int TITLE_MAX_LENGTH = 20;

	/** 对话记忆，用于删除会话时清空该会话的对话记忆 */
	private final ChatMemory chatMemory;

	/** 获取资源名称 */
	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	/**
	 * 校验会话是否存在
	 *
	 * @param conversationId 会话 ID
	 * @throws BusinessResultException 会话不存在时抛出
	 */
	@Override
	public void has(Long conversationId) {
		LambdaQueryWrapper<ChatConversationRecordDO> wrapper = Wrappers.lambdaQuery();
		wrapper.eq(ChatConversationRecordDO::getId, conversationId);
		super.checkHas(wrapper);
	}

	/**
	 * 查询当前用户的会话分页列表
	 *
	 * @param pageParam 分页查询参数
	 * @return 会话分页列表
	 */
	public PageResult<ChatConversationRecordListResult> adminList(PageParam<ChatConversationRecordListParam> pageParam) {

		Long userId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUserId);

		MPJLambdaWrapper<ChatConversationRecordDO> wrapper = super.buildQueryWrapper(pageParam, ChatConversationRecordListResult.class);
		wrapper.eq(ChatConversationRecordDO::getUserId,userId);

		Page<ChatConversationRecordListResult> page = super.selectJoinListPage(
				super.convertPage(pageParam),
				ChatConversationRecordListResult.class,
				wrapper
		);

		// 转换分页结果并填充关联数据
		PageResult<ChatConversationRecordListResult> pageResult = super.convertPageResult(page);
		super.setRelationData(pageResult, ChatConversationRecordListResult.class);

		return pageResult;
	}

	/**
	 * 创建会话
	 * <p>
	 * 以首条消息作为会话标题，超过 20 字时截断前 20 字并追加省略号；
	 * 会话归属当前登录用户。
	 * </p>
	 *
	 * @param param 携带首条消息内容的请求参数
	 * @return 新建会话 ID
	 * @throws BusinessResultException 消息内容为空时抛出
	 */
	public Long adminSave(RequestParam<String> param) {

		String title = param.getData();
		if(ObjectUtil.isNullEmpty(title)){
			throw new BusinessResultException("消息不能为空");
		}

		ChatConversationRecordDO domain = new ChatConversationRecordDO();
		domain.setTitle(title.length() > TITLE_MAX_LENGTH ? StrUtil.subPre(title, TITLE_MAX_LENGTH) + "..." : title);
		domain.setUserId(CurrentUserInfoContext.CONTEXT.get(CurrentUserInfo::getUserId));

		super.save(domain);

		return domain.getId();

	}

	/**
	 * 更新会话标题
	 *
	 * @param conversationId 会话 ID
	 * @param param 携带新标题的请求参数
	 * @throws BusinessResultException 标题为空或长度超过 20 字符时抛出
	 */
	public void adminUpdateTitle(Long conversationId, RequestParam<String> param) {

		String title = param.getData();
		if(ObjectUtil.isNullEmpty(title)){
			throw new BusinessResultException("标题不能为空");
		}

		if (title.length() > TITLE_MAX_LENGTH) {
			throw new BusinessResultException("标题不能大于" + TITLE_MAX_LENGTH + "字符");
		}

		ChatConversationRecordDO domain = new ChatConversationRecordDO();
		domain.setId(conversationId);
		domain.setTitle(title);

		super.updateById(domain);

	}

	/**
	 * 删除会话，同时清空该会话的对话记忆
	 *
	 * @param conversationId 会话 ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void adminRemove(String conversationId){
		super.removeById(conversationId);
		chatMemory.clear(conversationId);
	}

}
