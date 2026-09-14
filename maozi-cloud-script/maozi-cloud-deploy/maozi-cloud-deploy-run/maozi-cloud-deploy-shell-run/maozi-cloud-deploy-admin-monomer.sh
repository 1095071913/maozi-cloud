#!/bin/bash

# ============================================================
# 后台前端 (maozi-cloud-admin) 容器化部署入口 - 单体模式 (self-contained)
# 对应 bat 版: maozi-cloud-deploy-bat-run/maozi-cloud-deploy-admin-monomer.bat
# 对应微服务版: maozi-cloud-deploy-shell-run/maozi-cloud-deploy-admin-distributed.sh
# ------------------------------------------------------------
# 与微服务版的区别: 镜像 / 容器名为 maozi-cloud-admin-monomer,
# 拉起 maozi-cloud-monomer-docker/maozi-cloud-admin-monomer-docker.yml
# ------------------------------------------------------------
# 与后端 jar 服务不同, 前端是 Docker 多阶段镜像构建:
#   1. 定位 maozi-cloud-admin 源码目录 (构建上下文)
#   2. 把 maozi-cloud-admin-nginx.conf 临时拷入源码目录
#      (Dockerfile COPY 只能取上下文内文件), 构建结束由 trap 删除
#   3. docker buildx build: node 阶段 npm ci + npm run build 产出 dist,
#      nginx 阶段托管 dist 并做同源转发 (API -> 网关, /nacos, /grafana)
#   4. docker-compose up -d 拉起容器 (端口 999)
# ------------------------------------------------------------
# 前置:
#   1. maozi-cloud-network 已创建
#   2. 单体服务已启动 (maozi-cloud-service-monomer, 由
#      maozi-cloud-deploy-services-monomer.sh 拉起);
#      单体模式无独立网关容器, /api/ 转发地址需指向单体服务,
#      详见 maozi-cloud-admin-image/maozi-cloud-admin-nginx.conf 的单体模式说明
#   3. basics-docker 已启动 (/nacos /grafana 同源代理依赖)
#   4. 无需宿主机 node, 构建全部在 Docker 内完成
# ------------------------------------------------------------
# 与微服务版 (maozi-cloud-admin-distributeds) 互斥: 同占宿主机 999 端口,
# 切换模式前先停掉另一模式的 admin 容器。
# ============================================================

cd "$(dirname "$0")"
current_directory="$(pwd)"

# 源码仓库根目录: 由脚本所在位置推导 (maozi-cloud-deploy-shell-run 向上四级);
# 前端源码目录 maozi-cloud-admin 与 maozi-cloud-parent 是仓库根下的同级目录
repo_root="$(cd "$current_directory/../../../.." && pwd)"
admin_directory="$repo_root/maozi-cloud-admin"

# 部署资产目录 (镜像定义与 compose 均随脚本仓库走, 不侵入前端项目)
image_directory="$current_directory/../../maozi-cloud-deploy-docker-image/maozi-cloud-admin-image"
nginx_conf_name="maozi-cloud-admin-nginx.conf"
COMPOSE_FILE="$current_directory/../../maozi-cloud-deploy-docker/maozi-cloud-monomer-docker/maozi-cloud-admin-monomer-docker.yml"
IMAGE_TAG="maozi-cloud-admin-monomer:laster"

# ---- 前置检查 ----
if [ ! -f "$admin_directory/package.json" ]; then
    echo "[deploy] package.json not found at $admin_directory"
    exit 1
fi
if [ ! -f "$image_directory/Dockerfile" ]; then
    echo "[deploy] Dockerfile not found at $image_directory"
    exit 1
fi
if [ ! -f "$image_directory/$nginx_conf_name" ]; then
    echo "[deploy] nginx conf not found at $image_directory/$nginx_conf_name"
    exit 1
fi
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "[deploy] compose file not found at $COMPOSE_FILE"
    exit 1
fi

# ---- 临时拷入 nginx.conf, 退出时无论成败都清理 ----
cp "$image_directory/$nginx_conf_name" "$admin_directory/"
trap 'rm -f "$admin_directory/$nginx_conf_name"' EXIT

# ---- 1. 多阶段构建镜像 ----
echo "[deploy] building image $IMAGE_TAG (npm build in node stage, served by nginx)"
docker buildx build -f "$image_directory/Dockerfile" -t "$IMAGE_TAG" "$admin_directory"
if [ $? -ne 0 ]; then
    echo "[deploy] FAILED: docker buildx error, abort"
    exit 1
fi

# ---- 2. 拉起容器 (停旧 / 起新由 compose 自动处理) ----
echo "[deploy] docker-compose -f $COMPOSE_FILE up -d"
docker-compose -f "$COMPOSE_FILE" up -d
if [ $? -ne 0 ]; then
    echo "[deploy] FAILED: docker-compose up error"
    exit 1
fi

echo "[deploy] done: maozi-cloud-admin -> $IMAGE_TAG (port 999)"
