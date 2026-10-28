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
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ServerFlowConfig {

    /** Default value of exceed count. */
    public static final double DEFAULT_EXCEED_COUNT = 1.0d;
    /** Default value of max occupy ratio. */
    public static final double DEFAULT_MAX_OCCUPY_RATIO = 1.0d;

    /** Default statistic interval in milliseconds. */
    public static final int DEFAULT_INTERVAL_MS = 1000;
    /** Default sample count in the statistic interval. */
    public static final int DEFAULT_SAMPLE_COUNT= 10;
    /** Default value of max allowed QPS. */
    public static final double DEFAULT_MAX_ALLOWED_QPS= 30000;

    /** The namespace of the server flow config. */
    private final String namespace;

    /** The exceed count. */
    private Double exceedCount = DEFAULT_EXCEED_COUNT;
    /** The max occupy ratio. */
    private Double maxOccupyRatio = DEFAULT_MAX_OCCUPY_RATIO;
    /** The statistic interval in milliseconds. */
    private Integer intervalMs = DEFAULT_INTERVAL_MS;
    /** The sample count in the statistic interval. */
    private Integer sampleCount = DEFAULT_SAMPLE_COUNT;

    /** The max allowed QPS. */
    private Double maxAllowedQps = DEFAULT_MAX_ALLOWED_QPS;

    /** Create a server flow config with the default namespace ("default"). */
    public ServerFlowConfig() {
        this("default");
    }

    /** Create a server flow config with the given namespace. */
    public ServerFlowConfig(String namespace) {
        this.namespace = namespace;
    }

    /** Get the namespace of the server flow config. */
    public String getNamespace() {
        return namespace;
    }

    /** Get the exceed count. */
    public Double getExceedCount() {
        return exceedCount;
    }

    /** Set the exceed count and return this. */
    public ServerFlowConfig setExceedCount(Double exceedCount) {
        this.exceedCount = exceedCount;
        return this;
    }

    /** Get the max occupy ratio. */
    public Double getMaxOccupyRatio() {
        return maxOccupyRatio;
    }

    /** Set the max occupy ratio and return this. */
    public ServerFlowConfig setMaxOccupyRatio(Double maxOccupyRatio) {
        this.maxOccupyRatio = maxOccupyRatio;
        return this;
    }

    /** Get the statistic interval in milliseconds. */
    public Integer getIntervalMs() {
        return intervalMs;
    }

    /** Set the statistic interval in milliseconds and return this. */
    public ServerFlowConfig setIntervalMs(Integer intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    /** Get the sample count in the statistic interval. */
    public Integer getSampleCount() {
        return sampleCount;
    }

    /** Set the sample count and return this. */
    public ServerFlowConfig setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
        return this;
    }

    /** Get the max allowed QPS. */
    public Double getMaxAllowedQps() {
        return maxAllowedQps;
    }

    /** Set the max allowed QPS and return this. */
    public ServerFlowConfig setMaxAllowedQps(Double maxAllowedQps) {
        this.maxAllowedQps = maxAllowedQps;
        return this;
    }

    /** Get the string representation of this config. */
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
