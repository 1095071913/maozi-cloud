/**
 * 批量重命名项目：将该目录下所有目录名、文件名、文件内容与根目录名中的旧字符替换为新字符。
 *
 * 执行管线（全部在主进程，文件内容替换走 worker 线程池）：
 * 1. scan    递归遍历（无限深度，跳过排除目录与符号链接），收集文件/目录清单
 * 2. content worker 线程池（N = min(8, CPU 核数)）并行做字节级替换：Buffer.indexOf 定位、
 *            分段拼接写回，多组规则按序应用；textOnly 时先做 NUL 字节检测防二进制破坏
 * 3. rename  文件重命名（原始路径，逐个执行）→ 目录重命名（深度降序，子目录先改，
 *            父目录 rename 后子孙路径自动跟随）→ 根目录自身（返回新路径）
 *
 * 进度经 rename:progress 事件推送（80ms 节流 + 阶段切换强制）；目标名已存在时跳过并记错
 * （大小写不敏感卷上的纯大小写改名放行）。
 */
import {BrowserWindow, ipcMain} from 'electron'
import {Worker} from 'node:worker_threads'
import fsp from 'node:fs/promises'
import path from 'node:path'
import os from 'node:os'
import type {RenameOptions, RenameProgress, RenameRule, RenameStats} from './types'

/** 文本类扩展名白名单（textOnly 时参与内容替换） */
const TEXT_EXTS = new Set([
  '.txt', '.md', '.markdown', '.json', '.jsonc', '.json5', '.yml', '.yaml', '.xml', '.html', '.htm',
  '.css', '.scss', '.less', '.js', '.mjs', '.cjs', '.ts', '.mts', '.cts', '.tsx', '.jsx', '.vue',
  '.svelte', '.astro', '.java', '.kt', '.kts', '.gradle', '.groovy', '.py', '.rb', '.php', '.go',
  '.rs', '.c', '.h', '.cpp', '.hpp', '.cc', '.hh', '.cs', '.swift', '.m', '.mm', '.scala', '.clj',
  '.sh', '.bash', '.zsh', '.fish', '.bat', '.cmd', '.ps1', '.sql', '.properties', '.toml', '.ini',
  '.cfg', '.conf', '.env', '.lock', '.editorconfig', '.gitignore', '.dockerignore', '.eslintrc',
  '.prettierrc', '.babelrc', '.npmrc', '.csv', '.log', '.proto', '.thrift', '.graphql', '.gql',
  '.dockerfile', '.makefile', '.cmake', '.gradlew', '.svnignore', '.npmignore', '.nvmrc', '.node-version',
  // 脚本：Lua
  '.lua',
  // 模板类：项目模板（.template/.tpl）与常见模板引擎（FreeMarker/Velocity/Jinja/EJS/Handlebars/Mustache/Liquid/Twig/ERB）
  '.template', '.tpl', '.ftl', '.vm', '.j2', '.jinja', '.jinja2', '.ejs', '.hbs', '.handlebars',
  '.mustache', '.liquid', '.twig', '.erb'
])

/** 无扩展名的常见文本文件名（前缀匹配，大小写不敏感） */
const TEXT_NAMES = [/^dockerfile/i, /^makefile/i, /^jenkinsfile/i, /^vagrantfile/i, /^rakefile/i, /^gemfile/i]

/** 按扩展名、dotfile 名或常见无扩展名文件名判断是否为文本文件 */
function isTextFile(name: string): boolean {
  const ext = path.extname(name).toLowerCase()
  if (ext && TEXT_EXTS.has(ext)) return true
  // dotfile（.gitignore / .env / .env.local 等）：path.extname 忽略前导点返回空或仅后段，
  // 改按完整文件名与首段（.env.local → .env）匹配白名单
  if (name.startsWith('.')) {
    const lower = name.toLowerCase()
    const dot2 = lower.indexOf('.', 1)
    const first = dot2 > 0 ? lower.slice(0, dot2) : lower
    if (TEXT_EXTS.has(lower) || TEXT_EXTS.has(first)) return true
  }
  return TEXT_NAMES.some(re => re.test(name))
}

/** 强制文本目录：路径段命中（大小写不敏感，任意层级、无限深度）的文件一律参与内容替换，
 *  不受扩展名白名单与 NUL 二进制检测限制（jar 解包的 META-INF 下多为无扩展名文本） */
const FORCE_TEXT_DIRS = new Set(['META-INF'])

/** 路径中是否含强制文本目录段 */
function underForceDir(filePath: string): boolean {
  return filePath.split(/[\\/]+/).some(seg => FORCE_TEXT_DIRS.has(seg.toUpperCase()))
}

/**
 * worker 内联代码（CJS eval，绕开 esbuild 打包 worker 文件路径问题）：
 * 领任务 → 读文件 → NUL 检测 → 多规则字节替换 → 有变化才写回 → 回报结果
 */
const WORKER_CODE = String.raw`
const parentPort = require('node:worker_threads').parentPort
const fsp = require('node:fs/promises')
parentPort.on('message', (msg) => {
  if (msg.type === 'end') return parentPort.close()
  const task = msg.task
  Promise.resolve().then(async () => {
    try {
      const buf = await fsp.readFile(task.file)
      // 二进制防护：前 8KB 含 NUL 字节按非文本跳过（textOnly 时）
      if (task.textOnly && buf.subarray(0, 8192).indexOf(0) >= 0) {
        return { file: task.file, changed: false, count: 0 }
      }
      let out = buf
      let count = 0
      let changed = false
      for (const r of task.rules) {
        const from = Buffer.from(r.from, 'utf8')
        if (from.length === 0 || out.length < from.length) continue
        const to = Buffer.from(r.to, 'utf8')
        let idx = out.indexOf(from)
        if (idx < 0) continue
        const parts = []
        let pos = 0
        while (idx >= 0) {
          parts.push(out.subarray(pos, idx), to)
          pos = idx + from.length
          count++
          idx = out.indexOf(from, pos)
        }
        parts.push(out.subarray(pos))
        out = Buffer.concat(parts)
        changed = true
      }
      if (changed) await fsp.writeFile(task.file, out)
      return { file: task.file, changed, count }
    } catch (err) {
      return { file: task.file, error: String((err && err.message) || err) }
    }
  }).then((res) => parentPort.postMessage(res))
})
`

/** 单次运行上下文（进度推送与取消都围绕它） */
interface RunCtx {
  /** 进度推送目标窗口 */
  win: BrowserWindow | null
  /** 是否已取消（由 syncCancel 从 currentCancel 同步） */
  cancelled: boolean
  stats: RenameStats
  /** 内容替换 + 重命名的总项数 */
  total: number
  /** 已完成项数 */
  done: number
  /** 上次推送时间（节流用） */
  lastEmit: number
}

/** 推送进度：80ms 节流，force 时立即推送（用于阶段切换） */
function emit(ctx: RunCtx, stage: RenameProgress['stage'], current: string, force = false): void {
  const now = Date.now()
  if (!force && now - ctx.lastEmit < 80) return
  ctx.lastEmit = now
  const payload: RenameProgress = { stage, done: ctx.done, total: ctx.total, current, stats: { ...ctx.stats } }
  ctx.win?.webContents.send('rename:progress', payload)
}

/** 记录错误信息（最多保留 50 条） */
function pushError(ctx: RunCtx, msg: string): void {
  if (ctx.stats.errors.length < 50) ctx.stats.errors.push(msg)
}

/** 应用全部规则到一个名称（basename 级字符串替换） */
function applyRules(name: string, rules: RenameRule[]): string {
  let out = name
  for (const r of rules) out = out.split(r.from).join(r.to)
  return out
}

/** 扫描到的文件 / 目录 */
interface ScanEntry {
  /** 原始绝对路径 */
  path: string
  /** 名称（basename） */
  name: string
  /** 层级深度（根目录的直接子项为 1） */
  depth: number
}

/** 递归扫描：跳过排除目录与符号链接；顺带统计 */
async function scan(
  root: string,
  exclude: Set<string>,
  ctx: RunCtx,
  onFile: (e: ScanEntry) => void
): Promise<{ files: ScanEntry[]; dirs: ScanEntry[] }> {
  const files: ScanEntry[] = []
  const dirs: ScanEntry[] = []
  const walk = async (dir: string, depth: number): Promise<void> => {
    syncCancel(ctx)
    if (ctx.cancelled) return
    let entries
    try {
      entries = await fsp.readdir(dir, { withFileTypes: true })
    } catch (err) {
      pushError(ctx, `读取目录失败 ${dir}：${(err as Error).message}`)
      return
    }
    for (const ent of entries) {
      if (ctx.cancelled) return
      // 符号链接跳过：防止循环与越界改写
      if (ent.isSymbolicLink()) continue
      if (ent.isDirectory()) {
        ctx.stats.scannedDirs++
        if (exclude.has(ent.name)) continue
        const item: ScanEntry = { path: path.join(dir, ent.name), name: ent.name, depth }
        dirs.push(item)
        emit(ctx, 'scan', path.relative(root, item.path) || ent.name)
        await walk(item.path, depth + 1)
      } else if (ent.isFile()) {
        ctx.stats.scannedFiles++
        const item: ScanEntry = { path: path.join(dir, ent.name), name: ent.name, depth }
        files.push(item)
        onFile(item)
        emit(ctx, 'scan', path.relative(root, item.path) || ent.name)
      }
    }
  }
  await walk(root, 1)
  return { files, dirs }
}

/**
 * 驱动单个 worker：循环从共享队列领文件，队列为空或已取消时终止该 worker 并 resolve。
 * 调用方用 Promise.all 等待全部 worker；单文件异常经 onResult 记入 stats.errors，不中断整体
 */
function startContentWorker(
  worker: Worker,
  queue: string[],
  rules: RenameRule[],
  textOnly: boolean,
  ctx: RunCtx,
  root: string,
  onResult: (res: { changed?: boolean; count?: number; error?: string }) => void
): Promise<void> {
  return new Promise(resolve => {
    const next = (): void => {
      syncCancel(ctx)
      if (ctx.cancelled) {
        void worker.terminate()
        resolve()
        return
      }
      const file = queue.shift()
      if (file === undefined) {
        worker.postMessage({ type: 'end' })
        void worker.terminate()
        resolve()
        return
      }
      emit(ctx, 'content', path.relative(root, file))
      worker.once('message', (res: { changed?: boolean; count?: number; error?: string }) => {
        onResult(res)
        ctx.done++
        emit(ctx, 'content', path.relative(root, file))
        next()
      })
      // 强制文本目录（META-INF）的文件同时豁免 worker 内的 NUL 二进制检测
      worker.postMessage({ type: 'task', task: { file, rules, textOnly: textOnly && !underForceDir(file) } })
    }
    next()
  })
}

/** 安全重命名：目标已存在时跳过（大小写不敏感卷上的纯大小写改名放行） */
async function safeRename(ctx: RunCtx, from: string, to: string): Promise<boolean> {
  try {
    const st = await fsp.lstat(to).catch(() => null)
    if (st) {
      const rf = await fsp.realpath(from).catch(() => from)
      const rt = await fsp.realpath(to).catch(() => to)
      if (rf !== rt) {
        pushError(ctx, `目标已存在，跳过：${path.basename(to)}`)
        return false
      }
    }
    await fsp.rename(from, to)
    return true
  } catch (err) {
    pushError(ctx, `重命名失败 ${path.basename(from)} → ${path.basename(to)}：${(err as Error).message}`)
    return false
  }
}

/** 取消请求标记：rename:cancel 置位，各阶段检查点同步进 ctx */
let currentCancel = false

/** 检查点：把取消标记同步进上下文 */
function syncCancel(ctx: RunCtx): void {
  if (currentCancel) ctx.cancelled = true
}

/** 注册批量重命名 IPC：rename:run 执行、rename:cancel 取消 */
export function registerRenameHandlers(): void {
  /** 执行批量重命名，返回统计与（可能已改名的）根目录新路径 */
  ipcMain.handle('rename:run', async (_e, input: { root?: string; rules?: RenameRule[]; options?: RenameOptions }) => {
    const root = (input.root ?? '').trim()
    // from 不做 trim：空格等空白本身就是合法的"旧字符"
    const rules = (input.rules ?? [])
      .map(r => ({ from: r.from ?? '', to: r.to ?? '' }))
      .filter(r => r.from.length > 0 && r.from !== r.to)
    if (!root) return { ok: false, error: '请选择目录' }
    if (!rules.length) return { ok: false, error: '请至少填写一组有效替换规则（旧字符非空且新旧不同）' }
    try {
      const st = await fsp.stat(root)
      if (!st.isDirectory()) return { ok: false, error: '所选路径不是目录' }
    } catch {
      return { ok: false, error: '目录不存在或无法访问' }
    }

    const opts = {
      includeContent: input.options?.includeContent ?? true,
      renameFiles: input.options?.renameFiles ?? true,
      renameDirs: input.options?.renameDirs ?? true,
      textOnly: input.options?.textOnly ?? true,
      maxFileMB: input.options?.maxFileMB ?? 16,
      excludeDirs: input.options?.excludeDirs ?? []
    }
    if (!opts.includeContent && !opts.renameFiles && !opts.renameDirs) {
      return { ok: false, error: '请至少保留一项替换操作（文件内容 / 文件名称 / 目录名称）' }
    }
    const exclude = new Set(opts.excludeDirs.map(s => s.trim()).filter(Boolean))
    const win = BrowserWindow.getFocusedWindow() ?? BrowserWindow.getAllWindows()[0] ?? null

    const ctx: RunCtx = {
      win,
      cancelled: false,
      total: 0,
      done: 0,
      lastEmit: 0,
      stats: {
        scannedFiles: 0,
        scannedDirs: 0,
        contentChanged: 0,
        replaceCount: 0,
        filesRenamed: 0,
        dirsRenamed: 0,
        rootRenamed: false,
        skippedLarge: 0,
        errors: [],
        cancelled: false
      }
    }
    currentCancel = false

    let newRoot = root
    try {
      // ===== 阶段 1：扫描 =====
      const maxBytes = opts.maxFileMB * 1024 * 1024
      const contentFiles: string[] = []
      const { files, dirs } = await scan(root, exclude, ctx, item => {
        if (!opts.includeContent) return
        // textOnly 白名单过滤；强制文本目录（META-INF）豁免
        if (opts.textOnly && !underForceDir(item.path) && !isTextFile(item.name)) return
        contentFiles.push(item.path)
      })
      syncCancel(ctx)
      if (ctx.cancelled) {
        ctx.stats.cancelled = true
        emit(ctx, 'done', '', true)
        return { ok: true, data: { stats: ctx.stats, newRoot } }
      }

      // 大小过滤（超限不进池，计 skippedLarge）
      const poolFiles: string[] = []
      for (const f of contentFiles) {
        try {
          const st = await fsp.stat(f)
          if (st.size > maxBytes) ctx.stats.skippedLarge++
          else poolFiles.push(f)
        } catch {
          poolFiles.push(f)
        }
      }

      // 待重命名项（renameDirs 含根目录自身；选项关闭时对应列表置空）
      const renameFiles = opts.renameFiles ? files.filter(f => applyRules(f.name, rules) !== f.name) : []
      const renameDirs = opts.renameDirs ? dirs.filter(d => applyRules(d.name, rules) !== d.name) : []
      const rootName = path.basename(root)
      const newRootName = applyRules(rootName, rules)
      const rootWillRename = opts.renameDirs && newRootName !== rootName

      ctx.total = poolFiles.length + renameFiles.length + renameDirs.length + (rootWillRename ? 1 : 0)
      ctx.done = 0
      emit(ctx, 'content', '', true)

      // ===== 阶段 2：文件内容（worker 线程池并行） =====
      if (poolFiles.length) {
        const n = Math.max(1, Math.min(8, os.cpus().length, poolFiles.length))
        const queue = [...poolFiles]
        const rulesForWorker = rules.map(r => ({ from: r.from, to: r.to }))
        const workers = Array.from({ length: n }, () => new Worker(WORKER_CODE, { eval: true, stdout: true, stderr: true }))
        const onResult = (res: { changed?: boolean; count?: number; error?: string }): void => {
          if (res.error) {
            pushError(ctx, `内容替换失败 ${path.relative(root, res.file ?? '')}：${res.error}`)
            return
          }
          if (res.changed) {
            ctx.stats.contentChanged++
            ctx.stats.replaceCount += res.count ?? 0
          }
        }
        await Promise.all(
          workers.map(w => startContentWorker(w, queue, rulesForWorker, opts.textOnly, ctx, root, onResult))
        )
      }
      if (ctx.cancelled) {
        ctx.stats.cancelled = true
        emit(ctx, 'done', '', true)
        return { ok: true, data: { stats: ctx.stats, newRoot } }
      }

      // ===== 阶段 3：重命名（文件 → 目录深度降序 → 根） =====
      // 先文件（原始路径逐个改名，互不影响）；再目录（子目录先改，父目录改名后子孙路径自动跟随）
      emit(ctx, 'rename', '', true)
      for (const f of renameFiles) {
        syncCancel(ctx)
        if (ctx.cancelled) break
        const newName = applyRules(f.name, rules)
        const target = path.join(path.dirname(f.path), newName)
        emit(ctx, 'rename', path.relative(root, f.path))
        if (await safeRename(ctx, f.path, target)) ctx.stats.filesRenamed++
        ctx.done++
      }
      for (const d of [...renameDirs].sort((a, b) => b.depth - a.depth)) {
        syncCancel(ctx)
        if (ctx.cancelled) break
        const newName = applyRules(d.name, rules)
        const target = path.join(path.dirname(d.path), newName)
        emit(ctx, 'rename', path.relative(root, d.path))
        if (await safeRename(ctx, d.path, target)) ctx.stats.dirsRenamed++
        ctx.done++
      }
      if (!ctx.cancelled && rootWillRename) {
        const parent = path.dirname(root)
        const target = path.join(parent, newRootName)
        emit(ctx, 'rename', rootName, true)
        if (await safeRename(ctx, root, target)) {
          ctx.stats.rootRenamed = true
          newRoot = target
        }
        ctx.done++
      }
      if (ctx.cancelled) ctx.stats.cancelled = true
      emit(ctx, 'done', '', true)
      return { ok: true, data: { stats: ctx.stats, newRoot } }
    } catch (err) {
      emit(ctx, 'error', (err as Error).message, true)
      return { ok: false, error: (err as Error).message }
    } finally {
      currentCancel = false
    }
  })

  /** 请求取消，下一个检查点生效 */
  ipcMain.handle('rename:cancel', () => {
    currentCancel = true
    return { ok: true }
  })
}
