package com.maozi.oauth.token.api.rest.fallback;

import com.maozi.common.JacksonUtil;
import com.maozi.common.ResultUtil;
import com.maozi.common.result.AbstractBaseResult;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.oauth.token.api.rest.RestOauthTokenService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * OAuth 令牌服务降级工厂
 * <p>
 * 当 OAuth 认证服务不可用时，提供降级响应，将服务端的错误信息原样返回。
 * 令牌内省主链路已切换为 Dubbo RPC（{@link com.maozi.oauth.token.api.rpc.RpcOauthTokenService}），
 * 本工厂仅配合保留的 Feign 兼容入口使用，Feign 链路移除时可一并删除。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class RestOauthTokenServiceFallBackFactory implements FallbackFactory<RestOauthTokenService>{

    /**
     * 创建降级实例
     *
     * @param e 异常信息
     * @return 降级的 OAuth 令牌服务实例
     */
    @Override
    public RestOauthTokenService create(Throwable e) {

        // 尝试将异常消息按服务端返回的 ErrorResult JSON 解析
        // 解析结果为空且原异常为业务异常时，回退使用业务异常携带的结果
        ErrorResult result = JacksonUtil.jsonToObject(e.getLocalizedMessage(), ErrorResult.class);
        if (Objects.isNull(result) && e instanceof BusinessResultException businessResultException) {
            result = businessResultException.getErrorResult();
        }

        // 仍无法取得错误结果（如连接超时等非业务异常）时返回统一系统错误
        // 避免强转 ClassCastException
        if (Objects.isNull(result)) {
            result = ResultUtil.error(SystemErrorCode.SYSTEM_ERROR);
        }

        // 匿名内部类引用需为最终变量
        final ErrorResult finalResult = result;
        return new RestOauthTokenService() {

            /** 降级处理：直接返回解析出的服务端错误结果 */
            @Override
            public AbstractBaseResult<Map<String, Object>> introspectResult(String token) {
                return finalResult;
            }

        };
    }

}
