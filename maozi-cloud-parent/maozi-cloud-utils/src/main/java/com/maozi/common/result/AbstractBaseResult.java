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

package com.maozi.common.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.common.result.success.SuccessResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * 接口响应结果集基类
 * <p>
 * 定义统一响应格式的抽象基类，包含业务内码、数据获取以及成功/错误判断等通用方法。
 * 子类 {@link SuccessResult} 和 {@link ErrorResult} 分别表示成功和错误的响应。
 * </p>
 *
 * @param <D> 响应数据类型
 * @author maozi
 */
@Data
@SuperBuilder
@NoArgsConstructor
@Schema(description = "接口响应结果集")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractBaseResult<D> implements Serializable {

	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 获取响应数据
	 *
	 * @return 响应数据
	 */
	@Schema(description = "数据")
	public abstract D getData();

	/**
	 * 获取业务内码
	 *
	 * @return 业务内码
	 */
	@Schema(description = "业务内码")
	public abstract Integer getCode();

	/**
	 * 判断响应是否成功（业务码为 200）
	 *
	 * @return 成功返回 true
	 */
	@JsonIgnore
	public Boolean isSuccess() {
		return getCode() == 200;
	}

	/**
	 * 判断响应是否失败
	 *
	 * @return 失败返回 true
	 */
	@JsonIgnore
	public Boolean isError() {
		return !isSuccess();
	}

	/**
	 * 获取成功结果实例
	 *
	 * @return 如果当前为成功结果则返回实例，否则返回 null
	 */
	@JsonIgnore
	public SuccessResult<D> getSuccessResult() {
		return this instanceof SuccessResult<D> successResult ? successResult : null;
	}

	/**
	 * 获取错误结果实例
	 *
	 * @return 如果当前为错误结果则返回实例，否则返回 null
	 */
	@JsonIgnore
	public ErrorResult<D> getErrorResult() {
		return this instanceof ErrorResult<D> errorResult ? errorResult : null;
	}

	/**
	 * 获取响应数据，失败时抛出异常
	 *
	 * @return 响应数据
	 * @throws BusinessResultException 响应失败时抛出
	 */
	@JsonIgnore
	public D getResultDataThrowError() {
		if(isError()) throwError();
		return getData();
	}

	/**
	 * 获取响应数据，失败或数据为空时抛出异常
	 *
	 * @param resourceName 资源名称，用于异常提示
	 * @return 响应数据
	 * @throws BusinessResultException 响应失败或数据为空时抛出
	 */
	@JsonIgnore
	public D getResultNotNullDataThrowError(String resourceName) {

		if(isError()) throwError();

		if(ObjectUtil.isNullEmpty(getData())) {
			throw new BusinessResultException(SystemErrorCode.DATA_NOT_EXIST_ERROR).setResource(resourceName);
		}

		return getData();

	}

	/**
	 * 抛出错误结果对应的业务异常
	 *
	 * @throws BusinessResultException 始终抛出，将当前 ErrorResult 包装为业务异常
	 */
	private void throwError() {
		throw new BusinessResultException(getErrorResult());
	}

}
