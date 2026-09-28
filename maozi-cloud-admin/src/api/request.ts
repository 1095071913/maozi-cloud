import axios, {type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig} from 'axios'
import {ElMessage} from 'element-plus'
import {clearTokens, getAccessToken, getRefreshToken, setAccessToken, setRefreshToken} from '@/utils/auth'
import {getGrayVersion} from '@/utils/gray'
import {getTempRequestUrl} from '@/utils/tempRequest'
import {refreshToken} from './auth'
import type {ApiResponse, TokenResult} from '@/types/api'

// 扩展 axios 配置，承载刷新流程所需的内部标记
declare module 'axios' {
  interface AxiosRequestConfig {
    /** 标记为刷新令牌请求：失败时跳过统一错误提示，由刷新逻辑的 catch 统一处理 */
    _isRefresh?: boolean
    /** 标记请求已因 401 重试过一次，避免刷新成功后再次 401 仍无限重试 */
    _retried?: boolean
  }
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

/** 需要跳过自动注入 token 的接口（登录/登出/刷新使用 Basic Auth） */
const WHITE_LIST = ['/oauth/oauth2/token', '/oauth/oauth2/revoke']

// 请求拦截器：注入 Bearer token、灰度标识与临时请求地址
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()
    const isWhitelisted = WHITE_LIST.some((p) => config.url?.includes(p))
    if (token && !isWhitelisted) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 灰度测试开启后，所有请求携带灰度标识，由网关路由到对应灰度服务
    const grayVersion = getGrayVersion()
    if (grayVersion) {
      config.headers['X-Version'] = grayVersion
    }
    // 临时请求开启后，所有请求改走临时地址
    const tempUrl = getTempRequestUrl()
    if (tempUrl) {
      config.baseURL = tempUrl
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ============ 刷新令牌（并发安全） ============
let isRefreshing = false
/** 刷新期间被暂存的请求回调，刷新成功后按新 token 重放，失败时传 null 触发拒绝 */
let pendingQueue: Array<(token: string | null) => void> = []

/** 登录页完整路径（拼上 vite base 前缀）：原生整页跳转不带 base 会在子路径部署下 404 */
const LOGIN_PATH = `${import.meta.env.BASE_URL}login`

/** 登录失效统一处理：清理令牌并跳转登录页 */
function handleUnauthorized(message?: string) {
  clearTokens()
  ElMessage.error(message || '登录已失效，请重新登录')
  // 跳转登录页（避免在登录页自身时重复 push）
  if (window.location.pathname !== LOGIN_PATH) {
    window.location.href = LOGIN_PATH
  }
}

/**
 * 处理响应头 X-Redirect：后端要求前端跳转
 * 值映射：0=登录页
 * @returns 是否触发了跳转（触发后调用方应中止后续逻辑并 reject）
 */
function applyRedirectHeader(headers: unknown): boolean {
  if (!headers) return false
  const h = headers as Record<string, unknown>
  const raw = h['x-redirect'] ?? h['X-Redirect']
  if (raw === undefined || raw === null || raw === '') return false
  const value = String(raw).trim()
  if (value === '0') {
    clearTokens()
    if (window.location.pathname !== LOGIN_PATH) {
      window.location.href = LOGIN_PATH
    }
    return true
  }
  return false
}

// 响应拦截器：剥离外层、统一错误处理
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 后端通过响应头 X-Redirect 要求跳转时优先处理（目前仅 0=登录页）
    if (applyRedirectHeader(response.headers)) {
      return Promise.reject(new Error('redirect:login'))
    }
    // 登录接口直接返回原始结构（含 access_token 等），其余接口校验业务码
    const data = response.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code !== 200) {
        ElMessage.error(data.message || '服务网络异常')
        return Promise.reject(new Error(data.message || `业务码异常: ${data.code}`))
      }
    }
    return data as unknown as AxiosResponse
  },
  async (error) => {
    // 主动中止的请求（如图片生成点停止）：静默拒绝，不弹错误提示
    if (axios.isCancel(error) || error.code === 'ERR_CANCELED') {
      return Promise.reject(error)
    }
    // 后端通过响应头 X-Redirect 要求跳转时优先处理（即使 HTTP 错误也生效）
    if (error.response && applyRedirectHeader(error.response.headers)) {
      return Promise.reject(new Error('redirect:login'))
    }
    const status = error.response?.status
    const originalRequest = error.config as
      | (InternalAxiosRequestConfig & { _retried?: boolean })
      | undefined

    // 刷新令牌接口自身失败：直接抛出，交由刷新逻辑的 catch 统一处理
    if (originalRequest?._isRefresh) {
      return Promise.reject(error)
    }

    // access_token 过期：尝试用 refresh_token 换取新令牌并重放原请求
    if (
      status === 401 &&
      originalRequest &&
      !originalRequest._retried &&
      getRefreshToken()
    ) {
      originalRequest._retried = true

      // 首个触发刷新的请求负责真正调用刷新接口
      if (!isRefreshing) {
        isRefreshing = true
        try {
          const res = (await refreshToken(
            getRefreshToken()
          )) as ApiResponse<TokenResult>
          setAccessToken(res.data.access_token)
          setRefreshToken(res.data.refresh_token)
          // 重放排队请求
          pendingQueue.forEach((cb) => cb(res.data.access_token))
          pendingQueue = []
          // 重放原请求
          return service(originalRequest)
        } catch (e) {
          // 刷新失败：清空队列并强制登出
          pendingQueue.forEach((cb) => cb(null))
          pendingQueue = []
          handleUnauthorized()
          return Promise.reject(e)
        } finally {
          isRefreshing = false
        }
      }

      // 正在刷新中：将请求排队，等待刷新结果后重放
      return new Promise((resolve, reject) => {
        pendingQueue.push((token: string | null) => {
          if (!token) {
            reject(error)
            return
          }
          originalRequest.headers.Authorization = `Bearer ${token}`
          resolve(service(originalRequest))
        })
      })
    }

    if (status === 401) {
      // 401 优先展示响应体 message，缺省回退“登录已失效”
      handleUnauthorized(error.response?.data?.message)
    } else {
      // 有响应体取其 message，无响应体（网络异常等）提示服务网络异常
      ElMessage.error(error.response?.data?.message || '服务网络异常')
    }
    return Promise.reject(error)
  }
)

export default service
