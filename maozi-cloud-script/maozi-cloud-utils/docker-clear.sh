#!/bin/bash
# Docker 清理脚本，支持 Linux / Mac / Windows(GitBash/WSL2)
# 白名单支持三种格式：
# 1. 精确匹配: mysql:8.0.29
# 2. 仓库全部tag: maozi-cloud-oauth-service:*
# 3. 仓库模糊匹配: maozi-cloud-*-service:*
set -uo pipefail

# 镜像白名单，不要删除这些镜像
WHITE_LIST=(
    "mysql:*"
    "redis:*"
    "grafana/grafana-oss:*"
    "prom/prometheus:*"
    "grafana/tempo:*"
    "grafana/loki:*"
    "grafana/promtail:*"
    "nacos/nacos-server:*"
    "seataio/seata-server:*"
    "xuxueli/xxl-job-admin:*"
    "opensnail/snail-job:*"

    "node:*"
    "maozi-cloud-base-jdk:*"
    "maozi-cloud-business-jdk:*"

    "openresty/openresty:*"

    "maozi-cloud-admin-monomer:*"
    "maozi-cloud-admin-distributeds:*"
    "maozi-cloud-service-monomer:*"
    "maozi-cloud-*-service:*"
)

# 网络白名单，保留不删除
NETWORK_WHITE_LIST=(
    "maozi-cloud-network"
)

info() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

show_disk() {
    echo -e "\n==== Disk usage ===="
    df -h
}

# 构建grep正则
# 白名单中的 * 替换成正则 [^:]+ （不跨越冒号，只匹配仓库名）
# :* 替换成 :.+ 匹配任意tag
build_white_regex() {
    local regex_parts=()
    for item in "${WHITE_LIST[@]}"; do
        # 先转义正则特殊字符 . /
        escaped=$(echo "$item" | sed -e 's/\//\\\//g' -e 's/\./\\./g')
        # 仓库部分的 * 替换成 [^:]+
        escaped=${escaped//\*/[^:]+}
        # :* 替换成 :.+
        escaped=$(echo "$escaped" | sed 's/:$/:./;s/:\*$/:+/')
        regex_parts+=("$escaped")
    done
    # 拼接 |
    echo "${regex_parts[*]}" | sed 's/ /|/g'
}

# 构建网络白名单正则
build_network_white_regex() {
    local regex_parts=()
    for net in "${NETWORK_WHITE_LIST[@]}"; do
        escaped=$(echo "$net" | sed -e 's/\./\\./g')
        regex_parts+=("$escaped")
    done
    echo "${regex_parts[*]}" | sed 's/ /|/g'
}

# 获取系统标识
OS=$(uname -s)
info "Detect OS: ${OS}"

info "===== Start Docker Clean Task ====="
info "【清理前磁盘】"
show_disk

# 系统分支判断：清理容器json日志
if [[ "${OS}" == "Linux" ]]; then
    info ">>> System: Linux / WSL2, start clean container json logs"
    log_files=$(find /var/lib/docker/containers/ -type f -name "*-json.log")
    if [[ -n "${log_files}" ]];then
        while IFS= read -r logfile; do
            info "Truncate log: ${logfile}"
            truncate -s 0 "${logfile}" || info "WARN: truncate failed -> ${logfile}"
        done <<< "${log_files}"
    else
        info "No docker json log files found."
    fi

elif [[ "${OS}" == "Darwin" ]]; then
    info ">>> System: macOS(Darwin), skip host log truncate (logs inside Docker VM)"

# Windows GitBash: MINGW64_NT / MSYS_NT
elif [[ "${OS}" == MINGW* || "${OS}" == MSYS* ]]; then
    info ">>> System: Windows GitBash/MSYS, skip host log truncate (docker in VM)"

else
    info ">>> Unknown OS: ${OS}, skip log cleaning"
fi

# ========== 替换原来 docker system prune -f，拆分执行 ==========
info -e "\n>>> Clean stopped containers & dangling images (skip network prune)"
# 删除停止的容器
docker container prune -f || info "WARN: docker container prune failed"
# 删除悬空镜像
docker image prune -f || info "WARN: docker image prune failed"

# 单独清理闲置网络，过滤网络白名单，不删除 maozi-cloud-network
info -e "\n>>> Clean unused networks (keep network white list)"
NET_WHITE_REGEX=$(build_network_white_regex)
# 拿到所有未使用网络名称，排除白名单
unused_nets=$(docker network ls --filter "dangling=true" --format "{{.Name}}" | grep -vE "(${NET_WHITE_REGEX})")
if [[ -n "${unused_nets}" ]]; then
    info "Networks to delete:"
    echo "${unused_nets}"
    echo "${unused_nets}" | xargs -r docker network rm || info "WARN: docker network rm some networks failed"
else
    info "No unused networks to delete or all networks in white list."
fi

# 2. 自定义删除未使用镜像，过滤白名单（支持 maozi-cloud-*-service:*）
WHITE_REGEX=$(build_white_regex)
info -e "\n>>> Start delete unused images, white regex: ${WHITE_REGEX}"

# 获取所有镜像(非悬空)：格式 repo:tag IMAGE_ID，排除白名单，取ID
img_ids=$(docker images --filter "dangling=false" --format "{{.Repository}}:{{.Tag}} {{.ID}}" \
    | grep -vE "(${WHITE_REGEX})" \
    | awk '{print $2}' | sort -u)

if [[ -n "${img_ids}" ]]; then
    info "Images to delete:"
    echo "${img_ids}"
    echo "${img_ids}" | xargs -r docker rmi || info "WARN: docker rmi some images failed"
else
    info "No unused images to delete after filter white list."
fi

# 清除15天未使用过的镜像
docker image prune -a --filter "until=360h" -f

# 3. 清理无用volume
info -e "\n>>> Run docker volume prune (remove unused volumes)"
docker volume prune -f || info "WARN: docker volume prune failed"

info "【清理后磁盘】"
show_disk
info -e "\n===== Docker Clean Task Finished ====="
