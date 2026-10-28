#!/bin/bash

# ============================================================
# 基础组件 + Java 中间件服务一键部署 (单一 git 仓库版)
# ------------------------------------------------------------
# 职责划分:
#   - maozi-cloud-services.json 里 is_middleware=true 的 Java 中间件服务
#     (monitor / sentinel): 由本脚本负责 mvn 构建与镜像构建,
#     distributed / distributed-force 业务部署链路会跳过它们
#   - 基础组件 (mysql / redis / nacos / seata / xxljob / snailjob / 可观测性栈):
#     镜像直接从仓库拉取, 无需构建, 统一由 basics compose 拉起
# ------------------------------------------------------------
# 工作流程:
#   1. 读 maozi-cloud-services.json 取 is_middleware=true 的 service_name
#        无参: 全部中间件服务
#        有参: 仅取参数中匹配到的中间件服务, 其余参数只参与最后的 compose 启动
#   2. docker image inspect 判断 service_name:latest 镜像是否已存在
#   3. 镜像不存在的服务: mvn clean install -pl <module> -amd 增量构建 jar
#      (参数与 maozi-cloud-deploy-jar-utils.sh 一致: -T 2*核数 / compile fork / skip tests;
#       上游依赖 jar 从本地仓库解析, 首次使用前需跑过一次全量构建)
#   4. 参考渲染器流程: 模板 + JSON 渲染 Dockerfile, buildx 构建 service_name:latest,
#      只构建镜像不启动容器 (渲染产物与临时 jar 由生成的 build-docker.sh 自清理)
#   5. docker-compose -f maozi-cloud-basics-docker/docker-compose.yml up -d
#        无参: 启动 compose 内全部服务
#        有参: 仅启动参数指定的服务 (up -d 幂等, 镜像更新过会自动重建容器)
# ------------------------------------------------------------
# 用法:
#   ./maozi-cloud-deploy-basics-all.sh
#       全量: 补齐缺失的中间件镜像后启动整个 basics compose
#   ./maozi-cloud-deploy-basics-all.sh maozi-cloud-basic-monitor maozi-cloud-basic-mysql
#       定向: 仅处理指定服务; 参数须为 compose 里的服务名
#       (可用 docker-compose -f docker-compose.yml config --services 查看)
# ============================================================

# 切换到本脚本所在目录, 使后续相对路径可靠
cd "$(dirname "$0")"
current_directory="$(pwd)"

# 源码仓库根目录: 由脚本所在位置推导 (maozi-cloud-deploy-docker-run 向上三级);
# 当前结构下脚本位于 maozi-cloud-script/maozi-cloud-deploy 内, 源码仓库 maozi-cloud-parent 是 maozi-cloud-script 的同级目录,
# 向上三级无 pom.xml 时进入 maozi-cloud-parent (兼容脚本位于仓库内部的旧结构)
repo_candidate="$(cd "$current_directory/../../.." && pwd)"
if [ -f "$repo_candidate/pom.xml" ]; then
	repo_directory="$repo_candidate"
else
	repo_directory="$repo_candidate/maozi-cloud-parent"
fi
cd "$repo_directory"

# 可部署服务源码根目录 (相对仓库根, 与 jar-utils 一致)
services_subdir="maozi-cloud-service/maozi-cloud-services"

# basics compose 与镜像渲染配置
compose_file="$current_directory/../maozi-cloud-deploy-docker/maozi-cloud-basics-docker/docker-compose.yml"
config_file="$current_directory/../maozi-cloud-deploy-docker-image/maozi-cloud-services-image/maozi-cloud-services.json"

if [ ! -f "$compose_file" ]; then
    echo "[basics] compose not found at $compose_file"
    exit 1
fi
if [ ! -f "$config_file" ]; then
    echo "[basics] config not found at $config_file"
    exit 1
fi

# 渲染器: 提供 render_image_dockerfile (只渲染不启容器) 与 cleanup_image_dirs
source "$current_directory/maozi-cloud-render-image-utils.sh"

# ============================================================
# 1. 校验参数必须是 basics compose 里的服务名
# ============================================================
compose_services="$(docker-compose -f "$compose_file" config --services 2>/dev/null)"
if [ -z "$compose_services" ]; then
    echo "[basics] cannot read services from $compose_file (is docker-compose available?)"
    exit 1
fi

for arg in "$@"; do
    if ! echo "$compose_services" | grep -Fxq "$arg"; then
        echo "[basics] unknown service '$arg', valid services in compose:"
        echo "$compose_services" | sed 's/^/        /'
        exit 1
    fi
done

# ============================================================
# 2. 取需处理镜像的中间件服务 (JSON 里 is_middleware=true)
#    有参时仅保留参数指定的中间件服务, 其余参数 (如 mysql) 不需要构建镜像
# ============================================================
middleware_all="$(python3 -c '
import json, sys
with open(sys.argv[1]) as f:
    cfg = json.load(f)
for s in cfg.get("services", []):
    if s.get("is_middleware"):
        print(s["service_name"])
' "$config_file")"

middleware_targets=""
while IFS= read -r svc; do
    [ -z "$svc" ] && continue
    keep=1
    if [ "$#" -gt 0 ]; then
        keep=0
        for arg in "$@"; do
            if [ "$arg" = "$svc" ]; then
                keep=1
                break
            fi
        done
    fi
    if [ "$keep" -eq 1 ]; then
        middleware_targets="${middleware_targets:+$middleware_targets }$svc"
    fi
done < <(printf '%s\n' "$middleware_all")

# ============================================================
# 3. 过滤出 service_name:latest 镜像不存在的中间件服务
# ============================================================
build_services=""
for svc in $middleware_targets; do
    if docker image inspect "$svc:latest" >/dev/null 2>&1; then
        echo "[basics] image exists: $svc:latest"
    else
        echo "[basics] image missing: $svc:latest"
        build_services="${build_services:+$build_services }$svc"
    fi
done

# ============================================================
# 4. mvn 增量构建镜像缺失的服务 (-pl 模块 -amd, 参数与 jar-utils 一致)
# ============================================================
if [ -n "$build_services" ]; then

    # -pl 列表: maozi-cloud-service/maozi-cloud-services/<service_name>, 逗号分隔
    pl_list=""
    for svc in $build_services; do
        pl_list="${pl_list:+$pl_list,}$services_subdir/$svc"
    done

    # Maven 并行线程数按当前机器 CPU 核数动态计算: 2 * 核数
    cpu_cores="$(getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)"
    mvn_threads=$(( cpu_cores * 2 ))
    echo "[build] mvn -T $mvn_threads -pl $pl_list -amd"

    mvn clean install -T "$mvn_threads" -Dmaven.compile.fork=true -Dmaven.test.skip=true \
        -pl "$pl_list" -amd

    # Maven 失败立即中止, 不进入镜像构建阶段 (避免用旧 jar 重建镜像)
    if [ $? -ne 0 ]; then
        echo "[build] FAILED: mvn clean install error, abort"
        exit 1
    fi
fi

# ============================================================
# 5. 渲染 Dockerfile + buildx 构建镜像 (不启动容器, 容器统一由第 6 步 compose 拉起)
# ============================================================
# 锁定仓库根的绝对路径, 让生成的临时脚本里的 cp 源路径不依赖 cwd
repo_root="$(pwd)"
build_count=0

for service_name in $build_services; do
    module_dir="$services_subdir/$service_name"

    # mvn 产物校验: jar 不存在说明模块构建异常, 跳过该服务的镜像构建
    if [ ! -f "$repo_root/$module_dir/target/${service_name}.jar" ]; then
        echo "[basics] skip $service_name: jar not found at $module_dir/target/${service_name}.jar"
        continue
    fi

    # 渲染 Dockerfile (复用渲染器; 不传 skip-middleware, 中间件服务照常渲染)
    image_directory="$current_directory/../maozi-cloud-deploy-docker-image/maozi-cloud-services-image"
    image_file="$image_directory/${service_name}-image"
    mkdir -p "$image_directory"

    if ! render_image_dockerfile "$service_name" "$image_file"; then
        echo "[basics] skip $service_name: render failed"
        continue
    fi

    # 生成临时 build-docker.sh: cp jar / buildx / rm jar / rm Dockerfile / rm 自身
    # 与 render_and_build_image 生成的脚本区别: 不执行 compose (容器由本脚本最后统一拉起)
    build_script="$image_directory/${service_name}-build-docker.sh"
    {
        echo "#!/bin/bash"
        echo "cp \"$repo_root/$module_dir/target/${service_name}.jar\" \"$image_directory/\""
        echo "cd \"$image_directory\""
        echo "docker buildx build -f \"$image_file\" -t \"${service_name}:latest\" ."
        echo "rm -f \"$image_directory/${service_name}.jar\""
        echo "rm -f \"$image_file\""
        echo "rm -f \"\$0\""
    } > "$build_script"
    chmod +x "$build_script"

    echo "[basics] $service_name: rendering image and building (no container start)"
    # 后台执行该构建脚本, 多个服务可并行构建
    bash "$build_script" &
    build_count=$((build_count + 1))
done

echo "[basics] images in flight: $build_count"

# 等待所有后台镜像构建完成
wait

# 清理空的 maozi-cloud-services-image / -basics-image 目录
cleanup_image_dirs

# ============================================================
# 6. 启动 basics compose (无参: 全部服务; 有参: 仅指定服务)
# ============================================================
if [ "$#" -gt 0 ]; then
    echo "[basics] docker-compose up -d $*"
    docker-compose -f "$compose_file" up -d "$@"
else
    echo "[basics] docker-compose up -d (all services)"
    docker-compose -f "$compose_file" up -d
fi

echo "[basics] all done"
