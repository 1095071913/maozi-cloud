package com.maozi.ai.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maozi.db.domain.AbstractBaseDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * AI交互会话实体类
 * <p>对应数据库表 ai_chat_conversation_record，存储用户与 AI 的交互会话基本信息，
 * 包括所属用户、会话标识和会话标题等属性。</p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_conversation_record")
public class ChatConversationRecordDo extends AbstractBaseDomain {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 主键ID，采用雪花算法自动分配 */
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	/** 用户ID，标识对话所属的用户 */
	private Long userId;

	/** 对话标题 */
	private String title;

}
