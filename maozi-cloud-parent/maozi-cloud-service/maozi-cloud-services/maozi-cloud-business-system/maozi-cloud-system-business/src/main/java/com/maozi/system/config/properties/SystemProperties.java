package com.maozi.system.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统配置属性类
 * <p>
 * 对应配置文件中 "application-project-system" 前缀下的配置项，
 * 用于映射和管理系统级别的配置属性，包括项目名称、公司名称、图标等信息。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@Configuration
@ConfigurationProperties("application-project-system")
public class SystemProperties implements Serializable {

    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目名称 */
    private String projectName;

    /** 公司名称 */
    private String corporationName;

    /** 系统图标 */
    private String icon;

    /** 运行环境（如 dev、test、prod） */
    private String environment;

    /** 系统描述信息 */
    private String description;

    /** 版权信息 */
    private String copyright;

}
