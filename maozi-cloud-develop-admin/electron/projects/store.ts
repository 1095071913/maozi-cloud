import {app} from 'electron'
import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import type {ProjectBinding, ProjectEntry} from './types'

/**
 * 项目列表持久化：userData/projects.json（仓库之外）
 * v2 格式：多项目列表 + 当前激活项目；v1（单绑定）自动迁移为一条项目记录
 */

/** 项目名称约束：字母数字与 . _ -（不允许中文等非 ASCII） */
const PROJECT_NAME_RE = /^[A-Za-z0-9][A-Za-z0-9._-]*$/
const PROJECT_NAME_CJK_RE = /[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]/

interface ProjectStore {
  version: 2
  projects: ProjectEntry[]
  activeId: string | null
}

function stateFile(): string {
  return path.join(app.getPath('userData'), 'projects.json')
}

function newProjectId(): string {
  return crypto.randomUUID()
}

function loadStore(): ProjectStore {
  const empty: ProjectStore = {version: 2, projects: [], activeId: null}
  try {
    const raw = JSON.parse(fs.readFileSync(stateFile(), 'utf8'))
    // v1（旧单绑定格式）→ 迁移成一条项目记录并保持激活
    if (raw?.version === 1 && raw.project?.name) {
      const entry: ProjectEntry = {
        id: newProjectId(),
        name: String(raw.project.name),
        alias: String(raw.project.name),
        remark: '',
        createdAt: Number(raw.project.boundAt) || Date.now(),
        binding: raw.project
      }
      return {version: 2, projects: [entry], activeId: entry.id}
    }
    if (raw?.version === 2 && Array.isArray(raw.projects)) {
      return {
        version: 2,
        projects: raw.projects.filter((p: ProjectEntry) => p && p.id && p.name),
        activeId: typeof raw.activeId === 'string' ? raw.activeId : null
      }
    }
    return empty
  } catch {
    return empty
  }
}

function saveStore(s: ProjectStore): void {
  fs.mkdirSync(path.dirname(stateFile()), { recursive: true })
  fs.writeFileSync(stateFile(), JSON.stringify(s, null, 2))
}

/** 项目列表 + 当前激活项 */
export function listProjects(): { projects: ProjectEntry[]; activeId: string | null } {
  const s = loadStore()
  return { projects: s.projects.map((p) => ({ ...p })), activeId: s.activeId }
}

/** 当前激活项目的绑定（未激活 / 未绑定为 null）——控制台全部逻辑基于它 */
export function getBinding(): ProjectBinding | null {
  const s = loadStore()
  const active = s.projects.find((p) => p.id === s.activeId)
  return active?.binding ?? null
}

/** 绑定写入当前激活项目；无激活项目时（旧流程兜底）按绑定名称落成一条新记录并激活 */
export function saveBinding(b: ProjectBinding): void {
  const s = loadStore()
  let entry = s.projects.find((p) => p.id === s.activeId)
  if (!entry) {
    entry = {
      id: newProjectId(),
      name: b.name,
      alias: b.name,
      remark: '',
      createdAt: Date.now(),
      binding: null
    }
    s.projects.push(entry)
    s.activeId = entry.id
  }
  entry.binding = b
  saveStore(s)
}

/** 解绑：清空当前项目的绑定信息，项目记录保留在列表中（activeId 一并清空，回到列表） */
export function clearBinding(): void {
  const s = loadStore()
  const entry = s.projects.find((p) => p.id === s.activeId)
  if (entry) entry.binding = null
  s.activeId = null
  saveStore(s)
}

/** 创建项目：校验名称（不允许中文，仅字母数字与 . _ -）与别名，重名拒绝；创建后即激活（进入绑定向导） */
export function createProject(input: { name: string; alias: string; remark: string }): ProjectEntry {
  const name = String(input.name ?? '').trim()
  const alias = String(input.alias ?? '').trim()
  const remark = String(input.remark ?? '').trim()
  if (!name) throw new Error('请填写项目名称')
  if (PROJECT_NAME_CJK_RE.test(name)) throw new Error('项目名称不允许中文')
  if (!PROJECT_NAME_RE.test(name)) throw new Error('项目名称仅支持字母、数字与 . _ -')
  if (!alias) throw new Error('请填写项目别名')
  const s = loadStore()
  if (s.projects.some((p) => p.name === name)) throw new Error(`已存在同名项目：${name}`)
  const entry: ProjectEntry = { id: newProjectId(), name, alias, remark, createdAt: Date.now(), binding: null }
  s.projects.push(entry)
  s.activeId = entry.id
  saveStore(s)
  return { ...entry }
}

/** 激活项目（进入控制台）：返回其绑定信息（待绑定为 null） */
export function activateProject(id: string): ProjectBinding | null {
  const s = loadStore()
  const entry = s.projects.find((p) => p.id === id)
  if (!entry) throw new Error('项目不存在或已删除')
  s.activeId = id
  saveStore(s)
  return entry.binding ? { ...entry.binding } : null
}

/** 更新项目信息（名称/别名/备注）：名称校验同创建（不允许中文，仅字母数字与 . _ -，重名拒绝） */
export function updateProject(
  id: string,
  patch: { name?: string; alias?: string; remark?: string }
): ProjectEntry {
  const s = loadStore()
  const entry = s.projects.find((p) => p.id === id)
  if (!entry) throw new Error('项目不存在或已删除')
  const name = patch.name !== undefined ? String(patch.name).trim() : entry.name
  const alias = patch.alias !== undefined ? String(patch.alias).trim() : entry.alias
  const remark = patch.remark !== undefined ? String(patch.remark).trim() : entry.remark
  if (!name) throw new Error('请填写项目名称')
  if (PROJECT_NAME_CJK_RE.test(name)) throw new Error('项目名称不允许中文')
  if (!PROJECT_NAME_RE.test(name)) throw new Error('项目名称仅支持字母、数字与 . _ -')
  if (!alias) throw new Error('请填写项目别名')
  if (s.projects.some((p) => p.name === name && p.id !== id)) throw new Error(`已存在同名项目：${name}`)
  entry.name = name
  entry.alias = alias
  entry.remark = remark
  saveStore(s)
  return { ...entry }
}

/** 退出到项目列表：仅清空激活项，不动绑定 */
export function deactivateProject(): void {
  const s = loadStore()
  s.activeId = null
  saveStore(s)
}

/** 删除项目（若删除的是当前激活项目则同时清空激活项） */
export function removeProject(id: string): void {
  const s = loadStore()
  const idx = s.projects.findIndex((p) => p.id === id)
  if (idx < 0) throw new Error('项目不存在或已删除')
  s.projects.splice(idx, 1)
  if (s.activeId === id) s.activeId = null
  saveStore(s)
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
 * 状态文件统一落 userData（与 bookmarks.json / configs.json 同目录，仓库之外）。
 * 旧版本存应用目录，userData 下缺文件时把历史文件搬过来，只迁一次、不删原件
 */
export function userDataStateFile(fileName: string): string {
  const target = path.join(app.getPath('userData'), fileName)
  try {
    if (!fs.existsSync(target)) {
      const legacy = path.join(app.getAppPath(), fileName)
      if (fs.existsSync(legacy)) {
        fs.mkdirSync(path.dirname(target), { recursive: true })
        fs.copyFileSync(legacy, target)
      }
    }
  } catch {
    /* 迁移失败按全新状态文件处理 */
  }
  return target
}

/**
 * 数据库初始化标记：userData/.db-init.json（项目路径 → 完成时间，JSON）
 */
function dbInitMarkFile(): string {
  return userDataStateFile('.db-init.json')
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
