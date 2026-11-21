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

import com.alibaba.csp.sentinel.dashboard.domain.cluster.request.ClusterAppAssignMap;

import java.util.Set;

/**
 * Request for binding a set of clients to one cluster token server.
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterAppSingleServerAssignRequest {

    /** The assignment of the single token server with its bound clients. */
    private ClusterAppAssignMap clusterMap;
    /** The set of machines that remain unassigned to the server. */
    private Set<String> remainingList;

    /** Gets the assignment of the single token server. */
    public ClusterAppAssignMap getClusterMap() {
        return clusterMap;
    }

    /** Sets the assignment of the token server and returns this. */
    public ClusterAppSingleServerAssignRequest setClusterMap(ClusterAppAssignMap clusterMap) {
        this.clusterMap = clusterMap;
        return this;
    }

    /** Gets the set of machines that remain unassigned. */
    public Set<String> getRemainingList() {
        return remainingList;
    }

    /** Sets the set of unassigned machines and returns this. */
    public ClusterAppSingleServerAssignRequest setRemainingList(Set<String> remainingList) {
        this.remainingList = remainingList;
        return this;
    }

    /** Gets the string representation of this request. */
    @Override
    public String toString() {
        return "ClusterAppSingleServerAssignRequest{" +
            "clusterMap=" + clusterMap +
            ", remainingList=" + remainingList +
            '}';
    }
}
