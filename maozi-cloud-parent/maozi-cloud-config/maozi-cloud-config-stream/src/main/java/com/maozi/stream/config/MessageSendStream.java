package com.maozi.stream.config;

import com.maozi.common.ObjectUtil;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.context.CurrentUserInfoContext;
import com.maozi.common.context.TestFlowContext;
import com.maozi.common.context.VersionContext;
import com.maozi.stream.enums.DelayMessageLevel;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息发送工具
 * <p>
 * 基于 Spring Cloud Stream 的 {@link StreamBridge} 封装消息发送功能，
 * 支持灰度版本路由、消息标签（Tag）和延迟消息级别。
 * 自动从服务发现中获取灰度版本信息，在消息头中设置灰度标识。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class MessageSendStream {

    /** Stream 桥接器，用于动态发送消息 */
    @Resource
    private StreamBridge stream;

    /** 服务发现客户端，用于获取灰度版本信息 */
    @Resource
    private DiscoveryClient discoveryClient;

    /**
     * 发送消息（完整参数）
     * <p>
     * 根据当前服务实例的版本信息，在消息头中设置灰度标识。
     * 若当前版本没有对应的服务实例，则回退到默认版本。
     * </p>
     *
     * @param bindingName 绑定名称
     * @param message 消息对象
     * @param <D> 消息数据类型
     * @return 是否发送成功
     */
    public <D> Boolean sendMessage(String bindingName, Message<D> message){

        // 基于原消息构建 MessageBuilder，用于追加消息头
        MessageBuilder<D> messageBuilder = MessageBuilder.fromMessage(message);

        // 获取当前服务在注册中心的全部存活实例
        List<ServiceInstance> instances = discoveryClient.getInstances(ApplicationEnvironmentContext.SERVICE_NAME);

        // 按实例元数据中的 version（灰度标识）对存活实例分组，用于判断当前版本是否仍有存活实例
        Map<String,List<ServiceInstance>> applicationClients = instances.stream().collect(Collectors.groupingBy((instance)-> instance.getMetadata().get(VersionContext.NACOS_VERSION_KEY)));

        // 当前请求版本号，链路上下文未设置时取默认版本 main
        String version = VersionContext.CONTEXT.get();

        // 当前为非默认版本且该版本已无存活实例时，灰度标识回退为默认版本，保证消息仍可被消费
        if(!VersionContext.APPLICATION_DEFAULT_VERSION.equals(version) && ObjectUtil.isNullEmpty(applicationClients.get(version))){
            version = VersionContext.APPLICATION_DEFAULT_VERSION;
        }

        // Gray 头写入用于灰度路由的版本（可能已回退为默认版本）
        messageBuilder.setHeader("Gray", version);
        // X-Version 头透传原始版本号，供消费端恢复链路上下文
        messageBuilder.setHeader(VersionContext.KEY, VersionContext.CONTEXT.get());
        // 透传测试流量标记与当前用户信息，供消费端恢复链路上下文
        messageBuilder.setHeader(TestFlowContext.KEY, TestFlowContext.CONTEXT.get());
        messageBuilder.setHeader(CurrentUserInfoContext.KEY, CurrentUserInfoContext.CONTEXT.get());

        return stream.send(bindingName,messageBuilder.build());

    }

    /**
     * 发送消息（仅数据）
     *
     * @param bindingName 绑定名称
     * @param data 消息数据
     * @param <D> 数据类型
     * @return 是否发送成功
     */
    public <D> Boolean sendMessage(String bindingName, D data){
        return sendMessage(bindingName,MessageBuilder.withPayload(data).build());
    }

    /**
     * 发送消息（带 Tag 标签）
     *
     * @param bindingName 绑定名称
     * @param data 消息数据
     * @param tags RocketMQ 消息标签
     * @param <D> 数据类型
     * @return 是否发送成功
     */
    public <D> Boolean sendMessage(String bindingName, D data, String tags){

        MessageBuilder<D> messageBuilder = MessageBuilder.withPayload(data);

        // 设置 RocketMQ 消息标签
        messageBuilder.setHeader(MessageConst.PROPERTY_TAGS, tags);

        return sendMessage(bindingName,messageBuilder.build());

    }

    /**
     * 发送延迟消息
     *
     * @param bindingName 绑定名称
     * @param data 消息数据
     * @param level 延迟消息级别
     * @param <D> 数据类型
     * @return 是否发送成功
     */
    public <D> Boolean sendMessage(String bindingName, D data, DelayMessageLevel level){

        MessageBuilder<D> messageBuilder = MessageBuilder.withPayload(data);

        // RocketMQ 延迟级别从 1 开始计数，枚举值从 0 开始，发送时需 +1 转换
        messageBuilder.setHeader(MessageConst.PROPERTY_DELAY_TIME_LEVEL, level.getValue() + 1);

        return sendMessage(bindingName,messageBuilder.build());

    }

    /**
     * 发送延迟消息（带 Tag 标签）
     *
     * @param bindingName 绑定名称
     * @param data 消息数据
     * @param tags RocketMQ 消息标签
     * @param level 延迟消息级别
     * @param <D> 数据类型
     * @return 是否发送成功
     */
    public <D> Boolean sendMessage(String bindingName, D data,String tags,DelayMessageLevel level){

        MessageBuilder<D> messageBuilder = MessageBuilder.withPayload(data);

        // 设置 RocketMQ 消息标签
        messageBuilder.setHeader(MessageConst.PROPERTY_TAGS, tags);

        // RocketMQ 延迟级别从 1 开始计数，枚举值从 0 开始，发送时需 +1 转换
        messageBuilder.setHeader(MessageConst.PROPERTY_DELAY_TIME_LEVEL, level.getValue() + 1);

        return sendMessage(bindingName,messageBuilder.build());

    }

}
