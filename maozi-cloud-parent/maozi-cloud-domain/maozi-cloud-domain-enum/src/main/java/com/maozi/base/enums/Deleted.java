package com.maozi.base.enums;

import com.maozi.common.enums.BaseEnum;
import lombok.Getter;

/**
 * 删除状态枚举
 * <p>
 * 标识数据记录的逻辑删除状态，配合 MyBatis-Plus 逻辑删除使用，
 * 查询时自动追加 {@code deleted = 0} 过滤条件。
 * </p>
 *
 * @author maozi
 */
public enum Deleted implements BaseEnum {

	/** 未删除 */
	NONE(0,"未删除"),

	/** 已删除 */
	DEL(1,"已删除"),

	;

	Deleted(Integer value,String desc) {

		this.value = value;

		this.desc = desc;

	}

	/** 枚举的整型值，对应数据库删除标记字段 */
	@Getter
	private final Integer value;

	/** 枚举的中文描述 */
	@Getter
	private final String desc;

	@Override
	public String toString() {
		return value + "." + desc;
	}


}
