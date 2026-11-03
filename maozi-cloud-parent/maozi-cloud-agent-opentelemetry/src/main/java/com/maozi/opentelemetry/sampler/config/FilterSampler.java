package com.maozi.opentelemetry.sampler.config;

import com.maozi.common.EnvironmentUtil;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.opentelemetry.sampler.enums.FilterType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;

/**
 * 自定义链路追踪采样器
 * <p>
 * 基于规则过滤噪声 Span，减少无用链路数据上报。过滤策略包括：
 * <ul>
 *   <li>启动阶段（main 线程）的 Span 丢弃，避免应用启动过程产生噪声</li>
 *   <li>Nacos 心跳等指定 Span 名称直接丢弃</li>
 *   <li>通过 {@link FilterType} 按 Span 属性过滤 HTTP（actuator/api-docs）、Redis（PING/AUTH 等）、MySQL（连接探活 SQL）等噪声请求</li>
 * </ul>
 * 未命中过滤规则的 Span 正常采样上报；过滤过程发生异常时为保证不丢数据，默认放行采样。
 * </p>
 *
 * @author pengjinlong
 */
public class FilterSampler implements Sampler {

    /** 需要直接丢弃的 Span 名称列表（如 Nacos 心跳） */
    private final List<String> EXCLUDED_TARGETS = List.of("NacosDiscoveryHeartBeatPublisher$$Lambda.run");

    /**
     * 返回采样器名称，与 SPI 提供者注册名一致
     *
     * @return 采样器注册名称
     */
    @Override
    public String getDescription() {
        return FilterSamplerProvider.PROVIDER_NAME;
    }

    /**
     * 采样决策：按规则过滤噪声 Span，未命中过滤规则或过滤异常时放行采样
     *
     * @param parentContext 父上下文
     * @param traceId 链路追踪 ID
     * @param name Span 名称
     * @param spanKind Span 类型
     * @param attributes Span 携带的属性集合
     * @param parentLinks 父链路列表
     * @return 采样结果（DROP 或 RECORD_AND_SAMPLE）
     */
    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {

        try{

            // 启动时屏蔽上报Sample
            if("main".equals(Thread.currentThread().getName())){
                return SamplingResult.create(SamplingDecision.DROP);
            }

            // 通过SpanName过滤
            if(EXCLUDED_TARGETS.contains(name)){
                return SamplingResult.create(SamplingDecision.DROP);
            }

            // 通过属性过滤
            if(!attributes.isEmpty()){
                for(FilterType type : FilterType.values()){

                    // 任一过滤器命中即丢弃该 Span
                    if(type.getHandle().filter(attributes)){
                        return SamplingResult.create(SamplingDecision.DROP);
                    }

                }
            }

            // 未命中任何过滤规则，放行采样
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);

        // 若异常则不过滤（放行采样），非生产环境打印堆栈便于排查
        }catch (Exception e){

            if(EnvironmentUtil.notEnvironment(EnvironmentType.PROD)){
                e.printStackTrace();
            }

            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);

        }

    }

}
