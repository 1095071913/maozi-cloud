import {app, BrowserWindow, dialog, ipcMain, shell} from 'electron'
import {execFile, spawn} from 'node:child_process'
import {promisify} from 'node:util'
import fs from 'node:fs'
import net from 'node:net'
import os from 'node:os'
import path from 'node:path'
import crypto from 'node:crypto'
import {
  activateProject,
  clearBinding,
  createProject,
  deactivateProject,
  getBinding,
  isDbInitialized,
  listProjects,
  markDbInitialized,
  parseConfigFile,
  removeProject,
  saveBinding,
  updateProject,
  userDataStateFile
} from './store'
import {listConfigs} from '../configs/store'
import {selectPlatform} from '../platform'
import type {EnvFile, EnvVarEntry, HostsEntry} from '../platform/types'
import {getShellPath} from '../system/sysinfo'
import type {
  AppServiceEntry,
  ComposeServiceStats,
  EnvSettingGroup,
  EnvSettingItem,
  EnvSettingSection,
  ProjectBinding
} from './types'

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
 * 且不能写 [ -f ]——方括号会被 expect 的 Tcl 双引号串当命令替换。
 * rc 配置可能整串替换 PATH（丢掉 /usr/bin 等系统目录，systemctl 等会 command not found），
 * source 后再补一次系统目录兜底
 */
const REMOTE_PATH_SETUP =
  'export PATH=$PATH:/usr/local/bin:/usr/bin:/usr/sbin:/usr/local/sbin:/opt/homebrew/bin:$HOME/.local/bin:$HOME/bin; test -f ~/.zshenv && . ~/.zshenv >/dev/null 2>&1; test -f ~/.zshrc && . ~/.zshrc >/dev/null 2>&1; test -f ~/.bashrc && . ~/.bashrc >/dev/null 2>&1; test -f ~/.profile && . ~/.profile >/dev/null 2>&1; export PATH=$PATH:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; true'

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
  // "assword for " 覆盖 sudo 提示语（[sudo] password for <user>:，sudo -S 写到 stderr），
  // 以同一份 SSH 密码自动应答（sudo 密码与登录密码通常一致）
  const script = `set timeout ${Math.ceil(timeoutMs / 1000)}\n${esc}\nexpect {\n  "password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "Password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "assword for " { send "$env(SSH_PASS)\\r"; exp_continue }\n  "yes/no" { send "yes\\r"; exp_continue }\n  eof\n}\ncatch wait result\nexit [lindex $result 3]`
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
      .filter((l) => l && !l.startsWith('spawn ') && !l.includes('assword:') && !l.includes('assword for'))
      .slice(-3)
      .join('；')
      .slice(0, 200)
    throw new Error(e.killed ? `远程命令超时（>${timeoutMs / 1000}s）` : `远程命令失败（exit ${e.code ?? '?'}）${tail ? '：' + tail : ''}`)
  }
  return stdout
    .split('\n')
    .map((l) => l.replace(/\r/g, ''))
    .filter((l) => !l.startsWith('spawn ') && !l.includes('password:') && !l.includes('Password:') && !l.includes('assword for') && !isSshNoise(l))
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
    // 过滤 SSH 密码提示行（(user@host) Password:）与 sudo 提示行（[sudo] password for <user>:）
    // ——expect 自动应答的提示不应出现在业务日志里
    if (clean && !isSshNoise(clean) && !/([Pp]assword:|assword for [^:]*:)\s*$/.test(clean) && !sender.isDestroyed())
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
      // catch wait + exit：把 ssh 的真实退出码透传出来，命令失败不再假成功；
      // "assword for " 同 sshExecRaw：sudo -S 的提示语用 SSH 密码自动应答
      args = ['-c', `set timeout -1\n${esc}\nexpect {\n  "password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "Password:" { send "$env(SSH_PASS)\\r"; exp_continue }\n  "assword for " { send "$env(SSH_PASS)\\r"; exp_continue }\n  "yes/no" { send "yes\\r"; exp_continue }\n  eof\n}\ncatch wait result\nexit [lindex $result 3]`]
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
        // 被中断的会话保留 stopRequested 标记（无 child）：热备健康等待与后续流程据此中止
        if (opts.sid) {
          if (stopped) runningProcs.set(opts.sid, { stopRequested: true })
          else runningProcs.delete(opts.sid)
        }
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
    // extraEnv 无法跨 SSH 生效，远程环境注入走 sshEnvPrefix（export 前缀）；
    // stdinData 以 printf 管道应答交互确认（compose rm 不带 --force 会问 y/N）。
    // 管道必须放在 cd 之后接住真实命令：拼在最前面会喂给 cd，rm 的确认永远等不到
    // 应答而挂起（会话一直显示运行中）
    const pipedIn = opts.stdinData !== undefined ? `printf 'y\\n' | ` : ''
    const full = `${opts.sshEnvPrefix ?? ''}cd "${cwd}" && ${pipedIn}${cmd} ${args.join(' ')}`
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

/** 项目内置 hosts 映射文件：JSON，key = ip，value = 域名（可逗号/空格分隔多个或字符串数组） */
const HOSTS_FILE = 'maozi-cloud-script/maozi-cloud-utils/init_hosts.json'

/** 项目环境变量定义文件：JSON，key 为中文名称，value 为环境变量 key；value 为对象时表示分组 */
/** 项目环境变量定义文件：JSON，一级属性为分节（Tab），节内中文名称 → 环境变量 key，可再嵌套一层分组 */
const ENV_VARS_FILE = 'maozi-cloud-script/maozi-cloud-utils/environment_variable.json'

/** 服务级 docker 配置定义文件：JSON，一级 key = 容器完整名称（如 maozi-cloud-admin-monomer），节内 配置描述 → 变量 key */
const DOCKER_VARS_FILE = 'maozi-cloud-script/maozi-cloud-utils/docker_variable.json'

/** 业务 docker 编排 .env 文件（每行 key=value）：环境设置第二个分节（Tab）的读写目标 */
const BUSINESS_ENV_FILE = 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-business-docker/.env'

/**
 * 数据库初始化定义文件：init_mysql_db.json（JSON 数组，每项一个 SQL 脚本路径，相对项目根）。
 * 初始化标记为 .db-init.json（见 store.ts，位于 maozi-cloud-develop-admin 应用目录，git 已忽略）。
 * 兼容尚未改名的旧文件 init_mysql_db（按行）
 */
const INIT_MYSQL_DB_FILE = 'maozi-cloud-script/maozi-cloud-utils/init_mysql_db.json'
const INIT_MYSQL_DB_FILE_LEGACY = 'maozi-cloud-script/maozi-cloud-utils/init_mysql_db'

/** 读取初始化脚本定义：优先 init_mysql_db.json，缺失时回退旧 init_mysql_db；均无返回 null */
async function readInitMysqlDb(): Promise<string | null> {
  if (await projFileExists(INIT_MYSQL_DB_FILE)) return readProjFile(INIT_MYSQL_DB_FILE)
  if (await projFileExists(INIT_MYSQL_DB_FILE_LEGACY)) return readProjFile(INIT_MYSQL_DB_FILE_LEGACY)
  return null
}

/** 初始化镜像定义文件：JSON，key = 镜像名（含 tag），value = 构建目录（相对 DOCKER_IMAGE_DIR） */
const INIT_BASE_IMAGE_FILE = 'maozi-cloud-script/maozi-cloud-utils/init_base_image.json'
const DOCKER_IMAGE_DIR = 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-image'

interface HostsDef {
  ip: string
  domains: string
}

/**
 * 解析 init_hosts.json（key = ip，value = 域名）：同一 ip 可出现多条（JSON.parse 会丢弃
 * 重复 key，改对原始文本逐条正则提取）；value 支持单个域名、逗号/空格分隔多个、字符串数组
 */
function parseProjectHosts(text: string): HostsDef[] {
  const defs: HostsDef[] = []
  const re = /"([^"]+)"\s*:\s*(?:"([^"]*)"|\[([^\]]*)\])/g
  let m: RegExpExecArray | null
  while ((m = re.exec(text)) !== null) {
    const ip = m[1].trim()
    const raw = m[2] !== undefined ? m[2] : m[3] ?? ''
    const domains = raw
      .split(/[,，\s]+/)
      .map((s) => s.trim().replace(/^"|"$/g, ''))
      .filter(Boolean)
      .join(' ')
    if (!ip || !domains) continue
    if (!/^[0-9a-fA-F:.]+$/.test(ip)) continue
    defs.push({ ip, domains })
  }
  return defs
}

/**
 * 解析 environment_variable.json（JSON）：一级属性 = 分节（Tab）。节内 value 为字符串是直接映射，
 * 对象为分组（组内 key 为中文名称、value 为变量 key 字符串）；顶层 value 为字符串的旧两级
 * 格式归入未命名分节，保持兼容
 */
function parseEnvVarDefs(text: string): EnvSettingSection[] {
  let data: unknown
  try {
    data = JSON.parse(text)
  } catch (err) {
    throw new Error(`environment_variable.json 不是合法 JSON：${(err as Error).message}`)
  }
  if (typeof data !== 'object' || data === null || Array.isArray(data)) {
    throw new Error('environment_variable.json 格式错误：顶层应为 JSON 对象')
  }
  const pushItem = (sec: EnvSettingSection, group: string, label: string, key: string): void => {
    let g = sec.groups.find((x) => x.name === group)
    if (!g) {
      g = { name: group, items: [] }
      sec.groups.push(g)
    }
    g.items.push({ label, key, value: '', found: false, enabled: false, source: '' })
  }
  const sections: EnvSettingSection[] = []
  for (const [name, val] of Object.entries(data as Record<string, unknown>)) {
    if (typeof val === 'string') {
      if (!sections[0] || sections[0].name) sections.unshift({ name: '', source: 'env', groups: [] })
      pushItem(sections[0], '', name, val)
      continue
    }
    if (typeof val !== 'object' || val === null || Array.isArray(val)) {
      throw new Error(`environment_variable.json 中「${name}」的值应为字符串或对象`)
    }
    const sec: EnvSettingSection = { name, source: 'env', groups: [] }
    for (const [label, entry] of Object.entries(val as Record<string, unknown>)) {
      if (typeof entry === 'string') {
        pushItem(sec, '', label, entry)
        continue
      }
      if (typeof entry !== 'object' || entry === null || Array.isArray(entry)) {
        throw new Error(`environment_variable.json 中「${name}.${label}」的值应为字符串或对象`)
      }
      for (const [l2, key] of Object.entries(entry as Record<string, unknown>)) {
        if (typeof key !== 'string') throw new Error(`environment_variable.json「${name}.${label}.${l2}」的值应为环境变量 key 字符串`)
        pushItem(sec, label, l2, key)
      }
    }
    sections.push(sec)
  }
  // 分节取值来源：第一节 = 系统/shell 环境变量（原行为），第二节 = 业务 .env 文件
  if (sections[1]) {
    sections[1].source = 'file'
    sections[1].file = BUSINESS_ENV_FILE
  }
  return sections
}

/** 解析 .env（每行 key=value，# 注释与空行忽略）：key → value */
function parseEnvFile(text: string): Map<string, string> {
  const map = new Map<string, string>()
  for (const raw of text.split('\n')) {
    const line = raw.trim()
    if (!line || line.startsWith('#')) continue
    const eq = line.indexOf('=')
    if (eq <= 0) continue
    map.set(line.slice(0, eq).trim(), line.slice(eq + 1).trim())
  }
  return map
}

/** 业务 .env 写入/删除一个 key 行（value 为 null 时删除该行）：原位更新或末尾追加，本地直写 / 远程 SSH */
async function businessEnvFileSet(key: string, value: string | null): Promise<void> {
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) throw new Error(`变量名不合法: ${key}`)
  if (value !== null && /[\r\n]/.test(value)) throw new Error('变量值不能包含换行')
  const ctx = getProjectCtx()
  const abs = path.join(ctx.root, BUSINESS_ENV_FILE)
  if (ctx.isRemote && ctx.ssh) {
    if (value === null) {
      await sshExec(
        ctx.ssh,
        `test -f '${abs}' && grep -v '^${key}=' '${abs}' > '${abs}.mzi' 2>/dev/null || true; mv '${abs}.mzi' '${abs}'`,
        15_000
      )
    } else {
      const safeVal = value.replace(/'/g, "'\\''")
      await sshExec(
        ctx.ssh,
        `test -f '${abs}' && grep -v '^${key}=' '${abs}' > '${abs}.mzi' || true; echo '${key}=${safeVal}' >> '${abs}.mzi'; mv '${abs}.mzi' '${abs}'`,
        15_000
      )
    }
    return
  }
  if (!fs.existsSync(abs)) {
    if (value === null) return
    fs.writeFileSync(abs, `${key}=${value}\n`, 'utf8')
    return
  }
  const lines = fs.readFileSync(abs, 'utf8').split('\n')
  const idx = lines.findIndex((l) => l.startsWith(`${key}=`))
  if (value === null) {
    if (idx >= 0) lines.splice(idx, 1)
  } else if (idx >= 0) {
    lines[idx] = `${key}=${value}`
  } else {
    // 末尾追加（跳过文件尾部空行，保留原有注释与顺序）
    let end = lines.length
    while (end > 0 && lines[end - 1].trim() === '') end--
    lines.splice(end, 0, `${key}=${value}`)
  }
  fs.writeFileSync(abs, lines.join('\n'), 'utf8')
}

/**
 * 环境变量取值：shell 配置里已启用的最优先（文件顺序即展示顺序，zshrc/bash_profile 靠前），
 * 其次当前进程环境（覆盖 launchctl setenv 场景），最后保留被注释禁用行的值。
 * 空值一律视为未设置（export KEY="" / 进程环境为空串 = 清空取值，不算已配置）
 */
function applyEnvValues(groups: EnvSettingGroup[], vars: EnvVarEntry[], files: EnvFile[]): void {
  const fileNames = new Map(files.map((f) => [f.id, f.name]))
  const unset = (key: string): EnvSettingItem => ({ label: '', key, value: '', found: false, enabled: false, source: '' })
  const lookup = (key: string): EnvSettingItem => {
    const enabled = vars.find((v) => v.key === key && v.enabled)
    if (enabled) {
      if (enabled.value === '') return unset(key)
      return { label: '', key, value: enabled.value, found: true, enabled: true, source: fileNames.get(enabled.fileId) ?? '', fileId: enabled.fileId }
    }
    if (process.env[key]) {
      return { label: '', key, value: process.env[key] ?? '', found: true, enabled: true, source: '进程环境' }
    }
    const disabled = vars.find((v) => v.key === key)
    if (disabled) {
      if (disabled.value === '') return unset(key)
      return { label: '', key, value: disabled.value, found: true, enabled: false, source: fileNames.get(disabled.fileId) ?? '', fileId: disabled.fileId }
    }
    return unset(key)
  }
  for (const g of groups) {
    g.items = g.items.map((it) => ({ ...it, ...lookup(it.key), label: it.label }))
  }
}

/** 后台前端容器（maozi-cloud-admin-distributeds）compose 目录 / 文件与容器名 */
const ADMIN_COMPOSE_DIR =
  'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-business-docker'
const ADMIN_COMPOSE_FILE = 'maozi-cloud-admin-distributeds-docker.yml'
const ADMIN_CONTAINER = 'maozi-cloud-admin-distributeds'

/**
 * 应用服务（单体 / 微服务）compose 定义：两变体共用 maozi-cloud-business-docker 目录，
 * 各含 admin 与 services 两个 compose 文件。
 * 两变体互斥：启动任一变体服务前，先 down 掉另一变体的全部服务
 */
type AppSvcVariant = 'monomer' | 'distributeds'

/** 变体的 compose 文件清单：统一含 nginx，全部启停/互斥关闭/状态匹配均按整组处理 */
const appSvcFiles = (def: { servicesFile: string; adminFile: string; nginxFile: string }): string[] => [
  def.servicesFile,
  def.adminFile,
  def.nginxFile
]

const APP_SVC_DEFS: Record<
  AppSvcVariant,
  { label: string; dir: string; adminFile: string; servicesFile: string; nginxFile: string }
> = {
  monomer: {
    label: '单体',
    dir: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-business-docker',
    adminFile: 'maozi-cloud-admin-monomer-docker.yml',
    servicesFile: 'maozi-cloud-services-monomer-docker.yml',
    nginxFile: 'maozi-cloud-nginx-monomer-docker.yml'
  },
  distributeds: {
    label: '微服务',
    dir: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker/maozi-cloud-business-docker',
    adminFile: 'maozi-cloud-admin-distributeds-docker.yml',
    servicesFile: 'maozi-cloud-services-distributeds-docker.yml',
    nginxFile: 'maozi-cloud-nginx-distributeds-docker.yml'
  }
}

/** 应用服务日志/操作上下文：'basics' 或 '<variant>:<admin|services|nginx>' */
function resolveComposeCtx(ctx: string): { dir: string; file: string } | null {
  if (ctx === 'basics') return null
  const m = ctx.match(/^(monomer|distributeds):(admin|services|nginx)$/)
  if (!m) return null
  const def = APP_SVC_DEFS[m[1] as AppSvcVariant]
  return {
    dir: def.dir,
    file: m[2] === 'admin' ? def.adminFile : m[2] === 'nginx' ? def.nginxFile : def.servicesFile
  }
}

/** ${VAR} / ${VAR:-默认} 插值（由内向外逐轮展开，支持嵌套默认值）；未定义且无默认值的变量替换为空串 */
function interpolateTemplate(tpl: string, values: Record<string, string>): string {
  let out = tpl
  for (let round = 0; round < 6 && out.includes('${'); round++) {
    out = out.replace(/\$\{([^${}]+)\}/g, (_m, expr: string) => {
      const dm = expr.match(/^([A-Za-z_][A-Za-z0-9_]*):-(.*)$/)
      if (dm) {
        const v = values[dm[1]]
        return v !== undefined && v !== '' ? v : dm[2]
      }
      return values[expr] ?? ''
    })
  }
  return out
}

/** 应用服务名称插值取值：系统/shell 环境打底，业务 .env 覆盖（ENVIRONMENT / VERSION 等编排变量） */
async function appSvcNameValues(): Promise<Record<string, string>> {
  let values: Record<string, string> = {}
  try {
    values = buildChildEnv()
  } catch {
    /* 非 darwin 或读取失败：退回进程环境（buildChildEnv 内部已兜底，此处双保险） */
  }
  try {
    const dotEnv = parseEnvFile(await readProjFile(BUSINESS_ENV_FILE))
    values = { ...values, ...Object.fromEntries(dotEnv) }
  } catch {
    /* .env 缺失时仅用系统环境 */
  }
  return values
}

/** 应用服务 compose 项目名：-p maozi-cloud-business-docker-${ENVIRONMENT:-${APPLICATION_ENVIRONMENT:-dev}}-${VERSION:-${APPLICATION_VERSION:-main}}，
 *  与部署脚本 -p 一致。compose 不对 -p 做变量插值，须在此按 yml 同款回退链展开；
 *  取值与容器名插值同源（.env 优先，逐级回退 APPLICATION_* / dev / main） */
async function appSvcProjectName(): Promise<string> {
  const v = await appSvcNameValues()
  const environment = v.ENVIRONMENT || v.APPLICATION_ENVIRONMENT || 'dev'
  const version = v.VERSION || v.APPLICATION_VERSION || 'main'
  return `maozi-cloud-business-docker-${environment}-${version}`
}

/** ===== 应用服务「接口不停机更新」热备（单体 / 微服务通用） ===== */
/** 热备 compose 文件：变体 services yml 的拷贝，container_name 统一加 -backup 后缀 */
function hotSwapBackupFile(variant: AppSvcVariant): string {
  return variant === 'monomer'
    ? 'maozi-cloud-services-monomer-docker-backup.yml'
    : 'maozi-cloud-services-distributeds-docker-backup.yml'
}

/** 开关持久化在 .ui-state.json（与 Tab 记忆同存储），重启应用后仍生效 */
function hotSwapOn(variant: AppSvcVariant): boolean {
  const key = variant === 'monomer' ? 'monomerHotSwap' : 'distributedsHotSwap'
  try {
    return JSON.parse(fs.readFileSync(uiStateFile(), 'utf8'))[key] === true
  } catch {
    return false
  }
}

function setHotSwap(variant: AppSvcVariant, on: boolean): void {
  const key = variant === 'monomer' ? 'monomerHotSwap' : 'distributedsHotSwap'
  let cur: Record<string, unknown> = {}
  try {
    cur = JSON.parse(fs.readFileSync(uiStateFile(), 'utf8'))
  } catch {
    /* 首次创建 */
  }
  fs.writeFileSync(uiStateFile(), JSON.stringify({ ...cur, [key]: on }, null, 2), 'utf8')
}

/** 写项目内文件（本地 fs 或 SSH base64 回传） */
async function writeProjFile(relPath: string, content: string): Promise<void> {
  const ctx = getProjectCtx()
  const abs = path.join(ctx.root, relPath)
  if (ctx.isRemote && ctx.ssh) {
    const b64 = Buffer.from(content, 'utf8').toString('base64')
    await sshExec(ctx.ssh, `printf '%s' '${b64}' | base64 -d > '${abs}'`, 15_000)
    return
  }
  fs.writeFileSync(abs, content, 'utf8')
}

/** 删除项目内文件 */
async function deleteProjFile(relPath: string): Promise<void> {
  const ctx = getProjectCtx()
  const abs = path.join(ctx.root, relPath)
  if (ctx.isRemote && ctx.ssh) {
    await sshExec(ctx.ssh, `rm -f '${abs}'`, 10_000)
    return
  }
  if (fs.existsSync(abs)) fs.unlinkSync(abs)
}

/** 生成热备 compose：拷贝变体 services yml，所有 container_name 追加 -backup（占位符原样保留） */
async function writeHotSwapBackupYml(variant: AppSvcVariant, sender: Electron.WebContents, sid: string): Promise<void> {
  const def = APP_SVC_DEFS[variant]
  const backupFile = hotSwapBackupFile(variant)
  const src = await readProjFile(path.join(def.dir, def.servicesFile))
  const out = src.replace(/^(\s*container_name:\s*\S+)$/gm, '$1-backup')
  if (out === src) throw new Error(`未在 ${def.servicesFile} 中找到 container_name，无法生成热备文件`)
  await writeProjFile(path.join(def.dir, backupFile), out)
  if (!sender.isDestroyed()) {
    sender.send('projects:scriptLog', {
      kind: 'line',
      text: `✓ 已生成热备文件 ${backupFile}（container_name 加 -backup 后缀）`,
      sid
    })
  }
}

/** ===== 应用服务「接口不停机更新」优雅更新钩子（单体 / 微服务各一对前置/后置脚本） ===== */
const GRACEFUL_UPDATE_HOOKS: Record<AppSvcVariant, { before: string; after: string }> = {
  monomer: {
    before: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-services-monomer-graceful-update-before.sh',
    after: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-services-monomer-graceful-update-after.sh'
  },
  distributeds: {
    before: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-services-distributed-graceful-update-before.sh',
    after: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-services-distributed-graceful-update-after.sh'
  }
}

/**
 * 优雅更新前置/后置钩子（尽力而为，失败仅告警不中断主流程）：
 * before 拉起 -backup 热备组（独立 compose 项目）并等全部健康，主组才可安全停机更新；
 * after 等主组全部恢复健康后 down 热备组，流量回到主组。
 * 可选 service 只热备/回收该服务（脚本接受 compose 服务名或容器名，控制台传服务名），
 * 不传处理整组（编译启动 / 全部重启）。
 * 脚本返回值约定：1 = 成功，2 = 失败；projStream 非零退出即抛错，捕获后按 exit 1 判定成功
 */
async function gracefulUpdateHook(
  variant: AppSvcVariant,
  sender: Electron.WebContents,
  sid: string,
  kind: 'before' | 'after',
  service?: string
): Promise<void> {
  const script = GRACEFUL_UPDATE_HOOKS[variant][kind]
  const send = (text: string): void => {
    if (!sender.isDestroyed()) sender.send('projects:scriptLog', { kind: 'line', text, sid })
  }
  // 热备 compose 缺失时现生成（开关开启时已生成，兜底分支切换/文件被删场景），脚本依赖它拉起热备组
  if (!(await projFileExists(path.join(APP_SVC_DEFS[variant].dir, hotSwapBackupFile(variant))))) {
    await writeHotSwapBackupYml(variant, sender, sid)
  }
  if (!(await projFileExists(script))) {
    send(`⚠ [优雅更新] 未找到${kind === 'before' ? '前置' : '后置'}脚本 ${script}，跳过热备${kind === 'before' ? '拉起' : '回收'}`)
    return
  }
  send(
    `▶ [优雅更新] ${kind === 'before' ? '前置：拉起热备承接流量' : '后置：等主组健康后回收热备'}（${service ? `服务：${service}` : '整组'}）bash ${script}${service ? ` ${service}` : ''}`
  )
  const okLine =
    kind === 'before'
      ? `✓ [优雅更新] ${service ? `服务 ${service} 热备已启动且健康` : '热备组已全部启动且健康'}`
      : `✓ [优雅更新] ${service ? `主服务 ${service} 已恢复健康，热备已回收` : '主组已恢复健康，热备组已回收'}`
  try {
    // 脚本内部健康等待最长 300s、down 超时最长 150s，整体上限放宽到 10 分钟
    await projStream(sender, 'projects:scriptLog', 'bash', service ? [script, service] : [script], {
      timeoutMs: 10 * 60_000,
      sid
    })
    send(okLine)
  } catch (err) {
    const msg = (err as Error).message
    if (msg === '已手动中断') {
      // 中断不算失败：调用方据 stopRequested 标记跳过主流程（热备组回收见各调用点）
      send(`⏹ [优雅更新] ${kind === 'before' ? '前置' : '后置'}脚本已中断`)
    } else if (msg === 'exit 1') {
      send(okLine)
    } else {
      send(`⚠ [优雅更新] ${kind === 'before' ? '前置' : '后置'}脚本失败（${msg}），继续原流程`)
    }
  }
}

/** 读取变体的服务清单（admin + services 两个文件合并，标记归属文件）。
 *  服务名可含 ${VAR} 占位（按环境/版本区分实例）：name 为插值后的真实名称
 *  （docker compose 命令与状态匹配用），base 为占位前的静态前缀（前端展示用） */
async function readAppServices(bPath: string, variant: AppSvcVariant): Promise<AppServiceEntry[]> {
  const def = APP_SVC_DEFS[variant]
  const interp = await appSvcNameValues()
  const out: AppServiceEntry[] = []
  const read = async (fileName: string, tag: 'admin' | 'services' | 'nginx'): Promise<void> => {
    const ymlRel = path.join(def.dir, fileName)
    try {
      const ymlContent = await readProjFileCached(ymlRel)
      for (const raw of parseComposeServices(ymlContent)) {
        const name = interpolateTemplate(raw, interp)
        out.push({ name, file: tag, base: raw.split('${')[0].replace(/-+$/, '') || name })
      }
    } catch { /* 文件不存在时跳过 */ }
  }
  // nginx 流量入口排首位，其后是 admin 前端与 services 后端
  await read(def.nginxFile, 'nginx')
  await read(def.adminFile, 'admin')
  await read(def.servicesFile, 'services')
  return out
}

/** 变体所有服务的容器名映射（插值后的服务名 → 插值后的 container_name，读取两个 yml） */
async function appSvcContainerNames(bPath: string, variant: AppSvcVariant): Promise<Record<string, string>> {
  const def = APP_SVC_DEFS[variant]
  const interp = await appSvcNameValues()
  const raw: Record<string, string> = {}
  for (const fileName of appSvcFiles(def)) {
    try {
      const content = await readProjFileCached(path.join(def.dir, fileName))
      Object.assign(raw, parseServiceContainerNames(content))
    } catch { /* 文件不存在时跳过 */ }
  }
  const map: Record<string, string> = {}
  for (const [svc, container] of Object.entries(raw)) {
    map[interpolateTemplate(svc, interp)] = interpolateTemplate(container, interp)
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
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-services-distributed.sh',
    label: '微服务按需编译启动'
  },
  all: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-services-distributed-force.sh',
    label: '微服务全量启动'
  },
  admin: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-admin-distributed.sh',
    label: '后台启动'
  },
  monomerServices: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-services-monomer.sh',
    label: '单体服务编译启动'
  },
  monomerAdmin: {
    file: 'maozi-cloud-script/maozi-cloud-deploy/maozi-cloud-deploy-docker-run/maozi-cloud-deploy-admin-monomer.sh',
    label: '后台编译启动'
  },
  dockerClear: {
    file: 'maozi-cloud-script/maozi-cloud-utils/docker-clear.sh',
    label: '容器磁盘清除'
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

/**
 * 解析 init_mysql_db.json：JSON 数组，每项一个 SQL 脚本路径（相对项目根）。
 * 兼容旧格式（无 .json 后缀、每行一个路径）与解析失败时的逐行回退
 */
function parseInitMysqlDb(text: string): string[] {
  try {
    const data: unknown = JSON.parse(text)
    if (Array.isArray(data)) {
      return data.filter((s): s is string => typeof s === 'string' && !!s.trim()).map((s) => s.trim())
    }
  } catch {
    /* 非 JSON（旧格式）：按行解析 */
  }
  const out: string[] = []
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line || line.startsWith('#') || line.startsWith('[') || line.startsWith(']')) continue
    out.push(line.replace(/",?$/, '').replace(/^"/, ''))
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
    // 服务名可含 ${VAR}/${VAR:-默认} 占位（按环境/版本区分实例）：
    // 起始非空白 + 贪婪匹配到行尾冒号（占位符内也含冒号，不能截断）
    const m = raw.match(/^ {2}(\S.*):\s*(?:#.*)?$/)
    if (m) services.push(m[1])
  }
  return services
}

/** docker ps 的 Status 文本 → 健康状态：含 (healthy)/(unhealthy)/(health: starting) 标记时返回对应值，否则空串 */
function parseHealthMark(status: string): string {
  if (status.includes('(healthy)')) return 'healthy'
  if (status.includes('(unhealthy)')) return 'unhealthy'
  if (status.includes('(health: starting)')) return 'starting'
  return ''
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
    // 服务名可含 ${VAR} 占位（与 parseComposeServices 同规则：贪婪匹配到行尾冒号）
    const mSvc = raw.match(/^ {2}(\S.*):\s*(?:#.*)?$/)
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
      // 被中断的会话保留 stopRequested 标记（无 child）：热备健康等待与后续流程据此中止
      if (sid) {
        if (stopped) runningProcs.set(sid, { stopRequested: true })
        else runningProcs.delete(sid)
      }
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

/** Maven 探测：mvn -v 解析版本与 Maven home，定位 conf/settings.xml（本地/远程绑定各自在对应机器执行） */
async function mavenDetect(b: ProjectBinding): Promise<{ version: string; home: string; settingsFile: string }> {
  let out: string
  if (b.remote) {
    const t = resolveSshTarget(b.remote.configId)
    out = await sshExec(t, 'mvn -v 2>&1', 20_000)
  } else {
    const pathEnv = await getShellPath()
    const r = await exec('mvn', ['-v'], { timeout: 20_000, env: { ...process.env, PATH: pathEnv } })
    out = r.stdout + r.stderr
  }
  const version = out.match(/Apache Maven\s+(\S+)/)?.[1] ?? ''
  // Maven home 路径可能含空格（如 IntelliJ 自带 /Applications/IntelliJ IDEA.app/...），必须整行取值
  const home = out
    .split(/\r?\n/)
    .map((l) => l.trim())
    .find((l) => l.startsWith('Maven home:'))
    ?.slice('Maven home:'.length)
    .trim() ?? ''
  if (!home) throw new Error('mvn -v 输出中未找到 Maven home（请确认已安装 Maven）')
  return { version, home, settingsFile: path.posix.join(home.replace(/\/+$/, ''), 'conf/settings.xml') }
}

/** Docker daemon.json 路径：远程按 Linux 语义（/etc/docker 优先，回退 $HOME/.docker）；本地按平台（mac/Windows 为 ~/.docker） */
async function dockerConfigPath(b: ProjectBinding): Promise<string> {
  if (b.remote) {
    const t = resolveSshTarget(b.remote.configId)
    const home = ((await sshExec(t, 'echo $HOME', 10_000)).trim().split('\n').pop() ?? '').replace(/\/+$/, '')
    const homeDaemon = home ? `${home}/.docker/daemon.json` : '~/.docker/daemon.json'
    const probe = await sshExec(
      t,
      'test -f /etc/docker/daemon.json && echo __ETC__; test -f "$HOME/.docker/daemon.json" && echo __HOME__; true',
      10_000
    )
    if (probe.includes('__ETC__')) return '/etc/docker/daemon.json'
    if (probe.includes('__HOME__')) return homeDaemon
    return '/etc/docker/daemon.json'
  }
  const homeDaemon = path.join(os.homedir(), '.docker', 'daemon.json')
  const etcDaemon = '/etc/docker/daemon.json'
  const candidates = process.platform === 'linux' ? [etcDaemon, homeDaemon] : [homeDaemon]
  return candidates.find((f) => fs.existsSync(f)) ?? candidates[0]
}

export function registerProjectHandlers(): void {
  ipcMain.handle('projects:state', () => {
    try {
      return { ok: true, data: getBinding() }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 项目列表（含绑定信息与当前激活项） */
  ipcMain.handle('projects:projectsList', () => {
    try {
      return { ok: true, data: listProjects() }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 创建项目（名称不允许中文，仅字母数字与 . _ -）：创建后即激活，前端接着进入选择绑定方式 */
  ipcMain.handle('projects:projectCreate', (_e, input: { name?: string; alias?: string; remark?: string }) => {
    try {
      const entry = createProject({
        name: String(input?.name ?? ''),
        alias: String(input?.alias ?? ''),
        remark: String(input?.remark ?? '')
      })
      clearYmlCache()
      return { ok: true, data: entry }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 激活项目：已绑定返回绑定信息（进入控制台），待绑定返回 null（进入绑定向导） */
  ipcMain.handle('projects:projectActivate', (_e, id: string) => {
    try {
      const binding = activateProject(String(id ?? ''))
      clearYmlCache()
      composeOk = null
      return { ok: true, data: binding }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 更新项目信息（名称/别名/备注）：名称校验同创建 */
  ipcMain.handle(
    'projects:projectUpdate',
    (_e, id: string, patch: { name?: string; alias?: string; remark?: string }) => {
      try {
        const entry = updateProject(String(id ?? ''), patch ?? {})
        return { ok: true, data: entry }
      } catch (err) {
        return { ok: false, error: (err as Error).message }
      }
    }
  )

  /** 返回项目列表：仅清空激活项 */
  ipcMain.handle('projects:projectDeactivate', () => {
    try {
      deactivateProject()
      clearYmlCache()
      composeOk = null
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 删除项目（项目记录与其绑定信息一并移除） */
  ipcMain.handle('projects:projectRemove', (_e, id: string) => {
    try {
      removeProject(String(id ?? ''))
      clearYmlCache()
      composeOk = null
      return { ok: true }
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

  /** 项目是否 git 仓库 + 当前分支 + 本地最后一次提交（短 sha/时间）（非仓库 / 未装 git 时 isRepo=false，不报错） */
  ipcMain.handle('projects:gitInfo', async () => {
    const data = { isRepo: false, branch: '', lastSha: '', lastTime: '' }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      // 提取 "短sha|提交时间"（%ci 形如 2026-09-29 14:30:25 +0800）
      const pickLastCommit = (lines: string): void => {
        const hit = lines
          .split(/\r?\n/)
          .map((l) => l.trim())
          .find((l) => /^[0-9a-f]{7,}\|/.test(l))
        if (!hit) return
        const sep = hit.indexOf('|')
        data.lastSha = hit.slice(0, sep)
        data.lastTime = hit.slice(sep + 1).trim()
      }
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        // 分段带标记、各自吞错：&& 链会让 git log 异常（如空仓库/旧版 git）拖垮整条命令，
        // git 信息整体不显示；标记间取分支名，sha| 模式行取最后提交；
        // --show-current 需 git ≥ 2.22，旧版回退 rev-parse；
        // --format 必须引号包裹：远端经 shell 解释，裸 | 会被当管道符把输出吞进不存在的命令
        const out = await sshExec(
          t,
          `cd ${JSON.stringify(b.path)} && echo __MZ_BRANCH__ && { git branch --show-current 2>/dev/null || git rev-parse --abbrev-ref HEAD 2>/dev/null; }; echo __MZ_LOG__; git log -1 --format='%h|%ci' 2>/dev/null; true`,
          10_000
        )
        const lines = out.split(/\r?\n/).map((l) => l.trim())
        const brIdx = lines.indexOf('__MZ_BRANCH__')
        const logIdx = lines.indexOf('__MZ_LOG__')
        if (brIdx >= 0) {
          const cand = lines
            .slice(brIdx + 1, logIdx > brIdx ? logIdx : undefined)
            .filter((l) => l && !/^[0-9a-f]{7,}\|/.test(l))
          data.branch = cand[0] ?? ''
        }
        data.isRepo = data.branch !== ''
        pickLastCommit(out)
        return { ok: true, data: { ...data } }
      }
      const pathEnv = await getShellPath()
      const env = { ...process.env, PATH: pathEnv }
      const { stdout } = await exec('git', ['branch', '--show-current'], {
        cwd: b.path,
        timeout: 10_000,
        env
      })
      const branch = stdout.trim()
      if (branch) {
        const log = await exec('git', ['log', '-1', '--format=%h|%ci'], { cwd: b.path, timeout: 10_000, env })
        pickLastCommit(log.stdout)
      }
      return { ok: true, data: { ...data, isRepo: branch !== '', branch } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data }
    }
  })

  /** 分支列表：本地（git branch，离线）+ 远程（git ls-remote --heads，需网络；浅克隆本地无其他分支引用也能列出全部） */
  ipcMain.handle('projects:gitBranches', async () => {
    const data = { branches: [] as string[], locals: [] as string[], remoteError: '' }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      // 本地分支不走网络；--format 输出不带当前分支 * 前缀，逐行即分支名
      if (b.remote) {
        const out = await sshExec(resolveSshTarget(b.remote.configId), `cd ${JSON.stringify(b.path)} && git branch --format='%(refname:short)'`, 30_000)
        data.locals = out.split(/\r?\n/).map((l) => l.trim().replace(/^\* ?/, '')).filter(Boolean)
      } else {
        const pathEnv = await getShellPath()
        const r = await exec('git', ['branch', '--format=%(refname:short)'], {
          cwd: b.path,
          timeout: 30_000,
          env: { ...process.env, PATH: pathEnv }
        })
        data.locals = r.stdout.split(/\r?\n/).map((l) => l.trim().replace(/^\* ?/, '')).filter(Boolean)
      }
      // 远程分支：ls-remote 同样访问 GitHub，先解析 VPN 代理（静默不打日志），可用则注入
      const proxy = await resolveGitProxy(b)
      let stdout = ''
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
      data.branches = stdout
        .split(/\r?\n/)
        .map((l) => l.split('\t')[1] ?? '')
        .filter((ref) => ref.startsWith('refs/heads/'))
        .map((ref) => ref.slice('refs/heads/'.length))
        .filter(Boolean)
    } catch (err) {
      // 本地分支已到手则不算整体失败：远程不可达（超时/断网）时降级为仅本地列表
      if (data.locals.length > 0) {
        data.remoteError = (err as Error).message
        return { ok: true, data }
      }
      return { ok: false, error: (err as Error).message, data }
    }
    return { ok: true, data }
  })

  /**
   * 定时检测远程新提交：git fetch 后统计本地落后上游的提交数（behind）。
   * 非仓库 / 无上游 / 网络失败一律静默返回 behind=0（探测语义，不报错）
   */
  ipcMain.handle('projects:gitRemoteCheck', async () => {
    const data = { isRepo: false, branch: '', behind: 0, lastSha: '', lastTime: '' }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const proxy = await resolveGitProxy(b)
      const run = async (args: string[], timeout: number): Promise<string> => {
        if (b.remote) {
          const t = resolveSshTarget(b.remote.configId)
          // 远端命令经 shell 解释：含 | { } 等元字符的参数（--format=%h|%ci、HEAD..@{u}）必须引号包裹，
          // 否则 | 被当管道把输出吞掉（本地分支走 exec argv 无 shell，不需要也不能加引号）
          const remoteSafe = args
            .map((a) => (/^[\w./@:=,-]+$/.test(a) ? a : `'${a.replace(/'/g, "'\\''")}'`))
            .join(' ')
          return sshExec(t, `${proxy.sshEnvPrefix ?? ''}cd ${JSON.stringify(b.path)} && git ${remoteSafe}`, timeout)
        }
        const pathEnv = await getShellPath()
        const { stdout } = await exec('git', args, {
          cwd: b.path,
          timeout,
          env: { ...process.env, PATH: pathEnv, ...(proxy.extraEnv ?? {}) }
        })
        return stdout
      }
      const branch = (await run(['branch', '--show-current'], 10_000)).trim()
      if (!branch) return { ok: true, data }
      data.isRepo = true
      data.branch = branch
      // 本地最后一次提交：短 sha + 提交时间（%ci 形如 2026-09-29 14:30:25 +0800）
      const log = (await run(['log', '-1', '--format=%h|%ci'], 10_000)).trim()
      const sep = log.indexOf('|')
      if (sep > 0) {
        data.lastSha = log.slice(0, sep)
        data.lastTime = log.slice(sep + 1).trim()
      }
      await run(['fetch', 'origin', '--quiet', '--prune'], 30_000)
      const cnt = (await run(['rev-list', '--count', 'HEAD..@{u}'], 10_000)).trim()
      data.behind = parseInt(cnt, 10) || 0
      return { ok: true, data }
    } catch {
      return { ok: true, data }
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

  /** 切换分支：远程分支先 fetch（浅克隆兼容）再 checkout，并确保关联上游；输出走 projects:scriptLog */
  ipcMain.handle('projects:gitCheckout', async (event, branchArg: string, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const branch = String(branchArg ?? '').trim()
      if (!/^[A-Za-z0-9._/-]{1,100}$/.test(branch) || branch.includes('..') || branch.startsWith('/') || branch.endsWith('/')) {
        throw new Error('非法的分支名')
      }
      const send = (text: string): void => {
        if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
      }
      // 快捷 git 查询/设置（离线）：本地绑定走 exec argv；SSH 绑定拼远端命令，含 ( ) 等元字符的参数须引号包裹
      const quiet = async (args: string[], timeout = 15_000): Promise<string> => {
        if (b.remote) {
          const remoteSafe = args.map((a) => (/^[\w./@:=,-]+$/.test(a) ? a : `'${a.replace(/'/g, "'\\''")}'`)).join(' ')
          return sshExec(resolveSshTarget(b.remote.configId), `cd ${JSON.stringify(b.path)} && git ${remoteSafe}`, timeout)
        }
        const pathEnv = await getShellPath()
        const { stdout } = await exec('git', args, { cwd: b.path, timeout, env: { ...process.env, PATH: pathEnv } })
        return stdout
      }
      // 克隆带 --depth 1 --single-branch，refspec 只含主分支：目标分支不在 refspec 内时，
      // fetch 不建远端跟踪引用，checkout 的 DWIM/--track/pull 又都按 refspec 反查而落空，
      // 便报 pathspec 不匹配。先把 refspec 放开为全分支（幂等，此后任意分支可切、pull 可用）
      const ensureRefspec = (): Promise<string> => quiet(['config', 'remote.origin.fetch', '+refs/heads/*:refs/remotes/origin/*'])
      // 取回目标分支走 GitHub：先看 VPN 代理是否可用，可用则注入 git 代理环境（checkout 本身无网络）
      const fetchBranch = async (): Promise<void> => {
        const proxy = await prepareGitProxy(event.sender, 'projects:scriptLog', sid, b)
        await projStream(event.sender, 'projects:scriptLog', 'git', [...GIT_STALL_ARGS, 'fetch', '--progress', '--depth', '1', 'origin', `+refs/heads/${branch}:refs/remotes/origin/${branch}`], {
          cwd: b.path,
          timeoutMs: 10 * 60_000,
          sid,
          extraEnv: proxy.extraEnv,
          sshEnvPrefix: proxy.sshEnvPrefix
        })
      }
      // 本地已有该分支（含仅存在于本地的分支）时无需联网取回：直接 checkout 等价普通切换（不动本地提交）；
      // 远程分支首次切换才 fetch 建远端跟踪引用，checkout 的 DWIM 随之建本地分支并自动设上游
      let isLocal = false
      try {
        isLocal = (await quiet(['branch', '--list', branch, '--format=%(refname:short)'])).trim() === branch
      } catch {
        isLocal = false
      }
      if (!isLocal) {
        await ensureRefspec()
        await fetchBranch()
      }
      await projStream(event.sender, 'projects:scriptLog', 'git', ['checkout', '--progress', branch], {
        cwd: b.path,
        timeoutMs: 60_000,
        sid
      })
      // ===== 关联远程分支：远程分支 DWIM 首切已自动设上游，但切到本地已有分支（终端自建 /
      // push 未带 -u / 早期版本切换产物）不会补关联，pull 与落后检测（HEAD..@{u}）随之失效——
      // 统一检测，缺关联则补设 origin/<branch>；关联失败不影响切换结果
      try {
        const upstream = (await quiet(['for-each-ref', '--format=%(upstream:short)', `refs/heads/${branch}`])).trim()
        if (!upstream) {
          let remoteRef = (await quiet(['for-each-ref', '--format=%(refname:short)', `refs/remotes/origin/${branch}`])).trim()
          let fetchOk = true
          if (!remoteRef) {
            // 远端跟踪引用缺失（本地分支路径此前没 fetch 过）：取回一次再补设
            try {
              await ensureRefspec()
              await fetchBranch()
              remoteRef = (await quiet(['for-each-ref', '--format=%(refname:short)', `refs/remotes/origin/${branch}`])).trim()
            } catch (e) {
              remoteRef = ''
              // "couldn't find remote ref" = 远程确实没有该分支（纯本地分支），不算取回失败
              fetchOk = !/couldn'?t find remote ref/i.test((e as Error).message)
            }
          }
          if (remoteRef) {
            await quiet(['branch', `--set-upstream-to=origin/${branch}`, branch])
            send(`✓ 已关联远程分支 origin/${branch}`)
          } else if (fetchOk) {
            send(`ℹ 远程不存在分支 ${branch}，跳过上游关联（纯本地分支）`)
          } else {
            send(`⚠ 取回远程分支失败，本次未关联上游（不影响切换，可稍后重试）`)
          }
        }
      } catch (err) {
        send(`⚠ 关联远程分支失败：${(err as Error).message}（不影响切换）`)
      }
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
      // 优雅更新：编译启动脚本执行前先跑前置钩子拉起热备组承接流量（尽力而为），脚本真正结束后后置回收。
      // 脚本 → 热备变体：单体编译启动 → 单体；微服务按需/全量编译启动 → 微服务
      const HOT_SWAP_SCRIPT_KINDS: Record<string, AppSvcVariant> = {
        monomerServices: 'monomer',
        demand: 'distributeds',
        all: 'distributeds'
      }
      const swapVariant = HOT_SWAP_SCRIPT_KINDS[key]
      const hotSwap = !!swapVariant && hotSwapOn(swapVariant)
      if (hotSwap && swapVariant) {
        await gracefulUpdateHook(swapVariant, event.sender, sid, 'before')
        // 前置钩子被中断：跳过脚本执行（此路径不走 finally，热备组保持运行，下次操作时回收）
        if (runningProcs.get(sid)?.stopRequested) {
          event.sender.send('projects:scriptLog', { kind: 'line', text: '⏹ 已中断，跳过脚本执行', sid })
          return { ok: true }
        }
      }
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
      } finally {
        if (hotSwap && swapVariant) await gracefulUpdateHook(swapVariant, event.sender, sid, 'after')
        // 清理无子进程的标记条目（被中断会话保留的 stopRequested 存根）
        const entry = runningProcs.get(sid)
        if (entry && !entry.child) runningProcs.delete(sid)
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
   * 基础服务启动/停止/重启：up -d <svc> / down / stop + rm <svc>（优雅停机，触发生命周期钩子），
   * 重启 = 先 stop + rm 移除容器再 up -d 重建，日志走 projects:scriptLog
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
            ? [['compose', 'stop', service], ['compose', 'rm', service]]
            : [['compose', 'stop', service], ['compose', 'rm', service], ['compose', 'up', '-d', service]]
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
            sid,
            // rm 不带 --force 会交互式确认，stdin 预置 y 自动应答
            stdinData: args.includes('rm') ? 'y\n' : undefined
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
          ? ['compose', '-p', await appSvcProjectName(), '-f', ADMIN_COMPOSE_FILE, 'up', '-d']
          : ['compose', '-p', await appSvcProjectName(), '-f', ADMIN_COMPOSE_FILE, 'down']
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
        send('▶ SSH 写入远程 /etc/hosts')
        // 非 root 用户无权直接写 /etc/hosts，经 sudo 提权；-p password: 让 sudo 提示词与 expect 应答模式匹配
        const priv = (cmd: string): string => (ctx.ssh?.user === 'root' ? cmd : `sudo -S -p password: ${cmd}`)
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
      if (/[\r\n]/.test(value)) throw new Error('变量值不能包含换行')
      // 值允许为空：写 export KEY=''（临时清空变量、保留 key）
      const REMOTE_ENV_FILES: Record<string, string> = {
        zshrc: '~/.zshrc', zshenv: '~/.zshenv', zprofile: '~/.zprofile',
        bashrc: '~/.bashrc', bash_profile: '~/.bash_profile', profile: '~/.profile'
      }
      const filePath = REMOTE_ENV_FILES[params.fileId]
      if (!filePath) throw new Error('未知的远程配置文件: ' + params.fileId)
      const safeVal = value.replace(/'/g, "'\\''")
      if (['zshrc', 'zshenv', 'zprofile'].includes(params.fileId)) {
        // zsh 系与本地环境设置同机制：值落远程实时环境文件 + ~/.zshrc 幂等装同步钩子（md5 兼容
        // macOS md5 与 Linux md5sum），已打开的远程终端下一条命令即生效，新开终端经钩子 source 生效
        await sshExec(
          ctx.ssh,
          [
            'f=~/.maozi-cloud-develop-admin-env.sh; touch "$f"',
            `grep -v '^export ${key}=' "$f" > "$f.mzi" || true; echo "export ${key}='${safeVal}'" >> "$f.mzi"; mv "$f.mzi" "$f"`,
            'z=~/.zshrc; touch "$z"',
            `grep -q 'maozi-cloud-develop-admin (live-env)' "$z" || cat >> "$z" <<'MAOZI_EOF'`,
            '',
            '# >>> maozi-cloud-develop-admin (live-env) >>>',
            '_maozi_env_sync() {',
            '  local f="$HOME/.maozi-cloud-develop-admin-env.sh"',
            '  [[ -r "$f" ]] || return',
            `  local m; m=$(md5 -q "$f" 2>/dev/null || md5sum "$f" 2>/dev/null | awk '{print $1}')`,
            '  [[ "$m" == "${_MAOZI_ENV_M:-}" ]] && return',
            '  _MAOZI_ENV_M="$m"',
            '  source "$f"',
            '}',
            '[[ " ${preexec_functions[*]:-} " == *" _maozi_env_sync "* ]] || preexec_functions+=(_maozi_env_sync)',
            '[[ " ${precmd_functions[*]:-} " == *" _maozi_env_sync "* ]] || precmd_functions+=(_maozi_env_sync)',
            '# <<< maozi-cloud-develop-admin (live-env) <<<',
            'MAOZI_EOF'
          ].join('\n'),
          15_000
        )
      } else {
        // bash 系配置文件没有提示符钩子机制，维持直接写入（新开终端生效）
        await sshExec(
          ctx.ssh,
          `grep -v '^export ${key}=' ${filePath} > ${filePath}.mzi 2>/dev/null || true; echo "export ${key}='${safeVal}'" >> ${filePath}.mzi; mv ${filePath}.mzi ${filePath}`,
          15_000
        )
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
      // 当前编排取值（与 compose 占位符一致：业务 .env 优先、其次系统环境变量）：
      // 用于服务/容器命名，随列表一并提供给前端展示（环境 / 灰度）
      const interp = await appSvcNameValues()
      const env = {
        ENVIRONMENT: interpolateTemplate('${ENVIRONMENT:-${APPLICATION_ENVIRONMENT:-dev}}', interp),
        VERSION: interpolateTemplate('${VERSION:-${APPLICATION_VERSION:-main}}', interp)
      }
      return { ok: true, data: { services, env } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { services: [] } }
    }
  })

  /**
   * 应用服务运行状态（服务名 → State）+ 健康检查状态（定义了 healthcheck 的容器才有）：
   * docker ps 一次拉全量（Names|Status，Status 含 (healthy)/(unhealthy)/(health: starting) 标记），按容器名匹配。
   * 不走 compose ps —— compose 会因自定义 log 字段校验失败
   */
  ipcMain.handle('projects:appServicesStatus', async (_e, variantArg: string) => {
    const empty = { states: {} as Record<string, string>, healths: {} as Record<string, string> }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      await checkDockerCompose()

      const ctx = getProjectCtx()
      // 容器名 → 健康状态（healthy/unhealthy/starting；无 healthcheck 或已停止为空串）
      const healthOf = new Map<string, string>()

      if (ctx.isRemote && ctx.ssh) {
        // 远程：SSH 执行 docker ps，取 名称|状态（Status 文本含健康检查标记）
        const out = await sshExec(ctx.ssh, 'docker ps --format "{{.Names}}|{{.Status}}" 2>&1', 15_000)
        for (const l of out.split(/\r?\n/)) {
          const line = l.trim()
          if (!line || line.includes('command not found') || line.includes('error')) continue
          const sep = line.indexOf('|')
          if (sep <= 0) continue
          const name = line.slice(0, sep)
          const status = line.slice(sep + 1)
          healthOf.set(name, parseHealthMark(status))
        }
      } else {
        // 本地
        const pathEnv = await getShellPath()
        const { stdout } = await exec('docker', ['ps', '--format', '{{.Names}}|{{.Status}}'], {
          timeout: 15_000,
          env: { ...process.env, PATH: pathEnv }
        })
        for (const l of stdout.split(/\r?\n/)) {
          const line = l.trim()
          if (!line) continue
          const sep = line.indexOf('|')
          if (sep <= 0) continue
          healthOf.set(line.slice(0, sep), parseHealthMark(line.slice(sep + 1)))
        }
      }

      // 变体服务名 → 容器名 → 在运行列表中则为 running；healthcheck 状态一并带出
      const states: Record<string, string> = {}
      const healths: Record<string, string> = {}
      const names = await appSvcContainerNames(b.path, variantArg as AppSvcVariant)
      for (const [svc, container] of Object.entries(names)) {
        const h = healthOf.get(container)
        states[svc] = h !== undefined ? 'running' : 'exited'
        healths[svc] = h ?? ''
      }
      return { ok: true, data: { states, healths } }
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
   * 应用服务互斥停机：docker compose -p <项目名> -f <file> down 两个 compose 文件（与全部关闭同款语义）。
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
    const project = await appSvcProjectName()
    const dir = path.join(bPath, def.dir)
    for (const fileName of appSvcFiles(def)) {
      const args = [...composeArgs, '-p', project, '-f', fileName, 'down']
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
        if (fileTag !== 'admin' && fileTag !== 'services' && fileTag !== 'nginx') throw new Error('非法的文件标识')
        if (!/^[A-Za-z0-9_.-]+$/.test(String(service))) throw new Error('非法的服务名')
        if (action !== 'start' && action !== 'stop' && action !== 'restart') throw new Error('非法的操作')
        await checkDockerCompose()
        const variant = variantArg as AppSvcVariant
        const def = APP_SVC_DEFS[variant]
        const dir = path.join(b.path, def.dir)
        const fileName =
          fileTag === 'admin' ? def.adminFile : fileTag === 'nginx' ? def.nginxFile : def.servicesFile
        if (!fs.existsSync(path.join(dir, fileName))) throw new Error(`未找到 ${def.dir}/${fileName}`)
        const send = (text: string): void => {
          if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
        }

        // 互斥：启动/重启前关闭另一变体全部服务
        if (action !== 'stop') {
          const other: AppSvcVariant = variant === 'monomer' ? 'distributeds' : 'monomer'
          await appServicesDown(event, b.path, other, `启动${def.label}服务 ${service}`, sid)
        }

        // 优雅更新：services 文件的单服务重启，前置钩子拉起该服务的热备容器承接流量，重启完事后置回收
        const hotSwap = fileTag === 'services' && action === 'restart' && hotSwapOn(variant)
        if (hotSwap) await gracefulUpdateHook(variant, event.sender, sid, 'before', service)
        // 前置钩子被中断：跳过该服务重启（此路径不走 finally，热备保持运行，下次操作时回收）
        if (hotSwap && runningProcs.get(sid)?.stopRequested) {
          send(`⏹ 已中断，跳过 ${service} 重启`)
          return { ok: true }
        }

        // compose up -d 创建并启动（docker start 要求容器已存在：首次启动或 stop 回收后会 No such container）
        const pctx = getProjectCtx()
        const composeBin = pctx.isRemote && pctx.ssh
          ? (await remoteComposeCmd(pctx.ssh)).split(' ') // 远程可能是 v1 docker-compose
          : ['docker', 'compose']
        const [composeCmd, ...composeArgs] = composeBin
        const base = [...composeArgs, '-p', await appSvcProjectName(), '-f', fileName]
        // pre_stop 钩子只在 stop/down/restart 时触发（rm --stop --force 强杀会跳过）：
        // 停止/重启改为 stop（优雅停机、执行钩子）+ rm 两步，再按需 up -d
        const cmdList: string[][] =
          action === 'start'
            ? [[...base, 'up', '-d', service]]
            : action === 'stop'
              ? [[...base, 'stop', service], [...base, 'rm', service]]
              : [[...base, 'stop', service], [...base, 'rm', service], [...base, 'up', '-d', service]]
        try {
          for (const args of cmdList) {
            send(`▶ ${[composeCmd, ...args].join(' ')}`)
            try {
              await projStream(event.sender, 'projects:scriptLog', composeCmd, args, {
              cwd: dir,
              timeoutMs: 10 * 60_000,
              sid,
              // rm 不带 --force 会交互式确认，stdin 预置 y 自动应答
              stdinData: args.includes('rm') ? 'y\n' : undefined
            })
          } catch (err) {
            const msg = (err as Error).message
            throw new Error(msg.startsWith('exit ') ? `docker 操作失败（${msg}），详见日志` : msg)
          }
          }
        } finally {
          // 优雅更新：单服务重启完成后回收该服务的热备容器
          if (hotSwap) await gracefulUpdateHook(variant, event.sender, sid, 'after', service)
          // 清理无子进程的标记条目（被中断会话保留的 stopRequested 存根）
          const entry = runningProcs.get(sid)
          if (entry && !entry.child) runningProcs.delete(sid)
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
      if (action !== 'start' && action !== 'stop' && action !== 'restart') throw new Error('非法的操作')
      await checkDockerCompose()
      const variant = variantArg as AppSvcVariant
      const def = APP_SVC_DEFS[variant]
      const dir = projPath(def.dir)
      for (const fileName of appSvcFiles(def)) {
        if (!(await projFileExists(path.join(def.dir, fileName)))) throw new Error(`未找到 ${def.dir}/${fileName}`)
      }

      const send = (text: string): void => {
        if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
      }

      // 优雅更新：全部重启前先跑前置钩子拉起热备组承接流量（尽力而为），结束后后置回收
      const hotSwap = action === 'restart' && hotSwapOn(variant)
      if (hotSwap) await gracefulUpdateHook(variant, event.sender, sid, 'before')
      try {
          // 前置钩子被中断：跳过本次全部重启，finally 仍会回收热备组
          if (hotSwap && runningProcs.get(sid)?.stopRequested) {
            send('⏹ 已中断，跳过全部重启')
            return
          }
          // 先 down：stop 与 restart 共用（restart 之后紧跟 up，等价"先 docker-compose down 再 up"全新重建）
          if (action === 'stop' || action === 'restart') {
          // 全部关闭：docker compose -p <项目名> -f <file> down（与全部启动的 up -d 对称，compose 文件逐个执行）
          const pctx = getProjectCtx()
          const composeBinParts = pctx.isRemote && pctx.ssh
            ? (await remoteComposeCmd(pctx.ssh)).split(' ') // 远程可能是 v1 docker-compose
            : ['docker', 'compose']
          const [composeCmd, ...composeArgs] = composeBinParts
          const project = await appSvcProjectName()
          for (const fileName of appSvcFiles(def)) {
            const args = [...composeArgs, '-p', project, '-f', fileName, 'down']
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

        // 再 up：start 与 restart 共用（内含互斥——先 down 掉另一变体全部服务）
        if (action === 'start' || action === 'restart') {
          const other: AppSvcVariant = variant === 'monomer' ? 'distributeds' : 'monomer'
          await appServicesDown(event, b.path, other, `启动${def.label}全部服务`, sid)

          const ctx = getProjectCtx()
          const project = await appSvcProjectName()
          for (const fileName of appSvcFiles(def)) {
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
                  await sshExec(ctx.ssh, `nohup bash -c 'echo $$ > ${pidFile}; cd ${JSON.stringify(dir)} && ${composeBin} -p ${project} -f ${fileName} up -d' > ${logFile} 2>&1 &`, 10_000)
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
                  ['compose', '-p', project, '-f', fileName, 'up', '-d'], {
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
          }
      } finally {
        // 优雅更新：主流程结束后回收热备组
        if (hotSwap) await gracefulUpdateHook(variant, event.sender, sid, 'after')
        // 清理无子进程的标记条目（被中断会话保留的 stopRequested 存根）
        const entry = runningProcs.get(sid)
        if (entry && !entry.child) runningProcs.delete(sid)
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

  /** 数据库初始化状态：.db-init.json 标记判定（maozi-cloud-develop-admin 应用目录，git 已忽略）；init_mysql_db.json 不存在或为空时隐藏按钮 */
  ipcMain.handle('projects:dbInitStatus', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const content = await readInitMysqlDb()
      if (content === null) return { ok: true, data: { count: 0, initialized: false, missing: 0 } }
      const scripts = parseInitMysqlDb(content)
      if (scripts.length === 0) return { ok: true, data: { count: 0, initialized: false, missing: 0 } }
      const initialized = isDbInitialized(b.path)
      return { ok: true, data: { count: scripts.length, initialized, missing: initialized ? 0 : scripts.length } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: { count: 0, initialized: false, missing: 0 } }
    }
  })

  /**
   * 初始化数据库：按 init_mysql_db.json 定义（JSON 数组，每项一个脚本路径）逐个导入 SQL 到 mysql。
   * mysql 未运行时先 up -d 启动并等待就绪；初始化结束后仅回收本次启动的容器，
   * 点击前已在运行的容器保持原状（不 stop/rm）。日志走 projects:scriptLog，密码全程打码
   */
  ipcMain.handle('projects:dbInit', async (event, sidArg?: string) => {
    try {
      const sid = validSid(sidArg)
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const content = await readInitMysqlDb()
      if (content === null) throw new Error(`未找到 ${INIT_MYSQL_DB_FILE}`)
      const scripts = parseInitMysqlDb(content)
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

      // 仅回收本次启动的容器；点击前已运行的保持原状。
      // 优雅两步：stop（触发 pre_stop 等生命周期钩子）→ rm（交互确认由 stdin 自动应答）
      if (!wasRunning) {
        send('▶ docker compose stop maozi-cloud-basic-mysql && docker compose rm maozi-cloud-basic-mysql（回收本次启动的容器）')
        try {
          await projStream(event.sender, 'projects:scriptLog', 'docker', [
            'compose', 'stop', 'maozi-cloud-basic-mysql'
          ], { cwd: dir, timeoutMs: 10 * 60_000, sid })
          await projStream(event.sender, 'projects:scriptLog', 'docker', [
            'compose', 'rm', 'maozi-cloud-basic-mysql'
          ], { cwd: dir, timeoutMs: 10 * 60_000, sid, stdinData: 'y\n' })
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

      // 一次性拉所有运行中容器
      const { stdout: psOut } = await exec('docker', ['ps', '--format', '{{.Names}}'], { timeout: 15_000, env })
      const runningSet = new Set(psOut.split(/\r?\n/).map((l) => l.trim()).filter(Boolean))

      // 仅查 services yml 定义的容器（不含 admin），日志写到文件而非 stdout。
      // 服务名/容器名含 ${ENVIRONMENT}/${VERSION} 占位：须按 业务.env/系统环境 插值成真实名称，
      // 否则与 docker ps 的实际容器名（如 maozi-cloud-gateway-service-dev-main）比对不上
      const servicesYml = path.join(b.path, APP_SVC_DEFS.distributeds.dir, APP_SVC_DEFS.distributeds.servicesFile)
      const interp = await appSvcNameValues()
      const rawNames = parseServiceContainerNames(fs.readFileSync(servicesYml, 'utf8'))
      const svcNames: Record<string, string> = {}
      for (const [svc, container] of Object.entries(rawNames)) {
        svcNames[interpolateTemplate(svc, interp)] = interpolateTemplate(container, interp)
      }

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
      // 上下文：basics（默认）或应用服务 '<variant>:<admin|services>'（-p 项目名 + -f 指定 compose 文件）
      const ctx = ctxArg ? resolveComposeCtx(ctxArg) : null
      const dir = ctx ? path.join(b.path, ctx.dir) : path.join(b.path, COMPOSE_DIR)
      const fileArgs = ctx ? ['-p', await appSvcProjectName(), '-f', ctx.file] : []
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
        const fileArgs = ctx ? ['-p', await appSvcProjectName(), '-f', ctx.file] : []
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

  /** 环境设置：解析项目 environment_variable.json（一级属性 = 分节），实时读取各节当前值 */
  ipcMain.handle('projects:envSettings', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const sections = parseEnvVarDefs(await readProjFile(ENV_VARS_FILE))

      // .env 分节：文件当前值填充（key 不在文件中 → 未设置）；文件缺失时整节显示未设置
      for (const sec of sections) {
        if (sec.source !== 'file') continue
        let map = new Map<string, string>()
        try {
          map = parseEnvFile(await readProjFile(sec.file ?? BUSINESS_ENV_FILE))
        } catch {
          /* 远程绑定或文件缺失：保持未设置 */
        }
        for (const g of sec.groups) {
          // 空值视为未设置（KEY= 行保留 key 定义但取值为空）
          g.items = g.items.map((it) => {
            const v = map.get(it.key) ?? ''
            return { ...it, value: v, found: v !== '', enabled: v !== '', source: v !== '' ? '.env' : '' }
          })
        }
      }

      const ctx2 = getProjectCtx()
      if (ctx2.isRemote && ctx2.ssh) {
        // 远程绑定：SSH 读取远程 shell 环境变量
        const remoteEnv = await sshExec(ctx2.ssh, 'env', 15_000)
        const envMap = new Map<string, string>()
        for (const line of remoteEnv.split('\n')) {
          const eq = line.indexOf('=')
          if (eq > 0) envMap.set(line.slice(0, eq).trim(), line.slice(eq + 1).trim())
        }
          for (const sec of sections) {
            if (sec.source !== 'env') continue
            for (const g of sec.groups) {
              // 空值视为未设置
              g.items = g.items.map((it) => {
                const v = envMap.get(it.key) ?? ''
                return { ...it, value: v, found: v !== '', enabled: v !== '', source: v !== '' ? '远程环境' : '' }
              })
            }
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
        return { ok: true, data: { sections, files: remoteFiles, defaultFileId: defaultFile.id, remote: true } }
      }
      // 本地绑定：读本地 shell 配置
      const platform = selectPlatform()
      const files = platform.listEnvFiles()
      for (const sec of sections) {
        if (sec.source === 'env') applyEnvValues(sec.groups, platform.readEnvVars(), files)
      }
      const defaultFile = files.find((f) => f.exists) ?? files[0]
      return { ok: true, data: { sections, files, defaultFileId: defaultFile.id } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /**
   * Maven 配置：mvn -v 解析安装目录（Maven home）与版本，定位 conf/settings.xml。
   * 本地走 shell PATH 执行，远程绑定在服务器上执行
   */
  ipcMain.handle('projects:mavenInfo', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const { version, home, settingsFile } = await mavenDetect(b)
      let exists: boolean
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        exists = (await sshExec(t, `test -f ${JSON.stringify(settingsFile)} && echo __YES__`, 10_000)).includes('__YES__')
      } else {
        exists = fs.existsSync(settingsFile)
      }
      return { ok: true, data: { version, home, settingsFile, exists } }
    } catch (err) {
      const msg = (err as Error).message
      return { ok: false, error: /ENOENT|not found|未找到/i.test(msg) ? '未检测到 mvn 命令，请先安装 Maven' : msg }
    }
  })

  /** 读取 Maven settings.xml（本地 fs / 远程 cat） */
  ipcMain.handle('projects:mavenConfigRead', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const { settingsFile } = await mavenDetect(b)
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        return { ok: true, data: { content: await sshReadFile(t, settingsFile) } }
      }
      return { ok: true, data: { content: fs.readFileSync(settingsFile, 'utf8') } }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 保存 Maven settings.xml：直接覆盖保存（不备份）；远程经 base64 透传避免引号转义 */
  ipcMain.handle('projects:mavenConfigSave', async (_e, contentArg: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const content = String(contentArg ?? '')
      const { settingsFile: file } = await mavenDetect(b)
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        const b64 = Buffer.from(content, 'utf8').toString('base64')
        await sshExec(
          t,
          `printf %s '${b64}' | base64 -d > ${JSON.stringify(file)}`,
          20_000
        )
      } else {
        fs.writeFileSync(file, content, 'utf8')
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 读取项目 .git/config（本地 fs / 远程 cat）；非 Git 仓库（.git/config 不存在）如实报错 */
  ipcMain.handle('projects:gitConfigRead', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const file = path.join(b.path, '.git', 'config')
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        return { ok: true, data: { content: await sshReadFile(t, file), file } }
      }
      if (!fs.existsSync(file)) throw new Error(`未找到 ${file}（项目不是 Git 仓库）`)
      return { ok: true, data: { content: fs.readFileSync(file, 'utf8'), file } }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 保存项目 .git/config：直接覆盖保存（不备份）；远程经 base64 透传 */
  ipcMain.handle('projects:gitConfigSave', async (_e, contentArg: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const content = String(contentArg ?? '')
      const file = path.join(b.path, '.git', 'config')
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        const b64 = Buffer.from(content, 'utf8').toString('base64')
        await sshExec(t, `printf %s '${b64}' | base64 -d > ${JSON.stringify(file)}`, 20_000)
      } else {
        if (!fs.existsSync(path.join(b.path, '.git'))) throw new Error('项目不是 Git 仓库，无法保存 .git/config')
        fs.writeFileSync(file, content, 'utf8')
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /**
   * Docker 配置探测：docker -v 取版本；daemon.json 按平台解析——
   * mac / Windows（Docker Desktop）在 ~/.docker/daemon.json，
   * Linux 原生在 /etc/docker/daemon.json（不存在时回退 ~/.docker/）；
   * 远程绑定固定按 Linux 语义在服务器上解析
   */
  ipcMain.handle('projects:dockerInfo', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      let version = ''
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        try {
          const out = await sshExec(t, 'docker -v 2>&1', 20_000)
          version = out.match(/Docker version\s+(\S+)/)?.[1] ?? ''
        } catch {
          version = ''
        }
      } else {
        const pathEnv = await getShellPath()
        try {
          const r = await exec('docker', ['-v'], { timeout: 20_000, env: { ...process.env, PATH: pathEnv } })
          version = (r.stdout + r.stderr).match(/Docker version\s+(\S+)/)?.[1] ?? ''
        } catch {
          version = ''
        }
      }
      const platform = b.remote
        ? 'Linux（远程）'
        : process.platform === 'darwin'
          ? 'macOS（Docker Desktop）'
          : process.platform === 'win32'
            ? 'Windows（Docker Desktop）'
            : 'Linux'
      const configFile = await dockerConfigPath(b)
      let exists: boolean
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        exists = (await sshExec(t, `test -f ${JSON.stringify(configFile)} && echo __YES__; true`, 10_000)).includes('__YES__')
      } else {
        exists = fs.existsSync(configFile)
      }
      return { ok: true, data: { version, platform, configFile, exists } }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 读取 Docker daemon.json（本地 fs / 远程 cat） */
  ipcMain.handle('projects:dockerConfigRead', async () => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const file = await dockerConfigPath(b)
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        return { ok: true, data: { content: await sshReadFile(t, file) } }
      }
      return { ok: true, data: { content: fs.readFileSync(file, 'utf8') } }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 保存 Docker daemon.json：直接覆盖保存（不备份）；远程经 base64 透传（/etc/docker 普通用户写入失败时如实报错） */
  ipcMain.handle('projects:dockerConfigSave', async (_e, contentArg: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const content = String(contentArg ?? '')
      const file = await dockerConfigPath(b)
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        const b64 = Buffer.from(content, 'utf8').toString('base64')
        await sshExec(
          t,
          `printf %s '${b64}' | base64 -d > ${JSON.stringify(file)}`,
          20_000
        )
      } else {
        fs.mkdirSync(path.dirname(file), { recursive: true })
        fs.writeFileSync(file, content, 'utf8')
      }
      return { ok: true }
    } catch (err) {
      return { ok: false, error: `保存失败（Linux 下 /etc/docker 通常需要 root 权限）：${(err as Error).message}` }
    }
  })

  /**
   * 重启 Docker：Docker Desktop 安装的用官方 CLI docker desktop restart（阻塞至引擎恢复）；
   * 服务器直装的用 systemctl restart docker（远程 root 直可，普通用户 sudo -S，expect 自动应答密码）；
   * Windows = 重启 com.docker.service 服务（需管理员权限，失败如实报错）。
   * 各阶段进度（探测/分流/命令输出/健康等待）实时推送 projects:scriptLog，可中断
   */
  ipcMain.handle('projects:dockerRestart', async (event, sidArg?: string) => {
    const sid = validSid(sidArg)
    const send = (text: string): void => {
      if (!event.sender.isDestroyed()) event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
    }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      // 全程持有会话条目（无 child 的标记条目）：纯轮询阶段（docker info 探测）也能被中断
      const keepAlive = (): void => {
        if (!runningProcs.has(sid)) runningProcs.set(sid, { stopRequested: false })
      }
      keepAlive()
      const daemonUp = async (): Promise<boolean> => {
        try {
          if (b.remote) {
            await sshExec(resolveSshTarget(b.remote.configId), 'docker info >/dev/null 2>&1', 15_000)
          } else {
            const pathEnv = await getShellPath()
            await exec('docker', ['info'], { timeout: 15_000, env: { ...process.env, PATH: pathEnv } })
          }
          return true
        } catch {
          return false
        }
      }
      // 轮询等待 daemon 状态：每 3 秒一查，每 15 秒播报一次已等待时长；中断即抛
      const waitDaemon = async (want: boolean, timeoutMs: number, what: string): Promise<boolean> => {
        const startTs = Date.now()
        const deadline = startTs + timeoutMs
        let lastLog = -15
        while (Date.now() < deadline) {
          if (runningProcs.get(sid)?.stopRequested) throw new Error('已手动中断')
          if ((await daemonUp()) === want) return true
          const elapsed = Math.floor((Date.now() - startTs) / 1000)
          if (elapsed - lastLog >= 15) {
            send(`⏳ ${what}（已等 ${elapsed}s）`)
            lastLog = elapsed
          }
          await new Promise((r) => setTimeout(r, 3000))
        }
        return false
      }
      if (b.remote) {
        const t = resolveSshTarget(b.remote.configId)
        // Docker Desktop 安装的（有 desktop CLI 插件）用官方 CLI 重启；服务器直装的走 systemctl。
        // docker/systemctl 均解析绝对路径执行：rc 配置整串替换 PATH 时按名查找会 command not found
        send('▶ 检测安装方式：docker desktop version …')
        const dockerAbs = '$(command -v docker 2>/dev/null || echo /usr/bin/docker)'
        const hasDesktop = await (async (): Promise<boolean> => {
          try {
            await sshExec(t, `"${dockerAbs}" desktop version >/dev/null 2>&1`, 20_000)
            return true
          } catch {
            return false
          }
        })()
        keepAlive()
        if (hasDesktop) {
          send('✓ 检测到 Docker Desktop 安装：执行 docker desktop restart（阻塞至引擎恢复，可能需 1-2 分钟）')
          await sshStream(event.sender, 'projects:scriptLog', t, `"${dockerAbs}" desktop restart`, {
            timeoutMs: 180_000,
            sid
          })
        } else {
          send('· 非 Docker Desktop 安装：systemctl restart docker（普通用户 sudo 自动应答密码）')
          await sshStream(
            event.sender,
            'projects:scriptLog',
            t,
            'SYSTEMCTL="$(command -v systemctl 2>/dev/null || echo /usr/bin/systemctl)"; systemctl restart docker 2>/dev/null || sudo -S "$SYSTEMCTL" restart docker',
            { timeoutMs: 180_000, sid }
          )
        }
        keepAlive()
        if (!(await waitDaemon(true, 90_000, '等待远程 Docker daemon 恢复')))
          throw new Error('docker info 90 秒内未恢复，请手动检查')
        send('✓ Docker 已重启，daemon 恢复就绪')
        return { ok: true }
      }
      const pathEnv = await getShellPath()
      const env = { ...process.env, PATH: pathEnv }
      if (process.platform === 'darwin') {
        // 优先 Docker Desktop 官方 CLI（docker desktop restart 阻塞至引擎恢复）；
        // 旧版无 desktop 插件时回退 osascript 退出 Docker Desktop 后重新拉起
        send('▶ 检测安装方式：docker desktop version …')
        let hasDesktopCli = false
        try {
          await exec('docker', ['desktop', 'version'], { timeout: 15_000, env })
          hasDesktopCli = true
        } catch {
          hasDesktopCli = false
        }
        keepAlive()
        if (hasDesktopCli) {
          send('✓ 检测到 Docker Desktop 安装：执行 docker desktop restart（阻塞至引擎恢复，可能需 1-2 分钟）')
          await streamProcess(event.sender, 'projects:scriptLog', 'docker', ['desktop', 'restart'], {
            timeoutMs: 240_000,
            sid
          })
          keepAlive()
          if (!(await waitDaemon(true, 120_000, '等待 Docker daemon 恢复')))
            throw new Error('Docker Desktop 2 分钟内未就绪，请手动检查')
          send('✓ Docker 已重启，daemon 恢复就绪')
          return { ok: true }
        }
        send('· 旧版 Docker Desktop（无 desktop 插件）：退出后重新拉起')
        try {
          await exec('osascript', ['-e', 'quit app "Docker"'], { timeout: 20_000, env })
        } catch {
          /* 未运行时 quit 报错，忽略 */
        }
        if (!(await waitDaemon(false, 60_000, '等待 Docker Desktop 退出')))
          throw new Error('Docker Desktop 60 秒内未退出，请手动检查')
        send('▶ open -a Docker 重新拉起 …')
        await exec('open', ['-a', 'Docker'], { timeout: 20_000, env })
        keepAlive()
        if (!(await waitDaemon(true, 180_000, '等待 Docker daemon 恢复')))
          throw new Error('Docker Desktop 3 分钟内未就绪，请手动检查')
        send('✓ Docker 已重启，daemon 恢复就绪')
        return { ok: true }
      }
      if (process.platform === 'linux') {
        send('▶ systemctl restart docker …')
        await exec('systemctl', ['restart', 'docker'], { timeout: 60_000, env })
        if (!(await waitDaemon(true, 90_000, '等待 Docker daemon 恢复')))
          throw new Error('docker info 90 秒内未恢复，请手动检查')
        send('✓ Docker 已重启，daemon 恢复就绪')
        return { ok: true }
      }
      send('▶ PowerShell: Restart-Service com.docker.service …')
      await exec('powershell.exe', ['-Command', 'Restart-Service com.docker.service'], { timeout: 120_000, env })
      if (!(await waitDaemon(true, 180_000, '等待 Docker daemon 恢复')))
        throw new Error('Docker 3 分钟内未就绪，请手动检查')
      send('✓ Docker 已重启，daemon 恢复就绪')
      return { ok: true }
    } catch (err) {
      return { ok: false, error: `重启 Docker 失败：${(err as Error).message}` }
    } finally {
      // 清理无子进程的标记条目（被中断会话保留的 stopRequested 存根）
      const entry = runningProcs.get(sid)
      if (entry && !entry.child) runningProcs.delete(sid)
    }
  })

  /**
   * 应用服务配置：按容器完整名称匹配 docker_variable.json 的一级 key（真实服务名或静态前缀，
   * 不做去前缀/去尾段过滤），节内条目 = 配置描述 → 环境变量 key，取值读业务 .env；
   * 未匹配到一级属性时 data 为 null
   */
  ipcMain.handle('projects:serviceConfig', async (_e, serviceArg: string, baseArg?: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      // 候选名（大小写不敏感）：仅完整名称——真实服务名（含环境/灰度后缀）与静态前缀
      const cands = new Set<string>()
      for (const raw of [serviceArg, baseArg ?? '']) {
        const s = String(raw ?? '').trim().toLowerCase()
        if (s) cands.add(s)
      }
      const sections = parseEnvVarDefs(await readProjFile(DOCKER_VARS_FILE))
      const hit = sections.find((sec) => sec.name && cands.has(sec.name.trim().toLowerCase()))
      if (!hit) return { ok: true, data: null }
      // 值取业务 .env（空值视为未设置）；文件缺失时整节显示未设置
      let map = new Map<string, string>()
      try {
        map = parseEnvFile(await readProjFile(BUSINESS_ENV_FILE))
      } catch {
        /* 缺失时全部未设置 */
      }
      for (const g of hit.groups) {
        g.items = g.items.map((it) => {
          const v = map.get(it.key) ?? ''
          return { ...it, value: v, found: v !== '', enabled: v !== '', source: v !== '' ? '.env' : '' }
        })
      }
      return { ok: true, data: { section: hit.name, groups: hit.groups, file: BUSINESS_ENV_FILE } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: null }
    }
  })

  /**
   * 应用服务「接口不停机更新」开关（单体/微服务各一个）：
   * 开启 = 拷贝变体 services yml 生成热备文件（container_name 加 -backup）；
   * 关闭 = 仅删除热备文件（不动容器、不做健康等待——操作流程内的热备回收在各自会话中完成）。
   * 状态持久化 .ui-state.json
   */
  ipcMain.handle('projects:hotSwap', async (event, variantArg: string, enable: boolean, sidArg?: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      if (variantArg !== 'monomer' && variantArg !== 'distributeds') throw new Error('非法的变体')
      const variant = variantArg as AppSvcVariant
      const sid = validSid(sidArg ?? 'app-hotswap')
      const def = APP_SVC_DEFS[variant]
      const rel = path.join(def.dir, hotSwapBackupFile(variant))
      if (enable) {
        await checkDockerCompose()
        await writeHotSwapBackupYml(variant, event.sender, sid)
        // 优雅更新开关仅微服务需要：写入 .env（compose 环境注入，服务据此走不停机下线逻辑）；
        // 单体服务不需要
        if (variant === 'distributeds') {
          await businessEnvFileSet('GRACEFUL_UPDATE', 'true')
          if (!event.sender.isDestroyed()) {
            event.sender.send('projects:scriptLog', {
              kind: 'line',
              text: '✓ 已写入 GRACEFUL_UPDATE=true 到业务 .env',
              sid
            })
          }
        }
        setHotSwap(variant, true)
        return { ok: true, data: true }
      }
      // 关闭 = 删除热备文件（微服务并移除 .env 的 GRACEFUL_UPDATE）（不动容器、不做健康等待——
      // 操作流程内的热备回收在各自会话中完成）
      if (await projFileExists(rel)) {
        await deleteProjFile(rel)
        if (!event.sender.isDestroyed()) {
          event.sender.send('projects:scriptLog', {
            kind: 'line',
            text: `✓ 已删除热备文件 ${hotSwapBackupFile(variant)}`,
            sid
          })
        }
      }
      if (variant === 'distributeds') {
        await businessEnvFileSet('GRACEFUL_UPDATE', null)
        if (!event.sender.isDestroyed()) {
          event.sender.send('projects:scriptLog', {
            kind: 'line',
            text: '✓ 已从业务 .env 移除 GRACEFUL_UPDATE',
            sid
          })
        }
      }
      setHotSwap(variant, false)
      return { ok: true, data: false }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 环境设置 .env 分节保存：原位更新 key=value 行（无则在末尾追加），本地直写 / 远程 SSH */
  ipcMain.handle('projects:envFileSave', async (_e, params: { key: string; value: string }) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const key = String(params.key ?? '').trim()
      const value = String(params.value ?? '').trim()
      if (!key) throw new Error('变量名不合法')
      // 值允许为空：写 KEY= 行（清空取值、保留 key 定义）
      await businessEnvFileSet(key, value)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /**
   * 初始化镜像列表：解析 init_base_image.json（key = 镜像名，value = 构建目录），
   * 对照 docker images 列出每个镜像是否已存在
   */
  ipcMain.handle('projects:initImages', async () => {
    const empty = { images: [] as Array<{ name: string; dir: string; exists: boolean }> }
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      await checkDockerCompose()
      let defs: Record<string, unknown>
      try {
        defs = JSON.parse(await readProjFile(INIT_BASE_IMAGE_FILE))
      } catch (err) {
        throw new Error(`解析 ${INIT_BASE_IMAGE_FILE} 失败：${(err as Error).message}`)
      }
      if (typeof defs !== 'object' || defs === null || Array.isArray(defs)) {
        throw new Error(`${INIT_BASE_IMAGE_FILE} 顶层应为 JSON 对象`)
      }
      // 本地（或远程）已有镜像集合
      const ctx = getProjectCtx()
      let existing: Set<string>
      if (ctx.isRemote && ctx.ssh) {
        const out = await sshExec(ctx.ssh, 'docker images --format {{.Repository}}:{{.Tag}} 2>&1', 15_000)
        existing = new Set(
          out.split(/\r?\n/).map((l) => l.trim()).filter((l) => l && !l.includes('command not found') && !l.includes('error'))
        )
      } else {
        const pathEnv = await getShellPath()
        const { stdout } = await exec('docker', ['images', '--format', '{{.Repository}}:{{.Tag}}'], {
          timeout: 15_000,
          env: { ...process.env, PATH: pathEnv }
        })
        existing = new Set(stdout.split(/\r?\n/).map((l) => l.trim()).filter(Boolean))
      }
      const images = Object.entries(defs).map(([name, dir]) => ({
        name,
        dir: String(dir),
        exists: existing.has(name)
      }))
      return { ok: true, data: { images } }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: empty }
    }
  })

  /**
   * 构建初始化镜像：进入定义的构建目录执行 docker buildx build -f Dockerfile -t <镜像名> .
   * 镜像名/目录必须来自 init_base_image.json 定义（防任意命令），日志走 projects:scriptLog
   */
  ipcMain.handle('projects:initImageBuild', async (event, nameArg: string, sidArg?: string) => {
    try {
      const b = getBinding()
      if (!b) throw new Error('尚未绑定项目')
      const sid = validSid(sidArg)
      const name = String(nameArg ?? '')
      let defs: Record<string, unknown> = {}
      try {
        defs = JSON.parse(await readProjFile(INIT_BASE_IMAGE_FILE))
      } catch {
        throw new Error(`读取 ${INIT_BASE_IMAGE_FILE} 失败`)
      }
      const dir = String(defs[name] ?? '')
      if (!dir) throw new Error(`镜像 ${name} 不在 ${INIT_BASE_IMAGE_FILE} 定义中`)
      await checkDockerCompose()
      const cwdRel = path.join(DOCKER_IMAGE_DIR, dir)
      const cwd = projPath(cwdRel)
      if (!(await projFileExists(path.join(cwdRel, 'Dockerfile')))) {
        throw new Error(`未找到 ${path.join(cwdRel, 'Dockerfile')}`)
      }
      const send = (text: string): void => {
        if (!event.sender.isDestroyed()) {
          event.sender.send('projects:scriptLog', { kind: 'line', text, sid })
        }
      }
      send(`▶ cd ${cwdRel} && docker buildx build -f Dockerfile -t ${name} .`)
      await projStream(event.sender, 'projects:scriptLog', 'docker', [
        'buildx', 'build', '-f', 'Dockerfile', '-t', name, '.'
      ], { cwd, timeoutMs: 30 * 60_000, sid })
      send(`✓ 镜像 ${name} 构建完成`)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
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
