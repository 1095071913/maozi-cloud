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
package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowClusterConfig;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 热点参数流控规则的内存存储
 *
 * @author Eric Zhao
 * @date 2026/11/22
 * @since 0.2.1
 */
@Component
public class InMemParamFlowRuleStore extends AbstractInMemoryRuleRepository<ParamFlowRuleEntity> {

    /** 存储实体的静态 id 生成器 */
    private static AtomicLong ids = new AtomicLong(0);

    /** 使用静态计数器生成下一个自增 id */
    @Override
    protected long nextId() {
        return ids.incrementAndGet();
    }

    /**
     * 规则处于集群模式时将规则 id 设置为集群配置的集群流控 id；
     * 注意：实体原本无配置时此处新建的配置不会回写到实体上
     */
    @Override
    protected ParamFlowRuleEntity preProcess(ParamFlowRuleEntity entity) {
        if (entity != null && entity.isClusterMode()) {
            ParamFlowClusterConfig config = entity.getClusterConfig();
            if (config == null) {
                config = new ParamFlowClusterConfig();
            }
            // 设置集群规则 id
            config.setFlowId(entity.getId());
        }
        return entity;
    }
}
