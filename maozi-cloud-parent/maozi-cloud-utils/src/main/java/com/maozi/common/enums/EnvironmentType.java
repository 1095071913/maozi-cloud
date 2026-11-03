package com.maozi.common.enums;

import lombok.Getter;

/**
 * 环境类型枚举
 * <p>
 * desc 与配置的环境标识（local/test/prod）保持一致，用于运行环境判断。
 * </p>
 *
 * @author maozi
 */
public enum EnvironmentType implements BaseEnum {

	/** 本地开发环境 */
	LOCAL(0,"local"),

	/** 测试环境 */
	TEST(1,"test"),

	/** 生产环境 */
	PROD(2,"prod"),

	;

	EnvironmentType(Integer value,String desc) {

		this.value = value;

		this.desc = desc;

	}

	/** 枚举的整型值，用于数据库存储和环境标识 */
	@Getter
	private final Integer value;

	/** 环境描述字符串，与配置文件中的环境标识保持一致 */
	@Getter
	private final String desc;

	@Override
	public String toString() {
		return value + "." + desc;
	}

}
