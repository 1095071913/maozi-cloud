@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================================
REM maozi-cloud-service-monomer 专用部署脚本 (self-contained)
REM (源码模块为 maozi-cloud-all-service, 容器 / 镜像名独立命名, 见下方配置)
REM 对应 shell 版: maozi-cloud-deploy-shell-run/maozi-cloud-deploy-services-monomer.sh
REM ------------------------------------------------------------
REM 与标准 build 流程解耦:
REM   - 配置内嵌在脚本顶部, 不写入 maozi-cloud-services.json
REM   - 用专属的 maozi-cloud-monomer-docker/maozi-cloud-services-monomer-docker.yml 拉起容器
REM     (all-service 跟 gateway 都占 1100 端口, 与 services-docker 互斥,
REM      不能同时跑; 因此单独建 compose, 不混在 services-docker 里)
REM   - Dockerfile 在脚本里用 echo 重定向生成, 不依赖 maozi-cloud-service-image.template
REM ------------------------------------------------------------
REM 使用:
REM   maozi-cloud-deploy-services-monomer.bat            REM mvn 构建 + 部署
REM   maozi-cloud-deploy-services-monomer.bat --no-build REM 跳过 mvn, 直接用已有 jar 部署
REM ------------------------------------------------------------
REM 前置:
REM   1. maozi-cloud-basics-docker 已启动 (nacos / mysql / redis)
REM   2. maozi-cloud-base-jdk:1.0.0 已构建 (由 maozi-cloud-deploy-services-distributed-force.bat 完成)
REM   3. 外部网络 maozi-cloud-network 已创建
REM   4. 如 services-docker 里的 gateway 在跑, 先停掉 (端口 1100 冲突):
REM      docker-compose -f ...\maozi-cloud-distributeds-docker\maozi-cloud-services-distributeds-docker.yml stop maozi-cloud-gateway-service
REM ============================================================

cd /d "%~dp0"
set current_directory=%CD%
REM 源码仓库根目录: 向上 4 级无 pom.xml 时进入 maozi-cloud-parent
set repo_root=%~dp0..\..\..\..
if not exist "%repo_root%\pom.xml" set repo_root=%~dp0..\..\..\..\maozi-cloud-parent
cd /d "%repo_root%"
REM 把 repo_root 转为绝对路径, 让后续拼 jar 路径不依赖 cwd
for %%I in ("%repo_root%") do set repo_root=%%~fI

REM ============================================================
REM 配置 (与 maozi-cloud-services.json defaults 保持一致, 方便维护)
REM ============================================================
REM SERVICE_NAME: 容器 / 镜像 / compose 服务名 (与 maozi-cloud-monomer-docker 对应)
REM MODULE_NAME:  Maven artifactId, 决定源码模块目录与产物 jar 文件名,
REM               两者独立维护, 改容器名无需动后端模块
set SERVICE_NAME=maozi-cloud-service-monomer
set SERVICE_PORT=1100
set MODULE_NAME=maozi-cloud-all-service
set MODULE_DIR=maozi-cloud-service\maozi-cloud-services\%MODULE_NAME%
set BASE_IMAGE=maozi-cloud-base-jdk:1.0.0
REM OTel=false: 用 base-jdk 即可, 不需要 business-jdk 里的 OTel agent
set JVM_PARAMS=-Xms256m -Xmx256m -Xss256k -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC -XX:CompressedClassSpaceSize=32m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=32m
set IMAGE_TAG=%SERVICE_NAME%:laster
REM 部署用的 compose 文件 (端口 / 网络 / 日志挂载都在里面, 改部署参数改这里)
set COMPOSE_FILE=%current_directory%\..\..\maozi-cloud-deploy-docker\maozi-cloud-monomer-docker\maozi-cloud-services-monomer-docker.yml

REM ============================================================
REM 1. Maven 构建 (可选)
REM ------------------------------------------------------------
REM -am (also-make) 把 MODULE_DIR 的上游依赖模块也纳入 reactor 一起构建,
REM 否则 maven 会把 oauth-business / system-business 等当外部依赖去仓库拉,
REM 本地仓库缺失时就会因 HTTP blocker 失败 (或下载不必要的远程包).
REM ============================================================
if /i "%~1"=="--no-build" goto :check_jar

REM Maven 并行线程数按当前机器 CPU 核数动态计算: 2 * 核数
REM %NUMBER_OF_PROCESSORS% 为 Windows 自动设置的逻辑核数, 极端环境未定义时兜底 4
if not defined NUMBER_OF_PROCESSORS set NUMBER_OF_PROCESSORS=4
set /a MVN_THREADS=%NUMBER_OF_PROCESSORS% * 2
echo [build] mvn clean install -T %MVN_THREADS% -pl %MODULE_DIR% -am
call mvn clean install -T %MVN_THREADS% -Dmaven.compile.fork=true -Dmaven.test.skip=true -pl "%MODULE_DIR%" -am
if errorlevel 1 (
    echo [build] FAILED: mvn error, abort
    endlocal & exit /b 1
)

:check_jar
REM ============================================================
REM 2. 校验 jar 存在
REM ============================================================
set jar_path=%repo_root%\%MODULE_DIR%\target\%MODULE_NAME%.jar
if not exist "%jar_path%" (
    echo [deploy] jar not found: %jar_path%
    echo         run with no args to trigger mvn build, or check MODULE_DIR
    endlocal & exit /b 1
)

REM ============================================================
REM 3. 准备临时构建目录, 拷贝 jar + 写 Dockerfile
REM ============================================================
REM 用 TEMP 目录 + 固定子目录做 build_dir (mktemp -d 的等价物)
set build_dir=%TEMP%\%SERVICE_NAME%-build
if exist "%build_dir%" rmdir /s /q "%build_dir%" > nul 2>&1
mkdir "%build_dir%"
REM 拷贝时统一重命名为 SERVICE_NAME.jar, 让 Dockerfile 的 COPY / CMD 与容器内
REM /application/${APPLICATION_NAME} 路径自洽 (jar 原名仍是 Maven artifactId)
copy /Y "%jar_path%" "%build_dir%\%SERVICE_NAME%.jar" > nul

REM 写 Dockerfile (对应 shell 里的 cat <<EOF ... EOF heredoc)
REM 转义规则:
REM   ^^^^  -> 输出字面量 ^ (CMD 续行符, 在 echo 里必须双转义, 否则被当成行连接)
REM   ^( ^) -> 输出字面量 ( ) (避免与外层 if/for 的括号块冲突)
REM   ^>   -> 输出字面量 > (避免被当成重定向)
> "%build_dir%\Dockerfile" echo # ============================================================
>> "%build_dir%\Dockerfile" echo # Dockerfile - %SERVICE_NAME% ^(AUTO-GENERATED by maozi-cloud-deploy-services-monomer.bat^)
>> "%build_dir%\Dockerfile" echo # ------------------------------------------------------------
>> "%build_dir%\Dockerfile" echo # 聚合服务: 把所有业务模块打成一个 jar, 单端口 1100 暴露.
>> "%build_dir%\Dockerfile" echo # OTel=false, 无 Dubbo, 基础镜像用 maozi-cloud-base-jdk ^(不带 OTel agent^).
>> "%build_dir%\Dockerfile" echo # ============================================================
>> "%build_dir%\Dockerfile" echo FROM %BASE_IMAGE%
>> "%build_dir%\Dockerfile" echo.
>> "%build_dir%\Dockerfile" echo ENV APPLICATION_NAME=%SERVICE_NAME%
>> "%build_dir%\Dockerfile" echo ENV APPLICATION_PORT=%SERVICE_PORT%
>> "%build_dir%\Dockerfile" echo.
>> "%build_dir%\Dockerfile" echo RUN mkdir -p /application/${APPLICATION_NAME}
>> "%build_dir%\Dockerfile" echo.
>> "%build_dir%\Dockerfile" echo WORKDIR /application/${APPLICATION_NAME}
>> "%build_dir%\Dockerfile" echo.
>> "%build_dir%\Dockerfile" echo COPY ./%SERVICE_NAME%.jar ./
>> "%build_dir%\Dockerfile" echo.
>> "%build_dir%\Dockerfile" echo CMD java -server ^^
>> "%build_dir%\Dockerfile" echo   %JVM_PARAMS% ^^
>> "%build_dir%\Dockerfile" echo   --add-opens java.base/java.math=ALL-UNNAMED ^^
>> "%build_dir%\Dockerfile" echo   --add-opens java.base/java.lang=ALL-UNNAMED ^^
>> "%build_dir%\Dockerfile" echo   --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^^
>> "%build_dir%\Dockerfile" echo   -jar ${APPLICATION_NAME}.jar
>> "%build_dir%\Dockerfile" echo.
>> "%build_dir%\Dockerfile" echo EXPOSE ${APPLICATION_PORT}

REM ============================================================
REM 4. 构建 docker 镜像
REM ============================================================
echo [deploy] building image %IMAGE_TAG%
pushd "%build_dir%"
docker buildx build -f Dockerfile -t "%IMAGE_TAG%" .
if errorlevel 1 (
    popd
    echo [deploy] FAILED: docker buildx error, abort
    rmdir /s /q "%build_dir%" > nul 2>&1
    endlocal & exit /b 1
)
popd

REM ============================================================
REM 5. docker-compose up -d (停旧 / 起新由 compose 自动处理)
REM ------------------------------------------------------------
REM 端口 / 网络 / 日志挂载 / 环境变量都在 COMPOSE_FILE 里声明, 改部署参数改那里.
REM compose up -d 会自动重建基于新镜像的容器 (因为 image: maozi-cloud-service-monomer:laster)
REM ============================================================
echo [deploy] docker-compose -f %COMPOSE_FILE% up -d
docker-compose -f "%COMPOSE_FILE%" up -d
if errorlevel 1 (
    echo [deploy] FAILED: docker-compose up error
    rmdir /s /q "%build_dir%" > nul 2>&1
    endlocal & exit /b 1
)

REM 清理临时构建目录 (对应 shell 的 trap "rm -rf '$build_dir'" EXIT)
rmdir /s /q "%build_dir%" > nul 2>&1

echo [deploy] done: %SERVICE_NAME% -^> %IMAGE_TAG% (port %SERVICE_PORT%)
endlocal & exit /b 0
