import { execFile, spawn } from 'node:child_process'
import { promisify } from 'node:util'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { listConfigs } from '../configs/store'
import type { ConfigEntry } from '../configs/types'

const exec = promisify(execFile)

export interface SshTarget {
  user: string
  host: string
  port: number
  authType: 'password' | 'key'
  password: string
}

/** 从密钥管理解析 Linux 凭据为 SSH 目标 */
export function resolveSshTarget(configId: string): SshTarget {
  const entry = listConfigs().find((c) => c.id === configId)
  if (!entry) throw new Error('密钥不存在')
  if (entry.type !== 'Linux') throw new Error('仅支持 Linux 类型密钥')
  if (!entry.address?.trim()) throw new Error('该密钥未填写服务器地址')

  // address 格式：host 或 user@host 或 host:port 或 user@host:port
  let user = entry.username?.trim() || 'root'
  let host = entry.address.trim()
  let port = 22
  const atIdx = host.indexOf('@')
  if (atIdx > 0) {
    user = host.slice(0, atIdx)
    host = host.slice(atIdx + 1)
  }
  const colonIdx = host.lastIndexOf(':')
  if (colonIdx > 0) {
    const p = parseInt(host.slice(colonIdx + 1), 10)
    if (p > 0 && p < 65536) {
      port = p
      host = host.slice(0, colonIdx)
    }
  }
  if (!entry.password) throw new Error('该密钥未填写凭据（密码或私钥）')
  return {
    user,
    host,
    port,
    authType: (entry.authType ?? 'password') as 'password' | 'key',
    password: entry.password
  }
}

/** 写临时私钥文件（chmod 600），返回路径 */
function writeTempKey(keyContent: string): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'sshkey-'))
  const keyPath = path.join(dir, 'id_rsa')
  fs.writeFileSync(keyPath, keyContent + (keyContent.endsWith('\n') ? '' : '\n'), { mode: 0o600 })
  return keyPath
}

/** 清理临时私钥 */
function cleanupTempKey(keyPath: string): void {
  try {
    fs.rmSync(path.dirname(keyPath), { recursive: true, force: true })
  } catch {
    /* 忽略 */
  }
}

/** 构建 SSH 基础参数 */
function sshBaseArgs(target: SshTarget, keyPath?: string): string[] {
  const args = [
    '-o', 'StrictHostKeyChecking=no',
    '-o', 'ConnectTimeout=10',
    '-o', 'BatchMode=no',
    '-p', String(target.port)
  ]
  if (keyPath) args.push('-i', keyPath)
  args.push(`${target.user}@${target.host}`)
  return args
}

/**
 * SSH 执行远程命令（一次性）：
 * - key 认证：ssh -i <临时私钥> user@host command
 * - password 认证：expect 脚本包装（macOS 自带 expect）
 */
export async function sshExec(target: SshTarget, command: string, timeoutMs = 30_000): Promise<string> {
  if (target.authType === 'key') {
    const keyPath = writeTempKey(target.password)
    try {
      const { stdout } = await exec('ssh', [...sshBaseArgs(target, keyPath), command], {
        timeout: timeoutMs,
        env: process.env
      })
      return stdout
    } finally {
      cleanupTempKey(keyPath)
    }
  }

  // password 认证：expect 包装（密码走环境变量，不出现在命令行）
  const expectScript = [
    'set timeout 30',
    `spawn ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -p ${target.port} ${target.user}@${target.host} ${JSON.stringify(command)}`,
    'expect {',
    '  "password:" { send "$env(SSH_PASS)\r"; exp_continue }',
    '  "Password:" { send "$env(SSH_PASS)\r"; exp_continue }',
    '  "yes/no" { send "yes\r"; exp_continue }',
    '  eof',
    '}',
    'expect eof',
    'catch wait result',
    'exit [lindex $result 3]'
  ].join('\n')

  const { stdout } = await exec('expect', ['-c', expectScript], {
    timeout: timeoutMs + 10_000,
    env: { ...process.env, SSH_PASS: target.password }
  })
  // expect 的 spawn 输出包含命令回显，去掉第一行和最后一行
  const lines = stdout.split('\n')
  // 去掉 spawn 行和密码提示行
  const cleanLines = lines.filter(
    (l) => !l.startsWith('spawn ') && !l.includes('password:') && !l.includes('Password:')
  )
  return cleanLines.join('\n')
}

/** SSH 流式执行（用于 git clone 等长命令），输出推送到 channel */
export async function sshStream(
  sender: Electron.WebContents,
  channel: string,
  target: SshTarget,
  command: string,
  opts: { cwd?: string; timeoutMs: number; sid?: string }
): Promise<void> {
  const keyPath = target.authType === 'key' ? writeTempKey(target.password) : undefined
  const send = (kind: 'line' | 'update', text: string): void => {
    const clean = text.replace(/\x1b\[[0-9;]*[A-Za-z]/g, '').replace(/\s+$/, '')
    if (clean && !sender.isDestroyed()) sender.send(channel, { kind, text: clean, sid: opts.sid })
  }

  try {
    if (target.authType === 'key') {
      // key 认证：直接 spawn ssh
      const args = [...sshBaseArgs(target, keyPath), command]
      await streamSshProcess(sender, channel, 'ssh', args, opts)
    } else {
      // password 认证：expect 脚本
      const expectScript = [
        'set timeout -1',
        `spawn ssh -o StrictHostKeyChecking=no -p ${target.port} ${target.user}@${target.host} ${JSON.stringify(command)}`,
        'expect {',
        '  "password:" { send "$env(SSH_PASS)\r"; exp_continue }',
        '  eof',
        '}'
      ].join('\n')
      await streamSshProcess(
        sender,
        channel,
        'expect',
        ['-c', expectScript],
        { ...opts, extraEnv: { SSH_PASS: target.password } }
      )
    }
  } finally {
    if (keyPath) cleanupTempKey(keyPath)
  }
}

/** 内部：流式执行子进程 */
async function streamSshProcess(
  sender: Electron.WebContents,
  channel: string,
  cmd: string,
  args: string[],
  opts: { timeoutMs: number; sid?: string; extraEnv?: Record<string, string> }
): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    const child = spawn(cmd, args, {
      detached: true,
      env: { ...process.env, ...(opts.extraEnv ?? {}) }
    })

    const send = (kind: 'line' | 'update', text: string): void => {
      const clean = text
        .replace(/https:\/\/[^@\s]+@/g, 'https://***@')
        .replace(/\x1b\[[0-9;]*[A-Za-z]/g, '')
        .replace(/\s+$/, '')
      if (clean && !sender.isDestroyed()) sender.send(channel, { kind, text: clean, sid: opts.sid })
    }

    let carry = ''
    const feed = (chunk: Buffer): void => {
      carry += chunk.toString('utf8')
      let nl: number
      while ((nl = carry.indexOf('\n')) >= 0) {
        const seg = carry.slice(0, nl)
        carry = carry.slice(nl + 1)
        const parts = seg.split('\r')
        send('line', parts[parts.length - 1])
      }
      const cr = carry.lastIndexOf('\r')
      if (cr >= 0) {
        send('update', carry.slice(cr + 1))
        carry = carry.slice(cr + 1)
      }
    }
    child.stdout.on('data', feed)
    child.stderr.on('data', feed)

    const kill = (sig: NodeJS.Signals): void => {
      try {
        if (child.pid) process.kill(-child.pid, sig)
      } catch {
        child.kill(sig)
      }
    }
    const timer = setTimeout(() => kill('SIGKILL'), opts.timeoutMs)
    child.on('error', (e) => {
      clearTimeout(timer)
      reject(new Error(`无法执行 ${cmd}: ${e.message}`))
    })
    child.on('close', (code) => {
      clearTimeout(timer)
      if (carry.trim()) send('line', carry)
      if (code === 0) resolve()
      else reject(new Error(`exit ${code}`))
    })
  })
}

/** 列出远程目录（仅目录，不含隐藏） */
export async function sshListDir(target: SshTarget, dirPath: string): Promise<Array<{ name: string; isDir: boolean }>> {
  const cmd = `ls -1ap "${dirPath}" 2>/dev/null || echo "__ERROR__"`
  const out = await sshExec(target, cmd, 15_000)
  if (out.includes('__ERROR__')) throw new Error(`无法访问目录 ${dirPath}`)
  return out
    .split('\n')
    .filter((l) => l.trim() && !l.startsWith('spawn'))
    .map((l) => ({
      name: l.replace(/\/$/, ''),
      isDir: l.endsWith('/')
    }))
    .filter((e) => e.isDir && !e.name.startsWith('.'))
}

/** 读取远程 CONFIG 文件 */
export async function sshReadFile(target: SshTarget, filePath: string): Promise<string> {
  const cmd = `cat "${filePath}" 2>/dev/null || echo "__NOT_FOUND__"`
  const out = await sshExec(target, cmd, 15_000)
  if (out.includes('__NOT_FOUND__')) throw new Error(`远程文件不存在：${filePath}`)
  return out
}

/** 检查远程是否安装 git */
export async function sshCheckGit(target: SshTarget): Promise<boolean> {
  try {
    const out = await sshExec(target, 'which git 2>/dev/null', 10_000)
    return out.trim() !== '' && !out.includes('not found')
  } catch {
    return false
  }
}

/** 获取 Linux 密钥列表 */
export function listLinuxConfigs(): ConfigEntry[] {
  return listConfigs().filter((c) => c.type === 'Linux')
}
