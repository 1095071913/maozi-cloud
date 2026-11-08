package com.maozi.mvc.config.enums;

import cn.hutool.core.util.ClassUtil;
import com.maozi.common.CollectionUtil;
import com.maozi.common.constant.ApplicationNameConstant;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.enums.BaseEnum;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    /** 全局枚举映射表，键为枚举类名（首字母小写），值为枚举常量列表 */
    public static final Map<String, List<BaseEnum>> ENUMS = CollectionUtil.newHashMap();

    @PostConstruct
    public void init() {

        // 先加载基础枚举包下的所有枚举类
        initEnum(ApplicationEnvironmentContext.PACKAGE_PREFIX + ".base.enums");

        // 再加载业务枚举：聚合服务（maozi-cloud-all-service）时扫描整个基础包，普通服务仅扫描当前项目缩写对应的子包
        String subPath = ApplicationNameConstant.MAOZI_CLOUD_ALL_SERVICE.equals(ApplicationEnvironmentContext.SERVICE_NAME) ? "" : "." + ApplicationEnvironmentContext.APPLICATION_PROJECT_ABBREVIATION;
        initEnum(ApplicationEnvironmentContext.PACKAGE_PREFIX + subPath);

    }

    /**
     * 扫描指定包下的枚举类并注册到映射表
     *
     * @param packageName 需要扫描的包路径
     */
    private void initEnum(String packageName){

        // 扫描指定包下的所有类
        ClassUtil.scanPackage(packageName)

            .forEach(item -> {

                // 仅处理枚举类且实现了 BaseEnum 接口的类
                if (item.isEnum() && item.getEnumConstants() instanceof BaseEnum[] enumConstants) {

                    // 将类名首字母转为小写作为映射键（如 UserType -> userType）
                    char[] charArray = item.getSimpleName().toCharArray();

                    charArray[0] += 32; // ASCII 码加 32，即大写字母转小写

                    // 将枚举常量列表注册到全局映射表
                    ENUMS.put(new String(charArray), CollectionUtil.newArrayList(enumConstants));

                }

            });

    }

}
