
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
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.dto.CurrentUserInfo;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.enums.LogCommonType;
import com.maozi.common.monitor.Alarm;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;

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

    /** 限流熔断告警组件，由监控模块（如 Sentinel）提供实现并通过依赖注入获取 */
    @Autowired(required = false)
    private Alarm alarm;

    /**
     * 环绕通知：记录请求日志、执行业务逻辑并记录响应日志。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>记录请求开始时间和基本信息</li>
     *   <li>执行业务方法</li>
     *   <li>捕获异常并转换为统一错误结果</li>
     *   <li>在 finally 块中记录 SQL 日志、响应时间并按结果分类输出日志</li>
     * </ol>
     * </p>
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 业务方法执行结果
     * @throws AccessDeniedException 权限拒绝异常原样上抛，交由 Spring Security 处理
     */
    protected Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        /* 请求开始时间戳 */
        long startTime = System.currentTimeMillis();

        /* 方法参数字符串 */
        String param = Arrays.toString(proceedingJoinPoint.getArgs());

        /* 日志信息集合 */
        Map<String, String> logs = requestLog(proceedingJoinPoint);

        // 非生产环境记录请求参数
        if (EnvironmentUtil.notEnvironment(EnvironmentType.PROD)) {
            logs.put(LogTag.PARAM, param);
        }

        // 方法返回结果，初始为 null
        Object resultData = null;

        // 执行目标业务方法
        try {
            resultData = proceedingJoinPoint.proceed();

        // 权限拒绝异常交由 Spring Security 处理
        } catch (AccessDeniedException accessDeniedException) {
            throw accessDeniedException;

        // 业务异常：封装为业务错误结果（已包含错误码和错误信息），不记录 error 日志
        } catch (BusinessResultException businessResultException) {
            resultData = businessResultException.getErrorResult();

        // 未预期的系统异常：封装为统一的系统错误结果，并记录详细的错误日志
        } catch (Throwable e) {

            // 构建系统级错误响应
            resultData = ResultUtil.error(SystemErrorCode.SYSTEM_ERROR)
                    .setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);

            // 记录异常堆栈日志，便于排查问题
            LogUtil.error(log, e);

            // 收集系统错误相关的日志信息
            // 记录请求参数（系统异常时无论环境均记录）
            logs.put(LogTag.PARAM, param);
            Long userId = ApplicationLinkContext.getCurrentUserInfo(CurrentUserInfo::getUserId);
            if (ObjectUtil.isNotNullEmpty(userId)) {
                logs.put(LogTag.ERROR_USER, userId.toString());   // 记录当前操作用户
            }
            logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());                 // 记录异常描述

            // 记录异常发生的第一行代码位置
            StackTraceElement[] errorLines = e.getStackTrace();
            if (errorLines.length > 0) {
                logs.put(LogTag.ERROR_LINE, errorLines[0].toString());
            }

            return resultData;

        } finally {

            // 记录本次请求过程中执行的 SQL 日志（从 ThreadLocal 中获取）
            StringBuilder sqlLog = LogUtil.SQL_LOG.get();
            if (ObjectUtil.isNotNullEmpty(sqlLog)) {
                logs.put(LogTag.SQL, sqlLog.toString());
            }

            // 记录响应时间
            logs.put(LogTag.RT, (System.currentTimeMillis() - startTime) + " ms");

            // 判断结果是否为框架统一响应类型 && 请求失败（业务或系统错误）
            if (ObjectUtil.isNotNullEmpty(resultData) && resultData instanceof AbstractBaseResult<?> result
                    && !result.isSuccess()) {

                // 触发限流熔断告警组件
                // （任何请求失败都会调用，Sentinel 实现将本次失败计入阻塞 QPS 统计）
                if (ObjectUtil.isNotNullEmpty(alarm)) {
                    alarm.alarm();
                }

                // 获取错误详情，用于判断是业务错误还是系统错误
                ErrorResult<?> errorResult = result.getErrorResult();

                // 业务错误使用 warn 级别，系统错误使用 error 级别
                if (errorResult.autoIdentifyHttpCode().isBusinessError()) {
                    log.warn(LogUtil.convertLog(logs));
                } else {
                    LogUtil.error(log, logs);
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

        // 使用 LinkedHashMap 保证日志字段的输出顺序与插入顺序一致
        Map<String, String> logs = new LinkedHashMap<>(16);

        // 记录请求类型标识（WEB 或 RPC）
        logs.put(LogTag.TYPE, getType().getDesc());

        // 记录来源地址：HTTP 请求取客户端真实 IP，RPC 调用取当前服务（Dubbo 提供方）地址
        logs.put(LogTag.IP, getLocalHost());

        // 仅 HTTP 请求记录完整的请求 URL
        requestLog(logs);

        // 记录被调用的方法全路径，格式为 "类全限定名:方法名"
        logs.put(LogTag.FUNCTION, proceedingJoinPoint.getSignature().getDeclaringTypeName()
                + ":" + proceedingJoinPoint.getSignature().getName());

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
    protected abstract LogCommonType getType();

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
    protected void requestLog(Map<String, String> logs) {

    }

}
