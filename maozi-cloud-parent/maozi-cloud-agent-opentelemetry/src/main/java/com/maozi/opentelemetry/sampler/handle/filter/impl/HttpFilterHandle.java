package com.maozi.opentelemetry.sampler.handle.filter.impl;

import com.maozi.opentelemetry.sampler.handle.filter.BaseFilterHandle;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.InternalAttributeKeyImpl;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * HTTP Span 过滤器
 * <p>
 * 根据 Span 属性中的 {@code url.path} / {@code url.full} 匹配需要排除的 HTTP 路径
 * （如 {@code /actuator}、{@code /v3/api-docs/**}），命中则丢弃对应 Span，
 * 避免监控探活和文档接口产生噪声链路数据。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/07/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HttpFilterHandle extends BaseFilterHandle {

    /** 需要排除的 URL 路径模式列表（支持 ** 通配符） */
    private static final List<String> names = List.of(
            "/actuator",
            "/actuator/**",
            "/v3/api-docs/**"
    );

    /** 与 {@link #names} 对应的预编译正则列表（** 转为 .*），避免热路径上重复编译 */
    private static final List<Pattern> EXCLUDED_PATH_PATTERNS = names.stream()
            .map(excludedPath -> Pattern.compile(excludedPath.replace("**", ".*")))
            .toList();

    /** Span 属性键：HTTP 请求路径 */
    private static final String ATTR_URL_PATH = "url.path";

    /** Span 属性键：HTTP 完整 URL */
    private static final String ATTR_URL_FULL = "url.full";

    /** HTTP 协议前缀 */
    private static final String PROTOCOL_HTTP = "http://";

    /** HTTPS 协议前缀 */
    private static final String PROTOCOL_HTTPS = "https://";

    /** 协议分隔符 */
    private static final String PROTOCOL_SEPARATOR = "://";

    /**
     * 返回需要排除的 URL 路径模式列表
     *
     * @return 排除路径模式列表
     */
    @Override
    public List<String> getNames() {
        return names;
    }

    /**
     * 根据 Span 属性中的请求路径判断是否过滤该 Span
     * <p>
     * 优先取 {@code url.path}，为空时回退到 {@code url.full}；
     * 完整 URL 会先剥离协议与域名部分并去除查询参数，再进行路径匹配。
     * </p>
     *
     * @param attributes Span 携带的属性集合
     * @return {@code true} 表示路径命中排除规则应丢弃；{@code false} 表示保留
     */
    @Override
    public Boolean filter(Attributes attributes) {

        // 优先取 url.path 属性
        String httpPath = attributes.get(InternalAttributeKeyImpl.create(ATTR_URL_PATH, AttributeType.STRING));
        if(Objects.isNull(httpPath)){
            // url.path 缺失时回退到 url.full
            httpPath = attributes.get(InternalAttributeKeyImpl.create(ATTR_URL_FULL, AttributeType.STRING));
        }

        // 无路径信息，不过滤
        if(Objects.isNull(httpPath)){
            return false;
        }

        // 完整 URL：剥离协议与域名，仅保留路径部分
        if(httpPath.contains(PROTOCOL_HTTP) || httpPath.contains(PROTOCOL_HTTPS)){

            httpPath = httpPath.split(PROTOCOL_SEPARATOR)[1];

            // 定位域名后的首个 "/"，截取路径部分
            int index = httpPath.indexOf("/");

            if(index != -1){

                httpPath = httpPath.substring(index);

                // 去除查询参数
                httpPath = httpPath.split("\\?")[0];

            }

        }

        return matchPath(httpPath);

    }

    /**
     * 检查请求路径是否匹配需要排除的路径模式。
     * <p>
     * 使用预编译正则（排除规则中的 {@code **} 通配符已转为 {@code .*}）与请求路径整串匹配。
     * </p>
     *
     * @param path HTTP 请求路径
     * @return {@code true} 命中排除规则；{@code false} 未命中
     */
    public boolean matchPath(String path){

        for(Pattern excludedPattern : EXCLUDED_PATH_PATTERNS){

            // 使用预编译正则与请求路径整串匹配
            if(excludedPattern.matcher(path).matches()){
                return true;
            }

        }

        return false;

    }

}
