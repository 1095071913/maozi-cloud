package com.maozi.db.config;

import jakarta.annotation.Nonnull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库自动初始化处理器（基于 BeanFactoryPostProcessor）
 * <p>
 * 在所有 Bean 实例化之前执行（此时 Environment 已完全就绪，包括 Nacos 远程配置），
 * 检查目标 MySQL 数据库是否存在，不存在则自动创建（字符集 utf8mb4，排序规则 utf8mb4_general_ci），
 * 确保 DataSource 和 Flyway Bean 创建时数据库已存在。
 * </p>
 * <p>
 * 之所以选择在 BeanFactoryPostProcessor 扩展点执行本逻辑，
 * 而非依赖普通 Bean 的生命周期回调，是为了避免触发
 * {@code @Configuration} 配置类提前实例化的问题。
 * </p>
 *
  * @author maozi
 * @date 2026/11/08
 */
@Configuration
public class DatabaseInitBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware {

    /** 数据源 URL 配置键 */
    private static final String DATASOURCE_URL = "spring.datasource.url";

    /** 数据源用户名配置键 */
    private static final String DATASOURCE_USERNAME = "spring.datasource.username";

    /** 数据源密码配置键 */
    private static final String DATASOURCE_PASSWORD = "spring.datasource.password";

    /** JDBC MySQL 前缀 */
    private static final String JDBC_MYSQL_PREFIX = "jdbc:mysql://";

    /** Spring 环境上下文，用于读取数据源配置 */
    private Environment environment;

    /**
     * 注入 Spring 环境上下文
     *
     * @param environment Spring 环境对象
     */
    @Override
    public void setEnvironment(@Nonnull Environment environment) {
        this.environment = environment;
    }

    /**
     * 在 Bean 工厂后处理阶段执行数据库初始化
     * <p>
     * 从环境配置中读取数据源连接信息，连接到 MySQL 服务器，
     * 检查目标数据库是否存在，不存在则自动创建。
     * 该方法在所有 Bean 定义加载完成后、Bean 实例化之前执行，
     * 确保 DataSource 和 Flyway 等 Bean 创建时数据库已存在。
     * </p>
     *
     * @param beanFactory Bean 工厂（未使用，由 Spring 框架传入）
     * @throws RuntimeException 连接 MySQL 或执行检查/创建数据库语句失败时抛出（消息携带目标数据库名）
     */
    @Override
    public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String url = environment.getProperty(DATASOURCE_URL);
        String username = environment.getProperty(DATASOURCE_USERNAME);
        String password = environment.getProperty(DATASOURCE_PASSWORD, "");

        // 非 MySQL 数据源或未配置时跳过
        if (url == null || !url.startsWith(JDBC_MYSQL_PREFIX) || username == null) {
            return;
        }

        String dbName = extractDatabaseName(url);
        if (dbName == null || dbName.isBlank()) {
            return;
        }

        String serverUrl = buildServerUrl(url);

        // 连接到 MySQL 服务器（不指定具体数据库），用于检查和创建数据库
        try (Connection conn = DriverManager.getConnection(serverUrl, username, password);
             Statement stmt = conn.createStatement()) {

            // 检查数据库是否存在
            ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + dbName + "'");
            if (!rs.next()) {
                // 数据库不存在，使用 utf8mb4 字符集创建，支持完整的 Unicode 字符（包括 Emoji）
                stmt.executeUpdate("CREATE DATABASE `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            }

        } catch (Exception e) {
            throw new RuntimeException("数据库初始化失败: " + dbName, e);
        }
    }

    /**
     * 从 JDBC URL 中提取数据库名称
     * <p>
     * 示例：{@code jdbc:mysql://localhost:3306/my_db?useUnicode=true} → {@code my_db}
     * </p>
     *
     * @param url JDBC URL
     * @return 数据库名称；URL 中未找到路径分隔符时返回 {@code null}
     */
    private String extractDatabaseName(String url) {
        int dbStart = url.indexOf('/', JDBC_MYSQL_PREFIX.length());
        if (dbStart < 0) {
            return null;
        }
        int paramStart = url.indexOf('?', dbStart);
        if (paramStart > 0) {
            return url.substring(dbStart + 1, paramStart);
        }
        return url.substring(dbStart + 1);
    }

    /**
     * 构建 MySQL 服务器连接 URL（不含数据库名）
     * <p>
     * 示例：{@code jdbc:mysql://localhost:3306/my_db?useUnicode=true}
     * → {@code jdbc:mysql://localhost:3306/?useUnicode=true}
     * </p>
     *
     * @param url 原始 JDBC URL
     * @return 服务器级别连接 URL
     */
    private String buildServerUrl(String url) {
        int dbStart = url.indexOf('/', JDBC_MYSQL_PREFIX.length());
        int paramStart = url.indexOf('?', dbStart);
        if (paramStart > 0) {
            return url.substring(0, dbStart + 1) + url.substring(paramStart);
        }
        return url.substring(0, dbStart + 1);
    }
}
