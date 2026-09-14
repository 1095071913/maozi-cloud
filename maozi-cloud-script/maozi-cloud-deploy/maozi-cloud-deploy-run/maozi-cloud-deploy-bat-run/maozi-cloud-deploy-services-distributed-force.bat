@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================================
REM 强制全量部署入口 (单一 git 仓库版)
REM 对应 shell 版: maozi-cloud-deploy-shell-run/maozi-cloud-deploy-services-distributed-force.sh
REM ------------------------------------------------------------
REM 与 maozi-cloud-deploy-services-distributed.bat 的区别:
REM   - 不读取 / 写入 maozi-cloud-deploy-services-distributed-record 状态 (分支 / SHA)
REM   - 不做 git diff 增量比对
REM   - 不做 jar mtime 前后快照对比
REM   - 无条件: 全量 mvn 构建 + 所有在 JSON 配置里登记的服务全部重建并重启
REM ------------------------------------------------------------
REM 工作流程:
REM   1. 切换到仓库根目录
REM   2. 构建基础镜像 maozi-cloud-base-jdk:1.0.0 + maozi-cloud-business-jdk:1.0.0
REM      (服务镜像 FROM 它们, 必须先就绪)
REM   3. mvn clean install (并行线程数按 CPU 核数动态计算) 全量构建整个 reactor
REM   4. for /r 扫描 maozi-cloud-services 下所有 jar
REM   5. 对每个 jar:
REM        - 按服务名前缀路由镜像 / docker-compose 目录
REM        - 调用 render-image 渲染器: 读 maozi-cloud-services.json + 模板渲染
REM          ${service_name}-image, buildx 构建, compose up -d, rm 镜像文件
REM        - 服务不在 JSON 配置里时, 渲染器自动 skip, 不阻塞其他服务
REM        - 后台并行执行
REM   6. wait 等所有后台部署完成
REM ------------------------------------------------------------
REM 适用场景:
REM   - 首次部署
REM   - 切换分支 / 合并代码后想强制重新部署一切
REM   - base-jdk 镜像或 OTel agent 变更后需要全部重建
REM   - 怀疑增量部署有遗漏, 想做一次彻底的回归
REM ============================================================

REM 切换到本脚本所在目录, 使后续相对路径可靠
cd /d "%~dp0"
set current_directory=%CD%

REM 源码仓库根目录 (与 maozi-cloud-deploy-services-distributed.bat 保持一致:
REM 向上 4 级无 pom.xml 时进入 maozi-cloud-parent)
set repo_directory=%~dp0..\..\..\..
if not exist "%repo_directory%\pom.xml" set repo_directory=%~dp0..\..\..\..\maozi-cloud-parent
cd /d "%repo_directory%"

REM 可部署服务源码根目录 (仅此目录下生成的 jar 才会触发 Docker 部署)
set services_subdir=maozi-cloud-service\maozi-cloud-services

REM ============================================================
REM 工具函数路由: 由 render-image.bat 提供, 先初始化一次拿到全局变量
REM ============================================================
call "%current_directory%\maozi-cloud-deploy-bat-util\maozi-cloud-render-image.bat"

REM ============================================================
REM 1. 构建基础 JDK 镜像 (所有服务镜像 FROM 它 / business-jdk)
REM ------------------------------------------------------------
REM 两层结构, 必须按顺序构建:
REM   a) maozi-cloud-base-jdk:1.0.0     OS + 时区 + dumb-init (不含 OTel)
REM   b) maozi-cloud-business-jdk:1.0.0 FROM base-jdk + OTel Agent jar
REM ============================================================
set base_image_directory=%current_directory%\..\..\maozi-cloud-deploy-docker-image\maozi-cloud-base-jdk-image
set business_image_directory=%current_directory%\..\..\maozi-cloud-deploy-docker-image\maozi-cloud-business-jdk-image

if exist "%base_image_directory%\Dockerfile" (
    echo [base] building maozi-cloud-base-jdk:1.0.0
    pushd "%base_image_directory%"
    docker buildx build -f Dockerfile -t maozi-cloud-base-jdk:1.0.0 .
    if errorlevel 1 (
        popd
        echo [base] FAILED: maozi-cloud-base-jdk:1.0.0 build error, abort
        endlocal & exit /b 1
    )
    popd
    echo [base] done
) else (
    echo [base] skip: Dockerfile not found at %base_image_directory%\Dockerfile
)

if exist "%business_image_directory%\Dockerfile" (
    echo [base] building maozi-cloud-business-jdk:1.0.0
    pushd "%business_image_directory%"
    docker buildx build -f Dockerfile -t maozi-cloud-business-jdk:1.0.0 .
    if errorlevel 1 (
        popd
        echo [base] FAILED: maozi-cloud-business-jdk:1.0.0 build error, abort
        endlocal & exit /b 1
    )
    popd
    echo [base] done
) else (
    echo [base] skip: Dockerfile not found at %business_image_directory%\Dockerfile
)

REM ============================================================
REM 2. 全量 Maven 构建 (不做 -pl / -amd 增量, 直接整个 reactor)
REM ============================================================
echo [build] mvn clean install (full reactor)

REM Maven 并行线程数按当前机器 CPU 核数动态计算: 2 * 核数
REM %NUMBER_OF_PROCESSORS% 为 Windows 自动设置的逻辑核数, 极端环境未定义时兜底 4
if not defined NUMBER_OF_PROCESSORS set NUMBER_OF_PROCESSORS=4
set /a MVN_THREADS=%NUMBER_OF_PROCESSORS% * 2
echo [build] mvn threads: %MVN_THREADS% ^(cores: %NUMBER_OF_PROCESSORS%^)

call mvn clean install -T %MVN_THREADS% -Dmaven.compile.fork=true -Dmaven.test.skip=true
if errorlevel 1 (
    echo [build] FAILED: mvn clean install error, abort
    endlocal & exit /b 1
)

REM ============================================================
REM 3. 扫描所有 jar, 过滤出可部署的服务
REM ============================================================
REM 锁定仓库根的绝对路径, 让生成的临时脚本里的 cp 源路径不依赖 cwd
set repo_root=%CD%

REM find 所有 jar (包括首次部署时还没构建的情况也能正确跳过)
set deploy_count=0

REM 递归扫描 services_subdir 下所有 .jar
if not exist "%services_subdir%" goto :after_scan
for /r "%services_subdir%" %%f in (*.jar) do (
    call :process_jar "%%f"
)

:after_scan
echo [deploy] services in flight: !deploy_count!

REM 等待所有后台 Docker 部署完成
call :wait_all_deploys

REM 清理空的 maozi-cloud-services-image / -basics-image 目录
call "%current_directory%\maozi-cloud-deploy-bat-util\maozi-cloud-render-image.bat" cleanup_image_dirs

echo [deploy] all done
endlocal & exit /b 0


REM ============================================================
REM 处理单个 jar: 计算服务名 / 模块目录, 调用 render_and_build_image
REM ============================================================
:process_jar
setlocal enabledelayedexpansion
set pj_jarfile=%~1

REM dirname 两次: jar -> target -> 模块目录
REM Windows 路径: ...\target\xxx.jar -> 取倒数两级目录
for %%D in ("!pj_jarfile!\..") do set pj_target_dir=%%~fD
for %%D in ("!pj_target_dir!\..") do set pj_module_dir=%%~fD
if "!pj_module_dir:~-1!"=="\" set pj_module_dir=!pj_module_dir:~0,-1!

REM basename: 取模块目录名作为服务名
for %%N in ("!pj_module_dir!") do set pj_service_name=%%~nxN

REM 渲染 + 构建 + 清理统一交给 render-image:
REM   - 读 maozi-cloud-services.json 取该服务的端口 / Dubbo / OTel / JVM / base_image
REM   - 按模板渲染出 ${service_name}-image, buildx 构建, compose 启动, rm 镜像文件
REM 服务不在 JSON 配置里时, 渲染器返回非零并打印 skip, 不阻塞其他服务
call "%current_directory%\maozi-cloud-deploy-bat-util\maozi-cloud-render-image.bat" render_and_build_image "!pj_service_name!" "!pj_module_dir!"

REM 把 render-image 在本函数作用域里更新的全局变量传回调用方,
REM 否则 endlocal 会把 deploy_services_list 的累加结果清掉.
REM 成功时同时累加 deploy_count.
if not errorlevel 1 (
    endlocal & (
        set deploy_services_list=%deploy_services_list%
        set deploy_sentinel_dir=%deploy_sentinel_dir%
        set maozi_render_py=%maozi_render_py%
        set /a deploy_count+=1
    )
    exit /b 0
)
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
