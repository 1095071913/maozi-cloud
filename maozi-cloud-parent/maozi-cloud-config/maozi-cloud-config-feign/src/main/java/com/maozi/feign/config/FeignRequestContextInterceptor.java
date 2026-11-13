package com.maozi.feign.config;

import com.maozi.common.ObjectUtil;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.context.TestFlowContext;
import com.maozi.common.context.TraceIdContext;
import com.maozi.common.context.VersionContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * Feign 链路上下文请求拦截器
 * <p>
 * 在 Feign 远程调用时，自动将当前请求的链路上下文（当前登录用户信息、版本号、
 * 测试流量标记、链路追踪 ID）透传到下游服务，支撑下游的用户身份识别、
 * 灰度路由、测试流量隔离与日志关联。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class FeignRequestContextInterceptor implements RequestInterceptor {

    /**
     * 在请求模板中添加链路上下文头信息
     * <p>
     * 当前登录用户信息非空时才写入 {@code X-CurrentUserInfo} 头；
     * {@code X-Version}、{@code X-Test-Flow}、{@code X-TraceId} 头无条件设置，
     * 其中 {@code X-TraceId} 值可能为 null，会被 Feign 自动跳过，不随请求发送。
     * </p>
     *
     * @param requestTemplate Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate requestTemplate) {

        String currentUserInfoJson = CurrentUserInfoContext.CONTEXT.getJson();
        if(ObjectUtil.isNotNullEmpty(currentUserInfoJson)){
            requestTemplate.header(CurrentUserInfoContext.KEY, currentUserInfoJson);
        }

        requestTemplate.header(VersionContext.KEY, VersionContext.CONTEXT.get());
        requestTemplate.header(TestFlowContext.KEY, TestFlowContext.CONTEXT.get().toString());
        requestTemplate.header(TraceIdContext.KEY, TraceIdContext.CONTEXT.get());

    }

}
