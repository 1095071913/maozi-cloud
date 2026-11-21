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
 * Result of a cluster assignment operation, carrying the failed server/client sets and the total machine count.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterAppAssignResultVO {

    /** The set of servers that failed in the assignment. */
    private Set<String> failedServerSet;
    /** The set of clients that failed in the assignment. */
    private Set<String> failedClientSet;

    /** The total count of machines in the assignment. */
    private Integer totalCount;

    /** Gets the set of servers that failed in the assignment. */
    public Set<String> getFailedServerSet() {
        return failedServerSet;
    }

    /** Sets the set of failed servers and returns this. */
    public ClusterAppAssignResultVO setFailedServerSet(Set<String> failedServerSet) {
        this.failedServerSet = failedServerSet;
        return this;
    }

    /** Gets the set of clients that failed in the assignment. */
    public Set<String> getFailedClientSet() {
        return failedClientSet;
    }

    /** Sets the set of failed clients and returns this. */
    public ClusterAppAssignResultVO setFailedClientSet(Set<String> failedClientSet) {
        this.failedClientSet = failedClientSet;
        return this;
    }

    /** Gets the total count of machines in the assignment. */
    public Integer getTotalCount() {
        return totalCount;
    }

    /** Sets the total count of machines and returns this. */
    public ClusterAppAssignResultVO setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
        return this;
    }

    /** Gets the string representation of this VO. */
    @Override
    public String toString() {
        return "ClusterAppAssignResultVO{" +
            "failedServerSet=" + failedServerSet +
            ", failedClientSet=" + failedClientSet +
            ", totalCount=" + totalCount +
            '}';
    }
}
