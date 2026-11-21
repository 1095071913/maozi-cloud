package com.maozi.ai.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI文生图请求参数
 * <p>
 * 继承对话请求参数，追加生成数量与图片宽高，宽高与数量未传时使用默认值。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/8/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatGenerateImageParam extends ChatParam implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 生成数量，未传时默认生成 1 张 */
    @Schema(description = "生成数量")
    private Integer count;

    /** 图片高度，未传时默认 1024 像素 */
    @Schema(description = "图片高度")
    private Integer height;

    /** 图片宽度，未传时默认 1024 像素 */
    @Schema(description = "图片宽度")
    private Integer width;

}
