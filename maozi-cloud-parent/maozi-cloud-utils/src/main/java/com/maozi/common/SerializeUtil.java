package com.maozi.common;

/**
 * 序列化工具常量类
 * <p>
 * 定义序列化相关的常量字段名，用于反射操作时排除序列化内置字段。
 * </p>
 *
 * @author pengjinlong
 * @date 2026/4/29
 */
public final class SerializeUtil {

    /** 私有构造器，防止工具类被实例化 */
    private SerializeUtil() {}

    /** 序列化版本号字段名 */
    public static final String SERIAL_VERSION_UID_FIELD_NAME = "serialVersionUID";

    /** writeReplace 方法名 */
    public static final String WRITE_REPLACE_FIELD_NAME = "writeReplace";

}
