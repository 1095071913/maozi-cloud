<template>
  <div class="ev-page">
    <!-- ===== 顶部横幅（窗口拖拽区） ===== -->
    <header class="ev-hero">
      <div class="ev-deco ev-deco-1"></div>
      <div class="ev-deco ev-deco-2"></div>
      <div class="ev-hero-left">
        <div class="ev-hero-name">🌱 环境变量</div>
        <div class="ev-hero-sub">
          {{ vars.length }} 个变量 · {{ files.length }} 个配置文件 · {{ managedCount }} 条由本应用托管
          <span v-if="disabledCount" class="ev-hero-tip off">⏸ {{ disabledCount }} 条已禁用</span>
          <span class="ev-hero-tip">写入前自动备份 · 已打开的终端自动生效</span>
        </div>
      </div>
      <div class="ev-hero-tools">
        <div class="ev-hero-search">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input v-model="keyword" placeholder="搜索变量名 / 值" />
        </div>
        <button v-if="isMac" class="ev-hero-bolt" :disabled="launchingAll" @click="onLaunchctlAll">
          {{ launchingAll ? '⏳ 生效中…' : '⚡ 全部生效' }}
        </button>
        <button class="ev-hero-add" @click="openAdd">＋ 添加变量</button>
      </div>
    </header>

    <!-- ===== 主体：文件导航 + 变量卡片网格（锁定视口高度，无滚动条） ===== -->
    <div class="ev-body">
      <aside class="ev-rail">
        <div class="ev-rail-title">来源文件</div>
        <div class="ev-node" :class="{ active: fileFilter === 'all' }" @click="fileFilter = 'all'">
          <span class="ev-node-ico">📦</span>
          <span class="ev-node-label">全部变量</span>
          <span class="ev-node-count">{{ vars.length }}</span>
        </div>
        <div
          v-for="f in fileStats"
          :key="f.id"
          class="ev-node"
          :class="{ active: fileFilter === f.id }"
          @click="fileFilter = f.id"
        >
          <span class="ev-node-ico">📄</span>
          <span class="ev-node-label" :title="f.name">{{ f.name }}</span>
          <span class="ev-node-count">{{ f.count }}</span>
        </div>
        <div class="ev-rail-note">
          <div class="ev-rail-note-title">🛡 安全机制</div>
          <div class="ev-rail-note-text">写入前自动备份为 *.maozi.bak；本应用新增的变量统一放在「maozi-cloud-develop-admin」托管区块，不影响手写配置；已打开的终端经 zsh 钩子自动同步，无需重开。</div>
        </div>
        <div class="ev-rail-legend">
          <span class="ev-legend-item"><span class="ev-grip-demo">⠿</span>拖拽卡片可排序</span>
          <span class="ev-legend-item"><span class="ev-dot-demo"></span>点击值一行可复制</span>
        </div>
      </aside>

      <section class="ev-main">
        <div v-if="fileFilter !== 'all' || keyword" class="ev-filter-bar">
          <span class="ev-filter-text">
            {{ fileFilter === 'all' ? '全部文件' : fileName(fileFilter) }}
            <template v-if="keyword">· 关键词「{{ keyword }}」</template>
          </span>
          <span class="ev-filter-clear" @click="fileFilter = 'all'; keyword = ''">清除筛选 ✕</span>
        </div>

        <div class="ev-grid" v-show="filteredVars.length">
          <article
            v-for="v in filteredVars"
            :key="v.fileId + '::' + v.key"
            class="ev-card"
            :class="{
              dragging: dragKey === v.key,
              off: v.enabled === false,
              'drop-above': dropMark && dropMark.key === v.key && dropMark.above,
              'drop-below': dropMark && dropMark.key === v.key && !dropMark.above
            }"
            draggable="true"
            @dragstart="onCardDragStart($event, v)"
            @dragend="onCardDragEnd"
            @dragover="onCardDragOver($event, v)"
            @dragleave="onCardDragLeave(v)"
            @drop="onCardDrop($event, v)"
          >
            <div class="ev-card-head">
              <span class="ev-grip" title="拖拽排序（同文件内）">⠿</span>
              <span class="ev-key" :title="v.key">{{ v.key }}</span>
              <span v-if="v.managed" class="ev-badge managed">本应用</span>
              <span v-else class="ev-badge manual">手动</span>
              <el-switch
                v-model="v.enabled"
                size="small"
                class="ev-switch"
                :disabled="v.enabled === undefined"
                title="禁用 = 注释该行；启用 = 取消注释"
                @change="onToggle(v)"
              />
            </div>
            <div class="ev-card-val" title="点击复制" @click="copyVal(v)">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="ev-copy-ico">
                <rect x="9" y="9" width="12" height="12" rx="2" />
                <path d="M5 15V5a2 2 0 0 1 2-2h10" />
              </svg>
              <span class="ev-val-text">{{ v.value || '—' }}</span>
            </div>
            <div class="ev-card-foot">
              <span class="ev-file-tag">
                <span class="ev-file-dot" :style="{ background: fileColor(v.fileId) }"></span>
                {{ fileName(v.fileId) }}
              </span>
              <span class="ev-card-acts">
                <button class="ev-act edit" @click="openEdit(v)">编辑</button>
                <button
                  v-if="isMac"
                  class="ev-act live"
                  title="launchctl setenv：对已启动的图形应用立即生效（重启 Mac 后失效）"
                  @click="onLaunchctl(v)"
                >
                  生效
                </button>
                <button class="ev-act del" @click="onRemove(v)">删除</button>
              </span>
            </div>
          </article>
        </div>

        <div v-if="!filteredVars.length" class="ev-empty">
          <div class="ev-empty-ico">🌱</div>
          <div class="ev-empty-title">{{ keyword || fileFilter !== 'all' ? '没有匹配的变量' : '暂无环境变量' }}</div>
          <div class="ev-empty-sub">{{ keyword || fileFilter !== 'all' ? '换个关键词或文件试试' : '点击右上角「添加变量」开始配置' }}</div>
        </div>
      </section>
    </div>

    <!-- ===== 添加/编辑变量弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :show-close="false"
      append-to-body
      align-center
      modal-class="ev-dlg"
      width="620px"
    >
      <div class="evd-header">
        <div class="evd-deco evd-deco-1"></div>
        <div class="evd-deco evd-deco-2"></div>
        <div class="evd-header-main">
          <div class="evd-header-icon">{{ isEdit ? '✏️' : '🌱' }}</div>
          <div>
            <div class="evd-title">{{ isEdit ? '编辑变量' : '添加变量' }}</div>
            <div class="evd-subtitle">{{ isEdit ? `修改 ${form.key} 的值` : '写入 shell 配置文件的 export 变量' }}</div>
          </div>
        </div>
        <button class="evd-close" @click="dialogVisible = false">✕</button>
      </div>

      <div class="evd-body">
        <div class="evd-field">
          <div class="evd-label">变量名 <span class="evd-req">*</span></div>
          <input
            v-model="form.key"
            class="evd-input mono"
            :disabled="isEdit"
            placeholder="如 JAVA_HOME、MAOZI_ENV"
          />
        </div>
        <div class="evd-field">
          <div class="evd-label">值 <span class="evd-req">*</span></div>
          <textarea
            v-model="form.value"
            class="evd-input evd-textarea mono"
            rows="3"
            placeholder="如 /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home，支持 $(...) 与 ${VAR} 引用"
          ></textarea>
        </div>
        <div class="evd-field">
          <div class="evd-label">写入文件</div>
          <div class="evd-files">
            <button
              v-for="f in files"
              :key="f.id"
              type="button"
              class="evd-file"
              :class="{ active: form.fileId === f.id }"
              @click="form.fileId = f.id"
            >
              <span class="evd-file-ico">📄</span>
              <span class="evd-file-body">
                <span class="evd-file-name">{{ f.name }}</span>
                <span class="evd-file-tip">{{ f.exists ? '已存在' : '将自动创建' }}</span>
              </span>
              <span v-if="form.fileId === f.id" class="evd-file-check">✓</span>
            </button>
          </div>
        </div>
        <div class="evd-hint">💾 保存后立即生效，已打开的终端在下一条命令前自动同步{{ isMac ? '；「生效」按钮用于让 IDE、浏览器等图形应用立即读取' : '' }}</div>
      </div>

      <template #footer>
        <div class="evd-footer">
          <button class="evd-btn ghost" @click="dialogVisible = false">取消</button>
          <button class="evd-btn primary" :disabled="saving" @click="onSave">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, type EnvFile, type EnvVarEntry } from '../api'

const files = ref<EnvFile[]>([])
const vars = ref<EnvVarEntry[]>([])
const fileFilter = ref('all')
const keyword = ref('')
const isMac = ref(true)

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const form = ref({ key: '', value: '', fileId: 'zshrc' })

const filteredVars = computed(() =>
  vars.value.filter((v) => {
    if (fileFilter.value !== 'all' && v.fileId !== fileFilter.value) return false
    if (!keyword.value) return true
    const k = keyword.value.toLowerCase()
    return v.key.toLowerCase().includes(k) || v.value.toLowerCase().includes(k)
  })
)

const managedCount = computed(() => vars.value.filter((v) => v.managed).length)
const disabledCount = computed(() => vars.value.filter((v) => v.enabled === false).length)

/** 启用/禁用开关：立即写回 shell 文件（禁用=注释该行，启用=取消注释） */
async function onToggle(v: EnvVarEntry): Promise<void> {
  const r = await api.env.save({ mode: 'toggle', key: v.key, fileId: v.fileId, enabled: v.enabled })
  if (!r.ok) {
    ElMessage.error(r.error ?? '操作失败')
    v.enabled = !v.enabled
    return
  }
  if (v.enabled === false) {
    // 同步清理 launchd 图形会话中此前「立即生效」设置的值，否则新终端仍会继承
    await api.env.launchctl({ action: 'unset', key: v.key })
    ElMessage.success(`已禁用 ${v.key}：该行已注释，并清理图形会话继承`)
  } else {
    ElMessage.success(`已启用 ${v.key}，已打开的终端自动生效`)
  }
}

/** 侧边栏 / hero 芯片使用的文件统计 */
const fileStats = computed(() =>
  files.value.map((f) => ({
    ...f,
    count: vars.value.filter((v) => v.fileId === f.id).length
  }))
)

const FILE_COLORS = ['#2563eb', '#0891b2', '#0ea5e9', '#38bdf8', '#d97706', '#db2777']

function fileColor(fileId: string): string {
  const idx = files.value.findIndex((f) => f.id === fileId)
  return FILE_COLORS[(idx < 0 ? 0 : idx) % FILE_COLORS.length]
}

function fileName(fileId: string): string {
  return files.value.find((f) => f.id === fileId)?.name ?? fileId
}

async function copyVal(row: EnvVarEntry): Promise<void> {
  if (!row.value) return
  const result = await api.util.copy(row.value)
  if (result.ok) ElMessage.success(`已复制 ${row.key} 的值`)
}

async function load(): Promise<void> {
  const result = await api.env.list()
  files.value = result.files
  vars.value = result.vars
}

function openAdd(): void {
  isEdit.value = false
  form.value = { key: '', value: '', fileId: fileFilter.value !== 'all' ? fileFilter.value : 'zshrc' }
  dialogVisible.value = true
}

function openEdit(row: EnvVarEntry): void {
  isEdit.value = true
  form.value = { key: row.key, value: row.value, fileId: row.fileId }
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  const { key, value, fileId } = form.value
  if (!key) {
    ElMessage.warning('请填写变量名')
    return
  }
  if (!isEdit.value && !value) {
    ElMessage.warning('请填写变量值')
    return
  }
  saving.value = true
  try {
    const result = await api.env.save({
      mode: isEdit.value ? 'update' : 'add',
      key,
      value,
      fileId
    })
    if (!result.ok) {
      ElMessage.error(result.error ?? '保存失败')
      return
    }
    ElMessage.success(`已写入 ${fileName(fileId)}，立即生效`)
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row: EnvVarEntry): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定从 ${fileName(row.fileId)} 中删除 ${row.key} 吗？`,
      '删除变量',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await api.env.save({ mode: 'remove', key: row.key, fileId: row.fileId })
  if (!result.ok) {
    ElMessage.error(result.error ?? '删除失败')
    return
  }
  // 同步清理图形会话中可能存在的同名变量
  await api.env.launchctl({ action: 'unset', key: row.key })
  ElMessage.success('已删除')
  await load()
}

async function onLaunchctl(row: EnvVarEntry): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `将执行 launchctl setenv ${row.key}，对当前会话中的图形应用（IDE、浏览器等）立即生效；重启 Mac 后失效，持久配置已同时存在于 shell 文件中。`,
      '立即生效于图形应用',
      { confirmButtonText: '执行', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await api.env.launchctl({ action: 'set', key: row.key, value: row.value })
  if (!result.ok) {
    ElMessage.error(result.error ?? '执行失败')
    return
  }
  ElMessage.success('已设置，图形应用立即生效')
}

const launchingAll = ref(false)

/** ===== 卡片拖拽排序（仅同一文件内） ===== */
const dragKey = ref('')
const dropMark = ref<{ key: string; above: boolean } | null>(null)

function onCardDragStart(e: DragEvent, row: EnvVarEntry): void {
  dragKey.value = row.key
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', row.key)
  }
}

function onCardDragEnd(): void {
  dragKey.value = ''
  dropMark.value = null
}

function onCardDragOver(e: DragEvent, row: EnvVarEntry): void {
  if (!dragKey.value || row.key === dragKey.value) return
  const dragged = vars.value.find((v) => v.key === dragKey.value)
  if (!dragged || dragged.fileId !== row.fileId) return
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const above = e.clientY < rect.top + rect.height / 2
  if (!dropMark.value || dropMark.value.key !== row.key || dropMark.value.above !== above) {
    dropMark.value = { key: row.key, above }
  }
}

function onCardDragLeave(row: EnvVarEntry): void {
  if (dropMark.value && dropMark.value.key === row.key) dropMark.value = null
}

async function onCardDrop(_e: DragEvent, row: EnvVarEntry): Promise<void> {
  const mark = dropMark.value
  const key = dragKey.value
  onCardDragEnd()
  if (!mark || mark.key !== row.key || !key) return
  const dragged = vars.value.find((v) => v.key === key)
  if (!dragged || dragged.fileId !== row.fileId) return
  const sameFile = filteredVars.value.filter((v) => v.fileId === dragged.fileId)
  const target = sameFile.find((v) => v.key === row.key)
  if (!target || dragged === target) return
  sameFile.splice(sameFile.indexOf(dragged), 1)
  sameFile.splice(mark.above ? sameFile.indexOf(target) : sameFile.indexOf(target) + 1, 0, dragged)
  const result = await api.env.save({
    mode: 'reorder',
    fileId: dragged.fileId,
    orderedKeys: sameFile.map((v) => v.key)
  })
  if (!result.ok) {
    ElMessage.error(result.error ?? '排序失败')
    return
  }
  await load()
}

/** 对所有已配置变量批量执行 launchctl setenv，图形应用无需重启终端即可读取 */
async function onLaunchctlAll(): Promise<void> {
  const targets = vars.value.filter((v) => v.value)
  if (targets.length === 0) {
    ElMessage.info('没有可设置的变量')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将对 ${targets.length} 个变量批量执行 launchctl setenv，IDE、浏览器等图形应用立即生效；Mac 重启后失效（持久配置仍在 shell 文件中）。含 \$(...) 引用的变量按字面值设置。`,
      '全部立即生效',
      { confirmButtonText: '执行', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  launchingAll.value = true
  let ok = 0
  const failed: string[] = []
  for (const v of targets) {
    const r = await api.env.launchctl({ action: 'set', key: v.key, value: v.value })
    if (r.ok) ok++
    else failed.push(v.key)
  }
  launchingAll.value = false
  if (failed.length === 0) {
    ElMessage.success(`已设置 ${ok} 个变量，图形应用立即生效`)
  } else {
    ElMessage.warning(`成功 ${ok} 个，失败 ${failed.length} 个：${failed.join('、')}`)
  }
}

onMounted(async () => {
  const info = await api.app.info()
  isMac.value = info.platform === 'darwin'
  await load()
})
</script>

<style scoped>
/* ===== 页面骨架：锁定视口高度，整页无滚动条 ===== */
.ev-page {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  gap: 14px;
  animation: ev-page-in 0.3s ease both;
}

@keyframes ev-page-in {
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
.ev-hero {
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

.ev-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.ev-deco-1 {
  width: 260px;
  height: 260px;
  right: -60px;
  top: -140px;
}

.ev-deco-2 {
  width: 170px;
  height: 170px;
  right: 130px;
  bottom: -110px;
  border-color: rgba(255, 255, 255, 0.07);
}

.ev-hero-left {
  position: relative;
  min-width: 0;
}

.ev-hero-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.ev-hero-sub {
  margin-top: 7px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ev-hero-tip {
  margin-left: 10px;
  padding: 1px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 11px;
}

.ev-hero-tools {
  -webkit-app-region: no-drag;
  position: relative;
  display: flex;
  flex-wrap: nowrap;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
}

.ev-hero-search {
  display: flex;
  flex-shrink: 0;
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

.ev-hero-search:focus-within {
  background: rgba(255, 255, 255, 0.2);
}

.ev-hero-search input {
  border: none;
  outline: none;
  background: transparent;
  color: #fff;
  font-size: 13px;
  width: 170px;
}

.ev-hero-search input::placeholder {
  color: rgba(255, 255, 255, 0.55);
}

.ev-hero-bolt {
  white-space: nowrap;
  flex-shrink: 0;
  padding: 10px 15px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: background 0.15s ease, transform 0.15s ease;
}

.ev-hero-bolt:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.ev-hero-bolt:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.ev-hero-add {
  white-space: nowrap;
  flex-shrink: 0;
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

.ev-hero-add:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.24);
}

/* ===== 主体：文件导航 + 卡片网格 ===== */
.ev-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 14px;
}

.ev-rail {
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

.ev-rail-title {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 2px;
  font-weight: 600;
  padding: 0 10px;
  margin-bottom: 10px;
}

.ev-node {
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

.ev-node:hover {
  background: #f4f8fb;
}

.ev-node.active {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.22);
}

.ev-node.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 22%;
  bottom: 22%;
  width: 3px;
  border-radius: 2px;
  background: #2563eb;
}

.ev-node-ico {
  font-size: 14px;
  line-height: 1;
  flex-shrink: 0;
}

.ev-node-label {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ev-node.active .ev-node-label {
  color: #1d4ed8;
}

.ev-node-count {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 999px;
  padding: 1px 8px;
}

.ev-node.active .ev-node-count {
  color: #2563eb;
  background: rgba(255, 255, 255, 0.85);
}

.ev-rail-note {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
}

.ev-rail-note-title {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 6px;
}

.ev-rail-note-text {
  font-size: 12px;
  line-height: 1.75;
  color: #7d8a9e;
}

.ev-rail-legend {
  margin-top: auto;
  padding: 10px 10px 2px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ev-legend-item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: #94a3b8;
}

.ev-grip-demo {
  color: #b6c2d2;
  font-size: 13px;
  line-height: 1;
}

.ev-dot-demo {
  width: 14px;
  height: 8px;
  border-radius: 4px;
  background: #ecfdf5;
  border: 1px solid #d1fae5;
  flex-shrink: 0;
}

.ev-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ===== 筛选条 ===== */
.ev-filter-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-radius: 12px;
  background: #f0f9ff;
  border: 1px solid #dbeafe;
  margin-bottom: 12px;
}

.ev-filter-text {
  font-size: 12.5px;
  color: #1d4ed8;
  font-weight: 600;
}

.ev-filter-clear {
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
}

.ev-filter-clear:hover {
  color: #dc2626;
}

/* ===== 变量卡片网格（静默滚动：滚轮可用但不显示滚动条） ===== */
.ev-grid {
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

.ev-grid::-webkit-scrollbar {
  display: none;
}

.ev-card {
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

.ev-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 8px 22px rgba(37, 99, 235, 0.1);
}

.ev-card.dragging {
  opacity: 0.4;
}

.ev-card.off .ev-card-val,
.ev-card.off .ev-file-tag {
  opacity: 0.55;
}

.ev-card.off {
  background: #fbfcfe;
}

.ev-switch {
  flex-shrink: 0;
  margin-left: 2px;
}

.ev-hero-tip.off {
  background: rgba(148, 163, 184, 0.3);
}

.ev-card.drop-above {
  box-shadow: 0 -3px 0 0 #2563eb, 0 -8px 16px rgba(37, 99, 235, 0.18);
}

.ev-card.drop-below {
  box-shadow: 0 3px 0 0 #2563eb, 0 8px 16px rgba(37, 99, 235, 0.18);
}

.ev-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ev-grip {
  color: #c3cedd;
  font-size: 14px;
  line-height: 1;
  cursor: grab;
  user-select: none;
  flex-shrink: 0;
  transition: color 0.15s ease;
}

.ev-card:hover .ev-grip {
  color: #2563eb;
}

.ev-key {
  flex: 1;
  min-width: 0;
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ev-card-val {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  cursor: pointer;
  color: #475569;
  border-radius: 8px;
  padding: 3px 8px;
  background: #f1f5f9;
  border: 1px solid #e6edf5;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.ev-card-val:hover {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #dbeafe;
}

.ev-copy-ico {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.ev-card-val:hover .ev-copy-ico {
  opacity: 0.75;
}

.ev-val-text {
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ev-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ev-file-tag {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 999px;
  padding: 2px 10px;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ev-file-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.ev-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 999px;
  padding: 2px 9px;
  flex-shrink: 0;
}

.ev-badge::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.ev-badge.managed {
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.ev-badge.managed::before {
  background: #2563eb;
}

.ev-badge.manual {
  color: #64748b;
  background: #f1f5f9;
  border: 1px solid #e6edf5;
}

.ev-badge.manual::before {
  background: #94a3b8;
}

/* ===== 卡片操作按钮 ===== */
.ev-card-acts {
  display: inline-flex;
  gap: 6px;
  flex-shrink: 0;
}

.ev-act {
  padding: 4px 9px;
  border-radius: 8px;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;
  font-family: inherit;
}

.ev-act.edit {
  background: #eff6ff;
  color: #2563eb;
  border-color: #dbeafe;
}

.ev-act.edit:hover {
  background: #dbeafe;
}

.ev-act.live {
  background: #ecfdf5;
  color: #059669;
  border-color: #d1fae5;
}

.ev-act.live:hover {
  background: #d1fae5;
}

.ev-act.del {
  background: #fef2f2;
  color: #dc2626;
  border-color: #fee2e2;
}

.ev-act.del:hover {
  background: #fee2e2;
}

/* ===== 空状态 ===== */
.ev-empty {
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

.ev-empty-ico {
  font-size: 44px;
  margin-bottom: 12px;
  opacity: 0.85;
}

.ev-empty-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #334155;
}

.ev-empty-sub {
  margin-top: 6px;
  font-size: 12.5px;
  color: #94a3b8;
}

/* 响应式：窄窗口时收起文件栏 */
@media (max-width: 900px) {
  .ev-rail {
    display: none;
  }
}
</style>


<style>
.ev-dlg .el-dialog {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(15, 30, 48, 0.3);
  width: min(620px, 94vw) !important;
}

.ev-dlg .el-dialog__header,
.ev-dlg .el-dialog__body,
.ev-dlg .el-dialog__footer {
  padding: 0;
}

/* 覆盖全局弹窗拉伸规则：高度贴合内容，不再被拉到 84vh 留白 */
.ev-dlg .el-dialog {
  align-self: center;
  height: auto;
}

.ev-dlg .el-dialog__body {
  flex: 0 1 auto !important;
}

.evd-header {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
}

.evd-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.evd-deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -120px;
}

.evd-deco-2 {
  width: 140px;
  height: 140px;
  right: 110px;
  bottom: -90px;
  border-color: rgba(255, 255, 255, 0.07);
}

.evd-header-main {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.evd-header-icon {
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

.evd-title {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.evd-subtitle {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.evd-close {
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

.evd-close:hover {
  background: rgba(255, 255, 255, 0.26);
}

.evd-body {
  padding: 20px 24px 10px;
  background: #fff;
}

.evd-field {
  margin-bottom: 14px;
}

.evd-field:last-of-type {
  margin-bottom: 4px;
}

.evd-label {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  margin-bottom: 6px;
}

.evd-req {
  color: #dc2626;
}

.evd-input {
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

.evd-input.mono {
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 12.5px;
}

.evd-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.evd-input:disabled {
  background: #f8fafc;
  color: #64748b;
  cursor: not-allowed;
}

.evd-textarea {
  resize: none;
  min-height: 76px;
  line-height: 1.7;
}

/* 写入文件选择卡片 */
.evd-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.evd-file {
  flex: 1 1 calc((100% - 32px) / 3);
  min-width: 0;
}

.evd-file {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  box-sizing: border-box;
  padding: 9px 11px;
  border: 1.5px solid #e4eaf2;
  border-radius: 11px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
  font-family: inherit;
}

.evd-file:hover {
  border-color: #93c5fd;
  background: #f6faff;
}

.evd-file.active {
  border-color: #2563eb;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.14);
}

.evd-file-ico {
  font-size: 17px;
  line-height: 1;
  flex-shrink: 0;
}

.evd-file-body {
  min-width: 0;
  flex: 1;
}

.evd-file-name {
  display: block;
  font-family: 'SF Mono', ui-monospace, Menlo, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #1f2d3d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.evd-file.active .evd-file-name {
  color: #1d4ed8;
}

.evd-file-tip {
  display: block;
  margin-top: 2px;
  font-size: 10.5px;
  color: #7d8a9e;
}

.evd-file.active .evd-file-tip {
  color: #2563eb;
}

.evd-file-check {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.evd-hint {
  margin: 4px 0 8px;
  font-size: 11.5px;
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  border-radius: 7px;
  padding: 6px 10px;
}

.evd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
}

.evd-btn {
  padding: 9px 22px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  border: 1px solid transparent;
  font-family: inherit;
}

.evd-btn.ghost {
  background: #fff;
  border-color: #dbe2ea;
  color: #475569;
}

.evd-btn.ghost:hover {
  border-color: #b9c4d2;
  color: #1f2d3d;
}

.evd-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.evd-btn.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

.evd-btn.primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}
</style>
