package com.maozi.service.config;

import com.maozi.service.api.annotation.RemoteResource;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link RemoteResource} 字段注入处理器（微服务模式）。
 * <p>
 * {@link RemoteResource} 未组合 {@code @DubboReference}，无法借助 Dubbo 元注解方式完成注入；
 * 本处理器扫描其标注的字段，通过 Dubbo 编程式 API
 * （{@link ReferenceConfig} + {@link DubboBootstrap}）
 * 创建远程引用代理并反射写入（代理按接口类型缓存），
 * 代理写入前经 Spring 自动代理创建器包装，消费端调用可被
 * {@code DubboInvokeErrorAop} 等切面拦截，
 * 同时按字段名将代理注册为 Spring 单例，
 * 供 {@code SpringUtil.getBean(字段名)} 等按名查找使用（如 {@code BaseServiceImpl} 的
 * {@code @QueryMapping(serviceName=...)} 关联数据解析机制）。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/6/25
 */
@Component
public class RemoteResourceBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {

    /** 按接口类型缓存的远程引用代理 */
    private final ConcurrentMap<Class<?>, Object> referenceCache = new ConcurrentHashMap<>();

    /** 已注册的单例名称，避免重复注册 */
    private final Set<String> registeredNames = ConcurrentHashMap.newKeySet();

    /** Spring Bean 工厂，用于按字段名注册引用代理单例 */
    private ConfigurableListableBeanFactory beanFactory;

    /**
     * 保存 Bean 工厂供单例注册使用
     *
     * @param beanFactory Spring Bean 工厂
     */
    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    /**
     * 在属性注入阶段为 {@link RemoteResource} 标注的字段写入 Dubbo 引用代理
     * <p>
     * 跳过静态字段；代理按接口类型缓存，
     * 并按字段名注册为 Spring 单例后反射写入目标字段。
     * </p>
     *
     * @param pvs 原始属性值
     * @param bean 当前正在创建的 bean 实例
     * @param beanName bean 名称
     * @return 原始属性值（不修改常规属性注入流程）
     */
    @Override
    public PropertyValues postProcessProperties(@NotNull PropertyValues pvs, Object bean,
        @NotNull String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            if (field.isAnnotationPresent(RemoteResource.class) && !Modifier.isStatic(field.getModifiers())) {
                Object reference = referenceCache.computeIfAbsent(field.getType(), this::createReference);
                registerSingletonIfAbsent(field.getName(), reference);
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, bean, reference);
            }
        });
        return pvs;
    }

    /**
     * 按名称将引用代理注册为 Spring 单例（若尚未注册）。
     * <p>
     * 使 {@code SpringUtil.getBean(字段名)} 等按名查找可用，兼容项目中
     * {@code BaseServiceImpl} 通过 {@code @QueryMapping(serviceName=...)} 解析关联数据的机制。
     * </p>
     *
     * @param name     单例名称（取自字段名）
     * @param singleton 引用代理实例
     */
    private void registerSingletonIfAbsent(String name, Object singleton) {
        if (beanFactory != null && !beanFactory.containsSingleton(name) && registeredNames.add(name)) {
            beanFactory.registerSingleton(name, singleton);
        }
    }

    /**
     * 为指定接口创建 Dubbo 远程引用代理，并补充 Spring AOP 包装。
     * <p>
     * 调用 {@link ReferenceConfig#get(boolean)} 会自动触发 Dubbo 模块的初始化与启动，
     * 无需关心启动时序；代理通过 {@link DubboBootstrap#reference(ReferenceConfig)} 注册，
     * 由 Dubbo 统一管理生命周期。
     * Dubbo 生成的引用代理（类名形如 {@code xxx.api.rpc.XxxServiceDubboProxy0}，
     * 位于接口同包下）不是容器创建的 Bean，不经过自动代理后处理器；
     * 此处将其交给 {@link AbstractAutoProxyCreator} 补做一次标准的初始化后处理，
     * 使 {@code DubboInvokeErrorAop} 等匹配 {@code api.rpc} 包的切面在消费端调用时生效。
     * </p>
     *
     * @param interfaceClass 远程服务接口类型
     * @return 经 Spring AOP 包装后的 Dubbo 引用代理
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object createReference(Class<?> interfaceClass) {
        ReferenceConfig reference = new ReferenceConfig();
        reference.setInterface(interfaceClass);
        DubboBootstrap.getInstance().reference(reference);
        return applySpringAop(interfaceClass, reference.get(true));
    }

    /**
     * 将 Dubbo 引用代理交给 Spring 自动代理创建器补做初始化后处理。
     * <p>
     * {@code ReferenceConfig#get} 产生的代理绕过了容器生命周期，
     * 直接调用 {@link AbstractAutoProxyCreator#postProcessAfterInitialization}
     * 等价于让该代理走一遍容器标准后置处理：切点匹配（如
     * {@code DubboInvokeErrorAop} 的 {@code api.rpc} 包表达式，Dubbo 代理类
     * 恰好生成在接口同包下故可命中）的切面会被织入；
     * 无任何切面匹配时原样返回代理，行为不变。
     * </p>
     *
     * @param interfaceClass 远程服务接口类型，
     *                       其全限定名用作代理缓存键（保证接口维度唯一）
     * @param dubboReference Dubbo 原始引用代理
     * @return Spring AOP 包装后的代理；无切面匹配时为原始代理
     */
    private Object applySpringAop(Class<?> interfaceClass, Object dubboReference) {
        AbstractAutoProxyCreator autoProxyCreator = beanFactory.getBean(AbstractAutoProxyCreator.class);
        return autoProxyCreator.postProcessAfterInitialization(dubboReference, interfaceClass.getName());
    }

}
