package com.maozi.ai.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * AI文生图返回结果
 * <p>
 * 包含提示消息与生成图片的地址列表。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/22 20:48
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatGenerateImageResult implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 提示消息（正常完成时为"图片生成完成 ..."，生成期间被停止时为 null） */
    @Schema(description = "消息")
    private String message;

    /** 生成图片的地址列表 */
    @Schema(description = "图片列表")
    private Set<String> images;

}
