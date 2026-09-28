import {revoke} from '@/api/auth'
import {useAppStore} from '@/store/modules/app'
import {useUserStore} from '@/store/modules/user'

/**
 * 强制重新登录：同步清理本地会话后整页跳转登录页
 * 灰度测试/临时请求切换后调用，确保登录链路同样走新链路；
 * 整页跳转可彻底清理路由与 store 状态，令牌吊销尽力而为、不阻塞跳转
 * （临时地址/灰度后端不可达时也能立即回到登录页）
 */
export async function logoutToLogin() {
  const userStore = useUserStore()
  const appStore = useAppStore()
  const token = userStore.token
  // 先同步清理本地会话（含 localStorage 令牌），避免跳转后仍被视为已登录
  userStore.resetState()
  appStore.reset()
  // 整页跳转需拼上 vite base 前缀，否则子路径部署下会跳到根路径 404
  const loginPath = `${import.meta.env.BASE_URL}login`
  if (window.location.pathname !== loginPath) {
    window.location.href = loginPath
  } else {
    // 已在登录页时补拉被重置的系统详情
    appStore.fetchSystemInfo().catch(() => {})
  }
  // 尽力吊销旧令牌，失败不阻塞
  if (token) {
    revoke(token).catch(() => {})
  }
}
