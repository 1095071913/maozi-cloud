
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

package com.maozi.mvc.config.aop;

import com.maozi.common.WebUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.enums.LogCommonType;
import com.maozi.log.config.AbstractRequestEntranceLogAop;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * REST 接口请求入口日志切面
 * <p>
 * 拦截 REST 实现层（{@code api.impl.rest} 包）方法的请求入口，复用
 * {@link AbstractRequestEntranceLogAop} 的统一日志逻辑。权限拒绝异常
 * （{@link AccessDeniedException}）原样抛出，交由 Spring Security 处理，
 * 不封装为统一响应。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Aspect
@Component
public class RestRequestEntranceLogAop extends AbstractRequestEntranceLogAop {

	/** REST 实现层切点表达式（匹配 api.impl.rest 包下的方法） */
	private static final String REST_POINT = "* " + ApplicationEnvironmentContext.PACKAGE_PREFIX + ".*.*.api.impl.rest..*(..)";

	/** 基础服务实现类切点表达式 */
	private static final String BASE_RPC_POINT = ApplicationEnvironmentContext.PACKAGE_PREFIX + ".common.result.AbstractBaseResult " + ApplicationEnvironmentContext.PACKAGE_PREFIX + ".service.api.impl.result.BaseServiceResultImpl.*(..)";

	/** 组合切点表达式 */
	private static final String POINT = "execution(" + REST_POINT + ") || execution(" + BASE_RPC_POINT + ")";

	/**
	 * 环绕通知，绑定 REST 切点并委托给基类的统一日志逻辑
	 *
	 * @param proceedingJoinPoint AOP 连接点
	 * @return 业务方法执行结果；业务异常或系统异常时为基类封装的统一错误结果
	 * @throws Throwable 仅权限拒绝异常（{@link AccessDeniedException}）经基类原样上抛，交由 Spring Security 处理
	 */
	@Around(POINT)
	public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

		// 非 HTTP 流量时直接放行，不记录 HTTP 入口日志
		if(WebUtil.notHttpRequest()){
			return proceedingJoinPoint.proceed();
		}

		return super.doAround(proceedingJoinPoint);
	}

	/**
	 * 返回 WEB 类型的入口日志标识
	 *
	 * @return WEB 日志类型
	 */
	@Override
	protected LogCommonType getType() {
		return LogCommonType.WEB;
	}

	/**
	 * 返回当前 HTTP 请求的客户端真实 IP（穿透代理头解析）作为来源记录
	 *
	 * @return 客户端真实 IP
	 */
	@Override
	protected String getLocalHost() {
		return WebUtil.getRequestHost();
	}

	/**
	 * 追加 REST 请求专属日志信息
	 *
	 * @param logs 日志容器，写入当前请求的 URL（含协议、主机与路径，不含查询参数）
	 */
	@Override
	protected void requestLog(Map<String, String> logs) {
		logs.put(LogTag.URL, Objects.requireNonNull(WebUtil.getRequest()).getRequestURL().toString());
	}
}
