#!/bin/bash

# ============================================================
# 自动部署入口 (单一 git 仓库版)
# 对应 bat 版: maozi-cloud-deploy-bat-run/maozi-cloud-deploy-services-distributed.bat
# ------------------------------------------------------------
# 项目原先拆分为 parent / basics / services 三个独立 git 仓库,
# 现已合并为单一 git 仓库 (maozi-cloud-parent), 本脚本作为唯一入口:
#   1. 切换到 maozi-cloud-parent 仓库根目录
#   2. 调用 maozi-cloud-deploy-jar-utils.sh 完成状态比对 / Maven 构建 / Docker 部署
# ------------------------------------------------------------
# 构建判定逻辑:
#   - 分支与上次记录不一致 -> 全量构建 + 全量 Docker 部署
#   - 分支一致 SHA 不一致  -> git diff 取变更文件, 按模块 -pl -amd 增量构建,
#                             仅对实际生成 jar 的服务做 Docker 重启
#   - 分支与 SHA 均一致    -> 跳过
# ============================================================

# 切换到本脚本所在目录, 使后续相对路径 (jar-utils / scan-utils) 可靠
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

# 切换到仓库根目录, 后续 git / mvn 命令均在此执行
cd "$repo_directory"

# 调用统一构建逻辑, 把脚本目录作为参数传入, 用于定位 scan-file-utils 与 image / docker 目录
bash "$current_directory/maozi-cloud-deploy-shell-util/maozi-cloud-deploy-jar-utils.sh" "$current_directory"
