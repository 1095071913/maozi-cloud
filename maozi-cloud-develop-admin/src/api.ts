/**
 * 渲染进程对 preload 暴露能力的类型化封装
 */
import type {
  EnvFile,
  EnvSaveParams,
  EnvVarEntry,
  HostsEntry,
  LaunchctlParams,
  PlatformResult
} from '../electron/platform/types'
import type {
  DevToolInfo,
  GpuInfo,
  NetInterface,
  PublicIPInfo,
  SysDynamicInfo,
  SysStaticInfo
} from '../electron/system/sysinfo'
import type {Bookmark, PageMeta} from '../electron/bookmarks/types'
import type {AuthType, ConfigEntry, ConfigType, ToolAvailability} from '../electron/configs/types'
import type {ComposeServiceStats, EnvSettingGroup, EnvSettingItem, ProjectBinding} from '../electron/projects/types'

export type { EnvFile, EnvVarEntry, EnvSaveParams, LaunchctlParams, HostsEntry, PlatformResult }
export type { SysStaticInfo, SysDynamicInfo, NetInterface, PublicIPInfo, GpuInfo, DevToolInfo }
export type {
  Bookmark,
  PageMeta,
  ConfigEntry,
  ConfigType,
  AuthType,
  ToolAvailability,
  ProjectBinding,
  EnvSettingGroup,
  EnvSettingItem,
  ComposeServiceStats
}

/** 应用服务（单体/微服务）条目：name 为服务名，file 标记归属 compose 文件（admin/services） */
export interface AppServiceEntry {
  name: string
  file: 'admin' | 'services'
}

export interface IpcResult<T> {
  ok: boolean
  error?: string
  data?: T
}

export interface ElectronApi {
  app: {
    info: () => Promise<{ platform: string; hostsPath: string }>
  }
  env: {
    list: () => Promise<{ files: EnvFile[]; vars: EnvVarEntry[] }>
    save: (params: EnvSaveParams) => Promise<PlatformResult>
    launchctl: (params: LaunchctlParams) => Promise<PlatformResult>
  }
  hosts: {
    list: () => Promise<{ ok: boolean; error?: string; entries: HostsEntry[] }>
    save: (entries: HostsEntry[]) => Promise<PlatformResult>
    flushDns: () => Promise<PlatformResult>
    readRaw: () => Promise<IpcResult<string>>
    saveRaw: (text: string) => Promise<PlatformResult>
  }
  sysinfo: {
    static: () => Promise<IpcResult<SysStaticInfo>>
    dynamic: () => Promise<IpcResult<SysDynamicInfo>>
    network: () => Promise<IpcResult<{ interfaces: NetInterface[]; primaryIP: string }>>
    publicIP: () => Promise<IpcResult<PublicIPInfo>>
    devtools: () => Promise<IpcResult<DevToolInfo[]>>
  }
  bookmarks: {
    list: () => Promise<IpcResult<Bookmark[]>>
    save: (input: Partial<Bookmark>) => Promise<IpcResult<Bookmark>>
    remove: (id: string) => Promise<PlatformResult>
    fetchMeta: (url: string) => Promise<IpcResult<PageMeta>>
    reorder: (ids: string[]) => Promise<PlatformResult>
  }
  projects: {
    state: () => Promise<IpcResult<ProjectBinding | null>>
    pickDir: () => Promise<IpcResult<string | null>>
    bindDir: (dir: string) => Promise<IpcResult<ProjectBinding>>
    clone: (secretId: string, destDir: string) => Promise<IpcResult<ProjectBinding>>
    unbind: () => Promise<PlatformResult>
    /** 微服务全量启动（执行项目内部署脚本，日志经 onScriptLog 推送） */
    runScript: (key: string, sid?: string) => Promise<PlatformResult>
    composeServices: () => Promise<IpcResult<string[]>>
    composeAction: (service: string, action: string, sid?: string) => Promise<PlatformResult>
    composeStatus: () => Promise<IpcResult<Record<string, string>>>
    /** 基础服务实时资源占用（CPU/内存，按服务名索引；hostMemTotal 为物理内存，未设配额时作分母） */
    composeStats: () => Promise<IpcResult<{ stats: Record<string, ComposeServiceStats>; cpuCount: number; hostMemTotal: number } | null>>
    /**
     * 服务日志查询：最近 tail 行。ctx 为 compose 上下文 —— 'basics'（默认，基础服务）
     * 或 '<monomer|distributeds>:<admin|services>'（应用服务，-f 指定 compose 文件）
     */
    composeLogs: (service: string, tail?: number, ctx?: string) => Promise<IpcResult<string[]>>
    /** 服务日志实时跟踪（ctx 同 composeLogs；行经 onComposeLog 推送，停止复用 stopScript） */
    composeLogsFollow: (service: string, tail: number, sid?: string, ctx?: string) => Promise<PlatformResult>
    /** 应用服务清单（variant: monomer 单体 / distributeds 微服务） */
    appServices: (variant: string) => Promise<IpcResult<{ services: AppServiceEntry[] }>>
    /** 应用服务运行状态（服务名 → State） */
    appServicesStatus: (variant: string) => Promise<IpcResult<Record<string, string>>>
    /** 应用服务实时资源占用（两变体一次性快照，按服务名索引） */
    appServicesStats: () => Promise<
      IpcResult<{ stats: Record<'monomer' | 'distributeds', Record<string, ComposeServiceStats>>; cpuCount: number; hostMemTotal: number } | null>
    >
    /** 应用服务单服务操作（start/stop/restart；start/restart 前后端自动互斥关闭另一变体） */
    appServiceAction: (
      variant: string,
      file: string,
      service: string,
      action: string,
      sid?: string
    ) => Promise<PlatformResult>
    /** 应用服务全量启动/关闭（start 前自动互斥关闭另一变体） */
    appServiceAll: (variant: string, action: string, sid?: string) => Promise<PlatformResult>
    /** UI 状态（Tab 选中 等）：.ui-state.json 持久化 */
    uiStateGet: () => Promise<IpcResult<Record<string, unknown>>>
    uiStateSave: (patch: Record<string, unknown>) => Promise<PlatformResult>
    /** 订阅基础服务日志跟踪输出；返回取消订阅函数 */
    onComposeLog: (
      cb: (payload: { kind: 'line' | 'update'; text: string; sid?: string }) => void
    ) => () => void
    adminContainerStatus: () => Promise<IpcResult<string>>
    adminContainerAction: (action: string, sid?: string) => Promise<PlatformResult>
    /** 初始化 Hosts：项目 maozi-cloud-deploy-run/hosts 追加进系统 /etc/hosts，已设置的忽略 */
    hostsInitStatus: () => Promise<IpcResult<{ total: number; missing: number; initialized: boolean } | null>>
    hostsInit: (sid?: string) => Promise<PlatformResult>
    /** 容器网络：解析 compose 的 networks.default.external.name 并检查 docker 中是否已存在 */
    networkStatus: () => Promise<IpcResult<{ name: string; exists: boolean }>>
    /** 创建容器网络（docker network create，已存在则幂等成功） */
    networkCreate: (sid?: string) => Promise<PlatformResult>
    /** 数据库初始化状态：INIT_MYSQL_DB（每行一个脚本路径）是否已执行过标记（count=0 表示未定义，隐藏按钮） */
    dbInitStatus: () => Promise<IpcResult<{ count: number; initialized: boolean; missing: number }>>
    /** 初始化数据库：按需启动 mysql、逐个导入 SQL 脚本 */
    dbInit: (sid?: string) => Promise<PlatformResult>
    /** 环境设置：解析项目 ENVIRONMENT_VARIABLE 并实时读取环境变量当前值 */
    envSettings: () => Promise<IpcResult<{ groups: EnvSettingGroup[]; files: EnvFile[]; defaultFileId: string } | null>>
    /** 中断当前正在执行的脚本（进程组 SIGINT/SIGKILL） */
    stopScript: (sid?: string) => Promise<PlatformResult>
    onScriptLog: (
      cb: (payload: { kind: 'line' | 'update'; text: string; sid?: string }) => void
    ) => () => void
    openDir: () => Promise<PlatformResult>
    /** 订阅 git clone 实时日志；返回取消订阅函数 */
    onCloneLog: (cb: (payload: { kind: 'line' | 'update'; text: string }) => void) => () => void
  }
  configs: {
    list: () => Promise<IpcResult<{ configs: ConfigEntry[]; tools: ToolAvailability }>>
    save: (input: Partial<ConfigEntry>) => Promise<IpcResult<ConfigEntry>>
    remove: (id: string) => Promise<PlatformResult>
    login: (id: string) => Promise<IpcResult<string>>
  }
  util: {
    copy: (text: string) => Promise<PlatformResult>
    openUrl: (url: string) => Promise<PlatformResult>
  }
}

export const api: ElectronApi = window.api
