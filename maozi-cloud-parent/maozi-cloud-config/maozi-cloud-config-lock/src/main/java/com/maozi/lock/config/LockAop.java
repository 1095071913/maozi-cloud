package com.maozi.lock.config;

import com.maozi.common.CollectionUtil;
import com.maozi.lock.annotation.LockKey;
import com.maozi.lock.lock.LockType;
import com.maozi.lock.properties.LockRedissonProperties;
import com.maozi.redis.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * 分布式锁切面处理器
 * <p>
 * 拦截带有 {@link com.maozi.lock.annotation.Lock} 注解的方法，
 * 根据注解配置自动获取和释放分布式锁。支持 SpEL 表达式解析锁键、
 * 多种锁类型（可重入锁、公平锁、读写锁）和超时策略。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Aspect
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 2 )
public class LockAop {

    /** SpEL 表达式解析器 */
    private final ExpressionParser parser = new SpelExpressionParser();

    /** 参数名发现器 */
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /** 锁配置属性 */
    @Resource
    private LockRedissonProperties lockRedissonProperties;

    /**
     * 环绕通知，执行加锁、业务逻辑和释放锁
     *
     * @param joinPoint AOP 连接点
     * @param annotation 锁注解实例
     * @return 业务方法执行结果
     * @throws Throwable 业务方法或锁操作抛出的异常
     */
    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, com.maozi.lock.annotation.Lock annotation) throws Throwable {

        // 获取方法签名信息，用于后续提取方法名和参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 获取注解中配置的锁类型（可重入锁、公平锁、读锁、写锁）
        LockType type = annotation.type();

        // 解析业务键名称（来自 SpEL 表达式或 @LockKey 注解的参数）
        String businessKeyName = getKeyName(joinPoint,annotation);

        // 确定锁名称：如果注解指定了名称则使用指定名称，否则使用 "类名.方法名" 作为默认锁名称
        String lockName = org.apache.commons.lang3.StringUtils.isNotBlank(annotation.name()) ? annotation.name() : signature.getDeclaringTypeName()+ "." +signature.getMethod().getName();

        // 拼接锁键名：maozi-cloud:服务名:lock:锁名称_业务键名（REDIS_KEY_PREFIX 尾部自带冒号，业务键以 "_" 为前缀连接）
        lockName = RedisUtil.REDIS_KEY_PREFIX + "lock:" + lockName + businessKeyName;

        // 获取等待时间：如果注解未指定（Long.MIN_VALUE），则使用全局配置的默认值
        long waitTime = annotation.waitTime() == Long.MIN_VALUE ? lockRedissonProperties.getWaitTime() : annotation.waitTime();

        // 获取持有时间：如果注解未指定（Long.MIN_VALUE），则使用全局配置的默认值
        long leaseTime = annotation.leaseTime() == Long.MIN_VALUE ? lockRedissonProperties.getLeaseTime() : annotation.leaseTime();

        // 执行加锁操作，如果加锁失败则根据配置的策略进行处理
        type.lock(lockName,waitTime,leaseTime,annotation.lockTimeoutStrategy());

        // 执行目标业务方法，确保在 finally 块中释放锁
        try {return joinPoint.proceed();} finally {
            // 无论业务方法执行成功或失败，都执行解锁操作，并根据策略处理解锁超时
            type.unlock(lockName,annotation.releaseTimeoutStrategy());
        }

    }

    /**
     * 获取完整的锁键名称
     *
     * @param joinPoint AOP 连接点
     * @param lock 锁注解实例
     * @return 拼接后的锁键名称
     * @throws Exception 反射操作异常
     */
    public String getKeyName(JoinPoint joinPoint, com.maozi.lock.annotation.Lock lock) throws Exception {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Method method = signature.getMethod();

        // 如果方法声明在接口上，直接使用接口方法；否则从目标类获取实际方法，确保获取到正确的参数注解
        method = method.getDeclaringClass().isInterface() ? signature.getMethod() : joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(),method.getParameterTypes());

        List<String> keyList = CollectionUtil.newArrayList();

        // 收集通过 SpEL 表达式定义的锁键（来自 @Lock 注解的 keys 属性）
        keyList.addAll(getSpelDefinitionKey(joinPoint.getTarget(),lock.keys(), method, joinPoint.getArgs()) );

        // 收集通过 @LockKey 注解标记的参数值作为锁键
        keyList.addAll(getParameterKey(method.getParameters(), joinPoint.getArgs()) );

        // 每个业务键前拼接 "_"，键之间不加分隔符，形如 "_key1_key2"（分隔符为空、前缀 "_"、后缀为空）
        return StringUtils.collectionToDelimitedString(keyList,"","_","");

    }

    /**
     * 解析 SpEL 表达式定义的锁键
     *
     * @param rootObject SpEL 根对象（目标类实例）
     * @param definitionKeys SpEL 表达式数组
     * @param method 目标方法
     * @param parameterValues 方法参数值
     * @return 解析后的锁键列表
     */
    private List<String> getSpelDefinitionKey(Object rootObject, String[] definitionKeys, Method method, Object[] parameterValues) {

        List<String> definitionKeyList = CollectionUtil.newArrayList();

        for (String definitionKey : definitionKeys) {

            if (!ObjectUtils.isEmpty(definitionKey)) {

                // 创建基于方法的 SpEL 求值上下文，将方法参数绑定到上下文中以便 SpEL 表达式引用
                EvaluationContext context = new MethodBasedEvaluationContext(rootObject, method, parameterValues, nameDiscoverer);

                // 解析 SpEL 表达式并获取实际值，例如 "#orderId" 会被解析为实际的订单 ID
                Object objKey = parser.parseExpression(definitionKey).getValue(context);

                definitionKeyList.add(ObjectUtils.nullSafeToString(objKey));

            }

        }

        return definitionKeyList;

    }

    /**
     * 获取带有 {@link LockKey} 注解的参数值作为锁键
     *
     * @param parameters 方法参数数组
     * @param parameterValues 参数值数组
     * @return 锁键列表
     */
    private List<String> getParameterKey(Parameter[] parameters, Object[] parameterValues) {

        List<String> parameterKey = CollectionUtil.newArrayList();

        for (int i = 0; i < parameters.length; i++) {

            // 检查参数上是否有 @LockKey 注解
            if (parameters[i].getAnnotation(LockKey.class) != null) {

                LockKey keyAnnotation = parameters[i].getAnnotation(LockKey.class);

                if (keyAnnotation.value().isEmpty()) {

                    // 如果 @LockKey 未指定 SpEL 表达式，直接使用参数的 toString 值作为锁键
                    Object parameterValue = parameterValues[i];

                    parameterKey.add(ObjectUtils.nullSafeToString(parameterValue));

                } else {

                    // 如果 @LockKey 指定了 SpEL 表达式，则以参数对象为根对象解析表达式
                    // 例如参数为 User 对象，表达式为 "id"，则提取 user.getId() 的值
                    StandardEvaluationContext context = new StandardEvaluationContext(parameterValues[i]);

                    Object key = parser.parseExpression(keyAnnotation.value()).getValue(context);

                    parameterKey.add(ObjectUtils.nullSafeToString(key));

                }

            }

        }

        return parameterKey;

    }

}
