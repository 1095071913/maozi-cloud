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

import java.util.Set;

/**
 * 集群分配操作结果，携带分配失败的 server/client 集合及参与分配的机器总数。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterAppAssignResultVO {

    /** 分配失败的 server 集合。 */
    private Set<String> failedServerSet;
    /** 分配失败的 client 集合。 */
    private Set<String> failedClientSet;

    /** 参与分配的机器总数。 */
    private Integer totalCount;

    /** 获取分配失败的 server 集合。 */
    public Set<String> getFailedServerSet() {
        return failedServerSet;
    }

    /** 设置分配失败的 server 集合并返回当前对象。 */
    public ClusterAppAssignResultVO setFailedServerSet(Set<String> failedServerSet) {
        this.failedServerSet = failedServerSet;
        return this;
    }

    /** 获取分配失败的 client 集合。 */
    public Set<String> getFailedClientSet() {
        return failedClientSet;
    }

    /** 设置分配失败的 client 集合并返回当前对象。 */
    public ClusterAppAssignResultVO setFailedClientSet(Set<String> failedClientSet) {
        this.failedClientSet = failedClientSet;
        return this;
    }

    /** 获取参与分配的机器总数。 */
    public Integer getTotalCount() {
        return totalCount;
    }

    /** 设置参与分配的机器总数并返回当前对象。 */
    public ClusterAppAssignResultVO setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterAppAssignResultVO{"
                + "failedServerSet=" + failedServerSet
                + ", failedClientSet=" + failedClientSet
                + ", totalCount=" + totalCount
                + '}';
    }
}
