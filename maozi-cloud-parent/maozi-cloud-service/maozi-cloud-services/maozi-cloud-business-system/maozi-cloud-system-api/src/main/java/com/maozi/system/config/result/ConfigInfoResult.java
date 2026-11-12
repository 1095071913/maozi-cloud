package com.maozi.system.config.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maozi.base.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局配置详情展示对象
 * <p>
 * 用于全局配置详情接口的返回数据封装，
 * 包含配置名称、别名、类型、配置值、排序值与状态等属性（配置 ID 不对外输出）。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigInfoResult implements Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 配置 ID（序列化时忽略，不对外输出） */
	@JsonIgnore
	@Schema(hidden = true)
	private Long id;

	/** 配置名称 */
	@Schema(description = "名称")
	private String name;

	/** 配置别名 */
	@Schema(description = "别名")
	private String alias;

	/** 配置类型 */
	@Schema(description = "类型")
	private String type;

	/** 配置值 */
	@Schema(description = "配置值")
	private String value;

	/** 排序序号，值越小越靠前 */
	@Schema(description = "排序")
	private Integer sort;

	/** 状态（启用/禁用） */
	@Schema(description = "状态")
	@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
	private Status status;

}
