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
            <div class="card-sub">
              {{ network?.interfaces.length ?? 0 }} 个活动网卡
              <span v-if="proxyOn" class="vpn-chip">🛡️ VPN</span>
            </div>
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

          <!-- 系统代理（scutil --proxy）：任一启用时整节高亮 + 顶部横幅提示 -->
          <div class="proxy-sec" :class="{ active: proxyOn }">
            <div v-if="proxyOn" class="vpn-banner">
              <span class="vpn-ico">🛡️</span>
              <div class="vpn-text">
                <div class="vpn-title">VPN / 代理已开启</div>
                <div class="vpn-sub">网络请求可能经由代理转发（{{ proxyOnTypes.join(' · ') }}），访问内网服务与 Git 远程请注意路由</div>
              </div>
            </div>
            <div class="proxy-head">
              <span>系统代理</span>
              <span class="proxy-src mono-text">scutil --proxy</span>
            </div>
            <div class="proxy-grid">
              <div class="proxy-item" v-for="p in proxyRows" :key="p.label" :class="{ on: !!p.value }">
                <span class="proxy-label mono-text">{{ p.label }}</span>
                <span class="proxy-value mono-text">{{ p.value || '未启用' }}</span>
              </div>
            </div>
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
              :data-dev-id="t.id"
              :class="{
                dragging: devDragId === t.id,
                'drop-before': devDragOver === t.id && devDragId !== t.id && devDropPos === 'before',
                'drop-after': devDragOver === t.id && devDragId !== t.id && devDropPos === 'after',
                clickable: t.id === 'java'
              }"
              :title="t.id === 'java' ? '点击切换 JDK 版本 · 可拖拽调整顺序' : '可拖拽调整顺序'"
              @click="t.id === 'java' && openJdkDialog()"
              @pointerdown="onDevPointerDown(t, $event)"
            >
              <div class="dev-emoji" :class="'dev-bg-' + t.id">{{ devIcons[t.id] ?? '🔧' }}</div>
              <div class="dev-info">
                <div class="dev-name">
                  {{ t.name }}
                  <span v-if="t.id === 'java'" class="dev-switch-hint">⇄ 切换版本</span>
                </div>
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

    <!-- JDK 管理弹窗：渐变头部（当前版本）+ 版本列表（主版本大字块 / LTS 徽章 / 切换与卸载） -->
    <el-dialog
      v-model="jdkVisible"
      width="640px"
      class="jdk-dialog"
      :show-close="false"
      :close-on-click-modal="!jdkSwitching && !jdkUninstalling"
    >
      <template #header>
        <div class="jdk-hero">
          <div class="jdk-hero-deco jdk-hero-deco-1"></div>
          <div class="jdk-hero-deco jdk-hero-deco-2"></div>
          <div class="jdk-hero-left">
            <div class="jdk-hero-title">{{ jdkView === 'list' ? '☕ JDK 版本管理' : '⇩ 下载 JDK' }}</div>
            <div class="jdk-hero-sub">
              {{ jdkView === 'list' ? `共 ${jdks.length} 个版本 · 切换后已打开的终端自动生效` : '官方源下载 · 安装到 ~/Library/Java/JavaVirtualMachines' }}
            </div>
          </div>
          <div v-if="jdkView === 'list'" class="jdk-hero-cur" title="当前使用中的 JDK">
            <span class="jdk-hero-cur-cup">☕</span>
            <span class="jdk-hero-cur-label">当前</span>
            <span class="jdk-hero-cur-ver mono-text">{{ jdkCurrentVersion || '—' }}</span>
          </div>
          <div v-else class="jdk-hero-acts">
            <el-button size="small" class="jdk-hero-back" :disabled="jdkDlRunning" @click="jdkView = 'list'">← 返回列表</el-button>
          </div>
          <button
            type="button"
            class="jdk-close"
            :class="{ busy: jdkSwitching || jdkUninstalling || jdkDlRunning }"
            :title="jdkSwitching || jdkUninstalling || jdkDlRunning ? '操作进行中…' : '关闭'"
            @click="closeJdkDialog"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>
      </template>

      <!-- ===== 列表视图 ===== -->
      <div v-if="jdkView === 'list'" v-loading="jdkLoading" class="jdk-list-wrap">
        <template v-if="jdks.length">
          <div
            v-for="(j, jdkIdx) in jdks"
            :key="j.home"
            class="jdk-item"
            :class="{ current: j.home === jdkCurrent, busy: jdkSwitching || jdkUninstalling }"
            :style="{ '--jdk-i': jdkIdx }"
          >
            <div class="jdk-major" :class="{ cur: j.home === jdkCurrent }">
              <span class="jdk-major-num mono-text">{{ jdkMajorOf(j.version) }}</span>
              <span v-if="jdkIsLts(j.version)" class="jdk-lts">LTS</span>
            </div>
            <div class="jdk-mid">
              <div class="jdk-line1">
                <span class="jdk-fullver mono-text">{{ j.version }}</span>
                <span class="jdk-vendor">{{ j.vendor }}</span>
              </div>
              <div class="jdk-home mono-text" :title="j.home">{{ j.home }}</div>
            </div>
            <div class="jdk-acts">
              <span v-if="j.home === jdkCurrent" class="jdk-now">✓ 当前</span>
              <el-button
                v-else
                type="primary"
                size="small"
                class="jdk-btn jdk-btn-switch"
                :disabled="jdkSwitching || jdkUninstalling"
                @click="switchJdk(j)"
              >切换版本</el-button>
              <el-button
                size="small"
                text
                type="danger"
                class="jdk-btn"
                :disabled="j.home === jdkCurrent || jdkSwitching || jdkUninstalling"
                :title="j.home === jdkCurrent ? '当前使用中，请先切换后再卸载' : '卸载该 JDK（不可恢复）'"
                @click="uninstallJdkEntry(j)"
              >卸载</el-button>
            </div>
          </div>
          <div class="jdk-list-foot">
            <div class="jdk-tip">
              <span class="jdk-tip-ico">💡</span>
              <span>「切换版本」即时写入 ~/.zshrc（在线终端下一条命令即用新版本）；「卸载」按来源处理——用户目录直接删除、Homebrew 走 brew uninstall、系统位置需输入管理员密码</span>
            </div>
            <el-button type="primary" class="jdk-dl-open-btn" :disabled="jdkSwitching || jdkUninstalling" @click="openJdkDownload">
              <span class="jdk-dl-open-ico">⇩</span> 下载新版本
            </el-button>
          </div>
        </template>
        <el-empty v-else-if="!jdkLoading" description="未发现可切换的 JDK（支持 Apple 标准位置 / Homebrew / SDKMAN）" :image-size="60">
          <el-button type="primary" @click="openJdkDownload">⇩ 下载 JDK</el-button>
        </el-empty>
      </div>

      <!-- ===== 下载视图：先选版本，再选发行商 ===== -->
      <div v-else class="jdk-dl-wrap">
        <div class="jdk-dl-step">
          <div class="jdk-dl-step-title"><span class="jdk-dl-step-num">1</span>选择版本</div>
          <div class="jdk-dl-vers">
            <button
              v-for="v in JDK_DL_VERSIONS"
              :key="v"
              type="button"
              class="jdk-dl-ver"
              :class="{ on: jdkDlVersion === v, busy: jdkDlRunning }"
              :disabled="jdkDlRunning"
              @click="jdkDlVersion = v; if (!jdkDlVendorOk(jdkDlVendor)) jdkDlVendor = 'corretto'"
            >
              <span class="jdk-dl-ver-num mono-text">{{ v }}</span>
              <span class="jdk-lts">LTS</span>
            </button>
          </div>
        </div>

        <div class="jdk-dl-step">
          <div class="jdk-dl-step-title"><span class="jdk-dl-step-num">2</span>选择发行商</div>
          <div class="jdk-dl-vendors">
            <button
              v-for="vd in JDK_DL_VENDORS"
              :key="vd.id"
              type="button"
              class="jdk-dl-vendor"
              :class="{ on: jdkDlVendor === vd.id, off: !jdkDlVendorOk(vd.id), busy: jdkDlRunning }"
              :disabled="jdkDlRunning || !jdkDlVendorOk(vd.id)"
              :title="jdkDlVendorOk(vd.id) ? vd.sub : '该发行商不提供当前所选版本/架构'"
              @click="jdkDlVendor = vd.id"
            >
              <span class="jdk-dl-vendor-ico" :class="vd.id">{{ vd.icon }}</span>
              <span class="jdk-dl-vendor-text">
                <span class="jdk-dl-vendor-name">{{ vd.name }}</span>
                <span class="jdk-dl-vendor-sub" :class="{ warn: !jdkDlVendorOk(vd.id) }">{{ jdkDlVendorHint(vd.id) }}</span>
              </span>
              <span class="jdk-dl-check">✓</span>
            </button>
          </div>
        </div>

        <!-- 下载进度 / 开始按钮 -->
        <div class="jdk-dl-foot">
          <template v-if="jdkDlRunning">
            <div class="jdk-dl-bar-row">
              <div class="jdk-dl-bar">
                <div class="jdk-dl-bar-fill" :class="{ indeterminate: jdkDlStage !== 'download' }" :style="{ width: jdkDlStage === 'download' ? jdkDlPct + '%' : '40%' }"></div>
              </div>
              <span class="jdk-dl-pct mono-text">{{ jdkDlStage === 'download' ? jdkDlPct + '%' : '…' }}</span>
            </div>
            <div class="jdk-dl-msg">
              {{ jdkDlStageText }}<template v-if="jdkDlStage === 'download' && jdkDlTotalMB"> · {{ jdkDlReceivedMB }} / {{ jdkDlTotalMB }} MB</template>
            </div>
            <el-button text type="danger" size="small" class="jdk-dl-cancel" @click="cancelJdkDownload">取消下载</el-button>
          </template>
          <template v-else>
            <div class="jdk-dl-summary">
              即将下载 <b>JDK {{ jdkDlVersion }}</b>（{{ jdkDlVendorName }}）到 ~/Library/Java/JavaVirtualMachines，同名旧版本将被覆盖
            </div>
            <el-button type="primary" class="jdk-dl-start" @click="startJdkDownload">
              <span class="jdk-dl-open-ico">⇩</span> 开始下载
            </el-button>
          </template>
        </div>
      </div>
    </el-dialog>

    <!-- 指针拖拽预览：跟随光标浮层（teleport 到 body，不受卡片 overflow 裁剪） -->
    <teleport to="body">
      <div v-if="devGhost" class="dev-ghost" :style="{ left: devGhost.x + 'px', top: devGhost.y + 'px' }">
        <span class="dev-ghost-ico">{{ devGhost.icon }}</span>
        <span>{{ devGhost.name }}</span>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
// 系统信息页：设备概览、CPU/GPU/内存/磁盘实时指标与趋势、网络与代理、开发工具链版本
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, type SysStaticInfo, type SysDynamicInfo, type NetInterface, type ProxyInfo, type PublicIPInfo, type GpuInfo, type DevToolInfo, type JdkEntry } from '../api'
import RingProgress from './RingProgress.vue'
import MiniTrend from './MiniTrend.vue'
import { useSysTrend } from '../composables/useSysTrend'

const loadingStatic = ref(true)
const info = ref<SysStaticInfo | null>(null)
/** 动态数据来自常驻采样单例：切页不丢失，趋势窗口连续 */
const trend = useSysTrend()
const dyn = computed<SysDynamicInfo | null>(() => trend.latest)
const network = ref<{ interfaces: NetInterface[]; primaryIP: string; proxy?: ProxyInfo } | null>(null)
const publicIP = ref<PublicIPInfo | null>(null)
const publicIPLoading = ref(false)
const publicIPError = ref('')
const devTools = ref<DevToolInfo[]>([])

/** ===== JDK 切换：列出本机 JDK，点击切换（JAVA_HOME 写 ~/.zshrc，终端实时生效） ===== */
const jdkVisible = ref(false)
const jdkLoading = ref(false)
const jdkSwitching = ref(false)
const jdkUninstalling = ref(false)
const jdks = ref<JdkEntry[]>([])
const jdkCurrent = ref('')

/** 头部当前版本显示（列表中的当前项版本号；JAVA_HOME 指向列表外时显示路径尾段） */
const jdkCurrentVersion = computed(() => {
  const hit = jdks.value.find(j => j.home === jdkCurrent.value)
  if (hit) return hit.version
  return jdkCurrent.value ? jdkCurrent.value.split('/').slice(-3).join('/') : ''
})

/** 主版本号（21.0.3 → 21；1.8.0_392 → 8） */
function jdkMajorOf(version: string): string {
  const parts = version.split(/[._]/)
  const major = Number(parts[0])
  // 老式 1.8.0_x 命名：主版本取第 2 段
  return major === 1 ? String(parts[1] ?? '8') : parts[0] ?? version
}

/** LTS 版本判定（Oracle/社区长期支持线） */
function jdkIsLts(version: string): boolean {
  return ['8', '11', '17', '21', '25'].includes(jdkMajorOf(version))
}

async function openJdkDialog(): Promise<void> {
  if (jdkSwitching.value || jdkUninstalling.value) return
  jdkVisible.value = true
  if (jdks.value.length) return
  jdkLoading.value = true
  try {
    const r = await api.sysinfo.jdkList()
    if (r.ok && r.data) {
      jdks.value = r.data.list
      jdkCurrent.value = r.data.current
      jdkArch.value = r.data.arch ?? 'aarch64'
    } else {
      ElMessage.error(r.error ?? '读取 JDK 列表失败')
    }
  } finally {
    jdkLoading.value = false
  }
}

async function switchJdk(j: JdkEntry): Promise<void> {
  if (jdkSwitching.value || jdkUninstalling.value || j.home === jdkCurrent.value) return
  jdkSwitching.value = true
  try {
    const r = await api.sysinfo.jdkSwitch(j.home)
    if (r.ok) {
      jdkCurrent.value = j.home
      ElMessage.success(`已切换到 JDK ${j.version}（已打开的终端自动生效）`)
      await Promise.all([openJdkRefresh(), loadDevTools()])
    } else {
      ElMessage.error(r.error ?? '切换失败')
    }
  } finally {
    jdkSwitching.value = false
  }
}

/** 卸载 JDK：二次确认（不可恢复）后执行，按来源分类删除，完成后刷新列表 */
async function uninstallJdkEntry(j: JdkEntry): Promise<void> {
  if (jdkSwitching.value || jdkUninstalling.value || j.home === jdkCurrent.value) return
  try {
    await ElMessageBox.confirm(
      `确定卸载 JDK ${j.version}（${j.vendor}）吗？将删除：${j.home}。此操作不可恢复，建议先确认没有项目依赖该版本。`,
      '卸载 JDK',
      { confirmButtonText: '卸载', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  jdkUninstalling.value = true
  try {
    const r = await api.sysinfo.jdkUninstall(j.home)
    if (r.ok) {
      ElMessage.success(`已卸载 JDK ${j.version}`)
      await openJdkRefresh()
    } else {
      ElMessage.error(r.error ?? '卸载失败')
    }
  } finally {
    jdkUninstalling.value = false
  }
}

/** ===== JDK 下载：列表视图 ⇄ 下载视图（先选版本，再选发行商） ===== */
const JDK_DL_VERSIONS = ['8', '11', '17', '21', '25']
const JDK_DL_VENDORS = [
  { id: 'adoptium' as const, name: 'Eclipse Adoptium', sub: 'Temurin · Eclipse 基金会', icon: '☀️' },
  { id: 'corretto' as const, name: 'Amazon Corretto', sub: 'AWS · 免费 · 长期更新', icon: '📦' }
]
/** 本机架构（jdkList 带出）：Adoptium 不提供 Apple Silicon 的 JDK 8，用于禁用不可用组合 */
const jdkArch = ref<'aarch64' | 'x64'>('aarch64')
function jdkDlVendorOk(vendor: 'adoptium' | 'corretto'): boolean {
  return !(jdkArch.value === 'aarch64' && jdkDlVersion.value === '8' && vendor === 'adoptium')
}
function jdkDlVendorHint(vendor: 'adoptium' | 'corretto'): string {
  const vd = JDK_DL_VENDORS.find(v => v.id === vendor)
  if (vd && !jdkDlVendorOk(vendor)) return '未提供 Apple Silicon 的 JDK 8'
  return vd?.sub ?? ''
}

const jdkView = ref<'list' | 'download'>('list')
const jdkDlVersion = ref('21')
const jdkDlVendor = ref<'adoptium' | 'corretto'>('adoptium')
const jdkDlRunning = ref(false)
const jdkDlStage = ref<'idle' | 'download' | 'extract' | 'install' | 'done' | 'error'>('idle')
const jdkDlPct = ref(0)
const jdkDlReceivedMB = ref(0)
const jdkDlTotalMB = ref(0)
const jdkDlMsg = ref('')

const jdkDlVendorName = computed(() => JDK_DL_VENDORS.find(v => v.id === jdkDlVendor.value)?.name ?? '')
const jdkDlStageText = computed(() => {
  if (jdkDlMsg.value) return jdkDlMsg.value
  const names: Record<string, string> = { download: '下载中…', extract: '解压安装中…', install: '安装中…' }
  return names[jdkDlStage.value] ?? ''
})

function closeJdkDialog(): void {
  if (jdkSwitching.value || jdkUninstalling.value || jdkDlRunning.value) return
  jdkVisible.value = false
}

function openJdkDownload(): void {
  jdkView.value = 'download'
  jdkDlStage.value = 'idle'
  jdkDlPct.value = 0
  jdkDlMsg.value = ''
}

async function startJdkDownload(): Promise<void> {
  if (jdkDlRunning.value) return
  if (!jdkDlVendorOk(jdkDlVendor.value)) {
    ElMessage.warning(`Eclipse Adoptium 未提供 Apple Silicon（aarch64）的 JDK 8，请选择其它版本或改用 Amazon Corretto`)
    return
  }
  jdkDlRunning.value = true
  jdkDlStage.value = 'download'
  jdkDlPct.value = 0
  jdkDlReceivedMB.value = 0
  jdkDlTotalMB.value = 0
  jdkDlMsg.value = ''
  try {
    const r = await api.sysinfo.jdkDownload(jdkDlVersion.value, jdkDlVendor.value)
    if (r.ok) {
      jdkDlStage.value = 'done'
      ElMessage.success(`JDK ${jdkDlVersion.value}（${jdkDlVendorName.value}）下载安装完成`)
      jdkView.value = 'list'
      await openJdkRefresh()
    } else {
      jdkDlStage.value = 'error'
      ElMessage.error(r.error ?? '下载失败')
    }
  } finally {
    jdkDlRunning.value = false
  }
}

async function cancelJdkDownload(): Promise<void> {
  await api.sysinfo.jdkDownloadCancel()
}

let jdkDlOff: (() => void) | null = null
onMounted(() => {
  jdkDlOff = api.sysinfo.onJdkDownloadProgress(p => {
    jdkDlStage.value = p.stage
    jdkDlPct.value = p.pct
    jdkDlReceivedMB.value = p.receivedMB
    jdkDlTotalMB.value = p.totalMB
    jdkDlMsg.value = p.message
  })
})
onUnmounted(() => jdkDlOff?.())

/** 切换后刷新列表（保持弹窗内“当前”标记与最新环境一致） */
async function openJdkRefresh(): Promise<void> {
  const r = await api.sysinfo.jdkList()
  if (r.ok && r.data) {
    jdks.value = r.data.list
    jdkCurrent.value = r.data.current
    jdkArch.value = r.data.arch ?? 'aarch64'
  }
}
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

/**
 * 指针事件自定义拖拽（替代原生 HTML5 DnD）：原生拖拽进行中的光标由浏览器强制接管
 * （显示系统 move 光标），CSS 无法控制；指针拖拽全程可把光标锁定为抓手，直至释放。
 * 位移超过 6px 才进入拖拽（未超过的释放不拦截，原生 click 正常触发 Java 弹窗）
 */
const devGhost = ref<{ x: number; y: number; name: string; icon: string } | null>(null)
const DEV_DRAG_CLASS = 'dev-pointer-dragging'
let devPtr: { id: string; name: string; icon: string; startX: number; startY: number; active: boolean } | null = null

function onDevPointerDown(t: DevToolInfo, e: PointerEvent): void {
  if (e.button !== 0) return
  devPtr = {
    id: t.id,
    name: t.name,
    icon: devIcons[t.id] ?? '🔧',
    startX: e.clientX,
    startY: e.clientY,
    active: false
  }
  window.addEventListener('pointermove', onDevPointerMove)
  window.addEventListener('pointerup', onDevPointerUp)
}

/** 命中测试：坐标落在哪个 dev-item（data-dev-id 标记） */
function hitDevItem(x: number, y: number): { id: string; rect: DOMRect } | null {
  for (const el of document.querySelectorAll<HTMLElement>('.dev-item')) {
    const r = el.getBoundingClientRect()
    if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) {
      const id = el.getAttribute('data-dev-id') ?? ''
      if (id) return { id, rect: r }
    }
  }
  return null
}

function onDevPointerMove(e: PointerEvent): void {
  if (!devPtr) return
  if (!devPtr.active) {
    if (Math.hypot(e.clientX - devPtr.startX, e.clientY - devPtr.startY) < 6) return
    devPtr.active = true
    devDragId.value = devPtr.id
    document.documentElement.classList.add(DEV_DRAG_CLASS)
  }
  e.preventDefault()
  devGhost.value = { x: e.clientX, y: e.clientY, name: devPtr.name, icon: devPtr.icon }
  const target = hitDevItem(e.clientX, e.clientY)
  if (target && target.id !== devPtr.id) {
    devDragOver.value = target.id
    devDropPos.value = e.clientX < target.rect.left + target.rect.width / 2 ? 'before' : 'after'
  } else {
    devDragOver.value = ''
  }
}

function onDevPointerUp(): void {
  window.removeEventListener('pointermove', onDevPointerMove)
  window.removeEventListener('pointerup', onDevPointerUp)
  document.documentElement.classList.remove(DEV_DRAG_CLASS)
  if (devPtr?.active) {
    // 拖拽后抑制一次原生 click（避免拖拽落在 Java 项上误开弹窗）
    const suppress = (ev: Event): void => {
      ev.stopPropagation()
      ev.preventDefault()
    }
    window.addEventListener('click', suppress, { capture: true, once: true })
    const sourceId = devPtr.id
    const overId = devDragOver.value
    const pos = devDropPos.value
    devDragId.value = ''
    devDragOver.value = ''
    devDropPos.value = 'before'
    if (overId && sourceId !== overId) {
      const list = [...devTools.value]
      const from = list.findIndex((x) => x.id === sourceId)
      if (from >= 0) {
        const [moved] = list.splice(from, 1)
        const to = list.findIndex((x) => x.id === overId)
        if (to >= 0) {
          list.splice(pos === 'before' ? to : to + 1, 0, moved)
          devTools.value = list
          localStorage.setItem(DEV_ORDER_KEY, JSON.stringify(list.map((x) => x.id)))
        }
      }
    }
  }
  devPtr = null
  devGhost.value = null
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

/** 系统代理展示行（scutil --proxy）：HTTP / HTTPS / ALL(SOCKS)，未启用显示占位 */
const proxyRows = computed(() => [
  { label: 'HTTP', value: network.value?.proxy?.http ?? '' },
  { label: 'HTTPS', value: network.value?.proxy?.https ?? '' },
  { label: 'ALL', value: network.value?.proxy?.all ?? '' }
])

/** VPN/代理开启判定（任一启用）与启用的类型名（横幅展示用） */
const proxyOn = computed(() => proxyRows.value.some(p => !!p.value))
const proxyOnTypes = computed(() => proxyRows.value.filter(p => !!p.value).map(p => (p.label === 'ALL' ? 'SOCKS' : p.label)))

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

/* 卡头 VPN 徽章 */
.vpn-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 700;
  color: #b45309;
  background: #fef3c7;
  border: 1px solid #fde68a;
  vertical-align: 1px;
}

/* 系统代理（scutil --proxy）：VPN 开启时整节琥珀高亮 */
.proxy-sec {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}

.proxy-sec.active {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #fde68a;
  border-radius: 12px;
  background: linear-gradient(135deg, #fffbeb, #fef9ec);
}

.proxy-sec.active .proxy-head > span:first-child {
  color: #b45309;
}

/* VPN 高亮横幅：图标 + 标题 + 说明，脉冲点提示"当前生效" */
.vpn-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border: 1px solid #fbbf24;
}

.vpn-ico {
  flex: none;
  font-size: 22px;
  filter: drop-shadow(0 2px 4px rgba(180, 83, 9, 0.25));
}

.vpn-text {
  min-width: 0;
}

.vpn-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 800;
  color: #92400e;
}

.vpn-title::after {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d97706;
  animation: vpn-pulse 1.6s ease infinite;
}

@keyframes vpn-pulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

.vpn-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #a16207;
  line-height: 1.5;
}

.proxy-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.proxy-src {
  font-size: 10.5px;
  font-weight: 400;
  color: #94a3b8;
}

.proxy-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 9px;
}

.proxy-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.proxy-item.on {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.proxy-label {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #94a3b8;
}

.proxy-item.on .proxy-label {
  color: #2563eb;
}

.proxy-value {
  font-size: 11.5px;
  color: #cbd5e1;
  word-break: break-all;
  line-height: 1.4;
}

.proxy-item.on .proxy-value {
  color: #1e293b;
  font-weight: 600;
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
  /* 悬停保持普通箭头，拖拽时全局类锁定抓手（见 :global 规则） */
  cursor: default;
  touch-action: none;
  transition: opacity 0.15s ease, outline-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

/* 指针拖拽进行中：全页面光标锁定为抓手并禁止选中文本（html 上的全局类由脚本维护） */
:global(html.dev-pointer-dragging),
:global(html.dev-pointer-dragging *) {
  cursor: grab !important;
  user-select: none !important;
}

/* Java 项可点击切换 JDK：hover 点击手势 + 高亮 + 「切换版本」渐变胶囊 */
.dev-item.clickable {
  cursor: pointer;
}

.dev-item.clickable:hover {
  border-color: #fdba74;
  box-shadow: 0 4px 12px rgba(234, 88, 12, 0.12);
}

.dev-switch-hint {
  display: inline-flex;
  align-items: center;
  margin-left: 7px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #fff;
  background: linear-gradient(135deg, #f97316, #ea580c);
  box-shadow: 0 2px 6px rgba(234, 88, 12, 0.35);
  vertical-align: 1.5px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dev-item.clickable:hover .dev-switch-hint {
  transform: translateY(-1px);
  box-shadow: 0 3px 9px rgba(234, 88, 12, 0.45);
}

/* ===== JDK 管理弹窗 ===== */
/* 头部贴边：自定义渐变横幅铺满 header 区 */
.jdk-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.jdk-dialog :deep(.el-dialog) {
  overflow: hidden;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.28), 0 8px 24px rgba(30, 63, 174, 0.18);
}

/* 自定义关闭按钮：玻璃圆钮，hover 旋转 + 红渐变高亮，操作进行中禁点 */
.jdk-close {
  position: absolute;
  top: 13px;
  right: 13px;
  z-index: 3;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(6px);
  color: rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.3, 1.4, 0.5, 1), background 0.2s ease, border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.jdk-close svg {
  width: 13px;
  height: 13px;
}

.jdk-close:hover {
  background: linear-gradient(135deg, #f87171, #dc2626);
  border-color: rgba(255, 255, 255, 0.55);
  box-shadow: 0 3px 12px rgba(220, 38, 38, 0.45);
  transform: rotate(90deg) scale(1.08);
}

.jdk-close.busy {
  opacity: 0.45;
  pointer-events: none;
}

.jdk-dialog :deep(.el-dialog__body) {
  padding: 16px 18px 18px;
}

/* 头部横幅：深蓝渐变 + 装饰圆环 + 当前版本 */
.jdk-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 18px 56px 18px 22px;
  border-radius: var(--el-dialog-border-radius, 12px) var(--el-dialog-border-radius, 12px) 0 0;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
}

.jdk-hero-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.jdk-hero-deco-1 {
  width: 180px;
  height: 180px;
  right: -50px;
  top: -100px;
}

.jdk-hero-deco-2 {
  width: 110px;
  height: 110px;
  right: 100px;
  bottom: -60px;
  border-color: rgba(255, 255, 255, 0.08);
}

.jdk-hero-left {
  position: relative;
  min-width: 0;
}

.jdk-hero-title {
  font-size: 16.5px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.jdk-hero-sub {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

/* 当前版本卡：半透明玻璃 */
.jdk-hero-cur {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 16px;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(6px);
}

.jdk-hero-cur-cup {
  font-size: 15px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.25));
}

.jdk-hero-cur-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.jdk-hero-cur-ver {
  font-size: 19px;
  font-weight: 800;
  color: #fde68a;
  letter-spacing: 0.5px;
}

/* 列表行 */
.jdk-list-wrap {
  min-height: 120px;
}

.jdk-item {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 11px 13px;
  margin-bottom: 9px;
  border: 1.5px solid #e6edf5;
  border-radius: 13px;
  background: #fbfdff;
  transition: border-color 0.15s ease, background 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease;
  animation: jdk-item-in 0.34s cubic-bezier(0.2, 0.9, 0.3, 1) both;
  animation-delay: calc(var(--jdk-i, 0) * 35ms);
}

@keyframes jdk-item-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 悬停时主版本块发光呼应 */
.jdk-item:hover .jdk-major {
  box-shadow: 0 5px 15px rgba(30, 63, 174, 0.38), 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.jdk-item.current:hover .jdk-major {
  box-shadow: 0 5px 15px rgba(5, 150, 105, 0.4), 0 0 0 3px rgba(52, 211, 153, 0.14);
}

.jdk-item:hover {
  border-color: #fb923c;
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(234, 88, 12, 0.11);
}

.jdk-item.current {
  border-color: #34d399;
  background: linear-gradient(135deg, #f0fdf4, #ecfdf5);
  box-shadow: 0 3px 10px rgba(5, 150, 105, 0.08);
}

.jdk-item.busy {
  pointer-events: none;
  opacity: 0.75;
}

/* 主版本大字块：蓝渐变（当前项绿色）+ LTS 金徽章 */
.jdk-major {
  position: relative;
  flex: none;
  width: 58px;
  height: 52px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3fae, #3b82f6);
  box-shadow: 0 3px 8px rgba(30, 63, 174, 0.28);
}

.jdk-major.cur {
  background: linear-gradient(135deg, #059669, #34d399);
  box-shadow: 0 3px 8px rgba(5, 150, 105, 0.3);
}

.jdk-major-num {
  font-size: 23px;
  font-weight: 800;
  color: #fff;
  line-height: 1;
}

.jdk-lts {
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #92400e;
  background: linear-gradient(135deg, #fde68a, #fbbf24);
  border: 1px solid #f59e0b;
  box-shadow: 0 1px 4px rgba(180, 83, 9, 0.35);
}

/* 中列：完整版本 + 发行方 / 路径 */
.jdk-mid {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.jdk-line1 {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.jdk-fullver {
  font-size: 13.5px;
  font-weight: 800;
  color: #1f2d3d;
}

.jdk-vendor {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.jdk-item.current .jdk-vendor {
  color: #047857;
}

.jdk-home {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: #94a3b8;
}

/* 操作列 */
.jdk-acts {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.jdk-btn {
  padding: 6px 14px;
  border-radius: 9px;
  font-weight: 700;
}

.jdk-btn-switch {
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
  border: none !important;
  box-shadow: 0 3px 9px rgba(37, 99, 235, 0.32);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.jdk-btn-switch:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 5px 13px rgba(37, 99, 235, 0.42);
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
}

.jdk-now {
  padding: 4px 13px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 800;
  color: #059669;
  background: #d1fae5;
  border: 1px solid #a7f3d0;
}

/* 底部提示 */
.jdk-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 6px;
  padding: 9px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  font-size: 11.5px;
  color: #8a94a6;
  line-height: 1.65;
}

.jdk-tip-ico {
  flex: none;
  font-size: 13px;
}

/* 列表底部：提示 + 下载入口按钮 */
.jdk-list-foot {
  display: flex;
  align-items: stretch;
  gap: 10px;
  margin-top: 6px;
  padding: 11px 12px;
  border-radius: 13px;
  background: linear-gradient(135deg, #f8fafc, #f1f6fc);
  border: 1px solid #e8eef6;
}

.jdk-list-foot .jdk-tip {
  flex: 1;
  min-width: 0;
  margin-top: 0;
  background: transparent;
  border-color: #dde6f2;
}

.jdk-dl-open-btn {
  flex: none;
  align-self: center;
  height: 40px;
  padding: 0 20px;
  border-radius: 11px;
  font-weight: 700;
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
  border: none !important;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.32);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.jdk-dl-open-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(37, 99, 235, 0.42);
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
}

.jdk-dl-open-ico {
  margin-right: 4px;
  font-size: 15px;
}

/* 下载视图头部返回按钮（玻璃风） */
.jdk-hero-acts {
  position: relative;
}

.jdk-hero-back {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.13) !important;
  border: 1px solid rgba(255, 255, 255, 0.28) !important;
  border-radius: 9px;
  font-weight: 600;
  backdrop-filter: blur(6px);
}

.jdk-hero-back:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.22) !important;
}

/* ===== 下载视图：两步各自成卡片 ===== */
.jdk-dl-wrap {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.jdk-dl-step {
  padding: 13px 14px 14px;
  border: 1px solid #e8eef6;
  border-radius: 14px;
  background: linear-gradient(180deg, #fcfdff, #f8fafd);
  animation: jdk-item-in 0.34s ease both;
}

.jdk-dl-step:nth-child(2) {
  animation-delay: 70ms;
}

.jdk-dl-step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.jdk-dl-step-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11.5px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #1e3fae);
}

.jdk-dl-vers {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 9px;
}

.jdk-dl-ver {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 14px 4px 12px;
  border: 1.5px solid #e6edf5;
  border-radius: 12px;
  background: #fbfdff;
  cursor: pointer;
  transition: all 0.18s ease;
}

.jdk-dl-ver:hover {
  border-color: #93c5fd;
  transform: translateY(-2px);
  box-shadow: 0 5px 13px rgba(37, 99, 235, 0.12);
}

.jdk-dl-ver.on {
  border-color: #2563eb;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.2), inset 0 0 0 1px rgba(37, 99, 235, 0.15);
  transform: translateY(-2px);
}

/* 选中态顶部小圆点 */
.jdk-dl-ver.on::before {
  content: '';
  position: absolute;
  top: 6px;
  right: 6px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.18);
}

.jdk-dl-ver.busy {
  pointer-events: none;
  opacity: 0.7;
}

.jdk-dl-ver-num {
  font-size: 22px;
  font-weight: 800;
  color: #1f2d3d;
}

.jdk-dl-ver.on .jdk-dl-ver-num {
  color: #1d4ed8;
}

.jdk-dl-ver .jdk-lts {
  position: static;
  transform: none;
}

/* 步骤 2 发行商卡 */
.jdk-dl-vendors {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.jdk-dl-vendor {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 14px;
  border: 1.5px solid #e6edf5;
  border-radius: 13px;
  background: #fbfdff;
  cursor: pointer;
  text-align: left;
  transition: all 0.18s ease;
}

.jdk-dl-vendor:hover {
  border-color: #93c5fd;
  transform: translateY(-2px);
  box-shadow: 0 5px 13px rgba(37, 99, 235, 0.12);
}

.jdk-dl-vendor.on {
  border-color: #2563eb;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.16);
}

.jdk-dl-vendor.busy {
  pointer-events: none;
  opacity: 0.7;
}

.jdk-dl-vendor.off {
  pointer-events: none;
  opacity: 0.5;
  border-style: dashed;
  background: #f8fafc;
}

.jdk-dl-vendor-sub.warn {
  color: #d97706;
  font-weight: 600;
}

.jdk-dl-vendor-ico {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.jdk-dl-vendor-ico.adoptium {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-color: #fdba74;
  box-shadow: 0 3px 8px rgba(249, 115, 22, 0.18);
}

.jdk-dl-vendor-ico.corretto {
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  border-color: #fca5a5;
  box-shadow: 0 3px 8px rgba(239, 68, 68, 0.16);
}

.jdk-dl-vendor-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.jdk-dl-vendor-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #1f2d3d;
}

.jdk-dl-vendor-sub {
  font-size: 11px;
  color: #8a94a6;
}

.jdk-dl-check {
  flex: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  opacity: 0;
  transform: scale(0.4);
  transition: all 0.2s cubic-bezier(0.3, 1.5, 0.5, 1);
}

.jdk-dl-vendor.on .jdk-dl-check {
  opacity: 1;
  transform: scale(1);
}

/* 底部：摘要 + 开始 / 进度条 */
.jdk-dl-foot {
  padding-top: 14px;
  border-top: 1px dashed #e2e8f0;
}

.jdk-dl-summary {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 12px;
}

.jdk-dl-summary b {
  color: #1d4ed8;
}

.jdk-dl-start {
  width: 100%;
  height: 42px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.jdk-dl-start:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
  background: linear-gradient(135deg, #2563eb, #1e3fae) !important;
}

.jdk-dl-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.jdk-dl-bar {
  flex: 1;
  height: 12px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.jdk-dl-bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #60a5fa, #2563eb);
  transition: width 0.25s ease;
}

.jdk-dl-bar-fill {
  position: relative;
  overflow: hidden;
}

/* 下载阶段：扫光 */
.jdk-dl-bar-fill:not(.indeterminate)::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  animation: jdk-dl-shine 1.8s linear infinite;
}

@keyframes jdk-dl-shine {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(100%);
  }
}

.jdk-dl-bar-fill.indeterminate {
  animation: jdk-dl-indet 1.2s ease-in-out infinite;
}

@keyframes jdk-dl-indet {
  0% {
    margin-left: -40%;
  }
  100% {
    margin-left: 100%;
  }
}

.jdk-dl-pct {
  flex: none;
  min-width: 44px;
  text-align: right;
  font-size: 13.5px;
  font-weight: 800;
  color: #1d4ed8;
  font-variant-numeric: tabular-nums;
}

.jdk-dl-msg {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 9px;
  font-size: 12px;
  color: #8a94a6;
  font-variant-numeric: tabular-nums;
}

.jdk-dl-msg::before {
  content: '⇩';
  font-size: 14px;
  font-weight: 800;
  color: #2563eb;
  animation: jdk-dl-bounce 1.1s ease-in-out infinite;
}

@keyframes jdk-dl-bounce {
  0%,
  100% {
    transform: translateY(-1px);
    opacity: 0.6;
  }
  50% {
    transform: translateY(2px);
    opacity: 1;
  }
}

.jdk-dl-cancel {
  margin-top: 6px;
}


.dev-item.dragging {
  opacity: 0.4;
}

/* 指针拖拽预览浮层：跟随光标（teleport 到 body，不裁剪不拦截命中） */
.dev-ghost {
  position: fixed;
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #fdba74;
  box-shadow: 0 8px 24px rgba(234, 88, 12, 0.25);
  font-size: 12.5px;
  font-weight: 700;
  color: #334155;
  transform: translate(-50%, -140%);
  pointer-events: none;
}

.dev-ghost-ico {
  font-size: 16px;
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
