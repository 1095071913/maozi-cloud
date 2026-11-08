/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maozi.dubbo.provider;

import org.apache.dubbo.registry.support.DefaultProviderFirstParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;

/**
 * 服务提供者优先参数配置
 * <p>
 * 继承 Dubbo 的 {@link DefaultProviderFirstParams}，定义服务提供者端
 * 优先使用的参数集合。这些参数以服务提供者的值为准，不会被消费者端的值覆盖。
 * </p>
 * <p>
 * 包含的参数说明：
 * <ul>
 *   <li>{@code release} - Dubbo 框架的发布版本号</li>
 *   <li>{@code dubbo} - Dubbo 协议版本号</li>
 *   <li>{@code methods} - 服务暴露的方法列表</li>
 *   <li>{@code timestamp} - 服务注册时间戳</li>
 *   <li>{@code tag} - 服务标签（可用于标签路由）</li>
 *   <li>{@code application.version} - 自定义的应用版本号（用于灰度发布路由）</li>
 * </ul>
 * </p>
 * <p>
 * 该类通过 Dubbo SPI 机制加载，需在 resources/META-INF/dubbo 目录下配置。
 * </p>
 *
 * @author maozi
 * @see DefaultProviderFirstParams
 */
public class ProviderFirstParams extends DefaultProviderFirstParams {

    /** 自定义的应用版本参数名（值随服务注册写入 URL，供灰度路由做版本匹配） */
    public static final String APPLICATION_VERSION_KEY = "application.version";

    /**
     * 服务提供者优先参数集合
     * <p>
     * 使用不可变集合（unmodifiableSet）包装，防止运行时被修改。
     * 集合中的参数在服务注册时会以提供者端的值为准，
     * 消费者即使传递了相同名称的参数也不会覆盖提供者的值。
     * </p>
     */
    private static final Set<String> PARAMS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(RELEASE_KEY, DUBBO_VERSION_KEY, METHODS_KEY, TIMESTAMP_KEY, TAG_KEY, APPLICATION_VERSION_KEY))
    );

    /**
     * 获取服务提供者优先参数集合
     * <p>
     * Dubbo 框架在服务注册和引用过程中会调用此方法，
     * 获取需要以提供者端值为准的参数名称集合。
     * </p>
     *
     * @return 不可变的参数名集合，集合中的参数在提供者端和消费者端冲突时优先使用提供者的值
     */
    @Override
    public Set<String> params() {
        return PARAMS;
    }

}
