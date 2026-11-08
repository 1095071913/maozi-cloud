package com.maozi.system.permission.enums;

import com.maozi.common.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 权限类型枚举
 * <p>
 * 用于定义系统中权限的分类类型，包括目录、菜单和按钮三种类型。
 * 实现 BaseEnum 接口，提供枚举值和描述的标准访问方式。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Schema(description = "权限类型",type = "integer")
public enum PermissionType implements BaseEnum {

	/** 目录类型 - 表示一级导航目录 */
	DIRECTORY(0,"目录"),

	/** 菜单类型 - 表示具体的功能菜单页面 */
	MENU(1,"菜单"),

	/** 按钮类型 - 表示页面中的操作按钮权限 */
	BUTTON(2,"按钮"),

	;

	/**
	 * 构造方法
	 *
	 * @param value 权限类型的数值编码
	 * @param desc  权限类型的中文描述
	 */
	PermissionType(Integer value,String desc) {

		this.value = value;

		this.desc = desc;

	}

	/** 权限类型的数值编码 */
	@Getter
	private final Integer value;

	/** 权限类型的中文描述 */
	@Getter
	private final String desc;

	/**
	 * 输出枚举的字符串表示
	 *
	 * @return 格式为 "编码.描述" 的字符串
	 */
	@Override
	public String toString() {
		return value + "." + desc;
	}

}
