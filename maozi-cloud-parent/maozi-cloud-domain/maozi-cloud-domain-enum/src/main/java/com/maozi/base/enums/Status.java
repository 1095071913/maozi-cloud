package com.maozi.base.enums;

import com.maozi.common.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 状态枚举
 * <p>
 * 标识数据记录的启用/禁用状态，常用于账号状态、功能开关等场景。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Schema(description = "状态",type = "integer")
public enum Status implements BaseEnum {

	/** 禁用 */
	DISABLE(0,"禁用"),

	/** 启用 */
	ENABLE(1,"启用"),

	;

	/**
	 * 按整型值和描述构造枚举实例
	 *
	 * @param value 状态值，对应数据库状态字段
	 * @param desc 状态的中文描述
	 */
	Status(Integer value,String desc) {

		this.value = value;

		this.desc = desc;

	}

	/** 枚举的整型值，对应数据库状态字段 */
	@Getter
	private final Integer value;

	/** 枚举的中文描述 */
	@Getter
	private final String desc;

	/**
	 * 返回"值.描述"格式的字符串
	 *
	 * @return 形如 "1.启用" 的字符串
	 */
	@Override
	public String toString() {
		return value + "." + desc;
	}

}
