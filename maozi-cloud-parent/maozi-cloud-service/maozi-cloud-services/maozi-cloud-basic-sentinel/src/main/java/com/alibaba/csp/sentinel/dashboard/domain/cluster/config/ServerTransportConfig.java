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
public class ServerTransportConfig {

    /** The default port of the cluster server transport. */
    public static final int DEFAULT_PORT = 18730;

    /** The default idle time in seconds of the cluster server transport. */
    public static final int DEFAULT_IDLE_SECONDS = 600;

    /** The port of the cluster server transport. */
    private Integer port;

    /** The idle time in seconds of the cluster server transport. */
    private Integer idleSeconds;

    /** Create a config with the default port and idle seconds. */
    public ServerTransportConfig() {
        this(DEFAULT_PORT, DEFAULT_IDLE_SECONDS);
    }

    /** Create a config with the given port and idle seconds. */
    public ServerTransportConfig(Integer port, Integer idleSeconds) {
        this.port = port;
        this.idleSeconds = idleSeconds;
    }

    /** Get the port. */
    public Integer getPort() {
        return port;
    }

    /** Set the port and return this. */
    public ServerTransportConfig setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Get the idle seconds. */
    public Integer getIdleSeconds() {
        return idleSeconds;
    }

    /** Set the idle seconds and return this. */
    public ServerTransportConfig setIdleSeconds(Integer idleSeconds) {
        this.idleSeconds = idleSeconds;
        return this;
    }

    /** Return a string representation of this config. */
    @Override
    public String toString() {
        return "ServerTransportConfig{" +
            "port=" + port +
            ", idleSeconds=" + idleSeconds +
            '}';
    }
}
