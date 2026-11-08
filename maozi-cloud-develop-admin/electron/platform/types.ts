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
  /** 变量名 */
  key: string
  /** 变量值（已去除引号与行内注释） */
  value: string
  /** 所属配置文件 id */
  fileId: string
  /** 是否由本应用托管（位于托管区块内） */
  managed: boolean
  /** 是否启用（禁用 = 该行被注释） */
  enabled?: boolean
}

/** env:list 返回结构 */
export interface EnvListResult {
  /** 候选 shell 配置文件 */
  files: EnvFile[]
  /** 各文件解析出的全部变量 */
  vars: EnvVarEntry[]
}

/** 环境变量保存模式 */
export type EnvSaveMode = 'add' | 'update' | 'remove' | 'reorder' | 'toggle' | 'rename'

/** 环境变量保存参数 */
export interface EnvSaveParams {
  mode: EnvSaveMode
  /** reorder 模式下不需要；mode=rename 时为旧变量名 */
  key?: string
  /** 变量值；mode=rename 时留空则沿用原值 */
  value?: string
  /** 目标配置文件 id */
  fileId: string
  /** mode=rename：新变量名 */
  newKey?: string
  /** mode=reorder：该文件内的变量按此顺序重排 export 行 */
  orderedKeys?: string[]
  /** mode=toggle：目标启用状态（true=取消注释，false=注释该行） */
  enabled?: boolean
}

/** launchctl 操作：设置 / 移除 */
export type LaunchctlAction = 'set' | 'unset'

/** launchctl 同步参数 */
export interface LaunchctlParams {
  action: LaunchctlAction
  /** 变量名 */
  key: string
  /** 变量值，action=set 时必填 */
  value?: string
}

/** hosts 文件中的一行 */
export interface HostsEntry {
  /** 前端生成的行 id */
  id: string
  /** entry = IP 映射行；comment = 纯注释/说明行 */
  kind: 'entry' | 'comment'
  /** IP 地址（kind=entry） */
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

/** 平台操作通用返回结构 */
export interface PlatformResult {
  ok: boolean
  /** 失败原因 */
  error?: string
}

/** 平台实现接口：环境变量与 hosts 管理 */
export interface Platform {
  /** 平台标识，用于前端展示 */
  id: 'darwin' | 'win32' | 'linux'
  /** hosts 文件绝对路径 */
  hostsPath(): string
  /** 列出可管理的环境变量配置文件 */
  listEnvFiles(): EnvFile[]
  /** 读取所有配置文件中的环境变量 */
  readEnvVars(): EnvVarEntry[]
  /** 按 mode 保存环境变量 */
  saveEnvVar(params: EnvSaveParams): void
  /** 读取并解析 hosts */
  readHosts(): HostsEntry[]
  /** 写入 hosts（内部完成提权写入与 DNS 缓存刷新） */
  writeHosts(entries: HostsEntry[]): Promise<void>
  /** 读取 hosts 原始文本（源文件编辑弹窗用） */
  readHostsRaw(): string
  /** 按原始文本写入 hosts（提权与 DNS 缓存刷新同 writeHosts） */
  writeHostsRaw(text: string): Promise<void>
  /** 仅刷新 DNS 缓存 */
  flushDns(): Promise<void>
  /** 将变量同步到图形应用环境（macOS: launchctl setenv） */
  launchctlSet(params: LaunchctlParams): Promise<void>
}
