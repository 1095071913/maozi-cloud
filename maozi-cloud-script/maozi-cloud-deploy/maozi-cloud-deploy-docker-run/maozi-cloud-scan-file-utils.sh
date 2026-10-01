# ============================================================
# 本文件用于被 source 执行, 对应 maozi-cloud-deploy-bat-run/maozi-cloud-deploy-bat-util/maozi-cloud-scan-file-utils.bat
# 请勿直接运行, 也不要使用 exit (否则会退出调用方 shell)。
# ------------------------------------------------------------
# 作用: 在 maozi-cloud-parent 仓库根目录比对分支与提交, 判定本次构建范围
#   通过脚本目录下的 maozi-cloud-deploy-services-distributed-record 文件夹记录上次构建状态:
#     CURRENT_BRANCH  上次构建时的分支
#     CURRENT_SHA     上次构建时的提交 SHA
#   并据此输出给 jar-utils:
#     build_mode      full / incremental / none
#     changed_files[] 增量模式下的变更文件 (相对仓库根的路径)
# ------------------------------------------------------------
# 四种判定:
#   A. 无 CURRENT_BRANCH 记录: 首次构建, 全量
#   B. 分支与记录不一致:       切换了分支, 全量
#   C. 分支一致但 SHA 变化:    同分支有新提交, 增量
#   D. 分支与 SHA 均一致:      无变化, 跳过
# ------------------------------------------------------------
# 依赖外部变量: current_directory  (由调用方 jar-utils 传入的脚本目录)
# 前置: cwd 已切换到 maozi-cloud-parent 仓库根目录
# 修改外部变量: build_mode, changed_files[]
# ============================================================

# 读取当前 git 分支与 HEAD SHA
current_branch="$(git branch --show-current)"
current_sha="$(git rev-parse HEAD)"

# env 状态目录: 统一存放 maozi-cloud-parent 仓库的构建状态
# 与原多仓库脚本不同, 现在整个项目合并为单一 git 仓库, 只保留一份状态
env_directory="$current_directory/maozi-cloud-deploy-services-distributed-record"
mkdir -p "$env_directory"

branch_file="$env_directory/CURRENT_BRANCH"
sha_file="$env_directory/CURRENT_SHA"

# 重置输出变量 (避免被上一次 source 残留污染)
build_mode="none"
changed_files=()

if [ -f "$branch_file" ]; then

    # 读取上次记录的分支并去除空白 (tr -d '[:space:]' 等价 bat 的 !var: =!)
    file_branch="$(tr -d '[:space:]' < "$branch_file")"

    if [ "$current_branch" != "$file_branch" ]; then

        # ---- B. 分支不一致: 切换了分支, 全量构建 ----
        build_mode="full"

    else

        # ---- C. 分支一致: 比较 SHA 判断是否有新提交 ----
        file_sha="$(tr -d '[:space:]' < "$sha_file")"

        if [ "$current_sha" != "$file_sha" ]; then

            # 有新提交: 增量构建, 用 git diff 列出变更文件 (相对仓库根)
            build_mode="incremental"

            while IFS= read -r line; do
                [ -z "$line" ] && continue
                changed_files+=("$line")
            done < <(git diff --name-only "$file_sha..$current_sha")

        fi

    fi

else

    # ---- A. 无记录: 首次构建, 全量 ----
    build_mode="full"

fi

# 记录本次构建的状态 (与原脚本一致, 无论后续 mvn 是否成功都更新到当前)
echo "$current_branch" > "$branch_file"
echo "$current_sha" > "$sha_file"
