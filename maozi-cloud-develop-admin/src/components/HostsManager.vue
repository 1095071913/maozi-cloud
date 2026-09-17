<template>
  <div class="ht-page">
    <!-- ===== 顶部横幅（窗口拖拽区） ===== -->
    <header class="ht-hero">
      <div class="ht-deco ht-deco-1"></div>
      <div class="ht-deco ht-deco-2"></div>
      <div class="ht-hero-left">
        <div class="ht-hero-name">🌐 Hosts 管理</div>
        <div class="ht-hero-sub">
          {{ entryCount }} 条映射 · {{ enabledCount }} 条启用 · {{ disabledCount }} 条禁用
          <span class="ht-hero-path">{{ hostsPath }}</span>
        </div>
      </div>
      <div class="ht-hero-tools">
        <div class="ht-hero-search">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input v-model="keyword" placeholder="搜索 IP / 域名 / 备注" />
        </div>
        <button class="ht-hero-ghost" @click="onFlushDns"> ⟳ 刷新 DNS</button>
        <button class="ht-hero-ghost" @click="onOpenFile">📄 源文件</button>
        <button class="ht-hero-add" @click="openAdd">＋ 添加记录</button>
        <button
          v-if="dirty"
          class="ht-hero-save"
          :disabled="saving"
          title="自动保存失败，点击重试写入系统 hosts"
          @click="onRetrySave"
        >
          <span class="ht-save-dot"></span>
          {{ saving ? '保存中…' : '重试保存' }}
        </button>
      </div>
    </header>

    <!-- ===== 主体：筛选导航 + 记录卡片网格（锁定视口高度，无滚动条） ===== -->
    <div class="ht-body">
      <aside class="ht-rail">
        <div class="ht-rail-title">记录筛选</div>
        <div class="ht-node" :class="{ active: filter === 'all' }" @click="filter = 'all'">
          <span class="ht-node-ico">🗂</span>
          <span class="ht-node-label">全部记录</span>
          <span class="ht-node-count">{{ entryCount }}</span>
        </div>
        <div class="ht-node" :class="{ active: filter === 'enabled' }" @click="filter = 'enabled'">
          <span class="ht-node-ico">✅</span>
          <span class="ht-node-label">已启用</span>
          <span class="ht-node-count ht-count-ok">{{ enabledCount }}</span>
        </div>
        <div class="ht-node" :class="{ active: filter === 'disabled' }" @click="filter = 'disabled'">
          <span class="ht-node-ico">⏸️</span>
          <span class="ht-node-label">已禁用</span>
          <span class="ht-node-count ht-count-off">{{ disabledCount }}</span>
        </div>

        <div class="ht-rail-note">
          <div class="ht-rail-note-title">🛡 保存机制</div>
          <div class="ht-rail-note-text">所有改动（新增 / 编辑 / 删除 / 开关 / 排序）都会立即写入系统 hosts 并自动刷新 DNS；关闭开关只是把该行注释掉；localhost 等系统默认条目与纯注释行会原样保留。</div>
        </div>

        <div class="ht-rail-legend">
          <span class="ht-legend-item"><span class="ht-grip-demo">⠿</span>拖拽卡片可排序</span>
          <span class="ht-legend-item"><span class="ht-chip-demo">域名</span>点击域名可复制</span>
        </div>

        <div class="ht-rail-file">
          <span class="ht-rail-file-path" :title="hostsPath">{{ hostsPath }}</span>
          <span class="ht-rail-reload" @click="load">⟳ 重新加载</span>
        </div>
      </aside>

      <section class="ht-main">
        <div v-if="filter !== 'all' || keyword" class="ht-filter-bar">
          <span class="ht-filter-text">
            {{ filter === 'all' ? '全部记录' : filter === 'enabled' ? '已启用' : '已禁用' }}
            <template v-if="keyword">· 关键词「{{ keyword }}」</template>
            <template v-if="dirty">· <b class="ht-dirty-mark">有未保存的更改</b></template>
          </span>
          <span class="ht-filter-clear" @click="filter = 'all'; keyword = ''">清除筛选 ✕</span>
        </div>

        <div class="ht-grid" v-show="filteredEntries.length">
          <article
            v-for="row in filteredEntries"
            :key="row.id"
            class="ht-card"
            :class="{
              off: !row.enabled,
              dragging: dragId === row.id,
              'drop-above': dropMark && dropMark.id === row.id && dropMark.above,
              'drop-below': dropMark && dropMark.id === row.id && !dropMark.above
            }"
            draggable="true"
            @dragstart="onCardDragStart($event, row)"
            @dragend="onCardDragEnd"
            @dragover="onCardDragOver($event, row)"
            @dragleave="onCardDragLeave(row)"
            @drop="onCardDrop($event, row)"
          >
            <div class="ht-card-head">
              <span class="ht-grip" title="拖拽调整顺序">⠿</span>
              <span class="ht-ip" :title="row.ip">
                <span class="ht-ip-dot" :style="{ background: ipColor(row.ip ?? '') }"></span>
                {{ row.ip }}
              </span>
              <span class="ht-state-badge" :class="row.enabled ? 'on' : 'off'">
                {{ row.enabled ? '生效中' : '已注释' }}
              </span>
              <el-switch v-model="row.enabled" size="small" class="ht-switch" @change="onToggle(row)" />
            </div>
            <div class="ht-domains">
              <span
                v-for="d in domainChips(row)"
                :key="d"
                class="ht-chip"
                title="点击复制域名"
                @click="copyText(d)"
              >{{ d }}</span>
            </div>
            <div v-if="row.comment" class="ht-comment">💬 {{ row.comment }}</div>
            <div class="ht-card-foot">
              <span class="ht-line-no"># {{ visibleIndexOf(row) + 1 }}</span>
              <span class="ht-card-acts">
                <button class="ht-act edit" @click="openEdit(row)">编辑</button>
                <button class="ht-act del" @click="onRemove(row)">删除</button>
              </span>
            </div>
          </article>
        </div>

        <div v-if="!filteredEntries.length" class="ht-empty">
          <div class="ht-empty-ico">🌐</div>
          <div class="ht-empty-title">{{ keyword || filter !== 'all' ? '没有匹配的记录' : 'hosts 文件为空' }}</div>
          <div class="ht-empty-sub">{{ keyword || filter !== 'all' ? '换个关键词或筛选条件试试' : '点击右上角「添加记录」开始配置' }}</div>
        </div>
      </section>
    </div>

    <!-- ===== 添加/编辑记录弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :show-close="false"
      append-to-body
      align-center
      modal-class="ht-dlg"
      width="560px"
    >
      <div class="htd-header">
        <div class="htd-deco htd-deco-1"></div>
        <div class="htd-deco htd-deco-2"></div>
        <div class="htd-header-main">
          <div class="htd-header-icon">{{ isEdit ? '✏️' : '🌐' }}</div>
          <div>
            <div class="htd-title">{{ isEdit ? '编辑记录' : '添加记录' }}</div>
            <div class="htd-subtitle">一条 IP 与域名的映射，保存后写入 hosts 文件</div>
          </div>
        </div>
        <button class="htd-close" @click="dialogVisible = false">✕</button>
      </div>

      <div class="htd-body">
        <div class="htd-cols">
          <div class="htd-field">
            <div class="htd-label">IP 地址 <span class="htd-req">*</span></div>
            <input v-model="form.ip" class="htd-input mono" placeholder="如 127.0.0.1 或 ::1" />
          </div>
          <div class="htd-field">
            <div class="htd-label">启用</div>
            <div class="htd-switch-row">
              <el-switch v-model="form.enabled" />
              <span class="htd-switch-hint">{{ form.enabled ? '写入为生效行' : '写入为注释行' }}</span>
            </div>
          </div>
        </div>
        <div class="htd-field">
          <div class="htd-label">域名 <span class="htd-req">*</span></div>
          <textarea
            v-model="form.domains"
            class="htd-input htd-textarea mono"
            rows="2"
            placeholder="如 api.maozi.cloud www.maozi.cloud，空格分隔"
          ></textarea>
        </div>
        <div class="htd-field">
          <div class="htd-label">备注</div>
          <input v-model="form.comment" class="htd-input" placeholder="写入行内注释，如：本地后端" />
        </div>
        <div class="htd-hint">💡 域名支持一次填写多个；确定后立即写入系统 hosts 并自动刷新 DNS</div>
      </div>

      <template #footer>
        <div class="htd-footer">
          <button class="htd-btn ghost" @click="dialogVisible = false">取消</button>
          <button class="htd-btn primary" @click="onConfirm">确定</button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 源文件编辑弹窗（暗色代码编辑器） ===== -->
    <el-dialog
      v-model="rawDialogVisible"
      :show-close="false"
      append-to-body
      align-center
      modal-class="ht-dlg ht-raw-dlg"
      width="760px"
    >
      <div class="htd-header">
        <div class="htd-deco htd-deco-1"></div>
        <div class="htd-deco htd-deco-2"></div>
        <div class="htd-header-main">
          <div class="htd-header-icon">📄</div>
          <div>
            <div class="htd-title">编辑 hosts 源文件</div>
            <div class="htd-subtitle mono">{{ hostsPath }} · 整体覆盖保存，自动备份原文件</div>
          </div>
        </div>
        <button class="htd-close" @click="rawDialogVisible = false">✕</button>
      </div>

      <div class="htd-body htd-raw-body">
        <textarea
          v-model="rawText"
          class="ht-raw-area mono"
          rows="20"
          spellcheck="false"
          placeholder="读取 hosts 内容..."
        ></textarea>
      </div>

      <template #footer>
        <div class="htd-footer">
          <button class="htd-btn ghost" @click="rawDialogVisible = false">取消</button>
          <button class="htd-btn danger" :disabled="rawSaving" @click="onSaveRaw">
            {{ rawSaving ? '保存中…' : '保存到系统' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, type HostsEntry } from '../api'

const entries = ref<HostsEntry[]>([])
const hostsPath = ref('/etc/hosts')
const saving = ref(false)
const keyword = ref('')
const filter = ref<'all' | 'enabled' | 'disabled'>('all')

/** 系统默认条目（精确匹配整行，自动忽略不显示，保存时原样保留在文件中） */
const SYSTEM_DEFAULT_HOSTS: Array<[string, string]> = [
  ['127.0.0.1', 'localhost'],
  ['255.255.255.255', 'broadcasthost'],
  ['::1', 'localhost']
]

function isSystemDefault(e: HostsEntry): boolean {
  return SYSTEM_DEFAULT_HOSTS.some(([ip, domains]) => e.ip === ip && e.domains === domains)
}

/** 只展示用户自己的映射记录；纯注释行与系统默认条目保留在 entries 中随保存原样写回 */
const visibleEntries = computed(() =>
  entries.value.filter((e) => e.kind === 'entry' && !isSystemDefault(e))
)

const filteredEntries = computed(() =>
  visibleEntries.value.filter((e) => {
    if (filter.value === 'enabled' && !e.enabled) return false
    if (filter.value === 'disabled' && e.enabled) return false
    if (!keyword.value) return true
    const k = keyword.value.toLowerCase()
    return (
      (e.ip ?? '').toLowerCase().includes(k) ||
      (e.domains ?? '').toLowerCase().includes(k) ||
      (e.comment ?? '').toLowerCase().includes(k)
    )
  })
)

const entryCount = computed(() => visibleEntries.value.length)
const enabledCount = computed(() => visibleEntries.value.filter((e) => e.enabled).length)
const disabledCount = computed(() => entryCount.value - enabledCount.value)

/** ===== 未保存更改标记（增删改/开关/排序后点亮保存按钮） ===== */
const dirty = ref(false)
let suppressDirty = false
watch(
  entries,
  () => {
    if (!suppressDirty) dirty.value = true
  },
  { deep: true }
)

/** 立即写入系统 hosts（含管理员授权、自动备份与 DNS 刷新），返回是否成功 */
async function saveToSystem(): Promise<boolean> {
  try {
    // reactive Proxy 无法跨 IPC 结构化克隆，先深拷贝成纯数组
    const plain = JSON.parse(JSON.stringify(entries.value)) as HostsEntry[]
    const result = await api.hosts.save(plain)
    if (!result.ok) {
      ElMessage.error(result.error ?? '写入系统 hosts 失败')
      return false
    }
    dirty.value = false
    return true
  } catch (err) {
    ElMessage.error((err as Error).message || '写入系统 hosts 失败')
    return false
  }
}

function domainChips(row: HostsEntry): string[] {
  return (row.domains ?? '').split(/\s+/).filter(Boolean)
}

function visibleIndexOf(row: HostsEntry): number {
  return visibleEntries.value.findIndex((e) => e.id === row.id)
}

const IP_COLORS = ['#2563eb', '#0891b2', '#0ea5e9', '#38bdf8', '#d97706', '#db2777']

function ipColor(ip: string): string {
  let hash = 0
  for (const ch of ip) hash = (hash * 31 + ch.charCodeAt(0)) % 997
  return IP_COLORS[hash % IP_COLORS.length]
}

async function copyText(text: string): Promise<void> {
  const result = await api.util.copy(text)
  if (result.ok) ElMessage.success(`已复制 ${text}`)
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ ip: '', domains: '', comment: '', enabled: true })
let editRowId = ''

const IP_RE = /^(\d{1,3}\.){3}\d{1,3}$/

async function load(): Promise<void> {
  const result = await api.hosts.list()
  if (!result.ok) {
    ElMessage.error(result.error ?? '读取 hosts 失败')
    return
  }
  suppressDirty = true
  entries.value = result.entries
  await nextTick()
  suppressDirty = false
  dirty.value = false
}

function openAdd(): void {
  isEdit.value = false
  editRowId = ''
  form.value = { ip: '', domains: '', comment: '', enabled: true }
  dialogVisible.value = true
}

function openEdit(row: HostsEntry): void {
  isEdit.value = true
  editRowId = row.id
  form.value = {
    ip: row.ip ?? '',
    domains: row.domains ?? '',
    comment: row.comment ?? '',
    enabled: row.enabled
  }
  dialogVisible.value = true
}

async function onConfirm(): Promise<void> {
  const { ip, domains, comment, enabled } = form.value
  const normalizedDomains = domains.split(/[\s,]+/).filter(Boolean).join(' ')
  if (!IP_RE.test(ip) && !ip.includes(':')) {
    ElMessage.warning('IP 地址格式不正确')
    return
  }
  if (!normalizedDomains) {
    ElMessage.warning('请填写域名')
    return
  }
  const entry: HostsEntry = {
    id: `row-${Date.now()}`,
    kind: 'entry',
    ip,
    domains: normalizedDomains,
    comment: comment || undefined,
    enabled
  }
  if (isEdit.value && editRowId) {
    const index = entries.value.findIndex((e) => e.id === editRowId)
    if (index >= 0) entries.value[index] = entry
    dialogVisible.value = false
    // 编辑确定后立即写入系统（失败时保留未保存标记，可经「保存到系统」重试）
    if (await saveToSystem()) {
      ElMessage.success('修改已写入系统 hosts，DNS 缓存已刷新')
    }
  } else {
    entries.value.push(entry)
    dialogVisible.value = false
    if (await saveToSystem()) {
      ElMessage.success('已添加并写入系统 hosts，DNS 缓存已刷新')
    }
  }
}

async function onRemove(row: HostsEntry): Promise<void> {
  const index = entries.value.findIndex((e) => e.id === row.id)
  if (index >= 0) entries.value.splice(index, 1)
  // 删除后立即写入系统（失败时保留未保存标记，可经「保存到系统」重试）
  if (await saveToSystem()) {
    ElMessage.success('已删除并写入系统 hosts，DNS 缓存已刷新')
  }
}

/** 自动保存失败后的手动重试入口 */
async function onRetrySave(): Promise<void> {
  saving.value = true
  try {
    if (await saveToSystem()) {
      ElMessage.success('hosts 已更新，DNS 缓存已刷新')
    }
  } finally {
    saving.value = false
  }
}

/** 启停开关切换后立即写入系统 */
async function onToggle(row: HostsEntry): Promise<void> {
  if (await saveToSystem()) {
    ElMessage.success(
      row.enabled ? `已启用并写入系统：${row.domains}` : `已注释并写入系统：${row.domains}`
    )
  }
}

async function onFlushDns(): Promise<void> {
  const result = await api.hosts.flushDns()
  if (!result.ok) {
    ElMessage.error(result.error ?? '刷新失败')
    return
  }
  ElMessage.success('DNS 缓存已刷新')
}

/** ===== 源文件编辑弹窗：读取原文，保存走管理员授权 ===== */
const rawDialogVisible = ref(false)
const rawText = ref('')
const rawSaving = ref(false)

async function onOpenFile(): Promise<void> {
  const result = await api.hosts.readRaw()
  if (!result.ok) {
    ElMessage.error(result.error ?? '读取 hosts 失败')
    return
  }
  rawText.value = result.data ?? ''
  rawDialogVisible.value = true
}

async function onSaveRaw(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '将按编辑区内容整体覆盖 hosts 文件并刷新 DNS 缓存，期间会弹出管理员授权框。原文件会自动备份。继续吗？',
      '保存源文件',
      { type: 'warning', confirmButtonText: '保存', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  rawSaving.value = true
  try {
    const result = await api.hosts.saveRaw(rawText.value)
    if (!result.ok) {
      ElMessage.error(result.error ?? '保存失败')
      return
    }
    ElMessage.success('hosts 源文件已更新，DNS 缓存已刷新')
    rawDialogVisible.value = false
    await load()
  } finally {
    rawSaving.value = false
  }
}

/** ===== 卡片拖拽排序（重排内存数组，随「保存到系统」写回文件；隐藏的注释/系统默认行保持原位） ===== */
const dragId = ref('')
const dropMark = ref<{ id: string; above: boolean } | null>(null)

function onCardDragStart(e: DragEvent, row: HostsEntry): void {
  dragId.value = row.id
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', row.id)
  }
}

function onCardDragEnd(): void {
  dragId.value = ''
  dropMark.value = null
}

function onCardDragOver(e: DragEvent, row: HostsEntry): void {
  if (!dragId.value || row.id === dragId.value) return
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const above = e.clientY < rect.top + rect.height / 2
  if (!dropMark.value || dropMark.value.id !== row.id || dropMark.value.above !== above) {
    dropMark.value = { id: row.id, above }
  }
}

function onCardDragLeave(row: HostsEntry): void {
  if (dropMark.value && dropMark.value.id === row.id) dropMark.value = null
}

async function onCardDrop(_e: DragEvent, row: HostsEntry): Promise<void> {
  const mark = dropMark.value
  const id = dragId.value
  onCardDragEnd()
  if (!mark || mark.id !== row.id || !id) return
  const dragged = entries.value.find((e) => e.id === id)
  const target = entries.value.find((e) => e.id === mark.id)
  if (!dragged || !target || dragged === target) return
  entries.value.splice(entries.value.indexOf(dragged), 1)
  const to = entries.value.indexOf(target)
  entries.value.splice(mark.above ? to : to + 1, 0, dragged)
  if (await saveToSystem()) {
    ElMessage.success('顺序已写入系统 hosts')
  }
}

onMounted(async () => {
  const info = await api.app.info()
  hostsPath.value = info.hostsPath
  await load()
})
</script>

<style scoped>
/* ===== 页面骨架：锁定视口高度，整页无滚动条 ===== */
.ht-page {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  gap: 14px;
  animation: ht-page-in 0.3s ease both;
}

@keyframes ht-page-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 顶部横幅（窗口拖拽区） ===== */
.ht-hero {
  -webkit-app-region: drag;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 28px;
  border-radius: 18px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
}

.ht-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.ht-deco-1 {
  width: 260px;
  height: 260px;
  right: -60px;
  top: -140px;
}

.ht-deco-2 {
  width: 170px;
  height: 170px;
  right: 130px;
  bottom: -110px;
  border-color: rgba(255, 255, 255, 0.07);
}

.ht-hero-left {
  position: relative;
  min-width: 0;
}

.ht-hero-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.ht-hero-sub {
  margin-top: 7px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ht-hero-path {
  margin-left: 10px;
  padding: 1px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 11px;
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
}

.ht-hero-tools {
  -webkit-app-region: no-drag;
  position: relative;
  display: flex;
  flex-wrap: nowrap;
  flex-shrink: 0;
  align-items: center;
  gap: 9px;
}

.ht-hero-search {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.8);
  transition: background 0.2s;
}

.ht-hero-search:focus-within {
  background: rgba(255, 255, 255, 0.2);
}

.ht-hero-search input {
  border: none;
  outline: none;
  background: transparent;
  color: #fff;
  font-size: 13px;
  width: 158px;
}

.ht-hero-search input::placeholder {
  color: rgba(255, 255, 255, 0.55);
}

.ht-hero-ghost {
  white-space: nowrap;
  flex-shrink: 0;
  padding: 10px 13px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: background 0.15s ease, transform 0.15s ease;
}

.ht-hero-ghost:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.ht-hero-add {
  white-space: nowrap;
  flex-shrink: 0;
  padding: 10px 16px;
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

.ht-hero-add:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.24);
}

.ht-hero-save {
  position: relative;
  white-space: nowrap;
  flex-shrink: 0;
  padding: 10px 16px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #f87171, #dc2626);
  color: #fff;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.ht-hero-save:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(220, 38, 38, 0.45);
}

.ht-hero-save:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.ht-hero-save {
  padding: 10px 14px;
}

.ht-save-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #fff;
  margin-right: 6px;
  animation: ht-dot-pulse 1.6s ease infinite;
  vertical-align: 1px;
}

@keyframes ht-dot-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.55);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(255, 255, 255, 0);
  }
}

/* ===== 主体：筛选导航 + 卡片网格 ===== */
.ht-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 14px;
}

.ht-rail {
  flex-shrink: 0;
  width: 232px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 14px 10px 12px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
  overflow: hidden;
}

.ht-rail-title {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 2px;
  font-weight: 600;
  padding: 0 10px;
  margin-bottom: 10px;
}

.ht-node {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
  margin-bottom: 2px;
}

.ht-node:hover {
  background: #f4f6fb;
}

.ht-node.active {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.22);
}

.ht-node.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 22%;
  bottom: 22%;
  width: 3px;
  border-radius: 2px;
  background: #2563eb;
}

.ht-node-ico {
  font-size: 14px;
  line-height: 1;
  flex-shrink: 0;
}

.ht-node-label {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ht-node.active .ht-node-label {
  color: #1d4ed8;
  font-weight: 700;
}

.ht-node-count {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 999px;
  padding: 1px 8px;
}

.ht-node.active .ht-node-count {
  color: #2563eb;
  background: rgba(255, 255, 255, 0.85);
}

.ht-count-ok {
  color: #059669 !important;
}

.ht-count-off {
  color: #dc2626 !important;
}

.ht-rail-note {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
}

.ht-rail-note-title {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 6px;
}

.ht-rail-note-text {
  font-size: 12px;
  line-height: 1.75;
  color: #7d8a9e;
}

.ht-rail-legend {
  margin-top: auto;
  padding: 10px 10px 4px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ht-legend-item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: #94a3b8;
}

.ht-grip-demo {
  color: #b6c2d2;
  font-size: 13px;
  line-height: 1;
}

.ht-chip-demo {
  padding: 1px 8px;
  border-radius: 6px;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  color: #1d4ed8;
  font-size: 10.5px;
}

.ht-rail-file {
  margin-top: 8px;
  padding: 9px 10px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ht-rail-file-path {
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 11px;
  color: #7d8a9e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ht-rail-reload {
  flex-shrink: 0;
  font-size: 11.5px;
  font-weight: 600;
  color: #2563eb;
  cursor: pointer;
}

.ht-rail-reload:hover {
  text-decoration: underline;
}

.ht-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ===== 筛选条 ===== */
.ht-filter-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-radius: 12px;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  margin-bottom: 12px;
}

.ht-filter-text {
  font-size: 12.5px;
  color: #1d4ed8;
  font-weight: 600;
}

.ht-dirty-mark {
  color: #dc2626;
}

.ht-filter-clear {
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
}

.ht-filter-clear:hover {
  color: #dc2626;
}

/* ===== 记录卡片网格（静默滚动） ===== */
.ht-grid {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(285px, 1fr));
  gap: 12px;
  align-content: start;
  padding: 2px 2px 6px;
}

.ht-grid::-webkit-scrollbar {
  display: none;
}

.ht-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 13px 15px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.04);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, opacity 0.15s ease;
}

.ht-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 8px 22px rgba(37, 99, 235, 0.12);
}

.ht-card.off .ht-domains,
.ht-card.off .ht-comment {
  opacity: 0.55;
}

.ht-card.dragging {
  opacity: 0.4;
}

.ht-card.drop-above {
  box-shadow: 0 -3px 0 0 #2563eb, 0 -8px 16px rgba(37, 99, 235, 0.2);
}

.ht-card.drop-below {
  box-shadow: 0 3px 0 0 #2563eb, 0 8px 16px rgba(37, 99, 235, 0.2);
}

.ht-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ht-grip {
  color: #c3cedd;
  font-size: 14px;
  line-height: 1;
  cursor: grab;
  user-select: none;
  flex-shrink: 0;
  transition: color 0.15s ease;
}

.ht-card:hover .ht-grip {
  color: #2563eb;
}

.ht-ip {
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ht-ip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.ht-state-badge {
  flex-shrink: 0;
  font-size: 10.5px;
  font-weight: 700;
  border-radius: 999px;
  padding: 2px 8px;
}

.ht-state-badge.on {
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.ht-state-badge.off {
  color: #64748b;
  background: #f1f5f9;
  border: 1px solid #e6edf5;
}

.ht-switch {
  flex-shrink: 0;
  margin-left: 2px;
}

.ht-domains {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.ht-chip {
  padding: 2px 9px;
  border-radius: 7px;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  color: #1d4ed8;
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 11.5px;
  font-weight: 600;
  cursor: copy;
  transition: background 0.15s ease, border-color 0.15s ease;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ht-chip:hover {
  background: #dbeafe;
  border-color: #bfdbfe;
}

.ht-comment {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ht-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 2px;
}

.ht-line-no {
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 10.5px;
  color: #c3cedd;
}

.ht-card-acts {
  display: inline-flex;
  gap: 6px;
  flex-shrink: 0;
}

.ht-act {
  padding: 4px 11px;
  border-radius: 8px;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;
  font-family: inherit;
}

.ht-act.edit {
  background: #eff6ff;
  color: #2563eb;
  border-color: #dbeafe;
}

.ht-act.edit:hover {
  background: #dbeafe;
}

.ht-act.del {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fee2e2;
}

.ht-act.del:hover {
  background: #fee2e2;
}

/* ===== 空状态 ===== */
.ht-empty {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px dashed #e2e8f0;
  border-radius: 14px;
}

.ht-empty-ico {
  font-size: 44px;
  margin-bottom: 12px;
  opacity: 0.85;
}

.ht-empty-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #334155;
}

.ht-empty-sub {
  margin-top: 6px;
  font-size: 12.5px;
  color: #94a3b8;
}

/* 响应式：窄窗口时收起筛选栏 */
@media (max-width: 900px) {
  .ht-rail {
    display: none;
  }
}
</style>

<!-- 弹窗挂载在 body 下（append-to-body），需全局样式；ht-dlg 为 modal-class -->
<style>
.ht-dlg .el-dialog {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(15, 30, 48, 0.3);
  width: min(560px, 94vw) !important;
}

/* 覆盖全局弹窗拉伸规则：高度贴合内容 */
.ht-dlg .el-dialog {
  align-self: center;
  height: auto;
}

.ht-dlg .el-dialog__body {
  flex: 0 1 auto !important;
}

.ht-raw-dlg .el-dialog {
  width: min(760px, 94vw) !important;
}

.ht-dlg .el-dialog__header,
.ht-dlg .el-dialog__body,
.ht-dlg .el-dialog__footer {
  padding: 0;
}

.htd-header {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
}

.htd-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.htd-deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -120px;
}

.htd-deco-2 {
  width: 140px;
  height: 140px;
  right: 110px;
  bottom: -90px;
  border-color: rgba(255, 255, 255, 0.07);
}

.htd-header-main {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.htd-header-icon {
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

.htd-title {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.htd-subtitle {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.htd-close {
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

.htd-close:hover {
  background: rgba(255, 255, 255, 0.26);
}

.htd-body {
  padding: 20px 24px 12px;
  background: #fff;
}

.htd-field {
  margin-bottom: 14px;
}

.htd-cols {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0 16px;
  align-items: end;
}

.htd-label {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  margin-bottom: 6px;
}

.htd-req {
  color: #dc2626;
}

.htd-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  padding: 9px 12px;
  font-size: 13px;
  color: #1f2d3d;
  outline: none;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  font-family: inherit;
}

.htd-input.mono {
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 12.5px;
}

.htd-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.htd-textarea {
  resize: none;
  min-height: 64px;
  line-height: 1.7;
}

.htd-switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 6px;
}

.htd-switch-hint {
  font-size: 11.5px;
  color: #94a3b8;
  white-space: nowrap;
}

.htd-hint {
  margin: 4px 0 4px;
  font-size: 11.5px;
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  border-radius: 7px;
  padding: 6px 10px;
}

/* ===== 源文件暗色编辑器 ===== */
.htd-raw-body {
  background: #0f172a;
  padding: 14px 16px 16px;
}

.ht-raw-area {
  display: block;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #334155;
  border-radius: 12px;
  background: #0b1220;
  color: #dbe4f3;
  padding: 12px 14px;
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.75;
  outline: none;
  resize: vertical;
  min-height: 420px;
  transition: border-color 0.15s ease;
}

.ht-raw-area:focus {
  border-color: #2563eb;
}

.ht-raw-area::placeholder {
  color: #475569;
}

.htd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
}

.htd-btn {
  padding: 9px 22px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  border: 1px solid transparent;
  font-family: inherit;
}

.htd-btn.ghost {
  background: #fff;
  border-color: #dbe2ea;
  color: #475569;
}

.htd-btn.ghost:hover {
  border-color: #b9c4d2;
  color: #1f2d3d;
}

.htd-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.htd-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

.htd-btn.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.35);
}

.htd-btn.danger:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(220, 38, 38, 0.45);
}

.htd-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.mono {
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
}
</style>
