package com.maozi.oauth.client.api.impl;

import cn.hutool.extra.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.maozi.base.param.PageParam;
import com.maozi.base.result.PageResult;
import com.maozi.common.ObjectUtil;
import com.maozi.common.result.error.exception.BusinessResultException;
import com.maozi.oauth.client.api.ClientService;
import com.maozi.oauth.client.domain.ClientDO;
import com.maozi.oauth.client.mapper.ClientMapper;
import com.maozi.oauth.client.param.ClientListParam;
import com.maozi.oauth.client.param.ClientSaveUpdateParam;
import com.maozi.oauth.client.result.ClientInfoResult;
import com.maozi.oauth.client.result.ClientListResult;
import com.maozi.service.api.impl.BaseServiceImpl;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth2客户端服务实现类。
 * <p>
 * 提供客户端的增删改查及OAuth2客户端设置、令牌设置的构建，
 * 涵盖客户端密钥加密与令牌有效期配置等核心逻辑。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Service
public class ClientServiceImpl extends BaseServiceImpl<ClientMapper, ClientDO> implements ClientService {

    /** 资源名称标识，用于异常信息提示 */
    public static final String RESOURCE_NAME = "客户端";

    /** 授权码存活时间（秒）：5 分钟 */
    protected static final long AUTH_CODE_TTL_SECONDS = 300;

    /** 设备码存活时间（秒）：5 分钟 */
    protected static final long DEVICE_CODE_TTL_SECONDS = 300;

    /** 访问令牌（Access Token）默认有效期：2小时 */
    protected static final long DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS = 60 * 60 * 2L;

    /** 刷新令牌（Refresh Token）默认有效期：7天 */
    protected static final long DEFAULT_REFRESH_TOKEN_VALIDITY_SECONDS = 60 * 60 * 24 * 7L;

    /** OAuth2专用的JSON序列化/反序列化对象，注册了Spring Security OAuth2的Jackson模块 */
    public static final ObjectMapper OAUTH_OBJECT_MAPPER;

    /** 密码编码器，用于对客户端密钥进行加密存储 */
    protected static final PasswordEncoder PASSWORD_ENCODER;

    // 初始化OAuth2专用ObjectMapper与委托式密码编码器
    static {

        OAUTH_OBJECT_MAPPER = new ObjectMapper();
        OAUTH_OBJECT_MAPPER.registerModule(new OAuth2AuthorizationServerJackson2Module());
        OAUTH_OBJECT_MAPPER.registerModules(SecurityJackson2Modules.getModules(RegisteredClientRepository.class.getClassLoader()));

        PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    }


    /**
     * 获取资源名称。
     */
    @Override
    public String getResourceName() {
        return RESOURCE_NAME;
    }

    /**
     * 客户端新增或更新的统一处理。
     * <p>
     * 新增时自动生成clientId并填充默认令牌有效期与客户端设置，更新时清空clientId防止被修改；
     * 密钥非空时加密存储，两种令牌有效期均已提供时构建令牌设置，否则不重建。
     * </p>
     *
     * @param id    客户端主键ID，新增时为null
     * @param param 客户端保存/更新参数
     * @return 保存后的客户端主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long adminSaveUpdate(Long id, ClientSaveUpdateParam param) {

        if(ObjectUtil.isNullEmpty(id)) {

            //新增：自动生成客户端标识（clientId）
            param.setClientId(String.valueOf(IdWorker.getId()));

            if(ObjectUtil.isNullEmpty(param.getAccessTokenValiditySeconds())){
                param.setAccessTokenValiditySeconds(DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS);
            }

            if(ObjectUtil.isNullEmpty(param.getRefreshTokenValiditySeconds())){
                param.setRefreshTokenValiditySeconds(DEFAULT_REFRESH_TOKEN_VALIDITY_SECONDS);
            }

            //不需要确认授权
            ClientSettings clientSettings = ClientSettings.builder().requireAuthorizationConsent(Boolean.FALSE).build();
            param.setClientSettings(clientSettings.getSettings());

        }else{
            //更新：清空clientId，防止客户端标识被修改
            param.setClientId(null);
        }
        
        //客户端密钥非空时加密存储（更新未传密钥则保持原值）
        if(ObjectUtil.isNotNullEmpty(param.getClientSecret())) {
            param.setClientSecret(PASSWORD_ENCODER.encode(param.getClientSecret()));
        }

        Long accessTokenValiditySeconds = param.getAccessTokenValiditySeconds();
        Long refreshTokenValiditySeconds = param.getRefreshTokenValiditySeconds();
        if(ObjectUtil.isNotNullEmpty(accessTokenValiditySeconds)
                && ObjectUtil.isNotNullEmpty(refreshTokenValiditySeconds)) {

            TokenSettings.Builder builder = TokenSettings.builder();

            //不透明令牌（REFERENCE），令牌值不携带信息，需通过内省端点校验
            builder.accessTokenFormat(OAuth2TokenFormat.REFERENCE);
            builder.authorizationCodeTimeToLive(Duration.ofSeconds(AUTH_CODE_TTL_SECONDS));
            builder.deviceCodeTimeToLive(Duration.ofSeconds(DEVICE_CODE_TTL_SECONDS));
            // 不重用 Refresh Token（每次刷新时签发新的 Refresh Token）
            builder.reuseRefreshTokens(Boolean.FALSE);


            if(ObjectUtil.isNotNullEmpty(accessTokenValiditySeconds)){
                builder.accessTokenTimeToLive(Duration.ofSeconds(accessTokenValiditySeconds));
            }

            if(ObjectUtil.isNotNullEmpty(refreshTokenValiditySeconds)) {
                builder.refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenValiditySeconds));
            }

            param.setTokenSettings(builder.build().getSettings());

        }
        
        return super.saveUpdate(id, param);
        
    }

    /**
     * 根据主键ID获取客户端详细信息。
     * <p>
     * 查询客户端基础信息后，从TokenSettings中提取Access Token和Refresh Token的有效期（秒），
     * 封装到ClientInfoResult中返回。
     * </p>
     *
     * @param id 客户端主键ID
     * @return 客户端详细信息结果对象
     * @throws BusinessResultException 客户端不存在时抛出 DATA_NOT_EXIST_ERROR，
     * 禁用时抛出 FORBIDDEN_ERROR
     */
    public ClientInfoResult adminGet(Long id) {

        ClientDO domain = super.getAvailableById(id,
                ClientDO::getClientId, ClientDO::getName, ClientDO::getAuthorizationGrantTypes,
                ClientDO::getTokenSettings, ClientDO::getRemark, ClientDO::getStatus);

        //防御性判空（getAvailableById在数据不存在时已抛出异常，正常不会进入该分支）
        if(ObjectUtil.isNullEmpty(domain)){
            return null;
        }

        ClientInfoResult response = CglibUtil.copy(domain, ClientInfoResult.class);

        //从tokenSettings中解析Access/Refresh Token有效期（秒）
        TokenSettings tokenSettings = TokenSettings.withSettings(domain.getTokenSettings()).build();
        response.setAccessTokenValiditySeconds(tokenSettings.getAccessTokenTimeToLive().getSeconds());
        response.setRefreshTokenValiditySeconds(tokenSettings.getRefreshTokenTimeToLive().getSeconds());

        return response;

    }

    /**
     * 分页查询客户端列表。
     * <p>
     * 根据查询参数构建查询条件，执行关联分页查询，
     * 并将每条记录的TokenSettings解析为Access Token和Refresh Token的有效期（秒）。
     * </p>
     *
     * @param pageParam 分页查询参数，包含查询条件和分页信息
     * @return 分页结果，包含客户端列表数据
     */
    public PageResult<ClientListResult> adminList(PageParam<ClientListParam> pageParam){

        Class<ClientListResult> responseClass = ClientListResult.class;
        MPJLambdaWrapper<ClientDO> wrapper = super.buildQueryWrapper(pageParam.getData(), responseClass);
        Page<ClientListResult> page = super.selectJoinListPage(super.convertPage(pageParam), responseClass,wrapper);

        page.getRecords().forEach(item -> {

            //从tokenSettings中解析Access/Refresh Token有效期（秒）
            TokenSettings tokenSettings = TokenSettings.withSettings(item.getTokenSettings()).build();
            item.setAccessTokenValiditySeconds(tokenSettings.getAccessTokenTimeToLive().getSeconds());
            item.setRefreshTokenValiditySeconds(tokenSettings.getRefreshTokenTimeToLive().getSeconds());

        });

        return super.convertPageResult(page);
    }

    /**
     * 根据客户端主键ID获取客户端标识（clientId）。
     *
     * @param id 客户端主键ID
     * @return 客户端标识字符串
     * @throws BusinessResultException 客户端不存在时抛出 DATA_NOT_EXIST_ERROR
     */
    @Override
    public String getClientId(Long id) {
        return super.getByIdThrowError(id, ClientDO::getClientId).getClientId();
    }

    /**
     * 根据客户端主键ID集合批量获取客户端ID与clientId的映射关系。
     *
     * @param ids 客户端主键ID集合
     * @return 键为主键ID、值为clientId的映射Map
     */
    @Override
    public Map<Long,String> getClientIdsMap(Collection<Long> ids) {

        LambdaQueryWrapper<ClientDO> wrapper = Wrappers.lambdaQuery();

        wrapper.select(ClientDO::getId,ClientDO::getClientId);
        
        wrapper.in(ClientDO::getId, ids);

        return super.list(wrapper).stream().collect(Collectors.toMap(ClientDO::getId, ClientDO::getClientId, (v1, v2) -> v1));
        
    }

}