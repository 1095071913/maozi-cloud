#!/bin/bash

# ============================================================
# maozi-cloud-deploy-services-monomer-graceful-update-before.sh
# 优雅更新前置钩子 —— 拉起 backup 组容器并等待健康
# ------------------------------------------------------------
# 配合 nginx dynamic_balancer.lua 的 A/Backup 双节点方案:
#   主组容器 (无后缀) 继续承接流量的同时, 先把 backup 组拉起来
#   (容器名带 -backup 后缀, 端口 0 随机分配不与主组冲突),
#   等 backup 组全部健康后, 主组才能安全停机更新.
# ------------------------------------------------------------
# 步骤:
#   1. 用 maozi-cloud-services-monomer-docker-backup.yml 拉起 backup 容器
#      (聚合服务单容器 maozi-cloud-service-monomer; 独立 compose 项目名带
#       -backup 后缀: 服务名与主组 yml 完全相同, 沿用主组项目名会被当作
#       同项目重建主组容器, 故必须隔离成第二个项目);
#   2. 循环检测项目下所有容器的 docker healthcheck 状态, 直到全部 healthy;
#   3. 全部健康 -> 返回 1 表示执行完成.
# ------------------------------------------------------------
# 返回值约定 (调用方以返回值 1 判定"执行完成"):
#   1 = backup 组容器已全部启动且健康 (执行完成)
#   2 = compose 拉起失败 / 项目下无容器 / 等待健康超时
# ------------------------------------------------------------
# 使用:
#   ./maozi-cloud-deploy-services-monomer-graceful-update-before.sh [服务名]
#   可选参数为目标服务 (compose 服务名或容器名均可), 只启动并检测该服务;
#   不传则处理 yml 内全部服务
# 可调参数 (环境变量覆盖):
#   HEALTH_TIMEOUT=300  等待全部健康的最长秒数 (Java 服务启动较慢)
#   CHECK_INTERVAL=5    每轮检测间隔秒数
# ============================================================

cd "$(dirname "$0")"
current_directory="$(pwd)"

# backup 组 compose 文件 (容器名带 -backup 后缀, 端口 0 随机分配, 与主组并存)
COMPOSE_FILE="$current_directory/../maozi-cloud-deploy-docker/maozi-cloud-business-docker/maozi-cloud-services-monomer-docker-backup.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "[before] FAILED: compose file not found: $COMPOSE_FILE"
    exit 2
fi

# v1 docker-compose 只从工作目录读 .env (ENVIRONMENT / GRACEFUL_UPDATE 等插值变量),
# 需 cd 进 compose 目录执行并显式指定项目名
compose_dir="$(dirname "$COMPOSE_FILE")"
cd "$compose_dir"

# .env 的值可含空格 (如 JVM_PARAMS), 不能整体 source, 仅按需提取插值 key;
# ENVIRONMENT / VERSION 回退链与 yml 容器名插值保持同源:
# 调用方环境变量优先, 其次 .env, 逐级回退 APPLICATION_* / dev / main
read_env() { grep -m1 "^$1=" .env 2>/dev/null | cut -d= -f2-; }
ENVIRONMENT="${ENVIRONMENT:-$(read_env ENVIRONMENT)}"
ENVIRONMENT="${ENVIRONMENT:-${APPLICATION_ENVIRONMENT:-$(read_env APPLICATION_ENVIRONMENT)}}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
VERSION="${VERSION:-$(read_env VERSION)}"
VERSION="${VERSION:-${APPLICATION_VERSION:-$(read_env APPLICATION_VERSION)}}"
VERSION="${VERSION:-main}"

# backup 组独立 compose 项目名 (主组 maozi-cloud-business-docker-${ENVIRONMENT}-${VERSION} 的项目名末尾追加 -backup)
COMPOSE_PROJECT="maozi-cloud-business-docker-${ENVIRONMENT}-${VERSION}-backup"

HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-1000}"
CHECK_INTERVAL="${CHECK_INTERVAL:-5}"

# ============================================================
# 目标服务 (可选入参 $1): 传服务名或容器名则只处理该服务, 不传则处理全部;
# 容器名会自动剥离去 -backup 与 -${ENVIRONMENT}-${VERSION} 后缀还原服务名
# ============================================================
TARGET_SERVICE=""
if [ -n "$1" ]; then
    service_list="$(docker-compose -p "$COMPOSE_PROJECT" -f "$(basename "$COMPOSE_FILE")" config --services 2>/dev/null)"
    for service_candidate in "$1" "${1%-backup}" "${1%-${ENVIRONMENT}-${VERSION}}" "${1%-${ENVIRONMENT}-${VERSION}-backup}"; do
        [ -n "$service_candidate" ] || continue
        if printf '%s\n' $service_list | grep -qx -- "$service_candidate"; then
            TARGET_SERVICE="$service_candidate"
            break
        fi
    done
    if [ -z "$TARGET_SERVICE" ]; then
        echo "[before] FAILED: unknown service '$1', available services: $(printf '%s ' $service_list)"
        exit 2
    fi
    echo "[before] target service: $TARGET_SERVICE"
fi

# ============================================================
# 1. 拉起 backup 组容器 (指定目标服务时只拉起该服务)
# ============================================================
up_exit=0
if [ -n "$TARGET_SERVICE" ]; then
    echo "[before] cd $compose_dir && docker-compose -p $COMPOSE_PROJECT -f $(basename "$COMPOSE_FILE") up -d $TARGET_SERVICE"
    docker-compose -p "$COMPOSE_PROJECT" -f "$(basename "$COMPOSE_FILE")" up -d "$TARGET_SERVICE" || up_exit=$?
else
    echo "[before] cd $compose_dir && docker-compose -p $COMPOSE_PROJECT -f $(basename "$COMPOSE_FILE") up -d"
    docker-compose -p "$COMPOSE_PROJECT" -f "$(basename "$COMPOSE_FILE")" up -d || up_exit=$?
fi
if [ "$up_exit" -ne 0 ]; then
    echo "[before] FAILED: docker-compose up error"
    exit 2
fi

# ============================================================
# 2. 循环检测容器健康状态
# ------------------------------------------------------------
# 按本 compose 文件的服务列出 backup 容器 (yml 增删服务自动对齐),
# 逐个取 docker healthcheck 状态 (starting -> healthy / unhealthy),
# 全部 healthy 才算完成
# ============================================================
# 按「项目 label + 服务 label」双重过滤运行中容器, 服务列表由本 yml 解析得出:
# 新版 compose (v5) 的 ps -q 只按项目名列出全部容器, 不按 -f 文件的服务过滤,
# 共用项目名的其他 compose 文件容器 (如 nginx-monomer / admin-monomer,
# 无 healthcheck, 状态永远为 none) 会被一并算入导致永远等不到健康;
# docker ps -q 仅列运行中容器, 已退出的历史容器天然排除
list_containers() {
    local service_name container_id
    for service_name in $(docker-compose -p "$COMPOSE_PROJECT" -f "$(basename "$COMPOSE_FILE")" config --services 2>/dev/null); do
        # 指定目标服务时跳过其他服务
        [ -n "$TARGET_SERVICE" ] && [ "$service_name" != "$TARGET_SERVICE" ] && continue
        container_id="$(docker ps -q \
            --filter "label=com.docker.compose.project=$COMPOSE_PROJECT" \
            --filter "label=com.docker.compose.service=$service_name" | head -n 1)"
        [ -n "$container_id" ] && echo "$container_id"
    done
}

# 容器列表在等待开始时固化一次: 中途崩溃退出的容器仍会被 inspect 出非健康状态,
# 避免每轮重新 docker ps 时容器恰好处于重启间隙被漏检而提前判定完成
container_ids="$(list_containers)"
if [ -z "$container_ids" ]; then
    echo "[before] FAILED: no running containers found for services in $COMPOSE_FILE (project $COMPOSE_PROJECT)"
    exit 2
fi
container_count="$(printf '%s\n' "$container_ids" | wc -l | tr -d ' ')"
echo "[before] waiting for $container_count backup containers to become healthy"

start_ts=$(date +%s)
while true; do
    all_healthy=true
    pending_list=""

    for container_id in $container_ids; do
        container_name="$(docker inspect -f '{{.Name}}' "$container_id" | sed 's#^/##')"
        container_state="$(docker inspect -f '{{.State.Status}}' "$container_id")"
        # 无 healthcheck 的容器输出 none, 与 unhealthy 同样视为未就绪
        health_status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id")"
        if [ "$health_status" != "healthy" ]; then
            all_healthy=false
            pending_list="$pending_list $container_name($container_state/$health_status)"
        fi
    done

    # ============================================================
    # 3. 全部健康 -> 返回 1 表示执行完成
    # ============================================================
    if [ "$all_healthy" = true ]; then
        echo "[before] done: all $container_count backup containers healthy"
        exit 1
    fi

    elapsed=$(( $(date +%s) - start_ts ))
    if [ "$elapsed" -ge "$HEALTH_TIMEOUT" ]; then
        echo "[before] FAILED: health wait timeout after ${HEALTH_TIMEOUT}s, pending:$pending_list"
        exit 2
    fi

    echo "[before] waiting(${elapsed}s):$pending_list"
    sleep "$CHECK_INTERVAL"
done
