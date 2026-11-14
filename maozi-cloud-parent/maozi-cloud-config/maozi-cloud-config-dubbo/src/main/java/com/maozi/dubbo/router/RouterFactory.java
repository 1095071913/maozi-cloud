package com.maozi.dubbo.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.cluster.CacheableRouterFactory;
import org.apache.dubbo.rpc.cluster.Router;

/**
 * 灰度路由工厂
 * <p>
 * 继承 Dubbo 的 {@link CacheableRouterFactory}，在消费者端（CONSUMER）自动激活，
 * 用于创建 {@link GrayRouter} 灰度路由器实例。
 * </p>
 * <p>
 * 通过 Dubbo SPI（Service Provider Interface）机制自动加载，
 * 需在 {@code resources/META-INF/dubbo/org.apache.dubbo.rpc.cluster.RouterFactory} 文件中配置。
 * 继承 {@link CacheableRouterFactory} 使得创建的路由器实例会被缓存，避免重复创建。
 * </p>
 * <p>
 * {@code @Activate(group = CommonConstants.CONSUMER)} 注解确保该工厂仅在消费者端激活，
 * 不影响服务提供者端的路由逻辑。
 * </p>
 *
 * @author maozi
 * @date 2024/01/01
 * @see CacheableRouterFactory
 * @see GrayRouter
 * @see Activate
 */
@Activate(group = CommonConstants.CONSUMER)
public class RouterFactory extends CacheableRouterFactory {

    /**
     * 创建灰度路由器实例
     * <p>
     * 由 Dubbo 框架在初始化路由链时调用。由于继承了 {@link CacheableRouterFactory}，
     * 此方法只会在首次访问时被调用一次，后续会从缓存中获取已创建的路由器实例。
     * </p>
     *
     * @param url 服务 URL，包含服务接口、注册中心地址等配置信息
     * @return 新创建的 {@link GrayRouter} 灰度路由器实例
     */
    @Override
    protected Router createRouter(URL url) {
        return new GrayRouter();
    }

}
