@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================================
REM 后台前端 (maozi-cloud-admin) 容器化部署入口 - 单体模式 (self-contained)
REM 对应 shell 版: maozi-cloud-deploy-shell-run/maozi-cloud-deploy-admin-monomer.sh
REM 对应微服务版: maozi-cloud-deploy-bat-run/maozi-cloud-deploy-admin-distributed.bat
REM ------------------------------------------------------------
REM 与微服务版的区别: 镜像 / 容器名为 maozi-cloud-admin-monomer,
REM 拉起 maozi-cloud-monomer-docker\maozi-cloud-admin-monomer-docker.yml
REM ------------------------------------------------------------
REM 与后端 jar 服务不同, 前端是 Docker 多阶段镜像构建:
REM   1. 定位 maozi-cloud-admin 源码目录 (构建上下文)
REM   2. 把 maozi-cloud-admin-nginx.conf 临时拷入源码目录
REM      (Dockerfile COPY 只能取上下文内文件), 构建结束删除
REM   3. docker buildx build: node 阶段 npm ci + npm run build 产出 dist,
REM      nginx 阶段托管 dist 并做同源转发 (API -> 网关, /nacos, /grafana)
REM   4. docker-compose up -d 拉起容器 (端口 999)
REM ------------------------------------------------------------
REM 前置:
REM   1. maozi-cloud-network 已创建
REM   2. 单体服务已启动 (maozi-cloud-service-monomer, 由
REM      maozi-cloud-deploy-services-monomer.bat 拉起);
REM      单体模式无独立网关容器, /api/ 转发地址需指向单体服务,
REM      详见 maozi-cloud-admin-image\maozi-cloud-admin-nginx.conf 的单体模式说明
REM   3. basics-docker 已启动 (/nacos /grafana 同源代理依赖)
REM   4. 无需宿主机 node, 构建全部在 Docker 内完成
REM ------------------------------------------------------------
REM 与微服务版 (maozi-cloud-admin-distributeds) 互斥: 同占宿主机 999 端口,
REM 切换模式前先停掉另一模式的 admin 容器。
REM ============================================================

cd /d "%~dp0"
set current_directory=%CD%

REM 源码仓库根目录: 本脚本向上回溯 4 级;
REM 前端源码目录 maozi-cloud-admin 与 maozi-cloud-parent 是仓库根下的同级目录
set repo_root=%~dp0..\..\..\..
cd /d "%repo_root%"
for %%I in ("%repo_root%") do set repo_root=%%~fI
set admin_directory=%repo_root%\maozi-cloud-admin

REM 部署资产目录 (镜像定义与 compose 均随脚本仓库走, 不侵入前端项目)
set image_directory=%current_directory%\..\..\maozi-cloud-deploy-docker-image\maozi-cloud-admin-image
set nginx_conf_name=maozi-cloud-admin-nginx.conf
set COMPOSE_FILE=%current_directory%\..\..\maozi-cloud-deploy-docker\maozi-cloud-monomer-docker\maozi-cloud-admin-monomer-docker.yml
set IMAGE_TAG=maozi-cloud-admin-monomer:laster

REM ---- 前置检查 ----
if not exist "%admin_directory%\package.json" (
    echo [deploy] package.json not found at %admin_directory%
    endlocal & exit /b 1
)
if not exist "%image_directory%\Dockerfile" (
    echo [deploy] Dockerfile not found at %image_directory%
    endlocal & exit /b 1
)
if not exist "%image_directory%\%nginx_conf_name%" (
    echo [deploy] nginx conf not found at %image_directory%\%nginx_conf_name%
    endlocal & exit /b 1
)
if not exist "%COMPOSE_FILE%" (
    echo [deploy] compose file not found at %COMPOSE_FILE%
    endlocal & exit /b 1
)

REM ---- 临时拷入 nginx.conf (对应 shell 的 trap, 失败路径也逐一 del) ----
copy /Y "%image_directory%\%nginx_conf_name%" "%admin_directory%\" > nul

REM ---- 1. 多阶段构建镜像 ----
echo [deploy] building image %IMAGE_TAG% (npm build in node stage, served by nginx)
docker buildx build -f "%image_directory%\Dockerfile" -t "%IMAGE_TAG%" "%admin_directory%"
if errorlevel 1 (
    echo [deploy] FAILED: docker buildx error, abort
    del /f /q "%admin_directory%\%nginx_conf_name%" > nul 2>&1
    endlocal & exit /b 1
)

REM ---- 2. 拉起容器 ----
echo [deploy] docker-compose -f %COMPOSE_FILE% up -d
docker-compose -f "%COMPOSE_FILE%" up -d
if errorlevel 1 (
    echo [deploy] FAILED: docker-compose up error
    del /f /q "%admin_directory%\%nginx_conf_name%" > nul 2>&1
    endlocal & exit /b 1
)

REM ---- 清理临时文件 (对应 shell 的 trap) ----
del /f /q "%admin_directory%\%nginx_conf_name%" > nul 2>&1

echo [deploy] done: maozi-cloud-admin -^> %IMAGE_TAG% (port 999)
endlocal & exit /b 0
