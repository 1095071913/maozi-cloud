package com.maozi.common.context;

import com.maozi.common.ObjectUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 版本号上下文
 * <p>
 * 存储当前线程的版本号（用于灰度路由），并提供默认版本、
 * Nacos 元数据版本键等灰度路由相关常量。
 * </p>
 *
 * @author maozi
 * @date 2026/11/13
 */
public class VersionContext extends AbstractRequestContextHolder<String> {

    /** 版本号请求头名称 */
    public static final String KEY = "X-Version";

    /** Nacos 元数据中的版本键 */
    public static final String NACOS_VERSION_KEY = "version";

    /** 默认应用版本（主版本） */
    public static final String APPLICATION_DEFAULT_VERSION = "main";

    /** 单例实例 */
    public static final VersionContext CONTEXT = new VersionContext();

    /** 私有构造器，防止外部实例化 */
    private VersionContext() {}

    /**
     * 获取当前线程的版本号
     *
     * @return 版本号，未设置时返回默认版本
     */
    @Override
    public String get(){
        return getVersionOrDefault(super.get());
    }

    /**
     * 设置当前线程的版本号
     *
     * @param version 版本号，为空时存入默认版本
     */
    @Override
    public void set(String version){
        super.set(getVersionOrDefault(version));
    }

    /**
     * 获取版本号，为空时返回默认版本
     *
     * @param version 版本号（取其字符串形式判空）
     * @return 非空版本号或默认版本
     */
    private String getVersionOrDefault(Object version) {
        boolean hasVersion = ObjectUtil.isNotNullEmpty(version) && StringUtils.isNotBlank(version.toString());
        return hasVersion ? version.toString() : APPLICATION_DEFAULT_VERSION;
    }

}
