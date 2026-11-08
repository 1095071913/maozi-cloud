package com.maozi.monitor.config;

import de.codecentric.boot.admin.client.config.InstanceProperties;
import de.codecentric.boot.admin.client.registration.ApplicationFactory;
import de.codecentric.boot.admin.client.registration.ServletApplicationFactory;
import de.codecentric.boot.admin.client.registration.metadata.CompositeMetadataContributor;
import de.codecentric.boot.admin.client.registration.metadata.MetadataContributor;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Spring Boot Admin 客户端注册地址修正配置
 * <p>
 * Admin 客户端默认通过 {@code InetAddress.getLocalHost()} 解析注册主机，
 * 宿主机上该结果来自 /etc/hosts 的 hostname 映射（通常为 127.0.0.1），
 * 导致注册到 Admin Server 的地址不可访问；Docker 容器内容器 hostname
 * 解析为容器 IP，因此不受影响。
 * </p>
 * <p>
 * 本配置以自定义 {@link ApplicationFactory} 覆盖自动装配的默认实现
 * （自动装配处标注 @ConditionalOnMissingBean，用户 Bean 优先生效），
 * 仅修正主机解析：注册 IP 固定取 Spring Cloud 解析的
 * {@code spring.cloud.client.ip-address}（选取时自动跳过回环地址，
 * 宿主机环境为内网 IP，容器环境为容器 IP）。
 * </p>
 * <p>
 * 注册端口仍由 WebServerInitializedEvent 事件在服务启动后动态获取实际端口，
 * 因此 {@code server.port=0} 随机端口场景同样适用；
 * 注意不可配置 service-base-url，配置后将使用静态地址导致随机端口失效。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        name = "spring.boot.admin.client.enabled",                 // 配置项名
        havingValue = "true",
        matchIfMissing = true                 // 没配置时默认生效
)
public class AdminClientConfig {

    /**
     * 创建注册地址使用内网 IP 的应用工厂
     * <p>
     * 替换 Spring Boot Admin 自动装配的 {@link ServletApplicationFactory}，
     * 注册主机改为读取 {@code spring.cloud.client.ip-address}，
     * 未解析到时回退默认实现（保持 server.address 配置的原始语义）。
     * </p>
     *
     * @param instance Admin 实例配置属性
     * @param management 管理端点服务配置属性
     * @param server Web 服务配置属性
     * @param servletContext Servlet 上下文
     * @param pathMappedEndpoints 端点路径映射
     * @param webEndpointProperties Web 端点配置属性
     * @param metadataContributors 注册元数据贡献器
     * @param dispatcherServletPath DispatcherServlet 路径
     * @param environment 运行环境上下文
     * @return 使用 Spring Cloud 解析 IP 的应用工厂实例
     */
    @Bean
    public ApplicationFactory applicationFactory(InstanceProperties instance,
            ManagementServerProperties management, ServerProperties server, ServletContext servletContext,
            PathMappedEndpoints pathMappedEndpoints, WebEndpointProperties webEndpointProperties,
            ObjectProvider<MetadataContributor> metadataContributors,
            DispatcherServletPath dispatcherServletPath, Environment environment) {

        List<MetadataContributor> contributors = metadataContributors.stream().toList();
        MetadataContributor metadataContributor = new CompositeMetadataContributor(contributors);

        return new CloudIpApplicationFactory(instance, management, server, servletContext,
                pathMappedEndpoints, webEndpointProperties, metadataContributor,
                dispatcherServletPath, environment);
    }

    /**
     * 注册主机使用 Spring Cloud 解析 IP 的应用工厂
     * <p>
     * 覆写 getServiceHost 修正注册主机；getManagementHost 在未配置
     * management.server.address 时会委托 getServiceHost，因此无需重复覆写。
     * 端口解析（getLocalServerPort）依赖 WebServerInitializedEvent 事件写入，
     * 继承默认实现即可，兼容随机端口。
     * </p>
     */
    static class CloudIpApplicationFactory extends ServletApplicationFactory {

        /** Spring Cloud 解析的本机 IP 配置项 */
        private static final String CLOUD_IP_PROPERTY = "spring.cloud.client.ip-address";

        /** 运行环境上下文，用于读取 Spring Cloud 解析的本机 IP */
        private final Environment environment;

        /**
         * 构造应用工厂
         *
         * @param instance Admin 实例配置属性
         * @param management 管理端点服务配置属性
         * @param server Web 服务配置属性
         * @param servletContext Servlet 上下文
         * @param pathMappedEndpoints 端点路径映射
         * @param webEndpointProperties Web 端点配置属性
         * @param metadataContributor 注册元数据贡献器
         * @param dispatcherServletPath DispatcherServlet 路径
         * @param environment 运行环境上下文
         */
        CloudIpApplicationFactory(InstanceProperties instance, ManagementServerProperties management,
                ServerProperties server, ServletContext servletContext, PathMappedEndpoints pathMappedEndpoints,
                WebEndpointProperties webEndpointProperties, MetadataContributor metadataContributor,
                DispatcherServletPath dispatcherServletPath, Environment environment) {
            super(instance, management, server, servletContext, pathMappedEndpoints,
                    webEndpointProperties, metadataContributor, dispatcherServletPath);
            this.environment = environment;
        }

        /**
         * 获取注册主机
         * <p>
         * 优先返回 Spring Cloud 解析的本机 IP（跳过回环地址的内网 IP），
         * 解析失败时回退默认实现。
         * </p>
         *
         * @return 注册主机 IP
         */
        @Override
        protected String getServiceHost() {
            String ipAddress = environment.getProperty(CLOUD_IP_PROPERTY);
            if (StringUtils.hasText(ipAddress)) {
                return ipAddress;
            }
            return super.getServiceHost();
        }

    }

}
