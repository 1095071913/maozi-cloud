#!/bin/bash
# Docker 清理脚本，支持 Linux / Mac / Windows(GitBash/WSL2)
set -uo pipefail

info() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

show_disk() {
    echo -e "\n==== Disk usage ===="
    df -h
}

# 获取系统标识
OS=$(uname -s)
info "Detect OS: ${OS}"

info "===== Start Docker Clean Task ====="
info "【清理前磁盘】"
show_disk

# 系统分支判断
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

# prune 命令全平台通用
info -e "\n>>> Run docker system prune (stopped containers, dangling images, unused networks)"
docker system prune -f -a || info "WARN: docker system prune failed"

# 【危险】取消注释开启：删除所有未使用镜像(含带tag镜像)
# info ">>> WARNING: Run docker system prune -a -f (delete ALL unused tagged images)"
# docker system prune -a -f || info "WARN: docker system prune -a failed"

info -e "\n>>> Run docker volume prune (remove unused volumes)"
docker volume prune -f || info "WARN: docker volume prune failed"

info "【清理后磁盘】"
show_disk
info -e "\n===== Docker Clean Task Finished ====="
