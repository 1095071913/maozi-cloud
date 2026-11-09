package com.maozi.ai.ai.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI对话消息列表返回结果
 * <p>
 * 包含会话是否正在对话中以及该会话的用户对话消息列表。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/22 20:58
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatListResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否对话中（true 表示该会话存在进行中的流式对话或文生图任务） */
    @Schema(description = "是否对话中")
    @JsonProperty("isLocked")
    private Boolean locked;

    /** 对话消息列表（仅用户与 AI 消息，不含系统消息） */
    @Schema(description = "对话内容")
    private List<ChatItemResult> items;

}
