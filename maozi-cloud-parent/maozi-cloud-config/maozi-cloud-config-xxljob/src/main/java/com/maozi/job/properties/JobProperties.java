package com.maozi.job.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 分布式任务调度配置属性
 * <p>
 * 从配置文件中读取 {@code xxl.job} 前缀下的属性，
 * 用于配置调度中心地址、访问令牌及执行器参数。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "xxl.job")
public class JobProperties {

    /** 访问令牌 */
    private String accessToken;

    /** 调度中心配置 */
    private Admin admin = new Admin();

    /** 执行器配置 */
    private Executor executor = new Executor();

    /**
     * 调度中心配置
     *
    * @author maozi
     */
    @Data
    public static class Admin {

        /** 调度中心地址 */
        private String addresses;

    }

    /**
     * 执行器配置
     *
    * @author maozi
     */
    @Data
    public static class Executor {

        /** 执行器应用名称 */
        private String appname;

        /** 执行器 IP */
        private String ip;

        /** 执行器端口 */
        private int port;

        /** 执行器日志文件路径 */
        private String logpath;

        /** 日志保留天数 */
        private int logretentiondays;

    }

}
