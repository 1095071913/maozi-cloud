package com.maozi.gateway.config;

import com.maozi.common.constant.SymbolConstant;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Micrometer 指标配置
 * <p>
 * 修复 Spring Cloud Gateway 的 URI 标签在 {@code /metrics} 端点中被错误标记为
 * <b>UNKNOWN</b> 的问题（见
 * <a href="https://github.com/spring-cloud/spring-cloud-gateway/issues/891">...</a>）。
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

    /** 路径模板通配后缀（如 /a/b/**） */
    private static final String PATH_WILDCARD_SUFFIX = "/**";

    /** 路径模板所需的最小段数（前导空段加前两段路径） */
    private static final int PATH_TEMPLATE_MIN_SECTION_COUNT = 3;

    /** URL 路径分隔符切分模式（预编译） */
    private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile(SymbolConstant.SLASH);

    /**
     * 创建自定义请求观测约定，修复 URI 标签为 UNKNOWN 的问题
     *
     * @return 请求观测约定实例
     */
    @Bean
    public ServerRequestObservationConvention uriTagContributorForObservationApi() {

        return new DefaultServerRequestObservationConvention() {

            /**
             * 在 URI 标签为空或 UNKNOWN 时，
             * 从请求路径前两段提取路径模板（如 {@code /a/b/**}）作为标签值
             *
             * @param context 请求观测上下文
             * @return 低基数键值集合
             */
            @Override
            public KeyValues getLowCardinalityKeyValues(@Nonnull ServerRequestObservationContext context) {

                KeyValues lowCardinalityKeyValues = super.getLowCardinalityKeyValues(context);

                if (isUriTagNullOrUnknown(lowCardinalityKeyValues)) {

                    String path = context.getCarrier().getPath().value();

                    String[] pathSection = PATH_SPLIT_PATTERN.split(path);

                    path = pathSection.length >= PATH_TEMPLATE_MIN_SECTION_COUNT
                            ? SymbolConstant.SLASH + pathSection[1]
                                    + SymbolConstant.SLASH + pathSection[2] + PATH_WILDCARD_SUFFIX
                            : UNKNOWN;

                    return lowCardinalityKeyValues.and(KeyValue.of(KEY_URI, path));

                }

                return lowCardinalityKeyValues;

            }

            /**
             * 判断 URI 标签是否缺失或为 UNKNOWN
             *
             * @param lowCardinalityKeyValues 当前低基数键值集合
             * @return {@code true} 表示 URI 标签缺失或为 UNKNOWN
             */
            private static boolean isUriTagNullOrUnknown(KeyValues lowCardinalityKeyValues) {

                Optional<KeyValue> uriKeyValue = lowCardinalityKeyValues.stream()
                        .filter(keyValue -> KEY_URI.equals(keyValue.getKey())).findFirst();

                return (uriKeyValue.isEmpty() || UNKNOWN.equals(uriKeyValue.get().getValue()));

            }

        };

    }

}
