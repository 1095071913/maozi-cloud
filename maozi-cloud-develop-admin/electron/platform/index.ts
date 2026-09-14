import { ipcMain } from 'electron'
import type { Platform } from './types'
import { darwinPlatform } from './darwin'
import { win32Platform } from './win32'

/**
 * 平台入口：按当前系统选择实现，并向 ipcMain 注册平台无关的接口。
 * 新增平台（如 linux）时在下方选择器中挂载对应实现即可。
 */
export function selectPlatform(): Platform {
  switch (process.platform) {
    case 'darwin':
      return darwinPlatform
    case 'win32':
      return win32Platform
    default:
      throw new Error(`暂不支持的平台: ${process.platform}`)
  }
}

export function registerPlatformHandlers(): Platform {
  const platform = selectPlatform()

  ipcMain.handle('app:info', () => ({
    platform: platform.id,
    hostsPath: platform.hostsPath()
  }))

  ipcMain.handle('env:list', () => ({
    files: platform.listEnvFiles(),
    vars: platform.readEnvVars()
  }))

  ipcMain.handle('env:save', (_e, params) => {
    try {
      platform.saveEnvVar(params)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('env:launchctl', async (_e, params) => {
    try {
      await platform.launchctlSet(params)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('hosts:list', () => {
    try {
      return { ok: true, entries: platform.readHosts() }
    } catch (err) {
      return { ok: false, error: (err as Error).message, entries: [] }
    }
  })

  ipcMain.handle('hosts:save', async (_e, entries) => {
    try {
      await platform.writeHosts(entries)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('hosts:flushDns', async () => {
    try {
      await platform.flushDns()
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 读取 hosts 原始文本（源文件编辑弹窗） */
  ipcMain.handle('hosts:readRaw', () => {
    try {
      return { ok: true, data: platform.readHostsRaw() }
    } catch (err) {
      return { ok: false, error: (err as Error).message, data: '' }
    }
  })

  /** 按原始文本写入 hosts（提权、备份、刷新 DNS 缓存） */
  ipcMain.handle('hosts:saveRaw', async (_e, text: string) => {
    try {
      await platform.writeHostsRaw(String(text ?? ''))
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  return platform
}
