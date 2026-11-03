<script setup lang="ts">
// 中间件管理页：按配置展示中间件卡片，内嵌窗口打开并按策略自动登录（Grafana/Nacos/XXL-Job 等）
import { computed, nextTick, onMounted, ref } from 'vue'
import { Link as LinkIcon, Promotion, Refresh } from '@element-plus/icons-vue'
import { Md5 } from 'ts-md5'
import { getConfigDropDownList } from '@/api/config'
import type { ConfigOptionItem } from '@/types/api'

/** 中间件配置类型编码 */
const MIDDLEWARE_TYPE = 'system_middleware_config'

/** 中间件项：由配置 value（JSON：type/url/icon/username/password）解析而来 */
interface MiddlewareItem {
  id: string | number
  name: string
  alias: string
  /** 中间件类型（登录策略匹配依据，未配置时回退配置名称） */
  type: string
  url: string
  /** 图标地址（空则展示首字母头像） */
  icon: string
  username: string
  password: string
}

/** 解析配置项的 value 为中间件信息，url 缺失视为无效配置 */
function parseMiddleware(item: ConfigOptionItem): MiddlewareItem | null {
  try {
    const v = JSON.parse(item.value || '')
    if (!v || typeof v !== 'object' || !v.url) return null
    return {
      id: item.id,
      name: item.name,
      alias: item.alias || item.name,
      type: String(v.type ?? ''),
      url: String(v.url),
      icon: String(v.icon ?? ''),
      username: String(v.username ?? ''),
      password: String(v.password ?? '')
    }
  } catch {
    return null
  }
}

/** 图标加载失败时清空地址，退回首字母头像 */
function handleIconError(mw: MiddlewareItem) {
  mw.icon = ''
}

const list = ref<MiddlewareItem[]>([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await getConfigDropDownList(MIDDLEWARE_TYPE)
    list.value = (res.data || [])
      .map(parseMiddleware)
      .filter((i): i is MiddlewareItem => i !== null)
  } finally {
    loading.value = false
  }
}

// ============ 内嵌打开 + 自动登录 ============
/** 自动登录策略 */
interface LoginStrategy {
  /**
   * form：直连中间件地址，no-cors 提交登录表单（Cookie 会话型）
   * sameOrigin：经后台同源代理加载，登录态写入方式见 auth
   */
  mode: 'form' | 'sameOrigin'
  /** sameOrigin 模式登录态写入方式：cookie=表单登录响应 Set-Cookie；token=登录响应写 localStorage；
   *  snailjob=按 Snail-Job 约定将 token 与默认命名空间写 localStorage */
  auth?: 'cookie' | 'token' | 'snailjob'
  /** 密码提交前做 md5 哈希（Snail-Job 登录页即以 md5 后的值提交，服务端存哈希比对） */
  hash?: 'md5'
  /** 登录体编码：form=表单（默认）；json=JSON 体（如 Grafana，表单格式会被其拒绝返回 400） */
  bodyType?: 'form' | 'json'
  /** 为 true 时先 GET 登录页取 XSRF-TOKEN Cookie，再随表单提交 _csrf（Spring Security 表单登录，如 SBA）。
   *  form 与 sameOrigin 模式均支持：form 直连场景下依赖同域（localhost 各端口共享 Cookie）可读到该 Cookie */
  csrf?: boolean
  /** sameOrigin 模式下经本服务代理的路径前缀（vite proxy / 反向代理配置） */
  proxyPath?: string
  /** 登录接口地址（sameOrigin 模式入参为代理路径前缀） */
  action: (base: string) => string
  /** 登录表单字段名与账号/密码的映射 */
  fields: { username: string; password: string }
}

/** Spring Boot Admin 策略：直连监控台地址 + Spring Security 表单登录（取 CSRF 后提交）。
 *  同域（localhost 不同端口）下 Cookie 按域共享，会话可直接生效；跨域部署时退回监控台自身登录页 */
const SPRING_BOOT_ADMIN_STRATEGY: LoginStrategy = {
  mode: 'form',
  csrf: true,
  action: (url) => `${url}/login`,
  fields: { username: 'username', password: 'password' }
}

/** XXL-Job 策略：AJAX 表单登录（字段 userName，接口 /auth/doLogin，无 CSRF）。
 *  成功下发 xxl_job_login_token Cookie（签名存库校验），同域共享生效 */
const XXL_JOB_STRATEGY: LoginStrategy = {
  mode: 'form',
  action: (url) => `${url}/auth/doLogin`,
  fields: { username: 'userName', password: 'password' }
}

/** Snail-Job 策略：官方前端生产构建即挂载于 /snail-job 子路径（hash 路由），经同源代理加载；
 *  密码 md5 后 JSON 提交 /auth/login，成功将 token 与默认命名空间写入 localStorage
 *  （与官方前端 localStg 的 JSON 序列化格式一致），控制台读取后即登录 */
const SNAIL_JOB_STRATEGY: LoginStrategy = {
  mode: 'sameOrigin',
  auth: 'snailjob',
  bodyType: 'json',
  hash: 'md5',
  proxyPath: '/snail-job/',
  action: (base) => `${base}auth/login`,
  fields: { username: 'username', password: 'password' }
}

/**
 * 各中间件登录策略（key 为 value.type 归一化结果，未配置 type 时用配置名称），未命中的走默认策略：
 * - grafana：经 /grafana 同源代理（容器已按子路径提供服务），表单登录后 Cookie 为第一方，自动登录稳定生效
 * - nacos：经 /nacos 同源代理，登录响应按其约定写入 localStorage.token，控制台读取后即登录
 * - springbootadmin：直连监控台地址，取 CSRF 后提交表单登录（同域 Cookie 共享生效）
 * - xxljob：直连调度中心，AJAX 表单登录（userName 字段），令牌 Cookie 同域共享生效
 * - snailjob：经 /snail-job 同源代理，md5 密码 JSON 登录，token + 默认命名空间写 localStorage 生效
 */
const LOGIN_STRATEGIES: Record<string, LoginStrategy> = {
  grafana: {
    mode: 'sameOrigin',
    auth: 'cookie',
    bodyType: 'json',
    proxyPath: '/grafana/',
    action: (base) => `${base}login`,
    fields: { username: 'user', password: 'password' }
  },
  nacos: {
    mode: 'sameOrigin',
    auth: 'token',
    proxyPath: '/nacos/',
    action: (base) => `${base}v1/auth/users/login`,
    fields: { username: 'username', password: 'password' }
  },
  // Spring Boot Admin 常见命名均指向同一策略
  springbootadmin: SPRING_BOOT_ADMIN_STRATEGY,
  sba: SPRING_BOOT_ADMIN_STRATEGY,
  monitor: SPRING_BOOT_ADMIN_STRATEGY,
  // XXL-Job（xxl-job / xxl_job / XXL-Job 等命名归一化后均命中）
  xxljob: XXL_JOB_STRATEGY,
  // Snail-Job（Snail-Job / snail_job / SnailJob 等命名归一化后均命中）
  snailjob: SNAIL_JOB_STRATEGY
}

const DEFAULT_STRATEGY: LoginStrategy = {
  mode: 'form',
  action: (url) => `${url}/login`,
  fields: { username: 'username', password: 'password' }
}

const dialogVisible = ref(false)
const current = ref<MiddlewareItem | null>(null)
/** login=登录中（展示过渡动画），ready=加载主 iframe */
const phase = ref<'login' | 'ready'>('login')
const frameSrc = ref('')
/** 主 iframe 的 key：自增触发元素重建，实现刷新 */
const frameKey = ref(0)

const currentStrategy = computed<LoginStrategy | null>(() => {
  if (!current.value) return null
  // 类型优先（value.type），未配置时回退配置名称；
  // 归一化匹配忽略大小写与分隔符，兼容 XXL-Job / xxl_job / SpringBootAdmin 等写法
  const raw = current.value.type || current.value.name
  const key = raw.toLowerCase().replace(/[^a-z0-9]/g, '')
  return LOGIN_STRATEGIES[key] || DEFAULT_STRATEGY
})

/** 按策略字段映射构造登录请求体（按 bodyType 编码为表单或 JSON） */
function buildLoginBody(mw: MiddlewareItem, strategy: LoginStrategy): string {
  const pairs: Record<string, string> = {}
  if (strategy.fields.username && mw.username) {
    pairs[strategy.fields.username] = mw.username
  }
  if (strategy.fields.password && mw.password) {
    pairs[strategy.fields.password] = strategy.hash === 'md5' ? Md5.hashStr(mw.password) : mw.password
  }
  if (!Object.keys(pairs).length) return ''
  return strategy.bodyType === 'json'
    ? JSON.stringify(pairs)
    : new URLSearchParams(pairs).toString()
}

/** 解析配置 URL 的同源加载地址：路径须以代理前缀开头（支持直达子页面，如 /grafana/explore），
 *  不匹配或 URL 非法时回退代理根路径 */
function sameOriginFramePath(mw: MiddlewareItem, proxyPath: string): string {
  try {
    const { pathname, search } = new URL(mw.url)
    if (pathname === proxyPath.replace(/\/+$/, '')) {
      // 根路径返回规范代理前缀（带尾斜杠）：Nacos 对无斜杠路径会 302 到其绝对地址，
      // 把 iframe 带出同源代理导致跨域报错
      return proxyPath
    }
    if (pathname.startsWith(proxyPath)) {
      return `${pathname}${search}`
    }
  } catch {
    // URL 非法时回退代理根路径
  }
  return proxyPath
}

/** 进入主界面：sameOrigin 模式加载配置 URL 对应的代理路径，其余加载原始地址 */
function showMainFrame() {
  if (!current.value) return
  const strategy = currentStrategy.value
  frameSrc.value =
    strategy?.mode === 'sameOrigin' && strategy.proxyPath
      ? sameOriginFramePath(current.value, strategy.proxyPath)
      : current.value.url
  phase.value = 'ready'
}

/**
 * form 模式：以 no-cors fetch 直连中间件登录接口提交账号密码：
 * 不经过 iframe，不受 X-Frame-Options 限制；同站环境下响应 Set-Cookie 写入浏览器。
 * 登录失败不阻塞，仍尝试加载（最多显示中间件自身登录页）
 */
async function performLogin(mw: MiddlewareItem, strategy: LoginStrategy) {
  let body = buildLoginBody(mw, strategy)
  if (body && strategy.csrf) {
    // Spring Security 表单登录：先 GET 登录页让服务端下发 XSRF-TOKEN Cookie。
    // 同域（localhost 各端口共享 Cookie）下 document.cookie 可读到该 Cookie，作为 _csrf 随表单提交
    try {
      await fetch(strategy.action(mw.url), { mode: 'no-cors', credentials: 'include' })
      const xsrf = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)?.[1]
      if (xsrf) {
        const params = new URLSearchParams(body)
        params.set('_csrf', decodeURIComponent(xsrf))
        body = params.toString()
      }
    } catch {
      // 取 CSRF 失败不阻塞，按原表单继续尝试
    }
  }
  if (body) {
    try {
      await fetch(strategy.action(mw.url), {
        method: 'POST',
        mode: 'no-cors',
        credentials: 'include',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
      })
    } catch {
      // 网络异常不阻塞，继续加载主界面（最多显示中间件自身登录页）
    }
  }
  showMainFrame()
}

/**
 * sameOrigin 模式（Grafana / Nacos / Snail-Job）：
 * 登录接口经同源代理，第一方上下文无跨站 Cookie 限制——
 * cookie 型（Grafana）：响应 Set-Cookie 直接生效，加载控制台即登录；
 * token 型（Nacos）：登录响应按其约定写入 localStorage.token，控制台读取后即登录；
 * snailjob 型（Snail-Job）：登录响应的 token 与默认命名空间按官方前端约定写入 localStorage。
 * 接口失败不阻塞，退回控制台自身登录页
 */
async function performSameOriginLogin(mw: MiddlewareItem, strategy: LoginStrategy) {
  const base = strategy.proxyPath || '/'
  let body = buildLoginBody(mw, strategy)
  if (body && strategy.csrf) {
    // Spring Security 表单登录：先 GET 登录页让服务端下发 XSRF-TOKEN Cookie，
    // 同源上下文可读取该 Cookie，将其作为 _csrf 随表单提交
    try {
      await fetch(strategy.action(base), { credentials: 'include' })
      const xsrf = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)?.[1]
      if (xsrf) {
        const params = new URLSearchParams(body)
        params.set('_csrf', decodeURIComponent(xsrf))
        body = params.toString()
      }
    } catch {
      // 取 CSRF 失败不阻塞，按原表单继续尝试
    }
  }
  if (body) {
    try {
      const res = await fetch(strategy.action(base), {
        method: 'POST',
        credentials: 'include',
        // 登录成功常以 302 响应（Location 为中间件绝对地址）；manual 避免跟随后跳出代理源，
        // Set-Cookie 在 302 响应本身即已生效
        redirect: 'manual',
        headers: {
          'Content-Type':
            strategy.bodyType === 'json'
              ? 'application/json'
              : 'application/x-www-form-urlencoded'
        },
        body
      })
      if (res.ok && strategy.auth === 'token') {
        localStorage.setItem('token', JSON.stringify(await res.json()))
      }
      if (res.ok && strategy.auth === 'snailjob') {
        // Snail-Job 登录响应：{ status, data: { token, id, namespaceIds: [{ uniqueId }] } }（status=1 成功）。
        //  按官方前端约定写 localStorage（JSON 序列化），控制台加载时读取 token 即视为已登录
        const data = (await res.json())?.data
        if (data?.token) {
          localStorage.setItem('token', JSON.stringify(data.token))
          const ns = data.namespaceIds?.[0]?.uniqueId
          if (ns) {
            localStorage.setItem('namespaceId', JSON.stringify(ns))
            if (data.id) {
              localStorage.setItem('userNamespace', JSON.stringify({ [data.id]: ns }))
            }
          }
        }
      }
    } catch {
      // 网络异常不阻塞，继续加载控制台
    }
  }
  showMainFrame()
}

function handleOpen(mw: MiddlewareItem) {
  current.value = mw
  frameSrc.value = ''
  phase.value = 'login'
  dialogVisible.value = true
  nextTick(() => {
    const strategy = currentStrategy.value
    if (!strategy) return
    if (strategy.mode === 'sameOrigin') {
      performSameOriginLogin(mw, strategy)
    } else {
      performLogin(mw, strategy)
    }
  })
}

/** 刷新内嵌页面：重建 iframe（跨域页面无法直接调用其 location.reload）；同源模式顺带重新自动登录 */
function handleRefresh() {
  if (phase.value !== 'ready' || !current.value) return
  const strategy = currentStrategy.value
  frameKey.value++
  if (strategy?.mode === 'sameOrigin') {
    phase.value = 'login'
    performSameOriginLogin(current.value, strategy)
  }
}

function handleDialogClosed() {
  current.value = null
  frameSrc.value = ''
  frameKey.value = 0
  phase.value = 'login'
}

/** 兜底：在独立浏览器窗口打开（iframe 被中间件安全策略拒绝时使用） */
function openExternal() {
  if (current.value) window.open(current.value.url, '_blank')
}

onMounted(loadList)
</script>

<template>
  <div class="list-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-card-header">
          <div class="header-left">
            <span class="card-title">中间件列表</span>
            <span class="card-subtitle">共 {{ list.length }} 个 · 点击卡片在内嵌窗口打开</span>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="mw-grid">
        <div v-for="m in list" :key="m.id" class="mw-card" @click="handleOpen(m)">
          <div class="mw-avatar">
            <img v-if="m.icon" :src="m.icon" :alt="m.alias" @error="handleIconError(m)" />
            <template v-else>{{ m.name.slice(0, 1).toUpperCase() }}</template>
          </div>
          <div class="mw-info">
            <div class="mw-name">
              <span class="name-text" :title="m.alias">{{ m.alias }}</span>
              <el-tag size="small" effect="plain" class="name-tag">{{ m.name }}</el-tag>
            </div>
            <div class="mw-url" :title="m.url">
              <el-icon :size="12"><LinkIcon /></el-icon>
              <span>{{ m.url }}</span>
            </div>
            <div v-if="m.username" class="mw-user">账号：{{ m.username }}</div>
          </div>
          <div class="mw-action">
            <el-icon :size="13"><Promotion /></el-icon>
            <span>打开</span>
          </div>
        </div>

        <el-empty
          v-if="!loading && list.length === 0"
          class="mw-empty"
          description="暂无中间件配置（system_middleware_config）"
        />
      </div>
    </el-card>

    <!-- 内嵌中间件窗口：隐藏表单先完成自动登录，再加载主 iframe -->
    <el-dialog
      v-model="dialogVisible"
      class="mw-dialog"
      fullscreen
      destroy-on-close
      append-to-body
      @closed="handleDialogClosed"
    >
      <template #header>
        <div class="mw-dialog-header">
          <img
            v-if="current && current.icon"
            :src="current.icon"
            class="mw-dialog-icon"
            alt=""
            @error="current && (current.icon = '')"
          />
          <span class="mw-dialog-title">{{ current?.alias }}</span>
          <span class="mw-dialog-url">{{ current?.url }}</span>
          <el-button
            link
            type="primary"
            :icon="Refresh"
            :disabled="phase !== 'ready'"
            @click="handleRefresh"
          >
            刷新
          </el-button>
          <el-button link type="primary" @click="openExternal">在新窗口打开</el-button>
        </div>
      </template>

      <div class="mw-body">
        <div v-if="phase === 'login'" class="mw-loading">
          <div class="mw-spinner" />
          <p>正在自动登录 {{ current?.alias }}，请稍候…</p>
          <el-button link type="primary" @click="showMainFrame">跳过等待，直接打开</el-button>
        </div>
        <template v-else>
          <iframe :key="frameKey" :src="frameSrc" class="mw-main-frame" />
          <div class="mw-frame-tip">
            页面空白？中间件可能禁止内嵌（如 Grafana 需开启 allow_embedding），
            可点右上角"在新窗口打开"
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.mw-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.mw-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #a5b4fc;
    box-shadow: 0 6px 18px rgba(99, 102, 241, 0.12);
    transform: translateY(-2px);

    .mw-action {
      color: #fff;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      border-color: transparent;
    }
  }
}

.mw-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  color: #6366f1;
  font-size: 20px;
  font-weight: 600;
  background: var(--el-fill-color-dark);
  border: 1px solid var(--el-border-color-light);
  box-shadow: none;

  img {
    width: 28px;
    height: 28px;
    object-fit: contain;
  }
}

.mw-info {
  flex: 1;
  min-width: 0;
}

.mw-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);

  // 别名单行省略，标签固定不收缩，空间不足时不再换行挤高卡片
  .name-text {
    flex: 0 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .name-tag {
    flex-shrink: 0;
  }
}

.mw-url {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.mw-user {
  margin-top: 4px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.mw-action {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 5px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  color: var(--el-text-color-regular);
  font-size: 12px;
  transition: all 0.2s;
}

.mw-empty {
  grid-column: 1 / -1;
}

// ============ 内嵌窗口 ============
.mw-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.mw-dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-right: 30px;
}

.mw-dialog-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  flex-shrink: 0;
}

.mw-dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.mw-dialog-url {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.mw-body {
  position: relative;
  height: calc(100vh - 58px);
  background: #fff;
}

.mw-main-frame {
  display: block;
  width: 100%;
  height: calc(100% - 28px);
  border: none;
}

.mw-frame-tip {
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  background: var(--el-fill-color-lighter);
}

.mw-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #f5f6fb 0%, #eef0fa 100%);

  p {
    margin: 0 0 8px;
    color: var(--el-text-color-secondary);
    font-size: 14px;
  }
}

.mw-spinner {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 3px solid #dcddfb;
  border-top-color: #6366f1;
  animation: mw-spin 0.8s linear infinite;
}

@keyframes mw-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
