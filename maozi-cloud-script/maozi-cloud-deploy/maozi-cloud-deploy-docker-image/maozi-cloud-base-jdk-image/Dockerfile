# ============================================================
# 平台最底层基础镜像 —— 仅含 OS + 时区 + dumb-init
# ------------------------------------------------------------
# Tag 命名：maozi-cloud-base-jdk:1.0.0
# ------------------------------------------------------------
# 上游 FROM 方:
#   不启用 OTel 的服务直接 FROM 本镜像 (maozi-cloud-basic-monitor /
#   maozi-cloud-basic-sentinel, 见 maozi-cloud-services.json 的 base_image 覆盖),
#   镜像体积更小, 不携带 OTel Agent jar.
#   注: maozi-cloud-business-jdk 已改为直接基于 eclipse-temurin 构建,
#   不再 FROM 本镜像 (见 maozi-cloud-business-jdk-image/Dockerfile).
# ------------------------------------------------------------
# 基础镜像选择（按目标部署主机的 Linux 发行版/架构组合切换）:
#   - 当前启用 eclipse-temurin:17-jre-jammy (基于 Ubuntu 22.04, 当前部署目标是 ARM 主机);
#   - eclipse-temurin:17-jre-alpine (基于 Alpine, 体积更小) 已注释,
#     用于 AMD 主机部署时切换；取消注释下方 AMD 分支、注释掉 ARM 分支即可。
#     注：tag 名称（jammy/alpine）本身代表发行版而非 CPU 架构，
#     实际多架构构建应通过 docker buildx --platform 或 manifest list 控制。
# ------------------------------------------------------------
# 注意:
#   1. 时区固定 Asia/Shanghai (与 promtail 解析、日志输出对齐);
#   2. dumb-init 作为 PID 1 的「init」进程, 负责回收僵尸进程与转发信号;
#   3. 下方 ${APP_NAME} 在本基础镜像构建时未显式赋值, 会展开为空，
#      mkdir -p / 即创建根目录占位（无副作用）；
#      业务子镜像（maozi-cloud-service-image.template）会通过 ENV APPLICATION_NAME
#      显式赋值并 mkdir 实际目录。两处变量名（APP_NAME / APPLICATION_NAME）不统一，
#      属于历史命名遗留，需注意子模板不再依赖基础镜像的 APP_NAME。
# ============================================================

# AMD 部署（基于 Alpine，需将下方 apt 改为 apk add）
#FROM eclipse-temurin:17-jre-alpine

# ARM 部署（基于 Ubuntu 22.04 jammy）
FROM eclipse-temurin:17-jre-jammy

# 创建 APP 目录占位（${APP_NAME} 未赋值时实际为根目录）+ 设置时区为 Asia/Shanghai
RUN mkdir -p /${APP_NAME} && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

# 安装 dumb-init（Ubuntu/Debian 系统用 apt，Alpine 用 apk）
RUN apt update && apt install -y dumb-init && rm -rf /var/lib/apt/lists/*
