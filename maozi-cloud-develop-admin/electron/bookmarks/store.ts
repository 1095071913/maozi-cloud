import {app, safeStorage} from 'electron'
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import type {Bookmark} from './types'

/**
 * 书签持久化：userData/bookmarks.json
 * 登录密码使用系统钥匙串加密（macOS Keychain / Windows DPAPI）后落盘，
 * 加密不可用时以 base64 兜底并在字段前标记
 */

interface StoredBookmark extends Omit<Bookmark, 'password'> {
  /** 加密后的 base64；仅当钥匙串不可用时为弱标记 base64 */
  passwordEnc: string
  weakEnc?: boolean
}

const WEAK_PREFIX = 'weak:'

function storeFile(): string {
  return path.join(app.getPath('userData'), 'bookmarks.json')
}

function encrypt(plain: string): StoredBookmark['passwordEnc'] & { weak?: boolean } {
  if (!plain) return ''
  if (safeStorage.isEncryptionAvailable()) {
    return safeStorage.encryptString(plain).toString('base64')
  }
  return `${WEAK_PREFIX}${Buffer.from(plain, 'utf8').toString('base64')}`
}

function decrypt(enc: string): string {
  if (!enc) return ''
  try {
    if (enc.startsWith(WEAK_PREFIX)) {
      return Buffer.from(enc.slice(WEAK_PREFIX.length), 'base64').toString('utf8')
    }
    return safeStorage.decryptString(Buffer.from(enc, 'base64'))
  } catch {
    return ''
  }
}

function readStore(): StoredBookmark[] {
  try {
    const raw = JSON.parse(fs.readFileSync(storeFile(), 'utf8'))
    return Array.isArray(raw?.bookmarks) ? raw.bookmarks : []
  } catch {
    return []
  }
}

function writeStore(list: StoredBookmark[]): void {
  fs.mkdirSync(path.dirname(storeFile()), { recursive: true })
  fs.writeFileSync(storeFile(), JSON.stringify({ version: 1, bookmarks: list }, null, 2))
}

function toDomain(b: StoredBookmark): Bookmark {
  const { passwordEnc, weakEnc, ...rest } = b
  return { ...rest, password: decrypt(passwordEnc) }
}

export function listBookmarks(): Bookmark[] {
  return readStore()
    .map(toDomain)
    .sort((a, b) => (a.sortOrder ?? a.createdAt) - (b.sortOrder ?? b.createdAt))
}

export function upsertBookmark(input: Partial<Bookmark> & { id?: string }): Bookmark {
  const list = readStore()
  const now = Date.now()
  let bookmark: Bookmark

  if (input.id) {
    const index = list.findIndex((b) => b.id === input.id)
    if (index < 0) throw new Error('书签不存在')
    const merged: Bookmark = {
      ...(toDomain(list[index]) as Bookmark),
      ...input,
      id: input.id,
      createdAt: list[index].createdAt
    }
    bookmark = merged
    list[index] = {
      ...merged,
      password: undefined,
      passwordEnc: encrypt(merged.password ?? '')
    } as unknown as StoredBookmark
  } else {
    if (!input.url) throw new Error('缺少地址')
    const existing = listBookmarks()
    const maxOrder = existing.reduce(
      (max, b) => Math.max(max, b.sortOrder ?? b.createdAt),
      0
    )
    bookmark = {
      id: crypto.randomUUID(),
      url: input.url,
      title: input.title ?? input.url,
      description: input.description ?? '',
      icon: input.icon,
      username: input.username ?? '',
      password: input.password ?? '',
      category1: input.category1 || '默认归宿',
      category2: input.category2 || '默认环境',
      category3: input.category3 || '默认类型',
      sortOrder: maxOrder + 1,
      createdAt: now
    }
    list.push({
      ...bookmark,
      password: undefined,
      passwordEnc: encrypt(bookmark.password)
    } as unknown as StoredBookmark)
  }
  writeStore(list)
  return bookmark
}

/**
 * 按传入的 id 顺序重排（拖拽排序）。
 * 全量列表中属于这些 id 的槽位按新顺序填充并重写 sortOrder，
 * 其余书签相对位置不变
 */
export function reorderBookmarks(ids: string[]): void {
  const list = readStore()
  const idSet = new Set(ids)
  const slots: number[] = []
  list.forEach((b, i) => {
    if (idSet.has(b.id)) slots.push(i)
  })
  const byId = new Map(list.map((b) => [b.id, b]))
  ids.forEach((id, k) => {
    const slot = slots[k]
    const item = byId.get(id)
    if (slot === undefined || !item) return
    list[slot] = { ...item, sortOrder: slot }
  })
  writeStore(list)
}

export function removeBookmark(id: string): void {
  const next = readStore().filter((b) => b.id !== id)
  writeStore(next)
}
