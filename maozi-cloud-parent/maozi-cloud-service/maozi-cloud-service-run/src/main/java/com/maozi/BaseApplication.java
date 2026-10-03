/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maozi;

import cn.hutool.extra.spring.SpringUtil;
import com.maozi.common.LogUtil;
import com.maozi.common.ObjectUtil;
import com.maozi.common.constant.LogTag;
import com.maozi.common.context.ApplicationEnvironmentContext;
import com.maozi.common.context.ApplicationLinkContext;
import com.maozi.common.enums.EnvironmentType;
import com.maozi.common.spi.ConfigInitializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 应用启动基类
 * <p>
 * 微服务应用的统一启动基类，各服务启动入口直接继承此类，或继承 {@code BaseApplicationDB}
 * （组合关系，非继承）并委托本类执行启动。负责统一初始化运行环境、应用名称、
 * 日志等基础启动参数，并在启动前扫描 classpath 下 {@code META-INF/run/config/*.properties}
 * 登记的配置初始化器（{@link ConfigInitializer}），由各初始化器写入 Nacos 配置中心/服务发现、
 * 缓存、定时任务等组件所需的系统属性。
 * </p>
 * <p>
 * 同时在启动最初阶段安装 {@link BannerFilterPrintStream} 包装标准输出，
 * 各组件模块（如 Dubbo、SnailJob 配置模块）通过 {@link #addBannerFilter(String)}
 * 注册第三方框架启动 banner 的过滤特征，由该包装器统一丢弃对应输出。
 * </p>
 *
 * @author maozi
 */
@Slf4j
@SpringBootApplication
@Import({SpringUtil.class})
@DependsOn({ApplicationEnvironmentContext.CLASS_NAME})
public class BaseApplication {

    /** 运行时配置初始化器扫描模式，匹配 classpath（含 JAR 内）下 META-INF/run/config 目录中的所有 .properties 文件，文件内容为实现类全路径 */
    private static final String CONFIG_LOCATION_PATTERN = "classpath*:META-INF/run/config/*.properties";

    /** banner 过滤特征列表，由各组件模块在配置初始化阶段注册，{@link BannerFilterPrintStream} 过滤时实时读取 */
    private static final List<String> BANNER_FILTERS = new CopyOnWriteArrayList<>();

    /**
     * 应用启动入口方法
     * <p>
     * 初始化系统属性、启动 Spring Boot 应用，记录启动日志（包括初始化时间、
     * 服务端口以及各初始化器追加的诊断信息如 Nacos 地址）。启动失败时记录错误日志并退出。
     * </p>
     *
     * @param args 命令行参数
     */
    protected static void ApplicationRun(String[] args) {

        initBannerFilter();

        initProperties();

        List<ConfigInitializer> initializers = initFileProperties();

        Map<String, String> logs = new LinkedHashMap<>();

        long begin = System.currentTimeMillis();

        SpringApplicationBuilder builder = new SpringApplicationBuilder(BaseApplication.class);

        try {

            ConfigurableApplicationContext context = builder.bannerMode(Mode.OFF).run(args);
            logs.put(LogTag.INIT_TIME, (System.currentTimeMillis() - begin) + " ms");

            if (context instanceof WebServerApplicationContext webServerContext) {
                int actualPort = webServerContext.getWebServer().getPort();
                logs.put(LogTag.SERVICE_PORT, Integer.toString(actualPort));
            }

            appendInitializerLogs(initializers, logs);
            LogUtil.info(log,logs);

        } catch (Exception e) {

            LogUtil.error(log,e);

            appendInitializerLogs(initializers, logs);
            logs.put(LogTag.ERROR_DESC, e.getLocalizedMessage());

            StackTraceElement stackTraceElement = e.getStackTrace()[0];
            logs.put(LogTag.ERROR_LINE, stackTraceElement.toString());

            LogUtil.error(log,logs);
            System.exit(0);

        }

    }

    /**
     * 安装启动 banner 过滤输出流
     * <p>
     * 在一切启动流程之前将标准输出包装为 {@link BannerFilterPrintStream}，
     * 后续各组件模块注册的过滤特征在输出时实时生效；已安装时跳过，保证全局仅一层包装
     * （多层嵌套会导致内层过滤被绕过而失效）。
     * </p>
     */
    private static void initBannerFilter() {

        PrintStream stdout = System.out;
        if (!(stdout instanceof BannerFilterPrintStream)) {
            System.setOut(new BannerFilterPrintStream(stdout, BANNER_FILTERS));
        }

    }

    /**
     * 注册启动 banner 过滤特征
     * <p>
     * 由各组件模块（如 Dubbo、SnailJob 配置模块）在配置初始化阶段调用，
     * 注册后标准输出中含该特征片段的内容将被 {@link BannerFilterPrintStream} 整体丢弃。
     * 特征重复注册或为空时忽略。
     * </p>
     *
     * @param flag banner 特征片段
     */
    public static void addBannerFilter(String flag) {

        if (ObjectUtil.isNullEmpty(flag) || BANNER_FILTERS.contains(flag)) {
            return;
        }

        BANNER_FILTERS.add(flag);

    }

    /**
     * 初始化系统属性
     * <p>
     * 解析运行环境（优先取系统属性 environment，其次取环境变量 ENVIRONMENT，
     * 均未设置时默认本地环境），推导应用名与项目简称，并设置应用名称、
     * 允许循环依赖、日志级别等 Spring Boot 启动参数。非本地环境使用异步文件日志
     * 输出到 {@code logs/应用名.log}，本地环境使用异步控制台日志。
     * 应用名称读取失败时抛出异常，终止启动。
     * </p>
     */
    @SneakyThrows
    private static void initProperties() {

        Properties properties = System.getProperties();

        String environment = (String) properties.get("application.environment");
        if(ObjectUtil.isNullEmpty(environment)){
            environment = System.getenv("APPLICATION_ENVIRONMENT");
            if(ObjectUtil.isNullEmpty(environment)){
                environment = EnvironmentType.LOCAL.getDesc();
            }
            properties.put("application.environment",environment);
        }

        String version = (String) properties.get("application.version");
        if(ObjectUtil.isNullEmpty(version)){
            version = System.getenv("APPLICATION_VERSION");
            if(ObjectUtil.isNullEmpty(version)){
                version = ApplicationLinkContext.APPLICATION_DEFAULT_VERSION;
            }
            properties.put("application.version", version);
        }

        String applicationName = getApplicationName();
        if(ObjectUtil.isNullEmpty(applicationName)){
            throw new Exception("服务名读取失败");
        }
        String applicationProjectAbbreviation = applicationName.replace("maozi-cloud","");
        applicationProjectAbbreviation = applicationProjectAbbreviation.substring(0, applicationProjectAbbreviation.length() - 9);

        properties.put("spring.application.name",applicationName);
        properties.put("application-project-abbreviation",applicationProjectAbbreviation);

        properties.put("spring.main.log-startup-info",false);
        properties.put("spring.main.allow-circular-references",true);

        properties.put("logging.level.root", "ERROR");
        properties.put("logging.level.com.maozi", "INFO");
        if(!Objects.equals(environment, EnvironmentType.LOCAL.getDesc())){
            properties.put("logging.appender","ASYNC_FILE");
            properties.put("logging.file.name","logs/${spring.application.name}.log");
        }else{
            properties.put("logging.appender","ASYNC_CONSOLE");
        }

    }

    /**
     * 加载并执行运行时配置初始化器
     * <p>
     * 扫描 classpath 下 {@code META-INF/run/config/*.properties} 文件，
     * 反射实例化文件中登记的 {@link ConfigInitializer} 实现类并依次执行其 initialize
     * 方法写入系统属性，最终返回初始化器列表供启动日志输出使用。
     * </p>
     *
     * @return 已加载并执行完成的配置初始化器列表
     */
    private static List<ConfigInitializer> initFileProperties() {

        Properties properties = System.getProperties();

        List<ConfigInitializer> initializers = new ArrayList<>();

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(CONFIG_LOCATION_PATTERN);
            for (Resource resource : resources) {
                Properties registrations = new Properties();
                try (InputStream inputStream = resource.getInputStream()) {
                    registrations.load(inputStream);
                }
                for (String className : registrations.stringPropertyNames()) {
                    ConfigInitializer initializer = instantiateInitializer(className);
                    initializer.initialize(properties);
                    initializers.add(initializer);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("加载运行时配置初始化器失败：" + CONFIG_LOCATION_PATTERN, e);
        } catch (Exception e) {
            throw new RuntimeException("执行运行时配置初始化器失败", e);
        }

        return initializers;

    }

    /**
     * 依次追加各初始化器的启动日志
     * <p>
     * 在 Spring Boot 应用启动（成功或失败）之后调用，将各 {@link ConfigInitializer}
     * 的诊断信息写入 logs 以便统一输出。
     * </p>
     *
     * @param initializers 已加载的配置初始化器列表
     * @param logs 启动日志容器
     */
    private static void appendInitializerLogs(List<ConfigInitializer> initializers, Map<String, String> logs) {
        for (ConfigInitializer initializer : initializers) {
            initializer.appendLogs(logs);
        }
    }

    /**
     * 实例化配置初始化器
     *
     * @param className 实现类全路径
     * @return 配置初始化器实例
     */
    private static ConfigInitializer instantiateInitializer(String className) {
        try {
            Class<?> clazz = Class.forName(className.trim());
            return (ConfigInitializer) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("实例化配置初始化器失败：" + className, e);
        }
    }

    /**
     * 获取应用名称
     * - jar 运行：取 jar 文件名
     * - 目录运行：从启动类路径往上推导项目名
     *
     * @return 应用名称（jar 运行取 jar 文件名去掉 .jar 后缀，目录运行取推导出的项目名）；无法定位时返回 null
     */
    public static String getApplicationName() {
        Class<?> mainClass = findMainClass();
        if (mainClass == null) {
            return null;
        }

        File codeLocation = getCodeLocation(mainClass);
        if (codeLocation == null) {
            return null;
        }

        String name = codeLocation.getName();

        // jar 包：直接取文件名去掉 .jar
        if (name.endsWith(".jar")) {
            return name.substring(0, name.length() - 4);
        }

        // 目录：往上跳过构建目录，取项目根目录名
        return deriveProjectName(codeLocation);
    }

    /**
     * 从线程栈中找到 main 方法所在的类
     *
     * @return main 方法所在的类；线程栈中无 main 方法或类加载失败时返回 {@code null}
     */
    private static Class<?> findMainClass() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if ("main".equals(element.getMethodName())) {
                try {
                    return Class.forName(element.getClassName());
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * 从目录往上推导项目根目录名
     *
     * @param dir 启动类所在的目录
     * @return 自下而上第一个非构建产物目录的名称；祖先均为构建产物目录时返回 dir 自身名称，dir 为 {@code null} 时返回 {@code null}
     */
    private static String deriveProjectName(File dir) {
        File current = dir;
        while (current != null) {
            String name = current.getName().toLowerCase();
            if (!isBuildDir(name)) {
                return current.getName();
            }
            current = current.getParentFile();
        }

        if (dir != null) {
            return dir.getName();
        }

        return null;
    }

    /**
     * 获取类所在的代码路径
     * <p>
     * Location 形如 jar 包（含 Spring Boot 3 的 nested jar）时，截取其中的 jar 文件路径；
     * 否则视为 IDE/目录运行场景，直接将 Location 转为文件。
     * </p>
     *
     * @param clazz 目标类
     * @return 类所在的 jar 文件或目录；无法获取代码来源时返回 null
     */
    @SneakyThrows
    private static File getCodeLocation(Class<?> clazz) {
        ProtectionDomain domain = clazz.getProtectionDomain();
        CodeSource codeSource = domain.getCodeSource();
        if (codeSource == null) {
            return null;
        }

        String urlStr = codeSource.getLocation().toString();

        // 场景1：jar 包运行（包括传统 jar 和 Spring Boot 3 nested jar）
        // jar:file:/path/to/app.jar!/...
        // jar:nested:/path/to/app.jar/!BOOT-INF/classes/!/
        int jarEndIndex = urlStr.indexOf(".jar");
        if (jarEndIndex != -1) {
            // 截取 URL 开头至 .jar 结束（含 .jar）的部分
            String beforeJar = urlStr.substring(0, jarEndIndex + 4);
            // 找到最后一个冒号的位置，跳过协议前缀
            int colonIndex = beforeJar.lastIndexOf(':');
            if (colonIndex != -1) {
                String path = beforeJar.substring(colonIndex + 1);
                // 处理 Windows 路径可能出现的前导 /
                if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                    path = path.substring(1);
                }
                return new File(path);
            }
        }

        // 场景2：IDE/目录运行，直接用 URI
        return new File(codeSource.getLocation().toURI());
    }

    /**
     * 判断给定目录名是否为构建产物目录（target、build、classes、bin、java、main、test）
     *
     * @param name 目录名（已转小写）
     * @return 是构建产物目录返回 true
     */
    private static boolean isBuildDir(String name) {
        return name.equals("target") || name.equals("build")
                || name.equals("classes") || name.equals("bin")
                || name.equals("java") || name.equals("main")
                || name.equals("test");
    }

}
