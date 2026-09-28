import {createRouter, createWebHistory, type RouteRecordRaw} from 'vue-router'
import {useUserStore} from '@/store/modules/user'
import {usePermissionStore} from '@/store/modules/permission'
import {useAppStore} from '@/store/modules/app'
import {markToPath} from '@/utils/permission'
import {viewModules} from './modules'
import {type PermissionTreeNode, PermissionType} from '@/types/api'

/** 布局路由名称，动态菜单作为其 children 注册 */
const LAYOUT_ROUTE_NAME = 'Layout'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: LAYOUT_ROUTE_NAME,
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' }
      },
      {
        // 个人中心为登录用户自有页面，走静态路由，不依赖后端菜单权限
        path: 'individual',
        name: 'Individual',
        component: () => import('@/views/system/user/individual.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  // 显式传入 vite base: createWebHistory() 无参时只读 <base> 标签并回退 '/',
  // 子路径部署 (/admin) 下若不传会导致路由解析与资源 base 不一致, 登录后落 404 兜底
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

/** 递归注册动态菜单路由（仅 type=1 菜单生成可访问路由） */
function registerMenuRoutes(tree: PermissionTreeNode[]) {
  const walk = (nodes: PermissionTreeNode[]) => {
    nodes.forEach((node) => {
      if (node.type === PermissionType.MENU) {
        const path = markToPath(node.mark)
        router.addRoute(LAYOUT_ROUTE_NAME, {
          path,
          name: node.mark,
          component:
            viewModules[node.mark] ||
            (() => import('@/views/placeholder/index.vue')),
          meta: { title: node.name, icon: node.icon, mark: node.mark }
        })
      }
      if (node.children && node.children.length) {
        walk(node.children)
      }
    })
  }
  walk(tree)

  // 兜底 404：必须在所有动态路由注册之后追加，保证兜底匹配在最后
  router.addRoute({
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  })
}

// 全局前置守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const hasToken = userStore.isLoggedIn()

  if (!hasToken) {
    // 未登录：仅允许访问登录页，其余一律跳登录
    if (to.path === '/login') {
      // 登录前拉取系统详情（匿名接口，登录页展示项目信息用），失败不阻塞登录页展示
      if (!useAppStore().systemInfo) {
        useAppStore()
          .fetchSystemInfo()
          .catch(() => {})
      }
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return
  }

  // 已登录访问登录页 → 跳首页
  if (to.path === '/login') {
    next({ path: '/' })
    return
  }

  // 首次进入：拉取用户信息 + 生成菜单 + 注册动态路由
  if (!permissionStore.loaded) {
    try {
      if (!userStore.name) {
        await userStore.fetchUserInfo()
      }
      const menuTree = await permissionStore.generateMenus()
      registerMenuRoutes(menuTree)
      // 系统详情（项目信息）非关键，失败不阻塞登录；登录前未拉取到时补拉
      if (!useAppStore().systemInfo) {
        useAppStore()
          .fetchSystemInfo()
          .catch(() => {})
      }
      // 路由表已变更，重新匹配一次
      next({ ...to, replace: true })
    } catch {
      await userStore.logout()
      permissionStore.reset()
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return
  }

  next()
})

/** 登出时调用：清除动态路由与权限缓存 */
export function resetDynamicRoutes() {
  const permissionStore = usePermissionStore()
  const all = router.getRoutes()
  all.forEach((r) => {
    if (
      r.name &&
      r.name !== 'Login' &&
      r.name !== LAYOUT_ROUTE_NAME &&
      r.name !== 'Dashboard' &&
      r.name !== 'Individual'
    ) {
      router.removeRoute(r.name)
    }
  })
  permissionStore.reset()
}

export default router
