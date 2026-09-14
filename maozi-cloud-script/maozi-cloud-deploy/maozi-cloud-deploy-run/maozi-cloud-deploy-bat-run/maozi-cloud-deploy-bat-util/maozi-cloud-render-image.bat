@echo off
chcp 65001 > nul

REM ============================================================
REM 镜像渲染器 (模板 + JSON -> Dockerfile, 然后构建并清理)
REM ------------------------------------------------------------
REM 被 maozi-cloud-deploy-jar-utils.bat / maozi-cloud-deploy-services-distributed-force.bat
REM 通过 call 调用, 暴露两个函数:
REM   call maozi-cloud-render-image.bat                       (初始化)
REM   call maozi-cloud-render-image.bat render_and_build_image "svc" "dir"
REM   call maozi-cloud-render-image.bat cleanup_image_dirs
REM 对应 shell 版: maozi-cloud-deploy-shell-run/maozi-cloud-deploy-shell-util/maozi-cloud-render-image.sh
REM ------------------------------------------------------------
REM 设计要点:
REM   - call 调用 (仿 maozi-cloud-scan-file-utils.bat), 不用 exit /b 终止调用方
REM   - JSON 解析 + 模板渲染一次性在 python 内完成, 不引入额外依赖
REM   - 顶层不使用 setlocal, 让全局变量 (deploy_sentinel_dir / deploy_services_list 等)
REM     自由泄露给调用方; 仅在函数内部用 setlocal 隔离临时变量, 末尾用
REM     endlocal & set 把要保留的全局变量传回调用方作用域
REM   - 派发用 goto (不用带括号的 if), 规避 %errorlevel% 在括号块里被提前展开的陷阱
REM ------------------------------------------------------------
REM 单独运行时 (maozi-cloud-render-image.bat <service_name>) 进入 dry-run:
REM 把渲染结果打到 stdout, 不构建, 不写文件, 便于人工核对模板渲染是否正确.
REM ============================================================

REM 始终幂等地初始化全局变量 (多次 call 本脚本不会覆盖已有值)
if not defined maozi_render_py    set maozi_render_py=%TEMP%\maozi-render-image.py
if not defined deploy_sentinel_dir set deploy_sentinel_dir=%TEMP%\maozi-deploy-sentinel
if not exist "%deploy_sentinel_dir%" rd /s /q "%deploy_sentinel_dir%" 2>nul & mkdir "%deploy_sentinel_dir%" 2>nul
if not defined deploy_services_list set deploy_services_list=

REM 每次进入本脚本时把 Python 渲染逻辑重写到临时 .py 文件
REM (必须重写: %TEMP% 里可能残留旧版本 .py, 若跳过会导致新渲染逻辑不生效)
call :write_render_py

REM 按第一个参数派发 (用 goto 避免 %errorlevel% 在括号块里被提前展开)
if "%~1"==""                  goto :mode_init
if "%~1"=="render_and_build_image" goto :mode_render
if "%~1"=="cleanup_image_dirs"     goto :mode_cleanup
goto :mode_dryrun

REM ------------------------------------------------------------
REM 初始化模式: 由 jar-utils.bat 在构建开始前调用一次
REM 作用: 设置全局变量 + 生成 Python 脚本 (上面已完成)
REM ------------------------------------------------------------
:mode_init
exit /b 0

REM ------------------------------------------------------------
REM 函数调用模式: render_and_build_image
REM   %2 = service_name, %3 = module_dir
REM ------------------------------------------------------------
:mode_render
call :render_and_build_image "%~2" "%~3"
exit /b %errorlevel%

REM ------------------------------------------------------------
REM 函数调用模式: cleanup_image_dirs
REM ------------------------------------------------------------
:mode_cleanup
call :cleanup_image_dirs
exit /b %errorlevel%

REM ------------------------------------------------------------
REM Dry-run 模式: 单独运行时, %1 = service_name, 把渲染结果打到 stdout
REM ------------------------------------------------------------
:mode_dryrun
setlocal enabledelayedexpansion
set dry_service=%~1
if not defined current_directory set current_directory=%~dp0
if not defined repo_root for %%I in ("%cd%") do set repo_root=%%~fI

REM 独立运行时 current_directory 是本脚本所在目录 (maozi-cloud-deploy-bat-util, 比入口
REM 脚本深一层), 故向上三级才到 maozi-cloud-deploy; call 调用时 current_directory 为入口目录
set image_root=!current_directory!\..\..\..\maozi-cloud-deploy-docker-image
set config_file=!image_root!\maozi-cloud-services.json
set template_file=!image_root!\maozi-cloud-service-image.template

if not exist "!config_file!" (
    echo config not found at !config_file!>&2
    endlocal & exit /b 1
)
if not exist "!template_file!" (
    echo template not found at !template_file!>&2
    endlocal & exit /b 1
)
where python >nul 2>&1
if errorlevel 1 (
    echo python not found in PATH>&2
    endlocal & exit /b 1
)

python "!maozi_render_py!" "!dry_service!" "!config_file!" "!template_file!" -
endlocal & exit /b %errorlevel%


REM ============================================================
REM 渲染并构建单个服务的镜像
REM   %~1 = service_name (如 maozi-cloud-system-service)
REM   %~2 = module_dir   (相对仓库根, 如 maozi-cloud-business-system\maozi-cloud-system-service)
REM ============================================================
:render_and_build_image
setlocal enabledelayedexpansion
set _ra_service_name=%~1
set _ra_module_dir=%~2

set _ra_image_root=%current_directory%\..\..\maozi-cloud-deploy-docker-image
set _ra_config_file=%_ra_image_root%\maozi-cloud-services.json
set _ra_template_file=%_ra_image_root%\maozi-cloud-service-image.template

if not exist "%_ra_config_file%" (
    echo [render] skip %_ra_service_name%: config not found at %_ra_config_file%
    endlocal & exit /b 1
)
if not exist "%_ra_template_file%" (
    echo [render] skip %_ra_service_name%: template not found at %_ra_template_file%
    endlocal & exit /b 1
)
where python >nul 2>&1
if errorlevel 1 (
    echo [render] skip %_ra_service_name%: python not found in PATH
    endlocal & exit /b 1
)

REM 路由镜像 / docker-compose 目录 (沿用 jar-utils 的前缀匹配规则)
call :route_image_dir "%_ra_service_name%"
set _ra_image_directory=%route_result%
call :route_docker_dir "%_ra_service_name%"
set _ra_docker_directory=%route_result%
set _ra_image_file=%_ra_image_directory%\%_ra_service_name%-image

REM compose 文件名: distributeds 目录已改用带前缀文件名, basics 目录仍为 docker-compose.yml
set _ra_compose_file=%_ra_docker_directory%\maozi-cloud-services-distributeds-docker.yml
if /i "%_ra_service_name:~0,19%"=="maozi-cloud-basics-" set _ra_compose_file=%_ra_docker_directory%\docker-compose.yml

REM 父目录可能被误删, 渲染前确保存在, 否则 Python open(out_path, "w") 会抛错
if not exist "%_ra_image_directory%" mkdir "%_ra_image_directory%"

REM python 一次完成: 读 JSON 取配置 + 读模板 + 计算占位符 + 渲染输出
python "!maozi_render_py!" "%_ra_service_name%" "%_ra_config_file%" "%_ra_template_file%" "%_ra_image_file%"
if errorlevel 1 (
    echo [render] skip %_ra_service_name%: not in %_ra_config_file% or render failed
    endlocal & exit /b 1
)

REM 生成临时 build-docker.bat: copy jar / buildx / compose up -d / del jar / del Dockerfile / del 自身
set _ra_build_script=%_ra_image_directory%\%_ra_service_name%-build-docker.bat
set _ra_sentinel=%deploy_sentinel_dir%\%_ra_service_name%.done
if exist "%_ra_sentinel%" del /f /q "%_ra_sentinel%" > nul 2>&1
> "%_ra_build_script%" echo @echo off
>> "%_ra_build_script%" echo chcp 65001 ^> nul
>> "%_ra_build_script%" echo copy /Y "%repo_root%\%_ra_module_dir%\target\%_ra_service_name%.jar" "%_ra_image_directory%\" ^> nul
>> "%_ra_build_script%" echo cd /d "%_ra_image_directory%"
>> "%_ra_build_script%" echo docker buildx build -f "%_ra_image_file%" -t "%_ra_service_name%:laster" .
>> "%_ra_build_script%" echo docker-compose -f "%_ra_compose_file%" up -d %_ra_service_name%
>> "%_ra_build_script%" echo del /f /q "%_ra_image_directory%\%_ra_service_name%.jar" ^> nul 2^>^&1
>> "%_ra_build_script%" echo del /f /q "%_ra_image_file%" ^> nul 2^>^&1
>> "%_ra_build_script%" echo del /f /q "%_ra_build_script%" ^> nul 2^>^&1
REM 最后一步写哨兵文件, 让主流程的 wait 轮询能感知本进程已结束
>> "%_ra_build_script%" echo ^(echo done^) ^> "%_ra_sentinel%"

echo [deploy] %_ra_service_name%: rendering image, building and starting container

REM 后台执行该 Docker 构建脚本, 多个服务并行部署
set _ra_log_file=%TEMP%\maozi-deploy-%_ra_service_name%.log
start "" /b cmd /c ""%_ra_build_script%" > "%_ra_log_file%" 2>&1"

REM 把服务名追加到全局列表, 供主流程末尾轮询等待
if not defined deploy_services_list (
    set deploy_services_list=%_ra_service_name%
) else (
    set deploy_services_list=!deploy_services_list!;%_ra_service_name%
)

REM 把要保留的全局变量传回调用方作用域 (endlocal 会清掉本函数内的所有局部变量)
endlocal & (
    set deploy_services_list=%deploy_services_list%
    set deploy_sentinel_dir=%deploy_sentinel_dir%
    set maozi_render_py=%maozi_render_py%
)
exit /b 0


REM ============================================================
REM 路由镜像目录
REM   maozi-cloud-basics-* -> maozi-cloud-basics-image
REM   其他 -> maozi-cloud-services-image
REM ============================================================
:route_image_dir
setlocal enabledelayedexpansion
set route_base=%current_directory%\..\..\maozi-cloud-deploy-docker-image
set route_prefix=%~1
set route_prefix_part=!route_prefix:~0,19!
if /i "!route_prefix_part!"=="maozi-cloud-basics-" (
    endlocal & set route_result=%route_base%\maozi-cloud-basics-image
) else (
    endlocal & set route_result=%route_base%\maozi-cloud-services-image
)
exit /b 0


REM ============================================================
REM 路由 docker-compose 目录
REM   maozi-cloud-basics-* -> maozi-cloud-basics-docker
REM   其他 -> maozi-cloud-distributeds-docker
REM ============================================================
:route_docker_dir
setlocal enabledelayedexpansion
set route_base=%current_directory%\..\..\maozi-cloud-deploy-docker
set route_prefix=%~1
set route_prefix_part=!route_prefix:~0,19!
if /i "!route_prefix_part!"=="maozi-cloud-basics-" (
    endlocal & set route_result=%route_base%\maozi-cloud-basics-docker
) else (
    endlocal & set route_result=%route_base%\maozi-cloud-distributeds-docker
)
exit /b 0


REM ============================================================
REM 部署完成后清理空的镜像目录
REM ------------------------------------------------------------
REM 设计: 只用 rmdir (不 rmdir /s /q), 如果目录非空 (说明有 build 失败遗留),
REM rmdir 失败, 函数跳过该目录, 保留现场供排查.
REM ============================================================
:cleanup_image_dirs
setlocal
set _cid_image_root=%current_directory%\..\..\maozi-cloud-deploy-docker-image
for %%D in ("%_cid_image_root%\maozi-cloud-services-image" "%_cid_image_root%\maozi-cloud-basics-image") do (
    if exist "%%~D" (
        rmdir "%%~D" 2>nul
        if not errorlevel 1 echo [cleanup] removed empty %%~nD\
    )
)
endlocal
exit /b 0


REM ============================================================
REM 把 Python 渲染逻辑写到临时 .py 文件
REM 每次调用都重写, 保证临时 .py 与本脚本渲染逻辑一致 (无旧版本残留问题)
REM ============================================================
:write_render_py
> "%maozi_render_py%" echo import json, os, sys
>> "%maozi_render_py%" echo service_name  = sys.argv[1]
>> "%maozi_render_py%" echo config_path   = sys.argv[2]
>> "%maozi_render_py%" echo template_path = sys.argv[3]
>> "%maozi_render_py%" echo out_path      = sys.argv[4]
>> "%maozi_render_py%" echo with open^(config_path^) as f:
>> "%maozi_render_py%" echo     cfg = json.load^(f^)
>> "%maozi_render_py%" echo d = cfg.get^("defaults", {}^) or {}
>> "%maozi_render_py%" echo svc = None
>> "%maozi_render_py%" echo for s in cfg.get^("services", []^):
>> "%maozi_render_py%" echo     if s.get^("service_name"^) == service_name:
>> "%maozi_render_py%" echo         svc = s
>> "%maozi_render_py%" echo         break
>> "%maozi_render_py%" echo if svc is None:
>> "%maozi_render_py%" echo     sys.exit^(1^)
>> "%maozi_render_py%" echo has_dubbo = "dubbo_port" in svc
>> "%maozi_render_py%" echo otel      = bool^(svc.get^("opentelemetry"^)^)
>> "%maozi_render_py%" echo jvm       = svc.get^("jvm_params"^)  or d.get^("jvm_params", ""^)
>> "%maozi_render_py%" echo base      = svc.get^("base_image"^)  or d.get^("base_image", "maozi-cloud-base-jdk:1.0.0"^)
>> "%maozi_render_py%" echo # 注册 IP 不写死在 JSON, 读构建机环境变量 NACOS_REGISTER_IP, 未设置则不注入 ENV
>> "%maozi_render_py%" echo dubbo_ip  = os.environ.get^("NACOS_REGISTER_IP", ""^)
>> "%maozi_render_py%" echo dubbo_flag = svc.get^("dubbo_flag"^) or d.get^("dubbo_flag", ""^)
>> "%maozi_render_py%" echo otel_flags = svc.get^("otel_flags"^) or d.get^("otel_flags", []^)
>> "%maozi_render_py%" echo add_opens  = svc.get^("add_opens"^)  or d.get^("add_opens", []^)
>> "%maozi_render_py%" echo dubbo_port_line = ^(", Dubbo " + str^(svc["dubbo_port"]^)^) if has_dubbo else ""
>> "%maozi_render_py%" echo dubbo_env_block = ""
>> "%maozi_render_py%" echo if has_dubbo:
>> "%maozi_render_py%" echo     dubbo_env_block = "ENV APPLICATION_DUBBO_PORT=" + str^(svc["dubbo_port"]^)
>> "%maozi_render_py%" echo     if dubbo_ip:
>> "%maozi_render_py%" echo         dubbo_env_block = "ENV DUBBO_IP_TO_REGISTRY=" + dubbo_ip + "\n" + dubbo_env_block
>> "%maozi_render_py%" echo dubbo_expose_line = "\nEXPOSE ${APPLICATION_DUBBO_PORT}" if has_dubbo else ""
>> "%maozi_render_py%" echo cmd_parts = ["java -server"]
>> "%maozi_render_py%" echo if has_dubbo and dubbo_flag:
>> "%maozi_render_py%" echo     cmd_parts.append^(dubbo_flag^)
>> "%maozi_render_py%" echo if otel and otel_flags:
>> "%maozi_render_py%" echo     cmd_parts.extend^(otel_flags^)
>> "%maozi_render_py%" echo cmd_parts.append^(jvm^)
>> "%maozi_render_py%" echo if add_opens:
>> "%maozi_render_py%" echo     cmd_parts.extend^(add_opens^)
>> "%maozi_render_py%" echo cmd_parts.append^("-jar ${APPLICATION_NAME}.jar"^)
>> "%maozi_render_py%" echo cmd_line = "CMD " + " \\\n  ".join^(cmd_parts^)
>> "%maozi_render_py%" echo otel_yes  = "yes" if otel      else "no"
>> "%maozi_render_py%" echo dubbo_yes = "yes" if has_dubbo else "no"
>> "%maozi_render_py%" echo with open^(template_path^) as f:
>> "%maozi_render_py%" echo     tpl = f.read^(^)
>> "%maozi_render_py%" echo replacements = {
>> "%maozi_render_py%" echo     "__BASE_IMAGE__":        base,
>> "%maozi_render_py%" echo     "__SERVICE_NAME__":      service_name,
>> "%maozi_render_py%" echo     "__SERVICE_PORT__":      str^(svc["service_port"]^),
>> "%maozi_render_py%" echo     "__DUBBO_PORT_LINE__":   dubbo_port_line,
>> "%maozi_render_py%" echo     "__DUBBO_PORT__":        str^(svc.get^("dubbo_port", ""^)^),
>> "%maozi_render_py%" echo     "__OTEL_YES_NO__":       otel_yes,
>> "%maozi_render_py%" echo     "__DUBBO_YES_NO__":      dubbo_yes,
>> "%maozi_render_py%" echo     "__DUBBO_ENV_BLOCK__":   dubbo_env_block,
>> "%maozi_render_py%" echo     "__DUBBO_EXPOSE_LINE__": dubbo_expose_line,
>> "%maozi_render_py%" echo     "__CMD_LINE__":          cmd_line,
>> "%maozi_render_py%" echo }
>> "%maozi_render_py%" echo for k, v in replacements.items^(^):
>> "%maozi_render_py%" echo     tpl = tpl.replace^(k, v^)
>> "%maozi_render_py%" echo if out_path == "-":
>> "%maozi_render_py%" echo     sys.stdout.write^(tpl^)
>> "%maozi_render_py%" echo else:
>> "%maozi_render_py%" echo     with open^(out_path, "w"^) as f:
>> "%maozi_render_py%" echo         f.write^(tpl^)
exit /b 0
