<template>
  <div class="sec-page">
    <!-- ===== Hero 头部 ===== -->
    <div class="sec-hero">
      <div class="sec-deco sec-deco-1"></div>
      <div class="sec-deco sec-deco-2"></div>
      <div class="sec-hero-left">
        <div class="sec-hero-name">🔑 密钥管理</div>
        <div class="sec-hero-sub">
          {{ configs.length }} 条密钥 · {{ c1Options.length }} 个归宿 · {{ c2Options.length }} 个环境
          <span class="sec-hero-tip">默认密钥不可删除 · 凭据仅存本机不入仓库</span>
        </div>
        <div class="sec-hero-chips">
          <span v-if="typeCount('Docker')" class="hero-chip docker">🐳 Docker × {{ typeCount('Docker') }}</span>
          <span v-if="typeCount('Helm')" class="hero-chip helm">⎈ Helm × {{ typeCount('Helm') }}</span>
          <span v-if="typeCount('Linux')" class="hero-chip linux">🐧 Linux × {{ typeCount('Linux') }}</span>
          <span v-if="typeCount('')" class="hero-chip plain">🔑 未分类 × {{ typeCount('') }}</span>
          <span v-if="authCount('key')" class="hero-chip key">🗝 密钥 × {{ authCount('key') }}</span>
        </div>
      </div>
      <div class="sec-hero-tools">
        <div class="sec-hero-search">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input v-model="keyword" placeholder="搜索名称 / 地址 / 账号" />
        </div>
        <button class="sec-hero-add" @click="openAdd">＋ 添加密钥</button>
      </div>
    </div>

    <!-- ===== 左侧分类树 + 分组卡片 ===== -->
    <div class="sec-layout">
      <div class="sec-aside">
        <div class="sec-aside-title">分类导航</div>
        <div class="cat-node cat-all" :class="{ active: !hasFilter }" @click="clearFilter">
          <span class="cat-label">全部密钥</span>
          <span class="cat-count">{{ configs.length }}</span>
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
            <span class="l1-dot" :style="{ background: g.color }"></span>
            <span class="cat-label">{{ g.label }}</span>
            <span class="cat-count">{{ g.count }}</span>
          </div>
          <div v-show="!collapsed.has(g.label)" class="cat-sub">
            <div
              v-for="env in g.children"
              :key="env.label"
              class="cat-node cat-l2"
              :class="{ active: filter.c1 === g.label && filter.c2 === env.label }"
              @click="selectNode(g.label, env.label)"
            >
              <span class="l2-dot"></span>
              <span class="cat-label">{{ env.label }}</span>
              <span class="cat-count">{{ env.count }}</span>
            </div>
          </div>
        </template>
      </div>

      <div class="sec-main">
        <div v-if="hasFilter || keyword" class="sec-filter-bar">
          <span class="filter-text">
            {{ [filter.c1, filter.c2].filter(Boolean).join(' / ') || '全部' }}
            <template v-if="keyword">· 关键词「{{ keyword }}」</template>
          </span>
          <span class="filter-clear" @click="clearAll">清除筛选 ✕</span>
        </div>

    <!-- ===== 按一级归宿分组的密钥卡片 ===== -->
    <div class="sec-group" v-for="g in grouped" :key="g.label">
      <div class="sec-group-header">
        <span class="sec-group-bar"></span>
        <span class="sec-group-title">{{ g.label }}</span>
        <span class="sec-group-count">{{ g.items.length }} 条</span>
      </div>
      <div class="sec-grid">
        <div
          class="sec-card"
          v-for="row in g.items"
          :key="row.id"
          :style="{ '--accent': typeAccent(row.type) }"
        >
          <div class="sec-card-head">
            <div class="sec-type-icon" :style="typeIconStyle(row.type)">{{ typeIcon(row.type) }}</div>
            <div class="sec-title-block">
              <div class="sec-title">
                {{ row.name }}
                <el-tag v-if="row.isDefault" size="small" type="warning" effect="light" round>默认</el-tag>
                <el-tag v-if="row.type" size="small" :type="typeTag(row.type)" effect="plain" round>{{ row.type }}</el-tag>
                <el-tag size="small" :type="row.authType === 'key' ? 'danger' : 'info'" effect="plain" round>
                  {{ row.authType === 'key' ? '密钥' : '账密' }}
                </el-tag>
              </div>
              <div class="sec-cats">
                <el-tag size="small" effect="plain" round>{{ row.category1 }}</el-tag>
                <el-tag size="small" type="warning" effect="plain" round>{{ row.category2 }}</el-tag>
              </div>
            </div>
          </div>

          <div class="sec-addr mono-cell" v-if="row.address" :title="row.address">{{ row.address }}</div>

          <div class="sec-creds">
            <div
              v-if="row.authType !== 'key'"
              class="sec-cred"
              :class="{ clickable: !!row.username }"
              :title="row.username ? '点击复制账号' : ''"
              @click="row.username && copyText(row.username)"
            >
              <span class="sec-cred-label">👤</span>
              <span class="sec-cred-value mono-cell">{{ row.username || '—' }}</span>
            </div>
            <div
              class="sec-cred"
              :class="{ clickable: !!row.password }"
              :title="row.password ? '点击复制密码' : ''"
              @click="row.password && copyText(row.password)"
            >
              <span class="sec-cred-label">🔐</span>
              <span class="sec-cred-value mono-cell">{{ row.password ? '••••••••' : '—' }}</span>
              <span v-if="row.password" class="sec-cred-copy">复制</span>
            </div>
          </div>

          <div class="sec-actions">
            <el-tooltip
              v-if="row.type === 'Docker' || row.type === 'Helm'"
              :disabled="toolReady(row)"
              :content="toolTip(row)"
              placement="top"
            >
              <span>
                <el-button
                  size="small"
                  type="success"
                  plain
                  :disabled="!toolReady(row)"
                  :loading="loggingId === row.id"
                  @click="onLogin(row)"
                >
                  一键登录
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip :disabled="toolReady(row)" :content="toolTip(row)" placement="top">
              <span>
                <el-button size="small" :disabled="!toolReady(row)" @click="openEdit(row)">编辑</el-button>
              </span>
            </el-tooltip>
            <el-tooltip :disabled="!row.isDefault" content="默认密钥不可删除" placement="top">
              <span>
                <el-button size="small" type="danger" plain :disabled="row.isDefault" @click="onRemove(row)">删除</el-button>
              </span>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>

        <el-empty v-if="filtered.length === 0 && configs.length > 0" description="没有符合条件的密钥" />
        <el-empty v-if="configs.length === 0" description="暂无密钥，点击右上角添加" />
      </div>
    </div>

    <!-- ===== 添加 / 编辑 ===== -->
    <el-dialog
      v-model="dialogVisible"
      width="640px"
      append-to-body
      modal-class="sk-dlg"
      :show-close="false"
    >
      <template #header>
        <div class="sk-header">
          <div class="sk-deco sk-deco-1"></div>
          <div class="sk-deco sk-deco-2"></div>
          <div class="sk-header-main">
            <div class="sk-header-icon">{{ isEdit ? '✏️' : '🔑' }}</div>
            <div>
              <div class="sk-title">{{ isEdit ? '编辑密钥' : '添加密钥' }}</div>
              <div class="sk-subtitle">密码经系统钥匙串加密存储 · 仅保存在本机，不进入代码仓库</div>
            </div>
          </div>
          <button class="sk-close" type="button" @click="dialogVisible = false">✕</button>
        </div>
      </template>

      <div class="sk-body">
        <!-- 基本信息 -->
        <div class="sk-sec">
          <span class="sk-sec-ico">📋</span>
          <span class="sk-sec-text">基本信息</span>
          <span class="sk-sec-line"></span>
        </div>
        <div class="sk-field">
          <div class="sk-label">名称 <span class="sk-req">*</span></div>
          <el-input v-model="form.name" placeholder="如 测试环境镜像仓库 / 生产 Helm 仓库" />
        </div>
        <div class="sk-cols">
          <div class="sk-field">
            <div class="sk-label">类型</div>
            <el-select v-model="form.type" placeholder="选择类型">
              <el-option label="无" value="" />
              <el-option label="Git 🌿" value="Git" />
              <el-option label="Docker 🐳" value="Docker" />
              <el-option label="Helm ⎈" value="Helm" />
              <el-option label="Linux 🐧" value="Linux" />
            </el-select>
            <div v-if="typeHint" class="sk-hint">{{ typeHint }}</div>
          </div>
          <div class="sk-field">
            <div class="sk-label">一级归宿</div>
            <el-input v-model="form.category1" placeholder="我的" />
          </div>
          <div class="sk-field">
            <div class="sk-label">二级环境</div>
            <el-input v-model="form.category2" placeholder="默认环境" />
          </div>
        </div>

        <!-- 地址与凭据 -->
        <div class="sk-sec sk-sec-cred">
          <span class="sk-sec-ico">🔐</span>
          <span class="sk-sec-text">地址与凭据</span>
          <span class="sk-sec-line"></span>
        </div>
        <div class="sk-field">
          <div class="sk-label">地址</div>
          <el-input v-model="form.address" placeholder="镜像仓库 / Helm 仓库地址（可留空）" class="mono-cell" />
        </div>
        <div class="sk-field">
          <div class="sk-label">密钥类型 <span class="sk-req">*</span></div>
          <el-select v-model="form.authType" placeholder="选择密钥类型">
            <el-option label="账密（账号 + 密码）" value="password" />
            <el-option label="密钥（单一 Token）" value="key" />
          </el-select>
        </div>
        <div :class="form.authType === 'key' ? 'sk-field-wide' : 'sk-cols two'">
          <div class="sk-field" v-if="form.authType !== 'key'">
            <div class="sk-label">账号</div>
            <el-input v-model="form.username" placeholder="登录账号（可留空）" class="mono-cell" />
          </div>
          <div class="sk-field">
            <div class="sk-label">{{ form.authType === 'key' ? '密钥' : '密码' }} <span class="sk-req">*</span></div>
            <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="form.authType === 'key' ? 'Token / 密钥内容' : '密码 / Token（可留空）'"
              class="mono-cell"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="sk-footer">
          <button class="sk-btn ghost" type="button" @click="dialogVisible = false">取消</button>
          <button class="sk-btn primary" type="button" :disabled="saving" @click="onSave">
            {{ saving ? '保存中…' : isEdit ? '保存修改' : '添加密钥' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, type ConfigEntry, type ConfigType, type AuthType, type ToolAvailability } from '../api'

const configs = ref<ConfigEntry[]>([])
const tools = ref<ToolAvailability>({ git: false, docker: false, helm: false })
const keyword = ref('')
const collapsed = ref(new Set<string>())
const filter = reactive<{ c1?: string; c2?: string }>({})
const loggingId = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const emptyForm = (): {
  id: string
  name: string
  type: ConfigType
  authType: AuthType
  category1: string
  category2: string
  address: string
  username: string
  password: string
} => ({
  id: '',
  name: '',
  type: '',
  authType: 'password',
  category1: '我的',
  category2: '默认环境',
  address: '',
  username: '',
  password: ''
})
const form = ref(emptyForm())

const c1Options = computed(() => [...new Set(configs.value.map((c) => c.category1).filter(Boolean))])
const c2Options = computed(() => [...new Set(configs.value.map((c) => c.category2).filter(Boolean))])

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return configs.value.filter((c) => {
    if (filter.c1 && c.category1 !== filter.c1) return false
    if (filter.c2 && c.category2 !== filter.c2) return false
    if (!k) return true
    return (
      c.name.toLowerCase().includes(k) ||
      c.address.toLowerCase().includes(k) ||
      c.username.toLowerCase().includes(k)
    )
  })
})

const hasFilter = computed(() => !!(filter.c1 || filter.c2))

function clearFilter(): void {
  filter.c1 = undefined
  filter.c2 = undefined
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

/** 点击一级节点：切换展开并筛选该归宿 */
function onLevel1Click(label: string): void {
  toggleCollapse(label)
  filter.c1 = label
  filter.c2 = undefined
}

function selectNode(c1: string, c2: string): void {
  filter.c1 = c1
  filter.c2 = c2
}

function typeCount(t: ConfigType): number {
  return configs.value.filter((c) => c.type === t).length
}

function authCount(a: 'password' | 'key'): number {
  return configs.value.filter((c) => (c.authType ?? 'password') === a).length
}

/** 归宿→环境 二级分类树 */
const categoryTree = computed(() => {
  const root = new Map<string, Map<string, number>>()
  for (const c of configs.value) {
    if (!root.has(c.category1)) root.set(c.category1, new Map())
    const envs = root.get(c.category1)!
    envs.set(c.category2, (envs.get(c.category2) ?? 0) + 1)
  }
  return [...root.entries()].map(([label, envs]) => ({
    label,
    color: colorOf(label),
    count: [...envs.values()].reduce((s, n) => s + n, 0),
    children: [...envs.entries()].map(([elabel, count]) => ({ label: elabel, count }))
  }))
})

/** 归宿颜色（稳定哈希取色） */
function colorOf(seed: string): string {
  const colors = ['#2563eb', '#7c3aed', '#db2777', '#dc2626', '#ea580c', '#059669', '#0891b2']
  let hash = 0
  for (const ch of seed || '?') hash = (hash * 31 + ch.charCodeAt(0)) % 997
  return colors[hash % colors.length]
}

/** 按一级归宿分组展示（保持数据顺序） */
const grouped = computed(() => {
  const map = new Map<string, ConfigEntry[]>()
  for (const c of filtered.value) {
    if (!map.has(c.category1)) map.set(c.category1, [])
    map.get(c.category1)!.push(c)
  }
  return [...map.entries()].map(([label, items]) => ({ label, items }))
})

/** 类型图标与配色 */
const TYPE_META: Record<string, { icon: string; grad: [string, string] }> = {
  Git: { icon: '🌿', grad: ['#34d399', '#059669'] },
  Docker: { icon: '🐳', grad: ['#60a5fa', '#2563eb'] },
  Helm: { icon: '⎈', grad: ['#a78bfa', '#7c3aed'] },
  Linux: { icon: '🐧', grad: ['#333333', '#1a1a2e'] },
}

function typeIcon(t: ConfigType): string {
  return TYPE_META[t]?.icon ?? '🔑'
}

function typeAccent(t: ConfigType): string {
  const meta = TYPE_META[t]
  if (!meta) return 'linear-gradient(90deg, #cbd5e1, #94a3b8)'
  return `linear-gradient(90deg, ${meta.grad[0]}, ${meta.grad[1]})`
}

function typeIconStyle(t: ConfigType): Record<string, string> {
  const meta = TYPE_META[t]
  if (!meta) return { background: 'linear-gradient(135deg, #cbd5e1, #94a3b8)' }
  return { background: `linear-gradient(135deg, ${meta.grad[0]}, ${meta.grad[1]})` }
}

function typeTag(t: ConfigType): 'success' | 'primary' | 'warning' {
  if (t === 'Git') return 'success'
  if (t === 'Docker') return 'primary'
  return t === 'Helm' ? 'warning' : 'info'
}

/** 表单类型联动提示 */
const typeHint = computed(() => {
  if (form.value.type === 'Git') return '可用于项目控制台的代码拉取'
  if (form.value.type === 'Docker') return '保存后可「一键登录」执行 docker login'
  if (form.value.type === 'Helm') return '保存后可「一键登录」执行 helm registry login'
  if (form.value.type === 'Linux') return 'Linux 服务器 SSH 凭据（账号 / 密码或密钥）'
  return ''
})

/** 类型对应的 CLI 已安装才允许修改/操作（无类型密钥不受限） */
function toolReady(row: ConfigEntry): boolean {
  if (row.type === 'Git') return tools.value.git
  if (row.type === 'Docker') return tools.value.docker
  if (row.type === 'Helm') return tools.value.helm
  if (row.type === 'Linux') return tools.value.linux
  return true
}

function toolTip(row: ConfigEntry): string {
  const bin = row.type === 'Git' ? 'git' : row.type === 'Docker' ? 'docker' : row.type === 'Helm' ? 'helm' : 'ssh'
  return `本机未安装 ${bin}，不允许修改`
}

async function load(): Promise<void> {
  const r = await api.configs.list()
  if (r.ok && r.data) {
    configs.value = r.data.configs
    tools.value = r.data.tools
  }
}

async function copyText(text: string): Promise<void> {
  await api.util.copy(text)
  ElMessage.success('已复制到剪贴板')
}

function openAdd(): void {
  isEdit.value = false
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(row: ConfigEntry): void {
  isEdit.value = true
  form.value = {
    id: row.id,
    name: row.name,
    type: row.type,
    authType: row.authType ?? 'password',
    category1: row.category1,
    category2: row.category2,
    address: row.address,
    username: row.username,
    password: row.password
  }
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  if (!form.value.authType) {
    ElMessage.warning('请选择密钥类型')
    return
  }
  saving.value = true
  try {
    const r = await api.configs.save({
      id: form.value.id || undefined,
      name: form.value.name.trim(),
      type: form.value.type,
      authType: form.value.authType,
      category1: form.value.category1.trim() || '我的',
      category2: form.value.category2.trim() || '默认环境',
      address: form.value.address.trim(),
      username: form.value.username,
      password: form.value.password
    })
    if (!r.ok) {
      ElMessage.error(r.error ?? '保存失败')
      return
    }
    ElMessage.success(isEdit.value ? '密钥已更新' : '密钥已添加')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onRemove(row: ConfigEntry): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除密钥「${row.name}」吗？`, '删除密钥', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  const r = await api.configs.remove(row.id)
  if (!r.ok) {
    ElMessage.error(r.error ?? '删除失败')
    return
  }
  ElMessage.success('已删除')
  await load()
}

/** Docker / Helm：一键登录 */
async function onLogin(row: ConfigEntry): Promise<void> {
  loggingId.value = row.id
  try {
    const r = await api.configs.login(row.id)
    if (!r.ok) {
      ElMessage.error(r.error ?? '登录失败')
      return
    }
    ElMessage.success(r.data || '登录成功')
  } finally {
    loggingId.value = ''
  }
}

onMounted(load)
</script>

<style scoped>
.sec-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ===== Hero ===== */
.sec-hero {
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

.sec-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.sec-deco-1 {
  width: 300px;
  height: 300px;
  right: -70px;
  top: -160px;
}

.sec-deco-2 {
  width: 190px;
  height: 190px;
  right: 150px;
  bottom: -120px;
  border-color: rgba(255, 255, 255, 0.07);
}

.sec-hero-left {
  position: relative;
  min-width: 0;
}

.sec-hero-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.sec-hero-sub {
  margin-top: 7px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.sec-hero-tip {
  margin-left: 10px;
  padding: 1px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 11px;
}

.sec-hero-chips {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.hero-chip {
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(6px);
}

.hero-chip.docker {
  background: rgba(59, 130, 246, 0.3);
}

.hero-chip.linux {
  color: #1e293b;
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.hero-chip.helm {
  background: rgba(167, 139, 250, 0.3);
}

.hero-chip.key {
  background: rgba(244, 114, 182, 0.3);
}

.sec-hero-tools {
  -webkit-app-region: no-drag;
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
}

.sec-hero-search {
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

.sec-hero-search:focus-within {
  background: rgba(255, 255, 255, 0.2);
}

.sec-hero-search input {
  border: none;
  outline: none;
  background: transparent;
  color: #fff;
  font-size: 13px;
  width: 200px;
}

.sec-hero-search input::placeholder {
  color: rgba(255, 255, 255, 0.55);
}

.sec-hero-add {
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

.sec-hero-add:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.24);
}

/* ===== 左侧分类树 + 主区布局 ===== */
.sec-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.sec-aside {
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

.sec-aside-title {
  font-size: 12px;
  color: #8a94a6;
  padding: 2px 10px 10px;
  letter-spacing: 1px;
}

.sec-main {
  flex: 1;
  min-width: 0;
}

/* 筛选进行中提示条 */
.sec-filter-bar {
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

/* 分类树节点 */
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

/* ===== 分组 ===== */
.sec-group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0 12px;
}

.sec-group-bar {
  width: 4px;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
}

.sec-group-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #1f2d3d;
}

.sec-group-count {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 9px;
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
}

/* ===== 卡片 ===== */
.sec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 14px;
}

.sec-card {
  position: relative;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.sec-card {
  animation: sec-card-in 0.28s ease both;
}

.sec-card:nth-child(2) { animation-delay: 0.04s; }
.sec-card:nth-child(3) { animation-delay: 0.08s; }
.sec-card:nth-child(4) { animation-delay: 0.12s; }
.sec-card:nth-child(5) { animation-delay: 0.16s; }
.sec-card:nth-child(6) { animation-delay: 0.2s; }

@keyframes sec-card-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sec-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--accent, linear-gradient(90deg, #3b82f6, #22d3ee));
  opacity: 0.45;
  transition: opacity 0.2s ease;
}

.sec-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px rgba(16, 24, 40, 0.12);
}

.sec-card:hover::before {
  opacity: 1;
}

.sec-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.sec-type-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  box-shadow: 0 4px 10px rgba(16, 24, 40, 0.12);
}

.sec-title-block {
  min-width: 0;
  flex: 1;
}

.sec-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f1e30;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.sec-cats {
  margin-top: 5px;
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.sec-addr {
  font-size: 11.5px;
  color: #8a94a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 6px 10px;
  background: #f8fafc;
  border-radius: 8px;
}

/* 凭据行 */
.sec-creds {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sec-cred {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f8fafc;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 12px;
  color: #1f2d3d;
  transition: background 0.15s, border-color 0.15s;
}

.sec-cred.clickable {
  cursor: pointer;
}

.sec-cred.clickable:hover {
  background: #eff6ff;
  border-color: #dbeafe;
}

.sec-cred-label {
  font-size: 13px;
}

.sec-cred-value {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sec-cred-copy {
  color: #2563eb;
  font-size: 11px;
  flex-shrink: 0;
}

/* 操作区 */
.sec-actions {
  display: flex;
  gap: 4px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}
</style>

<style>
/* ===== 添加/编辑密钥弹窗（append-to-body 挂在 body 下，需全局样式；sk-dlg 为 modal-class） ===== */
.sk-dlg .el-dialog {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(15, 30, 48, 0.3);
  width: min(640px, 94vw) !important;
}

.sk-dlg .el-dialog__header,
.sk-dlg .el-dialog__body,
.sk-dlg .el-dialog__footer {
  padding: 0;
}

/* ---- 头部横幅 ---- */
.sk-header {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
}

.sk-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.sk-deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -120px;
}

.sk-deco-2 {
  width: 140px;
  height: 140px;
  right: 110px;
  bottom: -90px;
  border-color: rgba(255, 255, 255, 0.07);
}

.sk-header-main {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.sk-header-icon {
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

.sk-title {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.sk-subtitle {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.sk-close {
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

.sk-close:hover {
  background: rgba(255, 255, 255, 0.26);
}

/* ---- 表单区 ---- */
.sk-body {
  padding: 20px 24px 8px;
  background: #fff;
}

.sk-field {
  margin-bottom: 13px;
}

.sk-field:last-child {
  margin-bottom: 6px;
}

.sk-label {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  margin-bottom: 6px;
}

.sk-req {
  color: #dc2626;
}

.sk-cols {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 14px;
}

.sk-cols.two {
  grid-template-columns: 1fr 1fr;
}

/* 密钥模式:凭据字段通栏,与地址输入框等长 */
.sk-field-wide {
  display: block;
}

/* 分区小标题 */
.sk-sec {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 11px;
}

.sk-sec-cred {
  margin-top: 10px;
}

.sk-sec-ico {
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

.sk-sec-text {
  font-size: 11.5px;
  color: #64748b;
  letter-spacing: 1px;
  font-weight: 600;
  flex-shrink: 0;
}

.sk-sec-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #e4eaf2, transparent);
}

/* 类型联动提示 */
.sk-hint {
  margin-top: 6px;
  font-size: 11.5px;
  color: #2563eb;
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  border-radius: 7px;
  padding: 4px 8px;
}

.sk-dlg .el-select {
  width: 100%;
}

/* 输入控件圆角精修 */
.sk-dlg .el-input__wrapper,
.sk-dlg .el-select__wrapper {
  border-radius: 10px;
}

/* ---- 页脚操作条 ---- */
.sk-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
}

.sk-btn {
  padding: 9px 22px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  border: 1px solid transparent;
}

.sk-btn.ghost {
  background: #fff;
  border-color: #dbe2ea;
  color: #475569;
}

.sk-btn.ghost:hover {
  border-color: #b9c4d2;
  color: #1f2d3d;
}

.sk-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.sk-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

.sk-btn.primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

@media (max-width: 640px) {
  .sk-cols,
  .sk-cols.two {
    grid-template-columns: 1fr;
  }
}
</style>
