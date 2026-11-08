import {defineStore} from 'pinia'
import {ref} from 'vue'
import {getPermissionList} from '@/api/permission'
import {useUserStore} from '@/store/modules/user'
import {buildPermissionTree} from '@/utils/permission'
import {type PermissionItem, type PermissionTreeNode, PermissionType} from '@/types/api'

/** 权限 store：全量权限列表与按用户权限过滤后的目录/菜单树 */
export const usePermissionStore = defineStore('permission', () => {
  /** 目录/菜单树（已按用户权限过滤） */
  const menuTree = ref<PermissionTreeNode[]>([])
  /** 全量权限列表 */
  const permissionList = ref<PermissionItem[]>([])
  /** 是否已加载 */
  const loaded = ref(false)

  /**
   * 拉取权限列表，并按用户自身 permissions 过滤、构建目录/菜单树。
   * - type=0 目录、type=1 菜单：进入侧边栏树
   * - type=2 按钮：不进树，仅靠 mark 与用户权限做按钮级显隐
   */
  async function generateMenus() {
    const res = await getPermissionList()
    const list = res.data || []
    permissionList.value = list

    const userStore = useUserStore()
    const owned = new Set(userStore.permissions)

    // 仅保留用户拥有 mark 的节点（目录/菜单用于导航，按钮也参与过滤但单独使用）
    const keep = (item: PermissionItem) =>
      item.type === PermissionType.DIRECTORY ||
      item.type === PermissionType.MENU
        ? owned.has(item.mark)
        : false

    menuTree.value = buildPermissionTree(list, keep)
    loaded.value = true
    return menuTree.value
  }

  /** 重置 */
  function reset() {
    menuTree.value = []
    permissionList.value = []
    loaded.value = false
  }

  /**
   * 判断是否拥有某个按钮/操作权限。
   * 既兼容按钮 mark，也兼容目录/菜单 mark。
   */
  function hasPermission(mark: string): boolean {
    const userStore = useUserStore()
    return userStore.permissions.includes(mark)
  }

  return {
    menuTree,
    permissionList,
    loaded,
    generateMenus,
    reset,
    hasPermission
  }
})
