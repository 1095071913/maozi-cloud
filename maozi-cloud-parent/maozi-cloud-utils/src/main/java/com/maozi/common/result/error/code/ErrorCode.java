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

package com.maozi.common.result.error.code;

import com.maozi.common.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 错误码对象
 * <p>
 * 定义错误码、错误信息和异常信息三个维度，用于统一描述业务和系统错误。
 * </p>
 *
 * @author maozi
 */
@Getter
@AllArgsConstructor
public class ErrorCode implements Serializable {

	/** 序列化标识 */
	@Serial
    private static final long serialVersionUID = 1L;

	/** 错误内码 */
	private final Integer code;

	/** 错误信息 */
	private final String message;

	/** 异常错误信息 */
	private final String exceptionMessage;

	/**
	 * 仅指定错误信息的构造方法（默认编码 400）
	 *
	 * @param message 错误信息
	 */
	public ErrorCode(String message) {
		this.code = 400;
		this.message = message;
		this.exceptionMessage = message;
	}

	/**
	 * 指定编码和错误信息的构造方法
	 *
	 * @param code 错误编码
	 * @param message 错误信息
	 */
	public ErrorCode(Integer code, String message) {
		this.code = code;
		this.message = message;
		this.exceptionMessage = message;
	}

	/**
	 * 获取异常信息，为空时返回错误信息
	 *
	 * @return 异常信息
	 */
	public String getExceptionMessage() {
		return ObjectUtil.isNotNullEmpty(this.exceptionMessage) ? this.exceptionMessage : this.message;
	}

}
