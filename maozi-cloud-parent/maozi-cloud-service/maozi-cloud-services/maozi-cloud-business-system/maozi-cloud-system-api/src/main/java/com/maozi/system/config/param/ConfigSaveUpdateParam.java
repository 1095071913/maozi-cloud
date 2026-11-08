package com.maozi.system.config.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局配置保存/更新参数
 * <p>
 * 用于新增或修改全局配置时的请求参数封装。
 * 仅允许传入配置名称、配置别名、配置类型、配置值与排序字段。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSaveUpdateParam implements Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 配置名称（全局唯一键） */
	@NotEmpty(message = "名称不能为空")
	@Schema(description = "名称")
	private String name;

	/** 配置别名 */
	@Schema(description = "别名")
	@NotEmpty(message = "别名不能为空")
	private String alias;

	/** 配置类型 */
	@NotEmpty(message = "类型不能为空")
	@Schema(description = "类型")
	private String type;

	/** 配置值 */
	@NotEmpty(message = "配置值不能为空")
	@Schema(description = "配置值")
	private String value;

	/** 排序序号，值越小越靠前 */
	@Schema(description = "排序")
	private Integer sort;

}
