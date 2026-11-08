import {contextBridge, ipcRenderer} from 'electron'

/**
 * 渲染进程唯一的能力入口：所有系统操作都经由 ipcMain.handle 完成，
 * 渲染进程不直接接触 Node API
 */
contextBridge.exposeInMainWorld('api', {
  app: {
    info: () => ipcRenderer.invoke('app:info')
  },
  env: {
    list: () => ipcRenderer.invoke('env:list'),
    save: (params: unknown) => ipcRenderer.invoke('env:save', params),
    launchctl: (params: unknown) => ipcRenderer.invoke('env:launchctl', params)
  },
  hosts: {
    list: () => ipcRenderer.invoke('hosts:list'),
    save: (entries: unknown) => ipcRenderer.invoke('hosts:save', entries),
    flushDns: () => ipcRenderer.invoke('hosts:flushDns'),
    readRaw: () => ipcRenderer.invoke('hosts:readRaw'),
    saveRaw: (text: string) => ipcRenderer.invoke('hosts:saveRaw', text)
  },
  sysinfo: {
    static: () => ipcRenderer.invoke('sysinfo:static'),
    dynamic: () => ipcRenderer.invoke('sysinfo:dynamic'),
    network: () => ipcRenderer.invoke('sysinfo:network'),
    publicIP: () => ipcRenderer.invoke('sysinfo:publicIP'),
    devtools: () => ipcRenderer.invoke('sysinfo:devtools'),
    /** JDK 列表（macOS）与当前 JAVA_HOME */
    jdkList: () => ipcRenderer.invoke('sysinfo:jdkList'),
    /** 切换 JDK：写 JAVA_HOME 到 ~/.zshrc（托管区块 + 实时生效钩子） */
    jdkSwitch: (home: string) => ipcRenderer.invoke('sysinfo:jdkSwitch', home),
    /** 卸载 JDK（危险操作，前端先确认）：用户级直删 / brew uninstall / 管理员授权删除 */
    jdkUninstall: (home: string) => ipcRenderer.invoke('sysinfo:jdkUninstall', home),
    /** 下载安装 JDK（Adoptium / Corretto 官方源 → 用户级 JavaVirtualMachines），进度走 jdkDownloadProgress */
    jdkDownload: (version: string, vendor: string) => ipcRenderer.invoke('sysinfo:jdkDownload', version, vendor),
    jdkDownloadCancel: () => ipcRenderer.invoke('sysinfo:jdkDownloadCancel'),
    onJdkDownloadProgress: (cb: (payload: {
      stage: 'download' | 'extract' | 'install' | 'done' | 'error'
      pct: number
      receivedMB: number
      totalMB: number
      message: string
    }) => void) => {
      const listener = (_e: unknown, payload: {
        stage: 'download' | 'extract' | 'install' | 'done' | 'error'
        pct: number
        receivedMB: number
        totalMB: number
        message: string
      }): void => cb(payload)
      ipcRenderer.on('sysinfo:jdkDownloadProgress', listener)
      return () => ipcRenderer.removeListener('sysinfo:jdkDownloadProgress', listener)
    }
  },
  bookmarks: {
    list: () => ipcRenderer.invoke('bookmarks:list'),
    save: (input: unknown) => ipcRenderer.invoke('bookmarks:save', input),
    remove: (id: string) => ipcRenderer.invoke('bookmarks:remove', id),
    fetchMeta: (url: string) => ipcRenderer.invoke('bookmarks:fetchMeta', url),
    reorder: (ids: string[]) => ipcRenderer.invoke('bookmarks:reorder', ids)
  },
  projects: {
    state: () => ipcRenderer.invoke('projects:state'),
    projectsList: () => ipcRenderer.invoke('projects:projectsList'),
    projectCreate: (name: string, alias: string, remark: string) =>
      ipcRenderer.invoke('projects:projectCreate', { name, alias, remark }),
    projectActivate: (id: string) => ipcRenderer.invoke('projects:projectActivate', id),
    projectUpdate: (id: string, name: string, alias: string, remark: string) =>
      ipcRenderer.invoke('projects:projectUpdate', id, { name, alias, remark }),
    projectDeactivate: () => ipcRenderer.invoke('projects:projectDeactivate'),
    projectRemove: (id: string) => ipcRenderer.invoke('projects:projectRemove', id),
    pickDir: () => ipcRenderer.invoke('projects:pickDir'),
    bindDir: (dir: string) => ipcRenderer.invoke('projects:bindDir', dir),
    clone: (secretId: string, destDir: string) => ipcRenderer.invoke('projects:clone', secretId, destDir),
    unbind: () => ipcRenderer.invoke('projects:unbind'),
    runScript: (key: string, sid?: string) => ipcRenderer.invoke('projects:runScript', key, sid),
    composeServices: () => ipcRenderer.invoke('projects:composeServices'),
    composeAction: (service: string, action: string, sid?: string) =>
      ipcRenderer.invoke('projects:composeAction', service, action, sid),
    composeStatus: () => ipcRenderer.invoke('projects:composeStatus'),
    composeStats: (preferCache?: boolean) => ipcRenderer.invoke('projects:composeStats', preferCache),
    /** 服务日志：一次性查询最近 N 行（ctx 缺省为基础服务，或应用服务 '<variant>:<admin|services|nginx>'） */
    composeLogs: (service: string, tail?: number, ctx?: string) => ipcRenderer.invoke('projects:composeLogs', service, tail, ctx),
    /** 服务日志实时跟踪（-f 流式，ctx 同 composeLogs，停止复用 stopScript） */
    composeLogsFollow: (service: string, tail: number, sid?: string, ctx?: string) =>
      ipcRenderer.invoke('projects:composeLogsFollow', service, tail, sid, ctx),
    onComposeLog: (cb: (payload: { kind: 'line' | 'update'; text: string; sid?: string }) => void) => {
      const listener = (
        _e: unknown,
        payload: { kind: 'line' | 'update'; text: string; sid?: string }
      ): void => cb(payload)
      ipcRenderer.on('projects:composeLog', listener)
      return () => ipcRenderer.removeListener('projects:composeLog', listener)
    },
    adminContainerStatus: () => ipcRenderer.invoke('projects:adminContainerStatus'),
    adminContainerAction: (action: string, sid?: string) =>
      ipcRenderer.invoke('projects:adminContainerAction', action, sid),
    hostsInitStatus: () => ipcRenderer.invoke('projects:hostsInitStatus'),
    hostsInit: (sid?: string) => ipcRenderer.invoke('projects:hostsInit', sid),
    /** 容器网络：解析 compose 外部网络名并检查是否已存在 */
    networkStatus: () => ipcRenderer.invoke('projects:networkStatus'),
    /** 创建容器网络（docker network create，幂等） */
    networkCreate: (sid?: string) => ipcRenderer.invoke('projects:networkCreate', sid),
    /** 应用服务（单体/微服务）：变体服务清单与运行状态 */
    appServices: (variant: string) => ipcRenderer.invoke('projects:appServices', variant),
    appServicesStatus: (variant: string) => ipcRenderer.invoke('projects:appServicesStatus', variant),
    appServicesStats: (preferCache?: boolean) => ipcRenderer.invoke('projects:appServicesStats', preferCache),
    appServiceAction: (variant: string, file: string, service: string, action: string, sid?: string) =>
      ipcRenderer.invoke('projects:appServiceAction', variant, file, service, action, sid),
    appServiceAll: (variant: string, action: string, sid?: string, opts?: { foreground?: boolean }) =>
      ipcRenderer.invoke('projects:appServiceAll', variant, action, sid, opts),
    /** 全链路日志查询：按链路 ID 检索分布式变体所有运行容器的日志 */
    traceLogQuery: (traceId: string, tail?: number) => ipcRenderer.invoke('projects:traceLogQuery', traceId, tail),
    /** 远程服务器（SSH）：密钥列表 / 连接测试 / 目录浏览 / 绑定 / git 检测 / 远程 clone */
    sshConfigs: () => ipcRenderer.invoke('projects:sshConfigs'),
    sshTest: (configId: string) => ipcRenderer.invoke('projects:sshTest', configId),
    sshListDir: (configId: string, path: string) => ipcRenderer.invoke('projects:sshListDir', configId, path),
    /** 在远程目录下创建子目录（选择目录页新建） */
    sshMkdir: (configId: string, parentDir: string, name: string) =>
      ipcRenderer.invoke('projects:sshMkdir', configId, parentDir, name),
    sshBindDir: (configId: string, path: string) => ipcRenderer.invoke('projects:sshBindDir', configId, path),
    sshCheckGit: (configId: string) => ipcRenderer.invoke('projects:sshCheckGit', configId),
    sshClone: (configId: string, gitSecretId: string, destDir: string, sid?: string) =>
      ipcRenderer.invoke('projects:sshClone', configId, gitSecretId, destDir, sid),
    /** 远程环境变量保存：写入远程 shell 配置 / 实时环境文件 */
    sshEnvSave: (params: { key: string; value: string; fileId: string }) =>
      ipcRenderer.invoke('projects:sshEnvSave', params),
    /** UI 状态持久化（Tab 选中 等，.ui-state.json） */
    uiStateGet: () => ipcRenderer.invoke('projects:uiStateGet'),
    uiStateSave: (patch: Record<string, unknown>) => ipcRenderer.invoke('projects:uiStateSave', patch),
    /** 数据库初始化状态：按 .db-init.json 标记判定是否已初始化（init_mysql_db.json 无脚本时 count=0） */
    dbInitStatus: () => ipcRenderer.invoke('projects:dbInitStatus'),
    /** 初始化数据库：mysql 未运行时临时启动，逐个导入 init_mysql_db.json 定义的 SQL 脚本，结束后回收本次启动的容器 */
    dbInit: (sid?: string) => ipcRenderer.invoke('projects:dbInit', sid),
    envSettings: () => ipcRenderer.invoke('projects:envSettings'),
    envFileSave: (input: { key: string; value: string }) => ipcRenderer.invoke('projects:envFileSave', input),
    serviceConfig: (service: string, base?: string) => ipcRenderer.invoke('projects:serviceConfig', service, base),
    /** Maven 配置：mvn -v 解析安装目录 + settings.xml 读写（保存直接覆盖，不备份） */
    mavenInfo: () => ipcRenderer.invoke('projects:mavenInfo'),
    mavenConfigRead: () => ipcRenderer.invoke('projects:mavenConfigRead'),
    mavenConfigSave: (content: string) => ipcRenderer.invoke('projects:mavenConfigSave', content),
    /** Git 配置：项目 .git/config 查看与修改（直接覆盖保存，不备份） */
    gitConfigRead: () => ipcRenderer.invoke('projects:gitConfigRead'),
    gitConfigSave: (content: string) => ipcRenderer.invoke('projects:gitConfigSave', content),
    /** Docker 配置：docker -v + daemon.json 按平台解析（mac/Win=~/.docker，Linux=/etc/docker 优先），保存直接覆盖（不备份） */
    dockerInfo: () => ipcRenderer.invoke('projects:dockerInfo'),
    dockerConfigRead: () => ipcRenderer.invoke('projects:dockerConfigRead'),
    dockerConfigSave: (content: string) => ipcRenderer.invoke('projects:dockerConfigSave', content),
    /** 重启 Docker（Docker Desktop 安装用 docker desktop restart，直装用 systemctl），各阶段进度推送执行日志会话 */
    dockerRestart: (sid?: string) => ipcRenderer.invoke('projects:dockerRestart', sid),
    hotSwap: (variant: string, enable: boolean, sid?: string) => ipcRenderer.invoke('projects:hotSwap', variant, enable, sid),
    initImages: () => ipcRenderer.invoke('projects:initImages'),
    initImageBuild: (name: string, sid?: string) => ipcRenderer.invoke('projects:initImageBuild', name, sid),
    stopScript: (sid?: string) => ipcRenderer.invoke('projects:stopScript', sid),
    onScriptLog: (cb: (payload: { kind: 'line' | 'update'; text: string; sid?: string }) => void) => {
      const listener = (
        _e: unknown,
        payload: { kind: 'line' | 'update'; text: string; sid?: string }
      ): void => cb(payload)
      ipcRenderer.on('projects:scriptLog', listener)
      return () => ipcRenderer.removeListener('projects:scriptLog', listener)
    },
    openDir: () => ipcRenderer.invoke('projects:openDir'),
    /** 项目 git 管理：仓库检测（含当前分支） */
    gitInfo: () => ipcRenderer.invoke('projects:gitInfo'),
    gitRemoteCheck: () => ipcRenderer.invoke('projects:gitRemoteCheck'),
    /** 分支列表：本地分支 + 远程分支（git ls-remote --heads，需网络；远程失败时降级为仅本地） */
    gitBranches: () => ipcRenderer.invoke('projects:gitBranches'),
    /** 拉取代码（git pull；mode：缺省普通 / rebase 变基 / merge 合并 / reset 硬重置到远程），日志走 onScriptLog */
    gitPull: (mode?: string, sid?: string) => ipcRenderer.invoke('projects:gitPull', mode, sid),
    /** 切换分支（fetch 目标分支后 checkout），日志走 onScriptLog */
    gitCheckout: (branch: string, sid?: string) => ipcRenderer.invoke('projects:gitCheckout', branch, sid),
    onCloneLog: (cb: (payload: { kind: 'line' | 'update'; text: string }) => void) => {
      const listener = (_e: unknown, payload: { kind: 'line' | 'update'; text: string }): void => cb(payload)
      ipcRenderer.on('projects:cloneLog', listener)
      return () => ipcRenderer.removeListener('projects:cloneLog', listener)
    },
    /** 绑定元信息变化推送：主进程监听本地 maozi-cloud-config.json，name/version 被外部修改时即时下发 */
    onBindingChanged: (cb: (payload: {
      name: string
      version: string
      path: string
      boundAt: number
      remote?: { configId: string; configName: string; user: string; host: string; port: number }
    }) => void) => {
      const listener = (
        _e: unknown,
        payload: {
          name: string
          version: string
          path: string
          boundAt: number
          remote?: { configId: string; configName: string; user: string; host: string; port: number }
        }
      ): void => cb(payload)
      ipcRenderer.on('projects:bindingChanged', listener)
      return () => ipcRenderer.removeListener('projects:bindingChanged', listener)
    }
  },
  configs: {
    list: () => ipcRenderer.invoke('configs:list'),
    save: (input: unknown) => ipcRenderer.invoke('configs:save', input),
    remove: (id: string) => ipcRenderer.invoke('configs:remove', id),
    login: (id: string) => ipcRenderer.invoke('configs:login', id)
  },
  rename: {
    /** 批量重命名项目：目录/文件名 + 文件内容 + 根目录名替换，进度走 onRenameProgress */
    run: (input: unknown) => ipcRenderer.invoke('rename:run', input),
    cancel: () => ipcRenderer.invoke('rename:cancel'),
    onRenameProgress: (cb: (payload: {
      stage: 'scan' | 'content' | 'rename' | 'done' | 'error'
      done: number
      total: number
      current: string
      stats: Record<string, unknown>
    }) => void) => {
      const listener = (_e: unknown, payload: {
        stage: 'scan' | 'content' | 'rename' | 'done' | 'error'
        done: number
        total: number
        current: string
        stats: Record<string, unknown>
      }): void => cb(payload)
      ipcRenderer.on('rename:progress', listener)
      return () => ipcRenderer.removeListener('rename:progress', listener)
    }
  },
  util: {
    copy: (text: string) => ipcRenderer.invoke('app:copy', text),
    openUrl: (url: string) => ipcRenderer.invoke('app:openUrl', url)
  }
})
