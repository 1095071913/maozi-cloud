import type {HostsEntry} from './types'

/**
 * hosts 文件解析与序列化（平台无关，darwin / win32 共用）
 */

const ENTRY_RE = /^(\S+)\s+(.+)$/

/** 解析 hosts 文本为行列表（空行丢弃） */
export function parseHosts(text: string): HostsEntry[] {
  const entries: HostsEntry[] = []
  const lines = text.split('\n')
  lines.forEach((line, index) => {
    const trimmed = line.trim()
    if (!trimmed) return
    const id = `line-${index}`
    if (trimmed.startsWith('#')) {
      // 注释行：去掉 # 后若能解析为 IP 映射，则视为被禁用的 entry
      const uncommented = trimmed.replace(/^#+\s*/, '')
      const parsed = parseEntryLine(uncommented)
      if (parsed) {
        entries.push({ id, kind: 'entry', ...parsed, enabled: false })
      } else {
        entries.push({ id, kind: 'comment', enabled: false, raw: trimmed })
      }
      return
    }
    const parsed = parseEntryLine(trimmed)
    if (parsed) {
      entries.push({ id, kind: 'entry', ...parsed, enabled: true })
    } else {
      entries.push({ id, kind: 'comment', enabled: false, raw: trimmed })
    }
  })
  return entries
}

function parseEntryLine(line: string): { ip: string; domains: string; comment?: string } | null {
  const m = line.match(ENTRY_RE)
  if (!m) return null
  const ip = m[1]
  if (!/^[0-9a-fA-F:.]+$/.test(ip)) return null
  let rest = m[2].trim()
  let comment: string | undefined
  const hashIndex = rest.indexOf('#')
  if (hashIndex >= 0) {
    comment = rest.slice(hashIndex + 1).trim() || undefined
    rest = rest.slice(0, hashIndex).trim()
  }
  if (!rest) return null
  return { ip, domains: rest, comment }
}

/** 序列化行列表为 hosts 文本 */
export function renderHosts(entries: HostsEntry[]): string {
  const lines = entries.map((e) => {
    if (e.kind === 'comment') {
      return e.raw?.startsWith('#') ? e.raw : `# ${e.raw ?? ''}`.trimEnd()
    }
    const body = `${e.ip ?? ''} ${e.domains ?? ''}`.trim()
    const commented = e.enabled ? body : `# ${body}`
    return e.comment ? `${commented} # ${e.comment}` : commented
  })
  return `${lines.join('\n')}\n`
}
