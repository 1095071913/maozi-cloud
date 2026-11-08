package com.maozi.base.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用请求入参
 * <p>
 * 统一封装请求参数的数据载体，支持泛型以适配不同的业务参数类型。
 * 搭配 {@link Valid} 注解实现嵌套参数校验。
 * </p>
 *
 * @param <D> 业务数据类型
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestParam<D> implements Serializable {

	/** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 业务数据 */
	@Valid
	@Schema(description = "数据")
	private D data;

}
