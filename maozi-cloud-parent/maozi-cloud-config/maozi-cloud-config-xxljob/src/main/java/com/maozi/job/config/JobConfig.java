package com.maozi.job.config;

import com.maozi.job.properties.JobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

        JobProperties.Executor executor = properties.getExecutor();

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(properties.getAdmin().getAddresses());
        xxlJobSpringExecutor.setAppname(executor.getAppname());
        xxlJobSpringExecutor.setIp(executor.getIp());
        xxlJobSpringExecutor.setPort(executor.getPort());
        xxlJobSpringExecutor.setAccessToken(properties.getAccessToken());
        xxlJobSpringExecutor.setLogPath(executor.getLogpath());
        xxlJobSpringExecutor.setLogRetentionDays(executor.getLogretentiondays());
        return xxlJobSpringExecutor;
    }

}
