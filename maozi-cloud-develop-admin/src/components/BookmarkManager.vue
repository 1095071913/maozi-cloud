<template>
  <div class="bm-page">
    <!-- ===== Hero 头部 ===== -->
    <div class="bm-hero">
      <div class="bm-deco bm-deco-1"></div>
      <div class="bm-deco bm-deco-2"></div>
      <div class="bm-hero-left">
        <div class="bm-hero-name">🔖 书签管理</div>
        <div class="bm-hero-sub">
          {{ stats.total }} 个书签 · {{ stats.companies }} 个归宿 · {{ stats.envs }} 个环境
          <span class="bm-hero-tip">卡片可拖拽排序 · 密码加密存储</span>
        </div>
      </div>
      <div class="bm-hero-tools">
        <div class="bm-hero-search">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input v-model="keyword" placeholder="搜索标题 / 地址 / 账号" />
        </div>
        <button class="bm-hero-add" @click="openAdd">＋ 添加书签</button>
      </div>
    </div>

    <div class="bm-layout">
      <!-- ===== 左侧三层分类树 ===== -->
      <div class="bm-aside">
        <div class="bm-aside-title">分类导航</div>
        <div class="cat-node cat-all" :class="{ active: !hasFilter }" @click="clearFilter">
          <span class="cat-label">全部书签</span>
          <span class="cat-count">{{ bookmarks.length }}</span>
        </div>
        <template v-for="g in categoryTree" :key="g.label">
          <div
            class="cat-node cat-l1"
            :class="{ active: filter.c1 === g.label && !filter.c2 }"
            @click="onLevel1Click(g.label)"
          >
            <span class="cat-arrow" @click.stop="toggleCollapse(g.label)">{{
              collapsed.has(g.label) ? '▸' : '▾'
            }}</span>
            <span class="l1-dot" :style="{ background: colorOf(g.label) }"></span>
            <span class="cat-label">{{ g.label }}</span>
            <span class="cat-count">{{ g.count }}</span>
          </div>
          <div v-show="!collapsed.has(g.label)" class="cat-sub">
            <!-- 二级：环境（点击筛选；三级类型在右侧详情分组展示） -->
            <div
              v-for="env in g.children"
              :key="env.label"
              class="cat-node cat-l2"
              :class="{ active: filter.c1 === g.label && filter.c2 === env.label }"
              @click="selectNode({ c1: g.label, c2: env.label })"
            >
              <span class="l2-dot"></span>
              <span class="cat-label">{{ env.label }}</span>
              <span class="cat-count">{{ env.count }}</span>
            </div>
          </div>
        </template>
        <el-empty
          v-if="bookmarks.length === 0"
          description="暂无书签"
          :image-size="50"
          style="margin-top: 20px"
        />
      </div>

      <!-- ===== 右侧书签区 ===== -->
      <div class="bm-main">
        <div v-if="hasFilter || keyword" class="bm-filter-bar">
          <span class="filter-text">
            {{ [filter.c1, filter.c2].filter(Boolean).join(' / ') || '全部' }}
            <template v-if="keyword">· 关键词「{{ keyword }}」</template>
          </span>
          <span class="filter-clear" @click="clearAll">清除筛选 ✕</span>
        </div>

        <!-- 按三级类型分组展示（整组为放置区：拖到组内任意空白处即可移入该组） -->
        <div
          class="type-group"
          v-for="g in groupedByType"
          :key="g.label"
          :class="{ 'group-area-droppable': dragOverArea === g.label }"
          @dragover.prevent="dragOverArea = g.label"
          @drop="onDropIntoGroup(g.label, $event)"
        >
          <div
            class="type-group-header"
            :class="{ 'group-droppable': dragOverGroup === g.label }"
            @dragover.prevent="dragOverGroup = g.label"
            @dragleave="dragOverGroup = ''"
          >
            <span class="tg-bar"></span>
            <span class="tg-title">{{ g.label }}</span>
            <span class="tg-count">{{ g.items.length }} 条</span>
            <span class="tg-drop-hint">松开移入此组</span>
          </div>
          <div class="bm-grid">
            <div
              class="bm-card"
              v-for="b in g.items"
              :key="b.id"
              draggable="true"
              :class="{
                dragging: dragId === b.id,
                'drop-before': dragOverId === b.id && dragId !== b.id && dropPos === 'before',
                'drop-after': dragOverId === b.id && dragId !== b.id && dropPos === 'after'
              }"
              @dragstart="onDragStart(b, $event)"
              @dragover="onDragOver(b, $event)"
              @dragleave="dragOverId = ''"
              @drop="onDrop(b, $event)"
              @dragend="onDragEnd"
            >
              <div class="bm-card-head" @click="openUrl(b)">
                <div class="bm-icon">
                  <img v-if="b.icon" :src="b.icon" alt="" />
                  <span v-else class="bm-icon-fallback" :style="{ background: colorOf(b.title) }">{{
                    (b.title || '?').charAt(0).toUpperCase()
                  }}</span>
                </div>
                <div class="bm-title-block">
                  <div class="bm-title">{{ b.title }}</div>
                  <div class="bm-url mono-text">{{ prettyUrl(b.url) }}</div>
                </div>
                <span class="bm-open-arrow">↗</span>
              </div>
              <div class="bm-cats">
                <el-tag size="small" effect="plain" round>{{ b.category1 }}</el-tag>
                <el-tag size="small" type="warning" effect="plain" round>{{ b.category2 }}</el-tag>
                <el-tag size="small" type="success" effect="plain" round>{{ b.category3 }}</el-tag>
              </div>
              <div class="bm-desc" v-if="b.description">{{ b.description }}</div>
              <div class="bm-creds" v-if="b.username || b.password">
                <div class="cred-item" v-if="b.username" @click="copyText(b.username)" title="点击复制账号">
                  <span class="cred-label">👤</span>
                  <span class="cred-value mono-text">{{ b.username }}</span>
                </div>
                <div class="cred-item" v-if="b.password" @click="copyText(b.password)" title="点击复制密码">
                  <span class="cred-label">🔑</span>
                  <span class="cred-value mono-text">••••••••</span>
                  <span class="cred-copy">复制</span>
                </div>
              </div>
              <div class="bm-actions">
                <el-button size="small" type="primary" plain @click.stop="openUrl(b)">打开</el-button>
                <el-button size="small" @click="openEdit(b)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="onRemove(b)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty
          v-if="filtered.length === 0 && bookmarks.length > 0"
          description="没有符合条件的书签"
        />
      </div>
    </div>

    <!-- ===== 添加 / 编辑（居中美化弹窗，挂到 body 避免 el-main 滚动条压在弹窗上） ===== -->
    <el-dialog
      v-model="dialogVisible"
      width="720px"
      append-to-body
      modal-class="bm-dlg"
      :show-close="false"
      @closed="resetDialog"
    >
      <template #header>
        <div class="bf-header">
          <div class="bf-deco bf-deco-1"></div>
          <div class="bf-deco bf-deco-2"></div>
          <div class="bf-header-main">
            <div class="bf-header-icon">{{ isEdit ? '✏️' : '🔖' }}</div>
            <div>
              <div class="bf-title">{{ isEdit ? '编辑书签' : '添加书签' }}</div>
              <div class="bf-subtitle">输入地址后自动读取标题、描述与图标 · 密码加密存储</div>
            </div>
          </div>
          <button class="bf-close" type="button" @click="dialogVisible = false">✕</button>
        </div>
      </template>

      <div class="bf-body">
        <!-- 地址(主字段) -->
        <div class="bf-field">
          <div class="bf-label">地址 <span class="bf-req">*</span></div>
          <div class="bf-url-row">
            <el-input
              v-model="form.url"
              size="large"
              placeholder="如 https://nacos.maozi.cloud（输入后自动读取网页信息）"
              class="mono-cell"
            />
            <span v-if="metaLoading" class="bf-loading" title="正在读取网页信息">⟳</span>
            <div class="bf-icon" title="网页图标（自动获取）">
              <img v-if="form.icon" :src="form.icon" alt="" />
              <span v-else class="bf-icon-fallback" :style="{ background: colorOf(form.title) }">{{
                (form.title || '?').charAt(0).toUpperCase()
              }}</span>
            </div>
          </div>
        </div>

        <div class="bf-cols">
          <!-- 左:网页信息 -->
          <div class="bf-card">
            <div class="bf-sec">
              <span class="bf-sec-ico">🌐</span>
              <span class="bf-sec-text">网页信息</span>
              <span class="bf-sec-line"></span>
            </div>
            <div class="bf-field">
              <div class="bf-label">标题 <span class="bf-req">*</span></div>
              <el-input v-model="form.title" placeholder="网页标题（自动读取，可修改）" />
              <div
                v-if="titleHint"
                class="bf-hint"
                @click="applyHint('title')"
                title="点击使用自动读取的标题"
              >
                🔗 已读取到「{{ titleHint }}」· 点击覆盖
              </div>
            </div>
            <div class="bf-field">
              <div class="bf-label">描述</div>
              <el-input
                v-model="form.description"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 5 }"
                placeholder="网页描述（自动读取，可修改）"
              />
              <div
                v-if="descHint"
                class="bf-hint"
                @click="applyHint('description')"
                title="点击使用自动读取的描述"
              >
                🔗 已读取到描述 · 点击覆盖
              </div>
            </div>
          </div>

          <!-- 右:登录凭证 -->
          <div class="bf-card bf-card-cred">
            <div class="bf-sec">
              <span class="bf-sec-ico">🔐</span>
              <span class="bf-sec-text">登录凭证</span>
              <span class="bf-sec-line"></span>
            </div>
            <div class="bf-field">
              <div class="bf-label">账号</div>
              <el-input v-model="form.username" placeholder="登录账号（可留空）" class="mono-cell" />
            </div>
            <div class="bf-field">
              <div class="bf-label">密码</div>
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="登录密码（可留空）"
                class="mono-cell"
              />
            </div>
            <div class="bf-cred-tip">🔒 凭证仅保存在本机，密码加密存储</div>
          </div>
        </div>

        <!-- 分类 -->
        <div class="bf-card bf-card-cats">
          <div class="bf-sec">
            <span class="bf-sec-ico">🗂️</span>
            <span class="bf-sec-text">归属分类</span>
            <span class="bf-sec-line"></span>
          </div>
          <div class="bf-cats">
            <div class="bf-field">
              <div class="bf-label">归宿</div>
              <el-select v-model="form.category1" filterable allow-create default-first-option placeholder="选择或输入">
                <el-option v-for="o in catOptions.c1" :key="o" :label="o" :value="o" />
              </el-select>
            </div>
            <div class="bf-field">
              <div class="bf-label">环境</div>
              <el-select v-model="form.category2" filterable allow-create default-first-option placeholder="选择或输入">
                <el-option v-for="o in catOptions.c2" :key="o" :label="o" :value="o" />
              </el-select>
            </div>
            <div class="bf-field">
              <div class="bf-label">类型</div>
              <el-select v-model="form.category3" filterable allow-create default-first-option placeholder="选择或输入">
                <el-option v-for="o in catOptions.c3" :key="o" :label="o" :value="o" />
              </el-select>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="bf-footer">
          <button class="bf-btn ghost" type="button" @click="dialogVisible = false">取消</button>
          <button class="bf-btn primary" type="button" :disabled="saving" @click="onSave">
            {{ saving ? '保存中…' : isEdit ? '保存修改' : '添加书签' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, type Bookmark } from '../api'

const bookmarks = ref<Bookmark[]>([])
const keyword = ref('')
const filter = reactive<{ c1?: string; c2?: string; c3?: string }>({})
const collapsed = ref(new Set<string>())

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const metaLoading = ref(false)

const emptyForm = () => ({
  id: '',
  url: '',
  title: '',
  description: '',
  icon: '',
  username: '',
  password: '',
  category1: '',
  category2: '',
  category3: ''
})
const form = ref(emptyForm())

/** 自动读取到、但用户已手动填写时暂存的覆盖提示 */
const titleHint = ref('')
const descHint = ref('')

let urlTimer: ReturnType<typeof setTimeout> | null = null
let skipUrlWatch = false

interface CatNode {
  label: string
  count: number
  children: CatNode[]
}

const categoryTree = computed<CatNode[]>(() => {
  const root = new Map<string, Map<string, Map<string, number>>>()
  for (const b of bookmarks.value) {
    if (!root.has(b.category1)) root.set(b.category1, new Map())
    const envs = root.get(b.category1)!
    if (!envs.has(b.category2)) envs.set(b.category2, new Map())
    const types = envs.get(b.category2)!
    types.set(b.category3, (types.get(b.category3) ?? 0) + 1)
  }
  return [...root.entries()].map(([label, envs]) => ({
    label,
    count: [...envs.values()].reduce((s, t) => s + [...t.values()].reduce((a, b) => a + b, 0), 0),
    children: [...envs.entries()].map(([elabel, types]) => ({
      label: elabel,
      count: [...types.values()].reduce((a, b) => a + b, 0),
      children: [...types.entries()].map(([tlabel, count]) => ({ label: tlabel, count, children: [] }))
    }))
  }))
})

const stats = computed(() => ({
  total: bookmarks.value.length,
  companies: categoryTree.value.length,
  envs: categoryTree.value.reduce((s, g) => s + g.children.length, 0)
}))

const catOptions = computed(() => {
  const uniq = (key: 'category1' | 'category2' | 'category3') => [
    ...new Set(bookmarks.value.map((b) => b[key]).filter(Boolean))
  ]
  return { c1: uniq('category1'), c2: uniq('category2'), c3: uniq('category3') }
})

const hasFilter = computed(() => !!(filter.c1 || filter.c2 || filter.c3))

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return bookmarks.value.filter((b) => {
    if (filter.c1 && b.category1 !== filter.c1) return false
    if (filter.c2 && b.category2 !== filter.c2) return false
    if (filter.c3 && b.category3 !== filter.c3) return false
    if (!k) return true
    return (
      b.title.toLowerCase().includes(k) ||
      b.url.toLowerCase().includes(k) ||
      b.username.toLowerCase().includes(k) ||
      b.description.toLowerCase().includes(k)
    )
  })
})

/** 右侧详情按三级类型分组展示 */
const groupedByType = computed(() => {
  const map = new Map<string, Bookmark[]>()
  for (const b of filtered.value) {
    if (!map.has(b.category3)) map.set(b.category3, [])
    map.get(b.category3)!.push(b)
  }
  return [...map.entries()].map(([label, items]) => ({ label, items }))
})

async function load(): Promise<void> {
  const r = await api.bookmarks.list()
  if (r.ok && r.data) bookmarks.value = r.data
}

function selectNode(node: { c1: string; c2?: string; c3?: string }): void {
  filter.c1 = node.c1
  filter.c2 = node.c2
  filter.c3 = node.c3
}

function clearFilter(): void {
  filter.c1 = undefined
  filter.c2 = undefined
  filter.c3 = undefined
}

function clearAll(): void {
  clearFilter()
  keyword.value = ''
}

function toggleCollapse(label: string): void {
  const next = new Set(collapsed.value)
  if (next.has(label)) next.delete(label)
  else next.add(label)
  collapsed.value = next
}

/** 点击一级节点：每次点击都切换展开/收起，并筛选该公司 */
function onLevel1Click(label: string): void {
  toggleCollapse(label)
  selectNode({ c1: label })
}

function colorOf(seed: string): string {
  const colors = ['#2563eb', '#7c3aed', '#db2777', '#dc2626', '#ea580c', '#059669', '#0891b2']
  let hash = 0
  for (const ch of seed || '?') hash = (hash * 31 + ch.charCodeAt(0)) % 997
  return colors[hash % colors.length]
}

function prettyUrl(url: string): string {
  return url.replace(/^https?:\/\//, '').replace(/\/$/, '')
}

async function copyText(text: string): Promise<void> {
  await api.util.copy(text)
  ElMessage.success('已复制到剪贴板')
}

async function openUrl(b: Bookmark): Promise<void> {
  await api.util.openUrl(b.url)
}

function resetDialog(): void {
  form.value = emptyForm()
  titleHint.value = ''
  descHint.value = ''
  metaLoading.value = false
}

function openAdd(): void {
  isEdit.value = false
  resetDialog()
  dialogVisible.value = true
}

function openEdit(b: Bookmark): void {
  isEdit.value = true
  skipUrlWatch = true
  form.value = {
    id: b.id,
    url: b.url,
    title: b.title,
    description: b.description,
    icon: b.icon ?? '',
    username: b.username,
    password: b.password,
    category1: b.category1,
    category2: b.category2,
    category3: b.category3
  }
  titleHint.value = ''
  descHint.value = ''
  dialogVisible.value = true
}

/** 地址输入停止 800ms 后异步抓取，不阻塞用户继续填写 */
watch(
  () => form.value.url,
  (url) => {
    if (skipUrlWatch) {
      skipUrlWatch = false
      return
    }
    if (urlTimer) clearTimeout(urlTimer)
    const trimmed = url.trim()
    if (!trimmed || /\s/.test(trimmed)) return
    urlTimer = setTimeout(() => void fetchMeta(trimmed), 800)
  }
)

async function fetchMeta(url: string): Promise<void> {
  metaLoading.value = true
  const r = await api.bookmarks.fetchMeta(url)
  metaLoading.value = false
  if (!r.ok || !r.data) return
  const meta = r.data

  if (meta.icon) form.value.icon = meta.icon
  if (meta.title) {
    if (form.value.title) {
      if (form.value.title !== meta.title) titleHint.value = meta.title
    } else {
      form.value.title = meta.title
    }
  }
  if (meta.description) {
    if (form.value.description) {
      if (form.value.description !== meta.description) descHint.value = meta.description
    } else {
      form.value.description = meta.description
    }
  }
}

function applyHint(field: 'title' | 'description'): void {
  if (field === 'title') {
    form.value.title = titleHint.value
    titleHint.value = ''
  } else {
    form.value.description = descHint.value
    descHint.value = ''
  }
}

async function onSave(): Promise<void> {
  if (!form.value.url) {
    ElMessage.warning('请填写地址')
    return
  }
  if (!form.value.title) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  const payload: Partial<Bookmark> = {
    id: form.value.id || undefined,
    url: form.value.url.trim(),
    title: form.value.title.trim(),
    description: form.value.description.trim(),
    icon: form.value.icon || undefined,
    username: form.value.username,
    password: form.value.password,
    category1: form.value.category1 || '默认归宿',
    category2: form.value.category2 || '默认环境',
    category3: form.value.category3 || '默认类型'
  }
  const r = await api.bookmarks.save(payload)
  saving.value = false
  if (!r.ok) {
    ElMessage.error(r.error ?? '保存失败')
    return
  }
  ElMessage.success(isEdit.value ? '书签已更新' : '书签已添加')
  dialogVisible.value = false
  await load()
}

async function onRemove(b: Bookmark): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除书签「${b.title}」吗？`, '删除书签', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  const r = await api.bookmarks.remove(b.id)
  if (!r.ok) {
    ElMessage.error(r.error ?? '删除失败')
    return
  }
  ElMessage.success('已删除')
  await load()
}

/* ===== 卡片拖拽排序 ===== */
const dragId = ref('')
const dragOverId = ref('')
const dragOverGroup = ref('')
/** 拖拽悬停的分组区域（整组作为放置区） */
const dragOverArea = ref('')
/** 释放位置：目标卡左半 = 插入其前，右半 = 插入其后 */
const dropPos = ref<'before' | 'after'>('before')

function onDragStart(b: Bookmark, e: DragEvent): void {
  dragId.value = b.id
  if (e.dataTransfer) {
    e.dataTransfer.setData('text/plain', b.id)
    e.dataTransfer.effectAllowed = 'move'
  }
}

function onDragOver(b: Bookmark, e: DragEvent): void {
  e.preventDefault()
  dragOverId.value = b.id
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  dropPos.value = e.clientX < rect.left + rect.width / 2 ? 'before' : 'after'
}

function onDragEnd(): void {
  dragId.value = ''
  dragOverId.value = ''
  dragOverGroup.value = ''
  dragOverArea.value = ''
  dropPos.value = 'before'
}

async function onDrop(b: Bookmark, e: DragEvent): Promise<void> {
  e.preventDefault()
  // 已精确处理落点，阻止冒泡到分组的放置处理，避免重复移动
  e.stopPropagation()
  const sourceId = dragId.value
  const pos = dropPos.value
  onDragEnd()
  if (!sourceId || sourceId === b.id) return

  const source = filtered.value.find((x) => x.id === sourceId)
  if (!source) return

  const ids = filtered.value.map((x) => x.id)
  const from = ids.indexOf(sourceId)
  if (from < 0) return
  ids.splice(from, 1)
  const to = ids.indexOf(b.id)
  if (to < 0) return
  ids.splice(pos === 'before' ? to : to + 1, 0, sourceId)

  // 跨组拖拽：更换三级类型（分组）
  if (source.category3 !== b.category3) {
    const moved = await api.bookmarks.save({ id: sourceId, category3: b.category3 })
    if (!moved.ok) {
      ElMessage.error(moved.error ?? '移动分组失败')
      return
    }
    ElMessage.success(`已移动到「${b.category3}」分组`)
  }

  const r = await api.bookmarks.reorder(ids)
  if (!r.ok) {
    ElMessage.error(r.error ?? '排序失败')
    return
  }
  await load()
}

/**
 * 拖到组区域（标题或组内空白处）= 移入该分组并排到组末尾。
 * 卡片上的精确落点由 onDrop 处理并已 stopPropagation，不会重复触发
 */
async function onDropIntoGroup(label: string, e: DragEvent): Promise<void> {
  e.preventDefault()
  const sourceId = dragId.value
  onDragEnd()
  if (!sourceId) return
  const source = filtered.value.find((x) => x.id === sourceId)
  if (!source) return

  // 排到目标组末尾
  const ids = filtered.value.map((x) => x.id).filter((id) => id !== sourceId)
  const groupIds = filtered.value.filter((x) => x.category3 === label).map((x) => x.id)
  const lastId = groupIds[groupIds.length - 1]
  const insertAt = lastId ? ids.lastIndexOf(lastId) : -1
  if (insertAt >= 0) ids.splice(insertAt + 1, 0, sourceId)
  else ids.push(sourceId)

  if (source.category3 !== label) {
    const moved = await api.bookmarks.save({ id: sourceId, category3: label })
    if (!moved.ok) {
      ElMessage.error(moved.error ?? '移动分组失败')
      return
    }
    ElMessage.success(`已移动到「${label}」分组`)
  }
  const r = await api.bookmarks.reorder(ids)
  if (!r.ok) {
    ElMessage.error(r.error ?? '排序失败')
    return
  }
  await load()
}

onMounted(load)
</script>

<style scoped>
.bm-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== Hero ===== */
.bm-hero {
  -webkit-app-region: drag;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 32px;
  border-radius: 18px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
}

.bm-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.bm-deco-1 {
  width: 300px;
  height: 300px;
  right: -70px;
  top: -160px;
}

.bm-deco-2 {
  width: 190px;
  height: 190px;
  right: 150px;
  bottom: -120px;
  border-color: rgba(255, 255, 255, 0.07);
}

.bm-hero-left {
  position: relative;
  min-width: 0;
}

.bm-hero-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.bm-hero-sub {
  margin-top: 7px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.bm-hero-tip {
  margin-left: 10px;
  padding: 1px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 11px;
}

.bm-hero-tools {
  -webkit-app-region: no-drag;
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}

.bm-hero-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.8);
  transition: background 0.2s;
}

.bm-hero-search:focus-within {
  background: rgba(255, 255, 255, 0.2);
}

.bm-hero-search input {
  border: none;
  outline: none;
  background: transparent;
  color: #fff;
  font-size: 13px;
  width: 210px;
}

.bm-hero-search input::placeholder {
  color: rgba(255, 255, 255, 0.55);
}

.bm-hero-add {
  padding: 10px 18px;
  border: none;
  border-radius: 12px;
  background: #fff;
  color: #1e3fae;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.bm-hero-add:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.24);
}

/* ===== 主体 ===== */
.bm-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

/* 分类树 */
.bm-aside {
  width: 234px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 14px 10px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
  position: sticky;
  top: 0;
}

.bm-aside-title {
  font-size: 12px;
  color: #8a94a6;
  padding: 2px 10px 10px;
  letter-spacing: 1px;
}

.cat-node {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  user-select: none;
}

.cat-node:hover {
  background: #f1f5f9;
  color: #334155;
}

.cat-node.active {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.12), rgba(59, 130, 246, 0.05));
  color: #1d4ed8;
  font-weight: 600;
}

.cat-arrow {
  width: 14px;
  color: #94a3b8;
  font-size: 11px;
  text-align: center;
  flex-shrink: 0;
}

.cat-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-count {
  font-size: 11px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

/* 全部书签 */
.cat-all {
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 4px;
  border-bottom: 1px dashed #eef2f7;
  border-radius: 0;
  padding-bottom: 10px;
}

.cat-all.active {
  background: transparent;
  color: #1d4ed8;
}

/* 一级：归宿 */
.cat-l1 {
  margin-top: 6px;
  font-weight: 700;
  font-size: 13.5px;
  color: #0f1e30;
}

.l1-dot {
  width: 9px;
  height: 9px;
  border-radius: 3px;
  flex-shrink: 0;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 二级/三级引导线 */
.cat-sub {
  margin-left: 17px;
  padding-left: 12px;
  border-left: 2px solid #e4eaf2;
}

.cat-l2 .l2-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd9ea;
  border: 1.5px solid #b3c6dd;
  flex-shrink: 0;
  box-sizing: border-box;
}

.cat-l2.active .l2-dot {
  background: #2563eb;
  border-color: #2563eb;
}

/* ===== 右侧类型分组 ===== */
.type-group {
  margin-bottom: 4px;
}

/* 拖拽悬停:整组作为放置区 */
.type-group.group-area-droppable {
  border-radius: 14px;
  background: #f0f7ff;
  outline: 2px dashed #93c5fd;
  outline-offset: 3px;
}

.type-group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 18px 0 12px;
}

.type-group:first-child .type-group-header {
  margin-top: 4px;
}

.tg-bar {
  width: 4px;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(180deg, #34d399, #059669);
}

.tg-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #1f2d3d;
}

.tg-count {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 9px;
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
}

/* 拖拽悬停组头时的可放入提示 */
.type-group-header.group-droppable {
  background: #f0fdf4;
  border-radius: 8px;
  outline: 2px dashed #059669;
  outline-offset: 2px;
}

.tg-drop-hint {
  display: none;
  font-size: 11px;
  color: #059669;
}

.type-group-header.group-droppable .tg-drop-hint {
  display: inline;
}

/* ===== 书签区 ===== */
.bm-main {
  flex: 1;
  min-width: 0;
}

.bm-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 7px 14px;
  margin-bottom: 12px;
  font-size: 12.5px;
  color: #1d4ed8;
}

.filter-clear {
  cursor: pointer;
  color: #64748b;
  font-size: 12px;
}

.filter-clear:hover {
  color: #dc2626;
}

.bm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(310px, 1fr));
  gap: 14px;
}

.bm-card {
  position: relative;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.15s ease;
  display: flex;
  flex-direction: column;
  gap: 10px;
  cursor: grab;
  overflow: hidden;
}

.bm-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #3b82f6, #22d3ee);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.bm-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.1);
}

.bm-card:hover::before {
  opacity: 1;
}

.bm-card:active {
  cursor: grabbing;
}

.bm-card.dragging {
  opacity: 0.4;
}

/* 拖拽插入位置指示：左半 = 插入目标前（左侧粗线+光晕），右半 = 插入目标后（右侧） */
.bm-card.drop-before {
  background: #f5f9ff;
  box-shadow:
    -5px 0 0 0 #2563eb,
    -12px 0 20px -4px rgba(37, 99, 235, 0.55),
    0 1px 3px rgba(16, 24, 40, 0.05);
}

.bm-card.drop-after {
  background: #f5f9ff;
  box-shadow:
    5px 0 0 0 #2563eb,
    12px 0 20px -4px rgba(37, 99, 235, 0.55),
    0 1px 3px rgba(16, 24, 40, 0.05);
}

.bm-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  min-width: 0;
}

.bm-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  border: 1px solid #eef2f7;
  transition: transform 0.2s ease;
}

.bm-card:hover .bm-icon {
  transform: scale(1.06);
}

.bm-icon.lg {
  width: 52px;
  height: 52px;
  border-radius: 12px;
}

.bm-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 5px;
  box-sizing: border-box;
}

.bm-icon-fallback {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bm-title-block {
  min-width: 0;
  flex: 1;
}

.bm-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f1e30;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bm-url {
  margin-top: 3px;
  font-size: 11.5px;
  color: #8a94a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bm-open-arrow {
  flex-shrink: 0;
  color: #c0c8d4;
  font-size: 14px;
  transition: color 0.2s, transform 0.2s;
}

.bm-card-head:hover .bm-open-arrow {
  color: #2563eb;
  transform: translate(2px, -2px);
}

.bm-cats {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.bm-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bm-creds {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cred-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 9px;
  background: #f8fafc;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s;
}

.cred-item:hover {
  background: #eff6ff;
  border-color: #dbeafe;
}

.cred-label {
  font-size: 13px;
}

.cred-value {
  color: #1f2d3d;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cred-copy {
  color: #2563eb;
  font-size: 11px;
  flex-shrink: 0;
}

.bm-actions {
  display: flex;
  gap: 4px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

/* 添加/编辑弹窗样式见文件末尾的全局 <style> 块（弹窗 append-to-body 挂在 body 下，scoped 选择器无法命中） */
</style>

<style>
/* ===== 添加/编辑书签弹窗（append-to-body 挂在 body 下，需全局样式；bm-dlg 为 modal-class） ===== */
.bm-dlg .el-dialog {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(15, 30, 48, 0.3);
  width: min(720px, 94vw) !important;
}

.bm-dlg .el-dialog__header,
.bm-dlg .el-dialog__body,
.bm-dlg .el-dialog__footer {
  padding: 0;
}

/* ---- 头部横幅 ---- */
.bf-header {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
}

.bf-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.bf-deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -120px;
}

.bf-deco-2 {
  width: 140px;
  height: 140px;
  right: 110px;
  bottom: -90px;
  border-color: rgba(255, 255, 255, 0.07);
}

.bf-header-main {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.bf-header-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.bf-title {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.bf-subtitle {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.bf-close {
  position: relative;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}

.bf-close:hover {
  background: rgba(255, 255, 255, 0.26);
}

/* ---- 表单区 ---- */
.bf-body {
  padding: 20px 24px 8px;
  background: #fff;
}

.bf-field {
  margin-bottom: 13px;
}

.bf-field:last-child {
  margin-bottom: 6px;
}

.bf-label {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  margin-bottom: 6px;
}

.bf-req {
  color: #dc2626;
}

.bf-url-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bf-url-row .el-input {
  flex: 1;
}

.bf-loading {
  color: #2563eb;
  font-size: 18px;
  display: inline-block;
  flex-shrink: 0;
  animation: bf-spin 1s linear infinite;
}

@keyframes bf-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.bf-icon {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  border: 1px solid #eef2f7;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.06);
}

.bf-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 4px;
  box-sizing: border-box;
}

.bf-icon-fallback {
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 双列分区卡片:网页信息 / 登录凭证 */
.bf-cols {
  display: grid;
  grid-template-columns: 7fr 5fr;
  gap: 0 14px;
  margin-top: 2px;
}

/* 分区卡片 */
.bf-card {
  background: #f8fafc;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  padding: 12px 16px 4px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

/* 聚焦任一字段时整卡泛光 */
.bf-card:focus-within {
  border-color: #93c5fd;
  background: #fbfdff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.09);
}

/* 凭证卡:提示沉底 */
.bf-card-cred {
  display: flex;
  flex-direction: column;
}

.bf-card-cred .bf-cred-tip {
  margin-top: auto;
}

/* 分类卡 */
.bf-card-cats {
  margin-top: 14px;
}

/* 卡片小节头:图标徽章 + 标题 + 延伸细线 */
.bf-sec {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 11px;
}

.bf-sec-ico {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: #fff;
  border: 1px solid #e6edf5;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.bf-sec-text {
  font-size: 11.5px;
  color: #64748b;
  letter-spacing: 1px;
  font-weight: 600;
  flex-shrink: 0;
}

.bf-sec-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #e4eaf2, transparent);
}

/* 自动读取提示 */
.bf-hint {
  margin-top: 6px;
  padding: 5px 10px;
  border-radius: 8px;
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  color: #1d4ed8;
  font-size: 12px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bf-hint:hover {
  background: #dbeafe;
}

.bf-cred-tip {
  margin-top: 6px;
  padding-bottom: 6px;
  font-size: 11.5px;
  color: #94a3b8;
}

/* 分类三列(卡片内) */
.bf-cats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 14px;
}

.bm-dlg .el-select {
  width: 100%;
}

/* ---- 页脚操作条 ---- */
.bf-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
}

.bf-btn {
  padding: 9px 22px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  border: 1px solid transparent;
}

.bf-btn.ghost {
  background: #fff;
  border-color: #dbe2ea;
  color: #475569;
}

.bf-btn.ghost:hover {
  border-color: #b9c4d2;
  color: #1f2d3d;
}

.bf-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.bf-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

.bf-btn.primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

/* 输入控件圆角精修 */
.bm-dlg .el-input__wrapper,
.bm-dlg .el-textarea__inner,
.bm-dlg .el-select__wrapper {
  border-radius: 10px;
}

/* 窗口较窄时收起为单列 */
@media (max-width: 640px) {
  .bf-cols,
  .bf-cats {
    grid-template-columns: 1fr;
  }
}
</style>
