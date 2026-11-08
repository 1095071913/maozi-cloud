<script setup lang="ts">
// 侧边栏菜单项：目录渲染为可折叠子菜单，菜单渲染为可点击跳转项
import type { PermissionTreeNode } from '@/types/api'
import { PermissionType } from '@/types/api'
import { markToPath } from '@/utils/permission'

defineOptions({ name: 'SidebarItem' })

const props = defineProps<{
  node: PermissionTreeNode
}>()

/** 菜单（type=1）的路由路径，作为 el-menu-item 的 index */
function menuPath(node: PermissionTreeNode) {
  return markToPath(node.mark)
}
</script>

<template>
  <!-- 目录：折叠子菜单 -->
  <el-sub-menu
    v-if="node.type === PermissionType.DIRECTORY"
    :index="String(node.id)"
  >
    <template #title>
      <span>{{ node.name }}</span>
    </template>
    <SidebarItem
      v-for="child in node.children"
      :key="child.id"
      :node="child"
    />
  </el-sub-menu>

  <!-- 菜单：可点击跳转 -->
  <el-menu-item v-else-if="node.type === PermissionType.MENU" :index="menuPath(node)">
    <span>{{ node.name }}</span>
  </el-menu-item>
</template>
