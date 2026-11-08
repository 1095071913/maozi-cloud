package com.maozi.system.config.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.maozi.base.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 全局配置列表展示对象
 * <p>
 * 用于全局配置分页列表接口的返回数据封装，
 * 默认按排序值升序、创建时间倒序排列。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
public class ConfigListResult implements Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 配置ID */
	@Schema(description = "标识")
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

	/** 创建时间 */
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/** 状态（启用/禁用） */
	@Schema(description = "状态")
	@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
	private Status status;

}
