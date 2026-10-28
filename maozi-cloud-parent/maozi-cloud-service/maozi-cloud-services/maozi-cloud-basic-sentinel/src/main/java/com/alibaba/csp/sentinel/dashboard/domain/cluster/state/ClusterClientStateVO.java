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
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ClusterClientStateVO {

    /**
     * Cluster token client config info (server address, client state and request timeout).
     */
    private ClusterClientInfoVO clientConfig;

    /** Get the cluster client config info. */
    public ClusterClientInfoVO getClientConfig() {
        return clientConfig;
    }

    /** Set the cluster client config info and return this. */
    public ClusterClientStateVO setClientConfig(ClusterClientInfoVO clientConfig) {
        this.clientConfig = clientConfig;
        return this;
    }

    /** Return a string representation of this VO. */
    @Override
    public String toString() {
        return "ClusterClientStateVO{" +
            "clientConfig=" + clientConfig +
            '}';
    }
}
