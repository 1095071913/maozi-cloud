package com.maozi.ai.ai.result;

import com.maozi.ai.ai.enums.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI对话消息结果
 * <p>
 * 前端展示的单条对话消息，包含消息 ID、消息类型（用户/AI）、
 * 消息内容、携带的图片地址以及创建时间。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/17 10:14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatItemResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息 ID（雪花 ID，存储于消息元数据） */
    @Schema(description = "ID")
    private String id;

    /** 消息类型（用户消息 / AI消息） */
    @Schema(description = "AI对话消息类型")
    private ChatMessageType type;

    /** 消息文本内容 */
    @Schema(description = "消息")
    private String message;

    /** 消息携带的图片地址列表 */
    @Schema(description = "图片列表地址")
    private List<String> images;

    /** 消息创建时间（由元数据中的写入时间戳转换而来） */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
