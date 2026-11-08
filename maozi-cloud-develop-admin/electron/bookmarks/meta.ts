import {net} from 'electron'
import type {PageMeta} from './types'

/**
 * 网页信息抓取：标题 / 描述 / 图标
 * 使用 Electron net.fetch（Chromium 网络栈）——自动走系统代理，
 * 浏览器能访问的站点这里就能抓到；只读 HTML 头部（前 256KB），8 秒超时
 */

/** 伪装为桌面版 Chrome，避免部分站点拦截非浏览器请求 */
const UA =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'

/** 补全协议（缺省 https）后解析为 URL */
function normalizeUrl(raw: string): URL {
  const trimmed = raw.trim()
  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`
  return new URL(withScheme)
}

/** 带超时的 net.fetch：超时即 abort（只覆盖到响应头返回为止） */
async function fetchWithTimeout(url: string, ms: number): Promise<Response> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), ms)
  try {
    return await net.fetch(url, {
      signal: controller.signal,
      redirect: 'follow',
      headers: { 'User-Agent': UA, Accept: 'text/html,*/*' }
    })
  } finally {
    clearTimeout(timer)
  }
}

/** 解码常见 HTML 实体并去除首尾空白 */
function decodeEntities(s: string): string {
  return s
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;|&apos;/g, "'")
    .replace(/&nbsp;/g, ' ')
    .trim()
}

/** 取 <meta name|property=name> 的 content，兼容 content 写在前面的属性顺序 */
function matchMetaTag(html: string, name: string): string | undefined {
  const re = new RegExp(
    `<meta[^>]+(?:name|property)=["']${name}["'][^>]*content=["']([^"']*)["']`,
    'i'
  )
  const reReverse = new RegExp(
    `<meta[^>]+content=["']([^"']*)["'][^>]*(?:name|property)=["']${name}["']`,
    'i'
  )
  const m = html.match(re) ?? html.match(reReverse)
  const value = m ? decodeEntities(m[1]) : ''
  return value || undefined
}

/** 按 rel 优先级收集图标地址：icon > apple-touch-icon > fluid-icon > 其他（mask-icon 等） */
function collectIconHrefs(html: string): string[] {
  const found: { href: string; priority: number }[] = []
  for (const tag of html.match(/<link[^>]+>/gi) ?? []) {
    const rel = tag.match(/rel=["']([^"']+)["']/i)?.[1]?.toLowerCase() ?? ''
    if (!rel.includes('icon')) continue
    const href = tag.match(/href=["']([^"']+)["']/i)?.[1]
    if (!href) continue
    let priority = 3
    if (rel === 'icon' || rel === 'shortcut icon') priority = 0
    else if (rel.includes('apple-touch-icon')) priority = 1
    else if (rel === 'fluid-icon') priority = 2
    found.push({ href, priority })
  }
  return found.sort((a, b) => a.priority - b.priority).map((f) => f.href)
}

/** 图标响应转 dataURL；为空、超过 512KB 或非图片类型（octet-stream 放行）时返回 undefined */
async function toDataUrl(res: Response): Promise<string | undefined> {
  const type = res.headers.get('content-type') ?? ''
  const buf = Buffer.from(await res.arrayBuffer())
  if (buf.length === 0 || buf.length > 512 * 1024) return undefined
  if (!type.startsWith('image/') && !type.includes('octet-stream')) return undefined
  const mime = type.split(';')[0] || 'image/png'
  return `data:${mime};base64,${buf.toString('base64')}`
}

/** 抓取网页标题、描述与图标；出错时返回已解析到的部分 */
export async function fetchPageMeta(rawUrl: string): Promise<PageMeta> {
  const meta: PageMeta = {}
  try {
    const url = normalizeUrl(rawUrl)
    const res = await fetchWithTimeout(url.toString(), 8000)
    const reader = res.body?.getReader()
    let html = ''
    if (reader) {
      const decoder = new TextDecoder('utf-8', { fatal: false })
      while (html.length < 256 * 1024) {
        const { done, value } = await reader.read()
        if (done) break
        html += decoder.decode(value, { stream: true })
        // head 一般在最前面，读完 </head> 即可提前停止
        if (/<\/head>/i.test(html)) break
      }
      // 可能提前跳出，主动取消以释放剩余响应体
      await reader.cancel().catch(() => {})
    } else {
      html = await res.text()
    }

    const title = html.match(/<title[^>]*>([^<]*)<\/title>/i)
    if (title) meta.title = decodeEntities(title[1]).slice(0, 120) || undefined
    meta.description = matchMetaTag(html, 'description')?.slice(0, 300)

    // 图标：<link rel=icon> 按优先级，最后兜底根路径 favicon.ico
    const candidates = [...collectIconHrefs(html), new URL('/favicon.ico', url).toString()]

    for (const href of candidates) {
      try {
        // 协议相对地址补 https，相对路径按页面地址解析
        const abs = href.startsWith('//')
          ? `https:${href}`
          : href.startsWith('http') || href.startsWith('data:')
            ? href
            : new URL(href, url).toString()
        if (abs.startsWith('data:')) {
          if (abs.length < 512 * 1024) {
            meta.icon = abs
            break
          }
          continue
        }
        const iconRes = await fetchWithTimeout(abs, 6000)
        if (!iconRes.ok) continue
        const dataUrl = await toDataUrl(iconRes)
        if (dataUrl) {
          meta.icon = dataUrl
          break
        }
      } catch {
        /* 尝试下一个候选 */
      }
    }
  } catch {
    /* 网络失败时返回已解析到的部分（通常为空） */
  }
  return meta
}
