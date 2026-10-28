# ============================================================
# 业务服务公共镜像 —— 叠加 OpenTelemetry Agent 与 Arthas 的 JRE 运行镜像
# ------------------------------------------------------------
# Tag 命名：maozi-cloud-business-jdk:1.0.0
# 基础层为 eclipse-temurin:17-jre-jammy, 时区与 dumb-init 由本 Dockerfile 安装,
# 叠加 OpenTelemetry 字节码增强 Agent 与 Arthas, 供启用 OTel 的服务镜像 FROM:
#   - maozi-cloud-system-service / maozi-cloud-oauth-service / maozi-cloud-ai-service (业务服务)
#   - maozi-cloud-gateway-service                                                              (basics 层)
# 不启用 OTel 的服务 (maozi-cloud-basic-monitor / maozi-cloud-basic-sentinel)
# 直接 FROM maozi-cloud-base-jdk:1.0.0, 减小镜像体积.
# ------------------------------------------------------------
# 两个制品统一放入 /opt/opentelemetry, 供所有继承镜像的 CMD -javaagent 引用:
#   opentelemetry-javaagent.jar           OTel 官方 Agent (字节码注入 + SPI 加载)
#   maozi-cloud-agent-opentelemetry.jar   自定义扩展 (如 FilterSamplerProvider)
# 由 maozi-cloud-utils/maozi-cloud-utils-opentelemetry 维护,
# 更新 jar 后在此目录覆盖同名文件, 并重新 build 本镜像即可.
# ------------------------------------------------------------
# Arthas 诊断工具 (alibaba/arthas 4.3.4, tunnel-server 镜像同为 4.3.4) 解压到 /opt/arthas:
#   arthas-agent.jar 为 -javaagent 挂载入口, 须与 arthas-core.jar / lib/ 等
#   同目录解压后使用; 是否挂载由 maozi-cloud-services.json 各服务的 arthas
#   开关控制 (true 时引入 defaults.arthas_flags, 同 OTel 机制).
#   多阶段构建: zip 与 unzip 只留在解压阶段, 不进最终镜像层.
# ============================================================

# ---- 阶段一: 解压 Arthas (JRE 镜像无解压工具, 装 unzip 解压后随阶段丢弃) ----
FROM eclipse-temurin:17-jre-jammy AS arthas-unpack

COPY arthas-bin.zip /tmp/arthas-bin.zip

RUN apt update && apt install -y unzip && rm -rf /var/lib/apt/lists/* && \
    unzip -q /tmp/arthas-bin.zip -d /opt/arthas && rm -f /tmp/arthas-bin.zip

# ---- 阶段二: 正式镜像 ----
FROM eclipse-temurin:17-jre-jammy

# 创建 APP 目录占位（${APP_NAME} 未赋值时实际为根目录）+ 设置时区为 Asia/Shanghai
RUN mkdir -p /${APP_NAME} && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

# 安装 dumb-init（Ubuntu/Debian 系统用 apt，Alpine 用 apk）
RUN apt update && apt install -y dumb-init && rm -rf /var/lib/apt/lists/*

COPY opentelemetry-javaagent.jar          /opt/opentelemetry/opentelemetry-javaagent.jar
COPY maozi-cloud-agent-opentelemetry.jar  /opt/opentelemetry/maozi-cloud-agent-opentelemetry.jar

COPY --from=arthas-unpack /opt/arthas /opt/arthas
