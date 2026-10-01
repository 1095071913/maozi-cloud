#!/bin/bash

# ============================================================
# maozi-cloud-deploy-services-monomer-graceful-update-after.sh
# 优雅更新后置钩子 —— 等主组容器健康后下线 backup 组
# ------------------------------------------------------------
# 与 graceful-update-before.sh 配对, 完成 A/Backup 双节点优雅更新闭环:
#   before: 拉起 backup 组 -> 等全部健康 -> 返回 1 (主组才可停机更新);
#   after:  等待更新后的主组容器全部恢复健康 -> down 掉 backup 组,
#           流量全部回到主组 (nginx dynamic_balancer 随之摘除 -backup 节点).
# ------------------------------------------------------------
# 步骤:
#   1. 循环检测主组 (maozi-cloud-services-monomer-docker.yml, 聚合服务单容器
#      maozi-cloud-service-monomer) 的 docker healthcheck 状态, 直到全部 healthy;
#   2. down 掉 backup 组容器;
#   3. 返回 1 表示执行成功.
# ------------------------------------------------------------
# 返回值约定 (调用方以返回值 1 判定"执行成功"):
#   1 = 主组全部健康且 backup 组已 down (执行成功)
#   2 = 主组容器不存在 / 等待健康超时 / backup down 失败
# ------------------------------------------------------------
# 使用:
#   ./maozi-cloud-deploy-services-monomer-graceful-update-after.sh [服务名]
#   可选参数为目标服务 (compose 服务名或容器名均可), 只检测主组该服务并只
#   下线热备组该服务; 不传则处理 yml 内全部服务
# 可调参数 (环境变量覆盖):
#   HEALTH_TIMEOUT=300  等待主组全部健康的最长秒数 (Java 服务启动较慢)
#   CHECK_INTERVAL=5    每轮检测间隔秒数
#   STOP_TIMEOUT=70     backup 组 down 的停机超时秒数 (与 monomer yml
#                       stop_grace_period 70s 对齐; monomer yml 无 pre_stop 钩子,
#                       SIGTERM 直达 JVM (exec java 为 PID 1) 触发 Spring 优雅停机,
#                       默认 10s 超时会 SIGKILL 中断优雅停机)
# ============================================================

cd "$(dirname "$0")"
current_directory="$(pwd)"

# 主组 compose 文件 (聚合服务, 容器名无 -backup 后缀)
COMPOSE_FILE="$current_directory/../maozi-cloud-deploy-docker/maozi-cloud-business-docker/maozi-cloud-services-monomer-docker.yml"
# backup 组 compose 文件 (容器名带 -backup 后缀, 端口 0 随机分配, 与主组并存)
BACKUP_COMPOSE_FILE="$current_directory/../maozi-cloud-deploy-docker/maozi-cloud-business-docker/maozi-cloud-services-monomer-docker-backup.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "[after] FAILED: compose file not found: $COMPOSE_FILE"
    exit 2
fi
if [ ! -f "$BACKUP_COMPOSE_FILE" ]; then
    echo "[after] FAILED: backup compose file not found: $BACKUP_COMPOSE_FILE"
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

# 主组项目名 / backup 组项目名 (backup 为主组项目名末尾追加 -backup)
COMPOSE_PROJECT="maozi-cloud-business-docker-${ENVIRONMENT}-${VERSION}"
BACKUP_COMPOSE_PROJECT="maozi-cloud-business-docker-${ENVIRONMENT}-${VERSION}-backup"

HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-1000}"
CHECK_INTERVAL="${CHECK_INTERVAL:-5}"
STOP_TIMEOUT="${STOP_TIMEOUT:-1000}"

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
        echo "[after] FAILED: unknown service '$1', available services: $(printf '%s ' $service_list)"
        exit 2
    fi
    echo "[after] target service: $TARGET_SERVICE"
fi

# ============================================================
# 1. 循环检测主组容器健康状态
# ------------------------------------------------------------
# 按本 compose 文件的服务列出主组容器 (yml 增删服务自动对齐,
# 不混入同项目下其他来源的容器), 逐个取 docker healthcheck 状态
# (starting -> healthy / unhealthy), 全部 healthy 才继续下线 backup
# ============================================================
# 按「项目 label + 服务 label」双重过滤运行中容器, 服务列表由本 yml 解析得出:
# 新版 compose (v5) 的 ps -q 只按项目名列出全部容器, 不按 -f 文件的服务过滤,
# 而主组项目名被多个 compose 文件共用 (如 nginx-monomer / admin-monomer / 微服务),
# 它们的容器多无 healthcheck (状态永远为 none), 会被一并算入导致主组明明已健康
# 却一直等待; docker ps -q 仅列运行中容器, 已退出的历史容器天然排除
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
    echo "[after] FAILED: no running containers found for services in $COMPOSE_FILE (project $COMPOSE_PROJECT)"
    exit 2
fi
container_count="$(printf '%s\n' "$container_ids" | wc -l | tr -d ' ')"
echo "[after] waiting for $container_count main containers to become healthy"

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

    if [ "$all_healthy" = true ]; then
        echo "[after] all $container_count main containers healthy"
        break
    fi

    elapsed=$(( $(date +%s) - start_ts ))
    if [ "$elapsed" -ge "$HEALTH_TIMEOUT" ]; then
        echo "[after] FAILED: health wait timeout after ${HEALTH_TIMEOUT}s, pending:$pending_list"
        exit 2
    fi

    echo "[after] waiting(${elapsed}s):$pending_list"
    sleep "$CHECK_INTERVAL"
done

# ============================================================
# 2. down 掉 backup 组容器 (指定目标服务时只下线该服务)
# ------------------------------------------------------------
# --timeout 与 monomer yml stop_grace_period(70s) 对齐: monomer yml 无 pre_stop
# 钩子, SIGTERM 直达 JVM (exec java 为 PID 1) 触发 Spring 优雅停机,
# 不给足超时会被 SIGKILL 中断优雅停机; down 会移除容器与网络
# (外部网络 maozi-cloud-network 不受影响)
# ============================================================
down_exit=0
if [ -n "$TARGET_SERVICE" ]; then
    echo "[after] cd $compose_dir && docker-compose -p $BACKUP_COMPOSE_PROJECT -f $(basename "$BACKUP_COMPOSE_FILE") down --timeout $STOP_TIMEOUT $TARGET_SERVICE"
    docker-compose -p "$BACKUP_COMPOSE_PROJECT" -f "$(basename "$BACKUP_COMPOSE_FILE")" down --timeout "$STOP_TIMEOUT" "$TARGET_SERVICE" || down_exit=$?
else
    echo "[after] cd $compose_dir && docker-compose -p $BACKUP_COMPOSE_PROJECT -f $(basename "$BACKUP_COMPOSE_FILE") down --timeout--timeout $STOP_TIMEOUT"
    docker-compose -p "$BACKUP_COMPOSE_PROJECT" -f "$(basename "$BACKUP_COMPOSE_FILE")" down --timeout "$STOP_TIMEOUT" || down_exit=$?
fi
if [ "$down_exit" -ne 0 ]; then
    echo "[after] FAILED: docker-compose down backup error"
    exit 2
fi

# ============================================================
# 3. 返回 1 表示执行成功
# ============================================================
echo "[after] done: main group healthy, backup group removed"
exit 1
