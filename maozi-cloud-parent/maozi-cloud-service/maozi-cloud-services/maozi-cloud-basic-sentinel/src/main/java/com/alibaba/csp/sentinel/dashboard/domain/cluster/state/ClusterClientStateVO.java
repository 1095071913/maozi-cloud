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
package com.alibaba.csp.sentinel.dashboard.domain.cluster.state;

import com.alibaba.csp.sentinel.dashboard.domain.cluster.ClusterClientInfoVO;

/**
 * 机器的集群 token client 状态。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ClusterClientStateVO {

    /** 集群 token client 配置信息（server 地址、客户端状态及请求超时时间）。 */
    private ClusterClientInfoVO clientConfig;

    /** 获取集群客户端配置信息。 */
    public ClusterClientInfoVO getClientConfig() {
        return clientConfig;
    }

    /** 设置集群客户端配置信息并返回当前对象。 */
    public ClusterClientStateVO setClientConfig(ClusterClientInfoVO clientConfig) {
        this.clientConfig = clientConfig;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterClientStateVO{" +
            "clientConfig=" + clientConfig +
            '}';
    }
}
