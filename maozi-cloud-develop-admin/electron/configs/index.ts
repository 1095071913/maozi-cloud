import {ipcMain} from 'electron'
import {execFile, spawn} from 'node:child_process'
import {promisify} from 'node:util'
import {listConfigs, removeConfig, upsertConfig} from './store'
import {getShellPath} from '../system/sysinfo'
import type {ConfigEntry, ConfigType, ToolAvailability} from './types'

const exec = promisify(execFile)

/**
 * 密钥管理 IPC：
 * - CLI 工具探测（Git/Docker/Helm/Linux 分别探测 git/docker/helm/ssh），缺失时对应类型密钥在前端禁用编辑
 * - Docker / Helm：一键登录（docker login / helm registry login，密码走 stdin）
 */

/** 各密钥类型对应探测的 CLI 可执行文件名 */
const TOOL_BIN: Record<Exclude<ConfigType, ''>, string> = {
  Git: 'git',
  Docker: 'docker',
  Helm: 'helm',
  Linux: 'ssh'
}

/** CLI 探测结果缓存，有效期 TOOL_CACHE_MS */
const toolCache: Partial<Record<Exclude<ConfigType, ''>, { ok: boolean; at: number }>> = {}
const TOOL_CACHE_MS = 60_000

/** 用登录 shell 的 PATH 执行 which，判断该类型对应 CLI 是否已安装（结果缓存） */
async function toolExists(type: Exclude<ConfigType, ''>): Promise<boolean> {
  const cached = toolCache[type]
  if (cached && Date.now() - cached.at < TOOL_CACHE_MS) return cached.ok
  let ok = false
  try {
    const pathEnv = await getShellPath()
    await exec('which', [TOOL_BIN[type]], { timeout: 8000, env: { ...process.env, PATH: pathEnv } })
    ok = true
  } catch {
    ok = false
  }
  toolCache[type] = { ok, at: Date.now() }
  return ok
}

/** 并行探测全部 CLI 工具的可用性 */
async function checkTools(): Promise<ToolAvailability> {
  const [git, docker, helm, linux] = await Promise.all([
    toolExists('Git'),
    toolExists('Docker'),
    toolExists('Helm'),
    toolExists('Linux')
  ])
  return { git, docker, helm, linux }
}

/** 执行命令并向 stdin 写入密码（避免出现在进程参数中） */
function execWithStdin(cmd: string, args: string[], input: string, timeoutMs = 30_000): Promise<string> {
  return new Promise((resolve, reject) => {
    void (async () => {
      const pathEnv = await getShellPath()
      const child = spawn(cmd, args, { env: { ...process.env, PATH: pathEnv } })
      let out = ''
      let err = ''
      const timer = setTimeout(() => child.kill(), timeoutMs)
      child.stdout.on('data', (d) => (out += d))
      child.stderr.on('data', (d) => (err += d))
      child.on('error', (e) => {
        clearTimeout(timer)
        reject(new Error(`无法执行 ${cmd}: ${e.message}`))
      })
      child.on('close', (code) => {
        clearTimeout(timer)
        const text = (out || err).trim()
        if (code === 0) resolve(text || '执行成功')
        else reject(new Error(text || `执行失败，退出码 ${code}`))
      })
      child.stdin.write(input)
      child.stdin.end()
    })()
  })
}

/** 按类型执行 docker login / helm registry login，返回 CLI 输出 */
async function doLogin(entry: ConfigEntry): Promise<string> {
  if (!entry.username) throw new Error('请先填写账号')
  if (!entry.password) throw new Error('请先填写密码')
  if (entry.type === 'Docker') {
    const args = ['login']
    if (entry.address.trim()) args.push(entry.address.trim())
    args.push('--username', entry.username, '--password-stdin')
    const out = await execWithStdin('docker', args, entry.password)
    return `docker login 成功：${out}`
  }
  if (entry.type === 'Helm') {
    if (!entry.address.trim()) throw new Error('Helm 仓库登录需要先填写地址')
    const out = await execWithStdin(
      'helm',
      ['registry', 'login', entry.address.trim(), '--username', entry.username, '--password-stdin'],
      entry.password
    )
    return `helm registry login 成功：${out}`
  }
  throw new Error('该密钥类型不支持一键登录')
}

/** 注册密钥管理 IPC 处理器 */
export function registerConfigHandlers(): void {
  /** 列出密钥，并附带本机 CLI 可用性 */
  ipcMain.handle('configs:list', async () => {
    try {
      return { ok: true, data: { configs: listConfigs(), tools: await checkTools() } }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('configs:save', async (_e, input: Partial<ConfigEntry>) => {
    try {
      // 已有密钥按原类型、新增按传入类型校验：对应 CLI 未安装时不允许修改（与前端禁用一致，后端兜底）
      const resultType = input.id
        ? (listConfigs().find((c) => c.id === input.id)?.type ?? input.type)
        : input.type
      // Linux 类型纯凭据存储，不关联 CLI 工具，跳过检测；其余类型需对应 CLI 可用
      if (resultType && resultType !== '' && resultType !== 'Linux' && !(await toolExists(resultType))) {
        throw new Error(`本机未安装 ${TOOL_BIN[resultType] ?? resultType}，不允许修改`)
      }
      if (input.authType !== undefined && input.authType !== 'password' && input.authType !== 'key') {
        throw new Error('密钥类型不合法')
      }
      return { ok: true, data: upsertConfig(input) }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('configs:remove', (_e, id: string) => {
    try {
      removeConfig(id)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 一键登录（Docker / Helm） */
  ipcMain.handle('configs:login', async (_e, id: string) => {
    try {
      const entry = listConfigs().find((c) => c.id === id)
      if (!entry) throw new Error('密钥不存在')
      if (entry.type !== 'Docker' && entry.type !== 'Helm') throw new Error('仅 Docker / Helm 类型密钥支持一键登录')
      if (!(await toolExists(entry.type))) throw new Error(`本机未安装 ${TOOL_BIN[entry.type] ?? entry.type}，无法执行`)
      return { ok: true, data: await doLogin(entry) }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })
}
