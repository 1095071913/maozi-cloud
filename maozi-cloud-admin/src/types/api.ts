/** 统一响应结构 */
export interface ApiResponse<T = unknown> {
  /** 业务内码，200 为成功 */
  code: number
  /** 数据 */
  data: T
  /** 错误信息（code 非 200 时返回） */
  message?: string
}

/** 图片上传结果（/system/image/upload） */
export interface ImageUploadResult {
  /** 文件原始名称 */
  fileName: string
  /** 文件大小（字节） */
  fileSize: string
  /** 对象存储文件路径 */
  fileKey: string
  /** 文件访问地址 */
  url: string
}

/** 系统详情（项目信息，/system/config/system/get） */
export interface SystemInfo {
  /** 项目名称 */
  projectName: string
  /** 公司名称 */
  corporationName: string
  /** 项目图标 */
  icon: string
  /** 项目环境 */
  environment: string
  /** 项目描述 */
  description: string
  /** 项目版权 */
  copyright: string
}

/** OAuth2 令牌返回结果 */
export interface TokenResult {
  /** 访问令牌 */
  access_token: string
  /** 刷新令牌 */
  refresh_token: string
  /** 令牌类型，固定 Bearer */
  token_type: string
  /** 访问令牌有效期（秒） */
  expires_in: string
  /** 刷新令牌有效期（秒） */
  refresh_token_expires_in: string
  /** 授权范围 */
  scope: string
}

/** 用户个人详情 */
export interface UserInfo {
  /** 名称 */
  name: string
  /** 头像 */
  icon: string
  /** 权限标识列表（用户自身拥有的 mark 集合） */
  permissions: string[]
}

/** 个人信息更新参数（/system/user/individual/update） */
export interface UserIndividualUpdateParam {
  /** 名称 */
  name: string
  /** 头像 */
  icon: string
  /** 旧密码（修改密码时必填） */
  password?: string
  /** 新密码（与旧密码一同留空则不修改密码） */
  newPassword?: string
}

/** 权限类型：0=目录 1=菜单 2=按钮 */
export enum PermissionType {
  /** 目录 - 一级导航目录 */
  DIRECTORY = 0,
  /** 菜单 - 具体的功能菜单页面 */
  MENU = 1,
  /** 按钮 - 页面中的操作按钮权限 */
  BUTTON = 2
}

/** 权限项（/system/permission/list 返回元素） */
export interface PermissionItem {
  /** 权限 ID */
  id: string | number
  /** 上级权限 ID */
  parentId: string | number
  /** 名称 */
  name: string
  /** 图标 */
  icon: string
  /** 权限唯一标识编码，如 system:user */
  mark: string
  /** 类型（0=目录 1=菜单 2=按钮） */
  type: PermissionType
  /** 排序（升序，越小越靠前） */
  sort?: number
}

/** 权限树节点（带 children） */
export interface PermissionTreeNode extends PermissionItem {
  /** 子节点 */
  children?: PermissionTreeNode[]
}

/** 权限下拉项（/system/permission/dropDownList 返回元素） */
export interface PermissionOptionItem {
  /** 权限 ID */
  id: string | number
  /** 上级权限 ID */
  parentId: string | number
  /** 名称 */
  name: string
  /** 类型（0=目录 1=菜单 2=按钮） */
  type: PermissionType
  /** 排序（升序，越小越靠前） */
  sort?: number
  /** 深度 */
  level?: number
}

/** OAuth2 授权类型：0=授权码 1=客户端 2=刷新令牌 3=密码 */
export enum AuthType {
  /** 授权码模式 */
  AUTHORIZATION_CODE = 0,
  /** 客户端模式 */
  CLIENT_CREDENTIALS = 1,
  /** 刷新令牌模式 */
  REFRESH_TOKEN = 2,
  /** 密码模式 */
  PASSWORD = 3
}

/** 权限详情（/system/permission/{id}/get） */
export interface PermissionDetail extends PermissionItem {
  /** 路由 */
  route?: string
  /** 服务地址 */
  serviceUri?: string
  /** 深度 */
  level?: number
}

/** 权限保存/更新参数 */
export interface PermissionSaveParam {
  /** 名称 */
  name: string
  /** 标识 */
  mark: string
  /** 类型 */
  type: PermissionType
  /** 上级 ID */
  parentId: string | number
  /** 图标 */
  icon?: string
  /** 路由 */
  route?: string
  /** 服务地址 */
  serviceUri?: string
  /** 排序 */
  sort?: number
  /** 深度 */
  level: number
}

/** 角色列表行（/system/role/list 返回元素） */
export interface RoleListItem {
  /** 角色 ID */
  id: string | number
  /** 名称 */
  name: string
  /** 描述 */
  description?: string
  /** 状态（0=禁用 1=启用） */
  status: number
  /** 创建时间 */
  createTime?: string
}

/** 角色详情（/system/role/{id}/get） */
export interface RoleDetail {
  /** 名称 */
  name: string
  /** 描述 */
  description?: string
  /** 状态 */
  status: number
  /** 已绑定权限 ID 列表 */
  permissionIds?: (string | number)[]
}

/** 角色保存/更新参数 */
export interface RoleSaveParam {
  /** 名称 */
  name: string
  /** 描述 */
  description?: string
  /** 状态 */
  status: number
  /** 绑定权限 ID 列表 */
  bindPermissionIds?: (string | number)[]
  /** 解绑权限 ID 列表 */
  unbindPermissionIds?: (string | number)[]
}

/** 客户端列表行（/oauth/client/list 返回元素） */
export interface ClientListItem {
  /** 主键 ID */
  id: string | number
  /** 客户端 ID */
  clientId: string
  /** 名称 */
  clientName: string
  /** 授权令牌有效期（秒） */
  accessTokenValiditySeconds?: string
  /** 刷新令牌有效期（秒） */
  refreshTokenValiditySeconds?: string
  /** 状态（0=禁用 1=启用） */
  status: number
}

/** 客户端详情（/oauth/client/{id}/get） */
export interface ClientDetail {
  /** 客户端 ID */
  clientId: string
  /** 名称 */
  name: string
  /** 授权令牌有效期（秒） */
  accessTokenValiditySeconds?: string
  /** 刷新令牌有效期（秒） */
  refreshTokenValiditySeconds?: string
  /** 授权类型列表（0/1/2/3） */
  authorizationGrantTypes?: number[]
  /** 备注 */
  remark?: string
  /** 状态 */
  status: number
}

/** 客户端保存/更新参数 */
export interface ClientSaveParam {
  /** 名称 */
  name: string
  /** 客户端密钥 */
  clientSecret?: string
  /** 授权令牌有效期（秒） */
  accessTokenValiditySeconds?: string
  /** 刷新令牌有效期（秒） */
  refreshTokenValiditySeconds?: string
  /** 授权类型列表 */
  authorizationGrantTypes: number[]
  /** 备注 */
  remark?: string
  /** 状态 */
  status: number
}

/** 通用状态：0=禁用 1=启用 */
export enum Status {
  /** 禁用 */
  DISABLE = 0,
  /** 启用 */
  ENABLE = 1
}

/** 下拉选项（客户端/角色等通用） */
export interface OptionItem {
  /** ID */
  id: string | number
  /** 名称 */
  name: string
}

/** 分页结果 */
export interface PageResult<T> {
  /** 当前页 */
  current: string | number
  /** 每页数量 */
  size: string | number
  /** 数据总数 */
  total: string | number
  /** 数据列表 */
  data: T[]
}

/** 用户列表行（/system/user/list 返回元素） */
export interface UserListItem {
  /** 用户 ID */
  id: string | number
  /** 名称 */
  name: string
  /** 所属客户端 */
  client?: OptionItem
  /** 状态（0=禁用 1=启用） */
  status: number
  /** 创建时间 */
  createTime?: string
}

/** 用户详情（/system/user/{id}/get，用于编辑回显） */
export interface UserDetail {
  /** 账号 */
  username: string
  /** 名称 */
  name: string
  /** 图标 */
  icon: string
  /** 状态 */
  status: number
  /** 所属客户端 */
  client?: OptionItem
  /** 已绑定角色 ID 列表 */
  roleIds?: (string | number)[]
}

/** 用户保存/更新参数 */
export interface UserSaveParam {
  /** 账号 */
  username: string
  /** 名称 */
  name: string
  /** 密码 */
  password: string
  /** 图标 */
  icon: string
  /** 状态 */
  status: number
  /** 客户端 ID */
  clientId: string | number
  /** 绑定角色 ID 列表 */
  bindRoleIds?: (string | number)[]
  /** 解绑角色 ID 列表 */
  unbindRoleIds?: (string | number)[]
}

/** AI 会话列表行（/ai/chat/conversation/list 返回元素） */
export interface ConversationItem {
  /** AI 聊天会话 ID */
  id: string | number
  /** 标题 */
  title: string
}

/** AI 对话消息类型（/ai/chat/{id}/list 返回）：0=用户消息 1=AI 消息 */
export enum ChatMessageType {
  /** 用户消息 */
  USER = 0,
  /** AI 消息 */
  AI = 1
}

/** AI 对话流类型（/ai/chat SSE 事件）：0=完成对话 1=持续对话中 */
export enum ChatStreamType {
  /** 完成对话（流结束标记） */
  FINISH = 0,
  /** 持续对话中（增量内容） */
  OUTPUT = 1
}

/** AI 对话消息（/ai/chat/{id}/list 返回 items 元素） */
export interface ChatMessageItem {
  /** 消息 ID（删除消息接口使用；本地新增消息在操作完成后由 getAfterMessageIds 回填） */
  id?: string
  /** 消息类型（0=用户消息 1=AI 消息） */
  type: ChatMessageType
  /** 消息内容 */
  message: string
  /** 携带图片的 URL 地址列表 */
  images?: string[]
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createTime?: string
}

/** AI 对话列表结果（/ai/chat/{id}/list） */
export interface ChatListResult {
  /** 是否对话中（生成进行中） */
  isLocked?: boolean
  /** 对话内容 */
  items?: ChatMessageItem[]
}

/** AI 图片生成结果（/ai/chat/generate/image） */
export interface GenerateImageResult {
  /** 消息（生成结果描述） */
  message?: string
  /** 生成图片的 URL 列表 */
  images?: string[]
}

/** 配置列表行（/system/config/list 返回元素） */
export interface ConfigListItem {
  /** 配置 ID */
  id: string | number
  /** 名称（全局唯一键） */
  name: string
  /** 别名 */
  alias: string
  /** 类型 */
  type: string
  /** 配置值 */
  value: string
  /** 排序（数值越小越靠前） */
  sort?: number
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createTime?: string
  /** 状态（0=禁用 1=启用） */
  status: number
}

/** 配置详情（/system/config/{id}/get） */
export interface ConfigDetail {
  /** 名称 */
  name: string
  /** 别名 */
  alias: string
  /** 类型 */
  type: string
  /** 配置值 */
  value: string
  /** 排序（数值越小越靠前） */
  sort?: number
  /** 状态（0=禁用 1=启用） */
  status: number
}

/** 配置保存/更新参数 */
export interface ConfigSaveParam {
  /** 名称（全局唯一键） */
  name: string
  /** 别名 */
  alias: string
  /** 类型 */
  type: string
  /** 配置值 */
  value: string
  /** 排序（数值越小越靠前） */
  sort?: number
}

/** 配置下拉选项（/system/config/{type}/dropDownList 返回元素） */
export interface ConfigOptionItem {
  /** 配置 ID */
  id: string | number
  /** 名称 */
  name: string
  /** 别名 */
  alias?: string
  /** 配置值（作为类型编码使用） */
  value?: string
  /** 排序（数值越小越靠前） */
  sort?: number
}
