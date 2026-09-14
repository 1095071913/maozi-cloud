@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================================
REM 统一构建逻辑 (单一 git 仓库版), 由 maozi-cloud-deploy-services-distributed.bat 调用
REM 对应 shell 版: maozi-cloud-deploy-shell-run/maozi-cloud-deploy-shell-util/maozi-cloud-deploy-jar-utils.sh
REM ------------------------------------------------------------
REM 参数: %~1 = current_directory (调用方脚本目录, 作为相对路径锚点)
REM 前置: cwd 已由调用方切换到 maozi-cloud-parent 仓库根目录
REM ------------------------------------------------------------
REM 工作流程:
REM   A. call scan-file-utils 比对 git 分支 / SHA, 得到 build_mode
REM        full          -> 全量构建整个 reactor
REM        incremental   -> 按 changed_files 归并出 Maven 模块列表
REM        none          -> 无变化, 直接退出
REM   B. 拍摄 maozi-cloud-services 下所有 jar 的 mtime 快照 (构建前)
REM        存入临时文件, 每行 "<jar 完整路径>|<mtime 字符串>"
REM   C. 归并 changed_files 为 mvn -pl 模块路径列表
REM        - 源文件: 截取 \src\ 之前的部分作为模块路径
REM        - pom.xml: 取所在目录, 仓库根 pom 触发全量
REM        - 其他非 Maven 文件 (脚本/文档等): 忽略
REM        去重通过临时文件 + findstr /x 实现
REM   D. mvn clean + mvn install -pl <modules> -amd
REM   E. 拍摄构建后的 jar mtime 快照, 对比前后两份文件,
REM        找出 mtime 变化 (或新增) 的 jar, 进入 Docker 部署
REM   F. 对每个变化的 jar:
REM        - 调用 render-image 渲染器: 读 maozi-cloud-services.json, 按模板渲染
REM          ${service_name}-image, buildx 构建, compose up -d, 最后 del 镜像文件
REM        - 服务不在 JSON 配置里时, 渲染器自动 skip, 不阻塞其他服务
REM        - 后台 start /b 派发, 末尾统一 :wait_all_deploys 等待 .done 出现
REM ============================================================

set current_directory=%~1

REM 可部署服务源码根目录 (相对仓库根, 路径分隔符已转为 Windows 反斜杠)
set services_subdir=maozi-cloud-service\maozi-cloud-services

REM 准备临时文件
set before_manifest=%TEMP%\maozi-jar-before.tmp
set after_manifest=%TEMP%\maozi-jar-after.tmp
set changed_jars_list=%TEMP%\maozi-jar-changed.tmp
set modules_file=%TEMP%\maozi-modules.tmp
for %%F in ("%before_manifest%" "%after_manifest%" "%changed_jars_list%" "%modules_file%") do (
    if exist "%%~F" del /q "%%~F" > nul 2>&1
)

REM ---- A. 比对 git 状态 (call scan-file-utils 设置 build_mode / changed_files_file) ----
call "%current_directory%\maozi-cloud-deploy-bat-util\maozi-cloud-scan-file-utils.bat"

REM 镜像渲染器: 初始化 (生成 Python 脚本 + 哨兵目录 + deploy_services_list)
call "%current_directory%\maozi-cloud-deploy-bat-util\maozi-cloud-render-image.bat"

echo [build] mode=!build_mode!

REM 无变更: 直接退出, 不触发 Maven 与 Docker
if /i "!build_mode!"=="none" goto :done

REM ---- B. 拍摄构建前 jar mtime 快照 ----
call :write_snapshot "%before_manifest%"

REM ---- C. 归并 changed_files 为 Maven 模块路径列表 ----
set build_files=
set force_full=0

if /i not "!build_mode!"=="full" goto :aggregate_incremental
REM 全量构建整个 reactor
set build_files=.
goto :maven_build

:aggregate_incremental
if not exist "!changed_files_file!" goto :maven_build
REM 增量构建: 把变更文件归并为模块路径, 写入 modules_file 做去重
for /f "usebackq tokens=* delims=" %%f in ("!changed_files_file!") do (
    if "!force_full!"=="0" call :process_changed_file "%%f"
)
if "!force_full!"=="1" (
    set build_files=.
    goto :maven_build
)
REM 合并模块路径为逗号分隔的 -pl 参数
set first=1
for /f "usebackq tokens=* delims=" %%m in ("%modules_file%") do (
    if !first!==1 (
        set build_files=%%m
        set first=0
    ) else (
        set build_files=!build_files!,%%m
    )
)

:maven_build
REM ---- D. 执行 Maven 构建 ----
if not defined build_files goto :after_build
if "!build_files!"=="" goto :after_build
REM Maven 并行线程数按当前机器 CPU 核数动态计算: 2 * 核数
REM %NUMBER_OF_PROCESSORS% 为 Windows 自动设置的逻辑核数, 极端环境未定义时兜底 4
if not defined NUMBER_OF_PROCESSORS set NUMBER_OF_PROCESSORS=4
set /a MVN_THREADS=%NUMBER_OF_PROCESSORS% * 2
echo [build] mvn -T %MVN_THREADS% -pl !build_files! -amd
call mvn clean install -T %MVN_THREADS% -Dmaven.compile.fork=true -Dmaven.test.skip=true -pl "!build_files!" -amd
if errorlevel 1 (
    echo [build] FAILED: mvn install error, abort
    goto :fail
)

:after_build
REM ---- E. 拍摄构建后 jar mtime 快照, 找出实际更新的 jar ----
call :write_snapshot "%after_manifest%"

REM 对比前后两份快照: 在 after 但不在 before / 在前后但 mtime 不同 -> 视为变化
if exist "%changed_jars_list%" del /q "%changed_jars_list%" > nul 2>&1
if not exist "%after_manifest%" goto :deploy_phase
for /f "usebackq tokens=1,* delims=|" %%a in ("%after_manifest%") do (
    call :diff_jar "%%a" "%%b"
)

set changed_count=0
if exist "%changed_jars_list%" for /f %%n in ('find /c /v "" ^< "%changed_jars_list%"') do set changed_count=%%n
echo [deploy] changed jars: !changed_count!

:deploy_phase
REM ---- F. 对每个变化的 jar 触发 Docker 部署 ----
REM 锁定仓库根的绝对路径, 让生成的临时脚本里的 cp 源路径不依赖 cwd
set repo_root=%CD%

if exist "%changed_jars_list%" for /f "usebackq tokens=* delims=" %%j in ("%changed_jars_list%") do (
    call :deploy_jar "%%j"
)

REM 等待所有后台 Docker 部署完成 (= shell 的 wait)
call :wait_all_deploys

REM 清理空的 maozi-cloud-services-image / -basics-image 目录
call "%current_directory%\maozi-cloud-deploy-bat-util\maozi-cloud-render-image.bat" cleanup_image_dirs

:done
REM 清理临时文件
for %%F in ("%before_manifest%" "%after_manifest%" "%changed_jars_list%" "%modules_file%") do (
    if exist "%%~F" del /q "%%~F" > nul 2>&1
)
endlocal & exit /b 0

:fail
for %%F in ("%before_manifest%" "%after_manifest%" "%changed_jars_list%" "%modules_file%") do (
    if exist "%%~F" del /q "%%~F" > nul 2>&1
)
endlocal & exit /b 1


REM ============================================================
REM 写入 mtime 快照到文件, 每行 "jar路径|mtime"
REM mtime 用 %%~tf 取 last-write-time 字符串 (区域相关但本机一致即可)
REM ============================================================
:write_snapshot
setlocal enabledelayedexpansion
set ws_output=%~1
if exist "%ws_output%" del /q "%ws_output%" > nul 2>&1
if not exist "%services_subdir%" (
    endlocal & exit /b 0
)
for /r "%services_subdir%" %%f in (*.jar) do (
    >> "%ws_output%" echo %%f^|%%~tf
)
endlocal & exit /b 0


REM ============================================================
REM 处理单个 changed_file, 计算所属 module_path 并写入 modules_file 做去重
REM 触发 force_full=1 时, 调用方会跳出循环改用全量构建
REM ============================================================
:process_changed_file
setlocal enabledelayedexpansion
set pc_file=%~1
REM git diff --name-only 在 Windows 上也用正斜杠输出路径,
REM 统一替换成反斜杠, 让后续 findstr / if exist / dirname 都能用 Windows 风格匹配
set pc_file=!pc_file:/=\!
set pc_module_path=

REM 仓库根 pom.xml 变更: 影响整个 reactor, 直接全量
if /i "!pc_file!"=="pom.xml" (
    endlocal & set force_full=1
    exit /b 0
)

REM 判断是否源文件: 路径里含 \src\
echo !pc_file! | findstr /i "\\src\\" > nul
if errorlevel 1 goto :check_pom
REM 源文件: 截取 \src\ 之前的部分作为模块路径
call :strip_src_suffix "!pc_file!"
set pc_module_path=!strip_result!

:check_pom
REM 子模块 / 聚合 pom.xml 变更: 取所在目录作为模块路径
if defined pc_module_path goto :check_dir
echo !pc_file! | findstr /i "\\pom.xml$" > nul
if errorlevel 1 goto :check_dir
for %%D in ("!pc_file!") do set pc_module_path=%%~dpD
if "!pc_module_path:~-1!"=="\" set pc_module_path=!pc_module_path:~0,-1!

:check_dir
REM 路径本身就是一个含 pom.xml 的目录 (git submodule 指针变化)
if defined pc_module_path goto :verify
if exist "!pc_file!\pom.xml" set pc_module_path=!pc_file!

:verify
REM 必须真实存在 pom.xml 才算 Maven 模块
if not defined pc_module_path (
    endlocal & exit /b 0
)
if not exist "!pc_module_path!\pom.xml" (
    endlocal & exit /b 0
)

REM findstr /x 精确整行匹配实现去重 (避免关联数组)
findstr /x /c:"!pc_module_path!" "%modules_file%" > nul 2>&1
if errorlevel 1 (
    >> "%modules_file%" echo !pc_module_path!
)
endlocal & exit /b 0


REM ============================================================
REM 把 ".../module/sub/src/main/.../X.java" 路径里 \src\ 及之后部分去掉
REM 结果放入 strip_result (相对仓库根的反斜杠路径)
REM ============================================================
:strip_src_suffix
setlocal enabledelayedexpansion
set strip_input=%~1
set strip_pos=-1
set strip_idx=0

:strip_src_loop
set strip_test=!strip_input:~%strip_idx%,5!
if /i "!strip_test!"=="\src\" set strip_pos=!strip_idx!
set /a strip_idx+=1
if "!strip_input:~%strip_idx%,1!" neq "" goto :strip_src_loop

if !strip_pos! lss 0 (
    endlocal & set strip_result=
    exit /b 0
)
set /a strip_len=strip_pos
set strip_result=!strip_input:~0,%strip_len%!
endlocal & set strip_result=%strip_result%
exit /b 0


REM ============================================================
REM 在 :after_build 循环里对每条 after_manifest 行做 before/after 比对
REM   %1 = jar_path, %2 = jar_time (mtime 字符串)
REM 若 before 不存在该 jar, 或 mtime 不同, 则追加到 changed_jars_list
REM ============================================================
:diff_jar
setlocal enabledelayedexpansion
set dj_path=%~1
set dj_time=%~2

REM 在 before 里精确整行匹配 "path|"
findstr /b /c:"!dj_path!|" "%before_manifest%" > nul 2>&1
if errorlevel 1 (
    REM 不在 before -> 新生成的 jar
    >> "%changed_jars_list%" echo !dj_path!
    endlocal & exit /b 0
)
REM 在 before, 检查 mtime 是否变化
findstr /b /c:"!dj_path!|!dj_time!" "%before_manifest%" > nul 2>&1
if errorlevel 1 (
    >> "%changed_jars_list%" echo !dj_path!
)
endlocal & exit /b 0


REM ============================================================
REM 对单个变化的 jar, 计算服务名 / 模块目录并调用 render-image 的渲染函数
REM ============================================================
:deploy_jar
setlocal enabledelayedexpansion
set dj_jarfile=%~1
if "!dj_jarfile!"=="" (
    endlocal & exit /b 0
)

REM dirname 两次: jar -> target -> 模块目录 (Windows 路径用 \.. 上溯)
for %%D in ("!dj_jarfile!\..") do set dj_target_dir=%%~fD
for %%D in ("!dj_target_dir!\..") do set dj_module_dir=%%~fD
REM 去掉可能的末尾反斜杠, 让 module_dir 跟 shell 版 (无尾 /) 一致
if "!dj_module_dir:~-1!"=="\" set dj_module_dir=!dj_module_dir:~0,-1!

REM basename: 取模块目录名作为服务名
for %%N in ("!dj_module_dir!") do set dj_service_name=%%~nxN

REM 调用 render-image 的渲染函数 (dispatcher 模式: 第一个参数是函数名)
call "%current_directory%\maozi-cloud-deploy-bat-util\maozi-cloud-render-image.bat" render_and_build_image "!dj_service_name!" "!dj_module_dir!"

REM 把 render-image 在本函数作用域里更新的全局变量传回调用方,
REM 否则 endlocal 会把 deploy_services_list 的累加结果清掉
endlocal & (
    set deploy_services_list=%deploy_services_list%
    set deploy_sentinel_dir=%deploy_sentinel_dir%
    set maozi_render_py=%maozi_render_py%
)
exit /b 0


REM ============================================================
REM 等待所有后台 Docker 部署完成
REM 轮询 %deploy_sentinel_dir% 下每个服务的 .done 文件是否都已出现
REM 每轮 sleep ~1s (用 ping 模拟), 直到全部就绪或超时 (30 分钟)
REM ============================================================
:wait_all_deploys
setlocal enabledelayedexpansion

if not defined deploy_services_list (
    echo [deploy] no services in flight
    endlocal & exit /b 0
)
REM 把分号分隔的列表展开成可遍历的形式
set wa_list=!deploy_services_list:;= !
set wa_count=0
for %%S in (!wa_list!) do set /a wa_count+=1
echo [deploy] services in flight: !wa_count!

set wa_timeout=1800
set wa_elapsed=0

:wait_loop
set wa_done=0
for %%S in (!wa_list!) do (
    if exist "%deploy_sentinel_dir%\%%~S.done" set /a wa_done+=1
)
if !wa_done! geq !wa_count! goto :wait_done
REM ~1s sleep
ping -n 2 127.0.0.1 > nul
set /a wa_elapsed+=1
if !wa_elapsed! geq !wa_timeout! (
    echo [deploy] wait timeout: !wa_done!/!wa_count! done after !wa_timeout!s
    goto :wait_done
)
goto :wait_loop

:wait_done
echo [deploy] all deploys settled (!wa_done!/!wa_count!)
endlocal & exit /b 0
