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

/**
 * 机器集群配置修改请求基类，持有目标机器公共字段（app、ip、port 和 mode）。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public abstract class AbstractClusterModifyRequest implements ClusterModifyRequest {

    /** 目标机器所属应用名。 */
    protected String app;
    /** 目标机器 IP 地址。 */
    protected String ip;
    /** 目标机器命令端口。 */
    protected Integer port;
    /** 机器的目标集群模式。 */
    protected Integer mode;

    /** 获取应用名。 */
    @Override
    public String getApp() {
        return app;
    }

    /** 设置应用名并返回当前对象。 */
    public AbstractClusterModifyRequest setApp(String app) {
        this.app = app;
        return this;
    }

    /** 获取 IP 地址。 */
    @Override
    public String getIp() {
        return ip;
    }

    /** 设置 IP 地址并返回当前对象。 */
    public AbstractClusterModifyRequest setIp(String ip) {
        this.ip = ip;
        return this;
    }

    /** 获取命令端口。 */
    @Override
    public Integer getPort() {
        return port;
    }

    /** 设置命令端口并返回当前对象。 */
    public AbstractClusterModifyRequest setPort(Integer port) {
        this.port = port;
        return this;
    }

    /** 获取集群模式。 */
    @Override
    public Integer getMode() {
        return mode;
    }

    /** 设置集群模式并返回当前对象。 */
    public AbstractClusterModifyRequest setMode(Integer mode) {
        this.mode = mode;
        return this;
    }
}
