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
package com.alibaba.csp.sentinel.dashboard.domain.cluster;

/**
 * 集群分组实体：一个 token server 及绑定到它的客户端。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterGroupEntity extends BaseClusterServerMachine {

    /** 返回该实体的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterGroupEntity{" +
            "machineId='" + machineId + '\'' +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            ", clientSet=" + clientSet +
            ", belongToApp=" + belongToApp +
            '}';
    }
}
