package com.maozi.opentelemetry.sampler.config;

import com.maozi.opentelemetry.sampler.enums.FilterTypeEnum;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 自定义链路追踪采样器
 * <p>
 * 基于规则过滤噪声 Span，减少无用链路数据上报。过滤策略包括：
 * <ul>
 *   <li>启动阶段（main 线程）的 Span 丢弃，避免应用启动过程产生噪声</li>
 *   <li>Nacos 心跳等指定 Span 名称直接丢弃</li>
 *   <li>通过 {@link FilterTypeEnum} 按 Span 属性过滤 HTTP（actuator/api-docs）、
 *       Redis（PING/AUTH 等）、MySQL（连接探活 SQL）等噪声请求</li>
 * </ul>
 * 未命中过滤规则的 Span 正常采样上报；
 * 过滤过程发生异常时为保证不丢数据，默认放行采样。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/07/04
 */
public class FilterSampler implements Sampler {

    /**
     * JUL 日志器
     * <p>
     * 【规约豁免说明】阿里巴巴 Java 开发手册要求使用 SLF4J 门面而非日志系统 API，
     * 但本类运行于 javaagent 独立类加载环境：引入 SLF4J 会与宿主应用的日志框架
     * 发生类冲突，OpenTelemetry 官方 javaagent 内部同样使用 JUL，
     * 故此处属于合理豁免，禁止改为 SLF4J。
     * </p>
     */
    private static final Logger LOGGER = Logger.getLogger(FilterSampler.class.getName());

    /** 主线程名称：启动阶段的 Span 直接丢弃，避免应用启动过程产生噪声 */
    private static final String MAIN_THREAD_NAME = "main";

    /** 需要直接丢弃的 Span 名称列表（如 Nacos 心跳） */
    private static final List<String> EXCLUDED_TARGETS = List.of("NacosDiscoveryHeartBeatPublisher$$Lambda.run");

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
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {

        // 属性集合空保护：预检查规避空指针，避免依赖运行时异常兜底分支
        if (Objects.isNull(attributes)) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }

        try{

            // 启动阶段（main 线程）的 Span 直接丢弃，避免上报噪声
            if (MAIN_THREAD_NAME.equals(Thread.currentThread().getName())){
                return SamplingResult.create(SamplingDecision.DROP);
            }

            // 通过SpanName过滤
            if (EXCLUDED_TARGETS.contains(name)){
                return SamplingResult.create(SamplingDecision.DROP);
            }

            // 通过属性过滤
            if (!attributes.isEmpty()){
                for (FilterTypeEnum type : FilterTypeEnum.values()){

                    // 任一过滤器命中即丢弃该 Span
                    if (type.getHandle().filter(attributes)){
                        return SamplingResult.create(SamplingDecision.DROP);
                    }

                }
            }

            // 未命中任何过滤规则，放行采样
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);

        // 边界兜底：javaagent 采样热路径不允许异常逃逸到 OTel SDK 中断采样链路，
        // 捕获后完整记录异常（含堆栈）并放行采样，非以异常做流程控制
        }catch (RuntimeException e){

            // 使用 Supplier 延迟构造日志消息，避免无谓的字符串拼接开销
            // （JUL 惰性重载签名：Level, Throwable, Supplier）
            LOGGER.log(Level.WARNING, e, () -> "Span filter error, fallback to sample. spanName=" + name);

            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);

        }

    }

}
