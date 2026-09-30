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
    ProxyInfo,
    PublicIPInfo,
    SysDynamicInfo,
    SysStaticInfo
} from '../electron/system/sysinfo'
import type {Bookmark, PageMeta} from '../electron/bookmarks/types'
import type {AuthType, ConfigEntry, ConfigType, ToolAvailability} from '../electron/configs/types'
import type {
    AppServiceEntry,
    ComposeServiceStats,
    EnvSettingGroup,
    EnvSettingItem,
    EnvSettingSection,
    ProjectBinding,
    ProjectEntry
} from '../electron/projects/types'

export type { EnvFile, EnvVarEntry, EnvSaveParams, LaunchctlParams, HostsEntry, PlatformResult }
export type { SysStaticInfo, SysDynamicInfo, NetInterface, ProxyInfo, PublicIPInfo, GpuInfo, DevToolInfo }
export type {
  Bookmark,
  PageMeta,
  ConfigEntry,
  ConfigType,
  AuthType,
  ToolAvailability,
  ProjectBinding,
  ProjectEntry,
  EnvSettingGroup,
  EnvSettingItem,
  EnvSettingSection,
  ComposeServiceStats,
  AppServiceEntry
}

/** 应用服务（单体/微服务）条目：见 electron/projects/types.ts（转发保持渲染进程既有导入路径） */

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
    network: () => Promise<IpcResult<{ interfaces: NetInterface[]; primaryIP: string; proxy: ProxyInfo }>>
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
    /** 项目列表（含绑定信息与当前激活项 id） */
    projectsList: () => Promise<IpcResult<{ projects: ProjectEntry[]; activeId: string | null }>>
    /** 创建项目（名称不允许中文，仅字母数字与 . _ -），创建后即激活并进入选择绑定方式 */
    projectCreate: (name: string, alias: string, remark: string) => Promise<IpcResult<ProjectEntry>>
    /** 激活项目：已绑定返回绑定信息，待绑定返回 null */
    projectActivate: (id: string) => Promise<IpcResult<ProjectBinding | null>>
    /** 更新项目信息（名称/别名/备注），名称校验同创建 */
    projectUpdate: (id: string, name: string, alias: string, remark: string) => Promise<IpcResult<ProjectEntry>>
    /** 返回项目列表：清空激活项（绑定保留在项目记录中） */
    projectDeactivate: () => Promise<IpcResult<null>>
    /** 删除项目（含其绑定信息） */
    projectRemove: (id: string) => Promise<IpcResult<null>>
    pickDir: () => Promise<IpcResult<string | null>>
    bindDir: (dir: string) => Promise<IpcResult<ProjectBinding>>
    clone: (secretId: string, destDir: string) => Promise<IpcResult<ProjectBinding>>
    unbind: () => Promise<PlatformResult>
    /** 微服务全量启动（执行项目内部署脚本，日志经 onScriptLog 推送） */
    runScript: (key: string, sid?: string) => Promise<PlatformResult>
    composeServices: () => Promise<IpcResult<string[]>>
    composeAction: (service: string, action: string, sid?: string) => Promise<PlatformResult>
    composeStatus: () => Promise<IpcResult<Record<string, string>>>
    /** 基础服务实时资源占用（CPU/内存，按服务名索引；hostMemTotal 为物理内存，未设配额时作分母）。preferCache=首屏命中启动预取快照 */
    composeStats: (preferCache?: boolean) => Promise<IpcResult<{ stats: Record<string, ComposeServiceStats>; cpuCount: number; hostMemTotal: number } | null>>
    /**
     * 服务日志查询：最近 tail 行。ctx 为 compose 上下文 —— 'basics'（默认，基础服务）
     * 或 '<monomer|distributeds>:<admin|services>'（应用服务，-f 指定 compose 文件）
     */
    composeLogs: (service: string, tail?: number, ctx?: string) => Promise<IpcResult<string[]>>
    /** 服务日志实时跟踪（ctx 同 composeLogs；行经 onComposeLog 推送，停止复用 stopScript） */
    composeLogsFollow: (service: string, tail: number, sid?: string, ctx?: string) => Promise<PlatformResult>
    /** 应用服务清单（variant: monomer 单体 / distributeds 微服务） */
    appServices: (variant: string) => Promise<IpcResult<{ services: AppServiceEntry[]; env?: { ENVIRONMENT: string; VERSION: string } }>>
    /** 应用服务运行状态（服务名 → State） */
    appServicesStatus: (
      variant: string
    ) => Promise<IpcResult<{ states: Record<string, string>; healths: Record<string, string> }>>
    /** 应用服务实时资源占用（两变体一次性快照，按服务名索引）。preferCache=首屏命中启动预取快照 */
    appServicesStats: (preferCache?: boolean) => Promise<
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
    appServiceAll: (variant: string, action: string, sid?: string, opts?: { foreground?: boolean }) => Promise<PlatformResult>
    /** 全链路日志查询：按链路 ID 检索所有运行中微服务容器的日志行 */
    traceLogQuery: (
      traceId: string,
      tail?: number
    ) => Promise<IpcResult<{ results: string[]; message: string }>>
    /** 远程服务器（SSH）：可用 Linux 密钥列表 */
    sshConfigs: () => Promise<IpcResult<Array<{ id: string; name: string; address: string }>>>
    /** SSH 连接测试 */
    sshTest: (configId: string) => Promise<IpcResult<string | null>>
    /** 远程目录列表（一级子目录） */
    sshListDir: (
      configId: string,
      path: string
    ) => Promise<IpcResult<{ path: string; items: Array<{ name: string; isDir: boolean }> }>>
    /** 在远程目录下创建子目录（选择目录页新建） */
    sshMkdir: (configId: string, parentDir: string, name: string) => Promise<IpcResult<null>>
    /** 远程绑定：读取远程 CONFIG 完成绑定 */
    sshBindDir: (configId: string, path: string) => Promise<IpcResult<ProjectBinding>>
    /** 检查远程是否安装 git */
    sshCheckGit: (configId: string) => Promise<IpcResult<boolean>>
    /** 远程拉取代码（SSH git clone），日志走 onScriptLog */
    sshClone: (configId: string, gitSecretId: string, destDir: string, sid?: string) => Promise<IpcResult<ProjectBinding>>
    /** UI 状态（Tab 选中 等）：.ui-state.json 持久化 */
    sshEnvSave: (params: { key: string; value: string; fileId: string }) => Promise<PlatformResult>
    uiStateGet: () => Promise<IpcResult<Record<string, unknown>>>
    uiStateSave: (patch: Record<string, unknown>) => Promise<PlatformResult>
    /** 订阅基础服务日志跟踪输出；返回取消订阅函数 */
    onComposeLog: (
      cb: (payload: { kind: 'line' | 'update'; text: string; sid?: string }) => void
    ) => () => void
    adminContainerStatus: () => Promise<IpcResult<string>>
    adminContainerAction: (action: string, sid?: string) => Promise<PlatformResult>
    /** 初始化 Hosts：项目 maozi-cloud-deploy-run/init_hosts.json 追加进系统 /etc/hosts，已设置的忽略 */
    hostsInitStatus: () => Promise<IpcResult<{ total: number; missing: number; initialized: boolean } | null>>
    hostsInit: (sid?: string) => Promise<PlatformResult>
    /** 容器网络：解析 compose 的 networks.default.external.name 并检查 docker 中是否已存在 */
    networkStatus: () => Promise<IpcResult<{ name: string; exists: boolean }>>
    /** 创建容器网络（docker network create，已存在则幂等成功） */
    networkCreate: (sid?: string) => Promise<PlatformResult>
    /** 数据库初始化状态：init_mysql_db.json（JSON 数组脚本路径）是否已执行过标记（count=0 表示未定义，隐藏按钮） */
    dbInitStatus: () => Promise<IpcResult<{ count: number; initialized: boolean; missing: number }>>
    /** 初始化数据库：按需启动 mysql、逐个导入 SQL 脚本 */
    dbInit: (sid?: string) => Promise<PlatformResult>
    /** 环境设置：解析项目 environment_variable.json 并实时读取环境变量当前值 */
    envSettings: () => Promise<IpcResult<{ sections: EnvSettingSection[]; files: EnvFile[]; defaultFileId: string; remote?: boolean } | null>>
    /** 环境设置 .env 分节保存：原位更新 key=value 行（无则末尾追加），本地直写 / 远程 SSH */
    envFileSave: (input: { key: string; value: string }) => Promise<PlatformResult>
    /** Maven 配置：mvn -v 解析安装目录与版本，定位 conf/settings.xml */
    mavenInfo: () => Promise<IpcResult<{ version: string; home: string; settingsFile: string; exists: boolean }>>
    /** 读取 Maven settings.xml 内容 */
    mavenConfigRead: () => Promise<IpcResult<{ content: string }>>
    /** 保存 Maven settings.xml（覆盖前备份为 settings.xml.bak.<时间戳>） */
    mavenConfigSave: (content: string) => Promise<IpcResult<{ backupFile: string }>>
    /** Docker 配置：版本 + daemon.json 按平台解析（mac/Windows=~/.docker，Linux=/etc/docker 优先，远程按 Linux） */
    dockerInfo: () => Promise<IpcResult<{ version: string; platform: string; configFile: string; exists: boolean }>>
    /** 读取 Docker daemon.json 内容 */
    dockerConfigRead: () => Promise<IpcResult<{ content: string }>>
    /** 保存 Docker daemon.json（覆盖前自动备份；修改后需重启 Docker 生效） */
    dockerConfigSave: (content: string) => Promise<IpcResult<{ backupFile: string }>>
    /** 应用服务配置：按服务名匹配 environment_variable.json 一级属性，条目值读业务 .env；未匹配返回 data:null */
    serviceConfig: (
      service: string,
      base?: string
    ) => Promise<IpcResult<{ section: string; groups: EnvSettingGroup[]; file: string } | null>>
    /** 应用服务「接口不停机更新」开关（variant = monomer | distributeds）：开启生成热备 yml；关闭回收热备容器并删除文件（状态持久化） */
    hotSwap: (
      variant: string,
      enable: boolean,
      sid?: string
    ) => Promise<{ ok: boolean; error?: string; data?: boolean }>
    /** 初始化镜像列表：解析 init_base_image.json 并对照 docker images 返回存在状态 */
    initImages: () => Promise<IpcResult<{ images: Array<{ name: string; dir: string; exists: boolean }> }>>
    /** 构建初始化镜像：进入定义目录 docker buildx build -f Dockerfile -t <name> .（日志走会话） */
    initImageBuild: (name: string, sid?: string) => Promise<PlatformResult>
    /** 中断当前正在执行的脚本（进程组 SIGINT/SIGKILL） */
    stopScript: (sid?: string) => Promise<PlatformResult>
    onScriptLog: (
      cb: (payload: { kind: 'line' | 'update'; text: string; sid?: string }) => void
    ) => () => void
    openDir: () => Promise<PlatformResult>
    /** 项目 git 管理：仓库检测（含当前分支与本地最后一次提交短 sha/时间） */
    gitInfo: () => Promise<IpcResult<{ isRepo: boolean; branch: string; lastSha: string; lastTime: string }>>
    /** 定时检测远程新提交：fetch 后统计本地落后上游的提交数（失败静默返回 behind=0）；附带本地最后一次提交短 sha 与时间 */
    gitRemoteCheck: () => Promise<IpcResult<{ isRepo: boolean; branch: string; behind: number; lastSha: string; lastTime: string }>>
    /** 远程分支列表（需网络） */
    gitBranches: () => Promise<IpcResult<{ branches: string[] }>>
    /** 拉取代码（git pull），日志走 onScriptLog */
    gitPull: (sid?: string) => Promise<PlatformResult>
    /** 切换分支，日志走 onScriptLog */
    gitCheckout: (branch: string, sid?: string) => Promise<PlatformResult>
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

/**
 * Electron preload 注入的桥。__apiStub 仅浏览器调试用（可加载后任意时刻注入，
 * 按调用时取值路由）；Electron 内恒走 window.api，代理分支不生效
 */
const lazyStubApi = (): ElectronApi =>
  new Proxy({} as ElectronApi, {
    get: (_t, section) =>
      new Proxy(
        {},
        {
          get: (_s, method) =>
            (...args: unknown[]) => {
              const root = (window as unknown as Record<string, unknown>)[`__apiStub_${String(section)}`] as
                | Record<string, unknown>
                | undefined
              const fn = root?.[String(method)]
              if (typeof fn === 'function') return (fn as (...a: unknown[]) => unknown)(...args)
              return Promise.resolve({ ok: true, data: null })
            }
        }
      )
  })
export const api: ElectronApi = window.api ?? lazyStubApi()
