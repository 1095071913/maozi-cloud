package com.maozi.common.result.error.code;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 错误码基类
 * <p>
 * 所有错误码分类的公共父类，子类通过静态常量定义具体的错误码实例。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
public class AbstractBaseErrorCode implements Serializable {

	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

}
