<script setup lang="ts">
// 主布局：左侧菜单 + 顶部标题/环境标识/灰度与临时请求入口 + 内容区 + 页脚
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { useAppStore } from '@/store/modules/app'
import { resetDynamicRoutes } from '@/router'
import { environmentInfo } from '@/utils/system'
import SidebarItem from './components/SidebarItem.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)
const menus = computed(() => permissionStore.menuTree)
const logoText = computed(() => appStore.systemInfo?.projectName || 'maozi-cloud')

/** 环境标识（全局显眼展示）：按环境名映射标签与颜色 */
const envInfo = computed(() =>
  environmentInfo(appStore.systemInfo?.environment || '')
)

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .catch(() => null)
    .then(async (action) => {
      if (action === 'confirm') {
        await userStore.logout()
        resetDynamicRoutes()
        appStore.reset()
        router.push('/login')
      }
    })
}
</script>

<template>
  <div class="layout-container">
    <aside class="layout-sider">
      <div class="layout-logo">{{ logoText }}</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#ffffff"
        text-color="#303133"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <span>首页</span>
        </el-menu-item>
        <SidebarItem
          v-for="node in menus"
          :key="node.id"
          :node="node"
        />
      </el-menu>
    </aside>

    <div class="layout-main">
      <header class="layout-header">
        <div class="header-left">
          <el-tag effect="dark" size="large" type="info" class="header-title">
            {{ (route.meta.title as string) || '' }}
          </el-tag>
          <el-tag
            v-if="envInfo"
            :type="envInfo.type"
            effect="dark"
            size="large"
            class="env-tag"
          >
            {{ envInfo.label }}
          </el-tag>
          <GrayRelease />
          <TempRequest />
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.icon" />
              <span class="user-name">{{ userStore.name }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/individual')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="layout-content">
        <router-view />
      </main>

      <footer
        v-if="appStore.systemInfo?.corporationName || appStore.systemInfo?.copyright"
        class="layout-footer"
      >
        <div v-if="appStore.systemInfo?.corporationName">
          {{ appStore.systemInfo.corporationName }}
        </div>
        <div v-if="appStore.systemInfo?.copyright">
          {{ appStore.systemInfo.copyright }}
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout-container {
  display: flex;
  width: 100%;
  height: 100vh;
}

.layout-sider {
  display: flex;
  flex-direction: column;
  width: 220px;
  background-color: #ffffff;
  border-right: 1px solid #e8e8e8;
}

.layout-logo {
  height: 60px;
  line-height: 60px;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  border-bottom: 1px solid #e8e8e8;
}

.layout-sider :deep(.el-menu) {
  border-right: none;
}

.layout-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.env-tag {
  // 与 large 按钮同高（el-tag large 默认 32px）
  height: 40px;
  justify-content: center;
  min-width: 132px;
  font-weight: 600;
  letter-spacing: 1px;
}

.header-title {
  // 与其余标识同风格同尺寸
  height: 40px;
  justify-content: center;
  min-width: 132px;
  font-weight: 600;
  letter-spacing: 1px;
}

.user-info {
  display: flex;
  gap: 8px;
  align-items: center;
  cursor: pointer;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.layout-content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.layout-footer {
  flex-shrink: 0;
  padding: 10px 20px;
  text-align: center;
  font-size: 12px;
  color: #909399;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  line-height: 20px;
}
</style>
