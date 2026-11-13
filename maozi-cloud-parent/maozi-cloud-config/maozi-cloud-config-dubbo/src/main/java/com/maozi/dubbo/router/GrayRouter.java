package com.maozi.dubbo.router;

import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.context.VersionContext;
import com.maozi.dubbo.provider.ProviderFirstParams;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.router.RouterResult;

import java.util.List;

/**
 * 灰度路由器
 * <p>
 * 基于 Dubbo 的 {@link AbstractRouter} 实现灰度发布路由策略。
 * 根据当前请求携带的版本号（从 {@link VersionContext} 获取），
 * 将流量路由到匹配版本的服务实例（灰度节点），若无匹配的灰度节点则回退到主版本节点。
 * </p>
 * <p>
 * 路由逻辑说明：
 * <ul>
 *   <li>灰度节点：服务实例的 {@code application.version} 参数与当前请求版本号完全匹配</li>
 *   <li>主版本节点：服务实例的 {@code application.version} 参数等于默认版本（{@link VersionContext#APPLICATION_DEFAULT_VERSION}）</li>
 *   <li>优先路由到灰度节点，若不存在灰度节点则回退到主版本节点，保证服务可用性</li>
 * </ul>
 * </p>
 * <p>
 * 该路由器由 {@link RouterFactory} 通过 Dubbo SPI 机制创建，
 * 在消费者端自动激活，对所有出站 RPC 调用生效。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 * @see AbstractRouter
 * @see RouterFactory
 * @see VersionContext
 */
public class GrayRouter extends AbstractRouter {

    /**
     * 根据请求版本号将可用服务实例分为灰度节点和主版本节点两组，
     * 优先返回灰度节点列表，若无灰度节点则返回主版本节点列表
     *
     * @param invokers           可用的服务调用者列表，包含所有已注册的服务实例
     * @param url                服务 URL，包含服务接口、分组、版本等元数据
     * @param invocation         调用信息，包含方法名、参数类型、参数值等
     * @param needToPrintMessage 是否需要打印路由信息（用于调试和日志）
     * @param <T>                服务接口类型
     * @return {@link RouterResult} 包含路由后的服务调用者列表
     * @throws RpcException RPC 调用异常
     */
    @Override
    public <T> RouterResult<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage) throws RpcException {

        // 从线程上下文中获取当前请求的版本号，用于灰度匹配
        String version = VersionContext.CONTEXT.get();

        // 主版本节点列表（默认版本的服务实例）
        List<Invoker<T>> mainApplicationClients = CollectionUtil.newArrayList();

        // 灰度节点列表（与请求版本匹配的服务实例）
        List<Invoker<T>> grayApplicationClients = CollectionUtil.newArrayList();

        // 遍历所有可用的服务实例，按版本号分类
        invokers.forEach(invoker ->{

            // 获取服务实例的 application.version 参数，用于版本匹配
            String clientApplicationVersion = invoker.getUrl().getParameter(ProviderFirstParams.APPLICATION_VERSION_KEY);

            // 如果请求版本号非空且与服务实例版本完全匹配，则归为灰度节点
            if(StringUtils.isNotBlank(version) && version.equals(clientApplicationVersion)){
                grayApplicationClients.add(invoker);
            }

            // 如果服务实例版本为默认版本，则归为主版本节点
            if(VersionContext.APPLICATION_DEFAULT_VERSION.equals(clientApplicationVersion)){
                mainApplicationClients.add(invoker);
            }

        });

        // 优先返回灰度节点，若无灰度节点则回退到主版本节点，保证服务可用性
        return new RouterResult<>(ObjectUtil.isNotNullEmpty(grayApplicationClients) ? grayApplicationClients : mainApplicationClients);

    }

}
