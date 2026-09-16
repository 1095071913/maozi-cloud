import {app, BrowserWindow, dialog, ipcMain, shell} from 'electron'
import {execFile, spawn} from 'node:child_process'
import {promisify} from 'node:util'
import fs from 'node:fs'
import net from 'node:net'
import os from 'node:os'
import path from 'node:path'
import crypto from 'node:crypto'
import {clearBinding, getBinding, isDbInitialized, markDbInitialized, parseConfigFile, saveBinding, userDataStateFile} from './store'
import {listConfigs} from '../configs/store'
import {selectPlatform} from '../platform'
import type {EnvFile, EnvVarEntry, HostsEntry} from '../platform/types'
import {getShellPath} from '../system/sysinfo'
import type {ComposeServiceStats, EnvSettingGroup, EnvSettingItem, ProjectBinding} from './types'

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

/** ===== SSH 远程服务器操作 ===== */
interface SshTarget {
  user: string
  host: string
  port: number
  authType: 'password' | 'key'
  password: string
}

function resolveSshTarget(configId: string): SshTarget {
  const entry = listConfigs().find((c) => c.id === configId)
  if (!entry) throw new Error('密钥不存在')
  if (entry.type !== 'Linux') throw new Error('仅支持 Linux 类型密钥')
  if (!entry.address?.trim()) throw new Error('该密钥未填写服务器地址')
  let user = entry.username?.trim() || 'root'
  let host = entry.address.trim()
  let port = 22
  const atIdx = host.indexOf('@')
  if (atIdx > 0) { user = host.slice(0, atIdx); host = host.slice(atIdx + 1) }
  const colonIdx = host.lastIndexOf(':')
  if (colonIdx > 0) {
    const p = parseInt(host.slice(colonIdx + 1), 10)
    if (p > 0 && p < 65536) { port = p; host = host.slice(0, colonIdx) }
  }
  if (!entry.password) throw new Error('该密钥未填写凭据（密码或私钥）')
  return { user, host, port, authType: (entry.authType ?? 'password') as 'password' | 'key', password: entry.password }
}

function writeTempKey(keyContent: string): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'sshkey-'))
  const kp = path.join(dir, 'id_rsa')
  fs.writeFileSync(kp, keyContent + (keyContent.endsWith('\n') ? '' : '\n'), { mode: 0o600 })
  return kp
}

function cleanupTempKey(kp: string): void {
  try { fs.rmSync(path.dirname(kp), { recursive: true, force: true }) } catch { /* */ }
}

/**
 * SSH 连接复用（ControlMaster/ControlPersist）：同目标的多条命令共用一条已认证主连接，
 * 免去每次 TCP 握手 + 密钥交换 + 密码认证（约 250ms/条 → 约 25ms/条）。
 * auto 模式下首条命令成为主连接并后台驻留 10 分钟，socket 失效时自动重建
 */
function sshMuxArgs(t: SshTarget): string[] {
  const key = crypto.createHash('md5').update(`${t.user}@${t.host}:${t.port}`).digest('hex').slice(0, 12)
  return ['-o', 'ControlMaster=auto', '-o', `ControlPath=/tmp/mzc-ssh-${key}`, '-o', 'ControlPersist=600']
}

/** expect 脚本里的 ssh 选项串（mux=true 含连接复用）。流式长命令（tail -f 等）必须 mux=false：
 * 复用主连接会话槽耗尽/主连接假死时客户端无限等待且零输出，且成为主连接的 ssh 会挂到 /dev/null 吞掉全部输出 */
function sshOptsStr(t: SshTarget, mux = true): string {
  const keepAlive = ['-o', 'ServerAliveInterval=15', '-o', 'ServerAliveCountMax=3']
  return ['-o', 'StrictHostKeyChecking=no', '-o', 'ConnectTimeout=10', ...(mux ? sshMuxArgs(t) : keepAlive), '-p', String(t.port)].join(' ')
}

/**
 * ssh 客户端 stderr 噪音行：复用 socket 失效回退（mux_client_* / ControlSocket already exists）、
 * 首次写入 known_hosts 等。expect 会把子进程 stderr 混入输出，这些行会污染 cat 出的文件内容，须过滤
 */
const isSshNoise = (l: string): boolean =>
  /^(mux_client|ControlSocket|Warning: Permanently added)/.test(l.trim())

function sshBaseArgs(t: SshTarget, kp?: string, mux = true): string[] {
  const keepAlive = ['-o', 'ServerAliveInterval=15', '-o', 'ServerAliveCountMax=3']
  const a = [
    '-o', 'StrictHostKeyChecking=no',
    '-o', 'ConnectTimeout=10',
    ...(mux ? sshMuxArgs(t) : keepAlive),
    '-p', String(t.port)
  ]
  if (kp) a.push('-i', kp)
  a.push(t.user + '@' + t.host)
  return a
}

/**
 * 远端命令统一环境前缀：补齐 PATH + source 远端 shell 配置（mvn/docker 等工具链常在用户配置里）。
 * POSIX sh（bash POSIX 模式）下 source 不存在的文件会中止整个命令，必须先 test -f 守卫；
 * 且不能写 [ -f ]——方括号会被 expect 的 Tcl 双引号串当命令替换
 */
const REMOTE_PATH_SETUP =
  'export PATH=$PATH:/usr/local/bin:/usr/bin:/usr/sbin:/usr/local/sbin:/opt/homebrew/bin:$HOME/.local/bin:$HOME/bin; test -f ~/.zshenv && . ~/.zshenv >/dev/null 2>&1; test -f ~/.zshrc && . ~/.zshrc >/dev/null 2>&1; test -f ~/.bashrc && . ~/.bashrc >/dev/null 2>&1; test -f ~/.profile && . ~/.profile >/dev/null 2>&1; true'

/**
 * 远端环境快照缓存（user@host → export 块）：
 * 首次真实 source 远端配置文件并导出 export -p（重配置的 .zshrc 每次实时 source 可能要几百毫秒），
 * TTL 内后续命令直接重放缓照（纯 shell 赋值，几毫秒）。经 sshEnvSave 修改远端配置后主动失效
 */
const remoteEnvCache = new Map<string, { block: string; at: number }>()
const REMOTE_ENV_TTL = 5 * 60_000

/** 无环境前缀的裸执行（remoteEnvSetup 自身使用，避免递归） */
async function sshExecRaw(t: SshTarget, command: string, timeoutMs = 30_000): Promise<string> {
  if (t.authType === 'key') {
    const kp = writeTempKey(t.password)
    try {
      const { stdout } = await exec('ssh', [...sshBaseArgs(t, kp), command], { timeout: timeoutMs, env: process.env })
      return stdout
    } finally { cleanupTempKey(kp) }
  }
  // $ 会被 Tcl 替换、[] 是 Tcl 命令替换，均需转义（JSON.stringify 已处理 " 与 \）
  const tclSafeCmd = JSON.stringify(command).replace(/\$/g, '\\$').replace(/\[/g, '\\[').replace(/\]/g, '\\]')
  const esc = `spawn ssh ${sshOptsStr(t)} ${t.user}@${t.host} ${tclSafeCmd}`
  const script = `set timeout ${Math.ceil(timeoutMs / 1000)}\n${esc}\nexpect {\n  "password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "Password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "yes/no" { send "yes\\r"; exp_continue }\n  eof\n}\ncatch wait result\nexit [lindex $result 3]`
  let stdout: string
  try {
    ;({ stdout } = await exec('expect', ['-c', script], {
      timeout: timeoutMs + 15_000,
      env: { ...process.env, SSH_PASS: t.password }
    }))
  } catch (err) {
    // 报错信息不得回显完整命令——环境前缀里可能携带密钥类变量
    const e = err as { code?: number; killed?: boolean; stdout?: string }
    const tail = String(e.stdout ?? '')
      .split('\n')
      .map((l) => l.replace(/\r/g, ''))
      .filter((l) => l && !l.startsWith('spawn ') && !l.includes('assword:'))
      .slice(-3)
      .join('；')
      .slice(0, 200)
    throw new Error(e.killed ? `远程命令超时（>${timeoutMs / 1000}s）` : `远程命令失败（exit ${e.code ?? '?'}）${tail ? '：' + tail : ''}`)
  }
  return stdout
    .split('\n')
    .map((l) => l.replace(/\r/g, ''))
    .filter((l) => !l.startsWith('spawn ') && !l.includes('password:') && !l.includes('Password:') && !isSshNoise(l))
    .join('\n')
}

/** 进行中的快照构建（user@host → Promise）：页面加载并发触发多条远程命令时共享同一次构建，避免惊群 */
const remoteEnvPending = new Map<string, Promise<string>>()

/** 取远端环境前缀：TTL 内重放缓照，过期则真实 source 一遍并重建快照 */
async function remoteEnvSetup(t: SshTarget): Promise<string> {
  const key = `${t.user}@${t.host}:${t.port}`
  const hit = remoteEnvCache.get(key)
  if (hit && Date.now() - hit.at < REMOTE_ENV_TTL) return hit.block
  // 并发去重：冷启动时页面加载的十余条命令同时发现缓存为空，若各自构建，
  // source 重配置会在远端串行排队（每次数百毫秒），整体加载慢数秒
  const pending = remoteEnvPending.get(key)
  if (pending) return pending
  const build = (async (): Promise<string> => {
    try {
      // 末尾单独捕获 $PATH：zsh 的 export -p 输出 export -T PATH path=( ... )，按 NAME= 过滤会丢失 PATH
      const out = await sshExecRaw(t, `${REMOTE_PATH_SETUP} && export -p; printf '__MZPATH__%s' "$PATH"`, 20_000)
      const idx = out.indexOf('__MZPATH__')
      const envPart = idx >= 0 ? out.slice(0, idx) : out
      const pathVal = idx >= 0 ? out.slice(idx + '__MZPATH__'.length).trim() : ''
      // 单行拼接：多行命令会让 expect 的 spawn 回显折行，第二行起泄漏进命令输出
      const block = envPart
        .split('\n')
        .filter((l) => /^export [A-Za-z_][A-Za-z0-9_]*=/.test(l) && !/^export (PWD|OLDPWD|SHLVL|_|SSH_[A-Z_]+|PIPESTATUS|HISTCMD)=/.test(l))
        .join('; ')
      const pathExport = pathVal ? `export PATH='${pathVal.replace(/'/g, "'\\''")}'` : ''
      const final = [block, pathExport, 'true'].filter(Boolean).join('; ')
      remoteEnvCache.set(key, { block: final, at: Date.now() })
      return final
    } finally {
      remoteEnvPending.delete(key)
    }
  })()
  remoteEnvPending.set(key, build)
  return build
}

async function sshExec(t: SshTarget, command: string, timeoutMs = 30_000): Promise<string> {
  const fullCmd = `${await remoteEnvSetup(t)} && ${command}`
  return sshExecRaw(t, fullCmd, timeoutMs)
}


/** 检测远程服务器可用的 compose 命令（v2 插件 docker compose / v1 独立 docker-compose）。
 * 服务器的 compose 形态会话内不会变，按目标缓存，避免每次轮询都多一次 SSH 往返 */
const remoteComposeCache = new Map<string, string>()
async function remoteComposeCmd(t: SshTarget): Promise<string> {
  const key = `${t.user}@${t.host}:${t.port}`
  const hit = remoteComposeCache.get(key)
  if (hit) return hit
  try {
    const out = await sshExec(t, 'docker compose version 2>/dev/null && echo __V2__ || echo __V1__', 10_000)
    const bin = out.includes('__V2__') ? 'docker compose' : 'docker-compose'
    remoteComposeCache.set(key, bin)
    return bin
  } catch {
    return 'docker-compose'
  }
}

async function sshListDir(t: SshTarget, dirPath: string): Promise<Array<{ name: string; isDir: boolean }>> {
  const out = await sshExec(t, `ls -1ap "${dirPath}" 2>/dev/null || echo "__ERROR__"`, 15_000)
  if (out.includes('__ERROR__')) throw new Error(`无法访问目录 ${dirPath}`)
  return out.split('\n').filter((l) => l.trim()).map((l) => ({ name: l.replace(/\/$/, ''), isDir: l.endsWith('/') })).filter((e) => e.isDir && !e.name.startsWith('.'))
}

async function sshReadFile(t: SshTarget, filePath: string): Promise<string> {
  const out = await sshExec(t, `cat "${filePath}" 2>/dev/null || echo "__NOT_FOUND__"`, 15_000)
  if (out.includes('__NOT_FOUND__')) throw new Error(`远程文件不存在：${filePath}`)
  return out
}

async function sshCheckGit(t: SshTarget): Promise<boolean> {
  try {
    const out = await sshExec(t, 'which git 2>/dev/null', 10_000)
    return out.trim() !== '' && !out.includes('not found')
  } catch { return false }
}

async function sshStream(
  sender: Electron.WebContents,
  channel: string,
  t: SshTarget,
  command: string,
  opts: { timeoutMs: number; sid?: string }
): Promise<void> {
  const kp = t.authType === 'key' ? writeTempKey(t.password) : undefined
  // 远端非登录 shell 缺少工具链 PATH（mvn/docker/docker-compose 等可能 command not found），与 sshExec 用同一环境前缀
  const fullCmd = `${await remoteEnvSetup(t)} && ${command}`
  const send = (kind: 'line' | 'update', text: string): void => {
    const clean = text.replace(/\x1b\[[0-9;]*[A-Za-z]/g, '').replace(/\s+$/, '')
    // 过滤 SSH 密码提示行（(user@host) Password:）——expect 自动应答的提示不应出现在业务日志里
    if (clean && !isSshNoise(clean) && !/[Pp]assword:\s*$/.test(clean) && !sender.isDestroyed())
      sender.send(channel, { kind, text: clean, sid: opts.sid })
  }
  try {
    let bin = 'ssh'
    let args: string[]
    let env: Record<string, string> | undefined
    // 流式长命令走独占连接（不复用）：tail -f / logs -f 可能跑数小时，复用主连接一旦假死输出永久静默
    if (t.authType === 'key') {
      args = [...sshBaseArgs(t, kp, false), fullCmd]
    } else {
      bin = 'expect'
      const tclSafe = JSON.stringify(fullCmd).replace(/\$/g, '\\$').replace(/\[/g, '\\[').replace(/\]/g, '\\]')
      const esc = `spawn ssh ${sshOptsStr(t, false)} ${t.user}@${t.host} ${tclSafe}`
      // catch wait + exit：把 ssh 的真实退出码透传出来，命令失败不再假成功
      args = ['-c', `set timeout -1\n${esc}\nexpect {\n  "password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "Password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "yes/no" { send "yes\\r"; exp_continue }\n  eof\n}\ncatch wait result\nexit [lindex $result 3]`]
      env = { SSH_PASS: t.password }
    }
    await new Promise<void>((resolve, reject) => {
      const child = spawn(bin, args, { detached: true, env: env ? { ...process.env, ...env } : process.env })
      // 注册会话：停止按钮（stopScript）才能杀到远程流式进程，且中断不误报失败
      if (opts.sid) runningProcs.set(opts.sid, { child, stopRequested: false })
      let carry = ''
      const feed = (chunk: Buffer): void => {
        carry += chunk.toString('utf8')
        let nl: number
        while ((nl = carry.indexOf('\n')) >= 0) {
          let seg = carry.slice(0, nl)
          carry = carry.slice(nl + 1)
          // expect 的 pty 行尾是 \r\n：先去掉行尾 \r 再按 \r 切（覆盖式进度取最后一段），否则每行都取到空串
          if (seg.endsWith('\r')) seg = seg.slice(0, -1)
          const parts = seg.split('\r')
          send('line', parts[parts.length - 1])
        }
      }
      child.stdout.on('data', feed)
      child.stderr.on('data', feed)
      const timer = setTimeout(() => { try { if (child.pid) process.kill(-child.pid, 'SIGKILL') } catch { /* */ } }, opts.timeoutMs)
      child.on('error', (e) => {
        clearTimeout(timer)
        if (opts.sid) runningProcs.delete(opts.sid)
        reject(new Error(`无法执行: ${e.message}`))
      })
      child.on('close', (code) => {
        clearTimeout(timer)
        if (carry.trim()) send('line', carry)
        const stopped = opts.sid ? (runningProcs.get(opts.sid)?.stopRequested ?? false) : false
        if (opts.sid) runningProcs.delete(opts.sid)
        if (stopped) reject(new Error('已手动中断'))
        else if (code === 0) resolve(); else reject(new Error(`exit ${code}`))
      })
    })
  } finally {
    if (kp) cleanupTempKey(kp)
  }
}

/** ===== 项目执行层：根据绑定类型自动路由本地 / SSH 远程 ===== */

interface ProjectCtx {
  /** 项目根目录（本地路径或远程路径） */
  root: string
  /** 是否远程绑定 */
  isRemote: boolean
  /** SSH 目标（远程绑定时非空） */
  ssh?: SshTarget
}

/** 获取当前项目上下文（本地或远程） */
function getProjectCtx(): ProjectCtx {
  const b = getBinding()
  if (!b) throw new Error('尚未绑定项目')
  if (b.remote) {
    const ssh = resolveSshTarget(b.remote.configId)
    return { root: b.path, isRemote: true, ssh }
  }
  return { root: b.path, isRemote: false }
}

/** 读取项目内文件（本地 fs 或 SSH cat） */
async function readProjFile(relPath: string): Promise<string> {
  const ctx = getProjectCtx()
  const abs = path.join(ctx.root, relPath)
  if (ctx.isRemote && ctx.ssh) {
    // 读文件：纯 SSH cat（expect 包装），完全绕过 sshExec 的 PATH/source（零干扰）
    const t = ctx.ssh
    const tclSafe = JSON.stringify(`cat "${abs}"`).replace(/\$/g, '\\$').replace(/\[/g, '\\[').replace(/\]/g, '\\]')
    const esc = `spawn ssh ${sshOptsStr(t)} ${t.user}@${t.host} ${tclSafe}`
    const script = `set timeout 30\n${esc}\nexpect {\n  "password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "Password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "yes/no" { send "yes\\r"; exp_continue }\n  eof\n}\nexpect eof`
    const { stdout: expOut } = await exec('expect', ['-c', script], {
      timeout: 25_000,
      env: { ...process.env, SSH_PASS: t.password }
    })
    const content = expOut
      .split('\n')
      .map((l) => l.replace(/\r/g, ''))
      .filter((l) => !l.startsWith('spawn ') && !l.includes('password:') && !l.includes('Password:') && !isSshNoise(l))
      .join('\n')
      .replace(/^\n+/, '')
      .replace(/\n+$/, '')
    if (!content) throw new Error(`远程文件为空或读取失败：${relPath}`)
    return content + '\n'
  }
  if (!fs.existsSync(abs)) throw new Error(`未找到 ${relPath}`)
  return fs.readFileSync(abs, 'utf8')
}

/** 检查项目内文件是否存在 */
async function projFileExists(relPath: string): Promise<boolean> {
  const ctx = getProjectCtx()
  const abs = path.join(ctx.root, relPath)
  if (ctx.isRemote && ctx.ssh) {
    try {
      await sshExec(ctx.ssh, `test -f "${abs}" && echo YES || echo NO`, 10_000)
      return true // sshReadFile 会 throw 如果不存在
    } catch {
      return false
    }
  }
  return fs.existsSync(abs)
}

/** 在项目环境中执行命令（本地 exec 或 SSH） */
async function projExec(cmd: string, args: string[], opts: { cwd?: string; timeout: number; maxBuffer?: number }): Promise<{ stdout: string }> {
  const ctx = getProjectCtx()
  const cwd = opts.cwd ?? ctx.root
  if (ctx.isRemote && ctx.ssh) {
    // shell 元字符（空格、管道、分号等）需要引号包裹
    const needsQuote = (a: string): boolean => /[\s|;&<>(){}$`"']/.test(a)
    const full = `cd "${cwd}" && ${cmd} ${args.map((a) => (needsQuote(a) ? `'${a}'` : a)).join(' ')}`
    const stdout = await sshExec(ctx.ssh, full, opts.timeout)
    return { stdout }
  }
  const pathEnv = await getShellPath()
  return await exec(cmd, args, {
    timeout: opts.timeout,
    cwd,
    maxBuffer: opts.maxBuffer,
    env: { ...process.env, PATH: pathEnv }
  })
}

/** 在项目环境中流式执行命令（本地 streamProcess 或 SSH 流），日志推送到 channel */
async function projStream(
  sender: Electron.WebContents,
  channel: string,
  cmd: string,
  args: string[],
  opts: { cwd?: string; timeoutMs: number; sid?: string; extraEnv?: Record<string, string>; stdinData?: string; sshEnvPrefix?: string }
): Promise<void> {
  const ctx = getProjectCtx()
  const cwd = opts.cwd ?? ctx.root
  if (ctx.isRemote && ctx.ssh) {
    // extraEnv 无法跨 SSH 生效，远程环境注入走 sshEnvPrefix（export 前缀）
    const full = `${opts.sshEnvPrefix ?? ''}cd "${cwd}" && ${cmd} ${args.join(' ')}`
    await sshStream(sender, channel, ctx.ssh, full, { timeoutMs: opts.timeoutMs, sid: opts.sid })
    return
  }
  await streamProcess(sender, channel, cmd, args, {
    cwd,
    timeoutMs: opts.timeoutMs,
    sid: opts.sid,
    extraEnv: opts.extraEnv,
    stdinData: opts.stdinData
  })
}

/** 项目内目录的绝对路径 */
/** yml 文件内容缓存（key=绝对路径，远程绑定时避免反复 SSH cat）；绑定切换时清空 */
const ymlCache = new Map<string, string>()

function clearYmlCache(): void {
  ymlCache.clear()
}

function readProjFileCached(relPath: string): Promise<string> {
  const key = relPath
  const cached = ymlCache.get(key)
  if (cached !== undefined) return Promise.resolve(cached)
  return readProjFile(relPath).then((content) => {
    ymlCache.set(key, content)
    return content
  })
}

function projPath(relPath: string): string {
  const ctx = getProjectCtx()
  return path.join(ctx.root, relPath)
}

/** 项目内文件是否存在（同步版本，用于快速判断） */
function projFileExistsSync(relPath: string): boolean {
  const b = getBinding()
  if (!b) return false
  if (b.remote) return true // 远程绑定无法同步检查，乐观返回 true
  return fs.existsSync(path.join(b.path, relPath))
}

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
async function readAppServices(bPath: string, variant: AppSvcVariant): Promise<Array<{ name: string; file: 'admin' | 'services' }>> {
  const def = APP_SVC_DEFS[variant]
  const out: Array<{ name: string; file: 'admin' | 'services' }> = []
  const read = async (fileName: string, tag: 'admin' | 'services'): Promise<void> => {
    const ymlRel = path.join(def.dir, fileName)
    try {
      const ymlContent = await readProjFileCached(ymlRel)
      for (const name of parseComposeServices(ymlContent)) out.push({ name, file: tag })
    } catch { /* 文件不存在时跳过 */ }
  }
  await read(def.adminFile, 'admin')
  await read(def.servicesFile, 'services')
  return out
}

/** 变体所有服务的容器名映射（服务名 → 容器名，读取两个 yml 的 container_name） */
async function appSvcContainerNames(bPath: string, variant: AppSvcVariant): Promise<Record<string, string>> {
  const def = APP_SVC_DEFS[variant]
  const map: Record<string, string> = {}
  for (const fileName of [def.servicesFile, def.adminFile]) {
    try {
      const content = await readProjFileCached(path.join(def.dir, fileName))
      Object.assign(map, parseServiceContainerNames(content))
    } catch { /* 文件不存在时跳过 */ }
  }
  // 未设 container_name 的服务回退为服务名本身（compose 默认行为）
  for (const e of await readAppServices(bPath, variant)) {
    if (!map[e.name]) map[e.name] = e.name
  }
  return map
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

/** 代理地址 TCP 可达性探测（1.5s）：GUI 读到的系统代理可能是 VPN 退出后的残留配置 */
function probeProxyReachable(url: string): Promise<boolean> {
  return new Promise((resolve) => {
    try {
      const u = new URL(url)
      const port = Number(u.port || (u.protocol === 'socks5:' ? 1080 : 80))
      const s = net.connect({ host: u.hostname, port, timeout: 1500 })
      s.once('connect', () => {
        s.destroy()
        resolve(true)
      })
      s.once('error', () => resolve(false))
      s.once('timeout', () => {
        s.destroy()
        resolve(false)
      })
    } catch {
      resolve(false)
    }
  })
}

/**
 * 远端代理（VPN）三级探测：shell 环境变量 → git 全局配置 http.proxy → macOS 系统代理（scutil，
 * ClashX 等只设系统代理不设环境变量）。返回可直接拼进 SSH 命令的 export 前缀与代理地址，
 * 检测失败不阻塞、按直连处理
 */
async function detectRemoteProxy(t: SshTarget): Promise<{ exports: string; proxy: string }> {
  try {
    const detect =
      'p="${https_proxy:-${http_proxy:-${all_proxy:-}}}"; ' +
      'if [ -z "$p" ]; then p=$(git config --get http.proxy 2>/dev/null); fi; ' +
      'if [ -z "$p" ] && command -v scutil >/dev/null 2>&1; then ' +
      'p=$(scutil --proxy 2>/dev/null | awk \'/HTTPSEnable : 1/{e=1} /HTTPSProxy :/{h=$3} /HTTPSPort :/{pt=$3} END{if(e&&h!="")printf "http://%s:%s",h,pt}\'); ' +
      'fi; printf \'%s\' "$p"'
    const proxy = (await sshExec(t, detect, 15_000)).trim()
    if (proxy) {
      return { exports: `export https_proxy=${JSON.stringify(proxy)} http_proxy=${JSON.stringify(proxy)}; `, proxy }
    }
  } catch {
    /* 检测失败按直连 */
  }
  return { exports: '', proxy: '' }
}

/**
 * git 网络操作（ls-remote / pull / fetch）的代理解析，不发日志：本地绑定读本机系统代理
 * 并探测可达性，远程绑定在服务器侧三级探测。检出可用代理返回注入用环境与代理地址，否则直连
 */
async function resolveGitProxy(b: ProjectBinding): Promise<{ extraEnv?: Record<string, string>; sshEnvPrefix?: string; proxy: string }> {
  if (b.remote) {
    const { exports, proxy } = await detectRemoteProxy(resolveSshTarget(b.remote.configId))
    return proxy ? { sshEnvPrefix: exports, proxy } : { proxy: '' }
  }
  const proxyEnv = await getProxyEnv()
  const tip = proxyEnv.https_proxy ?? proxyEnv.http_proxy ?? proxyEnv.all_proxy
  if (tip && (await probeProxyReachable(tip))) {
    return { extraEnv: proxyEnv, proxy: tip }
  }
  return { proxy: '' }
}

/**
 * git 网络操作（pull / fetch）前的代理就绪检查：解析代理并向日志打提示，返回注入用环境；
 * 分支列表拉取等不发日志的场景直接用 resolveGitProxy
 */
async function prepareGitProxy(
  sender: Electron.WebContents,
  channel: string,
  sid: string,
  b: ProjectBinding
): Promise<{ extraEnv?: Record<string, string>; sshEnvPrefix?: string }> {
  const send = (text: string): void => {
    if (!sender.isDestroyed()) sender.send(channel, { kind: 'line', text, sid })
  }
  const r = await resolveGitProxy(b)
  if (r.proxy) {
    send(b.remote ? `▶ 检测到远端代理（VPN）：${r.proxy}，git 将经代理拉取` : `▶ 已启用系统代理 ${r.proxy}，git 将经代理拉取`)
  } else {
    send(b.remote ? '▶ 未检测到远端代理，直连拉取（缓慢或卡住请先在服务器开启 VPN / 配置 http_proxy）' : '▶ 无可用系统代理，直连拉取（缓慢或卡住请先开启 VPN）')
  }
  return { extraEnv: r.extraEnv, sshEnvPrefix: r.sshEnvPrefix }
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
    // 浅克隆减小传输量；低速熔断（<1KB/s 持续 60s 判定链路假死中止）——GitHub 连接静默假死时 git 会无限挂起
    // 注意用 streamProcess 而非 projStream：本地拉取发生在绑定之前，projStream 依赖项目绑定上下文会报"尚未绑定项目"
    await streamProcess(
      sender,
      'projects:cloneLog',
      'git',
      ['clone', '--progress', '--depth', '1', '--single-branch', '-c', 'http.lowSpeedLimit=1024', '-c', 'http.lowSpeedTime=60', authedUrl, target],
      {
        timeoutMs: 10 * 60_000,
        extraEnv: proxyEnv
      }
    )
  } catch (err) {
    // 清理半成品目录：git clone 中止会留下不完整目录，不清理则重试直接报"目标位置已存在"
    try {
      fs.rmSync(target, { recursive: true, force: true })
    } catch {
      /* 清理失败不掩盖原始错误 */
    }
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
  const { stdout } = await projExec('docker', ['network', 'ls', '--format', '{{.Name}}'], { timeout: 15_000 })
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

/** docker exec 执行 mysql 查询（本地 exec 或 SSH 远程，返回 stdout） */
async function mysqlQuery(container: string, pwd: string, sql: string, timeoutMs = 20_000): Promise<string> {
  const { stdout } = await projExec('docker', ['exec', container, 'mysql', '-uroot', `-p${pwd}`, '-N', '-B', '-e', sql], {
    timeout: timeoutMs
  })
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
  const { stdout } = await projExec('docker', ['compose', 'ps', '--all', '--format', 'json'], {
    cwd: dir,
    timeout: 15_000
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

/** 解析 compose 中所有服务的 container_name 映射（服务名 → 容器名；未设则回退服务名） */
function parseServiceContainerNames(text: string): Record<string, string> {
  const map: Record<string, string> = {}
  let curSvc = ''
  for (const raw of text.split(/\r?\n/)) {
    if (!raw.trim() || /^\s*#/.test(raw)) continue
    const mSvc = raw.match(/^ {2}([A-Za-z0-9_.-]+):\s*(?:#.*)?$/)
    if (mSvc) {
      curSvc = mSvc[1]
      continue
    }
    if (!curSvc) continue
    if (/^\S/.test(raw)) break
    const mName = raw.match(/^ {4}container_name:\s*(\S+)/)
    if (mName) map[curSvc] = mName[1]
  }
  return map
}

/** 从日志行前缀解析时间戳：[ 2026-09-15 01:25:31(:539) ] → 毫秒；解析失败返回 0（排序时排最后） */
function parseLogTs(line: string): number {
  const m = line.match(/\[ (\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})(?::(\d{3}))? \]/)
  if (!m) return 0
  const ms = m[7] ? parseInt(m[7], 10) : 0
  return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6], ms).getTime()
}

/** 探测容器内 LOG_FILE 环境变量：返回容器名与日志文件路径（容器未运行或未设变量时 logFile 为空串）。
 * 走 projExec 本地/远程路由——远程绑定时必须查远端 docker，本地 docker 查不到 */
async function probeContainerLogFile(
  dir: string,
  fileArgs: string[],
  service: string
): Promise<{ container: string; logFile: string }> {
  try {
    const { stdout } = await projExec('docker', ['compose', ...fileArgs, 'ps', service, '--format', 'json'], {
      cwd: dir,
      timeout: 15_000
    })
    const pairs = parseComposePsPairs(stdout)
    if (pairs.length === 0) return { container: '', logFile: '' }
    const container = pairs[0].name
    // 容器运行中时查询 LOG_FILE；printenv 未找到变量会 exit 1 → catch 返回空
    const r = await projExec('docker', ['exec', container, 'printenv', 'LOG_FILE'], { timeout: 10_000 })
    const logFile = r.stdout.trim()
    return logFile ? { container, logFile } : { container, logFile: '' }
  } catch {
    return { container: '', logFile: '' }
  }
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

/** docker stats --format 单行：Name:CPUPerc:MemUsed/MemLimit */
function parseStatsLine(
  line: string
): { name: string; cpuPercent: number; memUsed: number; memLimit: number } | null {
  const parts = line.trim().split(':')
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
  // 仅缓存成功结果；失败不烙印——预取已在启动瞬间调用，瞬时 SSH 失败若被记住会毒化整个会话
  if (composeOk === true) return
  const b = getBinding()
  if (b?.remote) {
    // 远程绑定：SSH 检测，同时支持 v2 插件（docker compose）和 v1 独立命令（docker-compose）
    const target = resolveSshTarget(b.remote.configId)
    try {
      await sshExec(target, 'docker compose version 2>/dev/null || docker-compose --version 2>/dev/null', 15_000)
    } catch {
      throw new Error('远程服务器 docker / docker-compose 不可用，请先安装')
    }
  } else {
    // 本地绑定：检测本机 docker
    const pathEnv = await getShellPath()
    await exec('docker', ['compose', 'version'], { timeout: 10_000, env: { ...process.env, PATH: pathEnv } })
  }
  composeOk = true
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
const runningProcs = new Map<string, { child?: ReturnType<typeof spawn>; stopRequested: boolean }>()

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
        let seg = carry.slice(0, nl)
        carry = carry.slice(nl + 1)
        // CRLF 行尾先去掉 \r，否则按 \r 取最后一段会得到空串（完整行取最后一个 \r 段作为最终内容）
        if (seg.endsWith('\r')) seg = seg.slice(0, -1)
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

/** UI 状态持久化文件：userData/.ui-state.json（同 bookmarks.json 一类本地运行时数据，仓库之外） */
function uiStateFile(): string {
  return userDataStateFile('.ui-state.json')
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
    await projStream(event.sender, channel, 'docker', args, { cwd: dir, timeoutMs: 10 * 60_000, sid })
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
      clearYmlCache()
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
      clearYmlCache()
      return { ok: true, data: binding }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('projects:unbind', () => {
    try {
      clearBinding()
      clearYmlCache()
      composeOk = null
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

  /** ===== 项目 git 管理（绑定后）：仓库检测 / 分支列表 / 拉取 / 切换分支 ===== */

  /** git 通用低速熔断参数：GitHub 链路静默假死时快速失败而不是无限挂起 */
  const GIT_STALL_ARGS = ['-c', 'http.lowSpeedLimit=1024', '-c', 'http.lowSpeedTime=60']

  /** 项目是否 git 仓库 + 当前分支（非仓库 / 未装 git 时 isRepo=false，不报错） */
  ipcMain.handle('projects:gitInfo', async () => {
    const data = { isRepo: false, branch: '' }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        const out = await sshExec(t, `cd ${JSON.stringify(b.path)} && git branch --show-current`, 10_000)
        const branch = out.trim().split(/\r?\n/).filter(Boolean).pop() ?? ''
        return { ok: true, data: { isRepo: branch !== '', branch } }
      }
      const pathEnv = await getShellPath()
      const { stdout } = await exec('git', ['branch', '--show-current'], {
        cwd: b.path,
        timeout: 10_000,
        env: { ...process.env, PATH: pathEnv }
      })
      const branch = stdout.trim()
      return { ok: true, data: { isRepo: branch !== '', branch } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data }
    }
  })

  /** 远程分支列表（git ls-remote --heads，需网络；浅克隆本地无其他分支引用也能列出全部） */
  ipcMain.handle('projects:gitBranches', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      let stdout = ''
      // ls-remote 同样访问 GitHub：先解析 VPN 代理（静默不打日志），可用则注入
      const proxy = await resolveGitProxy(b)
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        stdout = await sshExec(t, `${proxy.sshEnvPrefix ?? ''}cd ${JSON.stringify(b.path)} && git ls-remote --heads origin`, 30_000)
      } else {
        const pathEnv = await getShellPath()
        const r = await exec('git', ['ls-remote', '--heads', 'origin'], {
          cwd: b.path,
          timeout: 30_000,
          env: { ...process.env, PATH: pathEnv, ...(proxy.extraEnv ?? {}) }
        })
        stdout = r.stdout
      }
      const branches = stdout
        .split(/\r?\n/)
        .map((l) => l.split('\t')[1] ?? '')
        .filter((ref) => ref.startsWith('refs/heads/'))
        .map((ref) => ref.slice('refs/heads/'.length))
        .filter(Boolean)
      return { ok: true, data: { branches } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { branches: [] } }
    }
  })

  /** 拉取代码（git pull），输出走 projects:scriptLog（带 sid） */
  ipcMain.handle('projects:gitPull', async (event, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      // GitHub 直连易链路假死：拉取前先看 VPN 代理是否可用，可用则注入 git 代理环境
      const proxy = await prepareGitProxy(event.sender, 'projects:scriptLog', sid, b)
      await projStream(event.sender, 'projects:scriptLog', 'git', [...GIT_STALL_ARGS, 'pull', '--progress'], {
        cwd: b.path,
        timeoutMs: 10 * 60_000,
        sid,
        extraEnv: proxy.extraEnv,
        sshEnvPrefix: proxy.sshEnvPrefix
      })
      return { ok: true }
    } catch (err) {
      const msg = (err as Error).message
      return { ok: false, error: msg === '已手动中断' ? msg : `git pull 失败：${msg}，详见日志` }
    }
  })

  /** 切换分支：先 fetch 目标分支（浅克隆兼容）再 checkout；输出走 projects:scriptLog */
  ipcMain.handle('projects:gitCheckout', async (event, branchArg: string, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const branch = String(branchArg ?? '').trim()
      if (!/^[A-Za-z0-9._/-]{1,100}$/.test(branch) || branch.includes('..') || branch.startsWith('/') || branch.endsWith('/')) {
        throw new Error('非法的分支名')
      }
      // 克隆带 --depth 1 --single-branch，refspec 只含主分支：目标分支不在 refspec 内时，
      // fetch 不建远端跟踪引用，checkout 的 DWIM/--track/pull 又都按 refspec 反查而落空，
      // 便报 pathspec 不匹配。先把 refspec 放开为全分支（幂等，此后任意分支可切、pull 可用），
      // 再显式 refspec 取回目标分支后普通 checkout：已有本地分支等价普通切换（不动本地提交），
      // 首次切换 DWIM 建分支并自动设上游
      const fullRefspec = '+refs/heads/*:refs/remotes/origin/*'
      if (b.remote) {
        await sshExec(resolveSshTarget(b.remote.configId), `cd ${JSON.stringify(b.path)} && git config remote.origin.fetch ${JSON.stringify(fullRefspec)}`, 15_000)
      } else {
        const pathEnv = await getShellPath()
        await exec('git', ['config', 'remote.origin.fetch', fullRefspec], {
          cwd: b.path,
          timeout: 15_000,
          env: { ...process.env, PATH: pathEnv }
        })
      }
      // 取回目标分支走 GitHub：先看 VPN 代理是否可用，可用则注入 git 代理环境（checkout 本身无网络）
      const proxy = await prepareGitProxy(event.sender, 'projects:scriptLog', sid, b)
      await projStream(event.sender, 'projects:scriptLog', 'git', [...GIT_STALL_ARGS, 'fetch', '--progress', '--depth', '1', 'origin', `+refs/heads/${branch}:refs/remotes/origin/${branch}`], {
        cwd: b.path,
        timeoutMs: 10 * 60_000,
        sid,
        extraEnv: proxy.extraEnv,
        sshEnvPrefix: proxy.sshEnvPrefix
      })
      await projStream(event.sender, 'projects:scriptLog', 'git', ['checkout', '--progress', branch], {
        cwd: b.path,
        timeoutMs: 60_000,
        sid
      })
      return { ok: true }
    } catch (err) {
      const msg = (err as Error).message
      return { ok: false, error: msg === '已手动中断' ? msg : `切换分支失败：${msg}，详见日志` }
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
        await projStream(event.sender, 'projects:scriptLog', 'bash', [script], {
          cwd: b.path,
          timeoutMs: 30 * 60_000,
          sid,
          extraEnv: proxyEnv
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
  ipcMain.handle('projects:composeServices', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const ymlContent = await readProjFile(path.join(COMPOSE_DIR, 'docker-compose.yml'))
      return { ok: true, data: parseComposeServices(ymlContent) }
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
      const dir = projPath(COMPOSE_DIR)
      if (!(await projFileExists(path.join(COMPOSE_DIR, 'docker-compose.yml')))) {
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
          await projStream(event.sender, 'projects:scriptLog', 'docker', args, {
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
      const dir = projPath(ADMIN_COMPOSE_DIR)
      if (!(await projFileExists(path.join(ADMIN_COMPOSE_DIR, ADMIN_COMPOSE_FILE)))) {
        throw new Error(`未找到 ${ADMIN_COMPOSE_DIR}/${ADMIN_COMPOSE_FILE}`)
      }
      const args =
        action === 'start'
          ? ['compose', '-f', ADMIN_COMPOSE_FILE, 'up', '-d']
          : ['compose', '-f', ADMIN_COMPOSE_FILE, 'down']
      if (!event.sender.isDestroyed()) {
        event.sender.send('projects:scriptLog', {
          kind: 'line',
          text: `▶ docker ${args.join(' ')}`,
          sid
        })
      }
      try {
        await projStream(event.sender, 'projects:scriptLog', 'docker', args, {
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
      const defs = parseProjectHosts(await readProjFile(HOSTS_FILE))
      const ctx = getProjectCtx()
      if (ctx.isRemote && ctx.ssh) {
        // 远程绑定：SSH 读远程 /etc/hosts
        const remoteHosts = await sshExec(ctx.ssh, 'cat /etc/hosts', 10_000)
        const existing = new Set(
          remoteHosts.split('\n').filter((l) => {
            const t = l.trim()
            return t && !t.startsWith('#') && /^\S+\s+/.test(t)
          }).map((l) => {
            const m = l.match(/^(\S+)\s+(.+)$/)
            return m ? `${m[1]} ${m[2].trim().split(/\s+/).join(' ')}` : ''
          }).filter(Boolean)
        )
        const missing = defs.filter((d) => !existing.has(`${d.ip} ${d.domains}`))
        return {
          ok: true,
          data: { total: defs.length, missing: missing.length, initialized: missing.length === 0 }
        }
      }
      // 本地绑定：读本地 /etc/hosts
      const platform = selectPlatform()
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
        await projStream(event.sender, 'projects:scriptLog', 'docker', ['network', 'create', name], {
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
      send(`▶ 读取 ${HOSTS_FILE}`)
      const defs = parseProjectHosts(await readProjFile(HOSTS_FILE))
      send(`  共 ${defs.length} 条映射`)
      const ctx = getProjectCtx()
      if (ctx.isRemote && ctx.ssh) {
        // 远程绑定：SSH 操作远程 /etc/hosts
        const remoteHosts = await sshExec(ctx.ssh, 'cat /etc/hosts', 10_000)
        const existing = new Set(
          remoteHosts.split('\n').filter((l) => {
            const t = l.trim()
            return t && !t.startsWith('#') && /^\S+\s+/.test(t)
          }).map((l) => {
            const m = l.match(/^(\S+)\s+(.+)$/)
            return m ? `${m[1]} ${m[2].trim().split(/\s+/).join(' ')}` : ''
          }).filter(Boolean)
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
          send('✓ 全部映射均已写入远程 hosts，无需初始化')
          return { ok: true }
        }
        send('▶ SSH 写入远程 /etc/hosts（自动备份原文件）')
        // 非 root 用户无权直接写 /etc/hosts，经 sudo 提权；-p password: 让 sudo 提示词与 expect 应答模式匹配
        const priv = (cmd: string): string => (ctx.ssh?.user === 'root' ? cmd : `sudo -S -p password: ${cmd}`)
        // 备份
        await sshExec(ctx.ssh, priv('cp /etc/hosts /etc/hosts.bak.' + Date.now()), 10_000)
        // 逐条追加（sudo 下重定向必须发生在 sh -c 内，否则 >> 仍按调用方用户权限执行）
        for (const d of missing) {
          await sshExec(ctx.ssh, priv(`sh -c 'echo ${d.ip}    ${d.domains} >> /etc/hosts'`), 10_000)
          send(`✓ 已写入 ${d.ip} ${d.domains}`)
        }
        send('✓ 远程 hosts 初始化完成')
        return { ok: true }
      }
      // 本地绑定：写本地 /etc/hosts
      const platform = selectPlatform()
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

  /** UI 状态（Tab 选中 等）：userData/.ui-state.json 持久化 */
  ipcMain.handle('projects:sshEnvSave', async (_e, params: { key: string; value: string; fileId: string }) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (!b.remote) throw new Error('当前为本地绑定')
      const ctx = getProjectCtx()
      if (!ctx.ssh) throw new Error('SSH 连接不可用')
      const key = String(params.key ?? '').trim()
      const value = String(params.value ?? '').trim()
      if (!key || !/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) throw new Error('变量名不合法')
      if (!value) throw new Error('变量值不能为空')
      const REMOTE_ENV_FILES: Record<string, string> = {
        zshrc: '~/.zshrc', zshenv: '~/.zshenv', zprofile: '~/.zprofile',
        bashrc: '~/.bashrc', bash_profile: '~/.bash_profile', profile: '~/.profile'
      }
      const filePath = REMOTE_ENV_FILES[params.fileId]
      if (!filePath) throw new Error('未知的远程配置文件: ' + params.fileId)
      const ts = Date.now()
      await sshExec(ctx.ssh, `cp ${filePath} ${filePath}.bak.${ts} 2>/dev/null; true`, 10_000)
      const existing = await sshExec(ctx.ssh, `grep -c '^export ${key}=' ${filePath} 2>/dev/null || echo 0`, 10_000)
      const count = parseInt(existing.trim()) || 0
      if (count > 0) {
        // 已有定义则替换：grep -v 剔除旧行 + 追加新行 + mv 回写。
        // 不用 sed 的 c\ 单行写法——macOS BSD sed 不支持（GNU sed 专属），会 exit 1
        const safeVal = value.replace(/'/g, "'\\''")
        await sshExec(ctx.ssh, `grep -v '^export ${key}=' ${filePath} > ${filePath}.mzi && echo "export ${key}='${safeVal}'" >> ${filePath}.mzi && mv ${filePath}.mzi ${filePath}`, 15_000)
      } else {
        await sshExec(ctx.ssh, `echo "export ${key}='${value.replace(/'/g, "'\\''")}'" >> ${filePath}`, 10_000)
      }
      // 远端配置已变更，环境快照立即失效（否则 TTL 内旧值仍会被重放）
      remoteEnvCache.delete(`${ctx.ssh.user}@${ctx.ssh.host}:${ctx.ssh.port}`)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

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
  ipcMain.handle('projects:appServices', async (_e, variantArg: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      const services = await readAppServices(b.path, variantArg)
      return { ok: true, data: { services } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { services: [] } }
    }
  })

  /**
   * 应用服务运行状态（服务名 → State）：docker ps 一次拉全量，按容器名匹配。
   * 不走 compose ps —— compose 会因自定义 log 字段校验失败
   */
  ipcMain.handle('projects:appServicesStatus', async (_e, variantArg: string) => {
    const empty: Record<string, string> = {}
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      await checkDockerCompose()

      const ctx = getProjectCtx()
      let runningNames: Set<string>

      if (ctx.isRemote && ctx.ssh) {
        // 远程：SSH 执行 docker ps，只取运行中容器名（最简格式，无特殊字符）
        const out = await sshExec(ctx.ssh, 'docker ps --format {{.Names}} 2>&1', 15_000)
        runningNames = new Set(
          out.split(/\r?\n/).map((l) => l.trim()).filter((l) => l && !l.includes('command not found') && !l.includes('error'))
        )
      } else {
        // 本地
        const pathEnv = await getShellPath()
        const { stdout } = await exec('docker', ['ps', '--format', '{{.Names}}'], {
          timeout: 15_000,
          env: { ...process.env, PATH: pathEnv }
        })
        runningNames = new Set(stdout.split(/\r?\n/).map((l) => l.trim()).filter(Boolean))
      }

      // 变体服务名 → 容器名 → 在运行列表中则为 running
      const states: Record<string, string> = {}
      const names = await appSvcContainerNames(b.path, variantArg as AppSvcVariant)
      for (const [svc, container] of Object.entries(names)) {
        states[svc] = runningNames.has(container) ? 'running' : 'exited'
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
/** 远程主机规格（CPU 核数 / 物理内存）：机器规格几乎不变，按目标缓存一次即可 */
const remoteHostSpecCache = new Map<string, { cpus: number; memTotal: number }>()
async function remoteHostSpecs(t: SshTarget): Promise<{ cpus: number; memTotal: number }> {
  const key = `${t.user}@${t.host}:${t.port}`
  const hit = remoteHostSpecCache.get(key)
  if (hit) return hit
  try {
    const out = await sshExec(t, "nproc; free -b | awk '/^Mem:/{print $2}'", 10_000)
    const [cpus, mem] = out.trim().split(/\r?\n/)
    const spec = { cpus: parseInt((cpus ?? '').trim(), 10) || 1, memTotal: parseInt((mem ?? '').trim(), 10) || 0 }
    remoteHostSpecCache.set(key, spec)
    return spec
  } catch {
    return { cpus: 1, memTotal: 0 }
  }
}

/** 资源快照缓存：docker stats 有 ~1.3s 固有采样窗口，App 启动即预取 + 落盘持久化，
 * 首屏请求（preferCache）命中即返。轮询请求不查缓存，始终拉新值；10 分钟外的快照视为过期不用 */
const STATS_SNAPSHOT_TTL = 10 * 60_000
type BasicsStatsData = { stats: Record<string, ComposeServiceStats>; cpuCount: number; hostMemTotal: number }
type AppSvcStatsData = {
  stats: Record<'monomer' | 'distributeds', Record<string, ComposeServiceStats>>
  cpuCount: number
  hostMemTotal: number
}
interface StatsSnap<T> {
  at: number
  /** 绑定标识（项目路径 + 远程目标）：换绑后旧快照不匹配，防止展示错项目的数据 */
  key: string
  data: T
}
let basicsStatsSnap: StatsSnap<BasicsStatsData> | null = null
let appSvcStatsSnap: StatsSnap<AppSvcStatsData> | null = null

/** 当前绑定的快照归属键 */
function statsBindingKey(): string {
  try {
    const b = getBinding()
    if (!b) return ''
    return `${b.path}@${b.remote ? `${b.remote.user}@${b.remote.host}:${b.remote.port}` : 'local'}`
  } catch {
    return ''
  }
}

/** 快照落盘（userData/stats-snapshot.json，防抖合并轮询写入）+ 启动回填：App 重开后首屏仍可秒显 */
function statsSnapshotFile(): string {
  return path.join(app.getPath('userData'), 'stats-snapshot.json')
}
let snapWriteTimer: NodeJS.Timeout | null = null
function persistStatsSnapshot(): void {
  if (snapWriteTimer) return
  snapWriteTimer = setTimeout(() => {
    snapWriteTimer = null
    try {
      fs.writeFileSync(statsSnapshotFile(), JSON.stringify({ basics: basicsStatsSnap, app: appSvcStatsSnap }))
    } catch {
      /* 落盘失败不影响功能 */
    }
  }, 1000)
}
function hydrateStatsSnapshot(): void {
  try {
    const raw = JSON.parse(fs.readFileSync(statsSnapshotFile(), 'utf8')) as {
      basics?: StatsSnap<BasicsStatsData>
      app?: StatsSnap<AppSvcStatsData>
    }
    const key = statsBindingKey()
    if (raw.basics?.key === key && Date.now() - raw.basics.at < STATS_SNAPSHOT_TTL) basicsStatsSnap = raw.basics
    if (raw.app?.key === key && Date.now() - raw.app.at < STATS_SNAPSHOT_TTL) appSvcStatsSnap = raw.app
  } catch {
    /* 无快照或文件损坏时忽略 */
  }
}

/** 应用服务资源统计核心（IPC 处理器与启动预取共用） */
async function computeAppSvcStats(): Promise<AppSvcStatsData> {
  const b = getBinding()
  if (!b) throw new Error('尚未绑定项目')
  await checkDockerCompose()
  const result: Record<'monomer' | 'distributeds', Record<string, ComposeServiceStats>> = {
    monomer: {},
    distributeds: {}
  }
  // 主机规格：本地绑定读本机；远程绑定取远端（缓存）——与 stats 并行拉取不增加首屏延迟
  const pctx = getProjectCtx()
  const specsPromise =
    pctx.isRemote && pctx.ssh
      ? remoteHostSpecs(pctx.ssh)
      : Promise.resolve({ cpus: os.cpus().length, memTotal: os.totalmem() })
  const pairs: Array<{ variant: AppSvcVariant; svc: string; name: string }> = []
  for (const variant of ['monomer', 'distributeds'] as AppSvcVariant[]) {
    // 从 yml 解析服务名→容器名映射（不走 compose ps —— log 字段校验失败）
    const names = await appSvcContainerNames(b.path, variant)
    for (const [svc, name] of Object.entries(names)) {
      pairs.push({ variant, svc, name })
    }
  }
  if (pairs.length > 0) {
    const names = pairs.map((p) => p.name)
    const [statsRes, inspRes] = await Promise.all([
      projExec('docker', ['stats', '--no-stream', '--format', '{{.Name}}:{{.CPUPerc}}:{{.MemUsage}}'], {
        timeout: 15_000
      }),
      // inspect 走 projExec 路由（远程绑定时查远端 docker，本地 exec 会查错机器）
      projExec(
        'docker',
        ['inspect', '--format', '{{.Name}} {{.HostConfig.Memory}} {{.HostConfig.NanoCpus}}', ...names],
        { timeout: 15_000 }
      ).catch(() => null),
      specsPromise
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
  const specs = await specsPromise
  return { stats: result, cpuCount: specs.cpus, hostMemTotal: specs.memTotal }
}

/** 去重进行中的基础服务统计（启动预取与首屏请求并发时共享同一次拉取），成功即写入快照并落盘 */
let basicsStatsJob: Promise<BasicsStatsData> | null = null
function freshBasicsStats(): Promise<BasicsStatsData> {
  basicsStatsJob ??= computeBasicsStats()
    .then((data) => {
      basicsStatsSnap = { at: Date.now(), key: statsBindingKey(), data }
      persistStatsSnapshot()
      return data
    })
    .finally(() => {
      basicsStatsJob = null
    })
  return basicsStatsJob
}

/** 同上：应用服务统计去重 */
let appSvcStatsJob: Promise<AppSvcStatsData> | null = null
function freshAppSvcStats(): Promise<AppSvcStatsData> {
  appSvcStatsJob ??= computeAppSvcStats()
    .then((data) => {
      appSvcStatsSnap = { at: Date.now(), key: statsBindingKey(), data }
      persistStatsSnapshot()
      return data
    })
    .finally(() => {
      appSvcStatsJob = null
    })
  return appSvcStatsJob
}

ipcMain.handle('projects:appServicesStats', async (_e, preferCacheArg?: boolean) => {
  try {
    if (
      preferCacheArg &&
      appSvcStatsSnap &&
      appSvcStatsSnap.key === statsBindingKey() &&
      Date.now() - appSvcStatsSnap.at < STATS_SNAPSHOT_TTL
    ) {
      return { ok: true, data: appSvcStatsSnap.data }
    }
    return { ok: true, data: await freshAppSvcStats() }
  } catch (err) {
    return { ok: false, error: (err as Error).message, data: null }
  }
})

  /**
   * 应用服务互斥停机：docker compose -f <file> down 两个 compose 文件（与全部关闭同款语义）。
   * 无运行容器时跳过。日志走 projects:scriptLog
   */
  async function appServicesDown(
    event: Electron.IpcMainInvokeEvent,
    bPath: string,
    variant: AppSvcVariant,
    reason: string,
    sid: string
  ): Promise<void> {
    const def = APP_SVC_DEFS[variant]
    const send = (text: string): void => {
      if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
    }
    if (reason) send(`▶ 互斥检查：${reason}需先关闭${def.label}全部服务`)

    // 有运行容器才需要 down
    const ctx = getProjectCtx()
    let runningSet: Set<string>
    if (ctx.isRemote && ctx.ssh) {
      const out = await sshExec(ctx.ssh, 'docker ps --format {{.Names}} 2>&1', 15_000)
      runningSet = new Set(out.split(/\r?\n/).map((l) => l.trim()).filter((l) => l && !l.includes('command not found')))
    } else {
      const pathEnv = await getShellPath()
      const { stdout } = await exec('docker', ['ps', '--format', '{{.Names}}'], {
        timeout: 15_000,
        env: { ...process.env, PATH: pathEnv }
      })
      runningSet = new Set(stdout.split(/\r?\n/).map((l) => l.trim()).filter(Boolean))
    }
    const names = await appSvcContainerNames(bPath, variant)
    const runningCount = Object.values(names).filter((c) => runningSet.has(c)).length

    if (runningCount === 0) {
      send(`✓ ${def.label}无运行服务，跳过`)
      return
    }
    send(`▶ 关闭${def.label} ${runningCount} 个运行中的服务`)
    const composeBinParts = ctx.isRemote && ctx.ssh
      ? (await remoteComposeCmd(ctx.ssh)).split(' ') // 远程可能是 v1 docker-compose
      : ['docker', 'compose']
    const [composeCmd, ...composeArgs] = composeBinParts
    const dir = path.join(bPath, def.dir)
    for (const fileName of [def.servicesFile, def.adminFile]) {
      const args = [...composeArgs, '-f', fileName, 'down']
      send(`▶ ${[composeCmd, ...args].join(' ')}`)
      try {
        await projStream(event.sender, 'projects:scriptLog', composeCmd, args, {
          cwd: dir,
          timeoutMs: 10 * 60_000,
          sid
        })
        send(`✓ ${fileName} 已全部关闭`)
      } catch (err) {
        const msg = (err as Error).message
        throw new Error(msg.startsWith('exit ') ? `docker compose down 失败（${msg}），详见日志` : msg)
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

        // compose up -d 创建并启动（docker start 要求容器已存在：首次启动或 stop 回收后会 No such container）
        const pctx = getProjectCtx()
        const composeBin = pctx.isRemote && pctx.ssh
          ? (await remoteComposeCmd(pctx.ssh)).split(' ') // 远程可能是 v1 docker-compose
          : ['docker', 'compose']
        const [composeCmd, ...composeArgs] = composeBin
        const base = [...composeArgs, '-f', fileName]
        const cmdList: string[][] =
          action === 'start'
            ? [[...base, 'up', '-d', service]]
            : action === 'stop'
              ? [[...base, 'rm', '--stop', '--force', service]]
              : [[...base, 'rm', '--stop', '--force', service], [...base, 'up', '-d', service]]
        for (const args of cmdList) {
          send(`▶ ${[composeCmd, ...args].join(' ')}`)
          try {
            await projStream(event.sender, 'projects:scriptLog', composeCmd, args, {
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
  ipcMain.handle('projects:appServiceAll', async (event, variantArg: string, action: string, sidArg?: string, opts?: { foreground?: boolean }) => {
    const sid = validSid(sidArg)
    const send = (text: string): void => {
      if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
    }

    const work = async (): Promise<void> => {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      if (action !== 'start' && action !== 'stop') throw new Error('非法的操作')
      await checkDockerCompose()
      const variant = variantArg as AppSvcVariant
      const def = APP_SVC_DEFS[variant]
      const dir = projPath(def.dir)
      for (const fileName of [def.servicesFile, def.adminFile]) {
        if (!(await projFileExists(path.join(def.dir, fileName)))) throw new Error(`未找到 ${def.dir}/${fileName}`)
      }

      const send = (text: string): void => {
        if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
      }

      if (action === 'start') {
        const other: AppSvcVariant = variant === 'monomer' ? 'distributeds' : 'monomer'
        await appServicesDown(event, b.path, other, `启动${def.label}全部服务`, sid)

        const ctx = getProjectCtx()
        for (const fileName of [def.servicesFile, def.adminFile]) {
          send(`▶ 启动 ${fileName}`)
          try {
            if (ctx.isRemote && ctx.ssh) {
              // 远程：后台执行 compose up，轮询进度（不长时间占用 SSH 连接）
              const composeBin = await remoteComposeCmd(ctx.ssh)
              const stamp = `${variant}-${Date.now()}`
              const logFile = `/tmp/maozi-compose-${stamp}.log`
              const pidFile = `/tmp/maozi-compose-${stamp}.pid`
              // 注册会话（无子进程）：停止按钮靠 stopRequested 中断轮询并杀远端后台进程
              runningProcs.set(sid, { stopRequested: false })
              try {
                // $$ 写 PID 文件判活 —— ps/grep 按文件名匹配会被含同名参数的僵尸 ssh 会话污染
                await sshExec(ctx.ssh, `nohup bash -c 'echo $$ > ${pidFile}; cd ${JSON.stringify(dir)} && ${composeBin} -f ${fileName} up -d' > ${logFile} 2>&1 &`, 10_000)
                send(`⏳ ${fileName} 正在后台启动（拉镜像/创建容器可能需要几分钟）…`)

                // 轮询进度（每 3 秒，最长 5 分钟）
                let lastLines = 0
                for (let poll = 0; poll < 100; poll++) {
                  await new Promise((r) => setTimeout(r, 3000))
                  if (runningProcs.get(sid)?.stopRequested) {
                    // 手动中断：杀远端后台 compose 并终止整个操作（会话已由前端收尾，不发完成标记）
                    void sshExec(ctx.ssh, `kill $(cat ${pidFile} 2>/dev/null) 2>/dev/null`, 5_000).catch(() => {})
                    send(`⏹ 已中断 ${fileName}`)
                    return
                  }
                  try {
                    // 检查后台进程是否还在运行（PID 文件，不受无关进程命令行干扰）
                    const alive = await sshExec(ctx.ssh, `kill -0 $(cat ${pidFile} 2>/dev/null) 2>/dev/null && echo RUN || echo DONE`, 5_000)
                    // 读取新增日志
                    const out = await sshExec(ctx.ssh, `wc -l < ${logFile} 2>/dev/null || echo 0`, 5_000)
                    const total = parseInt(out.trim()) || 0
                    if (total > lastLines) {
                      const newLog = await sshExec(ctx.ssh, `tail -n ${total - lastLines} ${logFile} 2>/dev/null`, 5_000)
                      for (const line of newLog.split('\n')) {
                        if (line.trim()) send(line)
                      }
                      lastLines = total
                    }
                    if (alive.includes('DONE')) {
                      send(`✓ ${fileName} 启动完成`)
                      break
                    }
                  } catch { /* 轮询失败继续 */ }
                }
              } finally {
                // 清理临时日志与 PID 文件
                void sshExec(ctx.ssh, `rm -f ${logFile} ${pidFile}`, 5_000).catch(() => {})
                runningProcs.delete(sid)
              }
            } else {
              // 本地：流式执行
              await projStream(event.sender, 'projects:scriptLog', 'docker',
                ['compose', '-f', fileName, 'up', '-d'], {
                  cwd: dir,
                  timeoutMs: 10 * 60_000,
                  sid
                })
              send(`✓ ${fileName} 启动完成`)
            }
          } catch (err) {
            const msg = (err as Error).message
            send(`✗ ${fileName} 启动失败：${msg}`)
            throw new Error(`docker compose up 失败（${msg}），详见日志`)
          }
        }
      } else {
        // 全部关闭：docker compose -f <file> down（与全部启动的 up -d 对称，两个 compose 文件逐个执行）
        const pctx = getProjectCtx()
        const composeBinParts = pctx.isRemote && pctx.ssh
          ? (await remoteComposeCmd(pctx.ssh)).split(' ') // 远程可能是 v1 docker-compose
          : ['docker', 'compose']
        const [composeCmd, ...composeArgs] = composeBinParts
        for (const fileName of [def.servicesFile, def.adminFile]) {
          const args = [...composeArgs, '-f', fileName, 'down']
          send(`▶ ${[composeCmd, ...args].join(' ')}`)
          try {
            await projStream(event.sender, 'projects:scriptLog', composeCmd, args, {
              cwd: dir,
              timeoutMs: 10 * 60_000,
              sid
            })
            send(`✓ ${fileName} 已全部关闭`)
          } catch (err) {
            const msg = (err as Error).message
            throw new Error(msg.startsWith('exit ') ? `docker compose down 失败（${msg}），详见日志` : msg)
          }
        }
      }
    }

    // 前台模式：供脚本会话内部串行调用（先关对侧容器再执行编译脚本），await 到真实完成且不发完成标记——
    // 后台模式的 __APP_SVC_DONE__ 会把调用方（脚本）会话提前标记完成，导致按钮提前变回可执行
    if (opts?.foreground) {
      try {
        await work()
        return { ok: true }
      } catch (err) {
        send(`✗ 操作失败：${(err as Error).message}`)
        return { ok: false, error: (err as Error).message }
      }
    }

    // 后台模式：立即返回，核心逻辑异步执行，完成时发标记驱动前端会话收尾
    setImmediate(() => {
      void (async () => {
        try {
          await work()
        } catch (err) {
          send(`✗ 操作失败：${(err as Error).message}`)
          send('__APP_SVC_FAIL__')
          return
        }
        send('__APP_SVC_DONE__')
      })()
    })

    return { ok: true, background: true }
  })

  /** 数据库初始化状态：.db-init.json 标记判定（maozi-cloud-develop-admin 应用目录，git 已忽略）；INIT_MYSQL_DB（每行一个脚本路径）不存在或为空时隐藏按钮 */
  ipcMain.handle('projects:dbInitStatus', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (!(await projFileExists(INIT_MYSQL_DB_FILE))) return { ok: true, data: { count: 0, initialized: false, missing: 0 } }
      const scripts = parseInitMysqlDb(await readProjFile(INIT_MYSQL_DB_FILE))
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
      if (!(await projFileExists(INIT_MYSQL_DB_FILE))) throw new Error(`未找到 ${INIT_MYSQL_DB_FILE}`)
      const scripts = parseInitMysqlDb(await readProjFile(INIT_MYSQL_DB_FILE))
      if (scripts.length === 0) throw new Error(`${INIT_MYSQL_DB_FILE} 中没有定义初始化脚本`)
      const dir = projPath(COMPOSE_DIR)
      if (!(await projFileExists(path.join(COMPOSE_DIR, 'docker-compose.yml')))) throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      await checkDockerCompose()
      const { container, password } = parseComposeMysqlConfig(await readProjFile(path.join(COMPOSE_DIR, 'docker-compose.yml')))
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
          await projStream(event.sender, 'projects:scriptLog', 'docker', [
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

      const ctxDb = getProjectCtx()
      for (const script of scripts) {
        const scriptAbs = path.isAbsolute(script) ? script : path.join(b.path, script)
        send(`▶ 导入脚本 ${script}`)
        try {
          if (ctxDb.isRemote && ctxDb.ssh) {
            // 远程：SQL 文件就在远端项目内，由远端 shell 重定向导入（sshStream 无法向远端注入 stdin）
            await sshStream(event.sender, 'projects:scriptLog', ctxDb.ssh,
              `docker exec -i ${container} mysql -uroot -p${password} --default-character-set=utf8mb4 < '${scriptAbs}'`,
              { timeoutMs: 5 * 60_000, sid })
          } else {
            if (!fs.existsSync(scriptAbs)) throw new Error(`未找到初始化脚本：${script}`)
            await projStream(
              event.sender,
              'projects:scriptLog',
              'docker',
              ['exec', '-i', container, 'mysql', '-uroot', `-p${password}`, '--default-character-set=utf8mb4'],
              { cwd: dir, timeoutMs: 5 * 60_000, sid, stdinData: fs.readFileSync(scriptAbs, 'utf8') }
            )
          }
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
          await projStream(event.sender, 'projects:scriptLog', 'docker', [
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

  /**
   * 全链路日志查询：按链路 ID（trace ID）检索分布式变体所有运行容器的日志，
   * 逐容器 docker logs --tail N + 子串匹配，返回每个服务命中的日志行
   */
  ipcMain.handle('projects:traceLogQuery', async (_e, traceId: string, tailArg?: number) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const kw = String(traceId ?? '').trim()
      if (!kw || kw.length < 4) throw new Error('请输入至少 4 个字符的链路 ID')
      if (!/^[A-Za-z0-9_-]+$/.test(kw)) throw new Error('链路 ID 仅支持字母、数字、中划线、下划线')
      const tail = Math.min(Math.max(parseInt(String(tailArg ?? 5000), 10) || 5000, 100), 20000)
      await checkDockerCompose()
      const pathEnv = await getShellPath()
      const env = { ...process.env, PATH: pathEnv }
      const lowerKw = kw.toLowerCase()

      // 获取分布式变体的 服务名→容器名 映射
      const names = appSvcContainerNames(b.path, 'distributeds')

      // 一次性拉所有运行中容器
      const { stdout: psOut } = await exec('docker', ['ps', '--format', '{{.Names}}'], { timeout: 15_000, env })
      const runningSet = new Set(psOut.split(/\r?\n/).map((l) => l.trim()).filter(Boolean))

      // 仅查 services yml 定义的容器（不含 admin），日志写到文件而非 stdout
      const servicesYml = path.join(b.path, APP_SVC_DEFS.distributeds.dir, APP_SVC_DEFS.distributeds.servicesFile)
      const svcNames = parseServiceContainerNames(fs.readFileSync(servicesYml, 'utf8'))

      // 逐容器并行 grep 日志文件
      interface HitLine {
        service: string
        ts: number
        line: string
      }
      const allHits: HitLine[] = []
      const jobs: Promise<void>[] = []
      for (const [svc, container] of Object.entries(svcNames)) {
        if (!runningSet.has(container)) continue
        jobs.push(
          (async () => {
            try {
              const { stdout } = await exec(
                'docker',
                ['exec', container, 'sh', '-c', `grep -i '${kw}' /application/*/logs/*.log 2>/dev/null | tail -${tail}`],
                { timeout: 30_000, env, maxBuffer: 64 * 1024 * 1024 }
              )
              for (const l of stdout.split(/\r?\n/)) {
                if (!l.trim()) continue
                allHits.push({ service: svc, ts: parseLogTs(l), line: l })
              }
            } catch {
              /* 单个容器查询失败不影响整体 */
            }
          })()
        )
      }
      await Promise.all(jobs)

      // 按时间戳升序（无时间戳的排最后）
      allHits.sort((a, b) => a.ts - b.ts)
      const results = allHits.map((h) => `[${h.service}] ${h.line}`)

      if (results.length === 0) {
        return { ok: true, data: { results: [], message: `未在任何运行中的微服务日志中找到包含「${kw}」的记录` } }
      }
      const svcCount = new Set(allHits.map((h) => h.service)).size
      return {
        ok: true,
        data: {
          results,
          message: `共 ${svcCount} 个服务命中 ${results.length} 行，已按时间排序（检索范围：最近 ${tail} 行/容器）`
        }
      }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { results: [], message: '' } }
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
      if (!(await projFileExists(ctx ? path.join(ctx.dir, ctx.file) : path.join(COMPOSE_DIR, 'docker-compose.yml')))) {
        throw new Error(ctx ? `未找到 ${ctx.dir}/${ctx.file}` : `未找到 ${COMPOSE_DIR}/docker-compose.yml`)
      }

      // 容器若设了 LOG_FILE 环境变量，tail 该文件；否则走 compose logs
      const probe = await probeContainerLogFile(dir, fileArgs, service)
      if (probe.logFile) {
        const { stdout } = await projExec(
          'docker',
          ['exec', probe.container, 'tail', '-n', String(tail), probe.logFile],
          { timeout: 30_000, maxBuffer: 64 * 1024 * 1024 }
        )
        const lines = stdout.split(/\r?\n/).filter((l) => l.trim() !== '')
        if (lines.length === 0) throw new Error('日志文件暂无内容')
        return { ok: true, data: lines }
      }
      const { stdout } = await projExec('docker', ['compose', ...fileArgs, 'logs', '--no-color', '--tail', String(tail), service], {
        cwd: dir,
        timeout: 30_000,
        maxBuffer: 64 * 1024 * 1024
      })
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
        const composeFile = ctx ? path.join(dir, ctx.file) : path.join(dir, 'docker-compose.yml')
        const fileArgs = ctx ? ['-f', ctx.file] : []
        if (!(await projFileExists(ctx ? path.join(ctx.dir, ctx.file) : path.join(COMPOSE_DIR, 'docker-compose.yml')))) {
          throw new Error(ctx ? `未找到 ${ctx.dir}/${ctx.file}` : `未找到 ${COMPOSE_DIR}/docker-compose.yml`)
        }
        const pathEnv = await getShellPath()

        // 容器若设了 LOG_FILE 环境变量，tail -f 该文件；否则 compose logs -f
        const probe = await probeContainerLogFile(dir, fileArgs, service)
        // 远程 v1 不支持 --no-color；检测 compose 命令后调整（ctx 是 compose 上下文，须另取项目上下文判远程）
        const pctx = getProjectCtx()
        const isRemote = pctx.isRemote && !!pctx.ssh
        let dockerCmd = 'docker'
        let composeParts: string[] = ['compose']
        if (isRemote && pctx.ssh) {
          const bin = await remoteComposeCmd(pctx.ssh) // 'docker compose' | 'docker-compose'
          const parts = bin.split(' ')
          if (parts.length > 1) composeParts = parts.slice(1)
          else {
            dockerCmd = parts[0]
            composeParts = []
          }
        }
        const logArgs = probe.logFile
          ? ['exec', probe.container, 'tail', '-f', '-n', String(tail), probe.logFile]
          : [
              ...composeParts,
              ...fileArgs,
              'logs',
              ...(isRemote ? [] : ['--no-color']),
              '--tail',
              String(tail),
              '--follow',
              service
            ]

        try {
          // probe.logFile 分支操作的是容器名，必须用 docker exec；compose 命令的 exec 子命令按服务名解析
          await projStream(event.sender, 'projects:composeLog', probe.logFile ? 'docker' : dockerCmd, logArgs, {
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

  /** 基础服务资源统计核心（IPC 处理器与启动预取共用）：compose ps 取服务→容器名映射，docker stats 一次性快照按容器名匹配 */
  async function computeBasicsStats(): Promise<BasicsStatsData> {
    const b = getBinding()
    if (!b) throw new Error('尚未绑定项目')
    // 远程绑定时 compose 文件在远端，须走 projFileExists（本地 fs.existsSync 会误判不存在）
    if (!(await projFileExists(path.join(COMPOSE_DIR, 'docker-compose.yml')))) {
      throw new Error(`未找到 ${COMPOSE_DIR}/docker-compose.yml`)
    }
    await checkDockerCompose()
    const dir = path.join(b.path, COMPOSE_DIR)
    let psOut = ''
    const ctxPs = getProjectCtx()
    if (ctxPs.isRemote && ctxPs.ssh) {
      // remoteComposeCmd 可能返回 'docker compose'（两段）或 'docker-compose'（一段，v1 独立命令）
      const parts = (await remoteComposeCmd(ctxPs.ssh)).split(' ')
      const psResult = await projExec(parts[0], [...parts.slice(1), 'ps', '--format', 'json'], {
        cwd: dir,
        timeout: 15_000
      })
      psOut = psResult.stdout
    } else {
      const psResult = await projExec('docker', ['compose', 'ps', '--format', 'json'], {
        cwd: dir,
        timeout: 15_000
      })
      psOut = psResult.stdout
    }
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
    // 主机规格：本地绑定读本机；远程绑定取远端（缓存）——与 stats 并行拉取不增加延迟
    const specsPromise =
      ctxPs.isRemote && ctxPs.ssh
        ? remoteHostSpecs(ctxPs.ssh)
        : Promise.resolve({ cpus: os.cpus().length, memTotal: os.totalmem() })
    if (nameByService.size > 0) {
      // stats 与 inspect 只依赖容器名，并行执行缩短首次数据延迟（stats 自身有 ~1s 采样窗口）
      const [statsRes, inspRes] = await Promise.all([
        projExec('docker', ['stats', '--no-stream', '--format', '{{.Name}}:{{.CPUPerc}}:{{.MemUsage}}'], {
          timeout: 15_000
        }),
        projExec('docker', ['inspect', '--format', '{{.Name}} {{.HostConfig.Memory}} {{.HostConfig.NanoCpus}}', ...nameByService.values()], {
          timeout: 15_000
        }).catch(() => null),
        specsPromise
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
      } else {
        for (const [svc, name] of nameByService) {
          const e = byName.get(name)
          if (!e) continue
          stats[svc] = { cpuPercent: e.cpuPercent, memUsed: e.memUsed, memLimit: 0, cpusLimit: 0 }
        }
      }
    }
    const specs = await specsPromise
    return { stats, cpuCount: specs.cpus, hostMemTotal: specs.memTotal }
  }

  ipcMain.handle('projects:composeStats', async (_e, preferCacheArg?: boolean) => {
    try {
      if (
        preferCacheArg &&
        basicsStatsSnap &&
        basicsStatsSnap.key === statsBindingKey() &&
        Date.now() - basicsStatsSnap.at < STATS_SNAPSHOT_TTL
      ) {
        return { ok: true, data: basicsStatsSnap.data }
      }
      return { ok: true, data: await freshBasicsStats() }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /** 环境设置：解析项目 ENVIRONMENT_VARIABLE，并实时读取各环境变量当前值 */
  ipcMain.handle('projects:envSettings', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const groups = parseEnvVarDefs(await readProjFile(ENV_VARS_FILE))
      const ctx2 = getProjectCtx()
      if (ctx2.isRemote && ctx2.ssh) {
        // 远程绑定：SSH 读取远程 shell 环境变量
        const remoteEnv = await sshExec(ctx2.ssh, 'env', 15_000)
        const envMap = new Map<string, string>()
        for (const line of remoteEnv.split('\n')) {
          const eq = line.indexOf('=')
          if (eq > 0) envMap.set(line.slice(0, eq).trim(), line.slice(eq + 1).trim())
        }
        for (const g of groups) {
          g.items = g.items.map((it) => ({
            ...it,
            value: envMap.get(it.key) ?? '',
            found: envMap.has(it.key),
            enabled: envMap.has(it.key),
            source: envMap.has(it.key) ? '远程环境' : ''
          }))
        }
        const remoteFiles = [
          { id: 'zshrc', name: '~/.zshrc', path: '', exists: true },
          { id: 'zshenv', name: '~/.zshenv', path: '', exists: true },
          { id: 'zprofile', name: '~/.zprofile', path: '', exists: true },
          { id: 'bashrc', name: '~/.bashrc', path: '', exists: true },
          { id: 'bash_profile', name: '~/.bash_profile', path: '', exists: true },
          { id: 'profile', name: '~/.profile', path: '', exists: true }
        ]
        for (const f of remoteFiles) {
          try {
            const check = await sshExec(ctx2.ssh, `test -f ${f.name} && echo YES || echo NO`, 5_000)
            f.exists = check.includes('YES')
          } catch { f.exists = false }
        }
        const defaultFile = remoteFiles.find((f) => f.exists) ?? remoteFiles[0]
        return { ok: true, data: { groups, files: remoteFiles, defaultFileId: defaultFile.id, remote: true } }
      }
      // 本地绑定：读本地 shell 配置
      const platform = selectPlatform()
      const files = platform.listEnvFiles()
      applyEnvValues(groups, platform.readEnvVars(), files)
      const defaultFile = files.find((f) => f.exists) ?? files[0]
      return { ok: true, data: { groups, files, defaultFileId: defaultFile.id } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /** 中断指定会话正在执行的脚本/命令：向进程组发 SIGINT，1.5s 后仍存活则 SIGKILL */
  /** ===== 远程服务器（SSH）绑定与操作 ===== */

  /** 获取可用的 Linux 密钥列表 */
  ipcMain.handle('projects:sshConfigs', () => {
    try {
      const configs = listConfigs().filter((c) => c.type === 'Linux')
      return { ok: true, data: configs.map((c) => ({ id: c.id, name: c.name, address: c.address ?? '' })) }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: [] }
    }
  })

  /** 测试 SSH 连接 */
  ipcMain.handle('projects:sshTest', async (_e, configId: string) => {
    try {
      const target = resolveSshTarget(configId)
      const out = await sshExec(target, 'echo OK', 15_000)
      const okConn = out.includes('OK')
      return { ok: okConn, error: okConn ? undefined : '连接失败', data: okConn ? `${target.user}@${target.host}:${target.port}` : null }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /** 列出远程目录（一级子目录） */
  ipcMain.handle('projects:sshListDir', async (_e, configId: string, dirPath: string) => {
    try {
      const target = resolveSshTarget(configId)
      const items = await sshListDir(target, dirPath || '/')
      return { ok: true, data: { path: dirPath || '/', items } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { path: dirPath, items: [] } }
    }
  })

  /** 在远程目录下创建子目录（浏览/拉取代码的选择目录页新建目标目录） */
  ipcMain.handle('projects:sshMkdir', async (_e, configId: string, parentDir: string, nameArg: string) => {
    try {
      const target = resolveSshTarget(configId)
      const dir = (parentDir || '/').replace(/\/+$/, '')
      const name = String(nameArg ?? '').trim()
      if (!/^[A-Za-z0-9._\u4e00-\u9fa5-]{1,64}$/.test(name) || name === '.' || name === '..') {
        throw new Error('目录名仅支持中文、字母、数字、点、下划线、连字符（1-64 字符）')
      }
      // 父目录即当前浏览目录（必然存在），用 mkdir 而非 mkdir -p：路径异常时让远端报错而非静默建多级
      await sshExec(target, `mkdir "${dir}/${name}"`, 15_000)
      return { ok: true, data: null }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /** 远程绑定：读取远程 CONFIG 文件完成绑定 */
  ipcMain.handle('projects:sshBindDir', async (_e, configId: string, dirPath: string) => {
    try {
      const target = resolveSshTarget(configId)
      const configContent = await sshReadFile(target, dirPath + '/CONFIG')
      const map = new Map<string, string>()
      for (const line of configContent.split('\n')) {
        const t = line.trim()
        if (!t || t.startsWith('#')) continue
        const eq = t.indexOf('=')
        if (eq < 0) continue
        map.set(t.slice(0, eq).trim(), t.slice(eq + 1).trim())
      }
      const name = map.get('name')
      if (!name) throw new Error('远程 CONFIG 文件中缺少 name')
      const cfg = listConfigs().find((c) => c.id === configId)
      const binding: ProjectBinding = {
        name,
        version: map.get('version') ?? '',
        path: dirPath,
        boundAt: Date.now(),
        remote: {
          configId,
          configName: cfg?.name ?? '',
          user: target.user,
          host: target.host,
          port: target.port
        }
      }
      saveBinding(binding)
      clearYmlCache()
      return { ok: true, data: binding }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 检查远程是否安装 git */
  ipcMain.handle('projects:sshCheckGit', async (_e, configId: string) => {
    try {
      const target = resolveSshTarget(configId)
      const has = await sshCheckGit(target)
      return { ok: true, data: has }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: false }
    }
  })

  /** 远程拉取代码：SSH 执行 git clone，日志走 projects:scriptLog */
  ipcMain.handle('projects:sshClone', async (event, configId: string, gitSecretId: string, destDir: string, sidArg?: string) => {
    const sid = validSid(sidArg)
    const send = (text: string): void => {
      if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
    }
    try {
      const target = resolveSshTarget(configId)
      const gitSecret = listConfigs().find((c) => c.id === gitSecretId)
      if (!gitSecret || gitSecret.type !== 'Git') throw new Error('Git 密钥不存在或类型错误')
      if (!gitSecret.password) throw new Error('Git 密钥未填写凭据')

      const hasGit = await sshCheckGit(target)
      if (!hasGit) throw new Error('远程服务器未安装 git，请先安装（apt/yum install git）')

      const auth =
        (gitSecret.authType ?? 'password') === 'key'
          ? encodeURIComponent(gitSecret.password)
          : encodeURIComponent(gitSecret.username) + ':' + encodeURIComponent(gitSecret.password)
      const authedUrl = REPO_URL.replace('https://', 'https://' + auth + '@')
      const targetDir = destDir + '/maozi-cloud'

      // 拉取前检测远端代理（VPN）：GitHub 直连极易链路假死；检测到代理则显式注入 git 环境
      const { exports: proxyExports, proxy: remoteProxy } = await detectRemoteProxy(target)
      if (remoteProxy) {
        send(`▶ 检测到远端代理（VPN）：${remoteProxy}，git clone 将经代理拉取`)
      } else {
        send('▶ 未检测到远端代理，直连 GitHub（若拉取缓慢或卡住，请先在服务器开启 VPN / 配置 http_proxy')
      }

      // 浅克隆减小传输量；低速熔断（<1KB/s 持续 60s 判定链路假死中止）——GitHub 连接静默假死时 git 会无限挂起
      const cmd =
        proxyExports +
        'mkdir -p "' + destDir + '" && cd "' + destDir + '" && git clone --progress --depth 1 --single-branch -c http.lowSpeedLimit=1024 -c http.lowSpeedTime=60 ' +
        JSON.stringify(REPO_URL) +
        ' maozi-cloud 2>&1'

      send('▶ SSH ' + target.user + '@' + target.host + ' 执行 git clone（浅克隆 --depth 1）')
      send('▶ 目标目录：' + targetDir)

      try {
        await sshStream(event.sender, 'projects:scriptLog', target, cmd, { timeoutMs: 10 * 60_000, sid })
      } catch (err) {
        // 清理半成品目录：git clone 中止会留下不完整目录，不清理则重试直接报"目录已存在"
        await sshExec(target, `rm -rf "${targetDir}"`, 15_000).catch(() => {})
        const msg = (err as Error).message
        throw new Error(msg.startsWith('exit ') ? '远程 git clone 失败（' + msg + '），详见日志' : msg)
      }

      // 读取远程 CONFIG 完成绑定
      const configContent = await sshReadFile(target, targetDir + '/CONFIG')
      const map = new Map<string, string>()
      for (const line of configContent.split('\n')) {
        const t = line.trim()
        if (!t || t.startsWith('#')) continue
        const eq = t.indexOf('=')
        if (eq < 0) continue
        map.set(t.slice(0, eq).trim(), t.slice(eq + 1).trim())
      }
      const name = map.get('name') ?? 'maozi-cloud'
      const configEntry = listConfigs().find((c) => c.id === configId)
      const binding: ProjectBinding = {
        name,
        version: map.get('version') ?? '',
        path: targetDir,
        boundAt: Date.now(),
        remote: {
          configId,
          configName: configEntry?.name ?? '',
          user: target.user,
          host: target.host,
          port: target.port
        }
      }
      saveBinding(binding)
      clearYmlCache()
      return { ok: true, data: binding }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('projects:stopScript', (_e, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const entry = runningProcs.get(sid)
      if (entry) {
        // 无 child 的会话（远程 nohup 后台 + 轮询）靠 stopRequested 标记中断
        entry.stopRequested = true
        if (entry.child?.pid) {
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
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 启动预取资源快照：先回填上次会话落盘的快照（重开 App 首屏秒显），
   * 再后台拉新（docker stats ~1.3s 采样窗口在用户点开控制台之前完成），顺带预热 yml 缓存 */
  hydrateStatsSnapshot()
  void (async () => {
    try {
      if (!getBinding()) return
      void readProjFileCached(path.join(COMPOSE_DIR, 'docker-compose.yml')).catch(() => {})
      await checkDockerCompose()
      await Promise.allSettled([freshBasicsStats(), freshAppSvcStats()])
    } catch {
      /* 预取失败不阻塞：首屏退回正常拉取 */
    }
  })()
}
