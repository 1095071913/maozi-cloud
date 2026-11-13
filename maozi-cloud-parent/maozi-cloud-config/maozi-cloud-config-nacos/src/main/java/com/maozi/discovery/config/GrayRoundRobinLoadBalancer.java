package com.maozi.discovery.config;

import com.maozi.common.CollectionUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.context.VersionContext;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 灰度轮询负载均衡器
 * <p>
 * 实现 Spring Cloud 的 {@link ReactorServiceInstanceLoadBalancer} 接口，
 * 基于请求头中的版本号进行灰度流量路由。将服务实例分为灰度节点和主版本节点两组，
 * 优先路由到匹配版本的灰度节点，否则路由到主版本节点，在匹配的实例组内使用轮询策略。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
public class GrayRoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    /** 服务实例列表提供者 */
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    /** 轮询位置计数器 */
    private final AtomicInteger position = new AtomicInteger(new Random().nextInt(1000));

    /**
     * 构造方法
     *
     * @param serviceInstanceListSupplierProvider 服务实例列表提供者
     */
    public GrayRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    /**
     * 选择服务实例
     * <p>
     * 从请求上下文中提取请求头，传递给灰度路由逻辑。
     * </p>
     *
     * @param request 负载均衡请求
     * @return 包含选中服务实例的 Mono
     */
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {

        // 请求头，用于提取版本号进行灰度匹配
        HttpHeaders headers;

        // 负载均衡请求上下文
        Object requestContext = request.getContext();

        // 上下文为 DefaultRequestContext 时，从其封装的客户端请求中提取请求头
        if(requestContext instanceof DefaultRequestContext defaultRequestContext){

            RequestData clientRequest = (RequestData)defaultRequestContext.getClientRequest();

            headers = clientRequest.getHeaders();

        }else{
            // 否则上下文本身即请求头对象，直接强转使用
            headers = (HttpHeaders) requestContext;
        }

        // 获取服务实例列表提供者，容器中不存在时退化为空实现
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);

        // 异步获取服务实例列表，并基于请求头进行灰度路由选择
        return supplier.get().next().map(list -> getInstanceResponse(list, headers));

    }

    /**
     * 根据版本号进行灰度路由并轮询选择实例
     * <p>
     * 从请求头中获取版本号，将服务实例分为灰度节点和主版本节点两组。
     * 优先使用灰度节点，若无匹配的灰度节点则使用主版本节点。
     * 在选中的实例组内使用轮询策略选择具体实例。
     * </p>
     *
     * @param instances 可用的服务实例列表
     * @param headers HTTP 请求头
     * @return 包含选中服务实例的响应
     */
    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, HttpHeaders headers) {

        // 无可用服务实例时直接抛出服务不存在异常
        if(ObjectUtil.isNullEmpty(instances)){
            throw new BusinessResultException(SystemErrorCode.SERVICE_NOT_EXIST_ERROR).setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);
        }

        // 从请求头获取调用方携带的版本号（可能为空，为空时不参与灰度匹配）
        String version = headers.getFirst(VersionContext.KEY);

        // 主版本（main）实例分组
        List<ServiceInstance> mainApplicationClients = CollectionUtil.newArrayList();

        // 与请求版本匹配的灰度实例分组
        List<ServiceInstance> grayApplicationClients = CollectionUtil.newArrayList();

        instances.forEach(instance -> {

            // 实例元数据 version 的格式为 "{标识}-{版本}"，按 "-" 分割后取最后一段作为实际版本号参与灰度匹配
            String [] clientApplicationVersionSplit = instance.getMetadata().get(VersionContext.NACOS_VERSION_KEY).split("-");

            String clientApplicationVersion = clientApplicationVersionSplit[clientApplicationVersionSplit.length - 1];

            // 请求携带版本且与实例版本一致时，归入灰度实例分组
            if(StringUtils.isNotBlank(version) && version.equals(clientApplicationVersion)){
                grayApplicationClients.add(instance);
            }

            // 实例版本为主版本时，归入主版本实例分组
            if(VersionContext.APPLICATION_DEFAULT_VERSION.equals(clientApplicationVersion)){
                mainApplicationClients.add(instance);
            }

        });

        // 优先使用灰度节点，若无匹配的灰度节点则回退到主版本节点
        List<ServiceInstance> applicationClients = ObjectUtil.isNotNullEmpty(grayApplicationClients) ? grayApplicationClients : mainApplicationClients;

        // 灰度分组与主版本分组均为空时，抛出服务不存在异常
        if(ObjectUtil.isNullEmpty(applicationClients)){
            throw new BusinessResultException(SystemErrorCode.SERVICE_NOT_EXIST_ERROR).setHttpCode(SystemErrorCode.SYSTEM_ERROR_DEFAULT_CODE);
        }

        // 轮询计数器自增后按位与 Integer.MAX_VALUE 屏蔽符号位保证非负，再对实例数取模实现组内均匀轮询
        int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;

        ServiceInstance instance = applicationClients.get(pos % applicationClients.size());

        return new DefaultResponse(instance);

    }

}
