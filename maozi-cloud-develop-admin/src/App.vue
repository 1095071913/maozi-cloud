<template>
  <el-container class="layout">
    <el-aside width="224px" class="aside">
      <div class="brand">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" width="42" height="42">
            <defs>
              <linearGradient id="logoBg" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#3b82f6" />
                <stop offset="1" stop-color="#1e3fae" />
              </linearGradient>
            </defs>
            <rect width="48" height="48" rx="13" fill="url(#logoBg)" />
            <polygon points="13.9,10 11.1,20.9 20.5,17.5" fill="#ffffff" />
            <polygon points="34.1,10 36.9,20.9 27.5,17.5" fill="#ffffff" />
            <ellipse cx="24" cy="27.2" rx="12.3" ry="10.9" fill="#ffffff" />
            <ellipse cx="19.8" cy="25.7" rx="1.6" ry="2.1" fill="#1e3fae" />
            <ellipse cx="28.2" cy="25.7" rx="1.6" ry="2.1" fill="#1e3fae" />
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-name">个人开发管理</div>
        </div>
      </div>

      <div class="menu-label" title="菜单可拖拽调整顺序">功能导航</div>
      <div
        v-for="item in menus"
        :key="item.key"
        class="nav-item"
        :class="{
          active: active === item.key,
          dragging: dragMenuKey === item.key,
          'drop-above': dragOverMenuKey === item.key && dropMenuPos === 'above' && dragMenuKey !== item.key,
          'drop-below': dragOverMenuKey === item.key && dropMenuPos === 'below' && dragMenuKey !== item.key
        }"
        draggable="true"
        @click="active = item.key"
        @dragstart="onMenuDragStart(item.key, $event)"
        @dragover.prevent="onMenuDragOver(item, $event)"
        @dragleave="dragOverMenuKey = ''"
        @drop="onMenuDrop(item, $event)"
        @dragend="onMenuDragEnd"
      >
        <span class="nav-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <component :is="item.icon" />
          </svg>
        </span>
        <span class="nav-text">{{ item.label }}</span>
      </div>

      <div class="aside-footer">
        <div class="status-line">
          <span class="status-dot"></span>
          运行正常
        </div>
        <div class="meta-line">v1.0.0 · {{ platformLabel }}</div>
      </div>
    </el-aside>

    <el-main class="main">
      <Transition name="page" mode="out-in" appear>
        <!-- KeepAlive 仅缓存项目控制台：切换导航后执行日志会话与状态保留，其余页面保持即切即销毁 -->
        <KeepAlive :include="['ProjectConsole']">
          <EnvManager v-if="active === 'env'" />
          <HostsManager v-else-if="active === 'hosts'" />
          <SystemInfo v-else-if="active === 'sys'" />
          <CodeTools v-else-if="active === 'tools'" />
          <ConfigManager v-else-if="active === 'configs'" />
          <ProjectConsole v-else-if="active === 'projects'" />
          <BookmarkManager v-else-if="active === 'bookmarks'" />
        </KeepAlive>
      </Transition>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, defineComponent, h, type Component } from 'vue'
import { api } from './api'
import EnvManager from './components/EnvManager.vue'
import HostsManager from './components/HostsManager.vue'
import SystemInfo from './components/SystemInfo.vue'
import CodeTools from './components/CodeTools.vue'
import ConfigManager from './components/ConfigManager.vue'
import ProjectConsole from './components/ProjectConsole.vue'
import BookmarkManager from './components/BookmarkManager.vue'

/** 侧边栏线性图标（stroke 风格，随文字颜色着色） */
const IconGauge = defineComponent(() => () =>
  h('g', [h('path', { d: 'M3 12h4l3-7 4 14 3-7h4' })])
)
const IconTerminal = defineComponent(() => () =>
  h('g', [h('path', { d: 'm5 7 4 4-4 4' }), h('path', { d: 'M12 17h7' })])
)
const IconGlobe = defineComponent(() => () =>
  h('g', [
    h('circle', { cx: '12', cy: '12', r: '9' }),
    h('path', { d: 'M3 12h18' }),
    h('path', { d: 'M12 3a14.5 14.5 0 0 1 0 18 14.5 14.5 0 0 1 0-18' })
  ])
)
const IconBookmark = defineComponent(() => () =>
  h('g', [h('path', { d: 'M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z' })])
)

const IconCode = defineComponent(() => () =>
  h('g', [h('path', { d: 'm8 6-6 6 6 6' }), h('path', { d: 'm16 6 6 6-6 6' })])
)

const IconRocket = defineComponent(() => () =>
  h('g', [
    h('path', { d: 'M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91 0z' }),
    h('path', { d: 'm12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z' }),
    h('path', { d: 'M9 12H4s.55-3.03 2-4c1.62-1.08 5 0 5 0' }),
    h('path', { d: 'M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5' })
  ])
)

const IconTool = defineComponent(() => () =>
  h('g', [
    h('path', {
      d: 'M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z'
    })
  ])
)

type MenuKey = 'env' | 'hosts' | 'sys' | 'bookmarks' | 'tools' | 'configs' | 'projects'
interface MenuItem {
  key: MenuKey
  label: string
  icon: Component
}

const DEFAULT_MENUS: MenuItem[] = [
  { key: 'sys', label: '系统信息', icon: IconGauge },
  { key: 'env', label: '环境变量', icon: IconTerminal },
  { key: 'hosts', label: 'Hosts 管理', icon: IconGlobe },
  { key: 'tools', label: '编程工具', icon: IconCode },
  { key: 'configs', label: '密钥管理', icon: IconTool },
  { key: 'projects', label: '项目控制台', icon: IconRocket },
  { key: 'bookmarks', label: '书签管理', icon: IconBookmark }
]

/** 菜单顺序持久化：按 localStorage 保存的 key 顺序恢复，未知项忽略、新增项补到末尾 */
const MENU_ORDER_KEY = 'maozi-cloud-develop-admin:nav-order'

function restoreMenuOrder(base: MenuItem[]): MenuItem[] {
  try {
    const saved = JSON.parse(localStorage.getItem(MENU_ORDER_KEY) ?? '[]') as string[]
    if (!Array.isArray(saved) || saved.length === 0) return base
    const byKey = new Map(base.map((m) => [m.key, m]))
    const ordered = saved.map((k) => byKey.get(k)).filter((m): m is MenuItem => !!m)
    if (ordered.length === 0) return base
    for (const m of base) {
      if (!ordered.includes(m)) ordered.push(m)
    }
    return ordered
  } catch {
    return base
  }
}

const menus = ref<MenuItem[]>(restoreMenuOrder(DEFAULT_MENUS))

/** ===== 菜单拖拽排序 ===== */
const dragMenuKey = ref('')
const dragOverMenuKey = ref('')
const dropMenuPos = ref<'above' | 'below'>('above')

function onMenuDragStart(key: MenuKey, e: DragEvent): void {
  dragMenuKey.value = key
  if (e.dataTransfer) {
    e.dataTransfer.setData('text/plain', key)
    e.dataTransfer.effectAllowed = 'move'
  }
}

function onMenuDragOver(target: MenuItem, e: DragEvent): void {
  dragOverMenuKey.value = target.key
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  dropMenuPos.value = e.clientY < rect.top + rect.height / 2 ? 'above' : 'below'
}

function onMenuDragEnd(): void {
  dragMenuKey.value = ''
  dragOverMenuKey.value = ''
  dropMenuPos.value = 'above'
}

function onMenuDrop(target: MenuItem, e: DragEvent): void {
  e.preventDefault()
  const sourceKey = dragMenuKey.value
  const above = dropMenuPos.value === 'above'
  onMenuDragEnd()
  if (!sourceKey || sourceKey === target.key) return
  const list = [...menus.value]
  const from = list.findIndex((m) => m.key === sourceKey)
  if (from < 0) return
  const [moved] = list.splice(from, 1)
  const to = list.findIndex((m) => m.key === target.key)
  if (to < 0) return
  list.splice(above ? to : to + 1, 0, moved)
  menus.value = list
  localStorage.setItem(MENU_ORDER_KEY, JSON.stringify(list.map((m) => m.key)))
}

const active = ref<MenuKey>('sys')
const platform = ref('')

const platformLabel = computed(() => {
  if (platform.value === 'darwin') return 'macOS'
  if (platform.value === 'win32') return 'Windows'
  return platform.value || '…'
})

onMounted(async () => {
  const info = await api.app.info()
  platform.value = info.platform
})
</script>

<style scoped>
.layout {
  height: 100%;
}

/* 无缝标题栏下,主内容区顶部留出呼吸位 */
.main {
  padding-top: 44px;
}

.aside {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #1a2940 0%, #16222f 100%);
  color: #cfd8e3;
  /* 无缝标题栏：顶部给红绿灯留位；侧栏空白区域可拖拽移动窗口 */
  padding-top: 44px;
  -webkit-app-region: drag;
}

.brand {
  padding: 6px 20px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  display: flex;
  align-items: center;
  gap: 13px;
}

.brand-logo {
  flex-shrink: 0;
  filter: drop-shadow(0 4px 10px rgba(37, 99, 235, 0.35));
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  line-height: 1.3;
}

.menu-label {
  padding: 18px 22px 8px;
  font-size: 11px;
  color: #5d7089;
  letter-spacing: 2px;
}

.nav-item {
  -webkit-app-region: no-drag;
  display: flex;
  align-items: center;
  gap: 11px;
  margin: 3px 12px;
  padding: 10px 14px;
  border-radius: 10px;
  color: #aebccd;
  cursor: pointer;
  position: relative;
  transition: background 0.2s ease, color 0.2s ease;
  user-select: none;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.9), rgba(59, 130, 246, 0.55));
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

/* 拖拽排序反馈 */
.nav-item.dragging {
  opacity: 0.4;
}

.nav-item.drop-above {
  box-shadow: inset 0 2px 0 0 #60a5fa;
}

.nav-item.drop-below {
  box-shadow: inset 0 -2px 0 0 #60a5fa;
}

.nav-icon {
  display: flex;
  align-items: center;
}

.nav-text {
  font-size: 13.5px;
  font-weight: 500;
}

.aside-footer {
  -webkit-app-region: no-drag;
  margin: auto 14px 16px;
  padding: 11px 13px;
  font-size: 11px;
  color: #6d7f96;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: background 0.2s ease, border-color 0.2s ease;
}

.aside-footer:hover {
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(255, 255, 255, 0.12);
}

.status-line {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #9fb4cc;
  font-weight: 600;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #34d399;
  animation: pulse 2s infinite;
}

.meta-line {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed rgba(255, 255, 255, 0.09);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.2px;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.5);
  }
  70% {
    box-shadow: 0 0 0 7px rgba(52, 211, 153, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(52, 211, 153, 0);
  }
}
</style>
