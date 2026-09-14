import { execFile } from 'node:child_process'
import { tmpdir } from 'node:os'
import fs from 'node:fs'
import path from 'node:path'
import { promisify } from 'node:util'
import type {
  EnvFile,
  EnvSaveParams,
  EnvVarEntry,
  HostsEntry,
  LaunchctlParams,
  Platform
} from './types'
import { parseHosts, renderHosts } from './shared'

const exec = promisify(execFile)

function hostsPath(): string {
  const systemRoot = process.env.SystemRoot || 'C:\\Windows'
  return path.join(systemRoot, 'drivers', 'etc', 'hosts')
}

/**
 * Windows 平台实现（为后续扩展预留）
 *
 * hosts 读写逻辑已完整实现；环境变量管理推荐方案（待实现）：
 *   - 用户级：reg add "HKCU\Environment" /v KEY /t REG_SZ /d VALUE /f
 *   - 系统级：reg add "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" ...
 *   - 修改后广播 WM_SETTINGCHANGE 让资源管理器生效（可用 powershell SendMessageTimeout）
 */
export const win32Platform: Platform = {
  id: 'win32',

  hostsPath,

  listEnvFiles(): EnvFile[] {
    return [
      {
        id: 'user',
        name: '用户环境变量（注册表 HKCU\\Environment）',
        path: 'HKCU\\Environment',
        exists: true
      }
    ]
  },

  readEnvVars(): EnvVarEntry[] {
    throw new Error('Windows 环境变量管理即将支持，当前请使用 hosts 功能')
  },

  saveEnvVar(_params: EnvSaveParams): void {
    throw new Error('Windows 环境变量管理即将支持，当前请使用 hosts 功能')
  },

  readHosts(): HostsEntry[] {
    return parseHosts(fs.readFileSync(hostsPath(), 'utf8'))
  },

  readHostsRaw(): string {
    return fs.readFileSync(hostsPath(), 'utf8')
  },

  async writeHosts(entries: HostsEntry[]): Promise<void> {
    await writeHostsText(renderHosts(entries))
  },

  async writeHostsRaw(text: string): Promise<void> {
    await writeHostsText(text)
  },



  async flushDns(): Promise<void> {
    await exec('ipconfig', ['/flushdns'])
  },

  async launchctlSet(_params: LaunchctlParams): Promise<void> {
    throw new Error('该功能仅适用于 macOS')
  }
}

/** 按原文写入 hosts：临时文件 + UAC 提权覆盖与备份 + 刷新 DNS 缓存 */
async function writeHostsText(text: string): Promise<void> {
  const tmp = path.join(tmpdir(), `maozi-hosts-${Date.now()}`)
  fs.writeFileSync(tmp, text, { mode: 0o644 })
  try {
    const hosts = hostsPath()
    const psArgs = [
      'Start-Process',
      'cmd.exe',
      '-Verb RunAs',
      '-Wait',
      '-WindowStyle Hidden',
      `-ArgumentList '/c copy /y "${hosts}" "${hosts}.maozi.bak" && copy /y "${tmp.replace(/\\/g, '\\\\')}" "${hosts}" && ipconfig /flushdns'`
    ]
    await exec('powershell.exe', ['-NoProfile', '-Command', psArgs.join(' ')])
  } finally {
    fs.rmSync(tmp, { force: true })
  }
}
