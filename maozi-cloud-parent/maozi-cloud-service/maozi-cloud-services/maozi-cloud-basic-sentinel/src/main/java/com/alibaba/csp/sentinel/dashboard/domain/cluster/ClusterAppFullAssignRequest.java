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

import java.util.List;
import java.util.Set;

/**
 * @author Eric Zhao
 * @since 1.4.1
 */
public class ClusterAppFullAssignRequest {

    /** The list of token server assignments with their bound clients. */
    private List<ClusterAppAssignMap> clusterMap;
    /** The set of machines that remain unassigned in the cluster map. */
    private Set<String> remainingList;

    /** Get the list of token server assignments. */
    public List<ClusterAppAssignMap> getClusterMap() {
        return clusterMap;
    }

    /** Set the list of token server assignments and return this. */
    public ClusterAppFullAssignRequest setClusterMap(
        List<ClusterAppAssignMap> clusterMap) {
        this.clusterMap = clusterMap;
        return this;
    }

    /** Get the set of machines that remain unassigned. */
    public Set<String> getRemainingList() {
        return remainingList;
    }

    /** Set the set of unassigned machines and return this. */
    public ClusterAppFullAssignRequest setRemainingList(Set<String> remainingList) {
        this.remainingList = remainingList;
        return this;
    }

    /** Get the string representation of this request. */
    @Override
    public String toString() {
        return "ClusterAppFullAssignRequest{" +
            "clusterMap=" + clusterMap +
            ", remainingList=" + remainingList +
            '}';
    }
}
