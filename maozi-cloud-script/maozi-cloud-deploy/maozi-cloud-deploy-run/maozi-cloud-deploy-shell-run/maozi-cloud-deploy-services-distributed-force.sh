#!/bin/bash

# ============================================================
# 强制全量部署入口 (单一 git 仓库版)
# ------------------------------------------------------------
# 与 maozi-cloud-deploy-services-distributed.sh 的区别:
#   - 不读取 / 写入 maozi-cloud-deploy-services-distributed-record 状态 (分支 / SHA)
#   - 不做 git diff 增量比对
#   - 不做 jar mtime 前后快照对比
#   - 无条件: 全量 mvn 构建 + 所有在 JSON 配置里登记的服务全部重建并重启
# ------------------------------------------------------------
# 工作流程:
#   1. 切换到仓库根目录
#   2. 构建基础镜像 maozi-cloud-base-jdk:1.0.0 (服务镜像 FROM 它, 必须先就绪)
#   3. mvn clean install (并行线程数按 CPU 核数动态计算) 全量构建整个 reactor
#   4. find 扫描 maozi-cloud-services 下所有 jar
#   5. 对每个 jar:
#        - 按服务名前缀路由镜像 / docker-compose 目录
#        - 调用 render-image 渲染器: 读 maozi-cloud-services.json + 模板渲染
#          ${service_name}-image, buildx 构建, compose up -d, rm 镜像文件
#        - 服务不在 JSON 配置里时, 渲染器自动 skip, 不阻塞其他服务
#        - 后台并行执行
#   6. wait 等所有后台部署完成
# ------------------------------------------------------------
# 适用场景:
#   - 首次部署
#   - 切换分支 / 合并代码后想强制重新部署一切
#   - base-jdk 镜像或 OTel agent 变更后需要全部重建
#   - 怀疑增量部署有遗漏, 想做一次彻底的回归
# ============================================================

# 切换到本脚本所在目录, 使后续相对路径可靠
cd "$(dirname "$0")"
current_directory="$(pwd)"

# 源码仓库根目录: 由脚本所在位置推导 (maozi-cloud-deploy-shell-run 向上四级);
# 当前结构下脚本位于 maozi-cloud-script/maozi-cloud-deploy 内, 源码仓库 maozi-cloud-parent 是 maozi-cloud-script 的同级目录,
# 向上四级无 pom.xml 时进入 maozi-cloud-parent (兼容脚本位于仓库内部的旧结构)
repo_candidate="$(cd "$current_directory/../../../.." && pwd)"
if [ -f "$repo_candidate/pom.xml" ]; then
	repo_directory="$repo_candidate"
else
	repo_directory="$repo_candidate/maozi-cloud-parent"
fi
cd "$repo_directory"

# 可部署服务源码根目录 (仅此目录下生成的 jar 才会触发 Docker 部署)
services_subdir="maozi-cloud-service/maozi-cloud-services"

# ============================================================
# 工具函数 (与 maozi-cloud-deploy-jar-utils.sh 保持一致)
# ============================================================

# 根据服务名前缀路由镜像目录
#   maozi-cloud-basics-*                                -> maozi-cloud-basics-image
#   其他（含 gateway-service / monitor-service / system / oauth）-> maozi-cloud-services-image
route_image_dir() {
    local service_name="$1"
    local base="$current_directory/../../maozi-cloud-deploy-docker-image"
    case "$service_name" in
        maozi-cloud-basics-*) echo "$base/maozi-cloud-basics-image" ;;
        *)                    echo "$base/maozi-cloud-services-image" ;;
    esac
}

# 根据服务名前缀路由 docker-compose 目录
route_docker_dir() {
    local service_name="$1"
    local base="$current_directory/../../maozi-cloud-deploy-docker"
    case "$service_name" in
        maozi-cloud-basics-*) echo "$base/maozi-cloud-basics-docker" ;;
        *)                    echo "$base/maozi-cloud-distributeds-docker" ;;
    esac
}

# 镜像渲染器: 模板 + JSON -> ${service_name}-image, 然后 buildx + compose, 最后清理
# 必须在 route_image_dir / route_docker_dir 定义之后再 source (渲染器内部会调用它们)
source "$current_directory/maozi-cloud-deploy-shell-util/maozi-cloud-render-image.sh"

# ============================================================
# 1. 构建基础 JDK 镜像 (所有服务镜像 FROM 它 / business-jdk)
# ------------------------------------------------------------
# 两层结构, 必须按顺序构建:
#   a) maozi-cloud-base-jdk:1.0.0     OS + 时区 + dumb-init (不含 OTel)
#   b) maozi-cloud-business-jdk:1.0.0 FROM base-jdk + OTel Agent jar
# 服务镜像按 OTel 开关 FROM 其中一个, 详见 maozi-cloud-services.json
# ============================================================
base_image_directory="$current_directory/../../maozi-cloud-deploy-docker-image/maozi-cloud-base-jdk-image"
business_image_directory="$current_directory/../../maozi-cloud-deploy-docker-image/maozi-cloud-business-jdk-image"

if [ -f "$base_image_directory/Dockerfile" ]; then
    echo "[base] building maozi-cloud-base-jdk:1.0.0"
    (cd "$base_image_directory" && docker buildx build -f Dockerfile -t maozi-cloud-base-jdk:1.0.0 .) || {
        echo "[base] FAILED: maozi-cloud-base-jdk:1.0.0 build error, abort"
        exit 1
    }
    echo "[base] done"
else
    echo "[base] skip: Dockerfile not found at $base_image_directory/Dockerfile"
fi

if [ -f "$business_image_directory/Dockerfile" ]; then
    echo "[base] building maozi-cloud-business-jdk:1.0.0"
    # business-jdk FROM base-jdk, 必须等上一步 base-jdk 构建完才能 FROM 到
    # 上下文为 Dockerfile 所在目录 (含 OTel agent jar)
    (cd "$business_image_directory" && docker buildx build -f Dockerfile -t maozi-cloud-business-jdk:1.0.0 .) || {
        echo "[base] FAILED: maozi-cloud-business-jdk:1.0.0 build error, abort"
        exit 1
    }
    echo "[base] done"
else
    echo "[base] skip: Dockerfile not found at $business_image_directory/Dockerfile"
fi

# ============================================================
# 2. 全量 Maven 构建 (不做 -pl / -amd 增量, 直接整个 reactor)
# ============================================================
echo "[build] mvn clean install (full reactor)"

# Maven 并行线程数按当前机器 CPU 核数动态计算: 2 * 核数
#   - 原 -T 16C 在多核机器上线程过多 (16 核 = 256), 配合 compile fork
#     会同时派生大量编译 JVM, 反而拖慢构建并吃紧内存
#   - getconf 同时支持 macOS / Linux, sysctl / nproc 作兜底
cpu_cores="$(getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)"
mvn_threads=$(( cpu_cores * 2 ))
echo "[build] mvn threads: $mvn_threads (cores: $cpu_cores)"

# 复用 jar-utils 的参数:
#   -T $mvn_threads           并行线程数 = 2 * CPU 核
#   -Dmaven.compile.fork=true 编译过程 fork 专属 JVM, 加快速度
#   -Dmaven.test.skip=true    跳过测试编译与执行, 加快部署
mvn clean install -T "$mvn_threads" -Dmaven.compile.fork=true -Dmaven.test.skip=true

# Maven 失败立即中止, 不进入 Docker 部署阶段 (避免用旧 jar 重建镜像)
if [ $? -ne 0 ]; then
    echo "[build] FAILED: mvn clean install error, abort"
    exit 1
fi

# ============================================================
# 3. 扫描所有 jar, 过滤出可部署的服务
# ============================================================
# 锁定仓库根的绝对路径, 让生成的临时脚本里的 cp 源路径不依赖 cwd
repo_root="$(pwd)"

# find 所有 jar (包括首次部署时还没构建的情况也能正确跳过)
deploy_count=0

# process substitution 避免 while 进入 subshell 丢变量
while IFS= read -r -d '' jarfile; do

    # dirname 两次: jar -> target -> 模块目录
    # business: maozi-cloud-business-xxx/maozi-cloud-xxx-service (启动模块)
    # basics:   maozi-cloud-gateway-service / maozi-cloud-monitor-service (扁平单模块)
    module_dir="$(dirname "$(dirname "$jarfile")")"
    service_name="$(basename "$module_dir")"

    # 渲染 + 构建 + 清理统一交给 maozi-cloud-render-image.sh:
    #   - 读 maozi-cloud-services.json 取该服务的端口 / Dubbo / OTel / JVM / base_image
    #   - 按模板渲染出 ${service_name}-image, buildx 构建, compose 启动, rm 镜像文件
    # 服务不在 JSON 配置里时, 渲染器返回非零并打印 skip, 不阻塞其他服务
    if render_and_build_image "$service_name" "$module_dir"; then
        deploy_count=$((deploy_count + 1))
    fi

done < <(find "$services_subdir" -name "*.jar" -print0 2>/dev/null)

echo "[deploy] services in flight: $deploy_count"

# 等待所有后台 Docker 部署完成
wait

# 清理空的 maozi-cloud-services-image / -basics-image 目录
# (渲染产物由各服务动态生成的 ${service_name}-build-docker.sh 自清理, 留下两个空目录, 一并 rmdir 掉)
cleanup_image_dirs

echo "[deploy] all done"
