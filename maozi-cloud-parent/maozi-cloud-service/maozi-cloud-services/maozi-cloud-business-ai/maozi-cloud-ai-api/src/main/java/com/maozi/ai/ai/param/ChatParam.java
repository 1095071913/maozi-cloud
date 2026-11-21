package com.maozi.ai.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI对话请求参数
 * <p>
 * 携带目标会话 ID 与用户输入内容，
 * 可选携带图片地址列表（多模态对话）与提示词配置 key。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 对话ID，即目标会话 ID */
    @Schema(description = "对话ID")
    @NotNull(message = "对话ID不能为空")
    private Long conversationId;

    /** 提示词配置 key，传入时从系统配置服务获取提示词内容作为系统消息 */
    @Schema(description = "提示词配置")
    private String promptConfig;

    /** 消息内容 */
    @Schema(description = "消息")
    @NotEmpty(message = "消息不能为空")
    private String message;

    /** 图片地址列表，携带时进行多模态（图生文）对话 */
    @Schema(description = "图片列表")
    private List<String> images;

}
