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
 * 机器集群配置修改请求基础接口。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public interface ClusterModifyRequest {

    /**
     * 获取目标机器所属应用名。
     *
     * @return 应用名
     */
    String getApp();

    /**
     * 获取目标机器 IP。
     *
     * @return 目标机器 IP
     */
    String getIp();

    /**
     * 获取目标机器命令端口。
     *
     * @return 目标机器命令端口
     */
    Integer getPort();

    /**
     * 获取机器的目标集群模式（客户端或服务端）。
     *
     * @return 目标集群模式
     */
    Integer getMode();
}
