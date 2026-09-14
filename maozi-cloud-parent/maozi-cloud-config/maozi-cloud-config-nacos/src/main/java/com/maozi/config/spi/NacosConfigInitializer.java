package com.maozi.config.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.spi.ConfigInitializer;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

/**
 * 分布式组件运行时配置初始化器
 * <p>
 * 通过 SPI 机制被 BaseApplication 加载，将 Nacos 配置中心
 * 连接信息与应用自身配置文件的导入声明写入系统属性。
 * </p>
 *
 * @author maozi
 */
public class NacosConfigInitializer implements ConfigInitializer {

    /** Nacos 客户端日志根路径，用于在 Spring 日志配置生效前压低其日志级别 */
    private static final String NACOS_CLIENT_LOGGER = "com.alibaba.nacos";

    /** 项目自定义的 Nacos 服务地址属性键 */
    private final static String PROJECT_NACOS_SERVER_KEY = "nacos.server";

    /** Nacos 服务地址默认值占位符：优先取环境变量 NACOS_SERVER，缺省使用内置 Nacos 服务地址 */
    private final static String PROJECT_NACOS_SERVER_VALUE = "${NACOS_SERVER:maozi-cloud-basic-nacos:8848}";

    /** Nacos 命名空间默认值占位符：优先取环境变量 NACOS_NAMESPACE，缺省跟随当前运行环境 */
    private final static String PROJECT_NACOS_NAMESPACE_VALUE = "${application.environment}";

    /** Nacos 配置中心服务地址属性键 */
    private final static String NACOS_CONFIG_SERVER_KEY = "spring.cloud.nacos.config.server-addr";

    /** Nacos 注册中心（服务发现）服务地址属性键 */
    private final static String NACOS_DISCOVERY_SERVER_KEY = "spring.cloud.nacos.discovery.server-addr";

    /** Nacos 配置中心命名空间属性键 */
    private final static String NACOS_CONFIG_NAMESPACE_KEY = "spring.cloud.nacos.config.namespace";

    /** Nacos 注册中心（服务发现）命名空间属性键 */
    private final static String NACOS_DISCOVERY_NAMESPACE_KEY = "spring.cloud.nacos.discovery.namespace";

    /**
     * 初始化 Nacos 配置中心与注册中心的连接属性。
     * <p>
     * 通过 {@code spring.config.import} 导入应用自身的配置文件
     * {@code {spring.application.name}.yml}（Spring Cloud Alibaba 2023.0.3.x 起
     * 旧 bootstrap 模式与 {@code shared-dataids} 已被移除，统一走 config data 机制）；
     * 若项目属性中已存在 {@code nacos.server} / {@code nacos.namespace}，
     * 则以占位符方式引用对应的项目属性值，否则直接写入默认占位符。
     * </p>
     *
     * @param properties 系统属性容器
     */
    @Override
    public void initialize(Properties properties) {

        // 导入应用自身的配置文件（{spring.application.name}.yml），采用后插方式保证其位于
        // 导入列表末尾（spring.config.import 中越靠后的导入优先级越高，应用自身配置因此
        // 优先于各共享配置文件）；optional 前缀保证文件不存在时不阻断启动
        properties.merge(
            "spring.config.import",
            "optional:nacos:" + properties.getProperty("spring.application.name") + ".yml",
            (existing, incoming) -> existing + "," + incoming
        );

        // 若已存在项目级 nacos.server 定义，则配置中心与注册中心地址以占位符方式引用该值
        if(properties.containsKey(PROJECT_NACOS_SERVER_KEY)){
            properties.put(NACOS_CONFIG_SERVER_KEY, "${" + PROJECT_NACOS_SERVER_KEY + "}");
            properties.put(NACOS_DISCOVERY_SERVER_KEY, "${" + PROJECT_NACOS_SERVER_KEY + "}");
        }else{
            // 否则写入默认占位符：优先取环境变量 NACOS_SERVER，缺省使用内置 Nacos 服务地址
            properties.put(NACOS_CONFIG_SERVER_KEY, PROJECT_NACOS_SERVER_VALUE);
            properties.put(NACOS_DISCOVERY_SERVER_KEY, PROJECT_NACOS_SERVER_VALUE);
        }

        properties.put(NACOS_CONFIG_NAMESPACE_KEY, PROJECT_NACOS_NAMESPACE_VALUE);
        properties.put(NACOS_DISCOVERY_NAMESPACE_KEY, PROJECT_NACOS_NAMESPACE_VALUE);

        // Nacos 客户端在 spring.config.import 阶段（早于 Spring 应用 logback-spring.xml）即初始化并输出 INFO 日志，
        // 此时 logback 仍处于内置默认配置（root=DEBUG + 控制台输出），启动前置写入的 logging.level.root 尚未生效；
        // 因此在 Spring 启动前直接以编程方式将 Nacos 客户端日志级别压至 WARN，屏蔽启动阶段的 INFO 刷屏。
        // Spring 加载 logback-spring.xml 时会 reset 上下文，此设置不会影响运行期日志体系。
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            loggerContext.getLogger(NACOS_CLIENT_LOGGER).setLevel(Level.WARN);
        }

    }

    /** 追加 Nacos 配置中心地址到启动日志。 */
    @Override
    public void appendLogs(Map<String, String> logs) {
        logs.put(LogTag.NACOS, ApplicationEnvironmentContext.CONFIG_ADDR);
    }

}
