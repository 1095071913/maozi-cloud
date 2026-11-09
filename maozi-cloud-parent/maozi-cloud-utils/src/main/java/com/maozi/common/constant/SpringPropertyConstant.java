package com.maozi.common.constant;

/**
 * Spring 配置属性常量类
 * <p>
 * 集中定义项目内多处使用的 Spring 配置属性键名，避免魔法值散落各处，
 * 供各模块配置初始化器（ConfigInitializer）统一引用。
 * </p>
 *
 * @author maozi
 * @since 2026/11/9 15:30
 */
public final class SpringPropertyConstant {

    /** 私有构造器，防止常量类被实例化 */
    private SpringPropertyConstant() {}

    /** Spring 配置文件导入属性键（多个导入项以逗号分隔，越靠后优先级越高） */
    public static final String SPRING_CONFIG_IMPORT = "spring.config.import";

    /** Spring 应用名称属性键 */
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    /** Spring Bean 定义覆盖开关属性键 */
    public static final String ALLOW_BEAN_DEFINITION_OVERRIDING = "spring.main.allow-bean-definition-overriding";

}
