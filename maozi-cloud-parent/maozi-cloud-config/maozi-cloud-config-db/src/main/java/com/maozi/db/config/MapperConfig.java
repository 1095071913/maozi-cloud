package com.maozi.db.config;

import com.maozi.common.context.ApplicationEnvironmentContext;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper 接口扫描配置
 * <p>
 * 按项目包前缀 + mapper 包模式扫描各模块下的 Mapper 接口并注册为 Bean。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/11/18 23:49
 */
@Configuration
@MapperScan(ApplicationEnvironmentContext.PACKAGE_PREFIX + MapperConfig.MAPPER_SCAN_PATTERN)
public class MapperConfig {

    /** Mapper 接口扫描包模式（拼接在项目包前缀之后，匹配各模块下的 mapper 包）；
     * 包级可见以便本类注解引用 */
    static final String MAPPER_SCAN_PATTERN = ".*.*.mapper";
    
}
