package com.maozi.oauth.token.config.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Spring Security 认证用户详情
 * <p>
 * 继承 Spring Security 的 {@link User}，在标准的用户名、密码、权限信息之外
 * 扩展了 attributes 附加属性（如用户ID），用于在颁发令牌时随 claims 一并写入；
 * 同时支持 Jackson 序列化/反序列化，以配合授权信息存入 Redis。
 * </p>
 * <p>
 * {@code @JsonTypeInfo(Id.CLASS)} 多态反序列化安全性说明：本类的 JSON 报文
 * 仅由 {@code RedisOAuth2AuthorizationServiceImpl} 写入 Redis 并从 Redis 读回
 * （注册了 Spring Security 官方 SecurityJackson2Modules），报文内容完全由
 * 服务端生成；外部用户输入（令牌字符串）仅用作 Redis 键，不会成为被反序列化
 * 的 JSON 报文，攻击者无法通过 {@code @class} 属性驱动任意类实例化，配置安全。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/8/13 21:25
 */
@SuppressWarnings("java:S6769")
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityUserDetails extends User implements Serializable {
    /** 序列化标识 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户附加属性（如用户ID），颁发令牌时会合并进令牌的claims */
    private Map<String, Object> attributes;

    /**
     * 业务构造函数
     * <p>
     * 使用该构造函数创建的用户默认为启用、未过期、凭证未过期、未锁定状态。
     * </p>
     *
     * @param username    用户名
     * @param password    密码（加密后的密文）
     * @param authorities 权限列表
     * @param attributes  用户附加属性（如用户ID）
     */
    public SecurityUserDetails(String username, String password,
            Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes) {
        super(username, password, authorities);
        this.attributes = attributes;
    }

    /**
     * Jackson 反序列化构造函数
     * <p>
     * 按JSON属性逐一还原用户状态、权限列表及附加属性，
     * 用于从Redis中反序列化用户信息。
     * </p>
     *
     * @param username              用户名
     * @param password              密码（加密后的密文）
     * @param enabled               是否启用
     * @param accountNonExpired     账号是否未过期
     * @param credentialsNonExpired 凭证是否未过期
     * @param accountNonLocked      账号是否未锁定
     * @param authorities           权限列表
     * @param attributes            用户附加属性（如用户ID）
     */
    @JsonCreator
    public SecurityUserDetails(@JsonProperty("username") String username, @JsonProperty("password") String password,
                               @JsonProperty("enabled") boolean enabled,
                               @JsonProperty("accountNonExpired") boolean accountNonExpired,
                               @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
                               @JsonProperty("accountNonLocked") boolean accountNonLocked,
                               @JsonProperty("authorities") Collection<SimpleGrantedAuthority> authorities,
                               @JsonProperty("attributes") Map<String, Object> attributes) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.attributes = attributes;
    }

}
