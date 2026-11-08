package com.maozi.system.config.result;

import com.maozi.base.result.DropDownResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 配置下拉选择结果
 * <p>
 * 用于配置下拉列表接口的返回结果对象。
 * 继承自通用下拉结果基类（包含配置 ID 与名称），额外包含配置别名、配置值与排序信息，
 * 以支持前端在下拉选择时直接展示或使用配置内容。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigDropDownResult extends DropDownResult implements Serializable {
	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 配置别名 */
	@Schema(description = "别名")
	private String alias;

	/** 配置值 */
	@Schema(description = "配置值")
	private String value;

	/** 排序序号，值越小越靠前 */
	@Schema(description = "排序")
	private Integer sort;

}
