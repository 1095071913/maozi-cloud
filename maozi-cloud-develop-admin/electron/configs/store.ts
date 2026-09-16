import {app, safeStorage} from 'electron'
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import type {ConfigEntry} from './types'

/**
 * 密钥持久化：userData/configs.json（仓库之外，新增数据不会进 git）
 * 密码使用系统钥匙串加密（macOS Keychain / Windows DPAPI）后落盘，
 * 加密不可用时以 base64 兜底并在字段前标记；与书签存储同款方案
 */

interface StoredConfig extends Omit<ConfigEntry, 'password'> {
  passwordEnc: string
  weakEnc?: boolean
}

const WEAK_PREFIX = 'weak:'

/** 默认密钥：首次启动播种，缺失时自动补齐，不可删除 */
const DEFAULT_CONFIGS: Array<Pick<ConfigEntry, 'id' | 'name' | 'type'> & Partial<ConfigEntry>> = [
  { id: 'default-docker', name: 'Docker', type: 'Docker' },
  { id: 'default-helm', name: 'Helm', type: 'Helm' }
]

function storeFile(): string {
  return path.join(app.getPath('userData'), 'configs.json')
}

function encrypt(plain: string): string {
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

function readStore(): StoredConfig[] {
  try {
    const raw = JSON.parse(fs.readFileSync(storeFile(), 'utf8'))
    return Array.isArray(raw?.configs) ? raw.configs : []
  } catch {
    return []
  }
}

function writeStore(list: StoredConfig[]): void {
  fs.mkdirSync(path.dirname(storeFile()), { recursive: true })
  fs.writeFileSync(storeFile(), JSON.stringify({ version: 1, configs: list }, null, 2))
}

function toDomain(c: StoredConfig): ConfigEntry {
  const { passwordEnc, weakEnc, ...rest } = c
  return { ...rest, password: decrypt(passwordEnc) }
}

/** 历史版本默认名自动归一到当前默认名（仅当名称未被用户修改过时） */
const LEGACY_DEFAULT_NAMES: Record<string, string[]> = {
  'default-docker': ['默认Docker镜像仓库配置', '默认Docker镜像仓库密钥'],
  'default-helm': ['默认Helm仓库配置', '默认Helm仓库密钥']
}

export function listConfigs(): ConfigEntry[] {
  const list = readStore()
  let seeded = false

  // 存量数据补密钥类型（缺省按账密）
  if (list.some((c) => !c.authType)) {
    for (const c of list) {
      if (!c.authType) c.authType = 'password'
    }
    seeded = true
  }

  for (const item of list) {
    if (item.isDefault && LEGACY_DEFAULT_NAMES[item.id]?.includes(item.name)) {
      const def = DEFAULT_CONFIGS.find((d) => d.id === item.id)
      if (def && def.name !== item.name) {
        item.name = def.name
        seeded = true
      }
    }
  }
  // 清除已废弃的默认密钥（isDefault=true 且 id 不在当前 DEFAULT_CONFIGS 定义中）
  const defaultIds = new Set(DEFAULT_CONFIGS.map((d) => d.id))
  const before = list.length
  const filtered = list.filter((c) => !c.isDefault || defaultIds.has(c.id))
  if (filtered.length !== before) {
    list.length = 0
    list.push(...filtered)
    seeded = true
  }

  for (const def of DEFAULT_CONFIGS) {
    if (!list.some((c) => c.id === def.id)) {
      const entry: ConfigEntry = {
        id: def.id,
        name: def.name,
        type: def.type,
        authType: 'password',
        category1: '我的',
        category2: '默认环境',
        address: '',
        username: '',
        password: '',
        isDefault: true,
        sortOrder: list.length + 1,
        createdAt: Date.now()
      }
      list.push({ ...entry, password: undefined, passwordEnc: '' } as unknown as StoredConfig)
      seeded = true
    }
  }
  if (seeded) writeStore(list)
  return list.map(toDomain).sort((a, b) => (a.sortOrder ?? a.createdAt) - (b.sortOrder ?? b.createdAt))
}

export function upsertConfig(input: Partial<ConfigEntry> & { id?: string }): ConfigEntry {
  const list = readStore()
  const now = Date.now()

  if (input.id) {
    const index = list.findIndex((c) => c.id === input.id)
    if (index < 0) throw new Error('密钥不存在')
    const merged: ConfigEntry = {
      ...(toDomain(list[index]) as ConfigEntry),
      ...input,
      id: input.id,
      // 默认标记只由播种逻辑决定，不允许经保存接口篡改
      isDefault: list[index].isDefault,
      createdAt: list[index].createdAt
    }
    list[index] = {
      ...merged,
      password: undefined,
      passwordEnc: encrypt(merged.password ?? '')
    } as unknown as StoredConfig
    writeStore(list)
    return merged
  }

  if (!input.name) throw new Error('缺少名称')
  const entry: ConfigEntry = {
    id: crypto.randomUUID(),
    name: input.name,
    type: input.type ?? '',
    authType: input.authType ?? 'password',
    category1: input.category1 || '我的',
    category2: input.category2 || '默认环境',
    address: input.address ?? '',
    username: input.username ?? '',
    password: input.password ?? '',
    isDefault: false,
    sortOrder: list.length + 1,
    createdAt: now
  }
  list.push({ ...entry, password: undefined, passwordEnc: encrypt(entry.password) } as unknown as StoredConfig)
  writeStore(list)
  return entry
}

export function removeConfig(id: string): void {
  const list = readStore()
  const target = list.find((c) => c.id === id)
  if (!target) return
  if (target.isDefault) throw new Error('默认密钥不可删除')
  writeStore(list.filter((c) => c.id !== id))
}
