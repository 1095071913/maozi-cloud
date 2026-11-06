import {execFile} from 'node:child_process'
import os from 'node:os'
import fs from 'node:fs'
import path from 'node:path'
import {promisify} from 'node:util'
import {ipcMain, net, type WebContents} from 'electron'
import {selectPlatform} from '../platform'

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

/** 系统代理（scutil --proxy）：未启用的项为空串 */
export interface ProxyInfo {
  http: string
  https: string
  /** SOCKS，对应 git 的 all_proxy */
  all: string
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
      // 用户临时目录与系统 tmp 下的临时挂载卷（Docker Desktop / 应用安装器把 DMG 挂到
      // /private/var/folders/<xx>/<yy>/T/com.docker.install/... 即此类），安装完成即卸载，无展示意义
      if (/^\/private\/var\/folders\/.+\/T\//.test(mount)) continue
      if (/^\/(private\/)?(tmp|var\/tmp)\//.test(mount)) continue
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
 * 此处先通过登录 shell（zsh -l -i，与真实终端一致）取回用户真实 PATH，
 * 失败时退登录非交互（规避 .zshrc 交互组件挂起），最后回退常见安装目录拼接
 */
let cachedShellPath: string | null = null

/** 供其他模块复用：解析 GUI 应用缺失的登录 shell PATH */
export async function getShellPath(): Promise<string> {
  if (cachedShellPath) return cachedShellPath
  const base = process.env.PATH ?? ''
  if (process.platform === 'darwin') {
    // 必须带 -l（登录 shell）：/usr/local/bin、/opt/homebrew/bin 由 /etc/zprofile 的 path_helper
    // 注入，仅 -i（交互非登录）不加载，而 .zshrc 只在继承值前追加——打包应用从 Finder/启动台
    // 启动时 PATH 只有系统最小集，取回的 PATH 将不含 /usr/local/bin（Docker Desktop 的 docker
    // 就在那），本地绑定的容器网段/初始化镜像等 docker 流程随之全部失效
    for (const flags of [['-l', '-i'], ['-l']] as const) {
      try {
        const { stdout } = await exec('zsh', [...flags, '-c', 'echo __PATH__$PATH'], { timeout: 8000 })
        const line = stdout.split('\n').find((l) => l.includes('__PATH__'))
        const p = line?.replace('__PATH__', '').trim()
        if (p && p.includes('/')) {
          cachedShellPath = [p, base].filter(Boolean).join(':')
          return cachedShellPath
        }
      } catch {
        /* 该方式失败换下一种 */
      }
    }
  }
  const extra = [
    '/opt/homebrew/bin',
    '/usr/local/bin',
    `${os.homedir()}/.nvm/versions/node/latest/bin`,
    `${os.homedir()}/.sdkman/candidates/maven/current/bin`,
    `${os.homedir()}/.sdkman/candidates/java/current/bin`
  ]
  cachedShellPath = [...new Set([base, ...extra].filter(Boolean))].join(':')
  return cachedShellPath
}

async function runTool(cmd: string, args: string[]): Promise<{ stdout: string; stderr: string }> {
  const path = await getShellPath()
  return await exec(cmd, args, { timeout: 6000, env: { ...process.env, PATH: path } })
}

// ---------- JDK 管理（切换 = 写 JAVA_HOME 到 ~/.zshrc 托管区块，复用环境变量实时生效机制） ----------

export interface JdkEntry {
  /** 版本号（如 17.0.2 / 1.8.0_392） */
  version: string
  /** 发行方标识（Eclipse Temurin / Homebrew / SDKMAN / 目录名） */
  vendor: string
  /** JAVA_HOME 路径 */
  home: string
}

/** 读 JDK home 的 release 文件取版本（无文件返回空，由调用方回退目录名） */
function jdkVersionFromRelease(home: string): string {
  try {
    const m = fs.readFileSync(path.join(home, 'release'), 'utf8').match(/^JAVA_VERSION="([^"]+)"/m)
    if (m) return m[1]
  } catch {
    /* 无 release 文件 */
  }
  return ''
}

/** 登录 shell 读当前 JAVA_HOME（与真实终端一致；先交互后非交互，规避 .zshrc 交互组件挂起） */
async function currentJavaHome(): Promise<string> {
  for (const flags of [['-l', '-i'], ['-l']] as const) {
    try {
      const { stdout } = await exec('zsh', [...flags, '-c', 'printf "__JH__%s" "$JAVA_HOME"'], { timeout: 6000 })
      const m = stdout.match(/__JH__(.*)/)
      const v = m?.[1].trim() ?? ''
      if (v) return v
      break
    } catch {
      /* 换下一种 */
    }
  }
  return process.env.JAVA_HOME ?? ''
}

/**
 * 列出本机可切换的 JDK（macOS）：
 * 首选 /usr/libexec/java_home -V（覆盖系统与 Homebrew bundle），兜底扫描 Apple 标准位置、
 * 用户级 JavaVirtualMachines、SDKMAN 与 Homebrew openjdk*（无 bundle 结构，java_home 看不到）
 */
async function listJdks(): Promise<{ list: JdkEntry[]; current: string }> {
  const byHome = new Map<string, JdkEntry>()
  if (process.platform === 'darwin') {
    try {
      const { stdout, stderr } = await exec('/usr/libexec/java_home', ['-V'], { timeout: 5000 })
      // 行样例：`    17.0.2 (arm64) "Homebrew" - "Homebrew" /opt/.../Contents/Home`
      for (const line of `${stdout}\n${stderr}`.split('\n')) {
        const m = line.match(/^\s*([\w.]+)\s+\([^)]*\)\s+"([^"]*)"\s+-\s+"[^"]*"\s+(\S+)\s*$/)
        if (m && fs.existsSync(m[3])) byHome.set(m[3], { version: m[1], vendor: m[2] || 'JDK', home: m[3] })
      }
    } catch {
      /* 回退目录扫描 */
    }
  }
  const scanRoots: Array<{ root: string; join: (base: string, name: string) => string; vendor: string }> = [
    { root: '/Library/Java/JavaVirtualMachines', join: (b, n) => path.join(b, n, 'Contents', 'Home'), vendor: '' },
    { root: path.join(os.homedir(), 'Library/Java/JavaVirtualMachines'), join: (b, n) => path.join(b, n, 'Contents', 'Home'), vendor: '' },
    { root: path.join(os.homedir(), '.sdkman/candidates/java'), join: (b, n) => path.join(b, n), vendor: 'SDKMAN' }
  ]
  for (const { root, join, vendor } of scanRoots) {
    let names: string[] = []
    try {
      names = fs.readdirSync(root)
    } catch {
      continue
    }
    for (const n of names) {
      if (n === 'current') continue
      const home = join(root, n)
      if (!fs.existsSync(path.join(home, 'bin', 'java')) || byHome.has(home)) continue
      byHome.set(home, {
        version: jdkVersionFromRelease(home) || n.replace(/\.jdk$/i, '').replace(/^jdk[-_]?/i, ''),
        vendor: vendor || n.replace(/\.jdk$/i, ''),
        home
      })
    }
  }
  // Homebrew openjdk*：无 bundle 结构（java_home -V 不列），libexec 下才是真 home
  for (const prefix of ['/opt/homebrew/opt', '/usr/local/opt']) {
    let names: string[] = []
    try {
      names = fs.readdirSync(prefix)
    } catch {
      continue
    }
    for (const n of names.filter(x => /^openjdk/i.test(x))) {
      for (const home of [path.join(prefix, n, 'libexec/openjdk.jdk/Contents/Home'), path.join(prefix, n, 'openjdk.jdk/Contents/Home')]) {
        if (fs.existsSync(path.join(home, 'bin', 'java')) && !byHome.has(home)) {
          byHome.set(home, { version: jdkVersionFromRelease(home) || n.replace(/^openjdk@?/i, ''), vendor: 'Homebrew', home })
          break
        }
      }
    }
  }
  return { list: [...byHome.values()], current: await currentJavaHome(), arch: os.arch() === 'arm64' ? 'aarch64' : 'x64' }
}

/**
 * 切换 JDK：JAVA_HOME 写入 ~/.zshrc（走平台环境变量托管区块——原位更新、备份、
 * precmd/preexec 实时生效钩子、跨文件迁移清理一并生效），并幂等追加一次
 * PATH 头部引用（$JAVA_HOME/bin 优先）；最后失效 shell PATH 缓存让探测立即反映新 JDK
 */
async function switchJdk(homeArg: string): Promise<void> {
  const home = homeArg.trim()
  if (!home) throw new Error('缺少 JDK 路径')
  if (!fs.existsSync(path.join(home, 'bin', 'java'))) throw new Error('目标不是有效的 JDK（缺少 bin/java）')
  const platform = selectPlatform()
  if (platform.id !== 'darwin') throw new Error('JDK 切换当前仅支持 macOS')
  platform.saveEnvVar({ mode: 'update', key: 'JAVA_HOME', value: home, fileId: 'zshrc' })
  // PATH 头部引用仅追加一次（幂等）：已有 $JAVA_HOME/bin 字样则不动，避免 PATH 重复膨胀
  const zshrc = path.join(os.homedir(), '.zshrc')
  const PATH_LINE = 'export PATH="$JAVA_HOME/bin:$PATH"'
  const text = fs.existsSync(zshrc) ? fs.readFileSync(zshrc, 'utf8') : ''
  if (!text.includes('$JAVA_HOME/bin')) {
    fs.writeFileSync(zshrc, `${text.replace(/\n*$/, '\n')}\n# >>> maozi-cloud-develop-admin (java-home-path) >>>\n${PATH_LINE}\n# <<< maozi-cloud-develop-admin (java-home-path) <<<\n`, { mode: 0o644 })
  }
  // 追加 PATH 行后重新生成实时环境文件：在线终端经 .zshrc 钩子（precmd/precmd 检测 md5 变化）
  // source 的正是这份文件，必须同时包含新 JAVA_HOME 与 PATH 前置，java 命令才会立即解析到新版本
  platform.saveEnvVar({ mode: 'update', key: 'JAVA_HOME', value: home, fileId: 'zshrc' })
  cachedShellPath = null
}

/** 流式下载一个 Response 到文件（返回接收字节数；progress 节流由调用方处理） */
async function streamResponseToFile(res: { ok: boolean; status: number; body: ReadableStream<Uint8Array> | null; headers: { get(k: string): string | null } }, file: string, onBytes: (received: number, total: number) => void): Promise<number> {
  if (!res.ok || !res.body) {
    if (res.status === 404) {
      throw new Error('下载源返回 HTTP 404：该发行商不提供此版本与架构的组合（如 Eclipse Adoptium 无 macOS Apple Silicon 的 JDK 8），请改选其它版本或发行商')
    }
    throw new Error(`HTTP ${res.status}`)
  }
  const total = Number(res.headers.get('content-length') ?? 0)
  let received = 0
  const out = fs.createWriteStream(file)
  const reader = res.body.getReader()
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    received += value.byteLength
    out.write(Buffer.from(value))
    onBytes(received, total)
  }
  await new Promise<void>((resolve, reject) => out.end(err => (err ? reject(err) : resolve())))
  return received
}

/** 系统 curl 下载（最终兜底：--fail 非 2xx 报错，进度按落盘字节数轮询） */
function curlDownload(url: string, file: string, signal: AbortSignal, onBytes: (received: number) => void): Promise<number> {
  return new Promise((resolve, reject) => {
    const { spawn } = require('node:child_process') as typeof import('node:child_process')
    const child = spawn('/usr/bin/curl', ['-L', '--fail', '--silent', '--show-error', '--max-time', '600', '-o', file, url])
    const onAbort = (): void => child.kill('SIGKILL')
    signal.addEventListener('abort', onAbort, { once: true })
    const timer = setInterval(() => {
      try {
        onBytes(fs.statSync(file).size)
      } catch {
        /* 文件尚未创建 */
      }
    }, 200)
    const cleanup = (): void => {
      clearInterval(timer)
      signal.removeEventListener('abort', onAbort)
    }
    child.once('error', err => {
      cleanup()
      reject(err)
    })
    child.once('exit', code => {
      cleanup()
      if (signal.aborted) return reject(new DOMException('Aborted', 'AbortError'))
      if (code === 0) {
        try {
          resolve(fs.statSync(file).size)
        } catch (err) {
          reject(err as Error)
        }
      } else {
        reject(new Error(`curl 退出码 ${code}`))
      }
    })
  })
}

/**
 * 三通道下载到文件，任一通道成功即返回（返回成功通道名）：
 * 1. net.fetch —— Chromium 网络栈，自动使用系统代理/VPN（对 GitHub 等直连不通的源关键）
 * 2. Node fetch —— 始终直连（代理把某些域名分流为"直连"但代理层又断连时，真直连反而可达）
 * 3. 系统 curl —— 最终兜底（走系统网络配置，跟随重定向）
 * 连接类错误自动降级下一通道；HTTP 4xx 等源问题直接上抛不重试
 */
async function downloadViaChannels(url: string, file: string, signal: AbortSignal, onProgress: (received: number, total: number, via: string) => void): Promise<string> {
  const errors: string[] = []
  const attempts: Array<{ name: string; run: () => Promise<number | string> }> = [
    {
      name: '系统代理',
      run: async () => {
        const res = await net.fetch(url, { signal })
        const n = await streamResponseToFile(res, file, (r, t) => onProgress(r, t, '系统代理'))
        return n
      }
    },
    {
      name: '直连',
      run: async () => {
        const res = await fetch(url, { redirect: 'follow', signal })
        const n = await streamResponseToFile(res, file, (r, t) => onProgress(r, t, '直连'))
        return n
      }
    },
    {
      name: 'curl',
      run: async () => {
        await curlDownload(url, file, signal, r => onProgress(r, 0, 'curl'))
        return 'curl'
      }
    }
  ]
  for (const a of attempts) {
    try {
      const r = await a.run()
      const received = typeof r === 'number' ? r : fs.statSync(file).size
      if (received < 1024 * 1024) throw new Error('下载内容异常（小于 1MB）')
      return a.name
    } catch (err) {
      if ((err as Error).name === 'AbortError') throw err
      const msg = (err as Error).message ?? String(err)
      errors.push(`${a.name}: ${msg}`)
      fs.rmSync(file, { force: true })
      // 源问题（4xx）不降级重试
      if (/HTTP 4\d\d/.test(msg)) throw new Error(msg)
    }
  }
  throw new Error(`下载失败（已依次尝试 ${errors.map(e => e.split(':')[0]).join(' → ')}）：${errors.join('；')}`)
}

/**
 * 卸载 JDK（危险操作，调用方须先经用户确认）：
 * - 用户级目录（~/Library/Java/JavaVirtualMachines、SDKMAN）直接删除
 * - Homebrew 安装走 brew uninstall（不用 sudo，brew 自己管理）
 * - 系统位置（/Library/Java/JavaVirtualMachines）经 osascript 管理员授权删除（弹系统密码框）
 * 安全护栏：仅允许白名单前缀内的路径；当前使用中的 JDK 拒绝卸载
 */
async function uninstallJdk(homeArg: string): Promise<void> {
  const home = homeArg.trim()
  if (!fs.existsSync(path.join(home, 'bin', 'java'))) throw new Error('目标不是有效的 JDK')
  const current = await currentJavaHome()
  if (current && path.resolve(current) === path.resolve(home)) {
    throw new Error('该 JDK 正在使用中，请先切换到其它版本再卸载')
  }
  const userPrefix = (p: string): string => path.join(os.homedir(), p)
  // 允许卸载的目录族（home 前缀 → JVM bundle 根目录推导方式）
  const families: Array<{ test: (h: string) => boolean; vmDir: (h: string) => string; mode: 'direct' | 'admin' | 'brew' }> = [
    // Apple 标准位置（系统级）：home = <vm>/Contents/Home → vm 目录，提权删除
    {
      test: h => h.startsWith('/Library/Java/JavaVirtualMachines/'),
      vmDir: h => path.resolve(h, '../..'),
      mode: 'admin'
    },
    // 用户级 JavaVirtualMachines：直接删除
    {
      test: h => h.startsWith(userPrefix('Library/Java/JavaVirtualMachines') + '/'),
      vmDir: h => path.resolve(h, '../..'),
      mode: 'direct'
    },
    // SDKMAN：home 即版本目录，直接删除
    {
      test: h => h.startsWith(userPrefix('.sdkman/candidates/java') + '/'),
      vmDir: h => h,
      mode: 'direct'
    },
    // Homebrew：home 在 opt/<formula>/... 或 Cellar/<formula>/<ver>/... 下，走 brew uninstall
    {
      test: h => /^\/(opt|usr\/local)\/(homebrew|Cellar.*|opt)\//.test(h) || h.startsWith('/opt/homebrew/opt/') || h.startsWith('/opt/homebrew/Cellar/') || h.startsWith('/usr/local/opt/') || h.startsWith('/usr/local/Cellar/'),
      vmDir: h => h,
      mode: 'brew'
    }
  ]
  const fam = families.find(f => f.test(path.resolve(home)))
  if (!fam) throw new Error('该 JDK 不在受支持的管理位置（标准 JavaVirtualMachines / Homebrew / SDKMAN），请手动删除')
  const vmDir = fam.vmDir(path.resolve(home))
  if (fam.mode === 'direct') {
    fs.rmSync(vmDir, { recursive: true, force: true })
    return
  }
  if (fam.mode === 'admin') {
    // osascript 管理员授权执行（系统弹密码框，与 Hosts 写入同机制）；路径用单引号包裹（JDK 目录名不含单引号）
    const script = `do shell script "rm -rf '${vmDir.replace(/'/g, "'\\\\''")}'" with administrator privileges`
    await exec('/usr/bin/osascript', ['-e', script], { timeout: 180_000 })
    return
  }
  // brew：从路径推导 formula——取最后一个 opt/ 或 Cellar/ 段之后的名字
  // （/opt/homebrew/opt/<formula>/... 、/opt/homebrew/Cellar/<formula>/<ver>/... 、/usr/local/opt/...）
  const segs = vmDir.split('/')
  const iOpt = segs.lastIndexOf('opt')
  const iCellar = segs.lastIndexOf('Cellar')
  const i = Math.max(iOpt, iCellar)
  const formula = i >= 0 ? (segs[i + 1] ?? '') : ''
  if (!/^[\w.@+-]+$/.test(formula)) throw new Error('无法从路径推导 Homebrew 包名：' + vmDir)
  await exec('/opt/homebrew/bin/brew', ['uninstall', '--force', formula], { timeout: 120_000, env: { ...process.env, PATH: await getShellPath() } }).catch(async err => {
    const e = err as { stderr?: string; message: string }
    throw new Error(`brew uninstall 失败：${e.stderr || e.message}`)
  })
}

// ---------- JDK 下载安装（官方发行商 API → 用户级 JavaVirtualMachines，无需 sudo） ----------

export type JdkVendor = 'adoptium' | 'corretto'

export interface JdkDownloadProgress {
  stage: 'download' | 'extract' | 'install' | 'done' | 'error'
  /** download：字节百分比 0-100；其余阶段忽略 */
  pct: number
  receivedMB: number
  totalMB: number
  message: string
}

/** 下载安装进行中（同时仅一个）；进度推送目标为发起下载的渲染层 */
let jdkDlAbort: AbortController | null = null
let jdkDlWin: WebContents | null = null

function jdkDlEmit(p: JdkDownloadProgress): void {
  jdkDlWin?.send('sysinfo:jdkDownloadProgress', p)
}

/**
 * 下载并安装 JDK：
 * - Eclipse Adoptium：api.adoptium.net latest GA（302 → GitHub release tar.gz）
 * - Amazon Corretto：corretto.aws latest 固定名包
 * 流式下载（进度推送）→ tar 解压到 ~/Library/Java/JavaVirtualMachines → 统一改名为 <vendor>-<major>.jdk
 * 已存在同名目录视为重装（先删后装）
 */
async function downloadJdk(versionArg: string, vendorArg: string): Promise<void> {
  const version = String(versionArg ?? '').trim()
  const vendor = String(vendorArg ?? '').trim() as JdkVendor
  if (!/^\d{1,2}$/.test(version)) throw new Error('版本号不合法（应为 8/11/17/21/25 这类主版本号）')
  if (vendor !== 'adoptium' && vendor !== 'corretto') throw new Error('发行商不合法（仅支持 Eclipse Adoptium / Amazon Corretto）')
  if (process.platform !== 'darwin') throw new Error('JDK 下载当前仅支持 macOS')
  if (jdkDlAbort) throw new Error('已有下载任务进行中')

  const arch = os.arch() === 'arm64' ? 'aarch64' : 'x64'
  // Corretto 固定名实测（302 → 版本化包，8 与 11+ 同为 -jdk 后缀，无需特判）
  const url =
    vendor === 'adoptium'
      ? `https://api.adoptium.net/v3/binary/latest/${version}/ga/mac/${arch}/jdk/hotspot/normal/eclipse`
      : `https://corretto.aws/downloads/latest/amazon-corretto-${version}-${arch}-macos-jdk.tar.gz`

  const vmRoot = path.join(os.homedir(), 'Library/Java/JavaVirtualMachines')
  fs.mkdirSync(vmRoot, { recursive: true })
  const before = new Set(fs.readdirSync(vmRoot))
  const tmpFile = path.join(os.tmpdir(), `maozi-jdk-dl-${vendor}-${version}-${Date.now()}.tar.gz`)

  jdkDlAbort = new AbortController()
  try {
    // ===== 下载（三通道自动降级：系统代理 → 直连 → curl；流式进度节流推送） =====
    jdkDlEmit({ stage: 'download', pct: 0, receivedMB: 0, totalMB: 0, message: `连接 ${vendor === 'adoptium' ? 'Eclipse Adoptium' : 'Amazon Corretto'} 下载源…` })
    let received = 0
    let lastEmit = 0
    const via = await downloadViaChannels(url, tmpFile, jdkDlAbort.signal, (r, t, viaName) => {
      received = r
      const now = Date.now()
      if (now - lastEmit > 200) {
        lastEmit = now
        jdkDlEmit({
          stage: 'download',
          pct: t > 0 ? Math.min(99, Math.round((r / t) * 100)) : 0,
          receivedMB: Math.round((r / 1048576) * 10) / 10,
          totalMB: Math.round((t / 1048576) * 10) / 10,
          message: `下载中（${viaName}）`
        })
      }
    })

    // ===== 解压（tar 无细粒度进度，推送阶段态） =====
    jdkDlEmit({ stage: 'extract', pct: 100, receivedMB: Math.round((received / 1048576) * 10) / 10, totalMB: 0, message: '解压安装中…' })
    await exec('/usr/bin/tar', ['-xzf', tmpFile, '-C', vmRoot], { timeout: 180_000 })

    // ===== 统一命名：新出现的目录改名为 <vendor>-<major>.jdk（重装=先删旧目录） =====
    const added = fs.readdirSync(vmRoot).filter(n => !before.has(n))
    const newName = `${vendor === 'adoptium' ? 'temurin' : 'corretto'}-${version}.jdk`
    const newDir = added.find(n => {
      const home = path.join(vmRoot, n, 'Contents', 'Home')
      return fs.existsSync(path.join(home, 'bin', 'java'))
    })
    if (!newDir) throw new Error('解压后未找到 JDK 目录（安装包结构异常）')
    if (newDir !== newName) {
      const old = path.join(vmRoot, newName)
      if (fs.existsSync(old)) fs.rmSync(old, { recursive: true, force: true })
      fs.renameSync(path.join(vmRoot, newDir), old)
    } else if (before.has(newName)) {
      // 同名重装：解压已直接覆盖，无需处理
    }
    jdkDlEmit({ stage: 'done', pct: 100, receivedMB: 0, totalMB: 0, message: `${newName} 安装完成（${via} 通道下载）` })
  } catch (err) {
    let msg = (err as Error).name === 'AbortError' ? '已取消下载' : (err as Error).message
    if (/ERR_CONNECTION|fetch failed|network|ENOTFOUND|ECONNREFUSED|ETIMEDOUT|timed? ?out/i.test(msg) && !msg.includes('已依次尝试')) {
      msg = `网络连接失败（${msg}）——已自动尝试代理/直连/curl 三种通道；请检查 VPN/代理可用性或稍后重试`
    }
    jdkDlEmit({ stage: 'error', pct: 0, receivedMB: 0, totalMB: 0, message: msg })
    throw new Error(msg)
  } finally {
    jdkDlAbort = null
    fs.rmSync(tmpFile, { force: true })
  }
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

/** 读 macOS 系统代理（scutil --proxy）：HTTP / HTTPS / SOCKS(ALL) 的地址端口；非 darwin 或读取失败按全未启用 */
async function getScutilProxy(): Promise<ProxyInfo> {
  if (process.platform !== 'darwin') return { http: '', https: '', all: '' }
  try {
    const { stdout } = await exec('scutil', ['--proxy'], { timeout: 3000 })
    const str = (k: string): string | null => {
      const m = stdout.match(new RegExp(`${k} : (.+)`))
      return m ? m[1].trim() : null
    }
    const url = (scheme: string, enableKey: string, hostKey: string, portKey: string): string => {
      if (!stdout.includes(`${enableKey} : 1`)) return ''
      const host = str(hostKey)
      const port = str(portKey)
      return host && port ? `${scheme}${host}:${port}` : ''
    }
    return {
      http: url('http://', 'HTTPEnable', 'HTTPProxy', 'HTTPPort'),
      https: url('http://', 'HTTPSEnable', 'HTTPSProxy', 'HTTPSPort'),
      all: url('socks5://', 'SOCKSEnable', 'SOCKSProxy', 'SOCKSPort')
    }
  } catch {
    return { http: '', https: '', all: '' }
  }
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
  // 下载进度推送目标窗口（渲染层订阅 sysinfo:jdkDownloadProgress）
  ipcMain.handle('sysinfo:jdkDownload', async (event, version: string, vendor: string) => {
    try {
      jdkDlWin = event.sender
      await downloadJdk(version, vendor)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('sysinfo:jdkDownloadCancel', () => {
    jdkDlAbort?.abort()
    return { ok: true }
  })

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

  ipcMain.handle('sysinfo:network', async () => {
    const proxy = await getScutilProxy()
    return { ok: true, data: { ...getNetwork(), proxy } }
  })

  ipcMain.handle('sysinfo:jdkList', async () => {
    try {
      return { ok: true, data: await listJdks() }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('sysinfo:jdkSwitch', async (_e, home: string) => {
    try {
      await switchJdk(String(home ?? ''))
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 卸载 JDK（用户确认后调用）：用户级直删 / Homebrew brew uninstall / 系统位置管理员授权删除 */
  ipcMain.handle('sysinfo:jdkUninstall', async (_e, home: string) => {
    try {
      await uninstallJdk(String(home ?? ''))
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
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
