/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.maozi.db.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.maozi.db.constant.TableColumnName;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MyBatis-Plus 配置类
 * <p>
 * 配置 MyBatis-Plus 的拦截器插件，包括多租户拦截器和分页插件。
 * 多租户通过 client_id 字段实现数据隔离，仅对配置中指定的表启用。
 * 分页插件适配 MySQL 方言。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class MybatisPlusConfig {

    /** 需要启用多租户的表名列表 */
    @Value("${mybatis-plus.configuration.tenantTables.split(','):}")
    private List<String> tenantTables;

    /**
     * 创建 MyBatis-Plus 拦截器
     * <p>
     * 注册多租户拦截器和分页拦截器。
     * </p>
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加多租户拦截器，通过 client_id 字段实现数据隔离
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {

            /**
             * 获取租户 ID
             *
             * @return 租户 ID 值（当前实现返回空字符串占位，尚未接入真实租户上下文）
             */
            @Override
            public LongValue getTenantId() {
                return new LongValue("");
            }

            /**
             * 判断是否忽略该表的多租户处理
             *
             * @param tableName 表名
             * @return 忽略返回 true
             */
            @Override
            public boolean ignoreTable(String tableName) {
                // 未配置租户表列表时，忽略所有表的多租户处理
                if (tenantTables == null) {
                    return true;
                }
                // 只对配置中指定的表启用多租户，其余表忽略
                return !tenantTables.contains(tableName);
            }

            /**
             * 获取租户 ID 字段名
             *
             * @return 字段名 client_id
             */
            @Override
            public String getTenantIdColumn() {
                return TableColumnName.CLIENT_ID;
            }

        }));

        // 添加分页插件，适配 MySQL 方言，自动处理分页 SQL 的拼接
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;

    }

}
