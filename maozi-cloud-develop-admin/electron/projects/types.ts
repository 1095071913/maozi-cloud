/** 项目控制台公共类型 */

export interface ProjectBinding {
  /** 项目名称（CONFIG 的 name） */
  name: string
  /** 项目版本号（CONFIG 的 version） */
  version: string
  /** 本地项目根目录 */
  path: string
  boundAt: number
}

/** 环境设置条目：ENVIRONMENT_VARIABLE 中一条「中文名称 → 环境变量 key」及其当前值 */
export interface EnvSettingItem {
  /** 中文名称（ENVIRONMENT_VARIABLE 的 key） */
  label: string
  /** 环境变量 key（ENVIRONMENT_VARIABLE 的 value） */
  key: string
  /** 当前值（未设置为空串） */
  value: string
  /** 是否读取到值 */
  found: boolean
  /** 值来自被注释禁用的行时为 false */
  enabled: boolean
  /** 值来源展示名（如 ~/.zshrc、进程环境） */
  source: string
  /** 来源配置文件 id（如 zshrc）；来自进程环境/未设置时为空，保存时落到默认文件 */
  fileId?: string
}

/** 环境设置分组：顶层直接映射的变量归入 name 为空的组 */
export interface EnvSettingGroup {
  name: string
  items: EnvSettingItem[]
}

/** 单个基础服务容器的实时资源占用 */
export interface ComposeServiceStats {
  /** CPU 百分比（docker 语义：相对单核，多核可超 100） */
  cpuPercent: number
  /** 已用内存（字节） */
  memUsed: number
  /** 容器内存配额（字节）；0 = 未设置限制（docker stats 显示的宿主机内存不算配额） */
  memLimit: number
  /** CPU 配额（核数，如 2 = 2 核）；0 = 未设置限制 */
  cpusLimit: number
}
