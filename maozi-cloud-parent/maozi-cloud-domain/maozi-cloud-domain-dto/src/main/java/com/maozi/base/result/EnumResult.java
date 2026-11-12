package com.maozi.base.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 枚举结果
 * <p>
 * 用于返回枚举类型的数据结构，包含枚举值和描述。
 * 适用于下拉选择、数据字典等场景。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EnumResult")
public class EnumResult implements Serializable {

	/** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

	/** 枚举编码 */
	@Schema(description = "枚举编码")
	private Integer value;

	/** 枚举描述 */
	@Schema(description = "描述")
	private String desc;

}
