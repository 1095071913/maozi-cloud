
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

package com.maozi.dubbo.aop;

import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.enums.LogCommonType;
import com.maozi.log.config.AbstractRequestEntranceLogAop;
import org.apache.dubbo.rpc.RpcContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * RPC 接口请求入口日志切面
 * <p>
 * 拦截 RPC 接口和基础服务实现类的请求入口，复用
 * {@link AbstractRequestEntranceLogAop} 的统一日志逻辑。非 HTTP 请求时
 * 以 Dubbo 服务地址作为来源记录。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Aspect
@Component
public class DubboRequestEntranceLogAop extends AbstractRequestEntranceLogAop {

	/** RPC 接口切点表达式：用 .. 替代 *.*，避免 AspectJ 解析歧义 */
	private static final String RPC_POINT = "* " + ApplicationEnvironmentContext.PACKAGE_PREFIX + ".*.*.api.impl.rpc..*(..)";

	/** 基础服务实现类切点表达式 */
	private static final String BASE_RPC_POINT = ApplicationEnvironmentContext.PACKAGE_PREFIX + ".common.result.AbstractBaseResult " + ApplicationEnvironmentContext.PACKAGE_PREFIX + ".service.api.impl.BaseServiceImpl.*(..)";

	/** 组合切点表达式 */
	private static final String POINT = "execution(" + RPC_POINT + ") || execution(" + BASE_RPC_POINT + ")";

	/**
	 * 环绕通知，绑定 RPC 切点并委托给基类的统一日志逻辑。
	 *
	 * @param proceedingJoinPoint AOP 连接点
	 * @return 业务方法执行结果
	 * @throws AccessDeniedException 权限拒绝时经基类原样上抛，交由 Spring Security 处理
	 */
	@Around(POINT)
	@Override
	public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		return super.doAround(proceedingJoinPoint);
	}

	/**
	 * 返回 RPC 类型的入口日志标识
	 *
	 * @return RPC 日志类型
	 */
	@Override
	protected LogCommonType getType() {
		return LogCommonType.RPC;
	}

	/**
	 * 返回当前 Dubbo RPC 服务地址，作为非 HTTP 请求的来源记录。
	 *
	 * @return 当前 Dubbo 服务地址
	 */
	@Override
	protected String getLocalHost() {
		return RpcContext.getServiceContext().getLocalHost();
	}

}
