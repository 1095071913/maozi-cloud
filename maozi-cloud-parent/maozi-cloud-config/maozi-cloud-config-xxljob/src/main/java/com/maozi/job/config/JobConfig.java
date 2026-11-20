package com.maozi.job.config;

import com.maozi.job.properties.JobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

/**
 * XXL-Job 分布式任务调度配置
 * <p>
 * 通过 {@link JobProperties} 读取调度中心地址、执行器名称、
 * 端口、日志路径和访问令牌等参数，注册 XXL-Job 执行器实例。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class JobConfig {

    /** XXL-Job 配置属性 */
    @Resource
    private JobProperties properties;

    /**
     * 创建 XXL-Job 执行器实例
     *
     * @return 配置好的 XxlJobSpringExecutor
     */
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {

        JobProperties.Admin admin = properties.getAdmin();
        JobProperties.Executor executor = properties.getExecutor();

        // 配置缺失时快速失败并给出明确提示，避免无默认值后的空指针难以定位
        Objects.requireNonNull(admin, "xxl.job.admin 调度中心配置缺失");
        Objects.requireNonNull(executor, "xxl.job.executor 执行器配置缺失");

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(admin.getAddresses());
        xxlJobSpringExecutor.setAppname(executor.getAppName());
        xxlJobSpringExecutor.setIp(executor.getIp());
        Integer port = executor.getPort();
        xxlJobSpringExecutor.setPort(port == null ? 0 : port);
        xxlJobSpringExecutor.setAccessToken(properties.getAccessToken());
        xxlJobSpringExecutor.setLogPath(executor.getLogPath());
        Integer logRetentionDays = executor.getLogRetentionDays();
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays == null ? 0 : logRetentionDays);
        return xxlJobSpringExecutor;
    }

}
