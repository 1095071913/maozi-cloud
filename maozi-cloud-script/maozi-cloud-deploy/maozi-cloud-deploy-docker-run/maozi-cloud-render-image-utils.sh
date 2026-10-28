#!/bin/bash

# ============================================================
# 镜像渲染器 (模板 + JSON -> Dockerfile, 然后构建并清理)
# ------------------------------------------------------------
# 被 maozi-cloud-deploy-jar-utils.sh / maozi-cloud-deploy-services-distributed-force.sh
# 通过 source 调用, 暴露 render_and_build_image 函数.
# 对应 bat 版: maozi-cloud-deploy-bat-run/maozi-cloud-deploy-bat-util/maozi-cloud-render-image.bat + .ps1
# ------------------------------------------------------------
# 设计要点:
#   - source 调用 (仿 maozi-cloud-scan-file-utils.sh), 不用 exit
#   - bash 3.2 兼容 (macOS 自带), 不用关联数组
#   - JSON 解析 + 模板渲染一次性在 python3 内完成, 不引入 jq 依赖,
#     也避免在 shell 里用 sed 处理含 URL / 反斜杠 / 特殊字符的字符串
#   - ENTRYPOINT (exec 数组写法, java 作为 PID 1 直接收 SIGTERM 优雅停机)
#     用单 __CMD_LINE__ 占位符整段替换, OTel 关闭时不会遗留孤立反斜杠
#   - 渲染产物 ${service_name}-image 由生成的 build-docker.sh 在构建后 rm 删除
#   - JSON 里 is_middleware=true 的服务 (中间件, 由独立 compose 编排) 统一 skip,
#     两个部署入口 (distributed / distributed-force) 因此都不会处理它们
#   - 渲染核心抽成顶层函数 render_image_dockerfile (只渲染, 不构建不启容器),
#     供 maozi-cloud-deploy-basics-all.sh 复用; render_and_build_image 是
#     "渲染 + buildx 构建 + compose 启动" 的完整封装
# ------------------------------------------------------------
# 单独运行时 (./maozi-cloud-render-image-utils.sh <service_name>) 进入 dry-run:
# 把渲染结果打到 stdout, 不构建, 不写文件, 便于人工核对模板渲染是否正确.
# ============================================================

# 从 compose 目录 .env 读取指定 key (缺失 / 文件不存在时回退默认值)
# .env 为纯 key=value 行, grep+cut 取值不引入解析依赖
read_env_key() {
    local file="$1" key="$2" default="$3" value
    value="$(grep -m1 "^${key}=" "$file" 2>/dev/null | cut -d= -f2-)"
    echo "${value:-$default}"
}

# ============================================================
# 渲染单个服务的 Dockerfile (模板 + JSON -> ${service_name}-image 内容)
# ------------------------------------------------------------
# 供 render_and_build_image 与 maozi-cloud-deploy-basics-all.sh 复用.
#   $1 = service_name (如 maozi-cloud-monitor-service)
#   $2 = out_path    ("-" = 打到 stdout, 即 dry-run)
#   $3 = 可选 skip-middleware 标志: 传入非空值时, is_middleware=true 的服务退出码 3
# 返回码: 0 成功 / 1 不在 JSON 里 / 环境缺失 / 渲染失败 / 3 skip-middleware 且 is_middleware=true
# 依赖调用方已设: current_directory
# ============================================================
render_image_dockerfile() {
    local service_name="$1"
    local out_path="$2"
    local skip_middleware="$3"

    local image_root="$current_directory/../maozi-cloud-deploy-docker-image/maozi-cloud-services-image"
    local config_file="$image_root/maozi-cloud-services.json"
    local template_file="$image_root/maozi-cloud-service-image.template"

    if [ ! -f "$config_file" ]; then
        echo "[render] config not found at $config_file" >&2
        return 1
    fi
    if [ ! -f "$template_file" ]; then
        echo "[render] template not found at $template_file" >&2
        return 1
    fi
    if ! command -v python3 >/dev/null 2>&1; then
        echo "[render] python3 not found in PATH" >&2
        return 1
    fi

    # python3 一次完成: 读 JSON 取配置 + 读模板 + 计算占位符 + 渲染输出
    python3 - "$service_name" "$config_file" "$template_file" "$out_path" "$skip_middleware" <<'PYEOF'
import json, sys

service_name    = sys.argv[1]
config_path     = sys.argv[2]
template_path   = sys.argv[3]
out_path        = sys.argv[4]
skip_middleware = sys.argv[5]

with open(config_path) as f:
    cfg = json.load(f)

d = cfg.get("defaults", {}) or {}
svc = None
for s in cfg.get("services", []):
    if s.get("service_name") == service_name:
        svc = s
        break
if svc is None:
    sys.stderr.write("service '%s' not in %s\n" % (service_name, config_path))
    sys.exit(1)

# skip-middleware (部署链路传入): is_middleware=true 的服务由 basics-all 独立编排,
# 退出码 3 让调用方打印 skip; 不传 (basics-all 自身 / dry-run) 则照常渲染
if skip_middleware and svc.get("is_middleware"):
    sys.exit(3)

otel      = bool(svc.get("opentelemetry"))
arthas    = bool(svc.get("arthas"))
jvm       = svc.get("jvm_params")  or d.get("jvm_params", "")
base      = svc.get("base_image")  or d.get("base_image", "maozi-cloud-base-jdk:1.0.0")

# OTel / Arthas / add-opens 标志全部从 JSON 读, 改参数不用动部署脚本
# 服务块里同名键会整体覆盖 defaults 里的列表 (不合并)
otel_flags   = svc.get("otel_flags")   or d.get("otel_flags", [])
arthas_flags = svc.get("arthas_flags") or d.get("arthas_flags", [])
add_opens    = svc.get("add_opens")    or d.get("add_opens", [])

# is_log_file: 默认 true (缺省 / null 均视为 true);
# true -> 渲染 ENV LOG_FILE 行, false -> 模板 __LOG_FILE_ENV__ 整行不渲染
is_log_file = svc.get("is_log_file")
if is_log_file is None:
    is_log_file = True

# ENTRYPOINT 行 (exec 数组写法): ["/bin/sh", "-c", "exec java -server [...] -jar"]
#   - 纯数组 ["java", ...] 不做变量展开, 而 ${JVM_PARAMS} 由 compose 运行时注入,
#     ${APPLICATION_NAME} 也要启动时展开, 故经 /bin/sh -c 执行
#   - 命令串前加 exec: java 顶替 sh 成为 PID 1, docker stop 的 SIGTERM 直达
#     JVM 触发 shutdown hooks 优雅停机 (裸 sh 作为 PID 1 不会转发信号)
#   - 续行 \ 由 Dockerfile 解析器在 JSON 解析前剥掉, 渲染结果仍是合法 JSON;
#     cmd_parts 每个元素独立占一行便于阅读, OTel 关闭时不会遗留孤立反斜杠
cmd_parts = ["java -server"]
if otel and otel_flags:
    cmd_parts.extend(otel_flags)
if arthas and arthas_flags:
    cmd_parts.extend(arthas_flags)
cmd_parts.append(jvm)
if add_opens:
    cmd_parts.extend(add_opens)
cmd_parts.append("-jar ${APPLICATION_NAME}.jar")
cmd_line = 'ENTRYPOINT ["/bin/sh", "-c", "exec ' + " \\\n  ".join(cmd_parts) + '"]'

otel_yes  = "yes" if otel      else "no"

with open(template_path) as f:
    tpl = f.read()

replacements = {
    "__BASE_IMAGE__":        base,
    "__SERVICE_NAME__":      service_name,
    "__OTEL_YES_NO__":       otel_yes,
    "__CMD_LINE__":          cmd_line,
    # key 含 \n: false 时连同换行一起替换为空, 不遗留空行
    "__LOG_FILE_ENV__\n":    ("ENV LOG_FILE=/application/${APPLICATION_NAME}/logs/${APPLICATION_NAME}.log\n"
                              if is_log_file else ""),
}
for k, v in replacements.items():
    tpl = tpl.replace(k, v)

if out_path == "-":
    sys.stdout.write(tpl)
else:
    with open(out_path, "w") as f:
        f.write(tpl)
PYEOF
}

# 渲染并构建单个服务的镜像
#   $1 = service_name (如 maozi-cloud-system-service)
#   $2 = module_dir   (相对仓库根, 如 maozi-cloud-business-system/maozi-cloud-system-service)
# 依赖调用方已设: current_directory, repo_root, route_image_dir, route_docker_dir
# 依赖调用方已 cd 到仓库根
render_and_build_image() {
    local service_name="$1"
    local module_dir="$2"

    # 路由镜像 / docker-compose 目录 (沿用 jar-utils 的前缀匹配规则)
    local image_directory docker_directory
    image_directory="$(route_image_dir "$service_name")"
    docker_directory="$(route_docker_dir "$service_name")"
    local image_file="$image_directory/${service_name}-image"

    # compose 文件名: business 目录已改用带前缀文件名, basics 目录仍为 docker-compose.yml
    local compose_file
    case "$service_name" in
        maozi-cloud-basics-*) compose_file="$docker_directory/docker-compose.yml" ;;
        *)                    compose_file="$docker_directory/maozi-cloud-services-distributeds-docker.yml" ;;
    esac

    # business 服务统一 compose 项目名
    # -p maozi-cloud-business-docker-${ENVIRONMENT:-${APPLICATION_ENVIRONMENT:-dev}}-${VERSION:-${APPLICATION_VERSION:-main}}:
    # compose 不对 -p 做变量插值, 按与 yml 插值相同的回退链解析 (调用方环境优先,
    # 其次 .env, 逐级回退 APPLICATION_* / dev / main) 后在生成临时脚本前拼好内嵌
    local compose_project=""
    case "$service_name" in
        maozi-cloud-basics-*) ;;
        *)
            local environment version
            environment="${ENVIRONMENT:-$(read_env_key "$docker_directory/.env" ENVIRONMENT "")}"
            environment="${environment:-${APPLICATION_ENVIRONMENT:-$(read_env_key "$docker_directory/.env" APPLICATION_ENVIRONMENT "")}}"
            environment="${environment:-dev}"
            version="${VERSION:-$(read_env_key "$docker_directory/.env" VERSION "")}"
            version="${version:-${APPLICATION_VERSION:-$(read_env_key "$docker_directory/.env" APPLICATION_VERSION "")}}"
            version="${version:-main}"
            compose_project="maozi-cloud-business-docker-${environment}-${version}"
            ;;
    esac

    # 父目录可能被误删 (例如静态 Dockerfile 清理后空目录被系统 / IDE 自动清理),
    # 渲染前确保存在, 否则 Python open(out_path, "w") 会抛 FileNotFoundError
    mkdir -p "$image_directory"

    # 渲染统一交给 render_image_dockerfile:
    #   - 读 maozi-cloud-services.json 取该服务的 OTel / Arthas / JVM / base_image 配置
    #   - 传 skip-middleware: is_middleware=true 的服务退出码 3 (由 basics-all 编排)
    # 退出码区分: 3 = is_middleware=true 跳过, 其他非零 = 不在 JSON 里 / 渲染失败
    render_image_dockerfile "$service_name" "$image_file" "skip-middleware"
    render_rc=$?
    if [ "$render_rc" -eq 3 ]; then
        echo "[render] skip $service_name: is_middleware=true, not managed by deploy scripts"
        return 1
    fi
    if [ "$render_rc" -ne 0 ]; then
        echo "[render] skip $service_name: not in config or render failed"
        return 1
    fi

    # 生成临时 build-docker.sh: cp jar / buildx / compose down+up -d / rm jar / rm Dockerfile / rm 自身
    # (compose down+up 均只针对本服务: 先停旧容器再启新容器)
    # 顺序: 必须先 buildx 再 compose, rm Dockerfile 必须在 buildx 之后 (否则构建找不到文件)
    # business 服务 compose 前先 cd 进 compose 目录 (v1 只从工作目录读 .env) 并带 -p 项目名
    local build_script="$image_directory/${service_name}-build-docker.sh"
    {
        echo "#!/bin/bash"
        echo "cp \"$repo_root/$module_dir/target/${service_name}.jar\" \"$image_directory/\""
        echo "cd \"$image_directory\""
        echo "docker buildx build -f \"$image_file\" -t \"${service_name}:latest\" ."
        if [ -n "$compose_project" ]; then
            echo "cd \"$docker_directory\""
            echo "docker-compose -p \"$compose_project\" -f \"$(basename "$compose_file")\" down ${service_name}"
            echo "docker-compose -p \"$compose_project\" -f \"$(basename "$compose_file")\" up -d ${service_name}"
        else
            echo "docker-compose -f \"$compose_file\" down ${service_name}"
            echo "docker-compose -f \"$compose_file\" up -d ${service_name}"
        fi
        echo "rm -f \"$image_directory/${service_name}.jar\""
        echo "rm -f \"$image_file\""
        echo "rm -f \"\$0\""
    } > "$build_script"
    chmod +x "$build_script"

    echo "[deploy] $service_name: rendering image, building and starting container"
    # 后台执行该 Docker 构建脚本, 多个服务可并行部署
    bash "$build_script" &
}

# ============================================================
# 部署完成后清理空的镜像目录
# ------------------------------------------------------------
# 渲染产物 ${service_name}-image 与 ${service_name}-build-docker.sh 由
# build-docker.sh 自身 rm 删除, 留下 maozi-cloud-services-image / -basics-image
# 两个空目录. 调用本函数把空目录一并清掉, 让仓库保持干净.
#
# 设计: 只用 rmdir (不 rm -rf), 如果目录非空 (说明有 build 失败遗留),
# rmdir 失败, 函数跳过该目录, 保留现场供排查.
# 依赖调用方已设: current_directory
# ============================================================
cleanup_image_dirs() {
    local image_root="$current_directory/../maozi-cloud-deploy-docker-image"
    local d
    for d in "$image_root/maozi-cloud-services-image" "$image_root/maozi-cloud-basics-image"; do
        if rmdir "$d" 2>/dev/null; then
            echo "[cleanup] removed empty $(basename "$d")/"
        fi
    done
}

# ============================================================
# 单独运行 (./maozi-cloud-render-image-utils.sh <service_name>): dry-run 模式
# 把渲染结果打到 stdout, 不写文件, 不构建. 用于人工核对模板渲染.
# source 调用时不执行此分支.
# ============================================================
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    service_name="$1"
    if [ -z "$service_name" ]; then
        echo "Usage: $0 <service_name>"
        echo "Render the Dockerfile for <service_name> to stdout (dry-run)."
        echo "Services are defined in maozi-cloud-deploy-docker-image/maozi-cloud-services-image/maozi-cloud-services.json"
        exit 1
    fi
    # 独立运行时 current_directory 是本脚本所在目录 (与入口脚本同目录, maozi-cloud-deploy-docker-run),
    # 向上一级即到 maozi-cloud-deploy, 与函数内 ../ 逻辑一致
    current_directory="$(cd "$(dirname "$0")" && pwd)"

    # 复用 render_image_dockerfile, 输出到 stdout (传 "-" 作为 out_path), 不构建不写文件
    # 不传 skip-middleware: dry-run 也可核对 is_middleware=true 服务的渲染结果
    render_image_dockerfile "$service_name" "-" || exit 1
fi
