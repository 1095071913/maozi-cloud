package com.maozi.lock.config;

import com.maozi.common.ObjectUtil;
import com.maozi.lock.properties.LockRedissonProperties;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.Resource;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置类
 * <p>
 * 根据配置自动创建 Redisson 客户端实例，支持单节点和集群两种模式。
 * 使用 NIO 事件循环组提高网络通信性能。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class RedissonConfig {

    /** 分布式锁配置属性 */
    @Resource
    private LockRedissonProperties lockRedissonProperties;

    /**
     * 创建 Redisson 客户端 Bean
     * <p>
     * 当配置了集群节点地址时使用集群模式，否则使用单节点模式。
     * </p>
     *
     * @return RedissonClient 实例
     * @throws Exception 配置异常
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedissonClient redisson() throws Exception {

        Config config = new Config();

        if(ObjectUtil.isNotNullEmpty(lockRedissonProperties.getNodeAddresses())){
            // 集群模式：配置多个 Redis 节点地址和密码
            config.useClusterServers()
                    .setPassword(lockRedissonProperties.getPassword())
                    .addNodeAddress(lockRedissonProperties.getNodeAddresses());
        }else {
            // 单节点模式：配置单个 Redis 地址、数据库编号和密码
            config.useSingleServer()
                    .setAddress(lockRedissonProperties.getAddress())
                    .setDatabase(lockRedissonProperties.getDatabase())
                    .setPassword(lockRedissonProperties.getPassword());
        }

        // 使用 Netty NIO 事件循环组，提高 Redisson 与 Redis 之间的网络通信性能
        config.setEventLoopGroup(new NioEventLoopGroup());

        return Redisson.create(config);

    }

}
