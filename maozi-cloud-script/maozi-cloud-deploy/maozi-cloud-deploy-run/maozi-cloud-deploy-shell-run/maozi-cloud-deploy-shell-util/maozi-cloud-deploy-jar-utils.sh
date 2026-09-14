#!/bin/bash

# ============================================================
# 统一构建逻辑 (单一 git 仓库版), 由 maozi-cloud-deploy-services-distributed.sh 调用
# 对应 bat 版: maozi-cloud-deploy-bat-run/maozi-cloud-deploy-bat-util/maozi-cloud-deploy-jar-utils.bat
# ------------------------------------------------------------
# 兼容性: macOS 自带 bash 3.2 不支持关联数组 (declare -A),
#          故本脚本所有 mtime 快照 / 模块去重均用临时文件 + awk / grep 实现,
#          不依赖 bash 4+ 特性。
# ------------------------------------------------------------
# 参数: $1 = current_directory (调用方脚本目录, 作为相对路径锚点)
# 前置: cwd 已由调用方切换到 maozi-cloud-parent 仓库根目录
# ------------------------------------------------------------
# 工作流程:
#   A. source scan-file-utils 比对 git 分支 / SHA, 得到 build_mode
#        full          -> 全量构建整个 reactor
#        incremental   -> 按 changed_files 归并出 Maven 模块列表
#        none          -> 无变化, 直接退出
#   B. 拍摄 maozi-cloud-services 下所有 jar 的 mtime 快照 (构建前)
#        存入临时文件, 每行 "<jar 完整路径>|<mtime epoch>"
#   C. 归并 changed_files 为 mvn -pl 模块路径列表
#        - 源文件: 截取 /src/ 之前的部分作为模块路径
#        - pom.xml: 取所在目录, 仓库根 pom 触发全量
#        - 其他非 Maven 文件 (脚本/文档等): 忽略
#        去重通过临时文件 + grep -Fxq 实现, 不使用关联数组
#   D. mvn clean + mvn install -pl <modules> -amd
#        -amd 自动让 "a 引用 b, b 修改 -> a 一起构建" 成立
#   E. 拍摄构建后的 jar mtime 快照, 用 awk 对比前后两份文件,
#        找出 mtime 变化 (或新增) 的 jar
#        只有这些 jar 才视为 "被 Maven 实际编译并生成", 进入 Docker 部署
#   F. 对每个变化的 jar:
#        - 按模块名前缀路由镜像目录 (basics-* -> maozi-cloud-basics-image 等)
#        - 调用 render-image 渲染器: 读 maozi-cloud-services.json, 按模板渲染
#          ${service_name}-image, buildx 构建, compose up -d, 最后 rm 镜像文件
#        - 服务不在 JSON 配置里时, 渲染器自动 skip, 不阻塞其他服务
#        - 后台执行, 最后 wait 等待所有部署完成
# ------------------------------------------------------------
# 模块结构说明 (business 服务):
#   业务服务采用聚合分层, 一个业务在 maozi-cloud-services 下聚合为:
#     maozi-cloud-business-xxx/
#       ├── maozi-cloud-xxx-api       对外 API 契约 (产出带版本 jar, 不部署)
#       ├── maozi-cloud-xxx-business  业务实现 (产出带版本 jar, 不部署)
#       └── maozi-cloud-xxx-service   可执行启动模块 (产出 maozi-cloud-xxx-service.jar, 部署入口)
#   find 会扫描到上述全部 jar, 但只有 *-service 模块在 maozi-cloud-services.json 里登记,
#   api / business 的 jar 因不在 JSON 里会在 F 步被渲染器自动跳过.
#   basics 层 (gateway-service / monitor-service) 与 all-service 仍为扁平单模块, 行为不变。
# ============================================================

current_directory="$1"

# 可部署服务源码根目录 (相对仓库根)
# 仅此目录下生成的 jar 才会触发 Docker 重新部署
services_subdir="maozi-cloud-service/maozi-cloud-services"

# ============================================================
# 工具函数
# ============================================================

# 跨平台读取文件 mtime (秒级 epoch)
#   macOS BSD stat:  stat -f %m file
#   Linux GNU stat:  stat -c %Y file
get_mtime() {
    stat -f %m "$1" 2>/dev/null || stat -c %Y "$1" 2>/dev/null
}

# 写入 mtime 快照到文件, 每行 "jar路径|mtime"
# 用 process substitution 避免管道 while 进入 subshell 导致变量丢失
write_snapshot() {
    local output="$1"
    : > "$output"
    local jarfile
    while IFS= read -r -d '' jarfile; do
        echo "${jarfile}|$(get_mtime "$jarfile")"
    done < <(find "$services_subdir" -name "*.jar" -print0 2>/dev/null) >> "$output"
}

# 根据服务名前缀路由镜像目录
#   maozi-cloud-basics-* -> maozi-cloud-basics-image
#   其他 (含 gateway / monitor / system / oauth 等业务服务) -> maozi-cloud-services-image
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

# ============================================================
# 准备临时文件 + 退出时清理
# ============================================================
before_manifest="$(mktemp -t maozi-jar-before)"
after_manifest="$(mktemp -t maozi-jar-after)"
changed_jars_list="$(mktemp -t maozi-jar-changed)"
modules_file="$(mktemp -t maozi-modules)"
trap 'rm -f "$before_manifest" "$after_manifest" "$changed_jars_list" "$modules_file"' EXIT

# ---- A. 比对 git 状态 ----
# 工具脚本位于入口脚本目录下的 maozi-cloud-deploy-shell-util 子目录,
# current_directory 由调用方传入, 指向入口脚本目录 (maozi-cloud-deploy-shell-run)
source "$current_directory/maozi-cloud-deploy-shell-util/maozi-cloud-scan-file-utils.sh"
# 镜像渲染器: 模板 + JSON -> ${service_name}-image, 然后 buildx + compose, 最后清理
source "$current_directory/maozi-cloud-deploy-shell-util/maozi-cloud-render-image.sh"

echo "[build] mode=$build_mode"

# 无变更: 直接退出, 不触发 Maven 与 Docker
if [ "$build_mode" = "none" ]; then
    exit 0
fi

# ---- B. 拍摄构建前 jar mtime 快照 ----
write_snapshot "$before_manifest"

# ---- C. 归并 changed_files 为 Maven 模块路径列表 ----
build_files=""
: > "$modules_file"

if [ "$build_mode" = "full" ]; then

    # 全量构建整个 reactor
    build_files="."

else

    # 增量构建: 把变更文件归并为模块路径, 写入 modules_file 做去重
    for file in "${changed_files[@]}"; do

        module_path=""

        if [[ "$file" == */src/* ]]; then

            # 源文件: 截取首个 /src/ 之前的部分作为模块路径
            module_path="${file%%/src/*}"

        elif [[ "$file" == "pom.xml" ]]; then

            # 仓库根 pom.xml 变更: 影响整个 reactor, 直接全量
            build_files="."
            break

        elif [[ "$file" == */pom.xml ]]; then

            # 子模块 / 聚合 pom.xml 变更: 取所在目录作为模块路径
            module_path="$(dirname "$file")"

        elif [ -d "$file" ] && [ -f "$file/pom.xml" ]; then

            # 路径本身就是一个含 pom.xml 的目录, 通常是 git submodule 指针变化
            # (git diff 只返回子模块目录路径, 不返回子模块内部文件)
            module_path="$file"

        fi

        # 其他非 Maven 文件 (脚本 / 文档 / .idea 等): module_path 为空, 忽略

        # 必须真实存在 pom.xml 才算 Maven 模块
        if [ -n "$module_path" ] && [ -f "$module_path/pom.xml" ]; then
            # grep -Fxq 精确整行匹配实现去重 (避免关联数组)
            if ! grep -Fxq -- "$module_path" "$modules_file" 2>/dev/null; then
                echo "$module_path" >> "$modules_file"
            fi
        fi

    done

    # 合并模块路径为逗号分隔的 -pl 参数, build_files 已置 "." 则跳过
    if [ "$build_files" != "." ] && [ -s "$modules_file" ]; then
        build_files="$(paste -sd ',' "$modules_file")"
    fi

fi

# ---- D. 执行 Maven 构建 ----
if [ -n "$build_files" ]; then

    # Maven 并行线程数按当前机器 CPU 核数动态计算: 2 * 核数
    # (getconf 同时支持 macOS / Linux, sysctl / nproc 作兜底)
    cpu_cores="$(getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)"
    mvn_threads=$(( cpu_cores * 2 ))
    echo "[build] mvn -T $mvn_threads -pl $build_files -amd"

    # 增量安装指定模块; -amd 同时构建依赖于它们的下游模块
    # 这一步实现了 "a 引用 b, b 修改 -> a 也重新构建"
    mvn clean install -T "$mvn_threads" -Dmaven.compile.fork=true -Dmaven.test.skip=true \
        -pl "$build_files" -amd

fi

# ---- E. 拍摄构建后 jar mtime 快照, 找出实际更新的 jar ----
write_snapshot "$after_manifest"

# awk: 第一遍读 before 建立 path->mtime 映射, 第二遍读 after 输出 mtime 变化或新增的 jar
# 完全不依赖 bash 关联数组, awk 自己的关联数组在所有平台都支持
# 规避 ! 字符: 部分外壳 (zsh history expansion 等) 在传给 awk 时会错误转义 !,
# 用 "k in before && before[k] == v 则 next, 否则 print" 等价语义改写
awk -F'|' '
    NR==FNR { before[$1] = $2; next }
    {
        k = $1
        v = $2
        if ((k in before) && (before[k] == v)) {
            next
        }
        print k
    }
' "$before_manifest" "$after_manifest" > "$changed_jars_list"

changed_count="$(wc -l < "$changed_jars_list" | tr -d '[:space:]')"
echo "[deploy] changed jars: $changed_count"

# ---- F. 对每个变化的 jar 触发 Docker 部署 ----
# 锁定仓库根的绝对路径, 让生成的临时脚本里的 cp 源路径不依赖 cwd
repo_root="$(pwd)"

while IFS= read -r jarfile; do

    [ -z "$jarfile" ] && continue

    # dirname 两次: jar -> target -> 模块目录
    # business 服务: 得到 maozi-cloud-business-xxx/maozi-cloud-xxx-service (启动模块)
    # basics 服务:   得到 maozi-cloud-gateway-service / maozi-cloud-monitor-service (扁平单模块)
    module_dir="$(dirname "$(dirname "$jarfile")")"
    service_name="$(basename "$module_dir")"

    # 渲染 + 构建 + 清理统一交给 maozi-cloud-render-image.sh:
    #   - 读 maozi-cloud-services.json 取该服务的端口 / Dubbo / OTel / JVM / base_image
    #   - 按模板渲染出 ${service_name}-image, buildx 构建, compose 启动, rm 镜像文件
    # 服务不在 JSON 配置里时, 渲染器返回非零并打印 skip, 不阻塞其他服务
    render_and_build_image "$service_name" "$module_dir"

done < "$changed_jars_list"

# 等待所有后台 Docker 部署完成
wait

# 清理空的 maozi-cloud-services-image / -basics-image 目录
# (渲染产物由各服务动态生成的 ${service_name}-build-docker.sh 自清理, 留下两个空目录, 一并 rmdir 掉)
cleanup_image_dirs
