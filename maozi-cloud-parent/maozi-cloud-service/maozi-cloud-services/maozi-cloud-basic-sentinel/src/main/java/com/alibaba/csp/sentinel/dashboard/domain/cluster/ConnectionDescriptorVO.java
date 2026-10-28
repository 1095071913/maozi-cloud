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
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ConnectionDescriptorVO {

    /** The remote address of the connection. */
    private String address;
    /** The remote host of the connection. */
    private String host;

    /** Get the remote address of the connection. */
    public String getAddress() {
        return address;
    }

    /** Set the remote address of the connection and return this. */
    public ConnectionDescriptorVO setAddress(String address) {
        this.address = address;
        return this;
    }

    /** Get the remote host of the connection. */
    public String getHost() {
        return host;
    }

    /** Set the remote host of the connection and return this. */
    public ConnectionDescriptorVO setHost(String host) {
        this.host = host;
        return this;
    }

    /** Get the string representation of this VO. */
    @Override
    public String toString() {
        return "ConnectionDescriptorVO{" +
            "address='" + address + '\'' +
            ", host='" + host + '\'' +
            '}';
    }
}
