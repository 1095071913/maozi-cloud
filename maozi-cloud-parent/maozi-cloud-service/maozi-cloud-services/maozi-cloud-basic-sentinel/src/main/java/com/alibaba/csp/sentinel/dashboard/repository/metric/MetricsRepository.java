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
package com.alibaba.csp.sentinel.dashboard.repository.metric;

import java.util.List;

/**
 * 聚合指标数据的仓储接口
 *
 * @param <T> 指标数据类型
 * @author Eric Zhao
 * @date 2026/11/22
 */
public interface MetricsRepository<T> {

    /**
     * 保存单条指标数据到仓储
     *
     * @param metric 待保存的指标数据
     */
    void save(T metric);

    /**
     * 批量保存指标数据到仓储
     *
     * @param metrics 待保存的指标数据集合
     */
    void saveAll(Iterable<T> metrics);

    /**
     * 查询指定应用和资源在一段时间内的全部指标数据
     *
     * @param app       Sentinel 应用名
     * @param resource  资源名
     * @param startTime 开始时间戳
     * @param endTime   结束时间戳
     * @return 满足查询条件的全部指标数据
     */
    List<T> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime);

    /**
     * 列出指定应用在 {@code [startTime, endTime]} 区间内有指标数据的资源名
     *
     * @param app       应用名
     * @param startTime 开始时间戳
     * @param endTime   结束时间戳
     * @return 资源名列表
     */
    List<String> listResourcesOfApp(String app, long startTime, long endTime);
}
