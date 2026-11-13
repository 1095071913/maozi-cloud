package com.maozi.base.plugin.join;

import lombok.Getter;

/**
 * JOIN 关联类型枚举
 * <p>
 * 定义支持的 JOIN 查询类型，包括内连接、左连接、右连接。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Getter
public enum JoinBaseType {

	/** 内连接 */
	INNER_JOIN("innerJoin", "内连接"),

	/** 左连接 */
	LEFT_JOIN("leftJoin", "左连接"),

	/** 右连接 */
	RIGHT_JOIN("rightJoin", "右连接"),

	;

	/**
	 * 按关联类型标识和名称构造枚举实例
	 *
	 * @param type 关联类型标识，用于 SQL 构建时确定 JOIN 方式
	 * @param name 关联类型的中文名称描述
	 */
	JoinBaseType(String type,String name) {

		this.type = type;

		this.name = name;

	}

	/** 关联类型标识 */
	@Getter
	private final String type;

	/** 关联类型名称 */
	@Getter
	private final String name;

}
