
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

package com.maozi.stream.config;

import com.maozi.common.EnvironmentUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.AbstractRequestContextHolder;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.context.SqlLogContext;
import com.maozi.common.context.TestFlowContext;
import com.maozi.common.context.VersionContext;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.enums.LogCommonType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Stream 消息入口日志切面
 * <p>
 * 拦截 Spring Cloud Stream 消费者方法，在消息处理前后记录日志信息，
 * 包括消息 ID、Topic、处理函数名、消息参数、SQL 日志和响应时间。
 * 从消息头中恢复版本号、测试流量标记与当前登录用户信息到链路上下文。
 * 异常时记录错误信息和堆栈行号。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Aspect
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
public class StreamEntranceLogAop {

    /** 日志中 Topic 的标签 */
    private static final String LOG_TAG_TOPIC = "Topic";

    /** 日志中消息 ID 的标签 */
    private static final String LOG_TAG_MESSAGE_ID = "MessageId";

    /** RocketMQ Topic 的消息头键名 */
    private static final String HEADER_ROCKET_MQ_TOPIC = "ROCKET_MQ_TOPIC";

    /** RocketMQ 消息 ID 的消息头键名 */
    private static final String HEADER_ROCKET_MQ_MESSAGE_ID = "ROCKET_MQ_MESSAGE_ID";

    /** Stream 消费者方法切点表达式 */
    private final String POINT = "execution(java.util.function.Consumer com.maozi.*.*.stream..*(..))";

    /**
     * 环绕通知，包装消息消费者并记录处理日志
     *
     * @param proceedingJoinPoint AOP 连接点
     * @return 包装后的消息消费者
     * @throws Throwable 切面或业务异常
     */
    @Around(POINT)
    public Consumer<Message<Object>> doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        // 执行目标方法取得原始消费者，随后返回包装了日志逻辑的新消费者
        Consumer<Object> resultData = (Consumer<Object>) proceedingJoinPoint.proceed();

        return (message) -> {

            // 标记消息处理是否发生异常，用于最终日志级别判断
            boolean error = false;

            // 消息处理开始时间戳
            long beginTime = System.currentTimeMillis();

            // 消息体内容
            Object messageData = message.getPayload();

            // 消息头
            MessageHeaders headers = message.getHeaders();

            // 日志信息集合
            Map<String, String> logs = new LinkedHashMap<>(16);

            // 记录消息类型、消息 ID、Topic 与消费方法
            logs.put(LogTag.TYPE, LogCommonType.MQ.getDesc());
            logs.put(LOG_TAG_MESSAGE_ID, Objects.requireNonNull(headers.get(HEADER_ROCKET_MQ_MESSAGE_ID)).toString());
            logs.put(LOG_TAG_TOPIC, Objects.requireNonNull(headers.get(HEADER_ROCKET_MQ_TOPIC)).toString());
            logs.put(LogTag.FUNCTION, proceedingJoinPoint.getSignature().getDeclaringTypeName()
                + ":" + proceedingJoinPoint.getSignature().getName());

            // 非生产环境直接记录消息参数；生产环境为避免参数外泄，仅在异常时补充记录
            boolean isNotProd = EnvironmentUtil.notEnvironment(EnvironmentType.PROD);
            if (isNotProd) {
                logs.put(LogTag.PARAM, messageData.toString());
            }

            // 从消息头中提取版本号设置到链路上下文
            Object versionObj = headers.get(VersionContext.KEY);
            if(ObjectUtil.isNotNullEmpty(versionObj)){
                VersionContext.CONTEXT.set(versionObj.toString());
            }

            // 从消息头中提取测试流量标记设置到链路上下文
            Object testFlowObj = headers.get(TestFlowContext.KEY);
            if(ObjectUtil.isNotNullEmpty(testFlowObj)){
                TestFlowContext.CONTEXT.set(testFlowObj.toString());
            }

            // 从消息头中提取当前登录用户信息设置到链路上下文
            Object currentUserInfoObj = headers.get(CurrentUserInfoContext.KEY);
            if(ObjectUtil.isNotNullEmpty(currentUserInfoObj)){
                CurrentUserInfoContext.CONTEXT.set(currentUserInfoObj.toString());
            }

            try {
                // 执行消息消费逻辑
                resultData.accept(message);
            } catch (Exception e) {

                // 标记消息处理发生异常
                error = true;

                // 生产环境异常时补充记录消息参数
                if (!isNotProd) {
                    logs.put(LogTag.PARAM, messageData.toString());
                }

                // 记录异常描述与发生的第一行代码位置
                logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());
                logs.put(LogTag.ERROR_LINE, e.getStackTrace()[0].toString());

                // 异常原样上抛，交由消息容器按消费失败处理
                throw e;

            } finally {

                // 记录消息处理过程中的 SQL 日志
                StringBuilder sqlLog = SqlLogContext.CONTEXT.get();
                if (ObjectUtil.isNotNullEmpty(sqlLog)) {
                    logs.put(LogTag.SQL, sqlLog.toString());
                }

                // 记录响应时间
                logs.put(LogTag.RT, String.valueOf(System.currentTimeMillis() - beginTime));

                // 按处理结果以 info/error 级别输出日志
                LogUtil.log(log, error, logs);

                // 清理线程上下文，防止线程池复用导致数据泄漏
                AbstractRequestContextHolder.clearContext();

            }

        };

    }

}
