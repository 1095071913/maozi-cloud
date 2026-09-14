import { execFile } from 'node:child_process'
import os from 'node:os'
import fs from 'node:fs'
import { promisify } from 'node:util'
import { ipcMain } from 'electron'

const exec = promisify(execFile)

/**
 * 系统信息服务：硬件识别、实时资源监控、内外网 IP
 * os.* / statfs 天然跨平台；vm_stat / df / system_profiler 为 darwin 实现，
 * Windows 扩展时按 process.platform 分支替换内存与磁盘采集即可
 */

// ---------- 类型 ----------

export interface GpuInfo {
  name: string
  chipsetModel: string
  cores?: number
  metal?: string
  vram?: number
  bus?: string
  displays: string[]
}

export interface SysStaticInfo {
  hostname: string
  computerName: string
  modelName: string
  modelIdentifier: string
  chip: string
  cpuModel: string
  cpuLogicalCores: number
  cpuPhysicalCores: number
  memoryTotal: number
  osName: string
  osVersion: string
  kernel: string
  arch: string
  gpus: GpuInfo[]
}

export interface CpuDynamic {
  usage: number
  cores: number[]
}

export interface MemDynamic {
  total: number
  used: number
  available: number
  usage: number
}

export interface DiskDynamic {
  mount: string
  total: number
  free: number
  used: number
  usage: number
}

export interface SysDynamicInfo {
  cpu: CpuDynamic
  mem: MemDynamic
  disks: DiskDynamic[]
  /** 与 static.gpus 一一对应的 GPU 利用率；null 表示该平台不支持采集 */
  gpuUsages: (number | null)[]
  uptime: number
}

export interface NetInterface {
  name: string
  ip: string
  mac: string
  internal: boolean
}

export interface PublicIPInfo {
  ip: string
  location?: string
  source: string
}

// ---------- CPU 差分采样 ----------

let lastCpuTimes: os.CpuInfo[] | null = null

function sampleCpu(): CpuDynamic {
  const cpus = os.cpus()
  const cores: number[] = []
  let totalIdle = 0
  let totalTick = 0

  cpus.forEach((cpu, i) => {
    const t = cpu.times
    const tick = t.user + t.nice + t.sys + t.idle + t.irq
    totalTick += tick
    totalIdle += t.idle
    const prev = lastCpuTimes?.[i]?.times
    if (prev) {
      const idleDelta = t.idle - prev.idle
      const tickDelta =
        tick - (prev.user + prev.nice + prev.sys + prev.idle + prev.irq)
      cores.push(tickDelta > 0 ? Math.max(0, Math.min(100, (1 - idleDelta / tickDelta) * 100)) : 0)
    } else {
      cores.push(0)
    }
  })

  let usage = 0
  if (lastCpuTimes) {
    const prevIdle = lastCpuTimes.reduce((s, c) => s + c.times.idle, 0)
    const prevTick = lastCpuTimes.reduce(
      (s, c) => s + c.times.user + c.times.nice + c.times.sys + c.times.idle + c.times.irq,
      0
    )
    const idleDelta = totalIdle - prevIdle
    const tickDelta = totalTick - prevTick
    if (tickDelta > 0) usage = Math.max(0, Math.min(100, (1 - idleDelta / tickDelta) * 100))
  }
  lastCpuTimes = cpus
  return { usage: Math.round(usage * 10) / 10, cores: cores.map((c) => Math.round(c)) }
}

// ---------- 内存（darwin: vm_stat 精确值；其他平台退化为 os.freemem） ----------

let pageSize = 4096

async function sampleMem(): Promise<MemDynamic> {
  const total = os.totalmem()
  if (process.platform !== 'darwin') {
    const free = os.freemem()
    const used = total - free
    return { total, used, available: free, usage: (used / total) * 100 }
  }
  try {
    const { stdout } = await exec('vm_stat')
    const pageMatch = stdout.match(/page size of (\d+) bytes/)
    if (pageMatch) pageSize = parseInt(pageMatch[1], 10)
    const grab = (key: string): number => {
      const m = stdout.match(new RegExp(`Pages ${key}[^\\d]*(\\d+)`))
      return m ? parseInt(m[1], 10) : 0
    }
    const availablePages =
      grab('free') + grab('speculative') + grab('inactive')
    const available = availablePages * pageSize
    const used = Math.max(0, total - available)
    return { total, used, available, usage: (used / total) * 100 }
  } catch {
    const free = os.freemem()
    const used = total - free
    return { total, used, available: free, usage: (used / total) * 100 }
  }
}

// ---------- 磁盘 ----------

let cachedMounts: string[] | null = null
let mountsCachedAt = 0
const MOUNTS_CACHE_MS = 30_000

/** df 列出真实磁盘卷（排除 devfs / 虚拟卷，APFS 同盘多卷去重），30s 缓存支持热插拔 */
async function listMounts(): Promise<string[]> {
  if (cachedMounts && Date.now() - mountsCachedAt < MOUNTS_CACHE_MS) return cachedMounts
  const mounts: string[] = []
  try {
    const { stdout } = await exec('df', ['-k'])
    const seenBase = new Set<string>()
    for (const line of stdout.trim().split('\n').slice(1)) {
      const cols = line.trim().split(/\s+/)
      if (cols.length < 9) continue
      const dev = cols[0]
      const mount = cols.slice(8).join(' ')
      if (!dev.startsWith('/dev/')) continue
      if (/^(devfs|map )/.test(dev)) continue
      // /System/Volumes/* 全部为系统内部卷（Data/Preboot/VM 等），仅保留根卷与用户挂载卷
      if (mount.startsWith('/System/Volumes')) continue
      // /dev/disk3s1 -> /dev/disk3，同盘多卷去重
      const base = dev.replace(/(s\d+)+$/, '')
      if (seenBase.has(base)) continue
      seenBase.add(base)
      mounts.push(mount)
    }
  } catch {
    mounts.push('/')
  }
  cachedMounts = mounts.length > 0 ? mounts : ['/']
  mountsCachedAt = Date.now()
  return cachedMounts
}

async function statDisk(mount: string): Promise<DiskDynamic | null> {
  try {
    const st = await fs.promises.statfs(mount)
    const total = st.blocks * st.bsize
    const free = st.bavail * st.bsize
    const used = total - st.bfree * st.bsize
    if (total <= 0) return null
    return { mount, total, free, used, usage: (used / total) * 100 }
  } catch {
    return null
  }
}

// ---------- 静态硬件信息（darwin: system_profiler，失败回退 os.*） ----------

/**
 * 解析 system_profiler SPDisplaysDataType：GPU 条目为 4 空格缩进标题，
 * 其下 6 空格为属性；GPU 内嵌 "Displays:" 段的 8 空格标题行为显示器名
 */
async function parseGpus(): Promise<GpuInfo[]> {
  const gpus: GpuInfo[] = []
  try {
    const { stdout } = await exec('system_profiler', ['SPDisplaysDataType'])
    let current: GpuInfo | null = null
    let inDisplays = false
    for (const line of stdout.split('\n')) {
      const gpuTitle = line.match(/^\s{4}(?!\s)([^:].+):$/)
      const prop = line.match(/^\s{6}(?!\s)([^:]+):\s*(.*)$/)
      const displayTitle = line.match(/^\s{8}(?!\s)([^:].+):$/)
      if (gpuTitle) {
        inDisplays = false
        current = null
        if (gpuTitle[1] !== 'Graphics/Displays' && gpuTitle[1] !== 'Displays') {
          current = { name: gpuTitle[1], chipsetModel: '', displays: [] }
          gpus.push(current)
        }
        continue
      }
      if (displayTitle && inDisplays && current) {
        current.displays.push(displayTitle[1])
        continue
      }
      if (prop && current) {
        const key = prop[1].trim()
        const value = prop[2].trim()
        if (key === 'Displays') {
          inDisplays = true
        } else if (key === 'Chipset Model') {
          current.chipsetModel = value
        } else if (key === 'Total Number of Cores') {
          current.cores = parseInt(value, 10) || undefined
        } else if (key === 'Metal Support') {
          current.metal = value
        } else if (key === 'Bus') {
          current.bus = value
        } else if (key.startsWith('VRAM')) {
          const gb = value.match(/([\d.]+)\s*(GB|MB)/)
          if (gb) {
            const n = parseFloat(gb[1])
            current.vram = Math.round(gb[2] === 'GB' ? n * 1024 ** 3 : n * 1024 ** 2)
          }
        }
      }
    }
  } catch {
    /* 非 darwin 或命令不可用时返回空列表 */
  }
  return gpus
}

/**
 * GPU 利用率（darwin）：IOAccelerator 的 PerformanceStatistics 无需 root 即可读取。
 * 每块 GPU 对应一个 IOAccelerator 实例，与 static.gpus 按索引对齐。
 * ioreg 的瞬时值会抖动（空闲时读到 0、偶发执行失败），此处做指数移动平均平滑，
 * 失败或解析为空时沿用上一次成功值，避免界面闪烁
 */
let lastGpuUsages: (number | null)[] | null = null

async function sampleGpuUsages(gpuCount: number): Promise<(number | null)[]> {
  const fallback = lastGpuUsages ?? Array.from({ length: gpuCount }, () => null)
  if (process.platform !== 'darwin' || gpuCount === 0) return fallback
  try {
    const { stdout } = await exec('ioreg', ['-r', '-d', '1', '-c', 'IOAccelerator'])
    const raw = [...stdout.matchAll(/"Device Utilization %"=(\d+)/g)]
      .map((m) => parseInt(m[1], 10))
      .slice(0, gpuCount)
    if (raw.length === 0) return fallback
    const result: (number | null)[] = Array.from({ length: gpuCount }, () => null)
    for (let i = 0; i < gpuCount; i++) {
      if (raw[i] === undefined) continue
      const prev = lastGpuUsages?.[i]
      result[i] =
        prev === null || prev === undefined ? raw[i] : Math.round(prev * 0.5 + raw[i] * 0.5)
    }
    lastGpuUsages = result
    return result
  } catch {
    return fallback
  }
}

export interface DevToolInfo {
  id: 'java' | 'maven' | 'node' | 'docker' | 'git' | 'python'
  name: string
  /** 解析出的版本号；null 表示未安装或不在 PATH */
  version: string | null
  /** 补充信息：JAVA_HOME / Maven home / npm 版本 / pip 版本等 */
  detail?: string
}

/**
 * 开发工具链版本探测（跨平台通用，命令不在 PATH 或执行失败视为未安装）
 *
 * GUI 应用不继承登录 shell 的 PATH（Finder/启动台启动只有系统默认路径），
 * nvm / homebrew / IDEA 内置 maven 等都会探测不到。
 * 此处先通过交互式登录 shell（zsh -i，加载 ~/.zshrc）取回用户真实 PATH，
 * 失败时回退到常见安装目录拼接
 */
let cachedShellPath: string | null = null

/** 供其他模块复用：解析 GUI 应用缺失的登录 shell PATH */
export async function getShellPath(): Promise<string> {
  if (cachedShellPath) return cachedShellPath
  const base = process.env.PATH ?? ''
  if (process.platform === 'darwin') {
    try {
      const { stdout } = await exec('zsh', ['-i', '-c', 'echo __PATH__$PATH'], { timeout: 8000 })
      const line = stdout.split('\n').find((l) => l.includes('__PATH__'))
      const p = line?.replace('__PATH__', '').trim()
      if (p && p.includes('/')) {
        cachedShellPath = [p, base].filter(Boolean).join(':')
        return cachedShellPath
      }
    } catch {
      /* .zshrc 加载失败时走回退 */
    }
  }
  const extra = [
    '/opt/homebrew/bin',
    '/usr/local/bin',
    `${homedir()}/.nvm/versions/node/latest/bin`,
    `${homedir()}/.sdkman/candidates/maven/current/bin`,
    `${homedir()}/.sdkman/candidates/java/current/bin`
  ]
  cachedShellPath = [...new Set([base, ...extra].filter(Boolean))].join(':')
  return cachedShellPath
}

async function runTool(cmd: string, args: string[]): Promise<{ stdout: string; stderr: string }> {
  const path = await getShellPath()
  return await exec(cmd, args, { timeout: 6000, env: { ...process.env, PATH: path } })
}

async function getDevTools(): Promise<DevToolInfo[]> {
  const [java, maven, node, npm, docker, git, python3, pip3] = await Promise.allSettled([
    runTool('java', ['-version']),
    runTool('mvn', ['--version']),
    runTool('node', ['--version']),
    runTool('npm', ['--version']),
    runTool('docker', ['--version']),
    runTool('git', ['--version']),
    runTool('python3', ['--version']),
    runTool('pip3', ['--version'])
  ])

  const javaHome = process.env.JAVA_HOME || ''

  const tools: DevToolInfo[] = []

  if (java.status === 'fulfilled') {
    const text = `${java.value.stdout} ${java.value.stderr}`
    const m = text.match(/version "([^"]+)"/)
    tools.push({
      id: 'java',
      name: 'Java',
      version: m ? m[1] : null,
      detail: javaHome || undefined
    })
  } else {
    tools.push({ id: 'java', name: 'Java', version: null, detail: javaHome || undefined })
  }

  if (maven.status === 'fulfilled') {
    const m = maven.value.stdout.match(/Apache Maven ([\d.]+)/)
    const home = maven.value.stdout.match(/Maven home: (.+)$/m)
    tools.push({
      id: 'maven',
      name: 'Maven',
      version: m ? m[1] : null,
      detail: home?.[1]?.trim()
    })
  } else {
    tools.push({ id: 'maven', name: 'Maven', version: null })
  }

  if (node.status === 'fulfilled') {
    const v = node.value.stdout.trim().replace(/^v/, '')
    tools.push({
      id: 'node',
      name: 'Node.js',
      version: v || null,
      detail: npm.status === 'fulfilled' ? `npm ${npm.value.stdout.trim()}` : undefined
    })
  } else {
    tools.push({ id: 'node', name: 'Node.js', version: null })
  }

  if (git.status === 'fulfilled') {
    const m = git.value.stdout.match(/git version ([\d.]+)/)
    const apple = git.value.stdout.match(/\(([^)]*Git[^)]*)\)/)
    tools.push({
      id: 'git',
      name: 'Git',
      version: m ? m[1] : null,
      detail: apple?.[1]
    })
  } else {
    tools.push({ id: 'git', name: 'Git', version: null })
  }

  if (python3.status === 'fulfilled') {
    const m = python3.value.stdout.match(/Python ([\d.]+)/)
    tools.push({
      id: 'python',
      name: 'Python',
      version: m ? m[1] : null,
      detail: pip3.status === 'fulfilled' ? `pip ${pip3.value.stdout.match(/pip ([\d.]+)/)?.[1] ?? ''}`.trim() : undefined
    })
  } else {
    tools.push({ id: 'python', name: 'Python', version: null })
  }

  if (docker.status === 'fulfilled') {
    const m = docker.value.stdout.match(/Docker version ([^,]+)/)
    tools.push({ id: 'docker', name: 'Docker', version: m ? m[1].trim() : null })
  } else {
    tools.push({ id: 'docker', name: 'Docker', version: null })
  }

  return tools
}

let staticCache: SysStaticInfo | null = null

async function parseHardware(): Promise<Record<string, string>> {
  const info: Record<string, string> = {}
  try {
    const { stdout } = await exec('system_profiler', ['SPHardwareDataType'])
    for (const line of stdout.split('\n')) {
      const m = line.match(/^\s{4}([^:]+):\s+(.+)$/)
      if (m) info[m[1].trim()] = m[2].trim()
    }
  } catch {
    /* 非 darwin 或命令不可用时回退 */
  }
  return info
}

async function getStatic(): Promise<SysStaticInfo> {
  if (staticCache) return staticCache
  const hw = await parseHardware()

  let computerName = os.hostname()
  try {
    const { stdout } = await exec('scutil', ['--get', 'ComputerName'])
    computerName = stdout.trim()
  } catch {
    /* ignore */
  }

  let physicalCores = parseInt(hw['Total Number of Cores'] ?? '0', 10)
  if (!physicalCores) {
    try {
      const { stdout } = await exec('sysctl', ['-n', 'hw.physicalcpu'])
      physicalCores = parseInt(stdout.trim(), 10)
    } catch {
      physicalCores = os.cpus().length
    }
  }

  staticCache = {
    hostname: os.hostname(),
    computerName,
    modelName: hw['Model Name'] ?? '',
    modelIdentifier: hw['Model Identifier'] ?? '',
    chip: hw['Chip'] ?? hw['Processor Name'] ?? os.cpus()[0]?.model ?? '',
    cpuModel: os.cpus()[0]?.model ?? hw['Processor Name'] ?? '',
    cpuLogicalCores: os.cpus().length,
    cpuPhysicalCores: physicalCores,
    memoryTotal: os.totalmem(),
    osName: 'macOS',
    osVersion: `${os.release()}（${hw['OSVersion'] ?? ''}）`.replace(/（）$/, ''),
    kernel: os.release(),
    arch: os.arch(),
    gpus: await parseGpus()
  }
  return staticCache
}

// ---------- 网络 ----------

function getNetwork(): { interfaces: NetInterface[]; primaryIP: string } {
  const ni = os.networkInterfaces()
  const interfaces: NetInterface[] = []
  for (const [name, addrs] of Object.entries(ni)) {
    for (const addr of addrs ?? []) {
      if (addr.family !== 'IPv4' || addr.internal) continue
      interfaces.push({ name, ip: addr.address, mac: addr.mac, internal: addr.internal })
    }
  }
  // en0（Wi-Fi/以太网）优先作为主 IP
  const primary = interfaces.find((i) => i.name === 'en0') ?? interfaces[0]
  return { interfaces, primaryIP: primary?.ip ?? '' }
}

/** 公网 IP 探测服务链（国内可达性优先） */
const PUBLIC_IP_SERVICES: { url: string; name: string }[] = [
  { url: 'https://myip.ipip.net', name: 'ipip.net' },
  { url: 'https://ip.3322.net', name: '3322.net' },
  { url: 'https://api.ipify.org?format=json', name: 'ipify' },
  { url: 'https://ifconfig.me/ip', name: 'ifconfig.me' }
]

async function fetchWithTimeout(url: string, ms: number): Promise<string> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), ms)
  try {
    const res = await fetch(url, {
      signal: controller.signal,
      headers: { 'User-Agent': 'curl/8.0' }
    })
    return await res.text()
  } finally {
    clearTimeout(timer)
  }
}

async function getPublicIP(): Promise<PublicIPInfo> {
  let lastError = '无法访问公网探测服务'
  for (const svc of PUBLIC_IP_SERVICES) {
    try {
      const text = (await fetchWithTimeout(svc.url, 5000)).trim()
      let ip = ''
      let location: string | undefined
      if (svc.name === 'ipify') {
        ip = JSON.parse(text).ip ?? ''
      } else {
        const m = text.match(/(\d{1,3}\.){3}\d{1,3}/)
        ip = m ? m[0] : ''
        const loc = text.match(/来自于[:：]\s*(.+)$/)
        if (loc) location = loc[1].trim()
      }
      if (ip) return { ip, location, source: svc.name }
      lastError = `${svc.name} 返回内容无法解析`
    } catch (err) {
      lastError = `${svc.name}: ${(err as Error).message}`
    }
  }
  throw new Error(lastError)
}

// ---------- IPC 注册 ----------

export function registerSysinfoHandlers(): void {
  ipcMain.handle('sysinfo:static', async () => {
    try {
      return { ok: true, data: await getStatic() }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('sysinfo:dynamic', async () => {
    try {
      const [mem, mounts, gpus] = await Promise.all([
        sampleMem(),
        listMounts(),
        getStatic().then((s) => sampleGpuUsages(s.gpus.length))
      ])
      const disks = (await Promise.all(mounts.map(statDisk))).filter(
        (d): d is DiskDynamic => d !== null
      )
      return {
        ok: true,
        data: {
          cpu: sampleCpu(),
          mem,
          disks,
          gpuUsages: gpus,
          uptime: os.uptime()
        } satisfies SysDynamicInfo
      }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('sysinfo:network', () => {
    const net = getNetwork()
    return { ok: true, data: net }
  })

  ipcMain.handle('sysinfo:devtools', async () => {
    try {
      return { ok: true, data: await getDevTools() }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('sysinfo:publicIP', async () => {
    try {
      return { ok: true, data: await getPublicIP() }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })
}
