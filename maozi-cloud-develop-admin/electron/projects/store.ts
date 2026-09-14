import {app} from 'electron'
import fs from 'node:fs'
import path from 'node:path'
import type {ProjectBinding} from './types'

/**
 * 项目绑定持久化：userData/projects.json（仓库之外）
 */

function stateFile(): string {
  return path.join(app.getPath('userData'), 'projects.json')
}

export function getBinding(): ProjectBinding | null {
  try {
    const raw = JSON.parse(fs.readFileSync(stateFile(), 'utf8'))
    return raw?.project?.name ? raw.project : null
  } catch {
    return null
  }
}

export function saveBinding(b: ProjectBinding): void {
  fs.mkdirSync(path.dirname(stateFile()), { recursive: true })
  fs.writeFileSync(stateFile(), JSON.stringify({ version: 1, project: b }, null, 2))
}

export function clearBinding(): void {
  try {
    fs.unlinkSync(stateFile())
  } catch {
    /* 未绑定过时忽略 */
  }
}

/**
 * 解析目录下 CONFIG 文件（key=value 逐行，# 注释）
 * name 为项目名称、version 为项目版本号
 */
export function parseConfigFile(dir: string): { name: string; version: string } {
  const file = path.join(dir, 'CONFIG')
  if (!fs.existsSync(file)) throw new Error('所选目录下未找到 CONFIG 文件')
  const map = new Map<string, string>()
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const t = line.trim()
    if (!t || t.startsWith('#')) continue
    const eq = t.indexOf('=')
    if (eq < 0) continue
    map.set(t.slice(0, eq).trim(), t.slice(eq + 1).trim())
  }
  const name = map.get('name')
  if (!name) throw new Error('CONFIG 文件中缺少 name（项目名称）')
  return { name, version: map.get('version') ?? '' }
}

/**
 * 数据库初始化标记：应用仓库根目录 .db-init.json（项目路径 → 完成时间，JSON）。
 * 与 bookmarks.json 等本地运行时数据同目录，已在 .gitignore，不会提交
 */
function dbInitMarkFile(): string {
  return path.join(app.getAppPath(), '.db-init.json')
}

export function isDbInitialized(projectPath: string): boolean {
  try {
    const map = JSON.parse(fs.readFileSync(dbInitMarkFile(), 'utf8'))
    return typeof map[projectPath] === 'string'
  } catch {
    return false
  }
}

export function markDbInitialized(projectPath: string): void {
  let map: Record<string, string> = {}
  try {
    map = JSON.parse(fs.readFileSync(dbInitMarkFile(), 'utf8'))
  } catch {
    /* 首次创建 */
  }
  map[projectPath] = new Date().toISOString()
  fs.writeFileSync(dbInitMarkFile(), JSON.stringify(map, null, 2), 'utf8')
}
