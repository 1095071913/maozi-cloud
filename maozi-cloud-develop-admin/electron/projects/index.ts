import { app, ipcMain, dialog, shell, BrowserWindow } from 'electron'
import { execFile, spawn } from 'node:child_process'
import { promisify } from 'node:util'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { getBinding, saveBinding, clearBinding, parseConfigFile, isDbInitialized, markDbInitialized } from './store'
import { listConfigs } from '../configs/store'
import { selectPlatform } from '../platform'
import type { EnvFile, EnvVarEntry, HostsEntry } from '../platform/types'
import { getShellPath } from '../system/sysinfo'
import type { ComposeServiceStats, EnvSettingGroup, EnvSettingItem, ProjectBinding } from './types'

const exec = promisify(execFile)

/**
 * 项目控制台 IPC：
 * - 选择本地目录 → 读取 CONFIG(name/version) 完成绑定
 * - 拉取代码 → 用密钥管理中的凭据(git clone HTTPS)，账密 / Token 两种认证，
 *   凭据仅拼接在内存中的克隆地址里，不落盘；错误信息会脱敏
 */

const REPO_URL = 'https://github.com/1095071913/maozi-cloud.git'
/** 基础服务 docker compose 目录（相对项目根目录） */
const COMPOSE_DIR =
  'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-basics-docker'

/** 项目内置 hosts 映射文件（相对项目根目录，每行 "IP<Tab>域名"） */
const HOSTS_FILE = 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/HOSTS'

/** 项目环境变量定义文件：JSON，key 为中文名称，value 为环境变量 key；value 为对象时表示分组 */
const ENV_VARS_FILE = 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/ENVIRONMENT_VARIABLE'

/**
 * 数据库初始化定义文件：每行一个 SQL 脚本路径（相对项目根），# 注释与空行忽略。
 * 初始化标记为 .db-init.json（见 store.ts，位于 maozi-cloud-develop-admin 应用目录，git 已忽略）
 */
const INIT_MYSQL_DB_FILE = 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/INIT_MYSQL_DB'

interface HostsDef {
  ip: string
  domains: string
}

/** 解析项目 hosts 文件：忽略空行与注释行 */
function parseProjectHosts(text: string): HostsDef[] {
  const defs: HostsDef[] = []
  for (const raw of text.split('\n')) {
    const line = raw.trim()
    if (!line || line.startsWith('#')) continue
    const m = line.match(/^(\S+)\s+(.+)$/)
    if (!m) continue
    if (!/^[0-9a-fA-F:.]+$/.test(m[1])) continue
    const domains = m[2].trim().split(/\s+/).filter(Boolean).join(' ')
    if (domains) defs.push({ ip: m[1], domains })
  }
  return defs
}

/** 解析 ENVIRONMENT_VARIABLE（JSON）：字符串 value 为直接映射，对象 value 为分组 */
function parseEnvVarDefs(text: string): EnvSettingGroup[] {
  let data: unknown
  try {
    data = JSON.parse(text)
  } catch (err) {
    throw new Error(`ENVIRONMENT_VARIABLE 不是合法 JSON：${(err as Error).message}`)
  }
  if (typeof data !== 'object' || data === null || Array.isArray(data)) {
    throw new Error('ENVIRONMENT_VARIABLE 格式错误：顶层应为 JSON 对象')
  }
  const groups: EnvSettingGroup[] = []
  for (const [name, val] of Object.entries(data as Record<string, unknown>)) {
    if (typeof val === 'string') {
      if (!groups[0] || groups[0].name) groups.unshift({ name: '', items: [] })
      groups[0].items.push({ label: name, key: val, value: '', found: false, enabled: false, source: '' })
      continue
    }
    if (typeof val !== 'object' || val === null || Array.isArray(val)) {
      throw new Error(`ENVIRONMENT_VARIABLE 中「${name}」的值应为字符串或对象`)
    }
    const items: EnvSettingItem[] = []
    for (const [label, key] of Object.entries(val as Record<string, unknown>)) {
      if (typeof key !== 'string') throw new Error(`ENVIRONMENT_VARIABLE「${name}.${label}」的值应为环境变量 key 字符串`)
      items.push({ label, key, value: '', found: false, enabled: false, source: '' })
    }
    groups.push({ name, items })
  }
  return groups
}

/**
 * 环境变量取值：shell 配置里已启用的最优先（文件顺序即展示顺序，zshrc/bash_profile 靠前），
 * 其次当前进程环境（覆盖 launchctl setenv 场景），最后保留被注释禁用行的值
 */
function applyEnvValues(groups: EnvSettingGroup[], vars: EnvVarEntry[], files: EnvFile[]): void {
  const fileNames = new Map(files.map((f) => [f.id, f.name]))
  const lookup = (key: string): EnvSettingItem => {
    const enabled = vars.find((v) => v.key === key && v.enabled)
    if (enabled) {
      return { label: '', key, value: enabled.value, found: true, enabled: true, source: fileNames.get(enabled.fileId) ?? '', fileId: enabled.fileId }
    }
    if (process.env[key] !== undefined) {
      return { label: '', key, value: process.env[key] ?? '', found: true, enabled: true, source: '进程环境' }
    }
    const disabled = vars.find((v) => v.key === key)
    if (disabled) {
      return { label: '', key, value: disabled.value, found: true, enabled: false, source: fileNames.get(disabled.fileId) ?? '', fileId: disabled.fileId }
    }
    return { label: '', key, value: '', found: false, enabled: false, source: '' }
  }
  for (const g of groups) {
    g.items = g.items.map((it) => ({ ...it, ...lookup(it.key), label: it.label }))
  }
}

/** 后台前端容器（maozi-cloud-admin-distributeds）compose 目录 / 文件与容器名 */
const ADMIN_COMPOSE_DIR =
  'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-distributeds-docker'
const ADMIN_COMPOSE_FILE = 'maozi-cloud-admin-distributeds-docker.yml'
const ADMIN_CONTAINER = 'maozi-cloud-admin-distributeds'

/**
 * 应用服务（单体 / 微服务）compose 定义：每变体一个目录，含 admin 与 services 两个 compose 文件。
 * 两变体互斥：启动任一变体服务前，先 down 掉另一变体的全部服务
 */
type AppSvcVariant = 'monomer' | 'distributeds'

const APP_SVC_DEFS: Record<AppSvcVariant, { label: string; dir: string; adminFile: string; servicesFile: string }> = {
  monomer: {
    label: '单体',
    dir: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-monomer-docker',
    adminFile: 'maozi-cloud-admin-monomer-docker.yml',
    servicesFile: 'maozi-cloud-services-monomer-docker.yml'
  },
  distributeds: {
    label: '微服务',
    dir: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-distributeds-docker',
    adminFile: 'maozi-cloud-admin-distributeds-docker.yml',
    servicesFile: 'maozi-cloud-services-distributeds-docker.yml'
  }
}

/** 应用服务日志/操作上下文：'basics' 或 '<variant>:<admin|services>' */
function resolveComposeCtx(ctx: string): { dir: string; file: string } | null {
  if (ctx === 'basics') return null
  const m = ctx.match(/^(monomer|distributeds):(admin|services)$/)
  if (!m) return null
  const def = APP_SVC_DEFS[m[1] as AppSvcVariant]
  return { dir: def.dir, file: m[2] === 'admin' ? def.adminFile : def.servicesFile }
}

/** 读取变体的服务清单（admin + services 两个文件合并，标记归属文件） */
function readAppServices(bPath: string, variant: AppSvcVariant): Array<{ name: string; file: 'admin' | 'services' }> {
  const def = APP_SVC_DEFS[variant]
  const out: Array<{ name: string; file: 'admin' | 'services' }> = []
  const read = (fileName: string, tag: 'admin' | 'services'): void => {
    const yml = path.join(bPath, def.dir, fileName)
    if (!fs.existsSync(yml)) return
    for (const name of parseComposeServices(fs.readFileSync(yml, 'utf8'))) out.push({ name, file: tag })
  }
  read(def.adminFile, 'admin')
  read(def.servicesFile, 'services')
  return out
}


/** 项目内可执行脚本（相对项目根目录） */
const DEPLOY_SCRIPTS: Record<string, { file: string; label: string }> = {
  demand: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/maozi-cloud-deploy-shell-run/maozi-cloud-deploy-services-distributed.sh',
    label: '微服务按需编译启动'
  },
  all: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/maozi-cloud-deploy-shell-run/maozi-cloud-deploy-services-distributed-force.sh',
    label: '微服务全量启动'
  },
  admin: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/maozi-cloud-deploy-shell-run/maozi-cloud-deploy-admin-distributed.sh',
    label: '后台启动'
  },
  monomerServices: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/maozi-cloud-deploy-shell-run/maozi-cloud-deploy-services-monomer.sh',
    label: '单体服务编译启动'
  },
  monomerAdmin: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-run/maozi-cloud-deploy-shell-run/maozi-cloud-deploy-admin-monomer.sh',
    label: '后台编译启动'
  }
}
const CLONE_DIR = 'maozi-cloud'

/** 执行 git（登录 shell PATH），错误信息中抹掉含凭据的 URL */
async function runGit(args: string[], timeoutMs: number): Promise<string> {
  const pathEnv = await getShellPath()
  try {
    const { stdout } = await exec('git', args, {
      timeout: timeoutMs,
      env: { ...process.env, PATH: pathEnv }
    })
    return stdout
  } catch (err) {
    const e = err as { killed?: boolean; stderr?: string; message?: string }
    const raw = (e.stderr || e.message || '').replace(/https:\/\/[^@\s]+@/g, 'https://***@')
    if (e.killed) throw new Error('git 执行超时，请检查网络后重试')
    throw new Error(raw.trim() || 'git 执行失败')
  }
}

async function ensureGit(): Promise<void> {
  try {
    await runGit(['--version'], 8000)
  } catch (err) {
    throw new Error(`本机未安装 git 或不可用：${(err as Error).message}`)
  }
}

function focusedWindow(): BrowserWindow | null {
  return BrowserWindow.getFocusedWindow() ?? BrowserWindow.getAllWindows()[0] ?? null
}

/** 读取 macOS 系统代理(scutil --proxy)转为 git 可用的代理环境变量；GUI 应用不继承 shell 代理，需显式注入 */
async function getProxyEnv(): Promise<Record<string, string>> {
  const env: Record<string, string> = {}
  if (process.platform !== 'darwin') return env
  try {
    const { stdout } = await exec('scutil', ['--proxy'])
    const str = (k: string): string | null => {
      const m = stdout.match(new RegExp(`${k} : (.+)`))
      return m ? m[1].trim() : null
    }
    const port = (k: string): string | null => {
      const m = stdout.match(new RegExp(`${k} : (\\d+)`))
      return m ? m[1] : null
    }
    if (stdout.includes('HTTPSEnable : 1')) {
      const host = str('HTTPSProxy')
      const p = port('HTTPSPort')
      if (host && p) {
        const url = `http://${host}:${p}`
        env.https_proxy = url
        env.HTTPS_PROXY = url
      }
    }
    if (stdout.includes('HTTPEnable : 1')) {
      const host = str('HTTPProxy')
      const p = port('HTTPPort')
      if (host && p) {
        const url = `http://${host}:${p}`
        env.http_proxy = url
        env.HTTP_PROXY = url
      }
    }
    // 无 HTTP(S) 代理时退回 SOCKS
    if (!env.https_proxy && !env.http_proxy && stdout.includes('SOCKSEnable : 1')) {
      const host = str('SOCKSProxy')
      const p = port('SOCKSPort')
      if (host && p) {
        const url = `socks5://${host}:${p}`
        env.all_proxy = url
        env.ALL_PROXY = url
      }
    }
  } catch {
    /* 读不到系统代理时按直连处理 */
  }
  return env
}

/** 流式执行 git clone，输出逐段推送到渲染进程；\r 进度段作为对上一行的覆盖更新 */
async function streamClone(
  sender: Electron.WebContents,
  authedUrl: string,
  target: string
): Promise<void> {
  const proxyEnv = await getProxyEnv()
  const proxyTip = proxyEnv.https_proxy ?? proxyEnv.http_proxy ?? proxyEnv.all_proxy
  if (proxyTip && !sender.isDestroyed()) {
    sender.send('projects:cloneLog', { kind: 'line', text: `▶ 已启用系统代理 ${proxyTip}` })
  }
  try {
    await streamProcess(sender, 'projects:cloneLog', 'git', ['clone', '--progress', authedUrl, target], {
      timeoutMs: 10 * 60_000,
      extraEnv: proxyEnv
    })
  } catch (err) {
    const msg = (err as Error).message
    throw new Error(msg.startsWith('exit ') ? `git clone 失败（${msg}），详见日志` : msg)
  }
}

/** 解析 docker-compose.yml 的 services 段服务名（两空格缩进的一级 key） */
/**
 * 解析 docker-compose.yml 的 networks.default 外部网络名：
 * 兼容 `external: true` + `name: xx` 与 `external: { name: xx }` 两种写法
 */
function parseComposeNetworkName(text: string): string {
  let inNetworks = false
  let inDefault = false
  for (const raw of text.split(/\r?\n/)) {
    if (!raw.trim() || /^\s*#/.test(raw)) continue
    if (/^networks:\s*(?:#.*)?$/.test(raw)) {
      inNetworks = true
      continue
    }
    if (!inNetworks) continue
    if (/^\S/.test(raw)) break // 顶级 key：networks 段结束
    if (/^ {2}default:\s*(?:#.*)?$/.test(raw)) {
      inDefault = true
      continue
    }
    if (inDefault) {
      const m = raw.match(/^ {4,}name:\s*['"]?([^'"\s#]+)['"]?\s*(?:#.*)?$/)
      if (m) return m[1]
    }
  }
  return ''
}

/** docker network ls 精确判断网络是否已存在 */
async function dockerNetworkExists(name: string): Promise<boolean> {
  const pathEnv = await getShellPath()
  const { stdout } = await exec('docker', ['network', 'ls', '--format', '{{.Name}}'], {
    timeout: 15_000,
    env: { ...process.env, PATH: pathEnv }
  })
  return stdout
    .split(/\r?\n/)
    .map((l) => l.trim())
    .includes(name)
}

/** 解析 INIT_MYSQL_DB：每行一个 SQL 脚本路径（相对项目根） */
function parseInitMysqlDb(text: string): string[] {
  const out: string[] = []
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line || line.startsWith('#')) continue
    out.push(line)
  }
  return out
}

/** 解析 compose 的 mysql 服务配置：container_name 与 MYSQL_ROOT_PASSWORD（未显式配置时容器名回退为服务名） */
function parseComposeMysqlConfig(text: string): { container: string; password: string } {
  let inSvc = false
  let inEnv = false
  let container = ''
  let password = ''
  for (const raw of text.split(/\r?\n/)) {
    if (!raw.trim() || /^\s*#/.test(raw)) continue
    if (/^ {2}maozi-cloud-basic-mysql:\s*(?:#.*)?$/.test(raw)) {
      inSvc = true
      inEnv = false
      continue
    }
    if (!inSvc) continue
    // 下一个服务 / 顶级 key：mysql 服务块结束
    if (/^(\S| {2}\S)/.test(raw)) break
    if (/^ {4}environment:\s*(?:#.*)?$/.test(raw)) {
      inEnv = true
      continue
    }
    if (inEnv) {
      const mPwd = raw.match(/^ {6}MYSQL_ROOT_PASSWORD:\s*['"]?([^'"]*?)['"]?\s*(?:#.*)?$/)
      if (mPwd) password = mPwd[1]
    }
    const mName = raw.match(/^ {4}container_name:\s*(\S+)/)
    if (mName) container = mName[1]
  }
  return { container: container || 'maozi-cloud-basic-mysql', password }
}

/** docker exec 执行 mysql 查询（登录 shell PATH；返回 stdout） */
async function mysqlQuery(container: string, pwd: string, sql: string, timeoutMs = 20_000): Promise<string> {
  const pathEnv = await getShellPath()
  const { stdout } = await exec(
    'docker',
    ['exec', container, 'mysql', '-uroot', `-p${pwd}`, '-N', '-B', '-e', sql],
    { timeout: timeoutMs, env: { ...process.env, PATH: pathEnv } }
  )
  return stdout
}

/** 等待容器内 MySQL 可用（启动初期会拒绝连接，最多 120s） */
async function waitMysqlReady(container: string, pwd: string, send: (t: string) => void): Promise<void> {
  send('▶ 等待 MySQL 就绪（最长 120s）…')
  for (let i = 0; i < 40; i++) {
    try {
      await mysqlQuery(container, pwd, 'SELECT 1', 10_000)
      send('✓ MySQL 已就绪')
      return
    } catch {
      await new Promise((r) => setTimeout(r, 3000))
    }
  }
  throw new Error('等待 MySQL 就绪超时，请查看容器日志后重试')
}

/** mysql 服务是否处于 running 状态（compose ps） */
async function mysqlServiceRunning(dir: string): Promise<boolean> {
  const pathEnv = await getShellPath()
  const { stdout } = await exec('docker', ['compose', 'ps', '--all', '--format', 'json'], {
    timeout: 15_000,
    env: { ...process.env, PATH: pathEnv },
    cwd: dir
  })
  return parseComposePs(stdout)['maozi-cloud-basic-mysql'] === 'running'
}

function parseComposeServices(text: string): string[] {
  const services: string[] = []
  let inServices = false
  for (const raw of text.split(/\r?\n/)) {
    if (!raw.trim() || /^\s*#/.test(raw)) continue
    if (/^services:\s*(?:#.*)?$/.test(raw)) {
      inServices = true
      continue
    }
    if (!inServices) continue
    // 顶级 key：services 段结束
    if (/^\S/.test(raw)) break
    const m = raw.match(/^ {2}([A-Za-z0-9_.-]+):\s*(?:#.*)?$/)
    if (m) services.push(m[1])
  }
  return services
}

/** 解析 docker compose ps --format json 输出为 服务名→状态 映射（兼容数组/逐行两种格式） */
function parseComposePs(stdout: string): Record<string, string> {
  const states: Record<string, string> = {}
  const text = stdout.trim()
  if (!text) return states
  let arr: Array<Record<string, unknown>> = []
  try {
    arr = JSON.parse(text)
  } catch {
    arr = text
      .split('\n')
      .filter(Boolean)
      .map((l) => {
        try {
          return JSON.parse(l)
        } catch {
          return null
        }
      })
      .filter(Boolean)
  }
  if (!Array.isArray(arr)) arr = [arr]
  for (const item of arr) {
    const svc = (item.Service ?? item.service) as string | undefined
    const state = (item.State ?? item.state ?? '') as string
    if (svc) states[svc] = state
  }
  return states
}

/** docker stats 输出的人类可读容量（如 231.2MiB、1.952GiB、0B）转字节 */
function parseDockerSize(s: string): number {
  const m = s.trim().match(/^([\d.]+)\s*(B|KiB|MiB|GiB|TiB|kB|MB|GB|TB)$/)
  if (!m) return 0
  const v = parseFloat(m[1]) || 0
  const units: Record<string, number> = {
    B: 1,
    kB: 1e3,
    MB: 1e6,
    GB: 1e9,
    TB: 1e12,
    KiB: 1024,
    MiB: 1024 ** 2,
    GiB: 1024 ** 3,
    TiB: 1024 ** 4
  }
  return v * (units[m[2]] ?? 0)
}

/** docker compose ps --format json → 服务名/容器名 对（兼容数组/逐行两种格式） */
function parseComposePsPairs(stdout: string): Array<{ svc: string; name: string }> {
  const text = stdout.trim()
  if (!text) return []
  let arr: Array<Record<string, unknown>> = []
  try {
    arr = JSON.parse(text)
  } catch {
    arr = text
      .split('\n')
      .filter(Boolean)
      .map((l) => {
        try {
          return JSON.parse(l)
        } catch {
          return null
        }
      })
      .filter(Boolean)
  }
  if (!Array.isArray(arr)) arr = [arr]
  const out: Array<{ svc: string; name: string }> = []
  for (const item of arr) {
    const svc = (item.Service ?? item.service) as string | undefined
    const name = (item.Name ?? item.name) as string | undefined
    if (svc && name) out.push({ svc, name })
  }
  return out
}

/** docker stats --format 单行：Name\tCPUPerc\tMemUsage */
function parseStatsLine(
  line: string
): { name: string; cpuPercent: number; memUsed: number; memLimit: number } | null {
  const parts = line.split('\t')
  if (parts.length < 3) return null
  const name = parts[0].trim()
  if (!name) return null
  const cpuPercent = parseFloat(parts[1]) || 0
  const [used = '', limit = ''] = parts[2].split('/')
  return { name, cpuPercent, memUsed: parseDockerSize(used), memLimit: parseDockerSize(limit) }
}

/** docker compose 可用性检测（走登录 shell PATH） */
let composeOk: boolean | null = null
async function checkDockerCompose(): Promise<void> {
  if (composeOk === true) return
  if (composeOk === false) throw new Error('本机 docker / docker compose 不可用')
  try {
    const pathEnv = await getShellPath()
    await exec('docker', ['compose', 'version'], { timeout: 10_000, env: { ...process.env, PATH: pathEnv } })
    composeOk = true
  } catch {
    composeOk = false
    throw new Error('本机 docker / docker compose 不可用，请先安装并启动 Docker')
  }
}

/**
 * 构造子进程环境：以 shell 配置文件的当前状态为准——
 * 禁用的变量剔除、启用的变量注入最新值，避免应用进程启动时的陈旧环境
 * （如曾经 launchctl setenv 过的变量）继续泄漏给 docker / 部署脚本。
 * 含 $ 引用的值无法在进程环境中展开、PATH 交由调用方指定，均跳过注入。
 */
function buildChildEnv(extra?: Record<string, string>): Record<string, string> {
  const env: Record<string, string> = { ...process.env }
  try {
    const seen = new Set<string>()
    for (const v of selectPlatform().readEnvVars()) {
      if (seen.has(v.key)) continue
      seen.add(v.key)
      if (v.enabled === false) {
        delete env[v.key]
        continue
      }
      if (v.key === 'PATH' || v.value.includes('$')) continue
      env[v.key] = v.value
    }
  } catch {
    /* 非 darwin 或读取失败：退回应用进程环境 */
  }
  return { ...env, ...(extra ?? {}) }
}

/** 各会话正在执行的子进程与中断标记（sid -> 进程组），支持多个脚本并发 */
const runningProcs = new Map<string, { child: ReturnType<typeof spawn>; stopRequested: boolean }>()

/** 校验会话 id（渲染进程生成的标识） */
function validSid(sid: unknown): string {
  const s = String(sid ?? '')
  if (!/^[A-Za-z0-9_-]{1,64}$/.test(s)) throw new Error('非法的会话标识')
  return s
}

/**
 * 通用流式子进程执行：输出逐段推送到渲染进程指定频道，
 * 处理 \r 覆盖式进度行；剥离 ANSI 颜色码，凭据脱敏
 */
async function streamProcess(
  sender: Electron.WebContents,
  channel: string,
  cmd: string,
  args: string[],
  opts: { cwd?: string; timeoutMs: number; extraEnv?: Record<string, string>; sid?: string; stdinData?: string }
): Promise<void> {
  const pathEnv = await getShellPath()
  return await new Promise<void>((resolve, reject) => {
    // detached 使子进程成为进程组组长，中断/超时可整组终止（脚本会派生 mvn/java 等子进程）
    const child = spawn(cmd, args, {
      cwd: opts.cwd,
      detached: true,
      env: buildChildEnv({ PATH: pathEnv, ...(opts.extraEnv ?? {}) })
    })
    const sid = opts.sid
    if (sid) runningProcs.set(sid, { child, stopRequested: false })

    // 管道输入（如 SQL 脚本内容注入 docker exec -i mysql）；EPIPE 忽略（进程早退时正常）
    if (opts.stdinData !== undefined) {
      child.stdin.on('error', () => {})
      child.stdin.end(opts.stdinData, 'utf8')
    }

    const send = (kind: 'line' | 'update', text: string): void => {
      const clean = text
        .replace(/https:\/\/[^@\s]+@/g, 'https://***@')
        .replace(/\x1b\[[0-9;]*[A-Za-z]/g, '')
        .replace(/\s+$/, '')
      if (clean && !sender.isDestroyed()) sender.send(channel, { kind, text: clean, sid })
    }

    let carry = ''
    const feed = (chunk: Buffer): void => {
      carry += chunk.toString('utf8')
      let nl: number
      while ((nl = carry.indexOf('\n')) >= 0) {
        const seg = carry.slice(0, nl)
        carry = carry.slice(nl + 1)
        // 完整行:取最后一个 \r 段作为最终内容
        const parts = seg.split('\r')
        send('line', parts[parts.length - 1])
      }
      // 未收完整的行:按 \r 前进,最新内容覆盖上一条
      const cr = carry.lastIndexOf('\r')
      if (cr >= 0) {
        const latest = carry.slice(cr + 1)
        carry = latest
        send('update', latest)
      }
    }

    child.stdout.on('data', feed)
    child.stderr.on('data', feed)

    const killGroup = (sig: NodeJS.Signals): void => {
      try {
        if (child.pid) process.kill(-child.pid, sig)
      } catch {
        child.kill(sig)
      }
    }
    const timer = setTimeout(() => killGroup('SIGKILL'), opts.timeoutMs)
    child.on('error', (e) => {
      clearTimeout(timer)
      if (sid) runningProcs.delete(sid)
      reject(new Error(`无法执行 ${cmd}: ${e.message}`))
    })
    child.on('close', (code) => {
      clearTimeout(timer)
      const stopped = sid ? (runningProcs.get(sid)?.stopRequested ?? false) : false
      if (sid) runningProcs.delete(sid)
      if (carry.trim()) send('line', carry)
      if (stopped) reject(new Error('已手动中断'))
      else if (code === 0) resolve()
      else reject(new Error(`exit ${code}`))
    })
  })
}

/** UI 状态持久化文件：应用目录 .ui-state.json（git 已忽略，同 bookmarks.json 一类本地运行时数据） */
function uiStateFile(): string {
  return path.join(app.getAppPath(), '.ui-state.json')
}

/** 记录命令行并流式执行 docker（统一错误包装） */
async function runLogged(
  event: Electron.IpcMainInvokeEvent,
  channel: string,
  dir: string,
  args: string[],
  sid: string
): Promise<void> {
  if (!event.sender.isDestroyed()) {
    event.sender.send(channel, { kind: 'line', text: `▶ docker ${args.join(' ')}`, sid })
  }
  try {
    await streamProcess(event.sender, channel, 'docker', args, { cwd: dir, timeoutMs: 10 * 60_000, sid })
  } catch (err) {
    const msg = (err as Error).message
    throw new Error(msg.startsWith('exit ') ? `docker 操作失败（${msg}），详见日志` : msg)
  }
}

export function registerProjectHandlers(): void {
  ipcMain.handle('projects:state', () => {
    try {
      return { ok: true, data: getBinding() }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 系统目录选择框；取消返回 data: null */
  ipcMain.handle('projects:pickDir', async () => {
    try {
      const r = await dialog.showOpenDialog(focusedWindow()!, {
        title: '选择目录',
        properties: ['openDirectory', 'createDirectory']
      })
      return { ok: true, data: r.canceled ? null : (r.filePaths[0] ?? null) }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 选择目录绑定：读取 CONFIG 的 name/version */
  ipcMain.handle('projects:bindDir', (_e, dir: string) => {
    try {
      const target = String(dir ?? '').trim()
      if (!target || !fs.existsSync(target)) throw new Error('目录不存在')
      const { name, version } = parseConfigFile(target)
      const binding: ProjectBinding = { name, version, path: target, boundAt: Date.now() }
      saveBinding(binding)
      return { ok: true, data: binding }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /**
   * 拉取代码绑定：密钥(账密→user:pass / 密钥→token) + 目标系统目录
   * 克隆到 <destDir>/maozi-cloud，成功后读 CONFIG 完成绑定。
   * git 进度日志实时推送到渲染进程（projects:cloneLog），
   * 处理 \r 覆盖式进度行（Receiving objects: xx%），凭据全程脱敏
   */
  ipcMain.handle('projects:clone', async (event, secretId: string, destDir: string) => {
    try {
      await ensureGit()
      const secret = listConfigs().find((c) => c.id === secretId)
      if (!secret) throw new Error('密钥不存在')
      if (secret.type !== 'Git') throw new Error('仅支持选择 Git 类型的密钥')
      if (!secret.password) throw new Error('所选密钥未填写密码 / Token')

      const auth =
        (secret.authType ?? 'password') === 'key'
          ? encodeURIComponent(secret.password)
          : `${encodeURIComponent(secret.username)}:${encodeURIComponent(secret.password)}`

      const dest = String(destDir ?? '').trim()
      if (!dest || !fs.existsSync(dest)) throw new Error('目标目录不存在')
      const target = path.join(dest, CLONE_DIR)
      if (fs.existsSync(target)) throw new Error(`目标位置已存在同名目录：${target}`)

      const authedUrl = REPO_URL.replace('https://', `https://${auth}@`)
      await streamClone(event.sender, authedUrl, target)

      const { name, version } = parseConfigFile(target)
      const binding: ProjectBinding = { name, version, path: target, boundAt: Date.now() }
      saveBinding(binding)
      return { ok: true, data: binding }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('projects:unbind', () => {
    try {
      clearBinding()
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 打开已绑定项目的本地目录 */
  ipcMain.handle('projects:openDir', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const errMsg = await shell.openPath(b.path)
      if (errMsg) throw new Error(errMsg)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 执行项目内脚本（all=微服务全量启动 / admin=后台启动），日志实时推送 projects:scriptLog */
  ipcMain.handle('projects:runScript', async (event, key: string, sidArg?: string) => {
    try {
      const def = DEPLOY_SCRIPTS[key]
      if (!def) throw new Error(`未知脚本：${key}`)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const sid = validSid(sidArg)
      const script = path.join(b.path, def.file)
      if (!fs.existsSync(script)) throw new Error(`未找到脚本：${def.file}`)
      const proxyEnv = await getProxyEnv()
      const proxyTip = proxyEnv.https_proxy ?? proxyEnv.http_proxy ?? proxyEnv.all_proxy
      if (proxyTip && !event.sender.isDestroyed()) {
        event.sender.send('projects:scriptLog', {
          kind: 'line',
          text: `▶ 已启用系统代理 ${proxyTip}`,
          sid
        })
      }
      event.sender.send('projects:scriptLog', {
        kind: 'line',
        text: `▶ [${def.label}] 执行 ${def.file}`,
        sid
      })
      try {
        await streamProcess(event.sender, 'projects:scriptLog', 'bash', [script], {
          cwd: b.path,
          timeoutMs: 30 * 60_000,
          extraEnv: proxyEnv,
          sid
        })
      } catch (err) {
        const msg = (err as Error).message
        throw new Error(msg.startsWith('exit ') ? `脚本执行失败（${msg}），详见日志` : msg)
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 解析基础服务 docker-compose.yml 的服务列表 */
  ipcMain.handle('projects:composeServices', () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const yml = path.join(b.path, COMPOSE_DIR, 'docker-compose.yml')
      if (!fs.existsSync(yml)) throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      return { ok: true, data: parseComposeServices(fs.readFileSync(yml, 'utf8')) }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: [] }
    }
  })

  /**
   * 基础服务启动/停止/重启：up -d <svc> / rm -sf <svc> / down，
   * 重启 = 先 rm -sf 移除容器再 up -d 重建，日志走 projects:scriptLog
   */
  ipcMain.handle('projects:composeAction', async (event, service: string, action: string, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (!/^[A-Za-z0-9_.-]+$/.test(String(service))) throw new Error('非法的服务名')
      if (action !== 'start' && action !== 'stop' && action !== 'restart') throw new Error('非法的操作')
      await checkDockerCompose()
      const dir = path.join(b.path, COMPOSE_DIR)
      if (!fs.existsSync(path.join(dir, 'docker-compose.yml'))) {
        throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      }
      const isAll = service === 'all'
      const cmdList: string[][] = isAll
        ? action === 'start'
          ? [['compose', 'up', '-d']]
          : action === 'stop'
            ? [['compose', 'down']]
            : [['compose', 'down'], ['compose', 'up', '-d']]
        : action === 'start'
          ? [['compose', 'up', '-d', service]]
          : action === 'stop'
            ? [['compose', 'rm', '--stop', '--force', service]]
            : [['compose', 'rm', '--stop', '--force', service], ['compose', 'up', '-d', service]]
      for (const args of cmdList) {
        if (!event.sender.isDestroyed()) {
          event.sender.send('projects:scriptLog', {
            kind: 'line',
            text: `▶ docker ${args.join(' ')}`,
            sid
          })
        }
        try {
          await streamProcess(event.sender, 'projects:scriptLog', 'docker', args, {
            cwd: dir,
            timeoutMs: 10 * 60_000,
            sid
          })
        } catch (err) {
          const msg = (err as Error).message
          throw new Error(msg.startsWith('exit ') ? `docker 操作失败（${msg}），详见日志` : msg)
        }
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 查询基础服务运行状态：服务名 → State(running/exited/...) */
  ipcMain.handle('projects:composeStatus', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const dir = path.join(b.path, COMPOSE_DIR)
      if (!fs.existsSync(path.join(dir, 'docker-compose.yml'))) {
        throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      }
      await checkDockerCompose()
      const pathEnv = await getShellPath()
      const { stdout } = await exec(
        'docker',
        ['compose', 'ps', '--all', '--format', 'json'],
        { timeout: 15_000, env: { ...process.env, PATH: pathEnv }, cwd: dir }
      )
      return { ok: true, data: parseComposePs(stdout) }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: {} as Record<string, string> }
    }
  })

  /** 查询后台前端容器状态：none（未创建）/ running / exited / unknown */
  ipcMain.handle('projects:adminContainerStatus', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      await checkDockerCompose()
      const pathEnv = await getShellPath()
      const { stdout } = await exec(
        'docker',
        ['ps', '-a', '--filter', `name=^/${ADMIN_CONTAINER}$`, '--format', '{{.State}}'],
        { timeout: 15_000, env: { ...process.env, PATH: pathEnv } }
      )
      const state = stdout.trim().split('\n')[0]?.trim() || 'none'
      return { ok: true, data: state }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: 'unknown' }
    }
  })

  /** 后台前端容器启动/关闭：docker compose up -d / down（移除容器），日志走 projects:scriptLog */
  ipcMain.handle('projects:adminContainerAction', async (event, action: string, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (action !== 'start' && action !== 'stop') throw new Error('非法的操作')
      await checkDockerCompose()
      const dir = path.join(b.path, ADMIN_COMPOSE_DIR)
      const composeFile = path.join(dir, ADMIN_COMPOSE_FILE)
      if (!fs.existsSync(composeFile)) {
        throw new Error(`未找到 ${ADMIN_COMPOSE_DIR}/${ADMIN_COMPOSE_FILE}`)
      }
      const args =
        action === 'start'
          ? ['compose', '-f', composeFile, 'up', '-d']
          : ['compose', '-f', composeFile, 'down']
      if (!event.sender.isDestroyed()) {
        event.sender.send('projects:scriptLog', {
          kind: 'line',
          text: `▶ docker ${args.join(' ')}`,
          sid
        })
      }
      try {
        await streamProcess(event.sender, 'projects:scriptLog', 'docker', args, {
          cwd: dir,
          timeoutMs: 10 * 60_000,
          sid
        })
      } catch (err) {
        const msg = (err as Error).message
        throw new Error(msg.startsWith('exit ') ? `docker 操作失败（${msg}），详见日志` : msg)
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 初始化 Hosts 状态：项目 hosts 文件中尚未写入系统 /etc/hosts 的映射数量 */
  ipcMain.handle('projects:hostsInitStatus', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const file = path.join(b.path, HOSTS_FILE)
      if (!fs.existsSync(file)) throw new Error(`未找到 ${HOSTS_FILE}`)
      const platform = selectPlatform()
      const defs = parseProjectHosts(fs.readFileSync(file, 'utf8'))
      const existing = new Set(
        platform
          .readHosts()
          .filter((e) => e.kind === 'entry')
          .map((e) => `${e.ip} ${e.domains}`)
      )
      const missing = defs.filter((d) => !existing.has(`${d.ip} ${d.domains}`))
      return {
        ok: true,
        data: { total: defs.length, missing: missing.length, initialized: missing.length === 0 }
      }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /** 容器网络状态：解析 compose 的 networks.default 外部网络名，检查 docker 中是否已存在 */
  ipcMain.handle('projects:networkStatus', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const yml = path.join(b.path, COMPOSE_DIR, 'docker-compose.yml')
      if (!fs.existsSync(yml)) throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      const name = parseComposeNetworkName(fs.readFileSync(yml, 'utf8'))
      if (!name) return { ok: true, data: { name: '', exists: false } }
      await checkDockerCompose()
      return { ok: true, data: { name, exists: await dockerNetworkExists(name) } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { name: '', exists: false } }
    }
  })

  /** 创建容器网络：docker network create <name>（幂等：已存在直接成功），日志走 projects:scriptLog */
  ipcMain.handle('projects:networkCreate', async (event, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const yml = path.join(b.path, COMPOSE_DIR, 'docker-compose.yml')
      if (!fs.existsSync(yml)) throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      const name = parseComposeNetworkName(fs.readFileSync(yml, 'utf8'))
      if (!name) throw new Error('docker-compose.yml 未定义 networks.default.external.name')
      await checkDockerCompose()
      const send = (text: string): void => {
        if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
      }
      if (await dockerNetworkExists(name)) {
        send(`✓ 容器网络 ${name} 已存在，无需创建`)
        return { ok: true }
      }
      send(`▶ docker network create ${name}`)
      try {
        await streamProcess(event.sender, 'projects:scriptLog', 'docker', ['network', 'create', name], {
          cwd: path.join(b.path, COMPOSE_DIR),
          timeoutMs: 60_000,
          sid
        })
      } catch (err) {
        const msg = (err as Error).message
        throw new Error(msg.startsWith('exit ') ? `docker network create 失败（${msg}），详见日志` : msg)
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 初始化 Hosts：把缺失映射追加进系统 /etc/hosts（已设置的忽略），日志走 projects:scriptLog */
  ipcMain.handle('projects:hostsInit', async (event, sidArg?: string) => {
    const sid = validSid(sidArg)
    const send = (text: string): void => {
      if (!event.sender.isDestroyed()) {
        event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
      }
    }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const file = path.join(b.path, HOSTS_FILE)
      if (!fs.existsSync(file)) throw new Error(`未找到 ${HOSTS_FILE}`)
      const platform = selectPlatform()
      send(`▶ 读取 ${HOSTS_FILE}`)
      const defs = parseProjectHosts(fs.readFileSync(file, 'utf8'))
      send(`  共 ${defs.length} 条映射`)
      const sysEntries: HostsEntry[] = platform.readHosts()
      const existing = new Set(
        sysEntries.filter((e) => e.kind === 'entry').map((e) => `${e.ip} ${e.domains}`)
      )
      const missing = defs.filter((d) => !existing.has(`${d.ip} ${d.domains}`))
      for (const d of defs) {
        send(
          existing.has(`${d.ip} ${d.domains}`)
            ? `✓ ${d.ip.padEnd(15)} ${d.domains}  已存在，忽略`
            : `＋ ${d.ip.padEnd(15)} ${d.domains}  待追加`
        )
      }
      if (missing.length === 0) {
        send('✓ 全部映射均已写入系统 hosts，无需初始化')
        return { ok: true }
      }
      const appended: HostsEntry[] = missing.map((d, i) => ({
        id: `init-${Date.now()}-${i}`,
        kind: 'entry',
        ip: d.ip,
        domains: d.domains,
        enabled: true
      }))
      send(`▶ 写入 ${platform.hostsPath()}（需要管理员授权，原文件自动备份）`)
      await platform.writeHosts([...sysEntries, ...appended])
      send('✓ hosts 已写入，正在刷新 DNS 缓存…')
      await platform.flushDns()
      send('✓ 初始化完成')
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** UI 状态（Tab 选中 等）：应用目录 .ui-state.json 持久化，git 已忽略 */
  ipcMain.handle('projects:uiStateGet', () => {
    try {
      return { ok: true, data: JSON.parse(fs.readFileSync(uiStateFile(), 'utf8')) }
    } catch {
      return { ok: true, data: {} }
    }
  })

  ipcMain.handle('projects:uiStateSave', (_e, patch: Record<string, unknown>) => {
    try {
      let cur: Record<string, unknown> = {}
      try {
        cur = JSON.parse(fs.readFileSync(uiStateFile(), 'utf8'))
      } catch {
        /* 首次创建 */
      }
      fs.writeFileSync(uiStateFile(), JSON.stringify({ ...cur, ...(patch ?? {}) }, null, 2), 'utf8')
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 应用服务清单（单体/微服务）：读取变体两个 compose 文件的服务定义 */
  ipcMain.handle('projects:appServices', (_e, variantArg: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      const services = readAppServices(b.path, variantArg)
      return { ok: true, data: { services } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { services: [] } }
    }
  })

  /** 应用服务运行状态：变体两个 compose 文件 ps 合并（服务名 → State） */
  ipcMain.handle('projects:appServicesStatus', async (_e, variantArg: string) => {
    const empty: Record<string, string> = {}
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      await checkDockerCompose()
      const def = APP_SVC_DEFS[variantArg as AppSvcVariant]
      const pathEnv = await getShellPath()
      const env = { ...process.env, PATH: pathEnv }
      const dir = path.join(b.path, def.dir)
      const states: Record<string, string> = {}
      for (const fileName of [def.servicesFile, def.adminFile]) {
        const yml = path.join(dir, fileName)
        if (!fs.existsSync(yml)) continue
        const { stdout } = await exec('docker', ['compose', '-f', fileName, 'ps', '--all', '--format', 'json'], {
          timeout: 15_000,
          env,
          cwd: dir
        })
        Object.assign(states, parseComposePs(stdout))
      }
      return { ok: true, data: states }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: empty }
    }
  })

  /**
   * 应用服务实时资源占用（单体+微服务一次性快照）：各变体 compose ps 取服务→容器映射，
   * 汇总后单次 docker stats + inspect（与基础服务 composeStats 同源逻辑）
   */
  ipcMain.handle('projects:appServicesStats', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      await checkDockerCompose()
      const pathEnv = await getShellPath()
      const env = { ...process.env, PATH: pathEnv }
      const result: Record<'monomer' | 'distributeds', Record<string, ComposeServiceStats>> = {
        monomer: {},
        distributeds: {}
      }
      const pairs: Array<{ variant: AppSvcVariant; svc: string; name: string }> = []
      for (const variant of ['monomer', 'distributeds'] as AppSvcVariant[]) {
        const def = APP_SVC_DEFS[variant]
        const dir = path.join(b.path, def.dir)
        for (const fileName of [def.servicesFile, def.adminFile]) {
          if (!fs.existsSync(path.join(dir, fileName))) continue
          const { stdout } = await exec('docker', ['compose', '-f', fileName, 'ps', '--format', 'json'], {
            timeout: 15_000,
            env,
            cwd: dir
          })
          for (const p of parseComposePsPairs(stdout)) pairs.push({ variant, svc: p.svc, name: p.name })
        }
      }
      if (pairs.length > 0) {
        const names = pairs.map((p) => p.name)
        const [statsRes, inspRes] = await Promise.all([
          exec('docker', ['stats', '--no-stream', '--format', '{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}'], {
            timeout: 15_000,
            env
          }),
          exec(
            'docker',
            ['inspect', '--format', '{{.Name}} {{.HostConfig.Memory}} {{.HostConfig.NanoCpus}}', ...names],
            { timeout: 15_000, env }
          ).catch(() => null)
        ])
        const byName = new Map<string, ReturnType<typeof parseStatsLine>>()
        for (const line of statsRes.stdout.split('\n')) {
          const e = parseStatsLine(line)
          if (e) byName.set(e.name, e)
        }
        const limitByName = new Map<string, { mem: number; cpus: number }>()
        if (inspRes) {
          for (const line of inspRes.stdout.trim().split('\n')) {
            if (!line.trim()) continue
            const [rawName, memStr, cpusStr] = line.trim().split(' ')
            limitByName.set(rawName.replace(/^\//, ''), {
              mem: parseInt(memStr ?? '0', 10) || 0,
              cpus: (parseInt(cpusStr ?? '0', 10) || 0) / 1e9
            })
          }
        }
        for (const p of pairs) {
          const e = byName.get(p.name)
          if (!e) continue
          const cgroup = limitByName.get(p.name)
          result[p.variant][p.svc] = {
            cpuPercent: e.cpuPercent,
            memUsed: e.memUsed,
            memLimit: cgroup?.mem ?? 0,
            cpusLimit: +(cgroup?.cpus ?? 0).toFixed(2)
          }
        }
      }
      return { ok: true, data: { stats: result, cpuCount: os.cpus().length, hostMemTotal: os.totalmem() } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /**
   * 应用服务互斥停机：down 掉目标变体的全部服务（两个 compose 文件逐个检查，无运行服务则跳过）。
   * 日志走 projects:scriptLog
   */
  async function appServicesDown(
    event: Electron.IpcMainInvokeEvent,
    bPath: string,
    variant: AppSvcVariant,
    reason: string,
    sid: string
  ): Promise<void> {
    const def = APP_SVC_DEFS[variant]
    const dir = path.join(bPath, def.dir)
    const pathEnv = await getShellPath()
    const env = { ...process.env, PATH: pathEnv }
    const send = (text: string): void => {
      if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
    }
    send(`▶ 互斥检查：${reason}需先关闭${def.label}全部服务`)
    for (const fileName of [def.servicesFile, def.adminFile]) {
      const yml = path.join(dir, fileName)
      if (!fs.existsSync(yml)) continue
      const { stdout } = await exec('docker', ['compose', '-f', fileName, 'ps', '--format', 'json'], {
        timeout: 15_000,
        env,
        cwd: dir
      })
      const running = Object.values(parseComposePs(stdout)).filter((s) => s === 'running').length
      if (running === 0) {
        send(`✓ ${fileName} 无运行服务，跳过`)
        continue
      }
      send(`▶ docker compose -f ${fileName} down（${running} 个运行中）`)
      try {
        await streamProcess(event.sender, 'projects:scriptLog', 'docker', ['compose', '-f', fileName, 'down'], {
          cwd: dir,
          timeoutMs: 10 * 60_000,
          sid
        })
      } catch (err) {
        const msg = (err as Error).message
        throw new Error(`关闭${def.label}服务失败（${msg}），详见日志`)
      }
    }
  }

  /**
   * 应用服务操作（单体/微服务）：start/stop/restart 单个服务。
   * start/restart 前强制互斥 —— 先 down 掉另一变体全部服务。日志走 projects:scriptLog
   */
  ipcMain.handle(
    'projects:appServiceAction',
    async (event, variantArg: string, fileTag: string, service: string, action: string, sidArg?: string) => {
      try {
        const sid = validSid(sidArg)
        const b = getBinding()
        if (!b) throw new Error('尚未绑定项目')
        if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
        if (fileTag !== 'admin' && fileTag !== 'services') throw new Error('非法的文件标识')
        if (!/^[A-Za-z0-9_.-]+$/.test(String(service))) throw new Error('非法的服务名')
        if (action !== 'start' && action !== 'stop' && action !== 'restart') throw new Error('非法的操作')
        await checkDockerCompose()
        const variant = variantArg as AppSvcVariant
        const def = APP_SVC_DEFS[variant]
        const dir = path.join(b.path, def.dir)
        const fileName = fileTag === 'admin' ? def.adminFile : def.servicesFile
        if (!fs.existsSync(path.join(dir, fileName))) throw new Error(`未找到 ${def.dir}/${fileName}`)
        const send = (text: string): void => {
          if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
        }

        // 互斥：启动/重启前关闭另一变体全部服务
        if (action !== 'stop') {
          const other: AppSvcVariant = variant === 'monomer' ? 'distributeds' : 'monomer'
          await appServicesDown(event, b.path, other, `启动${def.label}服务 ${service}`, sid)
        }

        const cmdList: string[][] =
          action === 'start'
            ? [['compose', '-f', fileName, 'up', '-d', service]]
            : action === 'stop'
              ? [['compose', '-f', fileName, 'rm', '--stop', '--force', service]]
              : [
                  ['compose', '-f', fileName, 'rm', '--stop', '--force', service],
                  ['compose', '-f', fileName, 'up', '-d', service]
                ]
        for (const args of cmdList) {
          send(`▶ docker ${args.join(' ')}`)
          try {
            await streamProcess(event.sender, 'projects:scriptLog', 'docker', args, {
              cwd: dir,
              timeoutMs: 10 * 60_000,
              sid
            })
          } catch (err) {
            const msg = (err as Error).message
            throw new Error(msg.startsWith('exit ') ? `docker 操作失败（${msg}），详见日志` : msg)
          }
        }
        return { ok: true }
      } catch (err) {
        return { ok: false, error: (err as Error).message }
      }
    }
  )

  /**
   * 应用服务全量操作（单体/微服务）：start（先互斥关闭另一变体，再 up 两个文件）/ stop（down 两个文件）。
   * 日志走 projects:scriptLog
   */
  ipcMain.handle('projects:appServiceAll', async (event, variantArg: string, action: string, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      if (action !== 'start' && action !== 'stop') throw new Error('非法的操作')
      await checkDockerCompose()
      const variant = variantArg as AppSvcVariant
      const def = APP_SVC_DEFS[variant]
      const dir = path.join(b.path, def.dir)
      for (const fileName of [def.servicesFile, def.adminFile]) {
        if (!fs.existsSync(path.join(dir, fileName))) throw new Error(`未找到 ${def.dir}/${fileName}`)
      }

      if (action === 'start') {
        const other: AppSvcVariant = variant === 'monomer' ? 'distributeds' : 'monomer'
        await appServicesDown(event, b.path, other, `启动${def.label}全部服务`, sid)
        for (const fileName of [def.servicesFile, def.adminFile]) {
          await runLogged(event, 'projects:scriptLog', dir, ['compose', '-f', fileName, 'up', '-d'], sid)
        }
      } else {
        await appServicesDown(event, b.path, variant, '', sid)
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 数据库初始化状态：.db-init.json 标记判定（maozi-cloud-develop-admin 应用目录，git 已忽略）；INIT_MYSQL_DB（每行一个脚本路径）不存在或为空时隐藏按钮 */
  ipcMain.handle('projects:dbInitStatus', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const file = path.join(b.path, INIT_MYSQL_DB_FILE)
      if (!fs.existsSync(file)) return { ok: true, data: { count: 0, initialized: false, missing: 0 } }
      const scripts = parseInitMysqlDb(fs.readFileSync(file, 'utf8'))
      if (scripts.length === 0) return { ok: true, data: { count: 0, initialized: false, missing: 0 } }
      const initialized = isDbInitialized(b.path)
      return { ok: true, data: { count: scripts.length, initialized, missing: initialized ? 0 : scripts.length } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { count: 0, initialized: false, missing: 0 } }
    }
  })

  /**
   * 初始化数据库：按 INIT_MYSQL_DB 定义（每行一个脚本路径）逐个导入 SQL 到 mysql。
   * mysql 未运行时先 up -d 启动并等待就绪；初始化结束后仅回收本次启动的容器，
   * 点击前已在运行的容器保持原状（不 stop/rm）。日志走 projects:scriptLog，密码全程打码
   */
  ipcMain.handle('projects:dbInit', async (event, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const file = path.join(b.path, INIT_MYSQL_DB_FILE)
      if (!fs.existsSync(file)) throw new Error(`未找到 ${INIT_MYSQL_DB_FILE}`)
      const scripts = parseInitMysqlDb(fs.readFileSync(file, 'utf8'))
      if (scripts.length === 0) throw new Error(`${INIT_MYSQL_DB_FILE} 中没有定义初始化脚本`)
      const dir = path.join(b.path, COMPOSE_DIR)
      const yml = path.join(dir, 'docker-compose.yml')
      if (!fs.existsSync(yml)) throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      await checkDockerCompose()
      const { container, password } = parseComposeMysqlConfig(fs.readFileSync(yml, 'utf8'))
      if (!password) throw new Error('docker-compose.yml 未配置 MYSQL_ROOT_PASSWORD')
      const send = (text: string): void => {
        if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
      }
      send(`▶ 读取 ${INIT_MYSQL_DB_FILE}：共 ${scripts.length} 个脚本`)

      // 记录点击前是否已运行：决定初始化结束后是否回收容器
      const wasRunning = await mysqlServiceRunning(dir)
      if (!wasRunning) {
        send('▶ docker compose up -d maozi-cloud-basic-mysql')
        try {
          await streamProcess(event.sender, 'projects:scriptLog', 'docker', [
            'compose', 'up', '-d', 'maozi-cloud-basic-mysql'
          ], { cwd: dir, timeoutMs: 10 * 60_000, sid })
        } catch (err) {
          const msg = (err as Error).message
          throw new Error(msg.startsWith('exit ') ? `mysql 启动失败（${msg}），详见日志` : msg)
        }
      } else {
        send('✓ maozi-cloud-basic-mysql 已在运行，跳过启动')
      }

      await waitMysqlReady(container, password, send)

      for (const script of scripts) {
        const scriptAbs = path.isAbsolute(script) ? script : path.join(b.path, script)
        if (!fs.existsSync(scriptAbs)) throw new Error(`未找到初始化脚本：${script}`)
        send(`▶ 导入脚本 ${script}`)
        try {
          await streamProcess(
            event.sender,
            'projects:scriptLog',
            'docker',
            ['exec', '-i', container, 'mysql', '-uroot', `-p${password}`, '--default-character-set=utf8mb4'],
            { cwd: dir, timeoutMs: 5 * 60_000, sid, stdinData: fs.readFileSync(scriptAbs, 'utf8') }
          )
        } catch (err) {
          const msg = (err as Error).message
          throw new Error(`导入 ${script} 失败（${msg}），详见日志`)
        }
        send(`✓ 脚本导入完成`)
      }
      send(`✓ 初始化完成：共导入 ${scripts.length} 个脚本`)

      // 导入已全部完成即写入初始化标记（maozi-cloud-develop-admin/.db-init.json，git 已忽略）；
      // 放在容器回收之前：回收失败不影响初始化成功的记录
      markDbInitialized(b.path)
      send('✓ 已记录初始化标记 .db-init.json（maozi-cloud-develop-admin 目录，不参与 git）')

      // 仅回收本次启动的容器；点击前已运行的保持原状
      if (!wasRunning) {
        send('▶ docker compose rm --stop --force maozi-cloud-basic-mysql（回收本次启动的容器）')
        try {
          await streamProcess(event.sender, 'projects:scriptLog', 'docker', [
            'compose', 'rm', '--stop', '--force', 'maozi-cloud-basic-mysql'
          ], { cwd: dir, timeoutMs: 10 * 60_000, sid })
        } catch (err) {
          const msg = (err as Error).message
          send(`⚠ 容器回收失败（${msg}），初始化结果已记录，可手动回收容器`)
        }
      } else {
        send('✓ maozi-cloud-basic-mysql 点击前已运行，保持运行不回收')
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 基础服务日志查询（一次性）：docker compose logs --tail N --no-color <svc>（容器已删除时无法读取） */
  ipcMain.handle('projects:composeLogs', async (_e, service: string, tailArg?: number, ctxArg?: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (!/^[A-Za-z0-9_.-]+$/.test(String(service))) throw new Error('非法的服务名')
      const tail = Math.min(Math.max(parseInt(String(tailArg ?? 200), 10) || 200, 1), 5000)
      await checkDockerCompose()
      // 上下文：basics（默认）或应用服务 '<variant>:<admin|services>'（-f 指定 compose 文件）
      const ctx = ctxArg ? resolveComposeCtx(ctxArg) : null
      const dir = ctx ? path.join(b.path, ctx.dir) : path.join(b.path, COMPOSE_DIR)
      const fileArgs = ctx ? ['-f', ctx.file] : []
      if (!fs.existsSync(ctx ? path.join(dir, ctx.file) : path.join(dir, 'docker-compose.yml'))) {
        throw new Error(ctx ? `未找到 ${ctx.dir}/${ctx.file}` : `未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      }
      const pathEnv = await getShellPath()
      const { stdout } = await exec(
        'docker',
        ['compose', ...fileArgs, 'logs', '--no-color', '--tail', String(tail), service],
        { timeout: 30_000, env: { ...process.env, PATH: pathEnv }, cwd: dir, maxBuffer: 64 * 1024 * 1024 }
      )
      const lines = stdout.split(/\r?\n/).filter((l) => l.trim() !== '')
      if (lines.length === 0) throw new Error('服务暂无日志输出')
      return { ok: true, data: lines }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: [] }
    }
  })

  /**
   * 基础服务日志实时跟踪：docker compose logs -f --tail N --no-color <svc>，
   * 输出经 projects:composeLog 推送（带 sid），停止跟踪复用 projects:stopScript（进程组中断）。
   * 服务未运行时打印完已有日志后正常退出
   */
  ipcMain.handle(
    'projects:composeLogsFollow',
    async (event, service: string, tailArg?: number, sidArg?: string, ctxArg?: string) => {
      try {
        const sid = validSid(sidArg)
        const b = getBinding()
        if (!b) throw new Error('尚未绑定项目')
        if (!/^[A-Za-z0-9_.-]+$/.test(String(service))) throw new Error('非法的服务名')
        const tail = Math.min(Math.max(parseInt(String(tailArg ?? 200), 10) || 200, 1), 5000)
        await checkDockerCompose()
        const ctx = ctxArg ? resolveComposeCtx(ctxArg) : null
        const dir = ctx ? path.join(b.path, ctx.dir) : path.join(b.path, COMPOSE_DIR)
        const fileArgs = ctx ? ['-f', ctx.file] : []
        if (!fs.existsSync(ctx ? path.join(dir, ctx.file) : path.join(dir, 'docker-compose.yml'))) {
          throw new Error(ctx ? `未找到 ${ctx.dir}/${ctx.file}` : `未找到 ${COMPOSE_DIR}/docker-compose.yml`)
        }
        try {
          await streamProcess(event.sender, 'projects:composeLog', 'docker', [
            'compose',
            ...fileArgs,
            'logs',
            '--no-color',
            '--tail',
            String(tail),
            '--follow',
            service
          ], {
            cwd: dir,
            timeoutMs: 24 * 60 * 60_000,
            sid
          })
        } catch (err) {
          const msg = (err as Error).message
          // 用户主动停止跟踪属正常结束
          if (msg === '已手动中断') return { ok: true }
          throw new Error(msg)
        }
        return { ok: true }
      } catch (err) {
        return { ok: false, error: (err as Error).message }
      }
    }
  )

  /** 基础服务实时资源占用：compose ps 取服务→容器名映射，docker stats 一次性快照按容器名匹配 */
  ipcMain.handle('projects:composeStats', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const dir = path.join(b.path, COMPOSE_DIR)
      if (!fs.existsSync(path.join(dir, 'docker-compose.yml'))) {
        throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      }
      await checkDockerCompose()
      const pathEnv = await getShellPath()
      const env = { ...process.env, PATH: pathEnv }
      const { stdout: psOut } = await exec('docker', ['compose', 'ps', '--format', 'json'], {
        timeout: 15_000,
        env,
        cwd: dir
      })
      // 服务名 -> 容器名（compose ps 默认只列运行中的容器）
      const nameByService = new Map<string, string>()
      const text = psOut.trim()
      if (text) {
        let arr: Array<Record<string, unknown>> = []
        try {
          arr = JSON.parse(text)
        } catch {
          arr = text
            .split('\n')
            .filter(Boolean)
            .map((l) => {
              try {
                return JSON.parse(l) as Record<string, unknown>
              } catch {
                return null
              }
            })
            .filter((x): x is Record<string, unknown> => !!x)
        }
        if (!Array.isArray(arr)) arr = [arr]
        for (const item of arr) {
          const svc = (item.Service ?? item.service) as string | undefined
          const name = (item.Name ?? item.name) as string | undefined
          if (svc && name) nameByService.set(svc, name)
        }
      }
      const stats: Record<string, ComposeServiceStats> = {}
      if (nameByService.size > 0) {
        // stats 与 inspect 只依赖容器名，并行执行缩短首次数据延迟（stats 自身有 ~1s 采样窗口）
        const [statsRes, inspRes] = await Promise.all([
          exec('docker', ['stats', '--no-stream', '--format', '{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}'], {
            timeout: 15_000,
            env
          }),
          exec(
            'docker',
            ['inspect', '--format', '{{.Name}} {{.HostConfig.Memory}} {{.HostConfig.NanoCpus}}', ...nameByService.values()],
            { timeout: 15_000, env }
          ).catch(() => null)
        ])
        const byName = new Map<string, ReturnType<typeof parseStatsLine>>()
        for (const line of statsRes.stdout.split('\n')) {
          const e = parseStatsLine(line)
          if (e) byName.set(e.name, e)
        }
        // 真实资源配额：Memory 为 0 表示内存未限制；NanoCpus 折算 CPU 核数，0 表示未限制
        const limitByName = new Map<string, { mem: number; cpus: number }>()
        if (inspRes) {
          for (const line of inspRes.stdout.trim().split('\n')) {
            if (!line.trim()) continue
            const [rawName, memStr, cpusStr] = line.trim().split(' ')
            limitByName.set(rawName.replace(/^\//, ''), {
              mem: parseInt(memStr ?? '0', 10) || 0,
              cpus: (parseInt(cpusStr ?? '0', 10) || 0) / 1e9
            })
          }
        }
        for (const [svc, name] of nameByService) {
          const e = byName.get(name)
          if (!e) continue
          const cgroup = limitByName.get(name)
          stats[svc] = {
            cpuPercent: e.cpuPercent,
            memUsed: e.memUsed,
            memLimit: cgroup?.mem ?? 0,
            cpusLimit: +(cgroup?.cpus ?? 0).toFixed(2)
          }
        }
      }
      return { ok: true, data: { stats, cpuCount: os.cpus().length, hostMemTotal: os.totalmem() } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /** 环境设置：解析项目 ENVIRONMENT_VARIABLE，并实时读取各环境变量当前值 */
  ipcMain.handle('projects:envSettings', () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const file = path.join(b.path, ENV_VARS_FILE)
      if (!fs.existsSync(file)) throw new Error(`未找到 ${ENV_VARS_FILE}`)
      const groups = parseEnvVarDefs(fs.readFileSync(file, 'utf8'))
      const platform = selectPlatform()
      const files = platform.listEnvFiles()
      applyEnvValues(groups, platform.readEnvVars(), files)
      // 新变量默认写入的配置文件：优先实际存在且最靠前的（zshrc 最前）
      const defaultFile = files.find((f) => f.exists) ?? files[0]
      return { ok: true, data: { groups, files, defaultFileId: defaultFile.id } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /** 中断指定会话正在执行的脚本/命令：向进程组发 SIGINT，1.5s 后仍存活则 SIGKILL */
  ipcMain.handle('projects:stopScript', (_e, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const entry = runningProcs.get(sid)
      if (entry?.child.pid) {
        entry.stopRequested = true
        try {
          process.kill(-entry.child.pid, 'SIGINT')
        } catch {
          entry.child.kill('SIGINT')
        }
        const pid = entry.child.pid
        setTimeout(() => {
          try {
            process.kill(-pid, 'SIGKILL')
          } catch {
            /* 进程已退出 */
          }
        }, 1500)
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })
}
