import {clipboard, ipcMain, shell} from 'electron'
import {listBookmarks, removeBookmark, reorderBookmarks, upsertBookmark} from './store'
import {fetchPageMeta} from './meta'
import type {Bookmark} from './types'

export function registerBookmarkHandlers(): void {
  ipcMain.handle('bookmarks:list', () => {
    try {
      return { ok: true, data: listBookmarks() satisfies Bookmark[] }
    } catch (err) {
      return { ok: false, error: (err as Error).message }
    }
  })

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

  /** 拖拽排序：按 id 顺序重写全局排序 */
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

  ipcMain.handle('app:openUrl', (_e, url: string) => {
    if (/^https?:\/\//i.test(url)) void shell.openExternal(url)
    return { ok: true }
  })
}
