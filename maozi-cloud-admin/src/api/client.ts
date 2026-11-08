import request from './request'
import type {ApiResponse, ClientDetail, ClientListItem, ClientSaveParam, PageResult} from '@/types/api'

/** 客户端列表查询参数 */
export interface ClientListParams {
  /** 当前页 */
  current?: number
  /** 每页数量 */
  size?: number
  /** 客户端名称（模糊查询） */
  name?: string
}

/**
 * 客户端分页列表
 * POST /oauth/client/list
 */
export function getClientList(params: ClientListParams) {
  return request.post('/oauth/client/list', {
    current: params.current ?? 1,
    size: params.size ?? 10,
    data: { name: params.name || undefined }
  }) as unknown as Promise<ApiResponse<PageResult<ClientListItem>>>
}

/**
 * 客户端详情
 * GET /oauth/client/{id}/get
 */
export function getClientDetail(id: string | number) {
  return request.get(`/oauth/client/${id}/get`) as unknown as Promise<
    ApiResponse<ClientDetail>
  >
}

/**
 * 新增客户端
 * POST /oauth/client/save
 */
export function saveClient(data: ClientSaveParam) {
  return request.post('/oauth/client/save', data) as unknown as Promise<
    ApiResponse<string>
  >
}

/**
 * 更新客户端
 * POST /oauth/client/{id}/update
 */
export function updateClient(id: string | number, data: ClientSaveParam) {
  return request.post(`/oauth/client/${id}/update`, data) as unknown as Promise<
    ApiResponse<string>
  >
}

/**
 * 更新客户端状态（0=禁用 1=启用）
 * POST /oauth/client/{id}/updateStatus
 */
export function updateClientStatus(id: string | number, status: number) {
  return request.post(`/oauth/client/${id}/updateStatus`, {
    data: status
  }) as unknown as Promise<ApiResponse<string>>
}

/**
 * 删除客户端
 * POST /oauth/client/{id}/delete
 */
export function removeClient(id: string | number) {
  return request.post(`/oauth/client/${id}/delete`) as unknown as Promise<
    ApiResponse<string>
  >
}
