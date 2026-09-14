@echo off
chcp 65001 > nul

REM ============================================================
REM 本文件用于被 call 执行, 对应 shell 版 maozi-cloud-scan-file-utils.sh
REM 请勿直接运行, 也不要使用 exit /b 终止调用方 bat
REM ------------------------------------------------------------
REM 作用: 在 maozi-cloud-parent 仓库根目录比对分支与提交, 判定本次构建范围
REM   通过脚本目录下的 maozi-cloud-deploy-services-distributed-record 文件夹记录上次构建状态:
REM     CURRENT_BRANCH  上次构建时的分支
REM     CURRENT_SHA     上次构建时的提交 SHA
REM   并据此输出给 jar-utils:
REM     build_mode           full / incremental / none
REM     changed_files_file   增量模式下指向记录变更文件的临时文件 (每行一个)
REM ------------------------------------------------------------
REM 四种判定:
REM   A. 无 CURRENT_BRANCH 记录: 首次构建, 全量
REM   B. 分支与记录不一致:       切换了分支, 全量
REM   C. 分支一致但 SHA 变化:    同分支有新提交, 增量
REM   D. 分支与 SHA 均一致:      无变化, 跳过
REM ------------------------------------------------------------
REM 依赖外部变量: current_directory  (由调用方 jar-utils 传入的脚本目录)
REM 前置: cwd 已切换到 maozi-cloud-parent 仓库根目录
REM 修改外部变量: build_mode, changed_files_file
REM 顶层不使用 setlocal, 让 build_mode 等结果直接泄露给调用方作用域
REM ============================================================

REM 重置输出变量 (避免被上一次 call 残留污染)
set build_mode=none
set changed_files_file=%TEMP%\maozi-changed-files.tmp
if exist "%changed_files_file%" del /q "%changed_files_file%" > nul 2>&1

REM 读取当前 git 分支与 HEAD SHA
set current_branch=
set current_sha=
for /f "delims=" %%i in ('git branch --show-current 2^>nul') do set current_branch=%%i
for /f "delims=" %%i in ('git rev-parse HEAD 2^>nul') do set current_sha=%%i

REM env 状态目录: 统一存放 maozi-cloud-parent 仓库的构建状态
set env_directory=%current_directory%\maozi-cloud-deploy-services-distributed-record
if not exist "%env_directory%" mkdir "%env_directory%"

set branch_file=%env_directory%\CURRENT_BRANCH
set sha_file=%env_directory%\CURRENT_SHA

if exist "%branch_file%" goto :has_record
REM ---- A. 无记录: 首次构建, 全量 ----
set build_mode=full
goto :save_state

:has_record
REM 读取上次记录的分支并去除空白 (对应 shell 的 tr -d '[:space:]')
set file_branch=
set /p file_branch=<"%branch_file%"
set file_branch=%file_branch: =%

if /i not "%current_branch%"=="%file_branch%" goto :branch_diff
REM ---- C. 分支一致: 比较 SHA 判断是否有新提交 ----
set file_sha=
set /p file_sha=<"%sha_file%"
set file_sha=%file_sha: =%

if /i not "%current_sha%"=="%file_sha%" goto :sha_diff
REM ---- D. 分支与 SHA 均一致: 无变化 ----
set build_mode=none
goto :save_state

:branch_diff
REM ---- B. 分支不一致: 切换了分支, 全量构建 ----
set build_mode=full
goto :save_state

:sha_diff
REM 有新提交: 增量构建, 用 git diff 列出变更文件 (相对仓库根)
set build_mode=incremental
git diff --name-only %file_sha%..%current_sha% > "%changed_files_file%" 2>nul
goto :save_state

:save_state
REM 记录本次构建的状态 (与原脚本一致, 无论后续 mvn 是否成功都更新到当前)
> "%branch_file%" echo %current_branch%
> "%sha_file%" echo %current_sha%
goto :eof
