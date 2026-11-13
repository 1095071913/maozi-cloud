/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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
 */

package org.springframework.security.access;

/**
 * Spring Security 权限不足异常占位类
 * <p>
 * 本类与 {@code spring-security-core} 中的 {@link AccessDeniedException} 同包同名，
 * 用于在 {@code maozi-cloud-utils} 等不直接依赖 Spring Security 的模块中引用该异常类型，
 * 通过类路径「同包覆盖」机制隔离对 Spring Security 的强依赖。运行时由实际 Security jar 提供真实实现。
 * </p>
 * <p>
 * 原始语义：当 {@link org.springframework.security.core.Authentication Authentication}
 * 对象不具有所需权限时抛出。
 * </p>
 *
 * @author Ben Alex
 */
public class AccessDeniedException extends RuntimeException {

	/**
	 * 使用指定的详细消息构造 <code>AccessDeniedException</code>
	 * @param msg 详细消息
	 */
	public AccessDeniedException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和根原因构造 <code>AccessDeniedException</code>
	 * @param msg 详细消息
	 * @param cause 根原因
	 */
	public AccessDeniedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
