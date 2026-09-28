# ============================================================
# maozi-cloud-admin 后台前端镜像 (多阶段构建)
# ------------------------------------------------------------
# 阶段一 builder: node:20-alpine 执行 npm ci + npm run build
#                (vue-tsc 类型检查 + vite build) 产出 dist
# 阶段二 runtime: nginx:alpine 托管静态资源 + 同源转发
# ------------------------------------------------------------
# 构建上下文为 maozi-cloud-admin 源码目录 (部署脚本以 -f 指定本文件),
# COPY 只能取上下文内文件, 故部署脚本会把本目录的
# maozi-cloud-admin-nginx.conf 临时拷入源码目录, 构建后删除。
# ============================================================

# ---------- 阶段一: 前端构建 ----------
FROM node:20-alpine AS builder

WORKDIR /build

# 先拷依赖清单再 npm ci, 利用 Docker 层缓存:
# 源码变更不触发依赖重装, 只有 package*.json 变化才重跑
COPY package.json package-lock.json ./
RUN npm ci

# 拷源码并构建 (VITE_API_BASE_URL 取 .env.production 的 /api)
COPY . .
RUN npm run build

# ---------- 阶段二: nginx 托管 ----------
FROM nginx:alpine

RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

# 配置模板化: 放入 templates 目录, 容器启动时 nginx:alpine 入口脚本
# 用 envsubst 把 ${API_BASE_PATH} 渲染进 /etc/nginx/conf.d/default.conf
# (覆盖自带欢迎页); ENV 提供默认值, compose 按部署模式覆盖即可
ENV API_BASE_PATH=http://maozi-cloud-gateway-service:1000
COPY maozi-cloud-admin-nginx.conf /etc/nginx/templates/default.conf.template

COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 999
