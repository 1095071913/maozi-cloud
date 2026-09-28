/** 项目控制台公共类型 */

export interface ProjectBinding {
  /** 项目名称（CONFIG 的 name） */
  name: string
  /** 项目版本号（CONFIG 的 version） */
  version: string
  /** 项目根目录（本地路径或远程路径） */
  path: string
  boundAt: number
  /** 远程绑定时记录服务器信息（本地绑定为空） */
  remote?: {
    configId: string
    configName: string
    user: string
    host: string
    port: number
  }
}

/** 环境设置条目：environment_variable.json 中一条「中文名称 → 环境变量 key」及其当前值 */
export interface EnvSettingItem {
  /** 中文名称（environment_variable.json 的 key） */
  label: string
  /** 环境变量 key（environment_variable.json 的 value） */
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

/** 环境设置分组：节内直接映射的变量归入 name 为空的组 */
export interface EnvSettingGroup {
  name: string
  items: EnvSettingItem[]
}

/** 环境设置分节（弹窗里的 Tab）：对应 environment_variable.json 的一个一级属性。
 *  第一节读系统/shell 环境变量（原行为），第二节读写业务 docker 编排的 .env 文件 */
export interface EnvSettingSection {
  /** 一级属性名（Tab 标题） */
  name: string
  /** 取值来源：env = 系统/shell 环境变量；file = 项目内 .env 文件 */
  source: 'env' | 'file'
  /** source=file 时目标文件相对路径（展示用） */
  file?: string
  /** 节内分组 */
  groups: EnvSettingGroup[]
}

/** 应用服务（单体/微服务）条目：name 为插值后的真实服务名，file 标记归属 compose 文件 */
export interface AppServiceEntry {
  /** 真实服务名（yml 中 ${VAR} 占位已按 系统/业务.env 环境插值；docker compose 命令与状态匹配用） */
  name: string
  /** 归属 compose 文件（services 后端 / admin 前端后台 / nginx 流量入口） */
  file: 'admin' | 'services' | 'nginx'
  /** 服务名含占位时的静态前缀（如 maozi-cloud-admin-monomer），前端展示用；无占位时与 name 相同 */
  base?: string
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
