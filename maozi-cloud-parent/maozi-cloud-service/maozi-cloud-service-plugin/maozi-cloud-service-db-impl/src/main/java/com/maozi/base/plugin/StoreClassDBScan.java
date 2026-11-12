package com.maozi.base.plugin;

import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.maozi.base.enums.StoreClassType;
import com.maozi.common.CollectionUtil;
import com.maozi.common.constant.ApplicationNameConstant;
import com.maozi.common.context.ApplicationEnvironmentContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据库存储类扫描器
 * <p>
 * 应用启动时扫描项目包下所有标注了 {@link TableName} 注解的类，
 * 将表名到类类型的映射注册到 {@link StoreClass} 中，
 * 供运行时动态映射使用。
 * </p>
 *
 * @author maozi
 * @date 2026/11/08
 */
@Component
public class StoreClassDBScan implements ApplicationRunner {

    /**
     * 应用启动时执行数据库实体类扫描
     *
     * @param args 应用启动参数
     * @throws Exception 扫描异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        Map<String, Class<?>> storeClassDBMap = CollectionUtil.newHashMap();

        // 聚合服务（maozi-cloud-all-service）扫描全包，普通服务仅扫描自身项目子包
        String subPath = ApplicationNameConstant.MAOZI_CLOUD_ALL_SERVICE.equals(ApplicationEnvironmentContext.SERVICE_NAME) ? "" : "." + ApplicationEnvironmentContext.APPLICATION_PROJECT_ABBREVIATION;
        ClassUtil.scanPackage(ApplicationEnvironmentContext.PACKAGE_PREFIX + subPath).forEach(clazz ->{

            // 仅收集标注 @TableName 的实体类，建立表名到类类型的映射
            if(clazz.isAnnotationPresent(TableName.class)){

                TableName annotation = clazz.getAnnotation(TableName.class);

                storeClassDBMap.put(annotation.value(),clazz);

            }

        });

        // 以 DB 类型注册到全局存储类映射，供运行时按表名反查实体类
        StoreClass.storeClassMap.put(StoreClassType.DB,storeClassDBMap);

    }

}
