<template>
  <div class="pc-page">
    <!-- ===== Hero ===== -->
    <div class="pc-hero">
      <div class="pc-deco pc-deco-1"></div>
      <div class="pc-deco pc-deco-2"></div>
      <div class="pc-hero-left">
        <div class="pc-hero-name">🚀 项目控制台</div>
        <div class="pc-hero-sub">maozi-cloud — 让开发者专注业务逻辑，实现高效快速开发</div>
        <div class="pc-hero-chips">
          <span class="pc-chip" :class="binding ? 'ok' : 'idle'">
            {{ binding ? `✅ 已绑定 · ${binding.name}` : '⚡ 待绑定' }}
          </span>
          <span v-if="binding?.version" class="pc-chip ver">🏷 v{{ binding.version }}</span>
          <span class="pc-chip">🌿 GitHub 开源仓库</span>
        </div>
      </div>
      <div class="pc-hero-tools">
        <button class="pc-link" title="打开 GitHub 仓库" @click="openRepo">🌐 GitHub 仓库</button>
      </div>
    </div>

    <!-- ===== 项目面板 ===== -->
    <div class="pc-panel">
      <!-- 项目简介 -->
      <div class="pc-intro">
        <div class="pc-intro-bar"></div>
        <div>
          <div class="pc-intro-title">项目简介</div>
          <div class="pc-intro-text">
            基于 Spring Cloud Alibaba + Dubbo 的一站式分布式解决方案开源封装，内置分布式应用开发所需全套组件，统一团队代码风格，保障代码质量，让开发者专注业务逻辑，实现高效快速开发。
          </div>
        </div>
      </div>

      <!-- 已绑定 -->
      <div v-if="binding" class="pc-bound">
        <div class="pc-bound-head">
          <div class="pc-proj-ico">🚀</div>
          <div class="pc-bound-title">
            <div class="pc-proj-name">
              {{ binding.name }}
              <span v-if="binding.version" class="pc-ver-pill">v{{ binding.version }}</span>
            </div>
            <div class="pc-path-row">
              <div v-if="binding.remote" class="pc-remote-badge" :title="`SSH ${binding.remote.user}@${binding.remote.host}:${binding.remote.port}`">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pc-remote-ico">
                  <rect x="2" y="4" width="20" height="7" rx="2" />
                  <rect x="2" y="13" width="20" height="7" rx="2" />
                </svg>
                <span>{{ binding.remote.configName || binding.remote.host }}</span>
                <em class="mono-text">{{ binding.remote.user }}@{{ binding.remote.host }}</em>
              </div>
              <div class="pc-path" title="点击复制路径" @click="copyPath">
                {{ binding.remote ? '📁' : '📂' }} <span class="mono-text">{{ binding.path }}</span>
                <span class="pc-path-copy">复制</span>
              </div>
            </div>
          </div>

          <!-- git 管理：检测到仓库才显示（拉取代码 / 切换分支） -->
          <div v-if="gitInfo.isRepo" class="pc-git-ops">
            <span class="pc-git-branch" :title="`当前分支：${gitInfo.branch}`">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="6" y1="3" x2="6" y2="15" />
                <circle cx="18" cy="6" r="3" />
                <circle cx="6" cy="18" r="3" />
                <path d="M18 9a9 9 0 0 1-9 9" />
              </svg>
              <span class="mono-text">{{ gitInfo.branch || '—' }}</span>
            </span>
            <button class="pc-git-btn" type="button" @click="openBranchDialog">切换分支</button>
            <button class="pc-git-btn primary" type="button" :disabled="gitPulling" @click="onGitPull">
              <span v-if="gitPulling" class="pc-env-spin"></span>
              {{ gitPulling ? '拉取中…' : '拉取代码' }}
            </button>
          </div>
        </div>

        <div class="pc-stats">
          <div class="pc-stat">
            <div class="pc-stat-ico" style="background: linear-gradient(135deg, #dbeafe, #eff6ff)">📦</div>
            <div>
              <div class="pc-stat-value">{{ binding.name }}</div>
              <div class="pc-stat-label">项目名称</div>
            </div>
          </div>
          <div class="pc-stat">
            <div class="pc-stat-ico" style="background: linear-gradient(135deg, #d1fae5, #ecfdf5)">🏷</div>
            <div>
              <div class="pc-stat-value mono-text">{{ binding.version || '—' }}</div>
              <div class="pc-stat-label">项目版本</div>
            </div>
          </div>
          <div class="pc-stat">
            <div class="pc-stat-ico" style="background: linear-gradient(135deg, #fef3c7, #fffbeb)">🕐</div>
            <div>
              <div class="pc-stat-value mono-text">{{ fmtTime(binding.boundAt) }}</div>
              <div class="pc-stat-label">绑定时间</div>
            </div>
          </div>
        </div>

        <!-- 环境准备卡片：环境变量 + Hosts（执行部署脚本前的前置动作，位于基础服务之前） -->
        <div class="pc-prep">
          <div class="pc-prep-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
            </svg>
          </div>
          <div class="pc-prep-text">
            <div class="pc-prep-title">
              环境准备
              <span v-if="prepReady" class="pc-prep-ok">✅ 已就绪</span>
            </div>
            <div class="pc-prep-desc">部署脚本执行前，建议先完成 hosts 初始化、容器网段创建、数据库初始化与环境变量配置</div>
          </div>
          <div class="pc-prep-ops">
            <button v-if="hostsInitState.initialized" class="pc-btn done" disabled title="项目 hosts 映射已全部写入系统 /etc/hosts">
              ✅ 已初始化Hosts
            </button>
            <button v-else class="pc-btn primary" @click="onHostsInit">
              {{ runningOf((a) => a.type === 'hostsInit') ? '⏳ 查看运行日志' : '🌐 初始化Hosts' }}
            </button>
            <template v-if="networkState?.name">
              <button
                v-if="networkState.exists"
                class="pc-btn done"
                disabled
                :title="`容器网络 ${networkState.name} 已存在于 docker`"
              >
                ✅ 已添加容器网段
              </button>
              <button v-else class="pc-btn primary" @click="onNetworkCreate">
                {{ runningOf((a) => a.type === 'networkCreate') ? '⏳ 查看运行日志' : '🔗 添加容器网段' }}
              </button>
            </template>
            <template v-if="dbInitState.count > 0">
              <button
                v-if="dbInitState.initialized"
                class="pc-btn done"
                disabled
                :title="`已完成初始化（标记：maozi-cloud-develop-admin/.db-init.json，已被 git 忽略）`"
              >
                ✅ 已初始化数据库
              </button>
              <button v-else class="pc-btn primary" @click="onDbInit">
                {{ runningOf((a) => a.type === 'dbInit') ? '⏳ 查看运行日志' : '🗄 初始化数据库' }}
              </button>
            </template>
            <button class="pc-btn plain" @click="openEnvSettings">🔧 环境设置</button>
          </div>
        </div>

        <!-- 基础服务（docker compose）：环形仪表汇总 + 筛选栏 + 品牌光晕服务卡片网格 -->
        <div class="pc-compose">
          <div class="pc-compose-head">
            <div class="pc-compose-title">
              <div class="pc-compose-badge">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="7" height="7" rx="1.8" />
                  <rect x="14" y="3" width="7" height="7" rx="1.8" />
                  <rect x="3" y="14" width="7" height="7" rx="1.8" />
                  <rect x="14" y="14" width="7" height="7" rx="1.8" />
                </svg>
              </div>
              <div class="pc-compose-heading">
                <div class="pc-compose-name">
                  基础服务
                  <span class="pc-compose-sub">docker compose</span>
                  <span class="pc-compose-live mono-text" :class="{ idle: runningServiceCount === 0 }">
                    <i></i>{{ runningServiceCount }}/{{ composeServices.length }} 运行
                  </span>
                </div>
                <div class="pc-compose-desc">中间件容器编排 · 一键启停 · 实时资源监控</div>
              </div>
            </div>
            <div class="pc-compose-head-ops">
              <button class="pc-compose-refresh" type="button" title="刷新运行状态" @click="loadComposeStatuses">
                <span class="refresh-ico" :class="{ spinning: statusLoading }">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 12a9 9 0 0 1 15.3-6.4L21 8" />
                    <path d="M21 3v5h-5" />
                    <path d="M21 12a9 9 0 0 1-15.3 6.4L3 16" />
                    <path d="M3 21v-5h5" />
                  </svg>
                </span>
              </button>
              <button class="svc-all-btn" type="button" @click="onCompose('all', 'start')">
                <svg viewBox="0 0 24 24"><path d="M8 5.6v12.8L19 12z" /></svg>
                全部启动
              </button>
              <button class="svc-all-btn stop" type="button" @click="onCompose('all', 'stop')">
                <svg viewBox="0 0 24 24"><rect x="6.5" y="6.5" width="11" height="11" rx="2.2" /></svg>
                全部关闭
              </button>
              <button
                class="pc-compose-refresh pc-collapse-btn"
                type="button"
                :title="basicsCollapsed ? '展开面板' : '收起面板'"
                @click="toggleBasicsCollapsed"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" class="pc-collapse-ico" :class="{ folded: basicsCollapsed }">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
            </div>
          </div>

          <div v-show="!basicsCollapsed" class="pc-collapse-body">
          <!-- 汇总仪表卡：环形进度（conic-gradient，占比按全核/配额归一）+ 关键数值 -->
          <div v-if="composeServices.length" class="pc-sum">
            <div class="pc-sum-card">
              <div
                class="pc-ring"
                :class="totalLevel(cpuBarValue)"
                :style="cpuRingStyle"
                title="环形占比 = 容器 CPU 合计 / 全部核心"
              >
                <div class="pc-ring-core">
                  <span class="pc-ring-val mono-text" :class="{ waiting: !statsLoaded }">
                    <template v-if="statsLoaded">{{ statsTotal.cpu.toFixed(1) }}%</template>
                    <template v-else>—</template>
                  </span>
                </div>
              </div>
              <div class="pc-sum-side">
                <span class="pc-sum-label">
                  <span class="pc-sum-ico cpu">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                    </svg>
                  </span>
                  总 CPU
                </span>
                <span class="pc-sum-note">
                  <template v-if="statsLoaded">{{ hostCpuCount }} 核 · 全核归一 {{ cpuBarValue.toFixed(0) }}%</template>
                  <template v-else>{{ hostCpuCount }} 核 · 采集占位</template>
                </span>
              </div>
            </div>

            <div class="pc-sum-card">
              <div
                class="pc-ring"
                :class="totalLevel(memBarValue)"
                :style="memRingStyle"
                title="环形占比 = 已用内存 / 分母（配额合计或物理内存）"
              >
                <div class="pc-ring-core">
                  <span class="pc-ring-val mono-text" :class="{ waiting: !statsLoaded }">
                    <template v-if="statsLoaded">{{ fmtBytes(statsTotal.memUsed).replace(' ', '') }}</template>
                    <template v-else>—</template>
                  </span>
                </div>
              </div>
              <div class="pc-sum-side">
                <span class="pc-sum-label">
                  <span class="pc-sum-ico mem">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="4" y="4" width="16" height="16" rx="2" />
                      <rect x="9" y="9" width="6" height="6" />
                      <path d="M9 2v2M15 2v2M9 20v2M15 20v2M2 9h2M2 15h2M20 9h2M20 15h2" />
                    </svg>
                  </span>
                  总内存
                </span>
                <span class="pc-sum-note">
                  <template v-if="statsLoaded">
                    {{ fmtBytes(statsTotal.memUsed) }}<em v-if="memTotalDenom > 0"> / {{ fmtBytes(memTotalDenom) }}</em>
                    · {{ memUnlimited ? '未设上限' : '配额合计' }}
                  </template>
                  <template v-else>采集占位</template>
                </span>
              </div>
            </div>

            <div class="pc-sum-card">
              <div class="pc-sum-card-main">
                <div class="pc-ring run" :style="svcRingStyle" :title="`运行比例 ${runningServiceCount}/${composeServices.length}`">
                  <div class="pc-ring-core">
                    <span class="pc-ring-val mono-text">{{ runningServiceCount }}<em>/{{ composeServices.length }}</em></span>
                    <span class="pc-ring-cap">运行中</span>
                  </div>
                </div>
                <div class="pc-sum-side">
                  <span class="pc-sum-label">
                    <span class="pc-sum-ico run">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                      </svg>
                    </span>
                    服务状态
                  </span>
                  <span class="pc-sum-live" :class="{ pending: !statsLoaded }">
                    <i></i>{{ statsLoaded ? '实时 · 3s 轮询' : '采集…' }}
                  </span>
                </div>
              </div>
              <div class="pc-sum-segs">
                <span
                  v-for="s in composeServices"
                  :key="s"
                  class="pc-sum-seg"
                  :class="{ on: isRunning(s) }"
                  :title="`${svcName(s)} · ${stateText(s)}`"
                ></span>
              </div>
            </div>
          </div>

          <!-- 服务列表工具栏：搜索 + 状态筛选（全部 / 运行中 / 已停止） -->
          <div v-if="composeServices.length" class="pc-svc-bar">
            <div class="pc-svc-bar-title">
              服务列表<span class="pc-svc-bar-count mono-text">{{ filteredServices.length }}/{{ composeServices.length }}</span>
            </div>
            <div class="pc-svc-search">
              <svg class="pc-svc-search-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
              <input v-model="svcSearch" type="text" placeholder="搜索服务…" />
              <button v-if="svcSearch" class="pc-svc-search-clear" type="button" title="清空" @click="svcSearch = ''">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="m6 6 12 12M18 6 6 18" />
                </svg>
              </button>
            </div>
            <div class="pc-filters">
              <button class="pc-filter" :class="{ active: svcFilter === 'all' }" type="button" @click="svcFilter = 'all'">
                全部<em class="mono-text">{{ composeServices.length }}</em>
              </button>
              <button class="pc-filter run" :class="{ active: svcFilter === 'running' }" type="button" @click="svcFilter = 'running'">
                运行中<em class="mono-text">{{ runningServiceCount }}</em>
              </button>
              <button class="pc-filter off" :class="{ active: svcFilter === 'stopped' }" type="button" @click="svcFilter = 'stopped'">
                已停止<em class="mono-text">{{ stoppedServiceCount }}</em>
              </button>
            </div>
          </div>

          <!-- 服务卡片：品牌渐变顶栏（点阵纹理）+ 指标双芯片 + 底部操作 -->
          <div v-if="composeServices.length" class="pc-svc-grid">
            <div
              v-for="(s, i) in filteredServices"
              :key="s"
              class="pc-svc"
              :class="{ on: isRunning(s) }"
              :style="{ animationDelay: `${Math.min(i * 0.04, 0.36)}s` }"
            >
              <div class="pc-svc-top" :style="svcBandStyle(s)">
                <div class="pc-svc-ava" :style="svcAvaStyle(s)">
                  {{ svcVisual(s).icon || svcName(s).charAt(0).toUpperCase() }}
                </div>
                <div class="pc-svc-title">
                  <span class="pc-svc-name mono-text" :title="s">{{ svcName(s) }}</span>
                  <span class="pc-svc-state" :class="stateClass(s)"><i></i>{{ stateText(s) }}</span>
                </div>
              </div>

              <div class="pc-svc-metrics">
                <div class="pc-svc-metric">
                  <div class="pc-svc-mrow">
                    <span class="pc-svc-mlabel">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                      </svg>
                      CPU
                    </span>
                    <b
                      class="mono-text"
                      :class="isRunning(s) && composeStats[s] ? svcCpuLevel(composeStats[s]) : 'na'"
                      :title="
                        isRunning(s) && composeStats[s]
                          ? composeStats[s].cpusLimit > 0
                            ? `CPU ${composeStats[s].cpuPercent.toFixed(1)}%（配额 ${composeStats[s].cpusLimit} 核）`
                            : `CPU ${composeStats[s].cpuPercent.toFixed(1)}%（单核语义，可超 100%，未设配额）`
                          : '服务未运行'
                      "
                      >{{
                        isRunning(s) && composeStats[s] ? `${composeStats[s].cpuPercent.toFixed(1)}%` : '—'
                      }}</b
                    >
                  </div>
                  <div class="pc-svc-track">
                    <span
                      v-if="isRunning(s) && composeStats[s] && svcCpuPct(composeStats[s]) >= 1"
                      class="pc-svc-fill cpu"
                      :class="svcCpuLevel(composeStats[s])"
                      :style="{ width: svcCpuBar(composeStats[s]) }"
                    ></span>
                  </div>
                </div>
                <div class="pc-svc-metric">
                  <div class="pc-svc-mrow">
                    <span class="pc-svc-mlabel">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="4" y="4" width="16" height="16" rx="2" />
                        <rect x="9" y="9" width="6" height="6" />
                        <path d="M9 2v2M15 2v2M9 20v2M15 20v2M2 9h2M2 15h2M20 9h2M20 15h2" />
                      </svg>
                      内存
                    </span>
                    <b
                      class="mono-text"
                      :class="isRunning(s) && composeStats[s] ? svcMemLevel(composeStats[s]) : 'na'"
                      :title="
                        isRunning(s) && composeStats[s]
                          ? composeStats[s].memLimit > 0
                            ? `内存 ${fmtBytes(composeStats[s].memUsed)} / 配额 ${fmtBytes(composeStats[s].memLimit)}`
                            : `内存 ${fmtBytes(composeStats[s].memUsed)} · 未设容器上限`
                          : '服务未运行'
                      "
                      >{{
                        isRunning(s) && composeStats[s] ? fmtBytes(composeStats[s].memUsed) : '—'
                      }}</b
                    >
                  </div>
                  <div class="pc-svc-track">
                    <span
                      v-if="isRunning(s) && composeStats[s] && svcMemPct(composeStats[s]) >= 1"
                      class="pc-svc-fill mem"
                      :class="svcMemLevel(composeStats[s])"
                      :style="{ width: svcMemBar(composeStats[s]) }"
                    ></span>
                  </div>
                </div>
              </div>

              <div class="pc-svc-foot">
                <button class="pc-svc-btn log" type="button" @click="openServiceLog(s)">
                  <svg viewBox="0 0 24 24">
                    <path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
                  </svg>
                  <span>日志</span>
                </button>
                <button v-if="!isRunning(s)" class="pc-svc-btn start" type="button" @click="onCompose(s, 'start')">
                  <svg viewBox="0 0 24 24"><path d="M8 5.6v12.8L19 12z" /></svg>
                  <span>启动</span>
                </button>
                <template v-else>
                  <button class="pc-svc-btn restart" type="button" title="移除容器并重建" @click="onCompose(s, 'restart')">
                    <svg viewBox="0 0 24 24">
                      <path d="M17.65 6.35A7.95 7.95 0 0 0 12 4a8 8 0 1 0 7.73 10h-2.08A6 6 0 1 1 12 6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" />
                    </svg>
                    <span>重启</span>
                  </button>
                  <button class="pc-svc-btn stop" type="button" @click="onCompose(s, 'stop')">
                    <svg viewBox="0 0 24 24"><rect x="6.5" y="6.5" width="11" height="11" rx="2.2" /></svg>
                    <span>停止</span>
                  </button>
                </template>
              </div>
            </div>

            <div v-if="filteredServices.length === 0" class="pc-svc-none">
              {{ svcNoneText }}
            </div>
          </div>

          <div v-else class="pc-compose-empty">
            <div class="pc-compose-empty-ico">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="7" height="7" rx="1.8" />
                <rect x="14" y="3" width="7" height="7" rx="1.8" />
                <rect x="3" y="14" width="7" height="7" rx="1.8" />
                <rect x="14" y="14" width="7" height="7" rx="1.8" />
              </svg>
            </div>
            <div class="pc-compose-empty-title">未找到 docker-compose 服务定义</div>
            <div class="pc-compose-empty-sub">
              {{ composeError || '请确认绑定的项目中包含 docker-compose.yml 编排文件' }}
            </div>
          </div>
          </div>
        </div>

        <!-- 应用服务：单体 / 微服务 Tab 互斥切换（选中状态持久化），服务卡片复用基础服务样式 -->
        <div class="pc-compose">
          <div class="pc-compose-head">
            <div class="pc-compose-title">
              <div class="pc-compose-badge pas-badge">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="4" width="18" height="6" rx="2" />
                  <rect x="3" y="14" width="18" height="6" rx="2" />
                  <circle cx="7" cy="7" r="0.8" fill="currentColor" />
                  <circle cx="7" cy="17" r="0.8" fill="currentColor" />
                </svg>
              </div>
              <div class="pc-compose-heading">
                <div class="pc-compose-name">
                  应用服务
                  <span class="pc-compose-sub">monomer · distributeds</span>
                  <span class="pc-compose-live mono-text" :class="{ idle: appSvcRunningCount(appSvcTab) === 0 }">
                    <i></i>{{ appSvcRunningCount(appSvcTab) }}/{{ activeAppSvcServices.length }} 运行
                  </span>
                </div>
                <div class="pc-compose-desc">单体与微服务互斥运行 · 启动一侧将自动关闭另一侧全部服务</div>
              </div>
            </div>
            <div class="pc-compose-head-ops">
              <button class="pc-compose-refresh" type="button" title="刷新运行状态" @click="loadAppSvcStatuses">
                <span class="refresh-ico">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 12a9 9 0 0 1 15.3-6.4L21 8" />
                    <path d="M21 3v5h-5" />
                    <path d="M21 12a9 9 0 0 1-15.3 6.4L3 16" />
                    <path d="M3 21v-5h5" />
                  </svg>
                </span>
              </button>
              <button class="svc-all-btn" type="button" @click="onAppServiceAll('start')">
                <svg viewBox="0 0 24 24"><path d="M8 5.6v12.8L19 12z" /></svg>
                全部启动
              </button>
              <button class="svc-all-btn stop" type="button" @click="onAppServiceAll('stop')">
                <svg viewBox="0 0 24 24"><rect x="6.5" y="6.5" width="11" height="11" rx="2.2" /></svg>
                全部关闭
              </button>
              <button
                class="pc-compose-refresh pc-collapse-btn"
                type="button"
                :title="appSvcCollapsed ? '展开面板' : '收起面板'"
                @click="toggleAppSvcCollapsed"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" class="pc-collapse-ico" :class="{ folded: appSvcCollapsed }">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
            </div>
          </div>

          <div v-show="!appSvcCollapsed" class="pc-collapse-body">

          <!-- Tab：单体 / 微服务（选中持久化） -->
          <div class="pas-tabs">
            <button class="pas-tab" :class="{ active: appSvcTab === 'monomer' }" type="button" @click="switchAppSvcTab('monomer')">
              🧩 单体服务
              <em class="mono-text">{{ appSvcRunningCount('monomer') }}/{{ appServices.monomer.length }}</em>
            </button>
            <button class="pas-tab" :class="{ active: appSvcTab === 'distributeds' }" type="button" @click="switchAppSvcTab('distributeds')">
              ☁️ 微服务
              <em class="mono-text">{{ appSvcRunningCount('distributeds') }}/{{ appServices.distributeds.length }}</em>
            </button>
            <span class="pas-tabs-hint">Tab 选中状态已本地记忆 · 两变体互斥运行</span>
          </div>

          <!-- 汇总仪表卡：当前 Tab 变体的 总CPU / 总内存 / 服务状态（与基础服务同款环形仪表） -->
          <div v-if="activeAppSvcServices.length" class="pc-sum">
            <div class="pc-sum-card">
              <div
                class="pc-ring"
                :class="totalLevel(appSvcCpuBarValue)"
                :style="appSvcCpuRingStyle"
                title="环形占比 = 容器 CPU 合计 / 全部核心"
              >
                <div class="pc-ring-core">
                  <span class="pc-ring-val mono-text" :class="{ waiting: !appStatsLoaded }">
                    <template v-if="appStatsLoaded">{{ appSvcTotal.cpu.toFixed(1) }}%</template>
                    <template v-else>—</template>
                  </span>
                </div>
              </div>
              <div class="pc-sum-side">
                <span class="pc-sum-label">
                  <span class="pc-sum-ico cpu">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                    </svg>
                  </span>
                  总 CPU
                </span>
                <span class="pc-sum-note">
                  <template v-if="appStatsLoaded">{{ hostCpuCount }} 核 · 全核归一 {{ appSvcCpuBarValue.toFixed(0) }}%</template>
                  <template v-else>{{ hostCpuCount }} 核 · 采集占位</template>
                </span>
              </div>
            </div>

            <div class="pc-sum-card">
              <div
                class="pc-ring"
                :class="totalLevel(appSvcMemBarValue)"
                :style="appSvcMemRingStyle"
                title="环形占比 = 已用内存 / 分母（配额合计或物理内存）"
              >
                <div class="pc-ring-core">
                  <span class="pc-ring-val mono-text" :class="{ waiting: !appStatsLoaded }">
                    <template v-if="appStatsLoaded">{{ fmtBytes(appSvcTotal.memUsed).replace(' ', '') }}</template>
                    <template v-else>—</template>
                  </span>
                </div>
              </div>
              <div class="pc-sum-side">
                <span class="pc-sum-label">
                  <span class="pc-sum-ico mem">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="4" y="4" width="16" height="16" rx="2" />
                      <rect x="9" y="9" width="6" height="6" />
                      <path d="M9 2v2M15 2v2M9 20v2M15 20v2M2 9h2M2 15h2M20 9h2M20 15h2" />
                    </svg>
                  </span>
                  总内存
                </span>
                <span class="pc-sum-note">
                  <template v-if="appStatsLoaded">
                    {{ fmtBytes(appSvcTotal.memUsed) }}<em v-if="appSvcMemDenom > 0"> / {{ fmtBytes(appSvcMemDenom) }}</em>
                  </template>
                  <template v-else>采集占位</template>
                </span>
              </div>
            </div>

            <div class="pc-sum-card">
              <div class="pc-sum-card-main">
                <div
                  class="pc-ring run"
                  :style="appSvcRingStyle"
                  :title="`运行比例 ${appSvcRunningCount(appSvcTab)}/${activeAppSvcServices.length}`"
                >
                  <div class="pc-ring-core">
                    <span class="pc-ring-val mono-text">
                      {{ appSvcRunningCount(appSvcTab) }}<em>/{{ activeAppSvcServices.length }}</em>
                    </span>
                    <span class="pc-ring-cap">运行中</span>
                  </div>
                </div>
                <div class="pc-sum-side">
                  <span class="pc-sum-label">
                    <span class="pc-sum-ico run">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                      </svg>
                    </span>
                    服务状态
                  </span>
                  <span class="pc-sum-live"><i></i>{{ APP_SVC_LABELS[appSvcTab] }} · 实时</span>
                </div>
              </div>
              <div class="pc-sum-segs">
                <span
                  v-for="x in activeAppSvcServices"
                  :key="x.name"
                  class="pc-sum-seg"
                  :class="{ on: isAppSvcRunning(appSvcTab, x.name) }"
                  :title="`${appSvcName(x.name)} · ${isAppSvcRunning(appSvcTab, x.name) ? '运行中' : '已停止'}`"
                ></span>
              </div>
            </div>
          </div>

          <!-- 部署栏：编译 + 启动走项目部署脚本，随 Tab 变体切换，对侧容器互斥 -->
          <div class="pas-deploy">
            <span class="pas-deploy-hint">
              maven 编译 + docker compose 启动 · {{ APP_SVC_LABELS[OTHER_APP_VARIANT[appSvcTab]] }}容器运行中时将提示先关闭
            </span>
            <template v-if="appSvcTab === 'distributeds'">
              <button class="pc-btn success" @click="onRunScript('admin')">
                {{ runningOf((a) => a.type === 'script' && a.kind === 'admin') ? '⏳ 查看运行日志' : '🖥️ 后台编译启动' }}
              </button>
              <button class="pc-btn violet" @click="onRunScript('demand')">
                {{ runningOf((a) => a.type === 'script' && a.kind === 'demand') ? '⏳ 查看运行日志' : '⚡ 微服务按需编译启动' }}
              </button>
              <button class="pc-btn primary" @click="onRunScript('all')">
                {{ runningOf((a) => a.type === 'script' && a.kind === 'all') ? '⏳ 查看运行日志' : '🧱 微服务全量编译启动' }}
              </button>
            </template>
            <template v-else>
              <button class="pc-btn success" @click="onRunScript('monomerAdmin')">
                {{ runningOf((a) => a.type === 'script' && a.kind === 'monomerAdmin') ? '⏳ 查看运行日志' : '🖥️ 后台编译启动' }}
              </button>
              <button class="pc-btn primary" @click="onRunScript('monomerServices')">
                {{ runningOf((a) => a.type === 'script' && a.kind === 'monomerServices') ? '⏳ 查看运行日志' : '🧱 单体服务编译启动' }}
              </button>
            </template>
          </div>

          <!-- 当前变体服务卡片 -->
          <div v-if="activeAppSvcServices.length" class="pc-svc-grid">
            <div
              v-for="(s, i) in activeAppSvcServices"
              :key="s.name"
              class="pc-svc"
              :class="{ on: isAppSvcRunning(appSvcTab, s.name) }"
              :style="{ animationDelay: `${Math.min(i * 0.04, 0.36)}s` }"
            >
              <div class="pc-svc-top" :style="svcBandStyle(s.name)">
                <div class="pc-svc-ava" :style="svcAvaStyle(s.name)">
                  {{ svcVisual(s.name).icon || appSvcName(s.name).charAt(0).toUpperCase() }}
                </div>
                <div class="pc-svc-title">
                  <span class="pc-svc-name mono-text" :title="s.name">{{ appSvcName(s.name) }}</span>
                  <span class="pc-svc-state" :class="isAppSvcRunning(appSvcTab, s.name) ? 'on' : 'off'">
                    <i></i>{{ isAppSvcRunning(appSvcTab, s.name) ? '运行中' : '已停止' }}
                  </span>
                </div>
              </div>
              <div class="pc-svc-metrics">
                <div class="pc-svc-metric">
                  <div class="pc-svc-mrow">
                    <span class="pc-svc-mlabel">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                      </svg>
                      CPU
                    </span>
                    <b
                      class="mono-text"
                      :class="isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name) ? svcCpuLevel(appStat(appSvcTab, s.name)!) : 'na'"
                      :title="
                        isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name)
                          ? appStat(appSvcTab, s.name)!.cpusLimit > 0
                            ? `CPU ${appStat(appSvcTab, s.name)!.cpuPercent.toFixed(1)}%（配额 ${appStat(appSvcTab, s.name)!.cpusLimit} 核）`
                            : `CPU ${appStat(appSvcTab, s.name)!.cpuPercent.toFixed(1)}%（单核语义，可超 100%，未设配额）`
                          : '服务未运行'
                      "
                      >{{
                        isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name) ? `${appStat(appSvcTab, s.name)!.cpuPercent.toFixed(1)}%` : '—'
                      }}</b
                    >
                  </div>
                  <div class="pc-svc-track">
                    <span
                      v-if="isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name) && svcCpuPct(appStat(appSvcTab, s.name)!) >= 1"
                      class="pc-svc-fill cpu"
                      :class="svcCpuLevel(appStat(appSvcTab, s.name)!)"
                      :style="{ width: svcCpuBar(appStat(appSvcTab, s.name)!) }"
                    ></span>
                  </div>
                </div>
                <div class="pc-svc-metric">
                  <div class="pc-svc-mrow">
                    <span class="pc-svc-mlabel">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="4" y="4" width="16" height="16" rx="2" />
                        <rect x="9" y="9" width="6" height="6" />
                        <path d="M9 2v2M15 2v2M9 20v2M15 20v2M2 9h2M2 15h2M20 9h2M20 15h2" />
                      </svg>
                      内存
                    </span>
                    <b
                      class="mono-text"
                      :class="isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name) ? svcMemLevel(appStat(appSvcTab, s.name)!) : 'na'"
                      :title="
                        isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name)
                          ? appStat(appSvcTab, s.name)!.memLimit > 0
                            ? `内存 ${fmtBytes(appStat(appSvcTab, s.name)!.memUsed)} / 配额 ${fmtBytes(appStat(appSvcTab, s.name)!.memLimit)}`
                            : `内存 ${fmtBytes(appStat(appSvcTab, s.name)!.memUsed)} · 未设容器上限`
                          : '服务未运行'
                      "
                      >{{
                        isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name) ? fmtBytes(appStat(appSvcTab, s.name)!.memUsed) : '—'
                      }}</b
                    >
                  </div>
                  <div class="pc-svc-track">
                    <span
                      v-if="isAppSvcRunning(appSvcTab, s.name) && appStat(appSvcTab, s.name) && svcMemPct(appStat(appSvcTab, s.name)!) >= 1"
                      class="pc-svc-fill mem"
                      :class="svcMemLevel(appStat(appSvcTab, s.name)!)"
                      :style="{ width: svcMemBar(appStat(appSvcTab, s.name)!) }"
                    ></span>
                  </div>
                </div>
              </div>
              <div class="pc-svc-foot">
                <button class="pc-svc-btn log" type="button" @click="openServiceLog(s.name, `${appSvcTab}:${s.file}`)">
                  <svg viewBox="0 0 24 24">
                    <path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" />
                  </svg>
                  <span>日志</span>
                </button>
                <button
                  v-if="!isAppSvcRunning(appSvcTab, s.name)"
                  class="pc-svc-btn start"
                  type="button"
                  @click="onAppService(s, 'start')"
                >
                  <svg viewBox="0 0 24 24"><path d="M8 5.6v12.8L19 12z" /></svg>
                  <span>启动</span>
                </button>
                <template v-else>
                  <button class="pc-svc-btn restart" type="button" @click="onAppService(s, 'restart')">
                    <svg viewBox="0 0 24 24">
                      <path d="M17.65 6.35A7.95 7.95 0 0 0 12 4a8 8 0 1 0 7.73 10h-2.08A6 6 0 1 1 12 6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" />
                    </svg>
                    <span>重启</span>
                  </button>
                  <button class="pc-svc-btn stop" type="button" @click="onAppService(s, 'stop')">
                    <svg viewBox="0 0 24 24"><rect x="6.5" y="6.5" width="11" height="11" rx="2.2" /></svg>
                    <span>停止</span>
                  </button>
                </template>
              </div>
            </div>
          </div>
          <div v-else class="pc-compose-empty">
            <div class="pc-compose-empty-ico">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="6" rx="2" />
                <rect x="3" y="14" width="18" height="6" rx="2" />
              </svg>
            </div>
            <div class="pc-compose-empty-title">未找到{{ APP_SVC_LABELS[appSvcTab] }} compose 定义</div>
            <div class="pc-compose-empty-sub">
              请确认 maozi-cloud-{{ appSvcTab === 'monomer' ? 'monomer' : 'distributeds' }}-docker 目录下的 compose 文件存在
            </div>
          </div>

          <!-- 全链路日志：仅微服务 Tab，放在服务列表下方 -->
          <div v-if="appSvcTab === 'distributeds' && activeAppSvcServices.length" class="pas-trace-row">
            <span class="pas-trace-hint">输入链路 ID，跨所有运行中的微服务检索日志</span>
            <button class="pc-btn cyan" @click="openTraceQuery">🔍 全链路日志查询</button>
          </div>
          </div>
        </div>

        <div class="pc-ops">
          <button
            v-if="sessions.length"
            class="pc-log-btn"
            :class="logBtnState"
            type="button"
            title="查看脚本执行日志"
            @click="scriptVisible = true"
          >
            <span class="pc-log-btn-core">
              <span class="pc-log-btn-ring"></span>
              <span class="pc-log-btn-ico">{{ logBtnIcon }}</span>
              <span v-if="runningSessionCount" class="pc-log-btn-badge">{{ runningSessionCount }}</span>
            </span>
            <span class="pc-log-btn-text">
              <span class="pc-log-btn-title">执行日志</span>
              <span class="pc-log-btn-sub">{{ logStatusText }}</span>
            </span>
          </button>
          <button class="pc-btn danger-ghost" @click="onUnbind">解绑</button>
        </div>
      </div>

      <!-- 未绑定 -->
      <div v-else class="pc-unbound">
        <div class="pc-unbound-title">选择绑定方式</div>
        <div class="pc-unbound-sub">绑定后展示项目名称与版本号，随时可解绑重新绑定</div>
        <div class="pc-options">
          <!-- 步骤一：选择本地 / 远程 -->
          <template v-if="!bindMode">
            <div class="pc-option" @click="bindMode = 'local'">
              <div class="pc-option-no">01</div>
              <div class="pc-option-ico dir">💻</div>
              <div class="pc-option-title">本地项目</div>
              <div class="pc-option-desc">在本地磁盘选择项目目录或拉取代码</div>
              <div class="pc-option-go">选择本地 →</div>
            </div>
            <div class="pc-option" @click="bindMode = 'remote'; loadSshConfigs()">
              <div class="pc-option-no">02</div>
              <div class="pc-option-ico ssh">🖥️</div>
              <div class="pc-option-title">远程服务器</div>
              <div class="pc-option-desc">SSH 登录远程 Linux 服务器绑定或拉取代码</div>
              <div class="pc-option-go">选择远程 →</div>
            </div>
          </template>
          <!-- 步骤二 -->
          <template v-else>
            <div class="pc-bind-back" @click="bindMode = ''; sshConnected = false">← 返回上一步</div>

            <!-- 远程：先连接 SSH -->
            <div v-if="bindMode === 'remote' && !sshConnected" class="pc-ssh-hero">
              <!-- 背景装饰 -->
              <div class="pc-ssh-hero-glow"></div>
              <div class="pc-ssh-hero-grid"></div>

              <!-- 顶部：标题 + 终端状态 -->
              <div class="pc-ssh-hero-top">
                <div class="pc-ssh-hero-left">
                  <div class="pc-ssh-hero-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="2" y="3" width="20" height="6" rx="2" />
                      <rect x="2" y="11" width="20" height="6" rx="2" />
                      <path d="M6 6h.01M6 14h.01" stroke-width="2.5" stroke-linecap="round" />
                    </svg>
                  </div>
                  <div>
                    <div class="pc-ssh-hero-title">SSH 远程连接</div>
                    <div class="pc-ssh-hero-sub">选择服务器 → 建立连接 → 浏览目录或拉取代码</div>
                  </div>
                </div>
                <div class="pc-ssh-hero-status" :class="{ connecting: sshTesting, error: !!sshError }">
                  <span class="pc-ssh-hero-status-dot"></span>
                  <span>{{ sshTesting ? 'CONNECTING' : sshError ? 'FAILED' : 'READY' }}</span>
                </div>
              </div>

              <!-- 服务器列表 -->
              <div class="pc-ssh-hero-list">
                <div v-if="sshConfigs.length === 0" class="pc-ssh-hero-empty">
                  <div class="pc-ssh-hero-empty-ico">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="2" y="3" width="20" height="6" rx="2" />
                      <rect x="2" y="11" width="20" height="6" rx="2" />
                    </svg>
                  </div>
                  <div class="pc-ssh-hero-empty-title">暂无 Linux 服务器密钥</div>
                  <div class="pc-ssh-hero-empty-sub">到「密钥管理」添加 → 类型选 Linux 🐧 → 填写地址与凭据</div>
                </div>
                <div
                  v-for="c in sshConfigs"
                  :key="c.id"
                  class="pc-ssh-srv"
                  :class="{ selected: sshConfigId === c.id, testing: sshTesting && sshConfigId === c.id }"
                  @click="sshConfigId = c.id; sshError = ''"
                  @dblclick="sshConfigId = c.id; testSshFromBind()"
                >
                  <div class="pc-ssh-srv-led"><span></span></div>
                  <div class="pc-ssh-srv-body">
                    <div class="pc-ssh-srv-name">{{ c.name }}</div>
                    <div class="pc-ssh-srv-addr mono-text">{{ c.address || '未设置地址' }}</div>
                  </div>
                  <div class="pc-ssh-srv-right">
                    <span v-if="sshTesting && sshConfigId === c.id" class="pc-ssh-srv-spin"></span>
                    <svg v-else-if="sshConfigId === c.id" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="pc-ssh-srv-check">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="pc-ssh-srv-arrow">
                      <path d="m9 18 6-6-6-6" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- 操作 -->
              <div v-if="sshConfigs.length > 0" class="pc-ssh-hero-actions">
                <button
                  class="pc-ssh-connect"
                  :disabled="!sshConfigId || sshTesting"
                  @click="testSshFromBind"
                >
                  <span v-if="sshTesting" class="pc-ssh-connect-spin"></span>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pc-ssh-connect-ico">
                    <path d="M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7" />
                    <path d="M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7" />
                  </svg>
                  {{ sshTesting ? '正在连接…' : '建立 SSH 连接' }}
                </button>
                <span class="pc-ssh-hero-tip">单击选中 · 双击直接连接</span>
              </div>

              <!-- 错误 -->
              <div v-if="sshError" class="pc-ssh-hero-err">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
                  <line x1="12" y1="9" x2="12" y2="13" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                <span>{{ sshError }}</span>
              </div>
            </div>

          
            <template v-if="bindMode === 'local' || sshConnected">
              <div class="pc-option" @click="bindMode === 'local' ? onPickDir() : openSshBrowser()">
                <div class="pc-option-no">01</div>
                <div class="pc-option-ico dir">📂</div>
                <div class="pc-option-title">选择目录</div>
                <div class="pc-option-desc">
                  {{ bindMode === 'local' ? '读取目录下 CONFIG 文件完成绑定' : '浏览远程目录，读取 CONFIG 完成绑定' }}
                </div>
                <div class="pc-option-go">开始绑定 →</div>
              </div>
              <div class="pc-option" @click="bindMode === 'local' ? openCloneDialog() : openSshCloneBrowser()">
                <div class="pc-option-no">02</div>
                <div class="pc-option-ico pull">⬇️</div>
                <div class="pc-option-title">拉取代码</div>
                <div class="pc-option-desc">
                  {{ bindMode === 'local' ? '选择 Git 密钥与目标目录，git clone 后自动绑定' : '选择远程父目录与 Git 密钥，SSH 在远程执行 git clone' }}
                </div>
                <div class="pc-option-go">开始拉取 →</div>
              </div>
            </template>
          </template>
        </div>
      </div>
    </div>

    <!-- ===== 拉取代码弹窗 ===== -->
    <el-dialog
      v-model="cloneVisible"
      width="640px"
      draggable
      append-to-body
      modal-class="pc-dlg"
      :show-close="false"
      :close-on-click-modal="false"
    >
      <template #header>
        <div class="pcd-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">⬇️</div>
            <div>
              <div class="pcd-title">拉取代码并绑定</div>
              <div class="pcd-subtitle">使用 Git 类型密钥克隆 maozi-cloud 到本地，完成后自动读取 CONFIG 绑定</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="cloneVisible = false">✕</button>
        </div>
      </template>

      <div class="pcd-body">
        <div class="pcd-sec">
          <span class="pcd-sec-ico">🔑</span>
          <span class="pcd-sec-text">选择密钥</span>
          <span class="pcd-sec-line"></span>
        </div>
        <div class="pcd-field">
          <div class="pcd-label">Git 密钥 <span class="pcd-req">*</span></div>
          <el-select v-model="cloneSecretId" placeholder="选择用于拉取代码的 Git 密钥">
            <el-option
              v-for="s in secrets"
              :key="s.id"
              :label="`${s.name}（${(s.authType ?? 'password') === 'key' ? '密钥/Token' : '账密'}${s.password ? '' : ' · 未填写凭据'}）`"
              :value="s.id"
              :disabled="!s.password"
            />
          </el-select>
          <div v-if="secrets.length === 0" class="pcd-hint warn">
            密钥管理中暂无 Git 类型密钥，请先到「密钥管理」添加（类型选择 Git）并填写账号 / 密码
          </div>
        </div>

        <div class="pcd-sec pcd-sec-dest">
          <span class="pcd-sec-ico">📂</span>
          <span class="pcd-sec-text">目标目录</span>
          <span class="pcd-sec-line"></span>
        </div>
        <div class="pcd-field">
          <div class="pcd-label">系统目录 <span class="pcd-req">*</span></div>
          <div class="pcd-dest">
            <el-input v-model="cloneDest" readonly placeholder="选择代码要存放的系统目录" class="mono-cell" />
            <button class="pcd-pick" type="button" @click="pickCloneDest">选择目录</button>
          </div>
          <div class="pcd-hint">代码将克隆到所选目录下的 maozi-cloud 子目录</div>
        </div>

        <!-- git 实时日志 -->
        <div v-if="cloning || cloneLogs.length" class="pcd-log-box">
          <div class="pcd-log-head">
            <span class="pcd-log-dot" :class="{ running: cloning }"></span>
            git 输出
          </div>
          <div class="pcd-log" ref="logBox">
            <div v-for="(l, i) in cloneLogs" :key="i" class="pcd-log-line">{{ l }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <button class="pcd-btn ghost" type="button" @click="cloneVisible = false">取消</button>
          <button class="pcd-btn primary" type="button" :disabled="cloning" @click="startClone">
            <span v-if="cloning" class="pcd-spin"></span>
            {{ cloning ? '拉取中…' : '开始拉取' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 微服务全量启动弹窗 ===== -->
    <el-dialog
      v-model="scriptVisible"
      width="760px"
      draggable
      append-to-body
      modal-class="pc-dlg"
      :show-close="false"
      :close-on-click-modal="false"
      :before-close="onBeforeScriptClose"
    >
      <template #header>
        <div class="pcd-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">{{ scriptIcon }}</div>
            <div>
              <div class="pcd-title">{{ scriptTitle }}</div>
              <div class="pcd-subtitle">{{ scriptSub }}</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="onCloseScript">✕</button>
        </div>
      </template>

      <div v-if="sessions.length > 1" class="pcd-tabs">
        <button
          v-for="s in sessions"
          :key="s.id"
          type="button"
          class="pcd-tab"
          :class="{ active: s.id === activeSid }"
          @click="focusSession(s.id)"
        >
          <span class="pcd-tab-dot" :class="{ running: s.running, ok: !s.running && s.status === 'ok', fail: !s.running && s.status === 'fail' }"></span>
          <span class="pcd-tab-label">{{ s.icon }} {{ s.label }}</span>
          <span v-if="!s.running" class="pcd-tab-x" title="移除该日志" @click.stop="removeSession(s.id)">✕</span>
        </button>
      </div>

      <div class="pcd-body">
        <div class="pcd-log-box">
          <div class="pcd-log-head">
            <span class="pcd-log-dot" :class="{ running: scriptRunning }"></span>
            <span>脚本输出</span>
            <span class="pcd-log-status">
              {{
                scriptRunning
                  ? `运行中 · 已 ${scriptElapsedText}`
                  : scriptStatus === 'ok'
                    ? `已结束 · 成功 · 耗时 ${scriptElapsedText}`
                    : scriptStatus === 'fail'
                      ? `已结束 · 失败 · 耗时 ${scriptElapsedText}`
                      : '待执行'
              }}
            </span>
          </div>
          <div class="pcd-log pcd-log-tall" ref="scriptLogBox">
            <div v-for="(l, i) in scriptLogs" :key="i" class="pcd-log-line">{{ l }}</div>
            <div v-if="scriptLogs.length === 0" class="pcd-log-empty">等待脚本输出…</div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <!-- 拉取代码会话不提供后台运行：克隆需关注结果且很快结束 -->
          <button
            v-if="scriptRunning && activeSession?.action.type !== 'sshClone'"
            class="pcd-btn ghost"
            type="button"
            @click="onRunBackground"
          >
            后台运行
          </button>
          <button class="pcd-btn ghost" type="button" @click="onCloseScript">
            {{ scriptRunning ? '中断执行' : '关闭' }}
          </button>
          <button
            class="pcd-btn primary"
            type="button"
            :disabled="scriptRunning"
            @click="onRerunLast"
          >
            <span v-if="scriptRunning" class="pcd-spin"></span>
            {{ scriptRunning ? '运行中…' : '重新执行' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 环境设置弹窗：进度总览 + 搜索筛选 + 分组卡片（修改写入 shell 配置，自动备份） ===== -->
    <el-dialog
      v-model="envVisible"
      width="780px"
      draggable
      append-to-body
      modal-class="pc-dlg pev-dlg"
      :show-close="false"
      :close-on-click-modal="false"
    >
      <template #header>
        <div class="pcd-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">🔧</div>
            <div>
              <div class="pcd-title">环境设置</div>
              <div class="pcd-subtitle">ENVIRONMENT_VARIABLE · 修改写入 shell 配置文件，部署脚本下次执行即生效</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="envVisible = false">✕</button>
        </div>
      </template>

      <div class="pcd-body">
        <div v-if="envLoading" class="pev-state"><span class="pc-env-spin"></span>正在读取环境变量…</div>
        <div v-else-if="envError" class="pev-state error"><span class="pev-state-ico">⚠️</span>{{ envError }}</div>
        <div v-else-if="envTotalCount === 0" class="pev-state">ENVIRONMENT_VARIABLE 中没有定义变量</div>
        <template v-else>
          <!-- 进度总览：环形就绪度仪表 + 状态徽标 -->
          <div class="pev-hero" :class="{ done: envProgressPct === 100 }">
            <div class="pev-ring" :style="envRingStyle">
              <div class="pev-ring-core">
                <span class="pev-ring-val mono-text">{{ envProgressPct }}<em>%</em></span>
              </div>
            </div>
            <div class="pev-hero-text">
              <div class="pev-hero-title">
                环境变量就绪度
                <span v-if="envProgressPct === 100" class="pev-done-badge">✅ 全部就绪</span>
              </div>
              <div class="pev-hero-sub">
                {{ envSetCount }} / {{ envTotalCount }} 项已配置 · {{ envRemote ? 'SSH 写入远程 shell 配置，自动备份' : '修改写入 shell 配置并自动备份' }}
              </div>
              <div class="pev-hero-chips">
                <span v-if="envMissingCount" class="pev-stat warn">⚠ {{ envMissingCount }} 未设置</span>
                <span v-if="envDisabledCount" class="pev-stat off">{{ envDisabledCount }} 已禁用</span>
                <span class="pev-stat ok">✓ {{ envSetCount }} 已设置</span>
              </div>
            </div>
          </div>

          <!-- 控制条：搜索 + 状态筛选 -->
          <div class="pev-ctrl">
            <div class="pev-search">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pev-search-ico">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
              <input v-model="envSearch" type="text" placeholder="搜索变量名 / 中文名 / 值…" />
              <button v-if="envSearch" class="pev-search-clear" type="button" title="清空" @click="envSearch = ''">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="m6 6 12 12M18 6 6 18" /></svg>
              </button>
            </div>
            <div class="pev-filters">
              <button class="pev-filter" :class="{ active: envFilter === 'all' }" type="button" @click="envFilter = 'all'">
                全部<em class="mono-text">{{ envTotalCount }}</em>
              </button>
              <button v-if="envMissingCount" class="pev-filter miss" :class="{ active: envFilter === 'missing' }" type="button" @click="envFilter = 'missing'">
                未设置<em class="mono-text">{{ envMissingCount }}</em>
              </button>
              <button v-if="envDisabledCount" class="pev-filter off" :class="{ active: envFilter === 'off' }" type="button" @click="envFilter = 'off'">
                已禁用<em class="mono-text">{{ envDisabledCount }}</em>
              </button>
            </div>
          </div>

          <!-- 分组列表：渐变徽章标题 + 分层容器 -->
          <div v-for="(g, gi) in envViewGroups" :key="g.name || gi" class="pev-group">
            <div v-if="g.name" class="pev-group-name">
              <span class="pev-group-badge" :style="{ background: groupGrad(gi) }">{{ g.name.slice(0, 1) }}</span>
              <span class="pev-group-title">{{ g.name }}</span>
              <span class="pev-group-count mono-text">{{ g.items.length }} 项</span>
              <span class="pev-group-line"></span>
            </div>
            <div class="pev-group-body">
              <div
              v-for="(it, ii) in g.items"
              :key="it.key"
              class="pev-item"
              :class="{
                editing: envEditing === it.key,
                ok: it.found && it.enabled,
                missing: !it.found,
                off: it.found && !it.enabled
              }"
              :style="{ animationDelay: `${Math.min(ii * 0.04, 0.24)}s` }"
            >
              <div class="pev-item-main">
                <div class="pev-item-label">
                  <span class="pev-dot"></span>
                  <span class="pev-name">{{ it.label }}</span>
                  <span v-if="isSecretEnv(it)" class="pev-secret" title="敏感变量，默认打码显示">🔒</span>
                </div>
                <span class="pev-key mono-text" :title="it.key">{{ it.key }}</span>
              </div>

              <div class="pev-item-side">
                <template v-if="envEditing === it.key">
                  <el-input
                    v-model="envDraft"
                    size="default"
                    class="pev-input"
                    :show-password="isSecretEnv(it)"
                    :placeholder="`输入 ${it.key} 的值`"
                    :disabled="envSaving"
                    @keyup.enter="saveEnvEdit(it)"
                    @keyup.esc="envEditing = ''"
                  />
                  <button class="pev-save" type="button" :disabled="envSaving" @click="saveEnvEdit(it)">
                    <span v-if="envSaving" class="pc-env-act-spin"></span>
                    <template v-else>✓ 保存</template>
                  </button>
                  <button class="pev-cancel" type="button" :disabled="envSaving" @click="envEditing = ''">取消</button>
                </template>
                <template v-else-if="it.found">
                  <span v-if="it.source" class="pev-src">{{ it.source }}</span>
                  <span
                    class="pev-value mono-text"
                    :class="{ disabled: !it.enabled }"
                    :title="isSecretEnv(it) && !envRevealed.has(it.key) ? '敏感值已打码' : it.value"
                    >{{ envDisplay(it) }}</span
                  >
                  <span v-if="!it.enabled" class="pev-tag off">已禁用</span>
                  <button
                    v-if="isSecretEnv(it)"
                    class="pev-ghost-btn"
                    type="button"
                    :title="envRevealed.has(it.key) ? '隐藏明文' : '显示明文'"
                    @click="toggleEnvReveal(it.key)"
                  >
                    {{ envRevealed.has(it.key) ? '🙈' : '👁' }}
                  </button>
                  <button class="pev-ghost-btn" type="button" title="修改值" @click="startEnvEdit(it)">✏️</button>
                  <button class="pev-ghost-btn" type="button" title="复制值" @click="copyEnvValue(it)">⧉</button>
                </template>
                <template v-else>
                  <span class="pev-tag missing">未设置</span>
                  <button class="pev-set" type="button" @click="startEnvEdit(it)">＋ 设置值</button>
                </template>
              </div>
            </div>
            </div>
          </div>

          <div v-if="envViewGroups.length === 0" class="pev-none">{{ envEmptyText }}</div>
        </template>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <button class="pcd-btn ghost" type="button" :disabled="envLoading" @click="loadEnvSettings">⟳ 刷新</button>
          <button class="pcd-btn primary" type="button" @click="envVisible = false">关闭</button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 服务日志查询弹窗：品牌化头部 + 等级着色控制台 + 快捷等级过滤 ===== -->
    <el-dialog
      v-model="logVisible"
      width="900px"
      draggable
      append-to-body
      modal-class="pc-dlg plg-dlg"
      :show-close="false"
      :close-on-click-modal="false"
      :before-close="onBeforeLogClose"
    >
      <template #header>
        <div class="pcd-header plg-header" :style="logHeaderStyle">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">{{ logHeaderIcon }}</div>
            <div>
              <div class="pcd-title">日志查询 · {{ svcName(logService) || '服务日志' }}</div>
              <div class="pcd-subtitle">docker compose logs · 等级着色 · 关键字过滤 · 实时跟踪</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="onCloseLog">✕</button>
        </div>
      </template>

      <div class="pcd-body">
        <!-- 控制条：数据源（服务/行数）| 过滤 | 操作，浅灰面板 + 白底控件 -->
        <div class="pc-log-toolbar">
          <div class="pc-log-src">
            <el-select v-model="logService" class="pc-log-svc" popper-class="plg-svc-popper" @change="onLogTargetChange">
              <template #prefix>
                <span class="plg-sel-dot" :style="{ background: svcVisual(logService).grad }"></span>
              </template>
              <el-option v-for="opt in logServiceOptions" :key="opt.name" :label="appSvcName(opt.name)" :value="opt.name">
                <span class="plg-opt">
                  <i class="plg-opt-dot" :style="{ background: svcVisual(opt.name).grad }"></i>
                  <span class="mono-text">{{ appSvcName(opt.name) }}</span>
                  <em v-if="logCtx === 'basics'" :class="{ run: isRunning(opt.name) }">{{ isRunning(opt.name) ? '运行' : '停止' }}</em>
                  <em v-else :class="{ run: isAppSvcRunning(logCtx.split(':')[0] as AppSvcVariant, opt.name) }">
                    {{ isAppSvcRunning(logCtx.split(':')[0] as AppSvcVariant, opt.name) ? '运行' : '停止' }}
                  </em>
                </span>
              </el-option>
            </el-select>
            <div class="pc-log-lines" title="拉取最近 N 行日志">
              <button
                v-for="n in [100, 500, 1000, 2000]"
                :key="n"
                class="pc-log-line-btn mono-text"
                :class="{ active: logTail === n }"
                type="button"
                @click="onTailPick(n)"
              >
                {{ n >= 1000 ? `${n / 1000}k` : n }}
              </button>
            </div>
          </div>
          <span class="pc-log-sep"></span>
          <el-input v-model="logFilter" class="pc-log-filter" clearable placeholder="关键字过滤，如 error / 异常 / ip">
            <template #prefix>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pc-log-filter-ico">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
            </template>
          </el-input>
          <div class="pc-log-toolbar-ops">
            <button class="pc-log-tool follow" :class="{ on: logFollowSid }" type="button" @click="toggleLogFollow">
              {{ logFollowSid ? '⏹ 停止跟踪' : '▶ 实时跟踪' }}
            </button>
            <button class="pc-log-tool" type="button" :disabled="logLoading || !!logFollowSid" @click="fetchLogs">
              ⟳ 刷新
            </button>
            <button class="pc-log-tool" type="button" @click="copyLogs">⧉ 复制</button>
          </div>
        </div>

        <div class="pcd-log-box">
          <div class="pcd-log-head">
            <span class="pcd-log-dot" :class="{ running: !!logFollowSid }"></span>
            <span class="mono-text pc-log-cmd">{{ logCmdText }}</span>
            <span v-if="logFollowSid && !logFollowEnded" class="plg-live"><i></i>LIVE</span>
            <span class="pcd-log-status">{{ logViewStatusText }}</span>
          </div>

          <!-- 等级统计条：一键过滤对应等级 -->
          <div v-if="logLines.length" class="plg-levels">
            <button
              v-if="logLevelCounts.error"
              class="plg-chip err"
              :class="{ active: levelFilterActive('ERROR') }"
              type="button"
              @click="toggleLevelFilter('ERROR')"
            >
              ERROR · {{ logLevelCounts.error }}
            </button>
            <button
              v-if="logLevelCounts.warn"
              class="plg-chip warn"
              :class="{ active: levelFilterActive('WARN') }"
              type="button"
              @click="toggleLevelFilter('WARN')"
            >
              WARN · {{ logLevelCounts.warn }}
            </button>
            <button
              v-if="logLevelCounts.info"
              class="plg-chip info"
              :class="{ active: levelFilterActive('INFO') }"
              type="button"
              @click="toggleLevelFilter('INFO')"
            >
              INFO · {{ logLevelCounts.info }}
            </button>
            <button
              v-if="logLevelCounts.debug"
              class="plg-chip debug"
              :class="{ active: levelFilterActive('DEBUG') }"
              type="button"
              @click="toggleLevelFilter('DEBUG')"
            >
              DEBUG · {{ logLevelCounts.debug }}
            </button>
            <span class="plg-levels-hint">点击等级快捷过滤</span>
          </div>

          <div class="pcd-log pc-log-view" ref="logBoxEl">
            <div v-if="logError" class="plg-error">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
              <span>{{ logError }}</span>
            </div>
            <template v-else>
              <div
                v-for="(m, i) in logLineModels"
                :key="i"
                class="plg-row"
                :class="m.level ? `has-${m.level}` : ''"
              >
                <span class="plg-no">{{ i + 1 }}</span>
                <span v-if="m.prefix" class="plg-prefix">{{ m.prefix }}</span>
                <span class="plg-text" :class="m.level ? `lv-${m.level}` : ''">
                  <template v-for="(seg, si) in m.segments" :key="si">
                    <mark v-if="seg.hit" class="plg-hit">{{ seg.text }}</mark>
                    <template v-else>{{ seg.text }}</template>
                  </template>
                </span>
              </div>
              <div v-if="logLineModels.length === 0 && !logLoading" class="plg-empty">
                <svg v-if="logFilter.trim()" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" class="plg-empty-ico">
                  <circle cx="11" cy="11" r="7" />
                  <path d="m20 20-3.5-3.5" />
                  <path d="m8.5 8.5 5 5M13.5 8.5l-5 5" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" class="plg-empty-ico">
                  <polyline points="22 12 16 12 14 15 10 15 8 12 2 12" />
                  <path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" />
                </svg>
                <span>{{ logFilter.trim() ? '没有匹配关键字的日志行' : '暂无日志输出 · 服务启动后日志将在这里展示' }}</span>
              </div>
              <div v-if="logLoading" class="plg-empty">
                <span class="pc-env-spin"></span>
                <span>正在读取日志…</span>
              </div>
            </template>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="pcd-footer plg-footer">
          <span class="plg-foot-hint">等级自动着色 · 行号为当前过滤结果序号 · LIVE 跟踪自动滚动到底部</span>
          <button class="pcd-btn ghost" type="button" @click="onCloseLog">关闭</button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 远程服务器绑定弹窗：选密钥 → SSH 连接 → 浏览远程目录 → 绑定 ===== -->
    <el-dialog
      v-model="sshBindVisible"
      width="620px"
      draggable
      append-to-body
      modal-class="pc-dlg pssh-dlg"
      :show-close="false"
      :close-on-click-modal="false"
      @open="loadSshConfigs"
    >
      <template #header>
        <div class="pcd-header pssh-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">{{ sshBrowserMode === 'clone' ? '⬇️' : '🖥️' }}</div>
            <div>
              <div class="pcd-title">{{ sshBrowserMode === 'clone' ? '远程拉取代码' : '远程服务器绑定' }}</div>
              <div class="pcd-subtitle">
                {{
                  sshBrowserMode === 'clone'
                    ? '选择远程父目录与 Git 密钥 · SSH 在远程执行 git clone 后自动绑定'
                    : 'SSH 登录远程 Linux 服务器 · 浏览目录 · 读取 CONFIG 完成绑定'
                }}
              </div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="sshBindVisible = false">✕</button>
        </div>
      </template>

      <div class="pcd-body">
        <!-- 密钥选择 + 连接 -->
        <div class="pssh-conn-row">
          <el-select v-model="sshConfigId" placeholder="选择 Linux 密钥" class="pssh-sel" @change="sshConnected = false">
            <el-option v-for="c in sshConfigs" :key="c.id" :label="`${c.name}（${c.address}）`" :value="c.id" />
          </el-select>
          <button class="pcd-btn primary" type="button" :disabled="!sshConfigId || sshTesting" @click="testSsh">
            <span v-if="sshTesting" class="pc-env-spin"></span>
            <template v-else>{{ sshConnected ? '已连接' : '连接' }}</template>
          </button>
        </div>
        <div v-if="sshConfigs.length === 0" class="pssh-hint warn">
          密钥管理中暂无 Linux 类型密钥，请先到「密钥管理」添加（类型选择 Linux 🐧）
        </div>

        <!-- 拉取模式：Git 密钥选择 -->
        <div v-if="sshBrowserMode === 'clone' && sshConnected" class="pssh-conn-row">
          <el-select v-model="cloneSecretId" placeholder="选择用于拉取代码的 Git 密钥" class="pssh-sel">
            <el-option v-for="s in secrets" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
        <div v-if="sshBrowserMode === 'clone' && sshConnected && secrets.length === 0" class="pssh-hint warn">
          密钥管理中暂无 Git 类型密钥，请先到「密钥管理」添加（类型选择 Git）
        </div>

        <!-- 错误 -->
        <div v-if="sshError" class="plg-error">⚠️ {{ sshError }}</div>

        <!-- 目录浏览器 -->
        <template v-if="sshConnected">
          <div class="pssh-browser">
            <div class="pssh-path-bar">
              <button class="pssh-up" type="button" title="上一级" @click="browseSshDir(sshCwd.replace(/\/[^/]+$/, '') || '/')">↑</button>
              <span class="mono-text pssh-path">{{ sshCwd }}</span>
              <button class="pssh-mkdir" type="button" :disabled="sshLoadingDir" title="在当前目录下新建子目录" @click="createSshDir">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                  <line x1="12" y1="11" x2="12" y2="17" />
                  <line x1="9" y1="14" x2="15" y2="14" />
                </svg>
                创建目录
              </button>
            </div>
            <div class="pssh-list">
              <div v-if="sshLoadingDir" class="pssh-loading"><span class="pc-env-spin"></span>读取中…</div>
              <div
                v-for="d in sshDirs"
                :key="d"
                class="pssh-dir"
                @click="browseSshDir(sshCwd === '/' ? '/' + d : sshCwd + '/' + d)"
              >
                📁 <span class="mono-text">{{ d }}</span>
              </div>
              <div v-if="!sshLoadingDir && sshDirs.length === 0" class="pssh-empty">无子目录</div>
            </div>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <span v-if="sshConnected" class="pssh-foot-hint">
            {{ sshBrowserMode === 'clone' ? `拉取到：${sshCwd}/maozi-cloud` : `当前目录：${sshCwd}` }}
          </span>
          <button class="pcd-btn ghost" type="button" @click="sshBindVisible = false">取消</button>
          <button
            class="pcd-btn primary"
            type="button"
            :disabled="!sshConnected || sshLoadingDir || (sshBrowserMode === 'clone' && !cloneSecretId)"
            @click="confirmSshBind"
          >
            {{ sshBrowserMode === 'clone' ? '⬇ 拉取到此目录' : '绑定此目录' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 切换分支弹窗：远程分支列表（当前分支置顶） ===== -->
    <el-dialog
      v-model="branchVisible"
      width="440px"
      draggable
      append-to-body
      modal-class="pc-dlg pbr-dlg"
      :show-close="false"
      :close-on-click-modal="false"
      @open="loadGitBranches"
    >
      <template #header>
        <div class="pcd-header pbr-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">🌿</div>
            <div>
              <div class="pcd-title">切换分支</div>
              <div class="pcd-subtitle">从远程分支列表选择 · 切换前自动 fetch 目标分支</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="branchVisible = false">✕</button>
        </div>
      </template>

      <div class="pcd-body">
        <div v-if="branchError" class="plg-error">⚠️ {{ branchError }}</div>
        <div v-if="branchLoading" class="pbr-loading"><span class="pc-env-spin"></span>获取远程分支中…</div>
        <div v-else class="pbr-list">
          <div
            v-for="b in branchList"
            :key="b"
            class="pbr-item"
            :class="{ current: b === gitInfo.branch }"
            @click="onPickBranch(b)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="6" y1="3" x2="6" y2="15" />
              <circle cx="18" cy="6" r="3" />
              <circle cx="6" cy="18" r="3" />
              <path d="M18 9a9 9 0 0 1-9 9" />
            </svg>
            <span class="mono-text">{{ b }}</span>
            <span v-if="b === gitInfo.branch" class="pbr-cur">当前</span>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pbr-arrow">
              <path d="m9 18 6-6-6-6" />
            </svg>
          </div>
          <div v-if="branchList.length === 0" class="pbr-empty">未获取到远程分支</div>
        </div>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <button class="pcd-btn ghost" type="button" @click="branchVisible = false">取消</button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 解绑项目确认弹窗 ===== -->
    <el-dialog
      v-model="unbindVisible"
      width="480px"
      draggable
      append-to-body
      modal-class="pc-dlg pub-dlg"
      :show-close="false"
      :close-on-click-modal="false"
    >
      <template #header>
        <div class="pcd-header pub-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10 2v6l-7 8a2 2 0 0 0 1.7 3h14a2 2 0 0 0 1.7-3l-7-8V2" />
                <path d="M8.5 2h7" />
                <line x1="10" y1="14" x2="14" y2="14" />
              </svg>
            </div>
            <div>
              <div class="pcd-title">解绑项目</div>
              <div class="pcd-subtitle">移除本应用与项目的绑定关系</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="unbindVisible = false">✕</button>
        </div>
      </template>

      <div class="pcd-body">
        <div class="pub-content">
          <div class="pub-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10 2v6l-7 8a2 2 0 0 0 1.7 3h14a2 2 0 0 0 1.7-3l-7-8V2" />
              <path d="M8.5 2h7" />
            </svg>
          </div>
          <div class="pub-text">
            <div class="pub-title">确定解绑 {{ binding?.name }} 吗？</div>
            <div class="pub-list">
              <div class="pub-item ok">✓ 本地代码目录不受影响</div>
              <div class="pub-item ok">✓ Docker 容器与数据卷保持原状</div>
              <div class="pub-item warn">⚠ 基础服务 / 应用服务面板将隐藏</div>
              <div class="pub-item warn">⚠ 执行日志会话与环境准备状态将清除</div>
            </div>
            <div class="pub-hint">解绑后可随时重新绑定，选择本地目录或拉取代码即可</div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <button class="pcd-btn ghost" type="button" @click="unbindVisible = false">取消</button>
          <button class="pcd-btn pub-confirm" type="button" @click="confirmUnbind">确定解绑</button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 全链路日志查询弹窗：输入链路 ID，跨服务检索日志 ===== -->
    <el-dialog
      v-model="traceVisible"
      width="860px"
      draggable
      append-to-body
      modal-class="pc-dlg ptq-dlg"
      :show-close="false"
      :close-on-click-modal="false"
      @closed="traceResults = []; traceError = ''; traceSummary = ''"
    >
      <template #header>
        <div class="pcd-header ptq-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
                <path d="M8.5 8.5l5 5M13.5 8.5l-5 5" />
              </svg>
            </div>
            <div>
              <div class="pcd-title">全链路日志查询</div>
              <div class="pcd-subtitle">输入链路 ID（trace ID），跨所有运行中的微服务检索日志</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="traceVisible = false">✕</button>
        </div>
      </template>

      <div class="pcd-body">
        <!-- 搜索栏 -->
        <div class="ptq-search-bar">
          <el-input
            v-model="traceId"
            placeholder="输入链路 ID，如 4bf92f3577b34da6a3ce929d0e0e4736"
            clearable
            class="ptq-input"
            @keyup.enter="runTraceQuery"
          />
          <button class="pcd-btn primary" type="button" :disabled="traceLoading" @click="runTraceQuery">
            <span v-if="traceLoading" class="pc-env-spin"></span>
            <template v-else>🔍 查询</template>
          </button>
        </div>

        <!-- 错误 -->
        <div v-if="traceError" class="plg-error">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <span>{{ traceError }}</span>
        </div>

        <!-- 结果 -->
        <template v-else>
          <div v-if="traceSummary" class="ptq-summary">{{ traceSummary }}</div>
          <div v-if="traceResults.length" class="pcd-log-box">
            <div class="pcd-log-head">
              <span class="pcd-log-dot"></span>
              <span class="mono-text">全链路日志 · {{ traceId.trim() }}</span>
              <span class="pcd-log-status">按时间排序</span>
            </div>
            <div class="pcd-log ptq-lines">
              <div v-for="(l, i) in traceResults" :key="i" class="plg-row">
                <span class="plg-no">{{ i + 1 }}</span>
                <span class="plg-text">{{ l }}</span>
              </div>
            </div>
          </div>
          <div v-if="!traceLoading && traceResults.length === 0 && traceSummary" class="plg-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" class="plg-empty-ico">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
              <path d="M8.5 8.5l5 5M13.5 8.5l-5 5" />
            </svg>
            <span>{{ traceSummary }}</span>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <span v-if="traceResults.length > 0" class="ptq-foot-info mono-text">共 {{ traceResults.length }} 行 · 按时间排序</span>
          <button class="pcd-btn ghost" type="button" @click="traceVisible = false">关闭</button>
        </div>
      </template>
    </el-dialog>

    <!-- ===== 互斥切换确认弹窗：左（将被关闭）→ 右（即将启动）可视化 ===== -->
    <el-dialog
      v-model="mutexVisible"
      width="560px"
      draggable
      append-to-body
      modal-class="pc-dlg pmx-dlg"
      :show-close="false"
      :close-on-click-modal="false"
      :before-close="() => closeMutex(false)"
    >
      <template #header>
        <div class="pcd-header pmx-header">
          <div class="pcd-deco pcd-deco-1"></div>
          <div class="pcd-deco pcd-deco-2"></div>
          <div class="pcd-header-main">
            <div class="pcd-header-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
            </div>
            <div>
              <div class="pcd-title">互斥切换</div>
              <div class="pcd-subtitle">单体与微服务互斥运行 · 启动一侧需关闭另一侧</div>
            </div>
          </div>
          <button class="pcd-close" type="button" @click="closeMutex(false)">✕</button>
        </div>
      </template>

      <div v-if="mutexPayload" class="pcd-body">
        <div class="pmx-desc">{{ mutexPayload.desc }}</div>

        <div class="pmx-flow">
          <!-- 左：将被关闭 -->
          <div class="pmx-card stop">
            <div class="pmx-card-head">
              <span class="pmx-card-dot stop"><i></i></span>
              <span class="pmx-card-title">将被关闭</span>
              <span class="pmx-card-tag">{{ mutexPayload.stopLabel }} · {{ mutexPayload.stopList.length }} 个运行中</span>
            </div>
            <div class="pmx-list">
              <div v-for="n in mutexPayload.stopList" :key="n" class="pmx-item">
                <span class="pmx-item-dot"></span>
                <span class="mono-text">{{ appSvcName(n) }}</span>
              </div>
            </div>
          </div>

          <!-- 中：流向箭头 -->
          <div class="pmx-arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
              <path d="M5 12h14" />
              <path d="m13 6 6 6-6 6" />
            </svg>
          </div>

          <!-- 右：即将启动 -->
          <div class="pmx-card start">
            <div class="pmx-card-head">
              <span class="pmx-card-dot start"><i></i></span>
              <span class="pmx-card-title">即将启动</span>
              <span class="pmx-card-tag">{{ mutexPayload.startSub }}</span>
            </div>
            <div class="pmx-start-name">
              <span class="pmx-item-dot run"></span>
              <span class="mono-text">{{ mutexPayload.startLabel }}</span>
            </div>
          </div>
        </div>

        <div class="pmx-hint">容器停止不会删除数据卷 · 重新启动后数据保留</div>
      </div>

      <template #footer>
        <div class="pcd-footer">
          <button class="pcd-btn ghost" type="button" @click="closeMutex(false)">取消</button>
          <button
            class="pcd-btn pmx-confirm"
            type="button"
            @click="closeMutex(true)"
          >
            关闭并启动（{{ mutexPayload?.stopList.length ?? 0 }} 个）
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onDeactivated, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, type AppServiceEntry, type ComposeServiceStats, type ConfigEntry, type EnvFile, type EnvSettingGroup, type EnvSettingItem, type ProjectBinding } from '../api'

// KeepAlive include 按组件名缓存，显式声明避免依赖文件名推断
defineOptions({ name: 'ProjectConsole' })

const REPO_PAGE = 'https://github.com/1095071913/maozi-cloud'

const binding = ref<ProjectBinding | null>(null)

async function loadState(): Promise<void> {
  const r = await api.projects.state()
  if (r.ok) binding.value = r.data ?? null
  else ElMessage.error(r.error ?? '读取绑定状态失败')
  if (!binding.value) return
  // 各加载互不依赖：并行发起；资源统计（CPU/内存）带 preferCache —— 命中启动预取的快照则零等待渲染
  void loadComposeStats(true)
  void loadAppSvcStats(true)
  void loadGitInfo()
  await Promise.all([
    loadComposeServices(),
    loadHostsInitStatus(),
    loadNetworkStatus(),
    loadDbInitStatus(),
    loadUiState(),
    loadAppServices(),
    loadAppSvcStatuses()
  ])
}

function openRepo(): void {
  void api.util.openUrl(REPO_PAGE)
}

function fmtTime(ts: number): string {
  const d = new Date(ts)
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function copyPath(): Promise<void> {
  if (!binding.value) return
  await api.util.copy(binding.value.path)
  ElMessage.success('路径已复制到剪贴板')
}

/** 选择本地目录绑定：读 CONFIG */
async function onPickDir(): Promise<void> {
  const picked = await api.projects.pickDir()
  if (!picked.ok) {
    ElMessage.error(picked.error ?? '选择目录失败')
    return
  }
  if (!picked.data) return
  const r = await api.projects.bindDir(picked.data)
  if (!r.ok || !r.data) {
    ElMessage.error(r.error ?? '绑定失败')
    return
  }
  ElMessage.success(`绑定成功：${r.data.name}${r.data.version ? `（v${r.data.version}）` : ''}`)
  await loadState()
}

/** 拉取代码绑定 */
const cloneVisible = ref(false)
const secrets = ref<ConfigEntry[]>([])
const cloneSecretId = ref('')
const cloneDest = ref('')
const cloning = ref(false)

/** git 实时日志：line 追加、update 覆盖上一行(git 的 \r 进度) */
const cloneLogs = ref<string[]>([])
const logBox = ref<HTMLElement>()

const offCloneLog = api.projects.onCloneLog((p) => {
  if (p.kind === 'update' && cloneLogs.value.length > 0) {
    cloneLogs.value.splice(cloneLogs.value.length - 1, 1, p.text)
  } else if (p.kind === 'line') {
    cloneLogs.value.push(p.text)
  }
})

watch(
  () => cloneLogs.value.length,
  async () => {
    await nextTick()
    logBox.value?.scrollTo({ top: 10_000_000 })
  }
)

onUnmounted(() => offCloneLog())

/** ===== 脚本日志：多会话并发模型（每个操作独立日志，弹窗内标签页切换） ===== */
const scriptVisible = ref(false)
const scriptLogBox = ref<HTMLElement>()

type LastAction =
  | { type: 'script'; kind: 'all' | 'demand' | 'admin' | 'monomerServices' | 'monomerAdmin' }
  | { type: 'sshClone' }
  | { type: 'gitPull' }
  | { type: 'gitCheckout'; branch: string }
  | { type: 'compose'; service: string; action: 'start' | 'stop' | 'restart' }
  | { type: 'adminStop' }
  | { type: 'hostsInit' }
  | { type: 'networkCreate' }
  | { type: 'dbInit' }
  | { type: 'appSvc'; variant: 'monomer' | 'distributeds'; file: 'admin' | 'services'; service: string; action: 'start' | 'stop' | 'restart' }
  | { type: 'appSvcAll'; variant: 'monomer' | 'distributeds'; action: 'start' | 'stop' }

interface LogSession {
  id: string
  action: LastAction
  label: string
  icon: string
  title: string
  sub: string
  logs: string[]
  status: '' | 'ok' | 'fail'
  running: boolean
  startedAt: number
  endedAt?: number
}

/** 毫秒 → 「X 秒 / X 分 Y 秒 / X 小时 Y 分 Z 秒」 */
function fmtDuration(ms: number): string {
  const sec = Math.max(0, Math.floor(ms / 1000))
  if (sec < 60) return `${sec} 秒`
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min} 分 ${sec % 60} 秒`
  return `${Math.floor(min / 60)} 小时 ${min % 60} 分 ${sec % 60} 秒`
}

/** 会话收尾：记录结束时间并在日志末尾追加耗时行（所有执行入口统一走这里） */
function finishSession(s: LogSession, okFlag: boolean): void {
  s.running = false
  s.status = okFlag ? 'ok' : 'fail'
  s.endedAt = Date.now()
  s.logs.push(`── ${okFlag ? '✓ 执行成功' : '✗ 执行失败'} · 耗时 ${fmtDuration(s.endedAt - s.startedAt)} ──`)
}

const sessions = ref<LogSession[]>([])
const activeSid = ref('')

const activeSession = computed(() => sessions.value.find((s) => s.id === activeSid.value) ?? null)

const runningSessionCount = computed(() => sessions.value.filter((s) => s.running).length)

const hasFailedSession = computed(() => sessions.value.some((s) => s.status === 'fail'))

/** 执行日志按钮状态：运行中 / 有失败 / 全部完成（互斥） */
const logBtnState = computed(() => {
  if (runningSessionCount.value > 0) return 'running'
  return hasFailedSession.value ? 'fail' : 'ok'
})

const logBtnIcon = computed(() => {
  if (logBtnState.value === 'running') return '⏳'
  return logBtnState.value === 'fail' ? '⚠️' : '📄'
})

const logStatusText = computed(() => {
  if (runningSessionCount.value > 0) return `${runningSessionCount.value} 个任务运行中`
  return hasFailedSession.value ? '有任务失败' : '全部完成'
})

/** 会话耗时展示：运行中每秒刷新（nowTick 心跳），结束后用 endedAt 定格 */
const nowTick = ref(Date.now())
const tickTimer = setInterval(() => (nowTick.value = Date.now()), 1000)
onUnmounted(() => clearInterval(tickTimer))

const scriptElapsedText = computed(() => {
  const s = activeSession.value
  if (!s) return ''
  const end = s.running ? nowTick.value : (s.endedAt ?? s.startedAt)
  return fmtDuration(end - s.startedAt)
})

/* 模板兼容视图（绑定到当前活动会话） */
const scriptRunning = computed(() => activeSession.value?.running ?? false)
const scriptStatus = computed(() => activeSession.value?.status ?? '')
const scriptLogs = computed(() => activeSession.value?.logs ?? [])
const scriptIcon = computed(() => activeSession.value?.icon ?? '🧱')
const scriptTitle = computed(() => activeSession.value?.title ?? '')
const scriptSub = computed(() => activeSession.value?.sub ?? '')

/** 是否存在与谓词匹配且正在运行的会话（用于按钮「查看运行日志」态） */
function runningOf(match: (a: LastAction) => boolean): boolean {
  return sessions.value.some((s) => s.running && match(s.action))
}

type DeployKind = 'all' | 'demand' | 'admin' | 'monomerServices' | 'monomerAdmin'

const DEPLOY_META: Record<DeployKind, { icon: string; title: string; sub: string; variant: AppSvcVariant; done: string }> = {
  demand: {
    icon: '⚡',
    title: '微服务按需编译启动',
    sub: 'maozi-cloud-deploy-services-distributed.sh · 实时输出',
    variant: 'distributeds',
    done: '微服务按需编译启动完成'
  },
  all: {
    icon: '🧱',
    title: '微服务全量编译启动',
    sub: 'maozi-cloud-deploy-services-distributed-force.sh · 实时输出',
    variant: 'distributeds',
    done: '微服务全量编译启动完成'
  },
  admin: {
    icon: '🖥️',
    title: '后台编译启动',
    sub: 'maozi-cloud-deploy-admin-distributed.sh · 实时输出',
    variant: 'distributeds',
    done: '后台编译启动完成'
  },
  monomerServices: {
    icon: '🧱',
    title: '单体服务编译启动',
    sub: 'maozi-cloud-deploy-services-monomer.sh · 实时输出',
    variant: 'monomer',
    done: '单体服务编译启动完成'
  },
  monomerAdmin: {
    icon: '🖥️',
    title: '后台编译启动',
    sub: 'maozi-cloud-deploy-admin-monomer.sh · 实时输出',
    variant: 'monomer',
    done: '后台编译启动完成'
  }
}

let sidSeq = 0

/** 新建会话并打开日志弹窗（返回响应式代理，后续状态变更才能触发视图更新） */
function startSession(
  action: LastAction,
  meta: { label: string; icon: string; title: string; sub: string }
): LogSession {
  const id = `s${Date.now().toString(36)}_${++sidSeq}`
  const session: LogSession = { id, action, ...meta, logs: [], status: '', running: true, startedAt: Date.now() }
  sessions.value.push(session)
  activeSid.value = id
  scriptVisible.value = true
  // 必须取数组中的代理对象：直接返回 session 原始对象时，对其属性的写入不会触发响应式更新
  return sessions.value[sessions.value.length - 1]
}

/** 同一操作已在运行：聚焦既有会话（返回 true 表示已处理） */
function focusRunning(match: (a: LastAction) => boolean): boolean {
  const found = sessions.value.find((s) => s.running && match(s.action))
  if (!found) return false
  activeSid.value = found.id
  scriptVisible.value = true
  return true
}

function removeSession(id: string): void {
  const idx = sessions.value.findIndex((s) => s.id === id)
  if (idx >= 0) sessions.value.splice(idx, 1)
  if (activeSid.value === id) activeSid.value = sessions.value[0]?.id ?? ''
}

function focusSession(id: string): void {
  activeSid.value = id
  void nextTick(() => scriptLogBox.value?.scrollTo({ top: 10_000_000 }))
}

/** 重新执行当前活动会话的操作（先移除已结束的旧会话） */
function onRerunLast(): Promise<void> {
  const s = activeSession.value
  if (!s || s.running) return Promise.resolve()
  const a = s.action
  removeSession(s.id)
  if (a.type === 'script') return onRunScript(a.kind)
  if (a.type === 'compose') return onCompose(a.service, a.action, true)
  if (a.type === 'adminStop') return onAdminStop(true)
  if (a.type === 'hostsInit') return onHostsInit(true)
  if (a.type === 'networkCreate') return onNetworkCreate(true)
  if (a.type === 'appSvc') return onAppService({ name: a.service, file: a.file }, a.action, true)
  if (a.type === 'appSvcAll') return onAppServiceAll(a.action, true)
  return onDbInit(true)
}

/** ===== 日志窗口关闭语义：执行中点关闭=中断当前会话；后台运行=隐藏窗口，所有会话继续 ===== */
const stopRequestedSids = new Set<string>()

async function interruptActive(): Promise<void> {
  const s = activeSession.value
  if (!s || !s.running || stopRequestedSids.has(s.id)) return
  stopRequestedSids.add(s.id)
  await api.projects.stopScript(s.id)
  ElMessage.info('已中断执行')
}

async function onCloseScript(): Promise<void> {
  await interruptActive()
  scriptVisible.value = false
}

function onRunBackground(): void {
  scriptVisible.value = false
}

function onBeforeScriptClose(done: () => void): void {
  void interruptActive().then(() => done())
}

const offScriptLog = api.projects.onScriptLog((p) => {
  const s = sessions.value.find((x) => x.id === p.sid)
  if (!s) return

  // 后台任务完成标记：结束会话并刷新状态
  if (p.text === '__APP_SVC_DONE__' || p.text === '__APP_SVC_FAIL__') {
    finishSession(s, p.text === '__APP_SVC_DONE__')
    if (p.text === '__APP_SVC_DONE__') ElMessage.success('操作完成')
    void loadAppSvcStatuses()
    void loadAppSvcStats()
    return
  }
  if (p.kind === 'update' && s.logs.length > 0) {
    s.logs.splice(s.logs.length - 1, 1, p.text)
  } else if (p.kind === 'line') {
    s.logs.push(p.text)
  }
})

watch(
  () => scriptLogs.value.length,
  async () => {
    await nextTick()
    scriptLogBox.value?.scrollTo({ top: 10_000_000 })
  }
)

watch(activeSid, () => {
  void nextTick(() => scriptLogBox.value?.scrollTo({ top: 10_000_000 }))
})

onUnmounted(() => offScriptLog())

/** ===== 服务日志查询：一次性拉取最近 N 行 + 可选实时跟踪（-f 流式，stopScript 中断） ===== */
const logVisible = ref(false)
const logService = ref('')
/** 日志上下文：'basics'（基础服务）或 '<monomer|distributeds>:<admin|services>'（应用服务 -f 文件） */
const logCtx = ref('basics')

function appSvcYmlName(ctx: string): string {
  const [v, f] = ctx.split(':')
  return APP_SVC_FILE_NAMES[v as AppSvcVariant]?.[f as 'admin' | 'services'] ?? ''
}

const logCmdText = computed(() =>
  [
    'docker',
    'compose',
    ...(logCtx.value === 'basics' ? [] : ['-f', appSvcYmlName(logCtx.value)]),
    'logs',
    logFollowSid.value ? '-f' : `--tail ${logTail.value}`,
    logService.value
  ].join(' ')
)

/** 日志弹窗服务下拉选项：随上下文切换（基础服务 / 应用服务当前变体） */
const logServiceOptions = computed(() => {
  if (logCtx.value === 'basics') return composeServices.value.map((s) => ({ name: s, file: '' }))
  const [v] = logCtx.value.split(':')
  return appServices.value[v as AppSvcVariant].map((e) => ({ name: e.name, file: e.file }))
})
const logTail = ref(500)
const logLines = ref<string[]>([])
const logFilter = ref('')
const logLoading = ref(false)
const logError = ref('')
/** 跟踪会话：sid 非空表示跟踪中；logFollowEnded 区分「自然结束」与「跟踪中」 */
const logFollowSid = ref('')
const logFollowEnded = ref(false)
const logBoxEl = ref<HTMLElement>()
/** 缓冲上限：超出丢弃最旧行，避免长时间跟踪内存膨胀 */
const LOG_MAX_LINES = 5000

const filteredLogLines = computed(() => {
  const kw = logFilter.value.trim().toLowerCase()
  return kw ? logLines.value.filter((l) => l.toLowerCase().includes(kw)) : logLines.value
})

/** 日志行解析：拆出容器前缀（隐藏项目前缀）与等级，用于行号 / 着色 / 等级统计 */
interface LogLineModel {
  prefix: string
  text: string
  level: '' | 'error' | 'warn' | 'info' | 'debug'
  segments: Array<{ text: string; hit: boolean }>
}

/** 按关键字（大小写不敏感）拆分文本为命中/未命中分段 */
function highlightSegments(text: string, kw: string): Array<{ text: string; hit: boolean }> {
  if (!kw) return [{ text, hit: false }]
  const lowerText = text.toLowerCase()
  const lowerKw = kw.toLowerCase()
  const segments: Array<{ text: string; hit: boolean }> = []
  let idx = 0
  while (idx <= text.length) {
    const found = lowerText.indexOf(lowerKw, idx)
    if (found === -1) {
      if (idx < text.length) segments.push({ text: text.slice(idx), hit: false })
      break
    }
    if (found > idx) segments.push({ text: text.slice(idx, found), hit: false })
    segments.push({ text: text.slice(found, found + kw.length), hit: true })
    idx = found + kw.length
  }
  return segments.length > 0 ? segments : [{ text, hit: false }]
}

const LOG_LEVEL_RE = /\b(FATAL|ERROR|WARNING|WARN|INFO|DEBUG|TRACE)\b/
const LOG_LEVEL_MAP: Record<string, LogLineModel['level']> = {
  FATAL: 'error',
  ERROR: 'error',
  WARNING: 'warn',
  WARN: 'warn',
  INFO: 'info',
  DEBUG: 'debug',
  TRACE: 'debug'
}

function parseLogLine(l: string): LogLineModel {
  const pm = l.match(/^(\S+\s+\|\s?)(.*)$/)
  const prefix = pm ? pm[1].replace(/^maozi-cloud-basic-/, '') : ''
  const text = pm ? pm[2] : l
  const lm = text.toUpperCase().match(LOG_LEVEL_RE)
  return { prefix, text, level: lm ? (LOG_LEVEL_MAP[lm[1]] ?? '') : '' }
}

const logLineModels = computed(() => {
  const kw = logFilter.value.trim()
  return filteredLogLines.value.map((l) => {
    const parsed = parseLogLine(l)
    return { ...parsed, segments: highlightSegments(parsed.text, kw) }
  })
})

const logLevelCounts = computed(() => {
  const c = { error: 0, warn: 0, info: 0, debug: 0 }
  for (const l of logLines.value) {
    const { level } = parseLogLine(l)
    if (level) c[level]++
  }
  return c
})

/** 等级快捷过滤：与关键字输入框同一数据源，再次点击取消 */
function levelFilterActive(level: string): boolean {
  return logFilter.value.trim().toUpperCase() === level
}

function toggleLevelFilter(level: string): void {
  logFilter.value = levelFilterActive(level) ? '' : level
}

/** 弹窗头部品牌化：使用服务卡片同款品牌渐变与图标 */
const logHeaderStyle = computed(() => ({ background: svcVisual(logService.value).grad }))

const logHeaderIcon = computed(() => {
  if (!logService.value) return '📄'
  return svcVisual(logService.value).icon || svcName(logService.value).charAt(0).toUpperCase()
})

const logViewStatusText = computed(() => {
  if (logFollowSid.value) {
    if (logFollowEnded.value) return '跟踪已结束'
    const kw = logFilter.value.trim()
    return kw
      ? `匹配 ${filteredLogLines.value.length} / ${logLines.value.length} 行 · 跟踪中`
      : '实时跟踪中 · 自动滚动'
  }
  if (logLoading.value) return '读取中…'
  const total = logLines.value.length
  if (!total) return '暂无日志'
  const shown = filteredLogLines.value.length
  return logFilter.value.trim() ? `匹配 ${shown} / ${total} 行` : `共 ${total} 行`
})

function scrollLogBottom(): void {
  void nextTick(() => logBoxEl.value?.scrollTo({ top: 10_000_000 }))
}

watch(
  () => filteredLogLines.value.length,
  () => scrollLogBottom()
)

function openServiceLog(s: string, ctx = 'basics'): void {
  logService.value = s
  logCtx.value = ctx
  logFilter.value = ''
  logVisible.value = true
  void fetchLogs()
}

/** 切换服务 / 行数：先停跟踪再重新拉取 */
function onLogTargetChange(): void {
  if (logVisible.value) void fetchLogs()
}

/** 行数分段控件选档：切换后立即按新行数拉取 */
function onTailPick(n: number): void {
  if (logTail.value === n) return
  logTail.value = n
  if (logVisible.value) void fetchLogs()
}

async function fetchLogs(): Promise<void> {
  if (!logService.value) return
  stopLogFollow()
  logLoading.value = true
  logError.value = ''
  logLines.value = []
  try {
    const r = await api.projects.composeLogs(logService.value, logTail.value, logCtx.value)
    if (r.ok) logLines.value = r.data ?? []
    else logError.value = r.error ?? '读取日志失败'
  } finally {
    logLoading.value = false
    scrollLogBottom()
  }
}

async function toggleLogFollow(): Promise<void> {
  if (logFollowSid.value) {
    stopLogFollow()
    return
  }
  if (!logService.value) return
  const sid = `log${Date.now().toString(36)}_${++sidSeq}`
  logFollowSid.value = sid
  logFollowEnded.value = false
  logError.value = ''
  logLines.value = []
  try {
    // 正常返回 = 跟踪结束（服务停止或输出流关闭）；用户主动停止在 finally 按 sid 匹配收尾
    const r = await api.projects.composeLogsFollow(logService.value, logTail.value, sid, logCtx.value)
    if (!r.ok && logFollowSid.value === sid) logError.value = r.error ?? '日志跟踪失败'
  } finally {
    if (logFollowSid.value === sid) {
      logFollowEnded.value = true
      logFollowSid.value = ''
    }
  }
}

function stopLogFollow(): void {
  const sid = logFollowSid.value
  logFollowSid.value = ''
  logFollowEnded.value = false
  if (sid) void api.projects.stopScript(sid)
}

async function copyLogs(): Promise<void> {
  if (filteredLogLines.value.length === 0) return
  await api.util.copy(filteredLogLines.value.join('\n'))
  ElMessage.success(`已复制 ${filteredLogLines.value.length} 行日志到剪贴板`)
}

function onCloseLog(): void {
  stopLogFollow()
  logVisible.value = false
}

function onBeforeLogClose(done: () => void): void {
  stopLogFollow()
  done()
}

/** 跟踪输出：同 sid 才追加（update 覆盖上一行，处理 \r 进度） */
const offComposeLog = api.projects.onComposeLog((p) => {
  if (!p.sid || p.sid !== logFollowSid.value) return
  if (p.kind === 'update' && logLines.value.length > 0) {
    logLines.value.splice(logLines.value.length - 1, 1, p.text)
  } else if (p.kind === 'line') {
    logLines.value.push(p.text)
    if (logLines.value.length > LOG_MAX_LINES) {
      logLines.value.splice(0, logLines.value.length - LOG_MAX_LINES)
    }
  }
})

onUnmounted(() => offComposeLog())

/** ===== 环境设置：解析项目 ENVIRONMENT_VARIABLE（中文名称 → 环境变量 key），打开时实时读取当前值 ===== */
const envVisible = ref(false)
const envLoading = ref(false)
const envGroups = ref<EnvSettingGroup[]>([])
const envFiles = ref<EnvFile[]>([])
const envDefaultFileId = ref('')
const envError = ref('')
const envRevealed = ref(new Set<string>())

/** 编辑态：同一时间只编辑一条（按 key 标记） */
const envEditing = ref('')
const envDraft = ref('')
const envSaving = ref(false)

/** 汇总统计（汇总条展示） */
const envRemote = ref(false)
const envTotalCount = computed(() => envGroups.value.reduce((n, g) => n + g.items.length, 0))
const envSetCount = computed(() => envGroups.value.reduce((n, g) => n + g.items.filter((i) => i.found && i.enabled).length, 0))
const envMissingCount = computed(() => envTotalCount.value - envGroups.value.reduce((n, g) => n + g.items.filter((i) => i.found).length, 0))
const envDisabledCount = computed(() => envGroups.value.reduce((n, g) => n + g.items.filter((i) => i.found && !i.enabled).length, 0))

/** 就绪度：已设置（含禁用行以外的启用值）占总量百分比 */
const envProgressPct = computed(() =>
  envTotalCount.value > 0 ? Math.round((envSetCount.value / envTotalCount.value) * 100) : 0
)

/** 环形仪表配色：常规蓝，全部就绪切换绿 */
const envRingStyle = computed(() => ({
  '--p': String(envProgressPct.value),
  '--c1': envProgressPct.value === 100 ? '#34d399' : '#60a5fa',
  '--c2': envProgressPct.value === 100 ? '#059669' : '#2563eb'
}))

/** ===== 搜索 + 状态筛选（全部 / 未设置 / 已禁用） ===== */
const envSearch = ref('')
const envFilter = ref<'all' | 'missing' | 'off'>('all')

const envViewGroups = computed(() => {
  const kw = envSearch.value.trim().toLowerCase()
  const match = (it: EnvSettingItem): boolean => {
    if (envFilter.value === 'missing' && it.found) return false
    if (envFilter.value === 'off' && !(it.found && !it.enabled)) return false
    if (kw && ![it.label, it.key, it.value].some((x) => x.toLowerCase().includes(kw))) return false
    return true
  }
  return envGroups.value
    .map((g) => ({ ...g, items: g.items.filter(match) }))
    .filter((g) => g.items.length > 0)
})

const envEmptyText = computed(() => {
  const kw = envSearch.value.trim()
  if (kw) return `没有匹配「${kw}」的环境变量`
  if (envFilter.value === 'missing') return '没有未设置的变量 · 全部已配置 ✅'
  return '没有已禁用的变量 ✅'
})

/** 分组徽章渐变：按组序号循环取色 */
const ENV_GROUP_GRADS = [
  'linear-gradient(135deg, #60a5fa, #2563eb)',
  'linear-gradient(135deg, #34d399, #059669)',
  'linear-gradient(135deg, #a78bfa, #7c3aed)',
  'linear-gradient(135deg, #fbbf24, #d97706)',
  'linear-gradient(135deg, #f472b6, #db2777)',
  'linear-gradient(135deg, #22d3ee, #0891b2)'
]

function groupGrad(gi: number): string {
  return ENV_GROUP_GRADS[gi % ENV_GROUP_GRADS.length]
}

/** 名称/变量名含密钥特征的默认打码显示 */
function isSecretEnv(it: EnvSettingItem): boolean {
  return /密|secret|token|key/i.test(it.label) || /(SECRET|TOKEN|KEY)/.test(it.key)
}

function envDisplay(it: EnvSettingItem): string {
  if (isSecretEnv(it) && !envRevealed.value.has(it.key)) return '••••••••••'
  return it.value === '' ? '(空)' : it.value
}

function toggleEnvReveal(key: string): void {
  const next = new Set(envRevealed.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  envRevealed.value = next
}

async function copyEnvValue(it: EnvSettingItem): Promise<void> {
  if (!it.found) return
  await api.util.copy(it.value)
  ElMessage.success(`「${it.label}」的值已复制到剪贴板`)
}

function envFileName(fileId?: string): string {
  return envFiles.value.find((f) => f.id === fileId)?.name ?? ''
}

function startEnvEdit(it: EnvSettingItem): void {
  envEditing.value = it.key
  envDraft.value = it.value
}

/**
 * 保存修改：已存在的原位更新（禁用行顺带启用），新变量写入默认配置文件。
 * buildChildEnv 每次执行脚本都重新读取配置文件，保存后部署脚本下次执行即生效
 */
async function saveEnvEdit(it: EnvSettingItem): Promise<void> {
  const value = envDraft.value.trim()
  if (!value) {
    ElMessage.warning('变量值不能为空；如需删除变量请到「环境变量」页操作')
    return
  }
  const fileId = it.fileId ?? envDefaultFileId.value
  envSaving.value = true
  try {
    const r = envRemote.value
      ? await api.projects.sshEnvSave({ key: it.key, value, fileId })
      : await api.env.save({ mode: it.fileId ? 'update' : 'add', key: it.key, value, fileId })
    if (!r.ok) {
      ElMessage.error(r.error ?? '保存失败')
      return
    }
    if (it.found && !it.enabled) {
      const t = await api.env.save({ mode: 'toggle', key: it.key, value, fileId, enabled: true })
      if (!t.ok) ElMessage.warning(t.error ?? '已保存，但启用该行失败，请到「环境变量」页手动启用')
    }
    ElMessage.success(
        envRemote.value
          ? `已写入远程 ${envFileName(fileId) || '配置文件'}，部署脚本下次执行即生效`
          : `已写入 ${envFileName(fileId) || '配置文件'}，部署脚本下次执行即生效`
      )
    envEditing.value = ''
    await loadEnvSettings()
  } finally {
    envSaving.value = false
  }
}

async function loadEnvSettings(): Promise<void> {
  envLoading.value = true
  envError.value = ''
  try {
    const r = await api.projects.envSettings()
    if (r.ok && r.data) {
      envGroups.value = r.data.groups
      envFiles.value = r.data.files
      envDefaultFileId.value = r.data.defaultFileId
      envRemote.value = !!(r.data as { remote?: boolean }).remote
      envRevealed.value = new Set()
    } else {
      envGroups.value = []
      envError.value = r.error ?? '读取 ENVIRONMENT_VARIABLE 失败'
    }
  } finally {
    envLoading.value = false
  }
}

function openEnvSettings(): void {
  envVisible.value = true
  envEditing.value = ''
  void loadEnvSettings()
}

/** ===== 基础服务（docker compose） ===== */
/** 展示名去掉项目前缀；docker 命令仍使用原始服务名 */
function svcName(s: string): string {
  return s.replace(/^maozi-cloud-basic-/, '')
}

/** 服务卡片视觉：按关键字匹配图标 + 品牌渐变 + 同色光晕（白底下低透明度；icon 为空串时回退显示名称首字母） */
const SVC_VISUALS: Array<{ match: RegExp; icon: string; grad: string; glow: string }> = [
  { match: /mysql/, icon: '🐬', grad: 'linear-gradient(135deg, #4ab3ff, #0b5cad)', glow: 'rgba(11, 92, 173, 0.32)' },
  { match: /redis/, icon: '', grad: 'linear-gradient(135deg, #fb7185, #be123c)', glow: 'rgba(190, 18, 60, 0.3)' },
  { match: /nacos/, icon: '🧭', grad: 'linear-gradient(135deg, #2dd4bf, #0f766e)', glow: 'rgba(15, 118, 110, 0.3)' },
  { match: /seata/, icon: '🔗', grad: 'linear-gradient(135deg, #38bdf8, #1d4ed8)', glow: 'rgba(29, 78, 216, 0.3)' },
  { match: /xxljob/, icon: '⏰', grad: 'linear-gradient(135deg, #fbbf24, #b45309)', glow: 'rgba(180, 83, 9, 0.3)' },
  { match: /snailjob/, icon: '🐌', grad: 'linear-gradient(135deg, #a3e635, #4d7c0f)', glow: 'rgba(77, 124, 15, 0.3)' },
  { match: /grafana/, icon: '📊', grad: 'linear-gradient(135deg, #fb923c, #c2410c)', glow: 'rgba(194, 65, 12, 0.3)' },
  { match: /prometheus/, icon: '🔥', grad: 'linear-gradient(135deg, #f97316, #b91c1c)', glow: 'rgba(185, 28, 28, 0.3)' },
  { match: /tempo/, icon: '⏱', grad: 'linear-gradient(135deg, #818cf8, #4338ca)', glow: 'rgba(67, 56, 202, 0.3)' },
  { match: /loki/, icon: '🗃', grad: 'linear-gradient(135deg, #facc15, #a16207)', glow: 'rgba(161, 98, 7, 0.3)' },
  { match: /promtail/, icon: '📨', grad: 'linear-gradient(135deg, #2dd4bf, #155e75)', glow: 'rgba(21, 94, 117, 0.3)' },
  { match: /rocketmq/, icon: '🚀', grad: 'linear-gradient(135deg, #f472b6, #be185d)', glow: 'rgba(190, 24, 93, 0.3)' },
  { match: /rabbitmq/, icon: '🐰', grad: 'linear-gradient(135deg, #fda4af, #e11d48)', glow: 'rgba(225, 29, 72, 0.3)' },
  { match: /gateway/, icon: '🛡', grad: 'linear-gradient(135deg, #38bdf8, #0369a1)', glow: 'rgba(3, 105, 161, 0.3)' },
  { match: /monitor/, icon: '📈', grad: 'linear-gradient(135deg, #2dd4bf, #0f766e)', glow: 'rgba(15, 118, 110, 0.3)' },
  { match: /oauth/, icon: '🔐', grad: 'linear-gradient(135deg, #a78bfa, #6d28d9)', glow: 'rgba(109, 40, 217, 0.3)' },
  { match: /system/, icon: '⚙', grad: 'linear-gradient(135deg, #94a3b8, #334155)', glow: 'rgba(51, 65, 85, 0.3)' },
  { match: /-ai-/, icon: '🤖', grad: 'linear-gradient(135deg, #f472b6, #be185d)', glow: 'rgba(190, 24, 93, 0.3)' },
  { match: /admin/, icon: '🖥', grad: 'linear-gradient(135deg, #818cf8, #4338ca)', glow: 'rgba(67, 56, 202, 0.3)' },
  { match: /monomer/, icon: '🧩', grad: 'linear-gradient(135deg, #fb923c, #c2410c)', glow: 'rgba(194, 65, 12, 0.3)' },
  { match: /elasticsearch|es/, icon: '🔍', grad: 'linear-gradient(135deg, #fcd34d, #d97706)', glow: 'rgba(217, 119, 6, 0.3)' }
]

function svcVisual(s: string): { icon: string; grad: string; glow: string } {
  return (
    SVC_VISUALS.find((v) => v.match.test(s)) ?? {
      icon: '📦',
      grad: 'linear-gradient(135deg, #94a3b8, #475569)',
      glow: 'rgba(71, 85, 105, 0.6)'
    }
  )
}

/** 头像样式：品牌渐变；运行中附加同色光晕，停止时保留浅色内高光 */
function svcAvaStyle(s: string): Record<string, string> {
  const v = svcVisual(s)
  const inset = 'inset 0 1px 0 rgba(255, 255, 255, 0.3)'
  return {
    background: v.grad,
    boxShadow: isRunning(s) ? `0 6px 16px ${v.glow}, ${inset}` : inset
  }
}

const composeServices = ref<string[]>([])
const composeError = ref('')
const composeStatuses = ref<Record<string, string>>({})
const composeStatusError = ref('')

function isRunning(s: string): boolean {
  return (composeStatuses.value[s] ?? '') === 'running'
}

function stateText(s: string): string {
  if (isRunning(s)) return '运行中'
  return composeStatusError.value ? '未知' : '已停止'
}

function stateClass(s: string): string {
  return isRunning(s) ? 'on' : 'off'
}

async function loadComposeServices(): Promise<void> {
  if (!binding.value) {
    composeServices.value = []
    return
  }
  const r = await api.projects.composeServices()
  if (r.ok && r.data) {
    composeServices.value = r.data
    composeError.value = ''
  } else {
    composeServices.value = []
    composeError.value = r.error ?? '读取 docker-compose.yml 失败'
  }
  await loadComposeStatuses()
}

const statusLoading = ref(false)

async function loadComposeStatuses(): Promise<void> {
  if (!binding.value) {
    composeStatuses.value = {}
    adminState.value = 'unknown'
    return
  }
  statusLoading.value = true
  try {
    await doLoadComposeStatuses()
  } finally {
    statusLoading.value = false
  }
  await loadAdminStatus()
}

/** ===== 后台前端容器（maozi-cloud-admin-distributeds）状态与启停 ===== */
const adminState = ref('unknown')

const adminRunning = computed(() => adminState.value === 'running')

/** ===== 初始化 Hosts（项目 maozi-cloud-deploy-run/HOSTS -> 系统 /etc/hosts） ===== */
const hostsInit = ref<{ total: number; missing: number; initialized: boolean } | null>(null)

const hostsInitState = computed(() => hostsInit.value ?? { total: 0, missing: 0, initialized: false })

/** ===== 容器网段（compose networks.default.external.name -> docker network create） ===== */
const networkState = ref<{ name: string; exists: boolean } | null>(null)

async function loadNetworkStatus(): Promise<void> {
  if (!binding.value) {
    networkState.value = null
    return
  }
  const r = await api.projects.networkStatus()
  networkState.value = r.ok && r.data ? r.data : null
}

/** ===== 初始化数据库（INIT_MYSQL_DB：表名=SQL脚本，按需启动 mysql 导入缺失表） ===== */
const dbInitState = ref<{ count: number; initialized: boolean; missing: number }>({ count: 0, initialized: false, missing: 0 })

async function loadDbInitStatus(): Promise<void> {
  if (!binding.value) {
    dbInitState.value = { count: 0, initialized: false, missing: 0 }
    return
  }
  const r = await api.projects.dbInitStatus()
  dbInitState.value = r.ok && r.data ? r.data : { count: 0, initialized: false, missing: 0 }
}

/** 初始化数据库：按需启动 mysql、逐表检测导入；日志走执行日志会话 */
async function onDbInit(skipConfirm = false): Promise<void> {
  if (focusRunning((a) => a.type === 'dbInit')) return
  const s = startSession(
    { type: 'dbInit' },
    {
      label: '初始化数据库',
      icon: '🗄',
      title: '数据库初始化 · INIT_MYSQL_DB',
      sub: 'MySQL 按需启动 · SQL 脚本逐个导入'
    }
  )
  try {
    const r = await api.projects.dbInit(s.id)
    finishSession(s, r.ok)
    if (!r.ok && !stopRequestedSids.has(s.id)) ElMessage.error(r.error ?? '数据库初始化失败')
    else if (r.ok && !stopRequestedSids.has(s.id)) ElMessage.success('数据库初始化完成')
  } finally {
    s.running = false
    stopRequestedSids.delete(s.id)
    // 无论成功/失败/中断都刷新一次：标记由主进程写入，状态以最新查询为准
    await loadDbInitStatus()
  }
}

/** ===== 应用服务（单体 / 微服务）：Tab 互斥切换，选中状态持久化 .ui-state.json ===== */
type AppSvcVariant = 'monomer' | 'distributeds'

const APP_SVC_LABELS: Record<AppSvcVariant, string> = { monomer: '单体服务', distributeds: '微服务' }
const APP_SVC_ICONS: Record<AppSvcVariant, string> = { monomer: '🧩', distributeds: '☁️' }
const APP_SVC_FILE_NAMES: Record<AppSvcVariant, Record<'admin' | 'services', string>> = {
  monomer: { admin: 'maozi-cloud-admin-monomer-docker.yml', services: 'maozi-cloud-services-monomer-docker.yml' },
  distributeds: {
    admin: 'maozi-cloud-admin-distributeds-docker.yml',
    services: 'maozi-cloud-services-distributeds-docker.yml'
  }
}
const OTHER_APP_VARIANT: Record<AppSvcVariant, AppSvcVariant> = { monomer: 'distributeds', distributeds: 'monomer' }

const appSvcTab = ref<AppSvcVariant>('distributeds')
const appServices = ref<Record<AppSvcVariant, AppServiceEntry[]>>({ monomer: [], distributeds: [] })
const appSvcStatuses = ref<Record<AppSvcVariant, Record<string, string>>>({ monomer: {}, distributeds: {} })

/** 展示名去掉 maozi-cloud- 前缀与最后一个 - 后缀段（gateway-service → gateway） */
function appSvcName(s: string): string {
  return s.replace(/^maozi-cloud-/, '').replace(/-[^-]+$/, '')
}

function isAppSvcRunning(v: AppSvcVariant, s: string): boolean {
  return (appSvcStatuses.value[v][s] ?? '') === 'running'
}

function appSvcRunningCount(v: AppSvcVariant): number {
  return appServices.value[v].filter((x) => isAppSvcRunning(v, x.name)).length
}

const activeAppSvcServices = computed(() => appServices.value[appSvcTab.value])

/** Tab 切换：记忆到 .ui-state.json（git 忽略） */
async function switchAppSvcTab(v: AppSvcVariant): Promise<void> {
  if (appSvcTab.value === v) return
  appSvcTab.value = v
  void api.projects.uiStateSave({ appSvcTab: v })
}

/** ===== 远程服务器（SSH）绑定 ===== */
/** 绑定两步选择：'' 未选 / 'local' 本地 / 'remote' 远程 */
const bindMode = ref<'' | 'local' | 'remote'>('')
const sshBindVisible = ref(false)
const sshConfigs = ref<Array<{ id: string; name: string; address: string }>>([])
const sshConfigId = ref('')
const sshConnected = ref(false)
const sshTesting = ref(false)
const sshCwd = ref('/')
const sshDirs = ref<string[]>([])
const sshLoadingDir = ref(false)
const sshError = ref('')

async function loadSshConfigs(): Promise<void> {
  const r = await api.projects.sshConfigs()
  sshConfigs.value = r.ok && r.data ? r.data : []
}

/** 绑定页内嵌的 SSH 连接（连接成功后显示操作选项） */
/** 打开远程目录浏览器（已连接时补加载根目录列表） */
function openSshBrowser(): void {
  sshBrowserMode.value = 'bind'
  sshBindVisible.value = true
  if (sshConnected.value && sshDirs.value.length === 0) {
    void browseSshDir('/')
  }
}

/** 目录浏览器模式：bind=读取 CONFIG 绑定当前目录；clone=选父目录 + Git 密钥远程拉取代码 */
const sshBrowserMode = ref<'bind' | 'clone'>('bind')

/** 拉取代码（远程）：进入目录浏览器的拉取模式 */
function openSshCloneBrowser(): void {
  sshBrowserMode.value = 'clone'
  sshBindVisible.value = true
  if (sshConnected.value && sshDirs.value.length === 0) {
    void browseSshDir('/')
  }
  void loadGitSecrets()
}

/** 加载 Git 类型密钥（本地/远程拉取代码共用） */
async function loadGitSecrets(): Promise<void> {
  const r = await api.configs.list()
  secrets.value = r.ok && r.data ? r.data.configs.filter((s) => s.type === 'Git') : []
  cloneSecretId.value = ''
}

async function testSshFromBind(): Promise<void> {
  if (!sshConfigId.value) return
  sshTesting.value = true
  sshError.value = ''
  sshConnected.value = false
  try {
    const r = await api.projects.sshTest(sshConfigId.value)
    if (r.ok) {
      sshConnected.value = true
      sshCwd.value = '/'
      await browseSshDir('/')
    } else {
      sshError.value = r.error ?? '连接失败'
    }
  } finally {
    sshTesting.value = false
  }
}

async function testSsh(): Promise<void> {
  if (!sshConfigId.value) return
  sshTesting.value = true
  sshError.value = ''
  sshConnected.value = false
  try {
    const r = await api.projects.sshTest(sshConfigId.value)
    if (r.ok) {
      sshConnected.value = true
      sshCwd.value = '/'
      await browseSshDir('/')
    } else {
      sshError.value = r.error ?? '连接失败'
    }
  } finally {
    sshTesting.value = false
  }
}

async function browseSshDir(dir: string): Promise<void> {
  sshLoadingDir.value = true
  sshError.value = ''
  try {
    const r = await api.projects.sshListDir(sshConfigId.value, dir)
    if (r.ok && r.data) {
      sshCwd.value = r.data.path
      sshDirs.value = r.data.items.filter((i) => i.isDir).map((i) => i.name)
    } else {
      sshError.value = r.error ?? '目录读取失败'
    }
  } finally {
    sshLoadingDir.value = false
  }
}

/** 选择目录页：在当前浏览目录下新建子目录，创建成功后刷新列表 */
const SSH_DIR_NAME_RE = /^[A-Za-z0-9._\u4e00-\u9fa5-]{1,64}$/
async function createSshDir(): Promise<void> {
  let name = ''
  try {
    const r = await ElMessageBox.prompt(`将在 ${sshCwd.value} 下创建子目录`, '创建目录', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '目录名（中文 / 字母 / 数字 / . _ -）',
      inputValidator: (v: string): string | boolean => {
        const n = (v ?? '').trim()
        return SSH_DIR_NAME_RE.test(n) && n !== '.' && n !== '..' ? true : '目录名仅支持中文、字母、数字、点、下划线、连字符（1-64 字符）'
      }
    })
    name = (r.value ?? '').trim()
  } catch {
    return /* 用户取消 */
  }
  const res = await api.projects.sshMkdir(sshConfigId.value, sshCwd.value, name)
  if (res.ok) {
    ElMessage.success(`已创建目录：${name}`)
    await browseSshDir(sshCwd.value)
  } else {
    sshError.value = res.error ?? '创建目录失败'
  }
}

async function confirmSshBind(): Promise<void> {
  if (sshBrowserMode.value === 'clone') {
    await confirmSshClone()
    return
  }
  sshError.value = ''
  try {
    const r = await api.projects.sshBindDir(sshConfigId.value, sshCwd.value)
    if (r.ok && r.data) {
      ElMessage.success(`远程绑定成功：${r.data.name}`)
      sshBindVisible.value = false
      await loadState()
    } else {
      sshError.value = r.error ?? '绑定失败'
    }
  } catch (err) {
    sshError.value = (err as Error).message
  }
}

/** 拉取模式确认：选 Git 密钥 → SSH 在远程 git clone 到 <当前目录>/maozi-cloud → 自动绑定；日志走脚本日志会话 */
async function confirmSshClone(): Promise<void> {
  if (!cloneSecretId.value) {
    ElMessage.warning('请选择用于拉取代码的 Git 密钥')
    return
  }
  if (focusRunning((a) => a.type === 'sshClone')) return
  const s = startSession({ type: 'sshClone' }, {
    label: `远程拉取 → ${sshCwd.value}/maozi-cloud`,
    icon: '⬇️',
    title: '远程拉取代码',
    sub: `SSH git clone → ${sshCwd.value}/maozi-cloud`
  })
  sshError.value = ''
  try {
    const r = await api.projects.sshClone(sshConfigId.value, cloneSecretId.value, sshCwd.value, s.id)
    finishSession(s, r.ok)
    if (r.ok && r.data) {
      ElMessage.success(`拉取完成并绑定：${r.data.name}${r.data.version ? `（v${r.data.version}）` : ''}`)
      sshBindVisible.value = false
      await loadState()
    } else if (!stopRequestedSids.has(s.id)) {
      ElMessage.error(r.error ?? '远程拉取失败')
    }
  } catch (err) {
    finishSession(s, false)
    if (!stopRequestedSids.has(s.id)) ElMessage.error((err as Error).message)
  }
}

/** ===== 全链路日志查询：按链路 ID 检索所有运行中微服务容器的日志 ===== */
const traceVisible = ref(false)
const traceId = ref('')
const traceLoading = ref(false)
const traceError = ref('')
const traceResults = ref<string[]>([])
const traceSummary = ref('')

function openTraceQuery(): void {
  traceVisible.value = true
}

async function runTraceQuery(): Promise<void> {
  const kw = traceId.value.trim()
  if (!kw) {
    ElMessage.warning('请输入链路 ID')
    return
  }
  traceLoading.value = true
  traceError.value = ''
  traceResults.value = []
  try {
    const r = await api.projects.traceLogQuery(kw)
    if (r.ok && r.data) {
      traceResults.value = r.data.results
      traceSummary.value = r.data.message
    } else {
      traceError.value = r.error ?? '查询失败'
    }
  } finally {
    traceLoading.value = false
  }
}

/** 面板收起/展开：状态持久化 .ui-state.json */
const basicsCollapsed = ref(false)
const appSvcCollapsed = ref(false)

function toggleBasicsCollapsed(): void {
  basicsCollapsed.value = !basicsCollapsed.value
  void api.projects.uiStateSave({ basicsCollapsed: basicsCollapsed.value })
}

function toggleAppSvcCollapsed(): void {
  appSvcCollapsed.value = !appSvcCollapsed.value
  void api.projects.uiStateSave({ appSvcCollapsed: appSvcCollapsed.value })
}

async function loadUiState(): Promise<void> {
  const r = await api.projects.uiStateGet()
  if (r.ok && r.data) {
    const t = r.data.appSvcTab
    if (t === 'monomer' || t === 'distributeds') appSvcTab.value = t
    if (typeof r.data.basicsCollapsed === 'boolean') basicsCollapsed.value = r.data.basicsCollapsed
    if (typeof r.data.appSvcCollapsed === 'boolean') appSvcCollapsed.value = r.data.appSvcCollapsed
  }
}

async function loadAppServices(): Promise<void> {
  if (!binding.value) return
  await Promise.all(
    (['monomer', 'distributeds'] as AppSvcVariant[]).map(async (v) => {
      const r = await api.projects.appServices(v)
      appServices.value[v] = r.ok && r.data ? r.data.services : []
    })
  )
}

async function loadAppSvcStatuses(): Promise<void> {
  if (!binding.value) return
  await Promise.all(
    (['monomer', 'distributeds'] as AppSvcVariant[]).map(async (v) => {
      const r = await api.projects.appServicesStatus(v)
      appSvcStatuses.value[v] = ((r.ok && r.data ? r.data : {}) ?? {}) as Record<string, string>
    })
  )
}

const appSvcStats = ref<Record<AppSvcVariant, Record<string, ComposeServiceStats>>>({ monomer: {}, distributeds: {} })

/** 首次统计是否已返回（未返回前环形卡数值做呼吸占位） */
const appStatsLoaded = ref(false)
let appStatsInFlight = false

async function loadAppSvcStats(preferCache = false): Promise<void> {
  if (!binding.value || appStatsInFlight) return
  appStatsInFlight = true
  try {
    const r = await api.projects.appServicesStats(preferCache)
    if (r.ok && r.data) {
      appSvcStats.value = r.data.stats
      hostCpuCount.value = r.data.cpuCount || 1
      hostMemTotal.value = r.data.hostMemTotal || 0
    } else {
      appSvcStats.value = { monomer: {}, distributeds: {} }
    }
  } catch {
    appSvcStats.value = { monomer: {}, distributeds: {} }
  } finally {
    appStatsInFlight = false
    appStatsLoaded.value = true
  }
}

/** 当前 Tab 某服务的资源占用 */
function appStat(v: AppSvcVariant, name: string): ComposeServiceStats | undefined {
  return appSvcStats.value[v]?.[name]
}

/** 当前 Tab 变体的汇总（CPU/内存合计，仅运行中的服务有统计） */
const appSvcTotal = computed(() => {
  let cpu = 0
  let memUsed = 0
  let memLimit = 0
  for (const x of appServices.value[appSvcTab.value]) {
    const st = appSvcStats.value[appSvcTab.value]?.[x.name]
    if (!st) continue
    cpu += st.cpuPercent
    memUsed += st.memUsed
    memLimit += st.memLimit
  }
  return { cpu, memUsed, memLimit }
})

const appSvcCpuBarValue = computed(() => {
  const cap = Math.max(hostCpuCount.value, 1) * 100
  return Math.min((appSvcTotal.value.cpu / cap) * 100, 100)
})

const appSvcMemDenom = computed(() => {
  const entries = Object.values(appSvcStats.value[appSvcTab.value] ?? {})
  const allLimited = entries.length > 0 && entries.every((st) => st.memLimit > 0)
  return allLimited ? appSvcTotal.value.memLimit : hostMemTotal.value
})

const appSvcMemBarValue = computed(() =>
  appSvcMemDenom.value > 0 ? Math.min((appSvcTotal.value.memUsed / appSvcMemDenom.value) * 100, 100) : 0
)

const appSvcCpuRingStyle = computed(() => ringStyle(appSvcCpuBarValue.value, RING_COLORS[totalLevel(appSvcCpuBarValue.value)]))
const appSvcMemRingStyle = computed(() => ringStyle(appSvcMemBarValue.value, RING_COLORS_MEM[totalLevel(appSvcMemBarValue.value)]))
const appSvcRingStyle = computed(() =>
  ringStyle(
    activeAppSvcServices.value.length ? (appSvcRunningCount(appSvcTab.value) / activeAppSvcServices.value.length) * 100 : 0,
    ['#34d399', '#059669']
  )
)

/** ===== 互斥切换确认弹窗：左（将被关闭）→ 右（即将启动）的可视化确认 ===== */
interface MutexPayload {
  desc: string
  stopLabel: string
  stopList: string[]
  startLabel: string
  startSub: string
}

const mutexVisible = ref(false)
const mutexPayload = ref<MutexPayload | null>(null)
let mutexResolve: ((v: boolean) => void) | null = null

function confirmMutex(payload: MutexPayload): Promise<boolean> {
  mutexPayload.value = payload
  mutexVisible.value = true
  return new Promise((resolve) => {
    mutexResolve = resolve
  })
}

function closeMutex(result: boolean): void {
  mutexVisible.value = false
  mutexResolve?.(result)
  mutexResolve = null
}

/** 互斥确认：启动/重启前另一变体有运行服务时弹可视化确认（skipConfirm 用于重新执行） */
async function confirmAppSvcMutex(v: AppSvcVariant, what: string, skipConfirm: boolean): Promise<boolean> {
  if (skipConfirm) return true
  const other = OTHER_APP_VARIANT[v]
  const otherRunning = appServices.value[other].filter((x) => isAppSvcRunning(other, x.name))
  if (otherRunning.length === 0) return true
  return await confirmMutex({
    desc: `启动「${APP_SVC_LABELS[v]} · ${what}」前，需先关闭${APP_SVC_LABELS[other]}的全部运行容器`,
    stopLabel: APP_SVC_LABELS[other],
    stopList: otherRunning.map((x) => x.name),
    startLabel: what,
    startSub: APP_SVC_LABELS[v]
  })
}

/** 单服务操作：start/stop/restart；主进程在 start/restart 前自动 down 掉另一变体 */
async function onAppService(svc: AppServiceEntry, action: 'start' | 'stop' | 'restart', skipConfirm = false): Promise<void> {
  const v = appSvcTab.value
  const verb = action === 'start' ? '启动' : action === 'stop' ? '关闭' : '重启'
  if (focusRunning((a) => a.type === 'appSvc' && a.service === svc.name && a.action === action)) return
  if (action !== 'stop' && !(await confirmAppSvcMutex(v, `${verb} ${appSvcName(svc.name)}`, skipConfirm))) return
  const yml = APP_SVC_FILE_NAMES[v][svc.file]
  const s = startSession(
    { type: 'appSvc', variant: v, file: svc.file, service: svc.name, action },
    {
      label: `${appSvcName(svc.name)} ${verb}`,
      icon: APP_SVC_ICONS[v],
      title: `${APP_SVC_LABELS[v]} · ${verb} ${appSvcName(svc.name)}`,
      sub:
        action === 'restart'
          ? `docker compose -f ${yml} rm -sf ${svc.name} && up -d · 实时输出`
          : `docker compose -f ${yml} ${action === 'start' ? `up -d ${svc.name}` : `rm -sf ${svc.name}`} · 实时输出`
    }
  )
  try {
    const r = await api.projects.appServiceAction(v, svc.file, svc.name, action, s.id)
    finishSession(s, r.ok)
    if (!r.ok && !stopRequestedSids.has(s.id)) ElMessage.error(r.error ?? 'docker 操作失败')
    else if (r.ok && !stopRequestedSids.has(s.id)) ElMessage.success(`服务 ${appSvcName(svc.name)} 已${verb}`)
  } finally {
    s.running = false
    stopRequestedSids.delete(s.id)
    await loadAppSvcStatuses()
    void loadAppSvcStats()
  }
}

/** 全量启动/关闭当前 Tab 变体；启动前主进程自动 down 掉另一变体 */
async function onAppServiceAll(action: 'start' | 'stop', skipConfirm = false): Promise<void> {
  const v = appSvcTab.value
  if (focusRunning((a) => a.type === 'appSvcAll' && a.variant === v && a.action === action)) return
  if (action === 'start' && !(await confirmAppSvcMutex(v, '全部服务', skipConfirm))) return
  const s = startSession(
    { type: 'appSvcAll', variant: v, action },
    {
      label: `${APP_SVC_LABELS[v]}全部${action === 'start' ? '启动' : '关闭'}`,
      icon: APP_SVC_ICONS[v],
      title: `${APP_SVC_LABELS[v]} · ${action === 'start' ? '全部启动' : '全部关闭'}`,
      sub: `${APP_SVC_FILE_NAMES[v].services} + ${APP_SVC_FILE_NAMES[v].admin} · 实时输出`
    }
  )
  try {
    const r = await api.projects.appServiceAll(v, action, s.id)
    if (!r.ok) {
      finishSession(s, false)
      ElMessage.error(r.error ?? 'docker 操作失败')
      return
    }
    // 后台任务：不立即完成会话，等完成标记 __APP_SVC_DONE__/__APP_SVC_FAIL__ 触发
  } catch {
    finishSession(s, false)
  }
}

/** 全量启动/关闭当前 Tab 变体；启动前主进程自动 down 掉另一变体 */

/** 环境准备就绪：hosts 全部写入、容器网络（若定义）已存在、数据库（若定义）已初始化 */
const prepReady = computed(
  () =>
    hostsInitState.value.initialized &&
    (!networkState.value?.name || networkState.value.exists) &&
    (dbInitState.value.count === 0 || dbInitState.value.initialized)
)

/** 创建容器网络：docker network create（幂等），日志走执行日志会话 */
async function onNetworkCreate(skipConfirm = false): Promise<void> {
  if (focusRunning((a) => a.type === 'networkCreate')) return
  const name = networkState.value?.name ?? ''
  const s = startSession(
    { type: 'networkCreate' },
    {
      label: '添加容器网段',
      icon: '🔗',
      title: '容器网段 · docker network create',
      sub: `${name || 'docker-compose.yml networks.default.external.name'} · 解析自 docker-compose.yml`
    }
  )
  try {
    const r = await api.projects.networkCreate(s.id)
    finishSession(s, r.ok)
    if (!r.ok && !stopRequestedSids.has(s.id)) ElMessage.error(r.error ?? '容器网段创建失败')
    else if (r.ok && !stopRequestedSids.has(s.id)) {
      ElMessage.success(`容器网段 ${name} 已就绪`)
      await loadNetworkStatus()
    }
  } finally {
    s.running = false
    stopRequestedSids.delete(s.id)
  }
}

async function loadHostsInitStatus(): Promise<void> {
  if (!binding.value) {
    hostsInit.value = null
    return
  }
  const r = await api.projects.hostsInitStatus()
  hostsInit.value = r.ok && r.data ? r.data : null
}

async function onHostsInit(skipConfirm = false): Promise<void> {
  if (!skipConfirm) {
    const missing = hostsInit.value?.missing ?? 0
    try {
      await ElMessageBox.confirm(
        `将把项目 maozi-cloud-deploy-run/HOSTS 中缺失的 ${missing} 条映射写入系统 /etc/hosts（已设置的忽略），需要管理员授权并自动刷新 DNS 缓存。确定继续吗？`,
        '初始化 Hosts',
        { type: 'warning', confirmButtonText: '初始化', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  if (focusRunning((a) => a.type === 'hostsInit')) return
  const s = startSession(
    { type: 'hostsInit' },
    {
      label: '初始化Hosts',
      icon: '🌐',
      title: '初始化 Hosts · /etc/hosts',
      sub: 'maozi-cloud-deploy-run/HOSTS · 实时输出'
    }
  )
  try {
    const r = await api.projects.hostsInit(s.id)
    finishSession(s, r.ok)
    if (!r.ok && !stopRequestedSids.has(s.id)) ElMessage.error(r.error ?? '初始化失败')
    else if (r.ok && !stopRequestedSids.has(s.id)) {
      ElMessage.success('Hosts 初始化完成')
      await loadHostsInitStatus()
    }
  } finally {
    s.running = false
    stopRequestedSids.delete(s.id)
  }
}

async function loadAdminStatus(): Promise<void> {
  if (!binding.value) {
    adminState.value = 'unknown'
    return
  }
  const r = await api.projects.adminContainerStatus()
  adminState.value = r.ok && r.data ? r.data : 'unknown'
}

/** 关闭后台前端容器：docker compose stop（容器保留可随时重启） */
async function onAdminStop(skipConfirm = false): Promise<void> {
  if (!skipConfirm) {
    try {
      await ElMessageBox.confirm(
        '将停止并移除后台前端容器 maozi-cloud-admin-distributeds（docker compose down，镜像保留，重新启动会自动重建）。确定继续吗？',
        '后台关闭',
        { type: 'warning', confirmButtonText: '关闭', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  if (focusRunning((a) => a.type === 'adminStop')) return
  const s = startSession(
    { type: 'adminStop' },
    {
      label: '后台关闭',
      icon: '🖥️',
      title: '后台前端容器 · 后台关闭',
      sub: 'docker compose down · maozi-cloud-admin-distributeds-docker.yml · 实时输出'
    }
  )
  try {
    const r = await api.projects.adminContainerAction('stop', s.id)
    finishSession(s, r.ok)
    if (!r.ok && !stopRequestedSids.has(s.id)) ElMessage.error(r.error ?? 'docker 操作失败')
    else if (r.ok && !stopRequestedSids.has(s.id)) {
      ElMessage.success('后台前端容器已关闭')
      await loadAdminStatus()
    }
  } finally {
    s.running = false
    stopRequestedSids.delete(s.id)
  }
}

async function doLoadComposeStatuses(): Promise<void> {
  const r = await api.projects.composeStatus()
  if (r.ok && r.data) {
    composeStatuses.value = r.data
    composeStatusError.value = ''
  } else {
    composeStatuses.value = {}
    composeStatusError.value = r.error ?? 'docker 状态查询失败'
  }
}

/** ===== 实时资源占用：docker stats 一次性快照，前端每 3 秒轮询 ===== */
const composeStats = ref<Record<string, ComposeServiceStats>>({})
const hostCpuCount = ref(1)
const hostMemTotal = ref(0)
let statsInFlight = false
let statsTimer: ReturnType<typeof setInterval> | undefined

/** 首次统计是否已返回（未返回前大数字做呼吸占位，与服务全停的 0% 区分） */
const statsLoaded = ref(false)

async function loadComposeStats(preferCache = false): Promise<void> {
  if (!binding.value || statsInFlight) return
  statsInFlight = true
  try {
    const r = await api.projects.composeStats(preferCache)
    if (r.ok && r.data) {
      composeStats.value = r.data.stats
      hostCpuCount.value = r.data.cpuCount || 1
      hostMemTotal.value = r.data.hostMemTotal || 0
    } else {
      composeStats.value = {}
    }
  } catch {
    composeStats.value = {}
  } finally {
    statsInFlight = false
    statsLoaded.value = true
  }
}

const statsTotal = computed(() => {
  let cpu = 0
  let memUsed = 0
  let memLimit = 0
  for (const s of composeServices.value) {
    const st = composeStats.value[s]
    if (!st) continue
    cpu += st.cpuPercent
    memUsed += st.memUsed
    memLimit += st.memLimit
  }
  return { cpu, memUsed, memLimit }
})

const runningServiceCount = computed(() => composeServices.value.filter((s) => isRunning(s)).length)

/** CPU 条按全部核心归一化（docker 百分比为单核语义，总和可超 100） */
const cpuBarValue = computed(() => {
  const cap = Math.max(hostCpuCount.value, 1) * 100
  return Math.min((statsTotal.value.cpu / cap) * 100, 100)
})

/** 内存分母：全部运行容器都设了配额才按配额合计；有任一未设（含混合场景）用宿主机物理内存 */
const memAllLimited = computed(() => {
  const entries = Object.values(composeStats.value)
  return entries.length > 0 && entries.every((st) => st.memLimit > 0)
})

const memTotalDenom = computed(() => (memAllLimited.value ? statsTotal.value.memLimit : hostMemTotal.value))

const memBarValue = computed(() =>
  memTotalDenom.value > 0 ? Math.min((statsTotal.value.memUsed / memTotalDenom.value) * 100, 100) : 0
)

/** ===== 环形仪表（conic-gradient）：--p 为弧长百分比，颜色随负载等级切换 ===== */
const RING_COLORS: Record<string, [string, string]> = {
  '': ['#60a5fa', '#2563eb'],
  warn: ['#fbbf24', '#f59e0b'],
  high: ['#f87171', '#dc2626']
}
const RING_COLORS_MEM: Record<string, [string, string]> = {
  '': ['#a78bfa', '#7c3aed'],
  warn: ['#fbbf24', '#f59e0b'],
  high: ['#f87171', '#dc2626']
}

function ringStyle(p: number, colors: [string, string]): Record<string, string> {
  const v = Math.min(Math.max(Number.isFinite(p) ? p : 0, 0), 100)
  return { '--p': statsLoaded.value ? v.toFixed(2) : '0', '--c1': colors[0], '--c2': colors[1] }
}

const cpuRingStyle = computed(() => ringStyle(cpuBarValue.value, RING_COLORS[totalLevel(cpuBarValue.value)]))
const memRingStyle = computed(() => ringStyle(memBarValue.value, RING_COLORS_MEM[totalLevel(memBarValue.value)]))
const svcRingStyle = computed(() =>
  ringStyle(composeServices.value.length ? (runningServiceCount.value / composeServices.value.length) * 100 : 0, ['#34d399', '#059669'])
)

/** 服务卡片顶部品牌栏：品牌色柔和斜向渐变（配合点阵纹理层） */
function svcBandStyle(s: string): Record<string, string> {
  const soft = svcVisual(s).glow.replace(/,\s*[\d.]+\)$/, ', 0.1)')
  return { background: `linear-gradient(135deg, ${soft}, rgba(255, 255, 255, 0) 68%)` }
}

/** ===== 服务列表筛选：关键字搜索 + 状态（全部 / 运行中 / 已停止） ===== */
const svcFilter = ref<'all' | 'running' | 'stopped'>('all')
const svcSearch = ref('')

const stoppedServiceCount = computed(() => composeServices.value.length - runningServiceCount.value)

const filteredServices = computed(() => {
  const kw = svcSearch.value.trim().toLowerCase()
  return composeServices.value.filter((s) => {
    if (svcFilter.value === 'running' && !isRunning(s)) return false
    if (svcFilter.value === 'stopped' && isRunning(s)) return false
    if (kw && !s.toLowerCase().includes(kw) && !svcName(s).toLowerCase().includes(kw)) return false
    return true
  })
})

const svcNoneText = computed(() => {
  const kw = svcSearch.value.trim()
  if (kw) return `没有名称包含「${kw}」的服务`
  return svcFilter.value === 'stopped' ? '没有已停止的服务 · 全部运行中 ✅' : '没有运行中的服务'
})

/** 是否存在未设内存配额的运行容器（展示「未设上限」标注） */
const memUnlimited = computed(() => !memAllLimited.value)

/** 负载等级：正常 → 偏高(琥珀) → 过载(红)，用于进度条与走势柱变色 */
function totalLevel(v: number): string {
  if (v >= 85) return 'high'
  return v >= 60 ? 'warn' : ''
}

/** CPU 占比（相对 CPU 配额）；未设配额返回 0，不显示比例条 */
function svcCpuPct(st: ComposeServiceStats): number {
  if (st.cpusLimit <= 0) return 0
  return Math.min((st.cpuPercent / (st.cpusLimit * 100)) * 100, 100)
}

function svcCpuBar(st: ComposeServiceStats): string {
  return `${svcCpuPct(st)}%`
}

/** 内存占比（相对容器配额）；未设配额返回 0，不显示比例条 */
function svcMemPct(st: ComposeServiceStats): number {
  if (st.memLimit <= 0) return 0
  return Math.min((st.memUsed / st.memLimit) * 100, 100)
}

function svcMemBar(st: ComposeServiceStats): string {
  return `${svcMemPct(st)}%`
}

/** 负载等级（仅设了配额时才有"超配额"语义） */
function svcCpuLevel(st: ComposeServiceStats): string {
  if (st.cpusLimit <= 0) return ''
  const pct = svcCpuPct(st)
  if (pct >= 90) return 'high'
  return pct >= 60 ? 'warn' : ''
}

function svcMemLevel(st: ComposeServiceStats): string {
  if (st.memLimit <= 0) return ''
  const pct = svcMemPct(st)
  if (pct >= 90) return 'high'
  return pct >= 70 ? 'warn' : ''
}

function fmtBytes(n: number): string {
  if (!n || n <= 0) return '0 B'
  const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB']
  let v = n
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v >= 100 ? v.toFixed(0) : v.toFixed(1)} ${units[i]}`
}

/**
 * 资源轮询生命周期（组件被 KeepAlive 缓存）：
 * - 首次挂载与每次回到页面：onActivated 启动 3s 轮询并刷新运行状态
 * - 切走导航：onDeactivated 暂停轮询并收起弹窗（会话与日志订阅保留，数据继续累积）
 */
onActivated(() => {
  if (!statsTimer)
    statsTimer = setInterval(() => {
      void loadComposeStats()
      void loadAppSvcStats()
      void loadAppSvcStatuses()
    }, binding.value?.remote ? 10000 : 3000)
  void loadComposeStatuses()
  // 回到页面时刷新准备态：hosts 与容器网段可能已变化（如用户改了 docker-compose.yml 网段名）
  void loadHostsInitStatus()
  void loadNetworkStatus()
  void loadDbInitStatus()
  void loadAppSvcStatuses()
  // 回页首拉带 preferCache：1 分钟内的快照直接渲染，随后由轮询刷新
  void loadComposeStats(true)
  void loadAppSvcStats(true)
})

onDeactivated(() => {
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = undefined
  }
  stopLogFollow()
  scriptVisible.value = false
  logVisible.value = false
  cloneVisible.value = false
  envVisible.value = false
})

onUnmounted(() => {
  if (statsTimer) clearInterval(statsTimer)
})

async function onCompose(service: string, action: 'start' | 'stop' | 'restart', skipConfirm = false): Promise<void> {
  const isAll = service === 'all'
  const isAllStart = isAll && action === 'start'
  const isAllStop = isAll && action === 'stop'
  const isRestart = action === 'restart'
  if (focusRunning((a) => a.type === 'compose' && a.service === service && a.action === action)) return
  const s = startSession(
    { type: 'compose', service, action },
    {
      label: isAllStart
        ? '全量启动'
        : isAllStop
          ? '全量关闭'
          : `${svcName(service)} ${action === 'start' ? '启动' : action === 'stop' ? '关闭' : '重启'}`,
      icon: '🐳',
      title: isAllStart
        ? 'Docker 服务 · 全量中间件启动'
        : isAllStop
          ? 'Docker 服务 · 全量中间件关闭'
          : `Docker 服务 · ${action === 'start' ? '启动' : action === 'stop' ? '关闭' : '重启'} ${svcName(service)}`,
      sub: isAllStart
        ? 'docker compose up -d · 实时输出'
        : isAllStop
          ? 'docker compose down · 实时输出'
          : isRestart
            ? `docker compose rm -sf ${service} && docker compose up -d ${service} · 实时输出`
            : `docker compose ${action === 'start' ? `up -d ${service}` : `rm -sf ${service}`} · 实时输出`
    }
  )
  try {
    const r = await api.projects.composeAction(service, action, s.id)
    finishSession(s, r.ok)
    if (!r.ok && !stopRequestedSids.has(s.id)) ElMessage.error(r.error ?? 'docker 操作失败')
    else if (r.ok && !stopRequestedSids.has(s.id)) {
      ElMessage.success(
        isAllStart
          ? '全部基础中间件已启动'
          : isAllStop
            ? '全部基础中间件已关闭'
            : action === 'start'
              ? `服务 ${svcName(service)} 已启动`
              : action === 'stop'
                ? `服务 ${svcName(service)} 已关闭`
                : `服务 ${svcName(service)} 已重启`
      )
      await loadComposeStatuses()
    }
  } finally {
    s.running = false
    stopRequestedSids.delete(s.id)
  }
}

/** 编译启动脚本：按目标变体互斥 —— 对侧有运行容器时先确认并在同一会话中关闭，再执行脚本 */
async function onRunScript(kind: DeployKind, skipConfirm = false): Promise<void> {
  // 同一脚本已在运行则聚焦其日志会话，否则新建会话并发执行
  if (focusRunning((a) => a.type === 'script' && a.kind === kind)) return
  const meta = DEPLOY_META[kind]
  if (!meta) {
    ElMessage.warning('该操作请在页面上重新发起')
    return
  }
  // 互斥：目标变体的对侧有运行容器时弹可视化确认（重新执行时静默处理）
  const other = OTHER_APP_VARIANT[meta.variant]
  const otherRunning = () => appServices.value[other].filter((x) => isAppSvcRunning(other, x.name))
  if (!skipConfirm && otherRunning().length > 0) {
    const go = await confirmMutex({
      desc: `执行「${meta.title}」前，需先关闭${APP_SVC_LABELS[other]}的全部运行容器`,
      stopLabel: APP_SVC_LABELS[other],
      stopList: otherRunning().map((x) => x.name),
      startLabel: meta.title,
      startSub: '部署脚本 · 编译 + 启动'
    })
    if (!go) return
  }
  const s = startSession({ type: 'script', kind }, { label: meta.title, ...meta })
  try {
    // 同一会话内先关闭对侧容器（若有运行），再执行编译启动脚本。
    // foreground 模式：等待真实完成且不发 __APP_SVC_DONE__——后台模式的标记会把本脚本会话提前标记完成
    if (otherRunning().length > 0) {
      const rs = await api.projects.appServiceAll(other, 'stop', s.id, { foreground: true })
      if (!rs.ok) {
        finishSession(s, false)
        ElMessage.error(rs.error ?? '关闭对侧服务失败')
        return
      }
      await loadAppSvcStatuses()
    }
    const r = await api.projects.runScript(kind, s.id)
    finishSession(s, r.ok)
    if (!r.ok && !stopRequestedSids.has(s.id)) ElMessage.error(r.error ?? '脚本执行失败')
    else if (r.ok && !stopRequestedSids.has(s.id)) ElMessage.success(meta.done)
  } finally {
    s.running = false
    stopRequestedSids.delete(s.id)
    // 脚本结束（含后台运行结束）后刷新应用服务状态与指标
    await loadAppSvcStatuses()
    void loadAppSvcStats()
    // 容器从 created → running 需要几秒，延迟补刷确保最终状态正确
    setTimeout(() => void loadAppSvcStatuses(), 5000)
  }
}

async function openCloneDialog(): Promise<void> {
  await loadGitSecrets()
  cloneDest.value = ''
  cloneLogs.value = []
  cloneVisible.value = true
}

async function pickCloneDest(): Promise<void> {
  const r = await api.projects.pickDir()
  if (!r.ok) {
    ElMessage.error(r.error ?? '选择目录失败')
    return
  }
  if (r.data) cloneDest.value = r.data
}

async function startClone(): Promise<void> {
  if (!cloneSecretId.value) {
    ElMessage.warning('请选择密钥')
    return
  }
  if (!cloneDest.value) {
    ElMessage.warning('请选择目标目录')
    return
  }
  // 每次拉取前清空上一轮 git 输出
  cloneLogs.value = []
  cloning.value = true
  try {
    const r = await api.projects.clone(cloneSecretId.value, cloneDest.value)
    if (!r.ok || !r.data) {
      ElMessage.error(r.error ?? '拉取失败')
      return
    }
    ElMessage.success(`拉取完成并绑定：${r.data.name}${r.data.version ? `（v${r.data.version}）` : ''}`)
    cloneVisible.value = false
    await loadState()
  } finally {
    cloning.value = false
  }
}

async function onOpenDir(): Promise<void> {
  const r = await api.projects.openDir()
  if (!r.ok) {
    ElMessage.error(r.error ?? '打开目录失败')
  }
}

/** ===== 项目 git 管理：仓库检测 / 拉取代码 / 切换分支 ===== */
const gitInfo = ref<{ isRepo: boolean; branch: string }>({ isRepo: false, branch: '' })
const gitPulling = ref(false)

async function loadGitInfo(): Promise<void> {
  const r = await api.projects.gitInfo()
  gitInfo.value = r.ok && r.data ? r.data : { isRepo: false, branch: '' }
}

/** 拉取代码（git pull）：日志走脚本日志会话，成功后刷新分支与项目版本 */
async function onGitPull(): Promise<void> {
  if (focusRunning((a) => a.type === 'gitPull' || a.type === 'gitCheckout')) return
  const s = startSession({ type: 'gitPull' }, {
    label: '拉取代码',
    icon: '⬇️',
    title: '拉取代码',
    sub: `git pull · ${binding.value?.path ?? ''}`
  })
  gitPulling.value = true
  try {
    const r = await api.projects.gitPull(s.id)
    finishSession(s, r.ok)
    if (r.ok) {
      ElMessage.success('代码已更新到最新')
      await loadGitInfo()
      void loadState()
    } else if (r.error !== '已手动中断') {
      ElMessage.error(r.error ?? '拉取失败')
    }
  } finally {
    gitPulling.value = false
  }
}

const branchVisible = ref(false)
const branchLoading = ref(false)
const branchError = ref('')
const branchList = ref<string[]>([])

function openBranchDialog(): void {
  branchError.value = ''
  branchVisible.value = true
}

/** 远程分支列表（ls-remote，当前分支置顶） */
async function loadGitBranches(): Promise<void> {
  branchLoading.value = true
  branchError.value = ''
  try {
    const r = await api.projects.gitBranches()
    if (r.ok && r.data) {
      const cur = gitInfo.value.branch
      branchList.value = [...r.data.branches.filter((b) => b === cur), ...r.data.branches.filter((b) => b !== cur)]
    } else {
      branchError.value = r.error ?? '获取远程分支失败'
    }
  } finally {
    branchLoading.value = false
  }
}

/** 选中分支：确认后 fetch + checkout（日志走脚本日志会话），成功刷新分支与项目信息 */
async function onPickBranch(b: string): Promise<void> {
  if (b === gitInfo.value.branch) {
    branchVisible.value = false
    return
  }
  try {
    await ElMessageBox.confirm(`切换到分支 ${b}？本地未提交的改动可能导致切换失败`, '切换分支', {
      confirmButtonText: '切换',
      cancelButtonText: '取消'
    })
  } catch {
    return /* 用户取消 */
  }
  if (focusRunning((a) => a.type === 'gitPull' || a.type === 'gitCheckout')) return
  const s = startSession({ type: 'gitCheckout', branch: b }, {
    label: `切换分支 → ${b}`,
    icon: '🌿',
    title: '切换分支',
    sub: `git fetch + checkout ${b}`
  })
  branchVisible.value = false
  try {
    const r = await api.projects.gitCheckout(b, s.id)
    finishSession(s, r.ok)
    if (r.ok) {
      ElMessage.success(`已切换到分支 ${b}`)
      await loadGitInfo()
      void loadState()
    } else if (r.error !== '已手动中断') {
      ElMessage.error(r.error ?? '切换分支失败')
    }
  } catch (err) {
    finishSession(s, false)
    ElMessage.error((err as Error).message)
  }
}

/** 解绑确认弹窗状态 */
const unbindVisible = ref(false)

function onUnbind(): void {
  unbindVisible.value = true
}

async function confirmUnbind(): Promise<void> {
  unbindVisible.value = false
  const r = await api.projects.unbind()
  if (!r.ok) {
    ElMessage.error(r.error ?? '解绑失败')
    return
  }
  ElMessage.success('已解绑')
  await loadState()
}

onMounted(loadState)
</script>

<style scoped>
.pc-page {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ===== Hero ===== */
.pc-hero {
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

.pc-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.pc-deco-1 {
  width: 300px;
  height: 300px;
  right: -70px;
  top: -160px;
}

.pc-deco-2 {
  width: 190px;
  height: 190px;
  right: 150px;
  bottom: -120px;
  border-color: rgba(255, 255, 255, 0.07);
}

.pc-hero-left {
  position: relative;
  min-width: 0;
}

.pc-hero-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.pc-hero-sub {
  margin-top: 7px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.pc-hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.pc-chip {
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(6px);
}

.pc-chip.ok {
  background: rgba(52, 211, 153, 0.28);
}

.pc-chip.idle {
  background: rgba(251, 191, 36, 0.28);
}

.pc-chip.ver {
  background: rgba(167, 139, 250, 0.3);
}

.pc-hero-tools {
  -webkit-app-region: no-drag;
  position: relative;
  flex-shrink: 0;
}

.pc-link {
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: background 0.2s;
}

.pc-link:hover {
  background: rgba(255, 255, 255, 0.24);
}

/* ===== 项目面板 ===== */
.pc-panel {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 16px;
  padding: 20px 26px 24px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
}

/* 项目简介 */
.pc-intro {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 12px;
}

.pc-intro-bar {
  width: 4px;
  border-radius: 4px;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
  flex-shrink: 0;
}

.pc-intro-title {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.pc-intro-text {
  font-size: 12.5px;
  line-height: 1.8;
  color: #64748b;
}

/* 已绑定 */
.pc-bound {
  margin-top: 18px;
}

.pc-bound-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* git 管理操作区（检测到仓库才渲染）：当前分支徽标 + 切换分支 / 拉取代码 */
.pc-git-ops {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.pc-git-branch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border-radius: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
  font-size: 12px;
  max-width: 200px;
}

.pc-git-branch svg {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

.pc-git-branch .mono-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-git-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.pc-git-btn:hover:not(:disabled) {
  border-color: #94a3b8;
  color: #1e293b;
}

.pc-git-btn.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.pc-git-btn.primary:hover:not(:disabled) {
  background: #1d4ed8;
  color: #fff;
}

.pc-git-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* 切换分支弹窗 */
.pbr-list {
  max-height: 320px;
  overflow-y: auto;
  padding: 6px;
}

.pbr-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #334155;
}

.pbr-item svg {
  width: 14px;
  height: 14px;
  color: #94a3b8;
  flex-shrink: 0;
}

.pbr-item:hover {
  background: #f1f5f9;
}

.pbr-item.current {
  background: #f0fdf4;
  color: #15803d;
}

.pbr-item.current svg {
  color: #15803d;
}

.pbr-cur {
  margin-left: auto;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 999px;
  background: #dcfce7;
  color: #15803d;
}

.pbr-arrow {
  margin-left: auto;
}

.pbr-loading,
.pbr-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 0;
  color: #94a3b8;
  font-size: 13px;
}

.pc-proj-ico {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #16283c, #2563eb);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 27px;
  flex-shrink: 0;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3);
}

.pc-bound-title {
  min-width: 0;
  flex: 1;
}

.pc-proj-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 19px;
  font-weight: 700;
  color: #0f1e30;
}

.pc-ver-pill {
  padding: 2px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
}

/* 远程服务器标签 */
.pc-path-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.pc-remote-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 9px;
  font-size: 11.5px;
  font-weight: 600;
  color: #334155;
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
  border: 1px solid #cbd5e1;
  cursor: default;
}

.pc-remote-ico {
  width: 13px;
  height: 13px;
  color: #64748b;
}

.pc-remote-badge em {
  font-style: normal;
  font-size: 10.5px;
  color: #8a94a6;
  margin-left: 2px;
}

.pc-path {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 6px 12px;
  border-radius: 9px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
  max-width: 100%;
  transition: border-color 0.15s, background 0.15s;
}

.pc-path:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.pc-path .mono-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-path-copy {
  color: #2563eb;
  font-size: 11px;
  flex-shrink: 0;
}

/* 绑定信息统计卡 */
.pc-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.pc-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  min-width: 0;
}

.pc-stat-ico {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  flex-shrink: 0;
}

.pc-stat-value {
  font-size: 14px;
  font-weight: 700;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-stat-label {
  margin-top: 2px;
  font-size: 11px;
  color: #8a94a6;
}

/* ===== 应用服务（单体/微服务）面板 ===== */
/* 头部徽标：靛蓝渐变，与基础服务蓝色徽标区分 */
.pas-badge {
  background: linear-gradient(135deg, #818cf8, #4338ca) !important;
  box-shadow:
    0 6px 16px rgba(67, 56, 202, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.32) !important;
}

/* Tab 切换栏：大号分段控件，带运行计数 */
.pas-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 4px;
  margin-bottom: 14px;
  background: #eef1f8;
  border: 1px solid #e4e9f4;
  border-radius: 13px;
}

.pas-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.pas-tab em {
  font-style: normal;
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.12);
  color: #64748b;
  transition: all 0.18s ease;
}

.pas-tab:hover {
  color: #1f2d3d;
}

.pas-tab.active {
  background: #fff;
  color: #1e1b4b;
  box-shadow: 0 2px 8px rgba(30, 27, 75, 0.12);
}

.pas-tab.active em {
  background: linear-gradient(135deg, #818cf8, #4f46e5);
  color: #fff;
}

.pas-tabs-hint {
  margin-left: auto;
  font-size: 10.5px;
  color: #98a3b8;
  white-space: nowrap;
}

/* 部署栏：仅微服务 Tab —— 提示语居左，按钮组靠右（与上方仪表卡保持间距） */
.pas-deploy {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
  margin-bottom: 12px;
}

.pas-deploy .pc-btn:first-of-type {
  margin-left: auto;
}

/* 全链路日志行：服务列表下方靠右 */
.pas-trace-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #e4eaf3;
}

.pas-trace-hint {
  font-size: 11px;
  color: #98a3b8;
}

.pas-deploy-hint {
  font-size: 10.5px;
  color: #98a3b8;
  white-space: nowrap;
}

/* 环境准备卡片：图标 + 标题/描述 + 右侧操作（位于基础服务之前） */
.pc-prep {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 18px;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
}

.pc-prep-icon {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #818cf8, #4f46e5);
  box-shadow:
    0 6px 16px rgba(79, 70, 229, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.pc-prep-icon svg {
  width: 19px;
  height: 19px;
}

.pc-prep-text {
  flex: 1;
  min-width: 200px;
}

.pc-prep-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #1f2d3d;
}

.pc-prep-ok {
  font-size: 10.5px;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  padding: 1px 8px;
  border-radius: 999px;
}

.pc-prep-desc {
  margin-top: 3px;
  font-size: 11.5px;
  color: #8a94a6;
}

.pc-prep-ops {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.pc-ops {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #eef2f7;
}

.pc-btn {
  padding: 10px 20px;
  border-radius: 11px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  border: 1px solid transparent;
}

.pc-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.pc-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

.pc-btn.success {
  background: linear-gradient(135deg, #34d399, #059669);
  color: #fff;
  box-shadow: 0 4px 14px rgba(5, 150, 105, 0.3);
}

.pc-btn.success:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(5, 150, 105, 0.4);
}

/* 按需编译启动：紫色渐变（区别于绿色后台 / 蓝色全量） */
/* 全链路日志：青色渐变（与弹窗头部同色系） */
.pc-btn.cyan {
  background: linear-gradient(135deg, #22d3ee, #0891b2);
  color: #fff;
  box-shadow: 0 4px 14px rgba(8, 145, 178, 0.32);
}

.pc-btn.cyan:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 6px 18px rgba(8, 145, 178, 0.42);
}

.pc-btn.violet {
  background: linear-gradient(135deg, #a78bfa, #7c3aed);
  color: #fff;
  box-shadow: 0 4px 14px rgba(124, 58, 237, 0.32);
}

.pc-btn.violet:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 6px 18px rgba(124, 58, 237, 0.42);
}

.pc-btn.danger {
  background: linear-gradient(135deg, #f87171, #dc2626);
  color: #fff;
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.3);
}

.pc-btn.danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(220, 38, 38, 0.4);
}

.pc-btn.done {
  background: #f1f5f9;
  color: #64748b;
  cursor: default;
}

.pc-btn:disabled {
  opacity: 0.8;
}

.pc-btn.plain {
  background: #fff;
  border-color: #bfdbfe;
  color: #2563eb;
}

.pc-btn.plain:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.pc-btn.danger-ghost {
  background: #fff;
  border-color: #fecaca;
  color: #dc2626;
}

.pc-btn.danger-ghost:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}

/* 基础服务 */
/* ===== 基础服务：纯白监控面板（白色统计卡 + 品牌服务卡片网格） ===== */
.pc-compose {
  position: relative;
  margin-top: 20px;
  padding: 22px;
  border-radius: 20px;
  border: 1px solid #e6ecf4;
  background: linear-gradient(180deg, #fcfdff 0%, #f6f8fb 100%);
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.04);
}

.pc-compose-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px 16px;
  padding: 2px 2px 18px;
}

.pc-compose-title {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 0;
}

.pc-compose-badge {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow:
    0 6px 16px rgba(37, 99, 235, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.32);
}

.pc-compose-badge svg {
  width: 21px;
  height: 21px;
}

.pc-compose-heading {
  min-width: 0;
}

.pc-compose-name {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 16px;
  font-weight: 800;
  color: #0f1e30;
  letter-spacing: -0.2px;
  white-space: nowrap;
}

.pc-compose-sub {
  padding: 2px 8px;
  border-radius: 7px;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #64748b;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}

.pc-compose-desc {
  margin-top: 4px;
  font-size: 11.5px;
  color: #8a94a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 运行计数胶囊（带呼吸圆点） */
.pc-compose-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  white-space: nowrap;
}

.pc-compose-live i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: pc-live-pulse 1.8s ease infinite;
}

.pc-compose-live.idle {
  color: #64748b;
  background: #f1f5f9;
  border-color: #e2e8f0;
}

.pc-compose-live.idle i {
  background: #94a3b8;
  animation: none;
}

.pc-compose-head-ops {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
}

/* 全量启停：翠绿渐变主按钮与浅红描边幽灵变体 */
.svc-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 11px;
  background: linear-gradient(135deg, #34d399, #059669);
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition: filter 0.15s ease, transform 0.12s ease, box-shadow 0.15s ease;
}

.svc-all-btn svg {
  width: 12px;
  height: 12px;
  fill: currentColor;
}

.svc-all-btn:hover {
  filter: brightness(1.05);
  box-shadow: 0 6px 18px rgba(16, 185, 129, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.svc-all-btn:active {
  filter: brightness(0.96);
  transform: scale(0.98);
}

.svc-all-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  filter: none;
  box-shadow: none;
}

.svc-all-btn.stop {
  background: #fef2f2;
  color: #dc2626;
  box-shadow: inset 0 0 0 1px rgba(220, 38, 38, 0.25);
}

.svc-all-btn.stop:hover {
  background: #fee2e2;
  filter: none;
}

/* 刷新：方形图标按钮（加载时旋转） */
.pc-compose-refresh {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbe4ee;
  border-radius: 11px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.12s ease, border-color 0.12s ease;
}

.pc-compose-refresh:hover {
  background: #f1f5f9;
  border-color: #c4d2e2;
}

.pc-compose-refresh:active {
  background: #e8eef6;
}

.refresh-ico {
  display: flex;
}

.refresh-ico svg {
  width: 15px;
  height: 15px;
}

.refresh-ico.spinning {
  animation: refresh-spin 1s linear infinite;
}

@keyframes refresh-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes pc-live-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.3);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(16, 185, 129, 0);
  }
}

/* ===== 通用等宽字体 ===== */
.mono-text {
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-variant-numeric: tabular-nums;
}

/* ===== 汇总仪表卡：环形进度（conic-gradient）+ 关键数值 ===== */
.pc-sum {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.pc-sum-card {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
  animation: pc-sum-in 0.45s ease backwards;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.pc-sum-card:nth-child(2) {
  animation-delay: 0.07s;
}

.pc-sum-card:nth-child(3) {
  animation-delay: 0.14s;
}

@keyframes pc-sum-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.pc-sum-card:hover {
  transform: translateY(-2px);
  border-color: #d3e0f5;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
}

/* 第三张卡：上排（环 + 文案），下排分段条 */
.pc-sum-card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 环形仪表：--p 驱动弧长（@property 注册于全局样式，transition 生效） */
.pc-ring {
  --p: 0;
  --c1: #60a5fa;
  --c2: #2563eb;
  position: relative;
  width: 74px;
  height: 74px;
  flex-shrink: 0;
  border-radius: 50%;
  background: conic-gradient(
    from -90deg,
    var(--c1) 0%,
    var(--c2) calc(var(--p) * 1%),
    #edf1f7 calc(var(--p) * 1%) 100%
  );
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px -6px var(--c2);
  transition: --p 0.9s cubic-bezier(0.25, 0.8, 0.35, 1);
}

.pc-ring-core {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 1px 2px rgba(16, 24, 40, 0.05);
}

.pc-ring-val {
  font-size: 12.5px;
  font-weight: 800;
  color: #0f1e30;
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.pc-ring-val em {
  font-style: normal;
  font-size: 9.5px;
  font-weight: 600;
  color: #8a94a6;
}

.pc-ring-cap {
  margin-top: 1px;
  font-size: 8.5px;
  letter-spacing: 1.5px;
  color: #8a94a6;
}

.pc-ring-val.waiting {
  animation: pc-total-wait 1.1s ease infinite;
}

@keyframes pc-total-wait {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.8;
  }
}

/* 卡片右侧信息列 */
.pc-sum-side {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pc-sum-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #1f2d3d;
}

.pc-sum-ico {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pc-sum-ico svg {
  width: 12px;
  height: 12px;
}

.pc-sum-ico.cpu {
  background: #eff6ff;
  color: #2563eb;
}

.pc-sum-ico.mem {
  background: #f5f3ff;
  color: #7c3aed;
}

.pc-sum-ico.run {
  background: #ecfdf5;
  color: #059669;
}

.pc-sum-note {
  font-size: 10.5px;
  color: #8a94a6;
  line-height: 1.5;
  word-break: break-all;
}

.pc-sum-note em {
  font-style: normal;
  font-weight: 600;
  color: #475569;
}

/* 实时标记 */
.pc-sum-live {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 500;
  color: #059669;
}

.pc-sum-live.pending {
  color: #d97706;
}

.pc-sum-live i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  animation: pc-live-pulse 1.8s ease infinite;
}

/* 服务状态分段条：每格一个服务，绿色 = 运行 */
.pc-sum-segs {
  height: 6px;
  display: flex;
  align-items: center;
  gap: 3px;
  margin-top: 14px;
}

.pc-sum-seg {
  flex: 1;
  height: 100%;
  min-width: 3px;
  border-radius: 99px;
  background: #e8eef6;
  transition: background 0.3s ease, box-shadow 0.3s ease;
}

.pc-sum-seg.on {
  background: linear-gradient(90deg, #34d399, #059669);
  box-shadow: 0 0 6px rgba(5, 150, 105, 0.35);
}

/* ===== 服务列表工具栏：标题 + 筛选分段控件 ===== */
.pc-svc-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 18px 2px 12px;
}

.pc-svc-bar-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #1f2d3d;
}

.pc-svc-bar-title::before {
  content: '';
  width: 3.5px;
  height: 13px;
  border-radius: 4px;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
}

.pc-svc-bar-count {
  font-size: 10.5px;
  font-weight: 600;
  color: #8a94a6;
}

/* 服务搜索框：放大镜 + 清除按钮 */
.pc-svc-search {
  position: relative;
  width: 190px;
  flex-shrink: 0;
}

.pc-svc-search-ico {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 13px;
  height: 13px;
  color: #94a3b8;
  pointer-events: none;
}

.pc-svc-search input {
  box-sizing: border-box;
  width: 100%;
  height: 32px;
  padding: 0 28px 0 31px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
  color: #1f2d3d;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.pc-svc-search input::placeholder {
  color: #aab4c4;
}

.pc-svc-search input:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.pc-svc-search-clear {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.14s ease;
}

.pc-svc-search-clear svg {
  width: 10px;
  height: 10px;
}

.pc-svc-search-clear:hover {
  background: #eef2f7;
  color: #475569;
}

.pc-filters {
  margin-left: auto;
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  background: #eef2f7;
  border-radius: 11px;
}

.pc-filter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.pc-filter em {
  font-style: normal;
  font-size: 10px;
  font-weight: 700;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.12);
  color: #64748b;
  transition: all 0.18s ease;
}

.pc-filter:hover {
  color: #1f2d3d;
}

.pc-filter.active {
  background: #fff;
  color: #0f1e30;
  box-shadow: 0 1px 4px rgba(16, 24, 40, 0.1);
}

.pc-filter.run em {
  background: rgba(5, 150, 105, 0.12);
  color: #059669;
}

.pc-filter.off em {
  background: rgba(100, 116, 139, 0.14);
  color: #8a94a6;
}

.pc-filter.run.active {
  color: #059669;
}

.pc-filter.off.active {
  color: #475569;
}

/* 筛选无结果占位 */
.pc-svc-none {
  grid-column: 1 / -1;
  padding: 26px 16px;
  text-align: center;
  font-size: 12.5px;
  color: #8a94a6;
  background: #f8fafc;
  border: 1px dashed #dbe4ee;
  border-radius: 14px;
}

/* ===== 服务卡片：品牌渐变顶栏（点阵纹理）+ 指标双芯片 + 底部操作 ===== */
.pc-svc-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(252px, 1fr));
  gap: 14px;
}

.pc-svc {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.04);
  animation: pc-svc-in 0.4s ease backwards;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

@keyframes pc-svc-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 停止态：虚线边框 + 内容微降存在感 */
.pc-svc:not(.on) {
  border-style: dashed;
  background: #fcfdfe;
}

.pc-svc:hover {
  transform: translateY(-3px);
  border-color: #c7dbfb;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.1);
}

/* 品牌顶栏：柔和品牌渐变（svcBandStyle 内联）+ 点阵纹理层 */
.pc-svc-top {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 15px 16px 13px;
  border-bottom: 1px solid #f1f5f9;
}

.pc-svc-top::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(15, 30, 48, 0.07) 1px, transparent 1.2px);
  background-size: 11px 11px;
  -webkit-mask-image: linear-gradient(115deg, rgba(0, 0, 0, 0.9), transparent 68%);
  mask-image: linear-gradient(115deg, rgba(0, 0, 0, 0.9), transparent 68%);
  pointer-events: none;
}

.pc-svc-ava {
  position: relative;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  font-weight: 700;
  color: #fff;
  /* 阴影由 svcAvaStyle 内联提供：运行中带品牌色光晕 */
  transition: filter 0.25s ease, opacity 0.25s ease, box-shadow 0.3s ease;
}

/* 停止态：图标去色、名称置灰 */
.pc-svc:not(.on) .pc-svc-ava {
  filter: grayscale(0.9);
  opacity: 0.45;
}

.pc-svc-title {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.pc-svc-name {
  max-width: 100%;
  font-size: 13.5px;
  font-weight: 650;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-svc:not(.on) .pc-svc-name {
  color: #8a94a6;
}

/* 状态胶囊：浅灰 = 停止，浅绿 = 运行 */
.pc-svc-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  color: #94a3b8;
  background: #f1f5f9;
}

.pc-svc-state i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.pc-svc-state.on {
  color: #059669;
  background: #ecfdf5;
}

.pc-svc-state.on i {
  background: #10b981;
  animation: pc-live-pulse 1.8s ease infinite;
}

/* 底部操作：日志 + 启动/停止 等宽双按钮 */
.pc-svc-foot {
  display: flex;
  gap: 8px;
  padding: 12px 14px 14px;
  margin-top: auto;
}

.pc-svc-btn {
  flex: 1;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 9px;
  border: 1px solid transparent;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.16s ease;
}

.pc-svc-btn svg {
  width: 11px;
  height: 11px;
  fill: currentColor;
}

.pc-svc-btn.log {
  background: #fff;
  border-color: #dbe4f0;
  color: #475569;
}

.pc-svc-btn.log:hover {
  border-color: #93c5fd;
  color: #2563eb;
  background: #f0f7ff;
}

.pc-svc-btn.start {
  background: linear-gradient(135deg, #34d399, #059669);
  color: #fff;
  box-shadow: 0 3px 10px rgba(5, 150, 105, 0.28);
}

.pc-svc-btn.start:hover {
  filter: brightness(1.06);
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(5, 150, 105, 0.36);
}

/* 重启按钮：浅琥珀底，悬停琥珀渐变填充 */
.pc-svc-btn.restart {
  background: #fffbeb;
  border-color: #fde68a;
  color: #b45309;
}

.pc-svc-btn.restart:hover {
  background: linear-gradient(135deg, #fbbf24, #d97706);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.32);
}

.pc-svc-btn.stop {
  background: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

.pc-svc-btn.stop:hover {
  background: #fee2e2;
  border-color: #fca5a5;
}

/* 指标：双芯片（CPU / 内存）并排 */
.pc-svc-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 13px 14px 0;
}

.pc-svc-metric {
  min-width: 0;
  padding: 9px 11px 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 11px;
  transition: border-color 0.18s ease;
}

.pc-svc:hover .pc-svc-metric {
  border-color: #e3eaf3;
}

/* 停止态：指标整体降透明度 */
.pc-svc:not(.on) .pc-svc-metrics {
  opacity: 0.6;
}

.pc-svc-mrow {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6px;
  min-width: 0;
}

.pc-svc-mlabel {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #8a94a6;
}

.pc-svc-mlabel svg {
  width: 10px;
  height: 10px;
  opacity: 0.75;
}

.pc-svc-mrow b {
  font-size: 12.5px;
  font-weight: 600;
  color: #1f2d3d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pc-svc-mrow b.warn {
  color: #d97706;
}

.pc-svc-mrow b.high {
  color: #dc2626;
}

.pc-svc-mrow b.na {
  color: #aab4c4;
  font-weight: 400;
}

/* 指标轨道与填充（按负载等级变色） */
.pc-svc-track {
  margin-top: 6px;
  height: 5px;
  border-radius: 99px;
  background: #f1f5f9;
  overflow: hidden;
}

.pc-svc-fill {
  display: block;
  height: 100%;
  min-width: 4px;
  border-radius: 99px;
  transition: width 0.6s cubic-bezier(0.3, 0.8, 0.4, 1);
}

.pc-svc-fill.cpu {
  background: linear-gradient(90deg, #60a5fa, #2563eb);
  box-shadow: 0 0 6px rgba(37, 99, 235, 0.35);
}

.pc-svc-fill.mem {
  background: linear-gradient(90deg, #c4b5fd, #8b5cf6);
  box-shadow: 0 0 6px rgba(139, 92, 246, 0.35);
}

.pc-svc-fill.warn {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
}

.pc-svc-fill.high {
  background: linear-gradient(90deg, #f87171, #ef4444);
}

/* 窄窗：汇总统计卡退化为单列 */
@media (max-width: 980px) {
  .pc-sum {
    grid-template-columns: 1fr;
  }
}

.pc-compose-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 46px 16px;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #d7e0ec;
  border-radius: 16px;
}

.pc-compose-empty-ico {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: #eef2f7;
  color: #94a3b8;
}

.pc-compose-empty-ico svg {
  width: 24px;
  height: 24px;
}

.pc-compose-empty-title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.pc-compose-empty-sub {
  font-size: 11.5px;
  color: #8a94a6;
}
/* 未绑定 */
.pc-unbound {
  margin-top: 18px;
  text-align: center;
}

.pc-unbound-title {
  font-size: 17px;
  font-weight: 700;
  color: #1f2d3d;
}

.pc-unbound-sub {
  margin-top: 6px;
  font-size: 12.5px;
  color: #8a94a6;
}

.pc-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  max-width: 680px;
  margin: 22px auto 6px;
}

.pc-option {
  position: relative;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #e6edf5;
  border-radius: 16px;
  padding: 26px 22px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pc-option::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  opacity: 0.5;
  transition: opacity 0.2s ease;
}

.pc-option:nth-child(1)::before {
  background: linear-gradient(90deg, #60a5fa, #2563eb);
}

.pc-option:nth-child(2)::before {
  background: linear-gradient(90deg, #34d399, #059669);
}

.pc-option:hover {
  transform: translateY(-4px);
  border-color: #93c5fd;
  background: #f0f7ff;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.14);
}

.pc-option:hover::before {
  opacity: 1;
}

.pc-option-no {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 26px;
  font-weight: 800;
  color: #e2eaf4;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
}

.pc-option-ico {
  width: 54px;
  height: 54px;
  margin: 0 auto 12px;
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: #fff;
  box-shadow: 0 8px 18px rgba(16, 24, 40, 0.14);
}

.pc-option-ico.dir {
  background: linear-gradient(135deg, #60a5fa, #2563eb);
}

/* ===== SSH 远程连接：深色沉浸式面板 ===== */
.pc-ssh-hero {
  grid-column: 1 / -1;
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(145deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.35);
  padding: 24px;
}

/* 背景网格 */
.pc-ssh-hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.04) 1px, transparent 1px);
  background-size: 24px 24px;
  pointer-events: none;
}

/* 背景光晕 */
.pc-ssh-hero-glow {
  position: absolute;
  top: -80px;
  right: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.12), transparent 70%);
  pointer-events: none;
}

/* 顶部 */
.pc-ssh-hero-top {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.pc-ssh-hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pc-ssh-hero-icon {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.1);
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.pc-ssh-hero-icon svg {
  width: 24px;
  height: 24px;
}

.pc-ssh-hero-title {
  font-size: 18px;
  font-weight: 800;
  color: #f1f5f9;
  letter-spacing: -0.3px;
}

.pc-ssh-hero-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

/* 状态指示 */
.pc-ssh-hero-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 14px;
  border-radius: 999px;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: #34d399;
  background: rgba(52, 211, 153, 0.08);
  border: 1px solid rgba(52, 211, 153, 0.2);
}

.pc-ssh-hero-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  animation: pc-ssh-dot-pulse 2s ease infinite;
}

.pc-ssh-hero-status.connecting {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.08);
  border-color: rgba(251, 191, 36, 0.2);
}

.pc-ssh-hero-status.connecting .pc-ssh-hero-status-dot {
  animation-duration: 0.6s;
}

.pc-ssh-hero-status.error {
  color: #f87171;
  background: rgba(248, 113, 113, 0.08);
  border-color: rgba(248, 113, 113, 0.2);
}

@keyframes pc-ssh-dot-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.3); }
  50% { box-shadow: 0 0 0 5px rgba(52, 211, 153, 0); }
}

.pc-ssh-hero-status.connecting .pc-ssh-hero-status-dot {
  animation-name: pc-ssh-dot-pulse-amber;
}

@keyframes pc-ssh-dot-pulse-amber {
  0%, 100% { box-shadow: 0 0 0 0 rgba(251, 191, 36, 0.4); }
  50% { box-shadow: 0 0 0 5px rgba(251, 191, 36, 0); }
}

/* 服务器列表 */
.pc-ssh-hero-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 服务器条目：横条 + LED + 信息 */
.pc-ssh-srv {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border-radius: 12px;
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(4px);
}

.pc-ssh-srv:hover {
  background: rgba(51, 65, 85, 0.5);
  border-color: rgba(148, 163, 184, 0.25);
  transform: translateX(4px);
}

.pc-ssh-srv.selected {
  background: rgba(56, 189, 248, 0.08);
  border-color: rgba(56, 189, 248, 0.35);
  box-shadow: 0 0 20px rgba(56, 189, 248, 0.08);
}

.pc-ssh-srv.testing {
  animation: pc-ssh-srv-pulse 1.2s ease infinite;
}

@keyframes pc-ssh-srv-pulse {
  0%, 100% { box-shadow: 0 0 20px rgba(56, 189, 248, 0.08); }
  50% { box-shadow: 0 0 30px rgba(56, 189, 248, 0.15); }
}

/* LED 指示灯 */
.pc-ssh-srv-led {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pc-ssh-srv-led span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #334155;
  border: 1.5px solid #475569;
  transition: all 0.25s;
}

.pc-ssh-srv.selected .pc-ssh-srv-led span {
  background: #38bdf8;
  border-color: #38bdf8;
  box-shadow: 0 0 10px rgba(56, 189, 248, 0.6);
}

.pc-ssh-srv-body {
  flex: 1;
  min-width: 0;
}

.pc-ssh-srv-name {
  font-size: 14px;
  font-weight: 700;
  color: #e2e8f0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-ssh-srv-addr {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-ssh-srv-right {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pc-ssh-srv-check {
  width: 18px;
  height: 18px;
  color: #38bdf8;
}

.pc-ssh-srv-arrow {
  width: 16px;
  height: 16px;
  color: #475569;
  transition: color 0.2s, transform 0.2s;
}

.pc-ssh-srv:hover .pc-ssh-srv-arrow {
  color: #94a3b8;
  transform: translateX(2px);
}

.pc-ssh-srv.selected .pc-ssh-srv-arrow {
  display: none;
}

.pc-ssh-srv-spin {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(56, 189, 248, 0.2);
  border-top-color: #38bdf8;
  border-radius: 50%;
  animation: pc-ssh-rotate 0.7s linear infinite;
}

@keyframes pc-ssh-rotate {
  to { transform: rotate(360deg); }
}

/* 空态 */
.pc-ssh-hero-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 36px 16px;
  text-align: center;
}

.pc-ssh-hero-empty-ico {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(148, 163, 184, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.pc-ssh-hero-empty-ico svg {
  width: 22px;
  height: 22px;
  color: #475569;
}

.pc-ssh-hero-empty-title {
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
}

.pc-ssh-hero-empty-sub {
  font-size: 11.5px;
  color: #475569;
}

/* 连接按钮区 */
.pc-ssh-hero-actions {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

.pc-ssh-connect {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 42px;
  padding: 0 24px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.3);
  cursor: pointer;
  transition: all 0.2s ease;
}

.pc-ssh-connect:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 6px 22px rgba(14, 165, 233, 0.4);
}

.pc-ssh-connect:active:not(:disabled) {
  transform: translateY(0);
}

.pc-ssh-connect:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}

.pc-ssh-connect-ico {
  width: 16px;
  height: 16px;
}

.pc-ssh-connect-spin {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: pc-ssh-rotate 0.7s linear infinite;
}

.pc-ssh-hero-tip {
  font-size: 11px;
  color: #475569;
}

/* 错误 */
.pc-ssh-hero-err {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 14px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #fca5a5;
  font-size: 12.5px;
  line-height: 1.6;
}

.pc-ssh-hero-err svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.pc-bind-back {
  grid-column: 1 / -1;
  font-size: 12.5px;
  font-weight: 600;
  color: #2563eb;
  cursor: pointer;
  padding: 4px 0;
  transition: color 0.15s;
}

.pc-bind-back:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.pc-option-ico.ssh {
  background: linear-gradient(135deg, #64748b, #334155);
}

.pc-option-ico.pull {
  background: linear-gradient(135deg, #34d399, #059669);
}

.pc-option-title {
  font-size: 15.5px;
  font-weight: 700;
  color: #1f2d3d;
}

.pc-option-desc {
  margin-top: 7px;
  font-size: 12px;
  color: #8a94a6;
  line-height: 1.7;
}

.pc-option-go {
  margin-top: 14px;
  font-size: 12.5px;
  font-weight: 600;
  color: #2563eb;
  opacity: 0.75;
  transition: opacity 0.2s;
}

.pc-option:hover .pc-option-go {
  opacity: 1;
}

@media (max-width: 760px) {
  .pc-options,
  .pc-stats {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* ===== 拉取代码弹窗（append-to-body，全局样式；pc-dlg 为 modal-class） ===== */
.pc-dlg .el-dialog {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(15, 30, 48, 0.3);
  width: min(640px, 94vw) !important;
}

.pc-dlg .el-dialog__header,
.pc-dlg .el-dialog__body,
.pc-dlg .el-dialog__footer {
  padding: 0;
}

.pcd-header {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
}

.pcd-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.pcd-deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -120px;
}

.pcd-deco-2 {
  width: 140px;
  height: 140px;
  right: 110px;
  bottom: -90px;
  border-color: rgba(255, 255, 255, 0.07);
}

.pcd-header-main {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.pcd-header-icon {
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

.pcd-title {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.pcd-subtitle {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.pcd-close {
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

.pcd-close:hover {
  background: rgba(255, 255, 255, 0.26);
}

/* 表单区 */
.pcd-body {
  padding: 20px 24px 8px;
  background: #fff;
}

.pcd-field {
  margin-bottom: 13px;
}

.pcd-label {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  margin-bottom: 6px;
}

.pcd-req {
  color: #dc2626;
}

.pcd-sec {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 11px;
}

.pcd-sec-dest {
  margin-top: 10px;
}

.pcd-sec-ico {
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

.pcd-sec-text {
  font-size: 11.5px;
  color: #64748b;
  letter-spacing: 1px;
  font-weight: 600;
  flex-shrink: 0;
}

.pcd-sec-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #e4eaf2, transparent);
}

.pcd-hint {
  margin-top: 6px;
  font-size: 11.5px;
  color: #94a3b8;
}

.pcd-hint.warn {
  color: #d97706;
  background: #fffbeb;
  border: 1px dashed #fcd34d;
  border-radius: 7px;
  padding: 5px 9px;
}

.pcd-dest {
  display: flex;
  gap: 10px;
}

.pcd-pick {
  padding: 0 18px;
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s;
}

.pcd-pick:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.pc-dlg .el-select {
  width: 100%;
}

.pc-dlg .el-input__wrapper,
.pc-dlg .el-select__wrapper {
  border-radius: 10px;
}

/* git 日志控制台 */
.pcd-log-box {
  margin-top: 4px;
  border: 1px solid #1e293b;
  border-radius: 10px;
  overflow: hidden;
  background: #0b1220;
}

.pcd-log-head {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 12px;
  font-size: 11px;
  color: #7dd3fc;
  letter-spacing: 1px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid #1e293b;
}

.pcd-log-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #475569;
}

.pcd-log-dot.running {
  background: #22d3ee;
  animation: pcd-pulse 1.2s infinite;
}

@keyframes pcd-pulse {
  0% { box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.5); }
  70% { box-shadow: 0 0 0 6px rgba(34, 211, 238, 0); }
  100% { box-shadow: 0 0 0 0 rgba(34, 211, 238, 0); }
}

.pcd-log {
  height: 190px;
  overflow-y: auto;
  padding: 10px 12px;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 11.5px;
  line-height: 1.75;
  color: #a5f3fc;
}

.pcd-log-line {
  white-space: pre-wrap;
  word-break: break-all;
}

.pcd-log-tall {
  height: 340px;
}

.pcd-log-empty {
  color: #475569;
}

.pcd-log-status {
  margin-left: auto;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

/** 服务日志查询弹窗：控制条（浅灰面板 + 白底控件分组） */
.pc-log-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px;
  margin-bottom: 14px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 14px;
}

/* 数据源组：服务 + 行数 */
.pc-log-src {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.pc-log-svc {
  flex: 0 0 auto;
  width: 188px !important;
}

/* 行数分段控件：100 / 500 / 1k / 2k */
.pc-log-lines {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  background: #fff;
  border: 1px solid #dfe6ee;
  border-radius: 10px;
  flex-shrink: 0;
}

.pc-log-line-btn {
  height: 24px;
  min-width: 34px;
  padding: 0 9px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pc-log-line-btn:hover {
  color: #2563eb;
  background: #f0f7ff;
}

.pc-log-line-btn.active {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #fff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.32);
}

/* 分组分隔线 */
.pc-log-sep {
  width: 1px;
  height: 22px;
  background: #e2e8f0;
  flex-shrink: 0;
}

.pc-log-filter {
  flex: 1;
  min-width: 200px;
}

/* 灰面板上的控件统一白底 */
.pc-log-toolbar .el-input__wrapper,
.pc-log-toolbar .el-select__wrapper {
  background: #fff;
}

.pc-log-filter-ico {
  width: 13px;
  height: 13px;
}

.pc-log-toolbar-ops {
  margin-left: auto;
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* 工具按钮：白底描边，悬停蓝色 */
.pc-log-tool {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #dbe4f0;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pc-log-tool:hover:not(:disabled) {
  border-color: #93c5fd;
  color: #2563eb;
  background: #f0f7ff;
}

.pc-log-tool:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 跟踪进行中：蓝色实心 */
.pc-log-tool.follow.on {
  border-color: transparent;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #fff;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.35);
}

.pc-log-view {
  height: 460px;
  padding-top: 12px;
}

.pc-log-cmd {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 远程服务器绑定弹窗（pssh-）===== */
.pssh-dlg .el-dialog {
  width: min(620px, 94vw) !important;
}

.pssh-header {
  background: linear-gradient(135deg, #475569 0%, #334155 60%, #1e293b 100%) !important;
}

.pssh-conn-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.pssh-sel {
  flex: 1;
}

.pssh-hint {
  font-size: 12px;
  color: #8a94a6;
  padding: 8px 12px;
  border-radius: 10px;
  background: #f8fafc;
}

.pssh-hint.warn {
  color: #b45309;
  background: #fffbeb;
  border: 1px dashed #fde68a;
}

.pssh-browser {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.pssh-path-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
}

.pssh-up {
  width: 28px;
  height: 28px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
}

.pssh-path {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pssh-mkdir {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #475569;
  white-space: nowrap;
}

.pssh-mkdir svg {
  width: 13px;
  height: 13px;
}

.pssh-mkdir:hover:not(:disabled) {
  border-color: #94a3b8;
  color: #1e293b;
}

.pssh-mkdir:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pssh-list {
  max-height: 280px;
  overflow-y: auto;
  padding: 6px;
}

.pssh-dir {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 8px;
  font-size: 12.5px;
  color: #334155;
  cursor: pointer;
  transition: background 0.12s;
}

.pssh-dir:hover {
  background: #f0f7ff;
}

.pssh-loading,
.pssh-empty {
  padding: 20px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.pssh-foot-hint {
  margin-right: auto;
  font-size: 11px;
  color: #7c8aa0;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 解绑项目确认弹窗（pub-）===== */
.pub-dlg .el-dialog {
  width: min(480px, 94vw) !important;
}

/* 头部：暖灰渐变 */
.pub-header {
  background: linear-gradient(135deg, #64748b 0%, #475569 60%, #334155 100%) !important;
}

.pub-content {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 4px 2px;
}

.pub-icon {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}

.pub-icon svg {
  width: 24px;
  height: 24px;
}

.pub-text {
  flex: 1;
  min-width: 0;
}

.pub-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 10px;
}

.pub-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pub-item {
  font-size: 12px;
  line-height: 1.6;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.pub-item.ok {
  color: #059669;
}

.pub-item.warn {
  color: #b45309;
}

.pub-hint {
  margin-top: 12px;
  font-size: 11px;
  color: #94a3b8;
}

/* 确认按钮：暖灰渐变 */
.pub-confirm {
  background: linear-gradient(135deg, #64748b, #475569);
  color: #fff;
  box-shadow: 0 4px 14px rgba(71, 85, 105, 0.3);
}

.pub-confirm:hover {
  filter: brightness(1.08);
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(71, 85, 105, 0.4);
}

/* ===== 全链路日志查询弹窗（ptq-）===== */
.ptq-dlg .el-dialog {
  width: min(860px, 94vw) !important;
}

/* 头部：青色渐变（区别于其他弹窗） */
.ptq-header {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 60%, #0e7490 100%) !important;
}

/* 搜索栏 */
.ptq-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.ptq-input {
  flex: 1;
}

.ptq-input .el-input__wrapper {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #bfdbfe inset;
}

.ptq-input .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 3px rgba(37, 99, 235, 0.12);
}

/* 汇总 */
.ptq-summary {
  font-size: 12px;
  color: #64748b;
  padding: 8px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  margin-bottom: 10px;
}

.ptq-lines {
  height: auto;
  max-height: 240px;
  padding-top: 8px;
  /* 与日志弹窗同款深色控制台 */
  background: #0b1220;
  border: 1px solid #1e293b;
  border-radius: 10px;
}

/* 底部信息 */
.ptq-foot-info {
  margin-right: auto;
  font-size: 11.5px;
  color: #7c8aa0;
}

/* ===== 互斥切换确认弹窗（pmx-）===== */
.pmx-dlg .el-dialog {
  width: min(560px, 94vw) !important;
}

/* 头部：琥珀警示渐变 */
.pmx-header {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 70%, #b45309 100%) !important;
}

.pmx-desc {
  font-size: 13px;
  line-height: 1.8;
  color: #334155;
  padding: 2px 2px 0;
}

/* 左右卡 + 中间箭头 */
.pmx-flow {
  display: flex;
  align-items: stretch;
  gap: 10px;
  margin-top: 14px;
}

.pmx-card {
  flex: 1;
  min-width: 0;
  border-radius: 13px;
  padding: 12px 13px;
  border: 1px solid transparent;
}

.pmx-card.stop {
  background: #fef5f3;
  border-color: #f8d9d2;
}

.pmx-card.start {
  background: #f1faf6;
  border-color: #cdeee0;
}

.pmx-card-head {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}

.pmx-card-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #1f2d3d;
}

.pmx-card-tag {
  margin-left: auto;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.pmx-card.stop .pmx-card-tag {
  color: #b91c1c;
  background: #fee2e2;
}

.pmx-card.start .pmx-card-tag {
  color: #047857;
  background: #d1fae5;
}

.pmx-card-dot {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pmx-card-dot.stop {
  background: #fee2e2;
  color: #dc2626;
}

.pmx-card-dot.start {
  background: #d1fae5;
  color: #059669;
}

.pmx-card-dot i {
  display: none;
}

/* 将被关闭的服务清单 */
.pmx-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pmx-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 5px 9px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.75);
  font-size: 12px;
  color: #7c2d2d;
  border: 1px solid rgba(248, 217, 210, 0.7);
}

.pmx-item-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
}

.pmx-item-dot.run {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.16);
}

/* 即将启动的目标 */
.pmx-start-name {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 9px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.75);
  font-size: 12.5px;
  font-weight: 700;
  color: #065f46;
  border: 1px solid rgba(205, 238, 224, 0.9);
}

/* 中间箭头 */
.pmx-arrow {
  align-self: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d97706;
  background: #fffbeb;
  border: 1px solid #fde68a;
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.18);
}

.pmx-arrow svg {
  width: 16px;
  height: 16px;
}

.pmx-hint {
  margin-top: 12px;
  font-size: 10.5px;
  color: #94a3b8;
  text-align: center;
}

/* 确认按钮：琥珀渐变 */
.pmx-confirm {
  background: linear-gradient(135deg, #fbbf24, #d97706);
  color: #fff;
  box-shadow: 0 4px 14px rgba(217, 119, 6, 0.35);
}

.pmx-confirm:hover {
  filter: brightness(1.05);
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(217, 119, 6, 0.45);
}

/* 窄窗：左右卡纵向排列，箭头旋转 */
@media (max-width: 620px) {
  .pmx-flow {
    flex-direction: column;
  }

  .pmx-arrow {
    transform: rotate(90deg);
    align-self: center;
  }
}

/* ===== 面板收起/展开 ===== */
.pc-collapse-btn {
  flex-shrink: 0;
}

.pc-collapse-ico {
  width: 15px;
  height: 15px;
  transition: transform 0.25s ease;
}

/* 展开态箭头朝下；收起后旋转变为朝右 */
.pc-collapse-ico.folded {
  transform: rotate(-90deg);
}

/* 展开时内容轻微下滑淡入 */
.pc-collapse-body {
  animation: pc-collapse-in 0.25s ease;
}

@keyframes pc-collapse-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* 注册 --p（环形仪表弧长百分比），使 conic-gradient 里的 var(--p) 可参与 transition 动画 */
@property --p {
  syntax: '<number>';
  inherits: false;
  initial-value: 0;
}

/* ===== 服务日志弹窗（plg-）：品牌头部 + 等级着色控制台 ===== */
/* 日志弹窗加宽（覆盖 pc-dlg 的 640px 通用上限；控制条单行所需） */
.plg-dlg .el-dialog {
  width: min(960px, 94vw) !important;
}

/* 服务下拉：选中项前的品牌色圆点（el-select prefix） */
.pc-log-toolbar .el-select__prefix {
  display: inline-flex;
  align-items: center;
}

.plg-sel-dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-right: 3px;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.55);
}

/* 服务下拉选项：品牌圆点 + 名称 + 运行状态（popper 挂 body，走全局样式） */
.plg-svc-popper .el-select-dropdown__item {
  display: flex;
  align-items: center;
}

.plg-opt {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.plg-opt-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.6);
}

.plg-opt .mono-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plg-opt em {
  margin-left: auto;
  font-style: normal;
  font-size: 10px;
  font-weight: 600;
  color: #94a3b8;
  flex-shrink: 0;
  padding-left: 12px;
}

.plg-opt em.run {
  color: #059669;
}

/* 品牌化头部：背景色来自 logHeaderStyle 内联（服务卡片同款渐变） */
.plg-header {
  box-shadow: inset 0 -1px 0 rgba(255, 255, 255, 0.14);
}

/* LIVE 徽标（跟踪中呼吸闪烁） */
.plg-live {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  padding: 1.5px 9px;
  border-radius: 999px;
  background: rgba(34, 211, 238, 0.13);
  border: 1px solid rgba(34, 211, 238, 0.38);
  color: #67e8f9;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1.5px;
}

.plg-live i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22d3ee;
  animation: plg-pulse 1.4s ease infinite;
}

@keyframes plg-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(34, 211, 238, 0.55);
  }
  60% {
    box-shadow: 0 0 0 5px rgba(34, 211, 238, 0);
  }
}

/* 等级统计条：可点击的等级快捷过滤 */
.plg-levels {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 7px 12px;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid #1e293b;
}

.plg-chip {
  padding: 2.5px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.plg-chip.err {
  color: #fda4af;
  background: rgba(244, 63, 94, 0.1);
  border-color: rgba(244, 63, 94, 0.32);
}

.plg-chip.err:hover,
.plg-chip.err.active {
  background: #f43f5e;
  border-color: #f43f5e;
  color: #fff;
  box-shadow: 0 0 14px rgba(244, 63, 94, 0.45);
}

.plg-chip.warn {
  color: #fcd34d;
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.32);
}

.plg-chip.warn:hover,
.plg-chip.warn.active {
  background: #f59e0b;
  border-color: #f59e0b;
  color: #fff;
  box-shadow: 0 0 14px rgba(245, 158, 11, 0.45);
}

.plg-chip.info {
  color: #7dd3fc;
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.32);
}

.plg-chip.info:hover,
.plg-chip.info.active {
  background: #0ea5e9;
  border-color: #0ea5e9;
  color: #fff;
  box-shadow: 0 0 14px rgba(14, 165, 233, 0.45);
}

.plg-chip.debug {
  color: #a9b7cc;
  background: rgba(148, 163, 184, 0.14);
  border-color: rgba(148, 163, 184, 0.42);
}

.plg-chip.debug:hover,
.plg-chip.debug.active {
  background: #64748b;
  border-color: #64748b;
  color: #fff;
  box-shadow: 0 0 14px rgba(100, 116, 139, 0.45);
}

.plg-levels-hint {
  margin-left: auto;
  font-size: 10px;
  color: #475569;
  white-space: nowrap;
}

/* 日志行：行号 + 容器前缀 + 等级着色正文 */
.plg-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 2px 12px 2px 0;
  border-radius: 6px;
  transition: background 0.12s ease;
}

.plg-row:hover {
  background: rgba(148, 163, 184, 0.09);
}

.plg-row.has-error {
  background: rgba(244, 63, 94, 0.06);
}

.plg-row.has-error:hover {
  background: rgba(244, 63, 94, 0.11);
}

.plg-no {
  flex-shrink: 0;
  width: 44px;
  text-align: right;
  color: #33415a;
  font-size: 10.5px;
  user-select: none;
}

.plg-prefix {
  flex-shrink: 0;
  max-width: 170px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #55688a;
}

.plg-text {
  flex: 1;
  min-width: 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #b6c5d9;
}

/* 关键字命中高亮 */
.plg-hit {
  background: rgba(250, 204, 21, 0.22);
  color: #fde047;
  border-radius: 2px;
  padding: 0 2px;
  font-weight: 700;
}

.plg-text.lv-error {
  color: #fda4af;
}

.plg-text.lv-warn {
  color: #fcd34d;
}

.plg-text.lv-info {
  color: #7dd3fc;
}

.plg-text.lv-debug {
  color: #7f91ab;
}

/* 深色控制台滚动条 */
.pc-log-view::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.pc-log-view::-webkit-scrollbar-thumb {
  background: #2c3a58;
  border-radius: 8px;
  border: 2px solid #0b1220;
}

.pc-log-view::-webkit-scrollbar-thumb:hover {
  background: #41537a;
}

.pc-log-view::-webkit-scrollbar-track,
.pc-log-view::-webkit-scrollbar-corner {
  background: transparent;
}

/* 空态 / 加载态 / 错误态 */
.plg-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 100%;
  min-height: 180px;
  color: #546a8a;
  font-size: 12px;
}

.plg-empty-ico {
  width: 32px;
  height: 32px;
  opacity: 0.45;
}

.plg-error {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(244, 63, 94, 0.08);
  border: 1px solid rgba(244, 63, 94, 0.28);
  color: #fda4af;
  font-size: 12.5px;
  line-height: 1.7;
}

.plg-error svg {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
  margin-top: 1px;
}

/* 页脚：左侧说明 + 右侧关闭 */
.plg-footer {
  justify-content: space-between;
  align-items: center;
}

.plg-foot-hint {
  font-size: 11.5px;
  color: #7c8aa0;
  letter-spacing: 0.3px;
}

/** 环境设置弹窗（pev-）：进度总览 + 搜索筛选 + 分组卡片 */
/* 加载/错误/空状态 */
.pev-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 44px 0;
  color: #8a94a6;
  font-size: 13px;
}

.pev-state.error {
  color: #dc2626;
}

.pev-state-ico {
  font-size: 17px;
}

/* 旋转指示（日志弹窗空态也复用） */
.pc-env-spin,
.pc-env-act-spin {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  border-radius: 50%;
  border: 2px solid rgba(37, 99, 235, 0.18);
  border-top-color: #2563eb;
  animation: pc-env-rotate 0.7s linear infinite;
}

.pc-env-act-spin {
  width: 13px;
  height: 13px;
  border-color: rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
}

@keyframes pc-env-rotate {
  to {
    transform: rotate(360deg);
  }
}

/* 弹窗加宽（覆盖 pc-dlg 的 640px 通用上限） */
.pev-dlg .el-dialog {
  width: min(780px, 94vw) !important;
}

/* ===== 进度总览卡：环形就绪度仪表 + 状态徽标 ===== */
.pev-hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e8eef6;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.04);
}

.pev-hero.done {
  background: linear-gradient(135deg, #f2fdf8 0%, #ffffff 70%);
  border-color: #cdeee0;
}

/* 环形仪表（--p 已由全局 @property 注册，支持弧长过渡动画） */
.pev-ring {
  --p: 0;
  --c1: #60a5fa;
  --c2: #2563eb;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 50%;
  background: conic-gradient(
    from -90deg,
    var(--c1) 0%,
    var(--c2) calc(var(--p) * 1%),
    #e8eef6 calc(var(--p) * 1%) 100%
  );
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px -6px var(--c2);
  transition: --p 0.9s cubic-bezier(0.25, 0.8, 0.35, 1);
}

.pev-ring-core {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 1px 2px rgba(16, 24, 40, 0.05);
}

.pev-ring-val {
  font-size: 13px;
  font-weight: 800;
  color: #0f1e30;
  letter-spacing: -0.3px;
}

.pev-ring-val em {
  font-style: normal;
  font-size: 9px;
  font-weight: 600;
  color: #8a94a6;
}

.pev-hero-text {
  flex: 1;
  min-width: 0;
}

.pev-hero-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #1f2d3d;
}

.pev-done-badge {
  font-size: 10.5px;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  padding: 1px 9px;
  border-radius: 999px;
  white-space: nowrap;
}

.pev-hero-sub {
  margin-top: 3px;
  font-size: 11px;
  color: #8a94a6;
}

.pev-hero-chips {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 9px;
}

.pev-stat {
  font-size: 10.5px;
  font-weight: 600;
  padding: 2.5px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  white-space: nowrap;
}

.pev-stat.ok {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.pev-stat.warn {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.pev-stat.off {
  color: #94a3b8;
  background: #f8fafc;
  border-color: #eef2f7;
}

/* ===== 控制条：搜索 + 状态筛选 ===== */
.pev-ctrl {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 14px 0 4px;
}

.pev-search {
  position: relative;
  flex: 1;
  min-width: 200px;
}

.pev-search-ico {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 13px;
  height: 13px;
  color: #94a3b8;
  pointer-events: none;
}

.pev-search input {
  box-sizing: border-box;
  width: 100%;
  height: 32px;
  padding: 0 28px 0 31px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
  color: #1f2d3d;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.pev-search input::placeholder {
  color: #aab4c4;
}

.pev-search input:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.pev-search-clear {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.14s ease;
}

.pev-search-clear svg {
  width: 10px;
  height: 10px;
}

.pev-search-clear:hover {
  background: #eef2f7;
  color: #475569;
}

.pev-filters {
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  background: #eef2f7;
  border-radius: 11px;
}

.pev-filter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.pev-filter em {
  font-style: normal;
  font-size: 10px;
  font-weight: 700;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.12);
  color: #64748b;
  transition: all 0.18s ease;
}

.pev-filter:hover {
  color: #1f2d3d;
}

.pev-filter.active {
  background: #fff;
  color: #0f1e30;
  box-shadow: 0 1px 4px rgba(16, 24, 40, 0.1);
}

.pev-filter.miss em {
  background: rgba(217, 119, 6, 0.14);
  color: #b45309;
}

.pev-filter.off em {
  background: rgba(100, 116, 139, 0.14);
  color: #8a94a6;
}

.pev-filter.miss.active {
  color: #b45309;
}

.pev-filter.off.active {
  color: #475569;
}

/* ===== 分组标题：渐变徽章 + 渐隐分隔线 ===== */
.pev-group-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 2px 9px;
}

.pev-group-badge {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11.5px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 2px 6px rgba(16, 24, 40, 0.18);
}

.pev-group-title {
  font-size: 13px;
  font-weight: 700;
  color: #1f2d3d;
  letter-spacing: 0.3px;
}

.pev-group-count {
  font-size: 10.5px;
  color: #94a3b8;
}

.pev-group-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #dbe4f0, rgba(219, 228, 240, 0));
}

/* 分组容器：浅蓝灰底面板，行卡片浮于其上 */
.pev-group-body {
  padding: 8px;
  background: #f6f9fd;
  border: 1px solid #edf2f9;
  border-radius: 14px;
}

.pev-group-body .pev-item {
  box-shadow: none;
}

.pev-group-body .pev-item + .pev-item {
  margin-top: 6px;
}

/* 悬停浮现次要信息与行内操作，保持静止时表面干净 */
.pev-src,
.pev-ghost-btn {
  opacity: 0;
}

.pev-src {
  transition: opacity 0.18s ease;
}

.pev-item:hover .pev-src,
.pev-item:hover .pev-ghost-btn,
.pev-item:focus-within .pev-src,
.pev-item:focus-within .pev-ghost-btn,
.pev-item.editing .pev-src {
  opacity: 1;
}

/* 弹窗体：顶部一抹极浅的蓝调渐变 */
.pev-dlg .pcd-body {
  background: linear-gradient(180deg, #fafcff 0%, #ffffff 180px);
}

/* ===== 变量行卡片 ===== */
.pev-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 11px 14px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(31, 45, 61, 0.03);
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease,
    transform 0.18s ease;
  animation: pev-in 0.3s ease both;
}

.pev-item + .pev-item {
  margin-top: 8px;
}

@keyframes pev-in {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pev-item:hover {
  border-color: #c7d7f5;
  background: #f8fbff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

/* 编辑态：蓝框聚焦光环 */
.pev-item.editing,
.pev-item.editing:hover {
  border-color: #93c5fd;
  background: #f0f7ff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  transform: none;
}

/* 未设置：琥珀虚线 */
.pev-item.missing {
  background: #fffdf7;
  border-color: #f1e5c0;
  border-style: dashed;
}

.pev-item.missing:hover {
  background: #fffbeb;
  border-color: #fde68a;
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.08);
}

/* 已禁用：整体弱化 */
.pev-item.off {
  background: #fcfdfe;
}

.pev-item-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pev-item-label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2d3d;
}

.pev-dot {
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #cbd5e1;
}

.pev-item.ok .pev-dot {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.16);
}

.pev-item.missing .pev-dot {
  background: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.16);
}

.pev-secret {
  font-size: 10px;
  opacity: 0.8;
}

/* 变量名 key 芯片 */
.pev-key {
  align-self: flex-start;
  padding: 1px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  border: 1px solid #e6edf5;
  font-size: 10.5px;
  color: #64748b;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pev-item-side {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
  flex-shrink: 0;
  max-width: 56%;
  flex-wrap: wrap;
}

.pev-src {
  font-size: 10.5px;
  color: #b6c0cf;
  white-space: nowrap;
}

/* 值芯片 */
.pev-value {
  display: inline-block;
  max-width: 220px;
  padding: 3px 10px;
  border-radius: 8px;
  border: 1px solid #e6edf5;
  background: #f8fafc;
  font-size: 12.5px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 被禁用行的值：置灰 + 删除线 */
.pev-value.disabled {
  color: #94a3b8;
  background: #fbfcfd;
  text-decoration: line-through;
  text-decoration-color: rgba(148, 163, 184, 0.6);
}

/* 行内幽灵操作按钮 */
.pev-ghost-btn {
  min-width: 26px;
  height: 26px;
  padding: 0 5px;
  border: none;
  background: transparent;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #94a3b8;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.pev-ghost-btn:hover {
  background: #eef4ff;
  color: #2563eb;
  transform: translateY(-1px);
}

/* 状态标签 */
.pev-tag {
  font-size: 10.5px;
  padding: 2.5px 10px;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}

.pev-tag.missing {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #b45309;
}

.pev-tag.off {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  color: #94a3b8;
}

/* 未设置项的「设置值」按钮 */
.pev-set {
  height: 26px;
  padding: 0 13px;
  border: 1px solid #c7d7f5;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  color: #2563eb;
  background: #fff;
  cursor: pointer;
  transition: all 0.16s ease;
  white-space: nowrap;
}

.pev-set:hover {
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.3);
}

/* 编辑态输入框与保存/取消 */
.pev-input {
  width: 250px;
}

.pev-input .el-input__wrapper {
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 0 0 1px #bfdbfe inset;
  transition: box-shadow 0.18s ease;
}

.pev-input .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.pev-save {
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 9px;
  background: linear-gradient(135deg, #34d399, #059669);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(5, 150, 105, 0.28);
  transition: all 0.15s ease;
  white-space: nowrap;
}

.pev-save:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(5, 150, 105, 0.4);
}

.pev-save:disabled {
  opacity: 0.75;
  cursor: default;
}

.pev-cancel {
  height: 32px;
  padding: 0 13px;
  border: none;
  border-radius: 9px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pev-cancel:hover:not(:disabled) {
  background: #e2e8f0;
  color: #334155;
}

/* 筛选无结果 */
.pev-none {
  padding: 30px 16px;
  text-align: center;
  font-size: 12.5px;
  color: #8a94a6;
  background: #f8fafc;
  border: 1px dashed #dbe4ee;
  border-radius: 12px;
  margin-top: 10px;
}

/* 页脚 */
/** 执行日志按钮：固定在操作区最左侧；核心圆点按 运行中/成功/失败 切换配色 */
.pc-log-btn {
  margin-right: auto;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  padding: 0 20px 0 5px;
  border: 1px solid #dbe4f0;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffffff, #f7fafd);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.18s ease;
  animation: pc-log-btn-in 0.35s cubic-bezier(0.3, 1.2, 0.45, 1) both;
}

@keyframes pc-log-btn-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.pc-log-btn:hover {
  transform: translateY(-1px);
  border-color: #b8cdf1;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.16);
}

.pc-log-btn:active {
  transform: translateY(0);
}

/* 核心圆点：随状态切换渐变色 */
.pc-log-btn-core {
  position: relative;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #64748b, #475569);
  transition: background 0.25s ease;
}

.pc-log-btn.running .pc-log-btn-core {
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 68%, #2563eb 100%);
}

.pc-log-btn.ok .pc-log-btn-core {
  background: linear-gradient(135deg, #34d399, #059669);
}

.pc-log-btn.fail .pc-log-btn-core {
  background: linear-gradient(135deg, #f87171, #dc2626);
}

.pc-log-btn-ico {
  position: relative;
  font-size: 17px;
  line-height: 1;
  z-index: 1;
}

/* 运行中：外圈旋转虚线光环 */
.pc-log-btn-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px dashed rgba(37, 99, 235, 0.5);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.pc-log-btn.running .pc-log-btn-ring {
  opacity: 1;
  animation: pc-log-btn-spin 3.2s linear infinite;
}

@keyframes pc-log-btn-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 运行任务数角标 */
.pc-log-btn-badge {
  position: absolute;
  top: -3px;
  right: -5px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 10px;
  background: #ef4444;
  border: 2px solid #fff;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  z-index: 2;
  animation: pc-log-btn-pulse 1.6s ease infinite;
}

@keyframes pc-log-btn-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.45);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0);
  }
}

.pc-log-btn-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  white-space: nowrap;
}

.pc-log-btn-title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #1f2d3d;
  text-align: left;
}

.pc-log-btn-sub {
  font-size: 11px;
  color: #8a94a6;
  text-align: left;
  transition: color 0.25s ease;
}

.pc-log-btn.running .pc-log-btn-sub {
  color: #2563eb;
}

.pc-log-btn.ok .pc-log-btn-sub {
  color: #059669;
}

.pc-log-btn.fail .pc-log-btn-sub {
  color: #dc2626;
}

/** 多会话标签页 */
.pcd-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 20px 0;
  background: #fff;
}

.pcd-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px;
  border: 1px solid #e4eaf2;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pcd-tab:hover {
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.pcd-tab.active {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.pcd-tab-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.pcd-tab-dot.running {
  background: #2563eb;
  animation: pcd-tab-pulse 1.2s ease infinite;
}

.pcd-tab-dot.ok {
  background: #10b981;
}

.pcd-tab-dot.fail {
  background: #ef4444;
}

@keyframes pcd-tab-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(37, 99, 235, 0);
  }
}

.pcd-tab-label {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pcd-tab-x {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  font-size: 10px;
  color: #94a3b8;
  transition: all 0.15s ease;
}

.pcd-tab-x:hover {
  background: #fee2e2;
  color: #dc2626;
}

.pcd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
}

.pcd-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 22px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  border: 1px solid transparent;
}

.pcd-btn.ghost {
  background: #fff;
  border-color: #dbe2ea;
  color: #475569;
}

.pcd-btn.ghost:hover {
  border-color: #b9c4d2;
  color: #1f2d3d;
}

.pcd-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.pcd-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.45);
}

.pcd-btn.primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.pcd-spin {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: pcd-rotate 0.8s linear infinite;
}

@keyframes pcd-rotate {
  to { transform: rotate(360deg); }
}
</style>
