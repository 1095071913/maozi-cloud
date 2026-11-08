import request from './request'
import type {
  ApiResponse,
  ConfigDetail,
  ConfigListItem,
  ConfigOptionItem,
  ConfigSaveParam,
  PageResult
} from '@/types/api'

/** 配置列表查询参数 */
export interface ConfigListParams {
  /** 当前页 */
  current?: number
  /** 每页数量 */
  size?: number
  /** 配置类型（必传） */
  type: string
  /** 名称（模糊查询） */
  name?: string
  /** 别名（模糊查询） */
  alias?: string
}

/** 定义配置类型的配置项名称，其 value（JSON 字符串）即全部可用配置类型 */
export const CONFIG_TYPE_NAME = 'system_config_type'

/**
 * 根据名称获取配置
 * GET /system/config/{name}/dropDown
 *
 * 配置名称为全局唯一键；system_config_type 的 value 为 JSON 字符串，key 是类型编码，value 是别名
 */
export function getConfigByName(name: string) {
  return request.get(`/system/config/${name}/dropDown`) as unknown as Promise<
    ApiResponse<ConfigOptionItem>
  >
}

/**
 * 配置下拉列表（按类型）
 * GET /system/config/{type}/dropDownList
 *
 * 返回指定类型下启用状态的配置选项，每项包含 id、name、alias 与 value
 */
export function getConfigDropDownList(type: string) {
  return request.get(`/system/config/${type}/dropDownList`) as unknown as Promise<
    ApiResponse<ConfigOptionItem[]>
  >
}

/**
 * 配置分页列表
 * POST /system/config/list
 */
export function getConfigList(params: ConfigListParams) {
  return request.post('/system/config/list', {
    current: params.current ?? 1,
    size: params.size ?? 10,
    data: {
      type: params.type,
      name: params.name || undefined,
      alias: params.alias || undefined
    }
  }) as unknown as Promise<ApiResponse<PageResult<ConfigListItem>>>
}

/**
 * 配置详情
 * GET /system/config/{id}/get
 */
export function getConfigDetail(id: string | number) {
  return request.get(`/system/config/${id}/get`) as unknown as Promise<
    ApiResponse<ConfigDetail>
  >
}

/**
 * 新增配置
 * POST /system/config/save
 */
export function saveConfig(data: ConfigSaveParam) {
  return request.post('/system/config/save', data) as unknown as Promise<
    ApiResponse<string>
  >
}

/**
 * 更新配置
 * POST /system/config/{id}/update
 */
export function updateConfig(id: string | number, data: ConfigSaveParam) {
  return request.post(`/system/config/${id}/update`, data) as unknown as Promise<
    ApiResponse<string>
  >
}

/**
 * 更新配置状态（0=禁用 1=启用）
 * POST /system/config/{id}/updateStatus
 */
export function updateConfigStatus(id: string | number, status: number) {
  return request.post(`/system/config/${id}/updateStatus`, {
    data: status
  }) as unknown as Promise<ApiResponse<string>>
}

/**
 * 删除配置
 * POST /system/config/{id}/remove
 */
export function removeConfig(id: string | number) {
  return request.post(`/system/config/${id}/remove`) as unknown as Promise<
    ApiResponse<string>
  >
}
