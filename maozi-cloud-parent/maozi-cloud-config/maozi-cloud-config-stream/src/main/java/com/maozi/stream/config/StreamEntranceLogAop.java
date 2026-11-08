
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
import com.maozi.common.context.ApplicationLinkContext;
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
 * 从消息头中提取版本号设置到链路上下文，支持灰度环境标识。
 * 异常时记录错误信息和堆栈行号。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Slf4j
@Aspect
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1 )
public class StreamEntranceLogAop {

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

            boolean error = false;

            long beginTime = System.currentTimeMillis();

            Object messageData = message.getPayload();

            MessageHeaders headers = message.getHeaders();

            Map<String, String> logs = new LinkedHashMap<>();

            logs.put(LogTag.TYPE, LogCommonType.MQ.getDesc()) ;
            logs.put("MessageId", Objects.requireNonNull(headers.get("ROCKET_MQ_MESSAGE_ID")).toString());
            logs.put("Topic", Objects.requireNonNull(headers.get("ROCKET_MQ_TOPIC")).toString());
            logs.put(LogTag.FUNCTION, proceedingJoinPoint.getSignature().getDeclaringTypeName() + ":" + proceedingJoinPoint.getSignature().getName());

            // 非生产环境直接记录消息参数；生产环境为避免参数外泄，仅在异常时补充记录
            Boolean isNotProd = EnvironmentUtil.notEnvironment(EnvironmentType.PROD);
            if(isNotProd){
                logs.put(LogTag.PARAM, messageData.toString());
            }

            // 从消息头中提取版本号设置到链路上下文
            ApplicationLinkContext.setVersion(ApplicationLinkContext.getVersionDefault(headers.get(ApplicationLinkContext.VERSION_KEY)));

            try{resultData.accept(message);}catch (Exception e){

                error = true;

                StackTraceElement stackTraceElement = e.getStackTrace()[0];

                if(!isNotProd){
                    logs.put(LogTag.PARAM, messageData.toString());
                }
                logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());
                logs.put(LogTag.ERROR_LINE, stackTraceElement.toString());

                throw e;

            }finally {

                StringBuilder sqlLog = LogUtil.sqlLog.get();
                if(ObjectUtil.isNotNullEmpty(sqlLog)) {
                    logs.put(LogTag.SQL, sqlLog.toString());
                }

                logs.put(LogTag.RT, String.valueOf(System.currentTimeMillis() - beginTime));

                LogUtil.log(log,error,logs);

                ApplicationLinkContext.clearContext();

            }

        };

    }

}
