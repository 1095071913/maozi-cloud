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
 * 修改机器集群 token client 配置的请求。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterClientModifyRequest extends AbstractClusterModifyRequest {

    /** 集群客户端的新配置。 */
    private ClusterClientConfig clientConfig;

    /** 获取集群客户端配置。 */
    public ClusterClientConfig getClientConfig() {
        return clientConfig;
    }

    /** 设置集群客户端配置并返回当前对象。 */
    public ClusterClientModifyRequest setClientConfig(
            ClusterClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        return this;
    }

    /** 返回该请求的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterClientModifyRequest{"
                + "app='" + app + '\''
                + ", ip='" + ip + '\''
                + ", port=" + port
                + ", mode=" + mode
                + ", clientConfig=" + clientConfig
                + '}';
    }
}
