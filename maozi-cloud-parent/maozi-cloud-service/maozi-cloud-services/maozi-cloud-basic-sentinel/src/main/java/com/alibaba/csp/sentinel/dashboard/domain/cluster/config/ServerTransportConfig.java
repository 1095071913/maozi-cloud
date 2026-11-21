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
 * 集群 token server 传输配置：端口及空闲时间。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ServerTransportConfig {

    /** 集群 server 传输默认端口。 */
    public static final int DEFAULT_PORT = 18730;

    /** 集群 server 传输默认空闲时间，单位秒。 */
    public static final int DEFAULT_IDLE_SECONDS = 600;

    /** 集群 server 传输端口。 */
    private Integer port;

    /** 集群 server 传输空闲时间，单位秒。 */
    private Integer idleSeconds;

    /** 创建使用默认端口和空闲时间的配置。 */
    public ServerTransportConfig() {
        this(DEFAULT_PORT, DEFAULT_IDLE_SECONDS);
    }

    /** 创建指定端口和空闲时间的配置。 */
    public ServerTransportConfig(Integer port, Integer idleSeconds) {
        this.port = port;
        this.idleSeconds = idleSeconds;
    }

    /** 获取端口。 */
    public Integer getPort() {
        return port;
    }

    /** 设置端口并返回当前对象。 */
    public ServerTransportConfig setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** 获取空闲时间，单位秒。 */
    public Integer getIdleSeconds() {
        return idleSeconds;
    }

    /** 设置空闲时间，单位秒，并返回当前对象。 */
    public ServerTransportConfig setIdleSeconds(Integer idleSeconds) {
        this.idleSeconds = idleSeconds;
        return this;
    }

    /** 返回该配置的字符串表示。 */
    @Override
    public String toString() {
        return "ServerTransportConfig{" +
            "port=" + port +
            ", idleSeconds=" + idleSeconds +
            '}';
    }
}
