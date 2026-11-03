package com.maozi.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 基础枚举接口
 * <p>
 * 项目所有业务枚举的统一抽象，继承 MyBatis-Plus {@link IEnum}，
 * 实现类可与数据库整型字段自动相互转换，无需编写 TypeHandler。
 * </p>
 *
 * @author maozi
 * @see IEnum MyBatis-Plus 枚举接口
 */
public interface BaseEnum extends IEnum<Integer> {

	/**
	 * 获取枚举的中文描述
	 *
	 * @return 枚举的中文描述
	 */
	String getDesc();

}
