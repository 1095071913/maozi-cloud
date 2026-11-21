package com.maozi.mvc.config.enums;

import cn.hutool.core.util.ClassUtil;
import com.maozi.common.CollectionUtil;
import com.maozi.common.constant.ApplicationNameConstant;
import com.maozi.common.constant.SymbolConstant;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.enums.BaseEnum;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举配置
 * <p>
 * 启动时扫描基础枚举包和当前项目枚举包下所有实现了 {@link BaseEnum} 接口的枚举类，
 * 将枚举类名（首字母小写）作为键、枚举常量列表作为值，注册到全局枚举映射表中，
 * 供前端下拉选择等场景使用。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Data
@Component
public class EnumConfig {

    /**
     * 全局枚举映射表，键为枚举类名（首字母小写），值为枚举常量列表
     * （仅类内部可写，外部通过 {@link #getEnums()} 只读视图访问；
     * 启动线程写入后供任意请求线程并发读，使用 ConcurrentHashMap 保证跨线程安全发布）
     */
    private static final Map<String, List<BaseEnum>> ENUMS = new ConcurrentHashMap<>();

    /**
     * 启动时初始化全局枚举映射表：先加载基础枚举包，再按当前服务加载业务枚举包
     */
    @PostConstruct
    public void init() {

        // 先加载基础枚举包下的所有枚举类
        initEnum(ApplicationEnvironmentContext.PACKAGE_PREFIX + ".base.enums");

        // 再加载业务枚举：聚合服务（maozi-cloud-all-service）时扫描整个基础包，
        // 普通服务仅扫描当前项目缩写对应的子包
        String subPath = ApplicationNameConstant.MAOZI_CLOUD_ALL_SERVICE
                .equals(ApplicationEnvironmentContext.serviceName)
                ? SymbolConstant.EMPTY
                : SymbolConstant.DOT + ApplicationEnvironmentContext.applicationProjectAbbreviation;
        initEnum(ApplicationEnvironmentContext.PACKAGE_PREFIX + subPath);

    }

    /**
     * 扫描指定包下的枚举类并注册到映射表
     *
     * @param packageName 需要扫描的包路径
     */
    private void initEnum(String packageName) {

        // 扫描指定包下的所有类
        ClassUtil.scanPackage(packageName)

            .forEach(item -> {

                // 仅处理枚举类且实现了 BaseEnum 接口的类
                if (item.isEnum() && item.getEnumConstants() instanceof BaseEnum[] enumConstants) {

                    // 将类名首字母转为小写作为映射键（如 UserType -> userType），
                    // 使用 Character.toLowerCase 避免首字符非大写字母时产生错误字符
                    String simpleName = item.getSimpleName();
                    String enumKey = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);

                    // 以不可变列表注册，防止外部通过引用修改枚举常量列表
                    List<BaseEnum> constants = Collections.unmodifiableList(CollectionUtil.newArrayList(enumConstants));

                    // 将枚举常量列表注册到全局映射表
                    ENUMS.put(enumKey, constants);

                }

            });

    }

    /**
     * 获取全局枚举映射表的只读视图
     *
     * @return 不可变的枚举映射表视图（内部枚举常量列表同样不可变）
     */
    public static Map<String, List<BaseEnum>> getEnums() {
        return Collections.unmodifiableMap(ENUMS);
    }

}
