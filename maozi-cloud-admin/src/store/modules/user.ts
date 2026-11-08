import {defineStore} from 'pinia'
import {ref} from 'vue'
import {login as loginApi, revoke as revokeApi} from '@/api/auth'
import {getUserInfo} from '@/api/user'
import {clearTokens, getAccessToken, setAccessToken, setRefreshToken} from '@/utils/auth'

/** 用户 store：登录态、用户信息与权限列表 */
export const useUserStore = defineStore('user', () => {
  /** 访问令牌 */
  const token = ref<string>(getAccessToken())
  /** 用户名称 */
  const name = ref<string>('')
  /** 用户头像 */
  const icon = ref<string>('')
  /** 权限列表 */
  const permissions = ref<string[]>([])

  /** 是否已登录 */
  const isLoggedIn = () => !!token.value

  /** 账号密码登录 */
  async function login(username: string, password: string) {
    const res = await loginApi(username, password)
    const data = res.data
    token.value = data.access_token
    setAccessToken(data.access_token)
    setRefreshToken(data.refresh_token)
    return data
  }

  /** 拉取用户信息 */
  async function fetchUserInfo() {
    const res = await getUserInfo()
    const data = res.data
    name.value = data.name
    icon.value = data.icon
    permissions.value = data.permissions || []
    return data
  }

  /** 登出 */
  async function logout() {
    try {
      if (token.value) {
        await revokeApi(token.value)
      }
    } catch {
      // 即使接口失败也要清理本地状态
    } finally {
      resetState()
    }
  }

  /** 重置本地状态 */
  function resetState() {
    token.value = ''
    name.value = ''
    icon.value = ''
    permissions.value = []
    clearTokens()
  }

  /** 是否拥有某权限 */
  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  return {
    token,
    name,
    icon,
    permissions,
    isLoggedIn,
    login,
    fetchUserInfo,
    logout,
    resetState,
    hasPermission
  }
})
