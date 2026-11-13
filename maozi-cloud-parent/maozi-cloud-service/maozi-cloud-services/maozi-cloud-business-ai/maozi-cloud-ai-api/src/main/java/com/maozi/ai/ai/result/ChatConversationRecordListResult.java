package com.maozi.ai.ai.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI会话列表查询返回结果
 * <p>
 * 会话分页列表中单条会话的展示数据，仅包含会话 ID 与会话标题。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/17 09:34
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationRecordListResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** AI聊天会话ID */
    @Schema(description = "AI聊天会话ID")
    private Long id;

    /** 标题 */
    @Schema(description = "标题")
    private String title;

}
