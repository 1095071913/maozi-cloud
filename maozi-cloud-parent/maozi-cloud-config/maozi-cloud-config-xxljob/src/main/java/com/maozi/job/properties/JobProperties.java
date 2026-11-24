package com.maozi.job.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
@Component
@ConfigurationProperties(prefix = "xxl.job")
public class JobProperties {

    /** 与调度中心通信的访问令牌 */
    private String accessToken;

    /** 调度中心配置（缺省值由 boot-xxljob.yml 提供） */
    private Admin admin;

    /** 执行器配置（缺省值由 boot-xxljob.yml 提供） */
    private Executor executor;

    /**
     * 调度中心配置
     *
     * @author maozi
     * @date 2026/11/08
     */
    @Data
    public static class Admin {

        /** 调度中心地址，多个地址以英文逗号分隔 */
        private String addresses;

    }

    /**
     * 执行器配置
     *
     * @author maozi
     * @date 2026/11/08
     */
    @Data
    public static class Executor {

        /** 执行器应用名称 */
        private String appName;

        /** 执行器 IP，为空时自动获取本机 IP */
        private String ip;

        /** 执行器端口，未配置或小于等于 0 时自动从 9999 起探测可用端口 */
        private Integer port;

        /** 执行器日志文件路径 */
        private String logPath;

        /** 日志保留天数，未配置或小于 3 时不启用日志文件清理 */
        private Integer logRetentionDays;

    }

}
