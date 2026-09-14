<template>
  <div class="dashboard" v-loading="loadingStatic">
    <!-- ===== Hero：设备概览 + 公网 IP ===== -->
    <div class="hero">
      <div class="hero-deco hero-deco-1"></div>
      <div class="hero-deco hero-deco-2"></div>
      <div class="hero-main">
        <div class="hero-name">{{ info?.computerName || '...' }}</div>
        <div class="hero-time mono-text">
          <span class="hero-date">{{ now.date }} {{ now.weekday }}</span>
          <span class="hero-clock">{{ now.clock }}</span>
        </div>
        <div class="hero-host">{{ info?.hostname }} · macOS {{ info?.kernel }}</div>
        <div class="hero-chips">
          <span class="chip" v-if="info?.modelName">{{ info.modelName }}</span>
          <span class="chip chip-strong" v-if="info?.chip">{{ info.chip }}</span>
          <span class="chip" v-if="mainGpu?.cores">{{ mainGpu.cores }} 核 GPU</span>
          <span class="chip">{{ info?.cpuPhysicalCores }} 核 CPU</span>
          <span class="chip">{{ formatBytes(info?.memoryTotal ?? 0, 0) }} 内存</span>
          <span class="chip">{{ info?.arch }}</span>
          <span class="chip">已运行 {{ formatUptime(dyn?.uptime ?? 0) }}</span>
        </div>
      </div>
      <div class="hero-ip glass">
        <div class="ip-label">
          公网 IP
          <span v-if="publicIP" class="ip-source">via {{ publicIP.source }}</span>
        </div>
        <div class="ip-value mono-text">
          <template v-if="publicIP">{{ publicIP.ip }}</template>
          <template v-else-if="publicIPLoading"><span class="ip-loading">探测中…</span></template>
          <template v-else-if="publicIPError">
            <span class="ip-error">{{ publicIPError }}</span>
          </template>
          <template v-else>—</template>
        </div>
        <div class="ip-location" v-if="publicIP?.location">{{ publicIP.location }}</div>
        <div class="ip-refresh" @click="loadPublicIP" title="重新探测">
          <span class="refresh-icon" :class="{ spinning: publicIPLoading }">⟳</span>
        </div>
      </div>
    </div>

    <!-- ===== 实时指标（2×2 大卡） ===== -->
    <el-row :gutter="16" class="metric-row">
      <!-- CPU -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-head">
            <div class="card-icon icon-cpu">CPU</div>
            <div class="card-title">处理器</div>
            <div class="card-sub">{{ info?.cpuPhysicalCores }} 物理核 · {{ info?.cpuLogicalCores }} 逻辑核</div>
          </div>
          <div class="card-body">
            <RingProgress :value="dyn?.cpu.usage ?? 0" :size="132" :stroke="11" grad-id="cpuGrad" />
            <div class="metric-detail">
              <div class="detail-model" :title="info?.chip">{{ info?.chip || info?.cpuModel }}</div>
              <div class="core-grid">
                <div class="core-cell" v-for="(c, i) in dyn?.cpu.cores ?? []" :key="i">
                  <div class="core-bar">
                    <div class="core-bar-fill" :class="usageLevel(c)" :style="{ height: c + '%' }"></div>
                  </div>
                </div>
              </div>
              <div class="core-hint">每核实时占用</div>
            </div>
          </div>
          <div class="trend">
            <div class="trend-head">
              <span class="trend-legend"><i class="lg-dot" :style="{ background: '#2563eb' }"></i>近 5 分钟</span>
              <span class="trend-peak">峰值 <b>{{ peakOf(cpuHistory) }}%</b></span>
            </div>
            <MiniTrend :values="cpuHistory" :times="trendTimes" :theme="['#60a5fa', '#2563eb']" />
          </div>
        </div>
      </el-col>

      <!-- GPU -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-head">
            <div class="card-icon icon-gpu">GPU</div>
            <div class="card-title">图形处理器</div>
            <div class="card-sub">{{ mainGpu?.cores ? mainGpu.cores + ' 核心' : '' }}</div>
          </div>
          <div class="card-body">
            <RingProgress
              :value="mainGpuUsage ?? 0"
              :size="132"
              :stroke="11"
              grad-id="gpuGrad"
              :unsupported="mainGpuUsage === null"
              :theme="['#f472b6', '#db2777']"
            />
            <div class="metric-detail">
              <div class="detail-model" :title="mainGpu?.name">{{ mainGpu?.name || '未识别' }}</div>
              <div class="kv-row" v-if="mainGpu?.cores"><span>核心数</span><b>{{ mainGpu.cores }}</b></div>
              <div class="kv-row" v-if="mainGpu?.metal"><span>图形 API</span><b>{{ mainGpu.metal }}</b></div>
              <div class="kv-row" v-if="mainGpu?.vram"><span>显存</span><b>{{ formatBytes(mainGpu.vram, 0) }}</b></div>
              <div class="kv-row" v-if="mainGpu?.displays?.length">
                <span>连接显示器</span><b>{{ mainGpu.displays.length }} 台</b>
              </div>
            </div>
          </div>
          <div class="trend" v-if="gpuHasData">
            <div class="trend-head">
              <span class="trend-legend"><i class="lg-dot" :style="{ background: '#db2777' }"></i>近 5 分钟</span>
              <span class="trend-peak">峰值 <b>{{ peakOf(gpuHistory) }}%</b></span>
            </div>
            <MiniTrend :values="gpuHistory" :times="trendTimes" :theme="['#f472b6', '#db2777']" />
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="metric-row">
      <!-- 内存 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-head">
            <div class="card-icon icon-mem">MEM</div>
            <div class="card-title">内存</div>
            <div class="card-sub">共 {{ formatBytes(info?.memoryTotal ?? 0, 0) }}</div>
          </div>
          <div class="card-body">
            <RingProgress
              :value="dyn?.mem.usage ?? 0"
              :size="132"
              :stroke="11"
              grad-id="memGrad"
              :theme="['#a78bfa', '#7c3aed']"
            />
            <div class="metric-detail">
              <div class="kv-row"><span>已使用</span><b>{{ formatBytes(dyn?.mem.used ?? 0) }}</b></div>
              <div class="kv-row"><span>可用</span><b>{{ formatBytes(dyn?.mem.available ?? 0) }}</b></div>
              <div class="kv-row"><span>总量</span><b>{{ formatBytes(dyn?.mem.total ?? 0, 0) }}</b></div>
            </div>
          </div>
          <div class="trend">
            <div class="trend-head">
              <span class="trend-legend"><i class="lg-dot" :style="{ background: '#7c3aed' }"></i>近 5 分钟</span>
              <span class="trend-peak">峰值 <b>{{ peakOf(memHistory) }}%</b></span>
            </div>
            <MiniTrend :values="memHistory" :times="trendTimes" :theme="['#a78bfa', '#7c3aed']" />
          </div>
        </div>
      </el-col>

      <!-- 磁盘 -->
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-head">
            <div class="card-icon icon-disk">DISK</div>
            <div class="card-title">磁盘</div>
            <div class="card-sub">{{ dyn?.disks.length ?? 0 }} 个卷</div>
          </div>
          <div class="card-body">
            <RingProgress
              :value="mainDisk?.usage ?? 0"
              :size="132"
              :stroke="11"
              grad-id="diskGrad"
              :theme="['#34d399', '#059669']"
            />
            <div class="metric-detail disk-detail">
              <div class="disk-item" v-for="d in dyn?.disks ?? []" :key="d.mount">
                <div class="disk-line">
                  <span class="disk-mount mono-text">{{ d.mount }}</span>
                  <span class="disk-size mono-text">{{ formatBytes(d.used, 0) }} / {{ formatBytes(d.total, 0) }}</span>
                </div>
                <div class="disk-bar">
                  <div class="disk-bar-fill" :class="usageLevel(d.usage)" :style="{ width: d.usage + '%' }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ===== 网络 ===== -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <div class="card net-card">
          <div class="card-head">
            <div class="card-icon icon-net">LAN</div>
            <div class="card-title">内网地址</div>
            <div class="card-sub">{{ network?.interfaces.length ?? 0 }} 个活动网卡</div>
          </div>
          <div class="net-list">
            <div
              class="net-item"
              v-for="n in network?.interfaces ?? []"
              :key="n.name + n.ip"
              :class="{ active: n.ip === network?.primaryIP }"
            >
              <div class="net-badge mono-text">{{ n.name }}</div>
              <div class="net-info">
                <div class="net-ip mono-text">{{ n.ip }}</div>
                <div class="net-mac mono-text">{{ n.mac }}</div>
              </div>
              <span v-if="n.ip === network?.primaryIP" class="net-primary-tag">主连接</span>
            </div>
            <el-empty v-if="network && network.interfaces.length === 0" description="未检测到活动网卡" :image-size="60" />
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :md="10">
        <div class="card spec-card">
          <div class="card-head">
            <div class="card-icon icon-spec">SYS</div>
            <div class="card-title">设备信息</div>
          </div>
          <div class="spec-list">
            <div class="spec-row" v-for="s in specRows" :key="s.label">
              <span class="spec-label">{{ s.label }}</span>
              <span class="spec-value mono-text">{{ s.value }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ===== 开发环境 ===== -->
    <el-row :gutter="16">
      <el-col :span="24">
        <div class="card">
          <div class="card-head">
            <div class="card-icon icon-dev">DEV</div>
            <div class="card-title">开发环境</div>
            <div class="card-sub dev-refresh" @click="loadDevTools" title="重新检测版本">
              <span class="refresh-icon" :class="{ spinning: devLoading }">⟳</span>
              重新检测
            </div>
          </div>
          <div class="dev-grid" v-loading="devLoading">
            <div
              class="dev-item"
              v-for="t in devTools"
              :key="t.id"
              draggable="true"
              :class="{
                dragging: devDragId === t.id,
                'drop-before': devDragOver === t.id && devDragId !== t.id && devDropPos === 'before',
                'drop-after': devDragOver === t.id && devDragId !== t.id && devDropPos === 'after'
              }"
              title="可拖拽调整顺序"
              @dragstart="onDevDragStart(t, $event)"
              @dragover="onDevDragOver(t, $event)"
              @dragleave="devDragOver = ''"
              @drop="onDevDrop(t, $event)"
              @dragend="onDevDragEnd"
            >
              <div class="dev-emoji" :class="'dev-bg-' + t.id">{{ devIcons[t.id] ?? '🔧' }}</div>
              <div class="dev-info">
                <div class="dev-name">{{ t.name }}</div>
                <div class="dev-version mono-text" :class="{ missing: !t.version }">
                  {{ t.version ?? '未安装' }}
                </div>
                <div class="dev-detail" v-if="t.detail" :title="t.detail">{{ t.detail }}</div>
              </div>
            </div>
            <el-empty
              v-if="!devLoading && devTools.length === 0"
              description="未检测到开发工具"
              :image-size="60"
            />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { api, type SysStaticInfo, type SysDynamicInfo, type NetInterface, type PublicIPInfo, type GpuInfo, type DevToolInfo } from '../api'
import RingProgress from './RingProgress.vue'
import MiniTrend from './MiniTrend.vue'
import { useSysTrend } from '../composables/useSysTrend'

const loadingStatic = ref(true)
const info = ref<SysStaticInfo | null>(null)
/** 动态数据来自常驻采样单例：切页不丢失，趋势窗口连续 */
const trend = useSysTrend()
const dyn = computed<SysDynamicInfo | null>(() => trend.latest)
const network = ref<{ interfaces: NetInterface[]; primaryIP: string } | null>(null)
const publicIP = ref<PublicIPInfo | null>(null)
const publicIPLoading = ref(false)
const publicIPError = ref('')
const devTools = ref<DevToolInfo[]>([])
const devLoading = ref(false)
const devIcons: Record<string, string> = {
  java: '☕',
  maven: '🪶',
  node: '💚',
  docker: '🐳',
  git: '🌿',
  python: '🐍'
}

/** 拖拽排序状态；顺序偏好持久化在 localStorage */
const devDragId = ref('')
const devDragOver = ref('')
/** 释放位置：目标左半 = 插入其前，右半 = 插入其后 */
const devDropPos = ref<'before' | 'after'>('before')
const DEV_ORDER_KEY = 'maozi-devtool-order'

function applyDevOrder(list: DevToolInfo[]): DevToolInfo[] {
  let saved: string[] = []
  try {
    saved = JSON.parse(localStorage.getItem(DEV_ORDER_KEY) ?? '[]')
  } catch {
    saved = []
  }
  if (!Array.isArray(saved) || saved.length === 0) return list
  const order = new Map(saved.map((id, i) => [id, i]))
  return [...list].sort((a, b) => (order.get(a.id) ?? 999) - (order.get(b.id) ?? 999))
}

function onDevDragStart(t: DevToolInfo, e: DragEvent): void {
  devDragId.value = t.id
  if (e.dataTransfer) {
    e.dataTransfer.setData('text/plain', t.id)
    e.dataTransfer.effectAllowed = 'move'
  }
}

function onDevDragOver(t: DevToolInfo, e: DragEvent): void {
  e.preventDefault()
  devDragOver.value = t.id
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  devDropPos.value = e.clientX < rect.left + rect.width / 2 ? 'before' : 'after'
}

function onDevDragEnd(): void {
  devDragId.value = ''
  devDragOver.value = ''
  devDropPos.value = 'before'
}

function onDevDrop(t: DevToolInfo, e: DragEvent): void {
  e.preventDefault()
  const sourceId = devDragId.value
  const pos = devDropPos.value
  onDevDragEnd()
  if (!sourceId || sourceId === t.id) return
  const list = [...devTools.value]
  const from = list.findIndex((x) => x.id === sourceId)
  if (from < 0) return
  const [moved] = list.splice(from, 1)
  const to = list.findIndex((x) => x.id === t.id)
  if (to < 0) return
  list.splice(pos === 'before' ? to : to + 1, 0, moved)
  devTools.value = list
  localStorage.setItem(DEV_ORDER_KEY, JSON.stringify(list.map((x) => x.id)))
}

let clockTimer: ReturnType<typeof setInterval> | null = null

const now = ref({ date: '', weekday: '', clock: '' })

function pad(n: number): string {
  return n < 10 ? `0${n}` : `${n}`
}

function refreshClock(): void {
  const d = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  now.value = {
    date: `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日`,
    weekday: weekdays[d.getDay()],
    clock: `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
}

const mainDisk = computed(() => dyn.value?.disks.find((d) => d.mount === '/') ?? dyn.value?.disks[0])
const mainGpu = computed<GpuInfo | undefined>(() => info.value?.gpus[0])
const mainGpuUsage = computed<number | null>(() => dyn.value?.gpuUsages?.[0] ?? null)

const specRows = computed(() => [
  { label: '电脑名称', value: info.value?.computerName ?? '' },
  { label: '主机名', value: info.value?.hostname ?? '' },
  { label: '型号', value: [info.value?.modelName, info.value?.modelIdentifier].filter(Boolean).join(' · ') },
  { label: '芯片', value: info.value?.chip ?? '' },
  { label: '处理器', value: `${info.value?.cpuPhysicalCores ?? 0} 核 / ${info.value?.cpuLogicalCores ?? 0} 线程` },
  { label: '显卡', value: gpuSummary.value },
  { label: '内存', value: formatBytes(info.value?.memoryTotal ?? 0, 0) },
  { label: '系统内核', value: `Darwin ${info.value?.kernel ?? ''}` },
  { label: '架构', value: info.value?.arch ?? '' }
])

const gpuSummary = computed(() => {
  const gpus = info.value?.gpus ?? []
  if (gpus.length === 0) return '未识别'
  return gpus
    .map((g) => [g.chipsetModel || g.name, g.cores ? `${g.cores} 核心` : ''].filter(Boolean).join(' · '))
    .join('；')
})

function formatBytes(bytes: number, fractionDigits = 1): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let n = bytes
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024
    i++
  }
  return `${n.toFixed(i === 0 ? 0 : fractionDigits)} ${units[i]}`
}

function formatUptime(seconds: number): string {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (d > 0) return `${d} 天 ${h} 小时`
  if (h > 0) return `${h} 小时 ${m} 分`
  return `${m} 分钟`
}

function usageLevel(v: number): string {
  if (v >= 85) return 'level-danger'
  if (v >= 70) return 'level-warn'
  return 'level-ok'
}

async function loadStatic(): Promise<void> {
  const r = await api.sysinfo.static()
  if (r.ok && r.data) info.value = r.data
  loadingStatic.value = false
}

/* ===== 近 5 分钟趋势：数据在常驻单例中累积（见 composables/useSysTrend） ===== */
const cpuHistory = computed(() => trend.cpu)
const gpuHistory = computed(() => trend.gpu)
const memHistory = computed(() => trend.mem)
const trendTimes = computed(() => trend.times)

/** GPU 从未采集到过数据时隐藏趋势区（该平台不支持） */
const gpuHasData = computed(() => trend.gpu.some((v) => v !== null))

function peakOf(values: Array<number | null>): number {
  let max = 0
  for (const v of values) if (typeof v === 'number' && v > max) max = v
  return Math.round(max)
}

async function loadNetwork(): Promise<void> {
  const r = await api.sysinfo.network()
  if (r.ok && r.data) network.value = r.data
}

async function loadPublicIP(): Promise<void> {
  publicIPLoading.value = true
  publicIPError.value = ''
  publicIP.value = null
  const r = await api.sysinfo.publicIP()
  publicIPLoading.value = false
  if (r.ok && r.data) {
    publicIP.value = r.data
  } else {
    publicIPError.value = r.error ?? '探测失败'
  }
}

async function loadDevTools(): Promise<void> {
  devLoading.value = true
  const r = await api.sysinfo.devtools()
  if (r.ok && r.data) devTools.value = applyDevOrder(r.data)
  devLoading.value = false
}

onMounted(async () => {
  refreshClock()
  clockTimer = setInterval(refreshClock, 1000)
  // 动态指标轮询由 useSysTrend 单例常驻执行，这里只拉静态数据
  await Promise.all([loadStatic(), loadNetwork(), loadPublicIP(), loadDevTools()])
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mono-text {
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-variant-numeric: tabular-nums;
}

/* ===== Hero ===== */
.hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 18px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
}

.hero-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.hero-deco-1 {
  width: 340px;
  height: 340px;
  right: -80px;
  top: -180px;
}

.hero-deco-2 {
  width: 220px;
  height: 220px;
  right: 160px;
  bottom: -140px;
  border-color: rgba(255, 255, 255, 0.08);
}

.hero-main {
  position: relative;
  min-width: 0;
}

.hero-name {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.hero-time {
  margin-top: 8px;
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-size: 14px;
}

.hero-date {
  color: rgba(255, 255, 255, 0.82);
}

.hero-clock {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 18px rgba(96, 165, 250, 0.65);
}

.hero-host {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}

.hero-chips {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  animation: chip-in 0.4s ease both;
}

.chip:nth-child(1) { animation-delay: 0.05s; }
.chip:nth-child(2) { animation-delay: 0.1s; }
.chip:nth-child(3) { animation-delay: 0.15s; }
.chip:nth-child(4) { animation-delay: 0.2s; }
.chip:nth-child(5) { animation-delay: 0.25s; }
.chip:nth-child(6) { animation-delay: 0.3s; }

@keyframes chip-in {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.chip-strong {
  background: rgba(255, 255, 255, 0.92);
  color: #1e3fae;
  font-weight: 600;
  border-color: transparent;
}

/* 公网 IP 玻璃卡 */
.glass {
  position: relative;
  min-width: 260px;
  padding: 18px 22px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(12px);
}

.ip-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  gap: 8px;
}

.ip-source {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 10px;
}

.ip-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.ip-loading,
.ip-error {
  font-size: 14px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.75);
}

.ip-error {
  color: #fecaca;
  font-size: 13px;
}

.ip-location {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.ip-refresh {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  cursor: pointer;
  transition: background 0.2s;
}

.ip-refresh:hover {
  background: rgba(255, 255, 255, 0.28);
}

.refresh-icon {
  font-size: 15px;
  line-height: 1;
}

.spinning {
  display: inline-block;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 指标卡 ===== */
.metric-row {
  margin-bottom: 0 !important;
}

.metric-row + .metric-row {
  margin-top: 16px !important;
}

.metric-row + .el-row {
  margin-top: 16px;
}

.card {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 16px;
  padding: 20px 22px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  height: 100%;
  box-sizing: border-box;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.1);
}

.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.icon-cpu {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.icon-gpu {
  background: linear-gradient(135deg, #ec4899, #be185d);
}

.icon-mem {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
}

.icon-disk {
  background: linear-gradient(135deg, #10b981, #059669);
}

.icon-net {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}

.icon-spec {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.card-sub {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.card-body {
  display: flex;
  align-items: center;
  gap: 20px;
}

.metric-detail {
  flex: 1;
  min-width: 0;
}

.detail-model {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 每核占用 */
.core-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(14px, 1fr));
  gap: 4px;
}

.core-bar {
  height: 34px;
  border-radius: 4px;
  background: #f1f5f9;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.core-bar-fill {
  width: 100%;
  border-radius: 4px 4px 0 0;
  transition: height 0.6s ease;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
}

.core-hint {
  margin-top: 7px;
  font-size: 11px;
  color: #b0b7c3;
}

/* 卡片底部近 5 分钟趋势区 */
.trend {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #eef2f7;
}

.trend-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #8a94a6;
  margin-bottom: 6px;
}

.trend-legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.lg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.trend-peak b {
  color: #1f2d3d;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

/* 键值明细行（内存/GPU 卡），宽卡下单行展示不换行 */
.kv-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 0;
  border-bottom: 1px dashed #f0f2f5;
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
}

.kv-row:last-child {
  border-bottom: none;
}

.kv-row b {
  color: #1f2d3d;
  font-weight: 600;
  font-size: 13.5px;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-variant-numeric: tabular-nums;
}

/* 磁盘明细 */
.disk-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.disk-item {
  min-width: 0;
}

.disk-line {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 5px;
}

.disk-mount {
  color: #1f2d3d;
  font-weight: 600;
}

.disk-size {
  color: #909399;
}

.disk-bar {
  height: 8px;
  border-radius: 999px;
  background: #f1f5f9;
  overflow: hidden;
}

.disk-bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s ease;
  background: linear-gradient(90deg, #34d399, #059669);
}

.level-ok {
  background: linear-gradient(180deg, #34d399, #059669);
}

.level-warn {
  background: linear-gradient(180deg, #fbbf24, #d97706);
}

.level-danger {
  background: linear-gradient(180deg, #f87171, #dc2626);
}

.disk-bar-fill.level-ok {
  background: linear-gradient(90deg, #34d399, #059669);
}

.disk-bar-fill.level-warn {
  background: linear-gradient(90deg, #fbbf24, #d97706);
}

.disk-bar-fill.level-danger {
  background: linear-gradient(90deg, #f87171, #dc2626);
}

/* ===== 网卡列表 ===== */
.net-card {
  height: 100%;
  box-sizing: border-box;
}

.net-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.net-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  transition: border-color 0.2s;
}

.net-item.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.net-badge {
  padding: 3px 10px;
  border-radius: 6px;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 12px;
  font-weight: 600;
}

.net-item.active .net-badge {
  background: #2563eb;
  color: #fff;
}

.net-info {
  min-width: 0;
  flex: 1;
}

.net-ip {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.net-mac {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
}

.net-primary-tag {
  font-size: 11px;
  color: #2563eb;
  border: 1px solid #93c5fd;
  padding: 2px 8px;
  border-radius: 999px;
}

/* ===== 设备信息表 ===== */
.spec-card {
  height: 100%;
  box-sizing: border-box;
}

.spec-list {
  display: flex;
  flex-direction: column;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed #f0f2f5;
  font-size: 13px;
}

.spec-row:last-child {
  border-bottom: none;
}

.spec-label {
  color: #6b7280;
  flex-shrink: 0;
}

.spec-value {
  color: #1f2d3d;
  font-weight: 500;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 开发环境 ===== */
.icon-dev {
  background: linear-gradient(135deg, #06b6d4, #0e7490);
}

.dev-refresh {
  cursor: pointer;
  color: #2563eb;
  user-select: none;
}

.dev-refresh:hover {
  color: #1d4ed8;
}

.dev-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.dev-item {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  cursor: grab;
  transition: opacity 0.15s ease, outline-color 0.15s ease;
}

.dev-item:active {
  cursor: grabbing;
}

.dev-item.dragging {
  opacity: 0.4;
}

/* 拖拽插入位置指示：左侧/右侧粗线 + 光晕 */
.dev-item.drop-before {
  background: #f5f9ff;
  box-shadow:
    -5px 0 0 0 #2563eb,
    -12px 0 20px -4px rgba(37, 99, 235, 0.55);
}

.dev-item.drop-after {
  background: #f5f9ff;
  box-shadow:
    5px 0 0 0 #2563eb,
    12px 0 20px -4px rgba(37, 99, 235, 0.55);
}

.dev-emoji {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  flex-shrink: 0;
}

.dev-bg-java {
  background: linear-gradient(135deg, #fde7d3, #fbd5b0);
}

.dev-bg-maven {
  background: linear-gradient(135deg, #e2e8f0, #cbd5e1);
}

.dev-bg-node {
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
}

.dev-bg-docker {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
}

.dev-bg-git {
  background: linear-gradient(135deg, #fde8f0, #fbcfe8);
}

.dev-bg-python {
  background: linear-gradient(135deg, #e0f2fe, #bae6fd);
}

.dev-info {
  min-width: 0;
}

.dev-name {
  font-size: 12px;
  color: #8a94a6;
}

.dev-version {
  margin-top: 2px;
  font-size: 16px;
  font-weight: 700;
  color: #0f1e30;
}

.dev-version.missing {
  color: #c0c4cc;
  font-size: 14px;
  font-weight: 500;
}

.dev-detail {
  margin-top: 2px;
  font-size: 11px;
  color: #9ca3af;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
