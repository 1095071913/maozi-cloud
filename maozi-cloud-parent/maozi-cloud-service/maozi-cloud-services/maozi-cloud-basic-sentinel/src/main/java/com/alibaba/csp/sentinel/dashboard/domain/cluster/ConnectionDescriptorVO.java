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
 * 到集群 token server 的一条连接描述。
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 1.4.0
 */
public class ConnectionDescriptorVO {

    /** 连接的对端地址。 */
    private String address;
    /** 连接的对端主机名。 */
    private String host;

    /** 获取连接的对端地址。 */
    public String getAddress() {
        return address;
    }

    /** 设置连接的对端地址并返回当前对象。 */
    public ConnectionDescriptorVO setAddress(String address) {
        this.address = address;
        return this;
    }

    /** 获取连接的对端主机名。 */
    public String getHost() {
        return host;
    }

    /** 设置连接的对端主机名并返回当前对象。 */
    public ConnectionDescriptorVO setHost(String host) {
        this.host = host;
        return this;
    }

    /** 返回该 VO 的字符串表示。 */
    @Override
    public String toString() {
        return "ConnectionDescriptorVO{" +
            "address='" + address + '\'' +
            ", host='" + host + '\'' +
            '}';
    }
}
