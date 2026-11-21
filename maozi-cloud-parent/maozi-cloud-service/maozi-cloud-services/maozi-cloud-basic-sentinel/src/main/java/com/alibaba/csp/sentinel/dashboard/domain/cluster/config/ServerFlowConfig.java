/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.domain.cluster.config;

/**
 * 集群 token server 流控配置：超额数量、统计时长及最大允许 QPS。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ServerFlowConfig {

    /** 超额数量默认值。 */
    public static final double DEFAULT_EXCEED_COUNT = 1.0d;
    /** 最大占用比例默认值。 */
    public static final double DEFAULT_MAX_OCCUPY_RATIO = 1.0d;

    /** 默认统计时长，单位毫秒。 */
    public static final int DEFAULT_INTERVAL_MS = 1000;
    /** 统计时长内的默认抽样数。 */
    public static final int DEFAULT_SAMPLE_COUNT = 10;
    /** 最大允许 QPS 默认值。 */
    public static final double DEFAULT_MAX_ALLOWED_QPS = 30000;

    /** 流控配置所属 namespace。 */
    private final String namespace;

    /** 超额数量。 */
    private Double exceedCount = DEFAULT_EXCEED_COUNT;
    /** 最大占用比例。 */
    private Double maxOccupyRatio = DEFAULT_MAX_OCCUPY_RATIO;
    /** 统计时长，单位毫秒。 */
    private Integer intervalMs = DEFAULT_INTERVAL_MS;
    /** 统计时长内的抽样数。 */
    private Integer sampleCount = DEFAULT_SAMPLE_COUNT;

    /** 最大允许 QPS。 */
    private Double maxAllowedQps = DEFAULT_MAX_ALLOWED_QPS;

    /** 创建使用默认 namespace（"default"）的流控配置。 */
    public ServerFlowConfig() {
        this("default");
    }

    /** 创建指定 namespace 的流控配置。 */
    public ServerFlowConfig(String namespace) {
        this.namespace = namespace;
    }

    /** 获取流控配置所属 namespace。 */
    public String getNamespace() {
        return namespace;
    }

    /** 获取超额数量。 */
    public Double getExceedCount() {
        return exceedCount;
    }

    /** 设置超额数量并返回当前对象。 */
    public ServerFlowConfig setExceedCount(Double exceedCount) {
        this.exceedCount = exceedCount;
        return this;
    }

    /** 获取最大占用比例。 */
    public Double getMaxOccupyRatio() {
        return maxOccupyRatio;
    }

    /** 设置最大占用比例并返回当前对象。 */
    public ServerFlowConfig setMaxOccupyRatio(Double maxOccupyRatio) {
        this.maxOccupyRatio = maxOccupyRatio;
        return this;
    }

    /** 获取统计时长，单位毫秒。 */
    public Integer getIntervalMs() {
        return intervalMs;
    }

    /** 设置统计时长，单位毫秒，并返回当前对象。 */
    public ServerFlowConfig setIntervalMs(Integer intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    /** 获取统计时长内的抽样数。 */
    public Integer getSampleCount() {
        return sampleCount;
    }

    /** 设置统计时长内的抽样数并返回当前对象。 */
    public ServerFlowConfig setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
        return this;
    }

    /** 获取最大允许 QPS。 */
    public Double getMaxAllowedQps() {
        return maxAllowedQps;
    }

    /** 设置最大允许 QPS 并返回当前对象。 */
    public ServerFlowConfig setMaxAllowedQps(Double maxAllowedQps) {
        this.maxAllowedQps = maxAllowedQps;
        return this;
    }

    /** 返回该配置的字符串表示。 */
    @Override
    public String toString() {
        return "ServerFlowConfig{" +
            "namespace='" + namespace + '\'' +
            ", exceedCount=" + exceedCount +
            ", maxOccupyRatio=" + maxOccupyRatio +
            ", intervalMs=" + intervalMs +
            ", sampleCount=" + sampleCount +
            ", maxAllowedQps=" + maxAllowedQps +
            '}';
    }
}
