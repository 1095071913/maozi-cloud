package com.maozi.discovery.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动后强制将本实例在注册中心置为上线状态。
 * <p>
 * 场景：单机容器滚动更新时，新容器可能复用旧容器的 IP + 端口。若旧实例停机时
 * 曾被置为 DOWN 且注销记录未及时清理，新实例注册后可能继承下线状态，导致流量
 * 全部切走后无可用节点。ApplicationReadyEvent 时注册已完成（早于 Ready 的
 * WebServerInitializedEvent 触发注册），此处再显式覆盖一次为 UP，保证每次
 * 启动后的最终状态一定是上线。
 * </p>
 *
 * @author maozi
 */
@Component
public class NacosInstanceStartupUpInitializer {

    /** 注册中心操作入口（Nacos 实现为 NacosServiceRegistry） */
    private final ServiceRegistry<Registration> serviceRegistry;

    /** 本实例的注册信息（IP / 端口 / 服务名） */
    private final Registration registration;

    public NacosInstanceStartupUpInitializer(ServiceRegistry<Registration> serviceRegistry, Registration registration) {
        this.serviceRegistry = serviceRegistry;
        this.registration = registration;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void forceUp() {
        serviceRegistry.setStatus(registration, "UP");
    }
}
