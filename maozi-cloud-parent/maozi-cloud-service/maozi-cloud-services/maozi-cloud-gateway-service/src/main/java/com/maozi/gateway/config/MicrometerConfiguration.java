package com.maozi.gateway.config;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;

import java.util.Optional;

/**
 * Micrometer 指标配置
 * <p>
 * 修复 Spring Cloud Gateway 的 URI 标签在 {@code /metrics} 端点中被错误标记为
 * <b>UNKNOWN</b> 的问题（见 <a href="https://github.com/spring-cloud/spring-cloud-gateway/issues/891">...</a>）。
 * 通过自定义 {@link ServerRequestObservationConvention}，在 URI 标签为空或 UNKNOWN 时，
 * 从请求路径前两段提取路径模板，提升指标标签的可读性。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class MicrometerConfiguration {

    /** URI 标签键名 */
    private static final String KEY_URI = "uri";

    /** URI 标签未知占位值 */
    private static final String UNKNOWN = "UNKNOWN";

    /**
     * 创建自定义请求观测约定，修复 URI 标签为 UNKNOWN 的问题
     *
     * @return 请求观测约定实例
     */
    @Bean
    public ServerRequestObservationConvention uriTagContributorForObservationApi() {

        /*
         * 修复 Spring 框架问题导致 /metrics 端点中 URI 标签值被置为 UNKNOWN 的问题，
         * 参见 https://github.com/spring-cloud/spring-cloud-gateway/issues/891
         */
        return new DefaultServerRequestObservationConvention() {

            /**
             * 在 URI 标签为空或 UNKNOWN 时，从请求路径前两段提取路径模板（如 {@code /a/b/**}）作为标签值
             *
             * @param context 请求观测上下文
             * @return 低基数键值集合
             */
            @Override
            public KeyValues getLowCardinalityKeyValues(@Nonnull ServerRequestObservationContext context) {

                KeyValues lowCardinalityKeyValues = super.getLowCardinalityKeyValues(context);

                if (isUriTagNullOrUnknown(context, lowCardinalityKeyValues)) {

                    String path = context.getCarrier().getPath().value();

                    String[] pathSection = path.split("/");

                    path = pathSection.length > 2 ? "/"+pathSection[1]+"/"+pathSection[2]+"/**" : UNKNOWN;

                    return lowCardinalityKeyValues.and(KeyValue.of(KEY_URI, path));

                }

                return lowCardinalityKeyValues;

            }

            /**
             * 判断 URI 标签是否缺失或为 UNKNOWN
             *
             * @param context 请求观测上下文
             * @param lowCardinalityKeyValues 当前低基数键值集合
             * @return {@code true} 表示 URI 标签缺失或为 UNKNOWN
             */
            private static boolean isUriTagNullOrUnknown(ServerRequestObservationContext context, KeyValues lowCardinalityKeyValues) {

                Optional<KeyValue> uriKeyValue = lowCardinalityKeyValues.stream().filter(keyValue -> KEY_URI.equals(keyValue.getKey())).findFirst();

                return (uriKeyValue.isEmpty() || UNKNOWN.equals(uriKeyValue.get().getValue()));

            }

        };

    }

}