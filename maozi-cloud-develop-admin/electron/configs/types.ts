/** 密钥管理公共类型 */

/** 密钥所属工具类型，空串表示未指定 */
export type ConfigType = 'Git' | 'Docker' | 'Helm' | 'Linux' | ''

/** 密钥类型：账密（账号+密码）/ 密钥（单一 Token，无账号） */
export type AuthType = 'password' | 'key'

/** 密钥条目 */
export interface ConfigEntry {
  /** 唯一标识（默认密钥为固定 id，其余为 UUID） */
  id: string
  /** 名称 */
  name: string
  /** Git / Docker / Helm / Linux，允许为空 */
  type: ConfigType
  /** 密钥类型：账密 / 密钥（必填，历史数据缺省按账密处理） */
  authType: AuthType
  /** 一级分组：归宿 */
  category1: string
  /** 二级分组：环境 */
  category2: string
  /** 地址（Git 仓库 / 镜像仓库 / Helm 仓库，允许为空） */
  address: string
  /** 登录账号（authType=key 时不使用） */
  username: string
  /** 存储时经系统钥匙串加密，内存中明文 */
  password: string
  /** 默认密钥不可删除；用户新增的密钥为 false */
  isDefault: boolean
  /** 排序序号，缺省按创建时间排 */
  sortOrder?: number
  /** 创建时间戳（毫秒） */
  createdAt: number
}

/** 本机 CLI 工具可用性（缺失时对应类型配置不可编辑/操作） */
export interface ToolAvailability {
  git: boolean
  docker: boolean
  helm: boolean
  /** 探测的是 ssh */
  linux: boolean
}
