import {clipboard, ipcMain, shell} from 'electron'
import {listBookmarks, removeBookmark, reorderBookmarks, upsertBookmark} from './store'
import {fetchPageMeta} from './meta'
import type {Bookmark} from './types'

/** 注册书签 IPC 处理器；通用工具（剪贴板复制 app:copy / 外链打开 app:openUrl）也在此注册 */
export function registerBookmarkHandlers(): void {
  ipcMain.handle('bookmarks:list', () => {
    try {
      return { ok: true, data: listBookmarks() satisfies Bookmark[] }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 新增或更新书签（带 id 视为更新） */
  ipcMain.handle('bookmarks:save', (_e, input: Partial<Bookmark>) => {
    try {
      return { ok: true, data: upsertBookmark(input) }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('bookmarks:remove', (_e, id: string) => {
    try {
      removeBookmark(id)
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 拖拽排序：按传入 id 顺序重排，只改动这些书签所占的位置 */
  ipcMain.handle('bookmarks:reorder', (_e, ids: string[]) => {
    try {
      reorderBookmarks(Array.isArray(ids) ? ids : [])
      return { ok: true }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  /** 抓取网页标题 / 描述 / 图标（异步，不阻塞界面输入） */
  ipcMain.handle('bookmarks:fetchMeta', async (_e, url: string) => {
    try {
      return { ok: true, data: await fetchPageMeta(url) }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

  ipcMain.handle('app:copy', (_e, text: string) => {
    clipboard.writeText(text ?? '')
    return { ok: true }
  })

  /** 用系统默认浏览器打开链接，仅放行 http(s) */
  ipcMain.handle('app:openUrl', (_e, url: string) => {
    if (/^https?:\/\//i.test(url)) void shell.openExternal(url)
    return { ok: true }
  })
}
