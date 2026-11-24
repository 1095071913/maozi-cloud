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

package com.maozi.log.config;

import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.constant.SymbolConstant;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.context.PassThroughExceptionContainer;
import com.maozi.common.context.SqlLogContext;
import com.maozi.common.dto.CurrentUserInfoDTO;
import com.maozi.common.enums.EnvironmentTypeEnum;
import com.maozi.common.enums.LogCommonTypeEnum;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请求入口日志切面抽象基类
 * <p>
 * 拦截接口入口和基础服务实现类的请求，记录请求日志、响应日志和异常日志。
 * 在非生产环境下记录请求参数；异常发生时根据错误类型自动分类记录
 * （业务错误 warn 级别、系统错误 error 级别）。
 * </p>
 * <p>
 * 需原样上抛的异常（如 Spring Security 的权限拒绝异常）由各组件模块启动时注册到
 * {@link PassThroughExceptionContainer}，本类捕获异常后判断其类型是否在容器中，
 * 在则原样上抛交由对应框架处理，不封装为错误结果；本模块不依赖具体的异常来源框架。
 * </p>
 * <p>
 * 子类职责：
 * <ul>
 *   <li>提供具体的切点表达式
 *   （通过 {@code @Around} 绑定到 {@link #doAround(ProceedingJoinPoint)}）</li>
 *   <li>实现 {@link #getType()} 返回当前切面对应的日志类型（如 WEB、RPC）</li>
 *   <li>实现 {@link #getLocalHost()} 返回请求来源地址
 *   （REST 场景返回客户端真实 IP，RPC 场景返回当前 Dubbo 服务地址）</li>
 * </ul>
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
public abstract class AbstractRequestEntranceLogAop {

    /** 入口日志 Map 的初始容量 */
    private static final int LOG_MAP_INITIAL_CAPACITY = 16;

    /**
     * 环绕通知：记录请求日志、执行业务逻辑并记录响应日志。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>记录请求开始时间和基本信息</li>
     *   <li>执行业务方法</li>
     *   <li>捕获异常并转换为统一错误结果（容器中注册的透传异常原样上抛）</li>
     *   <li>在 finally 块中记录 SQL 日志、响应时间并按结果分类输出日志</li>
     * </ol>
     * </p>
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 业务方法执行结果
     * @throws Throwable 目标方法抛出的异常原样上抛，其中透传异常交由对应框架处理
     */
    protected Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        // 日志信息集合
        Map<String, String> logs = requestLog(proceedingJoinPoint);

        // 方法返回结果，初始为 null
        Object resultData = null;
        Throwable ex = null;

        // 执行目标业务方法
        try {
            resultData = proceedingJoinPoint.proceed();

            if (EnvironmentUtil.notEnvironment(EnvironmentTypeEnum.PROD)) {
                logs.put(LogTag.PARAM, Arrays.toString(proceedingJoinPoint.getArgs()));
            }

        // 业务异常：封装为业务错误结果（已包含错误码和错误信息），不记录 error 日志
        } catch (BusinessResultException businessResultException) {
            resultData = businessResultException.getErrorResult();

        // 未预期的系统异常：封装为统一的系统错误结果，并记录详细的错误日志
        } catch (Exception e) {

            // 透传异常（如 Spring Security 权限拒绝异常）原样上抛，交由对应框架处理
            if (PassThroughExceptionContainer.contains(e)) {
                throw e;
            }

            // 记录异常堆栈日志，便于排查问题
            ex = e;

            // 构建系统级错误响应
            resultData = ResultUtil.error(SystemErrorCode.SYSTEM_ERROR)
                    .setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);

            logs.put(LogTag.PARAM, Arrays.toString(proceedingJoinPoint.getArgs()));

            // 获取当前登录用户 ID
            Long userId = CurrentUserInfoContext.CONTEXT.get(CurrentUserInfoDTO::getUserId);
            if (ObjectUtil.isNotNull(userId)) {
                // 记录当前操作用户
                logs.put(LogTag.ERROR_USER, userId.toString());
            }

            return resultData;

        } finally {

            // 记录本次请求过程中执行的 SQL 日志（从 ThreadLocal 中获取）
            StringBuilder sqlLog = SqlLogContext.CONTEXT.get();
            if (ObjectUtil.isNotNull(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            // 判断结果是否为框架统一响应类型且请求失败（业务或系统错误）
            if (ObjectUtil.isNotNull(resultData) && resultData instanceof AbstractBaseResult<?> result
                    && !result.isSuccess()) {

                // 获取错误详情，用于判断是业务错误还是系统错误
                ErrorResult<?> errorResult = result.getErrorResult();
                // 业务错误使用 warn 级别，系统错误使用 error 级别
                if (errorResult.autoIdentifyHttpCode().isBusinessError()) {
                    log.warn(LogUtil.convertLog(logs));
                } else {
                    LogUtil.error(log, logs, ex);
                }

            } else {
                // 请求成功，记录 info 级别日志
                LogUtil.info(log, logs);
            }

        }

        return resultData;

    }

    /**
     * 构建 {@link LinkedHashMap} 结构的日志信息，保证字段输出顺序与插入顺序一致。
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 日志字段集合
     */
    private Map<String, String> requestLog(ProceedingJoinPoint proceedingJoinPoint) {

        Map<String, String> logs = new LinkedHashMap<>(LOG_MAP_INITIAL_CAPACITY);

        // 记录请求类型标识（WEB 或 RPC）
        logs.put(LogTag.TYPE, getType().getDesc());

        // 记录来源地址：HTTP 请求取客户端真实 IP，RPC 调用取当前服务（Dubbo 提供方）地址
        logs.put(LogTag.IP, getLocalHost());

        // 仅 HTTP 请求记录完整的请求 URL
        requestLog(logs);

        // 缓存方法签名信息，避免同一方法内重复调用
        Signature signature = proceedingJoinPoint.getSignature();

        // 记录被调用的方法全路径，格式为 "类全限定名:方法名"
        logs.put(LogTag.FUNCTION, signature.getDeclaringTypeName()
                + SymbolConstant.COLON + signature.getName());

        return logs;

    }

    /**
     * 返回请求来源地址，用于日志记录。
     * <p>
     * REST 子类返回 HTTP 请求的来源地址，RPC 子类返回对应的服务地址。
     * </p>
     *
     * @return 来源地址
     */
    protected abstract String getLocalHost();

    /**
     * 返回当前切面对应的请求入口日志类型。
     *
     * @return 日志类型标识（如 WEB、RPC）
     */
    protected abstract LogCommonTypeEnum getType();

    /**
     * 钩子方法：子类可重写以追加自定义的日志字段
     * <p>
     * 在标准入口日志拼装完成后被调用，默认实现为空。
     * 子类通过向 {@code logs} 中 put 自定义键值对来扩展日志内容
     * （如 Rest 子类记录的完整请求 URL）。
     * </p>
     *
     * @param logs 当前已收集的日志字段 Map（可变，允许子类追加条目）
     */
    protected void requestLog(Map<String, String> logs) {}

}
