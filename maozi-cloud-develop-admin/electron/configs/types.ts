/** 密钥管理公共类型 */

export type ConfigType = 'Git' | 'Docker' | 'Helm' | 'Linux' | ''

/** 密钥类型：账密（账号+密码）/ 密钥（单一 Token，无账号） */
export type AuthType = 'password' | 'key'

export interface ConfigEntry {
  id: string
  name: string
  /** Git / Docker / Helm，允许为空 */
  type: ConfigType
  /** 密钥类型：账密 / 密钥（必填，历史数据缺省按账密处理） */
  authType: AuthType
  /** 一级分组：归宿 */
  category1: string
  /** 二级分组：环境 */
  category2: string
  /** 地址（Git 仓库 / 镜像仓库 / Helm 仓库，允许为空） */
  address: string
  username: string
  /** 存储时经系统钥匙串加密，内存中明文 */
  password: string
  /** 默认密钥不可删除；用户新增的密钥为 false */
  isDefault: boolean
  sortOrder?: number
  createdAt: number
}

/** 本机 CLI 工具可用性（缺失时对应类型配置不可编辑/操作） */
export interface ToolAvailability {
  git: boolean
  docker: boolean
  helm: boolean
  linux: boolean
}
