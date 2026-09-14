/**
 * 平台抽象层公共类型
 * 渲染进程与主进程之间的数据契约，新增平台（如 linux）时保持接口不变
 */

/** 环境变量所属 shell 配置文件 */
export interface EnvFile {
  /** 配置文件 id，如 zshrc */
  id: string
  /** 展示名，如 ~/.zshrc */
  name: string
  /** 绝对路径 */
  path: string
  /** 文件是否存在（不存在时首次写入会创建） */
  exists: boolean
}

/** 从 shell 配置文件解析出的一条环境变量 */
export interface EnvVarEntry {
  key: string
  value: string
  /** 所属配置文件 id */
  fileId: string
  /** 是否由本应用托管（位于托管区块内） */
  managed: boolean
  /** 是否启用（禁用 = 该行被注释） */
  enabled?: boolean
}

export interface EnvListResult {
  files: EnvFile[]
  vars: EnvVarEntry[]
}

export type EnvSaveMode = 'add' | 'update' | 'remove' | 'reorder' | 'toggle'

export interface EnvSaveParams {
  mode: EnvSaveMode
  /** reorder 模式下不需要 */
  key?: string
  value?: string
  fileId: string
  /** mode=reorder：该文件内的变量按此顺序重排 export 行 */
  orderedKeys?: string[]
  /** mode=toggle：目标启用状态（true=取消注释，false=注释该行） */
  enabled?: boolean
}

export type LaunchctlAction = 'set' | 'unset'

export interface LaunchctlParams {
  action: LaunchctlAction
  key: string
  value?: string
}

/** hosts 文件中的一行 */
export interface HostsEntry {
  /** 前端生成的行 id */
  id: string
  /** entry = IP 映射行；comment = 纯注释/说明行 */
  kind: 'entry' | 'comment'
  ip?: string
  /** 域名，多个以空格分隔 */
  domains?: string
  /** 行内注释（# 之后的内容，不含 #） */
  comment?: string
  /** 被注释禁用的 entry 为 false */
  enabled: boolean
  /** comment 行的原始文本 */
  raw?: string
}

export interface PlatformResult {
  ok: boolean
  error?: string
}

export interface Platform {
  /** 平台标识，用于前端展示 */
  id: 'darwin' | 'win32' | 'linux'
  /** hosts 文件绝对路径 */
  hostsPath(): string
  listEnvFiles(): EnvFile[]
  readEnvVars(): EnvVarEntry[]
  saveEnvVar(params: EnvSaveParams): void
  readHosts(): HostsEntry[]
  /** 写入 hosts（内部完成提权、备份、DNS 缓存刷新） */
  writeHosts(entries: HostsEntry[]): Promise<void>
  /** 读取 hosts 原始文本（源文件编辑弹窗用） */
  readHostsRaw(): string
  /** 按原始文本写入 hosts（提权、备份、DNS 缓存刷新同 writeHosts） */
  writeHostsRaw(text: string): Promise<void>
  /** 仅刷新 DNS 缓存 */
  flushDns(): Promise<void>
  /** 将变量同步到图形应用环境（macOS: launchctl setenv） */
  launchctlSet(params: LaunchctlParams): Promise<void>
}
