package com.maozi.lock.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式锁配置属性
 * <p>
 * 从配置文件中读取 {@code lock.redisson} 前缀下的属性，
 * 用于配置 Redisson 客户端连接信息和锁的默认超时参数。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lock.redisson")
public class LockRedissonProperties {

    /** Redis 单节点地址 */
    private String address;

    /** Redis 连接密码 */
    private String password;

    /** Redis 数据库编号，默认为 0 */
    private int database = 0;

    /** Redis 集群节点地址列表 */
    private String[] nodeAddresses;

    /** 获取锁的默认等待时间（秒），默认 60 秒 */
    private long waitTime = 60;

    /** 锁的默认持有时间（秒），超时后自动释放，默认 60 秒 */
    private long leaseTime = 60;

}
