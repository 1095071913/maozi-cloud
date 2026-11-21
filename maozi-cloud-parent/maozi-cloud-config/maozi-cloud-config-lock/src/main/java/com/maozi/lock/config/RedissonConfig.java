package com.maozi.lock.config;

import com.maozi.common.ObjectUtil;
import com.maozi.lock.properties.LockRedissonProperties;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
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

    /** Redis 逻辑库默认编号（配置缺失时的兜底值，与 Redisson 官方默认一致） */
    private static final int DEFAULT_DATABASE = 0;

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
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedissonClient redisson() {

        Config config = new Config();

        String password = lockRedissonProperties.getPassword();
        if (ObjectUtil.isNotNull(lockRedissonProperties.getNodeAddresses())){
            // 集群模式：配置多个 Redis 节点地址和密码
            config.useClusterServers()
                    .setPassword(password)
                    .addNodeAddress(lockRedissonProperties.getNodeAddresses());
        }else {
            Integer database = lockRedissonProperties.getDatabase();
            // 单节点模式：配置单个 Redis 地址、数据库编号和密码
            // （逻辑库未配置时默认 0，避免自动拆箱 NPE）
            config.useSingleServer()
                    .setAddress(lockRedissonProperties.getAddress())
                    .setDatabase(database == null ? DEFAULT_DATABASE : database)
                    .setPassword(password);
        }

        // 使用 Netty NIO 事件循环组，提高 Redisson 与 Redis 之间的网络通信性能；
        // 通过自定义线程工厂指定有意义的线程名前缀，便于日志回溯定位归属
        config.setEventLoopGroup(new NioEventLoopGroup(
                Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory("redisson-netty", true)));

        return Redisson.create(config);

    }

}
