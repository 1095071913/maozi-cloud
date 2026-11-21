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
 * 单台机器的集群状态（地址、模式及目标地址）。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.1
 */
public class ClusterStateSingleVO {

    /** 机器地址。 */
    private String address;
    /** 机器的集群模式。 */
    private Integer mode;
    /** 目标地址。 */
    private String target;

    /** 获取机器地址。 */
    public String getAddress() {
        return address;
    }

    /** 设置机器地址并返回当前对象。 */
    public ClusterStateSingleVO setAddress(String address) {
        this.address = address;
        return this;
    }

    /** 获取机器的集群模式。 */
    public Integer getMode() {
        return mode;
    }

    /** 设置机器的集群模式并返回当前对象。 */
    public ClusterStateSingleVO setMode(Integer mode) {
        this.mode = mode;
        return this;
    }

    /** 获取目标地址。 */
    public String getTarget() {
        return target;
    }

    /** 设置目标地址并返回当前对象。 */
    public ClusterStateSingleVO setTarget(String target) {
        this.target = target;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ClusterStateSingleVO{" +
            "address='" + address + '\'' +
            ", mode=" + mode +
            ", target='" + target + '\'' +
            '}';
    }
}
