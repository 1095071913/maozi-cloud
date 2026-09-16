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
    devtools: () => ipcRenderer.invoke('sysinfo:devtools')
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
    /** 基础服务日志：一次性查询最近 N 行 */
    composeLogs: (service: string, tail?: number, ctx?: string) => ipcRenderer.invoke('projects:composeLogs', service, tail, ctx),
    /** 基础服务日志实时跟踪（-f 流式，停止复用 stopScript） */
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
    /** UI 状态持久化（Tab 选中 等，.ui-state.json） */
    sshEnvSave: (params: { key: string; value: string; fileId: string }) =>
      ipcRenderer.invoke('projects:sshEnvSave', params),
    uiStateGet: () => ipcRenderer.invoke('projects:uiStateGet'),
    uiStateSave: (patch: Record<string, unknown>) => ipcRenderer.invoke('projects:uiStateSave', patch),
    /** 数据库初始化状态：INIT_MYSQL_DB 定义的表是否均已存在 */
    dbInitStatus: () => ipcRenderer.invoke('projects:dbInitStatus'),
    /** 初始化数据库：按需启动 mysql、检测表、导入缺失脚本 */
    dbInit: (sid?: string) => ipcRenderer.invoke('projects:dbInit', sid),
    envSettings: () => ipcRenderer.invoke('projects:envSettings'),
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
    /** 远程分支列表（git ls-remote --heads，需网络） */
    gitBranches: () => ipcRenderer.invoke('projects:gitBranches'),
    /** 拉取代码（git pull），日志走 onScriptLog */
    gitPull: (sid?: string) => ipcRenderer.invoke('projects:gitPull', sid),
    /** 切换分支（fetch 目标分支后 checkout），日志走 onScriptLog */
    gitCheckout: (branch: string, sid?: string) => ipcRenderer.invoke('projects:gitCheckout', branch, sid),
    onCloneLog: (cb: (payload: { kind: 'line' | 'update'; text: string }) => void) => {
      const listener = (_e: unknown, payload: { kind: 'line' | 'update'; text: string }): void => cb(payload)
      ipcRenderer.on('projects:cloneLog', listener)
      return () => ipcRenderer.removeListener('projects:cloneLog', listener)
    }
  },
  configs: {
    list: () => ipcRenderer.invoke('configs:list'),
    save: (input: unknown) => ipcRenderer.invoke('configs:save', input),
    remove: (id: string) => ipcRenderer.invoke('configs:remove', id),
    login: (id: string) => ipcRenderer.invoke('configs:login', id)
  },
  util: {
    copy: (text: string) => ipcRenderer.invoke('app:copy', text),
    openUrl: (url: string) => ipcRenderer.invoke('app:openUrl', url)
  }
})
