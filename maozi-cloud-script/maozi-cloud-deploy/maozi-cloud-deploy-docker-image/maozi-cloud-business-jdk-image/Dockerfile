# ============================================================
# 业务服务公共镜像 —— 在 base-jdk 之上叠加 OpenTelemetry Agent
# ------------------------------------------------------------
# Tag 命名：maozi-cloud-business-jdk:1.0.0
# 继承自 maozi-cloud-base-jdk:1.0.0 (OS + 时区 + dumb-init), 在其之上
# 增加 OpenTelemetry 字节码增强 Agent, 供启用 OTel 的服务镜像 FROM:
#   - maozi-cloud-system-service  / maozi-cloud-oauth-service   (业务服务)
#   - maozi-cloud-gateway-service                               (basics 层)
# 不启用 OTel 的服务 (如 maozi-cloud-monitor-service) 可直接 FROM
# maozi-cloud-base-jdk:1.0.0, 减小镜像体积.
# ------------------------------------------------------------
# 两个制品统一放入 /opt/opentelemetry, 供所有继承镜像的 CMD -javaagent 引用:
#   opentelemetry-javaagent.jar           OTel 官方 Agent (字节码注入 + SPI 加载)
#   maozi-cloud-agent-opentelemetry.jar   自定义扩展 (如 FilterSamplerProvider)
# 由 maozi-cloud-utils/maozi-cloud-utils-opentelemetry 维护,
# 更新 jar 后在此目录覆盖同名文件, 并重新 build 本镜像即可 (无需重建 base-jdk).
# ============================================================

FROM eclipse-temurin:17-jre-jammy

# 创建 APP 目录占位（${APP_NAME} 未赋值时实际为根目录）+ 设置时区为 Asia/Shanghai
RUN mkdir -p /${APP_NAME} && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

# 安装 dumb-init（Ubuntu/Debian 系统用 apt，Alpine 用 apk）
RUN apt update && apt install -y dumb-init && rm -rf /var/lib/apt/lists/*

COPY opentelemetry-javaagent.jar          /opt/opentelemetry/opentelemetry-javaagent.jar
COPY maozi-cloud-agent-opentelemetry.jar  /opt/opentelemetry/maozi-cloud-agent-opentelemetry.jar
