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

package com.maozi.common.result.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.WebUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.code.ErrorCode;
import com.maozi.common.result.error.code.SystemErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 接口错误结果集
 * <p>
 * 表示 API 调用失败的响应结果，包含错误码、错误信息、HTTP 状态码等。
 * 支持自动设置 HTTP 响应状态码，并提供系统错误/业务错误/权限错误的判断方法。
 * </p>
 *
 * @param <D> 错误附加数据类型
  * @author maozi
 * @date 2026/11/08
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "接口错误结果集")
public class ErrorResult<D> extends AbstractBaseResult<D> implements Serializable {

	/** 序列化标识 */
	@Serial
	private static final long serialVersionUID = 1L;

	/** HTTP 状态码（仅服务端使用，不序列化到响应体） */
	@JsonIgnore
	protected Integer httpCode;

	/** 错误内码 */
	@Schema(description = "错误内码")
	private Integer code;

	/** 错误信息 */
	@Schema(description = "错误信息")
	private String message;

	/** 异常错误信息（仅服务端记录，不暴露给前端） */
	@JsonIgnore
	private String exceptionMessage;

	/** 错误附加数据 */
	@Schema(description = "错误数据")
	private D data;

	/**
	 * 仅指定错误信息的构造方法
	 *
	 * @param message 错误信息
	 */
	public ErrorResult(String message) {
		this.message = message;
	}

	/**
	 * 指定编码和错误信息的构造方法
	 *
	 * @param code 错误编码
	 * @param message 错误信息
	 */
	public ErrorResult(Integer code, String message) {
		this(message);
		this.code = code;
	}

	/**
	 * 指定编码、错误信息和附加数据的构造方法
	 *
	 * @param code 错误编码
	 * @param message 错误信息
	 * @param data 错误附加数据
	 */
	public ErrorResult(Integer code, String message, D data) {
		this(code,message);
		this.data = data;
	}

	/**
	 * 根据错误码构造
	 *
	 * @param errorCode 错误码对象
	 */
	public ErrorResult(ErrorCode errorCode) {
		this.code = errorCode.getCode();
		this.message = errorCode.getMessage();
		this.exceptionMessage = errorCode.getExceptionMessage();
	}

	/**
	 * 根据错误码和附加数据构造
	 *
	 * @param errorCode 错误码对象
	 * @param errorData 错误附加数据
	 */
	public ErrorResult(ErrorCode errorCode,D errorData) {
		this(errorCode);
		this.data = errorData;
	}

	/**
	 * 获取错误内码，为空时返回默认业务错误码
	 *
	 * @return 错误内码
	 */
	@Override
	public Integer getCode() {
		return ObjectUtil.isNotNullEmpty(code) ? code : SystemErrorCode.BUSINESS_ERROR_DEFAULT_CODE;
	}

	/**
	 * 获取 HTTP 状态码，为空时返回默认业务错误码
	 *
	 * @return HTTP 状态码
	 */
	public Integer getHttpCode() {
		return ObjectUtil.isNotNullEmpty(httpCode) ? httpCode : SystemErrorCode.BUSINESS_ERROR_DEFAULT_CODE;
	}

	/**
	 * 获取异常信息，为空时返回错误信息
	 *
	 * @return 异常信息
	 */
	public String getExceptionMessage() {
		return ObjectUtil.isNotNullEmpty(exceptionMessage) ? exceptionMessage : message;
	}

	/**
	 * 判断错误码是否在指定列表中
	 *
	 * @param codes 待检查的错误码列表
	 * @return 包含时返回 true
	 */
	@JsonIgnore
	public Boolean isCode(Integer... codes) {
		return CollectionUtil.newArrayList(codes).contains(code);
	}

	/**
	 * 判断是否为系统错误
	 *
	 * @return 系统错误返回 true
	 */
	@JsonIgnore
	public Boolean isSystemError() {
		return !isBusinessError();
	}

	/**
	 * 判断是否为业务错误（错误码大于系统最大错误码）
	 *
	 * @return 业务错误返回 true
	 */
	@JsonIgnore
	public Boolean isBusinessError() {
		return SystemErrorCode.MAX_SYSTEM_ERROR_CODE < code;
	}

	/**
	 * 判断是否为权限错误
	 *
	 * @return 权限不足或用户认证失败返回 true
	 */
	@JsonIgnore
	public Boolean isPermissionsError() {
		return Objects.equals(SystemErrorCode.PERMISSION_ERROR.getCode(),code) || Objects.equals(SystemErrorCode.USER_AUTH_ERROR.getCode(),code);
	}

	/**
	 * 设置错误信息
	 *
	 * @param message 错误信息
	 * @return 当前实例（链式调用）
	 */
	public ErrorResult<D> setMessage(String message) {
		this.message = message;
		return this;
	}

	/**
	 * 同时设置错误信息和异常错误信息
	 *
	 * @param message 错误信息
	 * @param exceptionMessage 异常错误信息
	 * @return 当前实例（链式调用）
	 */
	public ErrorResult<D> setMessage(String message,String exceptionMessage) {
		this.message = message;
		this.exceptionMessage = exceptionMessage;
		return this;
	}

	/**
	 * 设置异常错误信息
	 *
	 * @param exceptionMessage 异常错误信息
	 * @return 当前实例（链式调用）
	 */
	public ErrorResult<D> setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
		return this;
	}

	/**
	 * 设置 HTTP 状态码
	 *
	 * @param httpCode HTTP 状态码
	 * @return 当前实例（链式调用）
	 */
	public ErrorResult<D> setHttpCode(Integer httpCode) {
		this.httpCode = httpCode;
		return this;
	}

	/**
	 * 自动设置 HTTP 响应状态码
	 *
	 * @return 当前实例（链式调用）
	 */
	public ErrorResult<D> autoIdentifyHttpCode() {

		HttpServletResponse response = WebUtil.getResponse();
		if(ObjectUtil.isNotNullEmpty(response)) {
			response.setStatus(getHttpCode());
		}

		return this;

	}

	/**
	 * 设置 HTTP 状态码并自动应用到响应
	 *
	 * @param httpCode HTTP 状态码
	 * @return 当前实例（链式调用）
	 */
	public ErrorResult<D> autoIdentifyHttpCode(Integer httpCode) {

		this.setHttpCode(httpCode);

		autoIdentifyHttpCode();

		return this;

	}

}
