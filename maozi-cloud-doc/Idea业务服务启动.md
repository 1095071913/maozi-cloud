# Idea业务服务启动

## JDK安装

选择安装Eclipse Temurin 17

## 代码编译

```
mvn clean install -T 16C
```

## 启动业务服务

1. [单体服务启动](../maozi-cloud-parent/maozi-cloud-service/maozi-cloud-services/maozi-cloud-all-service/src/main/java/com/maozi/Application.java)
2. [网关服务启动](../maozi-cloud-parent/maozi-cloud-service/maozi-cloud-services/maozi-cloud-gateway-service/src/main/java/com/maozi/Application.java)
3. [监控服务启动](../maozi-cloud-parent/maozi-cloud-service/maozi-cloud-services/maozi-cloud-monitor-service/src/main/java/com/maozi/Application.java)
4. [系统服务启动](../maozi-cloud-parent/maozi-cloud-service/maozi-cloud-services/maozi-cloud-business-system/maozi-cloud-system-service/src/main/java/com/maozi/Application.java)
5. [认证服务启动](../maozi-cloud-parent/maozi-cloud-service/maozi-cloud-services/maozi-cloud-business-oauth/maozi-cloud-oauth-service/src/main/java/com/maozi/Application.java)

## 启动前需要设置JVM参数

```
-Xms256m
-Xmx256m
-Xss256k
-XX:MaxMetaspaceSize=192m
-XX:+UseSerialGC
-XX:CompressedClassSpaceSize=32m
-XX:ReservedCodeCacheSize=32m
-XX:MaxDirectMemorySize=64m
--add-opens java.base/java.math=ALL-UNNAMED
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED


# application.version 版本号,设置后根据前端传递的X-Version请求头流量分发到对应版本号服务
# application.port Web端口，默认随机
# application.dubbo.port Dubbo端口，默认随机
# 内存看情况给,当前代码绰绰有余
```
