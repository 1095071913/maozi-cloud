package com.maozi.common;

import com.maozi.common.enums.BaseEnum;
import com.maozi.common.result.error.code.SystemErrorCode;
import com.maozi.common.result.error.exception.BusinessResultException;

import java.util.Objects;

/**
 * 枚举工具类
 * <p>
 * 提供按整型值查找 {@link BaseEnum} 实现类枚举实例的通用方法，
 * 未找到时可返回 null 或抛出业务异常。
 * </p>
 *
 * @author pengjinlong
 * @since 2026/4/26 09:04
 */
public final class EnumUtil {

    /** 私有构造器，防止工具类被实例化 */
    private EnumUtil() {}

    /**
     * 根据整型值获取对应的枚举实例
     *
     * @param value 枚举整型值
     * @param clazz 枚举类型
     * @param <T> 枚举类型泛型
     * @return 匹配的枚举实例，未找到时返回 null
     */
    public static <T extends BaseEnum> T getEnum(Integer value, Class<T> clazz) {

        T[] values = clazz.getEnumConstants();

        for(T iEnum : values) {

            if(Objects.equals(iEnum.getValue(), value)) {
                return iEnum;
            }

        }

        return null;

    }

    /**
     * 根据整型值获取对应的枚举实例，未找到时抛出业务异常
     *
     * @param value 枚举整型值
     * @param clazz 枚举类型
     * @param resourceName 资源名称（用于异常提示定位）
     * @param <T> 枚举类型泛型
     * @return 匹配的枚举实例
     * @throws BusinessResultException 未找到对应枚举时抛出
     */
    public static <T extends BaseEnum> T getEnumNullThrow(Integer value, Class<T> clazz, String resourceName) {

        T[] values = clazz.getEnumConstants();

        for(T iEnum : values) {

            if(Objects.equals(iEnum.getValue(), value)) {
                return iEnum;
            }

        }

        throw new BusinessResultException(SystemErrorCode.DATA_NOT_EXIST_ERROR).setResource(resourceName);

    }

}
