/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.maozi.common.result.success;

import com.maozi.common.result.AbstractBaseResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 接口成功结果集
 * <p>
 * 表示 API 调用成功的响应结果，固定业务码为 200。
 * </p>
 *
 * @param <D> 响应数据类型
 * @author maozi
 * @date 2026/11/08
 */
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=true)
@Schema(description = "接口成功结果集")
public class SuccessResult<D> extends AbstractBaseResult<D> implements Serializable {

	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** 结果集成功默认码 */
	public static final Integer RESULT_SUCCESS_DEFAULT_CODE = 200;

	/**
	 * 获取业务内码
	 *
	 * @return 固定返回 200
	 */
	@Override
	public Integer getCode() {
		return RESULT_SUCCESS_DEFAULT_CODE;
	}

	/** 响应数据 */
	@Getter
	private D data;

	/**
	 * 构造成功结果
	 *
	 * @param data 响应数据
	 */
	public SuccessResult(D data) {
		this.data = data;
	}

}
