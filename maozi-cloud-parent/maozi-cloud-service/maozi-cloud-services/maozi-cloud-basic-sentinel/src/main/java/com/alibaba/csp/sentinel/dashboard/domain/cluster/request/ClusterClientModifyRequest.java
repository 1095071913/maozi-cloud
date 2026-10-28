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
package com.alibaba.csp.sentinel.dashboard.domain.cluster.request;

import com.alibaba.csp.sentinel.dashboard.domain.cluster.config.ClusterClientConfig;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ClusterClientModifyRequest implements ClusterModifyRequest {

    /** The app name of the target machine. */
    private String app;

    /** The IP address of the target machine. */
    private String ip;

    /** The command port of the target machine. */
    private Integer port;

    /** The target cluster mode of the machine. */
    private Integer mode;

    /** The new config of the cluster client. */
    private ClusterClientConfig clientConfig;

    /** Get the app name. */
    @Override
    public String getApp() {
        return app;
    }

    /** Set the app name and return this. */
    public ClusterClientModifyRequest setApp(String app) {
        this.app = app;
        return this;
    }

    /** Get the IP address. */
    @Override
    public String getIp() {
        return ip;
    }

    /** Set the IP address and return this. */
    public ClusterClientModifyRequest setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** Get the command port. */
    @Override
    public Integer getPort() {
        return port;
    }

    /** Set the command port and return this. */
    public ClusterClientModifyRequest setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** Get the cluster mode. */
    @Override
    public Integer getMode() {
        return mode;
    }

    /** Set the cluster mode and return this. */
    public ClusterClientModifyRequest setMode(Integer mode) {
        this.mode = mode;
        return this;
    }

    /** Get the cluster client config. */
    public ClusterClientConfig getClientConfig() {
        return clientConfig;
    }

    /** Set the cluster client config and return this. */
    public ClusterClientModifyRequest setClientConfig(
        ClusterClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        return this;
    }
}
