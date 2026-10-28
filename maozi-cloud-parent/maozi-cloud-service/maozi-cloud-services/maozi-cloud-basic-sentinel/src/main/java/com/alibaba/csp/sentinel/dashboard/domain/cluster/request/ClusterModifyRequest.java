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
package com.alibaba.csp.sentinel.dashboard.domain.cluster.request;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public interface ClusterModifyRequest {

    /** Get the application name of the target machine. */
    String getApp();

    /** Get the IP of the target machine. */
    String getIp();

    /** Get the command port of the target machine. */
    Integer getPort();

    /** Get the target cluster mode of the machine (client or server). */
    Integer getMode();
}
