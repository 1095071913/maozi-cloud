import {execFile} from 'node:child_process'
import {homedir, tmpdir} from 'node:os'
import fs from 'node:fs'
import path from 'node:path'
import {promisify} from 'node:util'
import type {EnvFile, EnvSaveParams, EnvVarEntry, HostsEntry, LaunchctlParams, Platform} from './types'
import {parseHosts, renderHosts} from './shared'

const exec = promisify(execFile)

/** macOS hosts 文件路径 */
export const HOSTS_PATH = '/etc/hosts'

/** 托管区块标记：本应用新增的变量统一写入区块内，避免与用户手写内容混排 */
const MANAGED_BEGIN = '# >>> maozi-cloud-develop-admin (managed) >>>'
const MANAGED_END = '# <<< maozi-cloud-develop-admin (managed) <<<'

/** macOS 常见 shell 配置文件：~/.zshrc 为 zsh 交互主配置（环境设置保存的默认目标），
 *  排在最前决定默认文件与同名变量的归属优先级；.zshenv 仅适合极早期变量，不作为默认写入目标 */
const ENV_FILE_DEFS = [
  { id: 'zshrc', name: '~/.zshrc', file: '.zshrc' },
  { id: 'zshenv', name: '~/.zshenv', file: '.zshenv' },
  { id: 'zprofile', name: '~/.zprofile', file: '.zprofile' },
  { id: 'bash_profile', name: '~/.bash_profile', file: '.bash_profile' },
  { id: 'bashrc', name: '~/.bashrc', file: '.bashrc' },
  { id: 'profile', name: '~/.profile', file: '.profile' }
]

/** 启用的 export 行：export KEY=... */
const EXPORT_RE = /^\s*export\s+([A-Za-z_][A-Za-z0-9_]*)=(.*)$/
/** 被注释掉的 export 行（禁用态）：# export KEY=... */
const COMMENTED_EXPORT_RE = /^\s*#\s*export\s+([A-Za-z_][A-Za-z0-9_]*)=(.*)$/

/** 配置文件 id 转绝对路径，未知 id 抛错 */
function envFilePath(fileId: string): string {
  const def = ENV_FILE_DEFS.find((d) => d.id === fileId)
  if (!def) throw new Error(`未知的配置文件: ${fileId}`)
  return path.join(homedir(), def.file)
}

/** 解析 export 行的值部分：剥离行内注释、按引号取值 */
function parseEnvValue(raw: string): string {
  let v = raw.trim()
  const hashMatch = v.match(/^("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')\s+#.*$/) // 引号值后跟行内注释
  if (hashMatch) v = hashMatch[1]
  else if (!v.startsWith('"') && !v.startsWith("'")) {
    const idx = v.indexOf(' #')
    if (idx > 0) v = v.slice(0, idx).trim()
  }
  if (v.length >= 2 && v.startsWith('"') && v.endsWith('"')) {
    return v.slice(1, -1).replace(/\\"/g, '"')
  }
  if (v.length >= 2 && v.startsWith("'") && v.endsWith("'")) {
    return v.slice(1, -1)
  }
  return v
}

/** 生成 export KEY="VALUE" 行（转义双引号与反斜杠） */
function serializeEnvLine(key: string, value: string): string {
  return `export ${key}="${value.replace(/(["\\])/g, '\\$1')}"`
}

/** 按行读取配置文件，文件不存在时返回空数组 */
function readEnvFileLines(fileId: string): string[] {
  const p = envFilePath(fileId)
  if (!fs.existsSync(p)) return []
  return fs.readFileSync(p, 'utf8').split('\n')
}

/** 写入文件：默认先把原文件备份为 .maozi.bak；backup=false 时不备份并删除遗留备份 */
function backupAndWrite(file: string, content: string, opts?: { backup?: boolean }): void {
  if (opts?.backup === false) {
    // 应用全量生成、可随时重建的文件无需备份；顺手清掉历史遗留备份，避免已删除的密钥明文残留
    fs.rmSync(`${file}.maozi.bak`, { force: true })
  } else if (fs.existsSync(file)) {
    fs.copyFileSync(file, `${file}.maozi.bak`)
  }
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, content, { mode: 0o644 })
}

/** macOS 平台实现：环境变量写入 shell 配置文件，hosts 经管理员授权或免密助手写入 */
export const darwinPlatform: Platform = {
  id: 'darwin',

  hostsPath(): string {
    return HOSTS_PATH
  },

  listEnvFiles(): EnvFile[] {
    return ENV_FILE_DEFS.map((d) => {
      const p = path.join(homedir(), d.file)
      return { id: d.id, name: d.name, path: p, exists: fs.existsSync(p) }
    })
  },

  /** 扫描全部配置文件的 export 行（含被注释的禁用行），并标记是否位于托管区块内 */
  readEnvVars(): EnvVarEntry[] {
    const result: EnvVarEntry[] = []
    for (const def of ENV_FILE_DEFS) {
      const lines = readEnvFileLines(def.id)
      let inManaged = false
      for (const line of lines) {
        if (line.trim() === MANAGED_BEGIN) {
          inManaged = true
          continue
        }
        if (line.trim() === MANAGED_END) {
          inManaged = false
          continue
        }
        const m = line.match(EXPORT_RE)
        if (m) {
          result.push({
            key: m[1],
            value: parseEnvValue(m[2]),
            fileId: def.id,
            managed: inManaged,
            enabled: true
          })
          continue
        }
        const cm = line.match(COMMENTED_EXPORT_RE)
        if (cm) {
          result.push({
            key: cm[1],
            value: parseEnvValue(cm[2]),
            fileId: def.id,
            managed: inManaged,
            enabled: false
          })
        }
      }
    }
    return result
  },

  /** 按 mode 增 / 改 / 删 / 启停 / 改名 / 排序变量，写回后刷新实时环境文件 */
  saveEnvVar(params: EnvSaveParams): void {
    const { mode, key = '', value = '', fileId } = params
    if (mode === 'reorder') {
      const lines = readEnvFileLines(fileId)
      reorderExportLines(lines, params.orderedKeys ?? [])
      const content = cleanupEmptyManagedBlock(lines).join('\n')
      backupAndWrite(envFilePath(fileId), `${content}\n`, { backup: false })
      installLiveHook()
      syncLiveEnv()
      return
    }
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) {
      throw new Error(`非法的变量名: ${key}`)
    }
    const filePath = envFilePath(fileId)
    const lines = readEnvFileLines(fileId)

    // 去掉结尾的空行，便于后面统一处理
    while (lines.length > 0 && lines[lines.length - 1].trim() === '') lines.pop()

    // 定位目标变量所在行（启用或禁用态均可）
    const matchIndex = lines.findIndex((l) => {
      const m = l.match(EXPORT_RE)
      if (m !== null) return m[1] === key
      const cm = l.match(COMMENTED_EXPORT_RE)
      return cm !== null && cm[1] === key
    })
    const matchedCommented = matchIndex >= 0 && COMMENTED_EXPORT_RE.test(lines[matchIndex])
    // 值写入模式（update/add）才做跨文件迁移清理；toggle/rename/remove 不动其它文件
    let valueWritten = false

    if (mode === 'toggle') {
      if (matchIndex < 0) throw new Error(`未找到变量 ${key}`)
      // 启用=去掉行首注释；禁用=加 # 前缀（保持行位置与值不动）
      lines[matchIndex] = params.enabled
        ? lines[matchIndex].replace(/^\s*#\s*/, '')
        : `# ${lines[matchIndex].replace(COMMENTED_EXPORT_RE, 'export $1=$2')}`
    } else if (mode === 'rename') {
      const newKey = params.newKey ?? ''
      if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(newKey)) throw new Error(`非法的变量名: ${newKey}`)
      if (matchIndex < 0) throw new Error(`未找到变量 ${key}`)
      if (newKey !== key) {
        const dup = lines.some((l) => {
          const m = l.match(EXPORT_RE) ?? l.match(COMMENTED_EXPORT_RE)
          return m?.[1] === newKey
        })
        if (dup) throw new Error(`${newKey} 在该文件中已存在`)
        // 整行换名：保持原行位置、值与启用/禁用注释态；value 缺省沿用原值
        const raw = lines[matchIndex].match(EXPORT_RE)?.[2] ?? lines[matchIndex].match(COMMENTED_EXPORT_RE)?.[2] ?? ''
        lines[matchIndex] = (matchedCommented ? '# ' : '') + serializeEnvLine(newKey, value || parseEnvValue(raw))
      }
    } else if (mode === 'remove') {
      if (matchIndex < 0) throw new Error(`未找到变量 ${key}`)
      lines.splice(matchIndex, 1)
    } else if (mode === 'update') {
      if (matchIndex >= 0) {
        lines[matchIndex] = (matchedCommented ? '# ' : '') + serializeEnvLine(key, value)
      } else {
        // 变量原在其它配置文件（保存目标已切换到默认文件）：按新增落到目标文件
        appendManaged(lines, serializeEnvLine(key, value))
      }
      valueWritten = true
    } else {
      // add：已存在则视为修改（保持原位置与启用/禁用状态不动）
      if (matchIndex >= 0) {
        lines[matchIndex] = (matchedCommented ? '# ' : '') + serializeEnvLine(key, value)
      } else {
        appendManaged(lines, serializeEnvLine(key, value))
      }
      valueWritten = true
    }

    // 迁移语义（仅值写入模式）：目标文件写入后移除其它配置文件中的同 key 行（含禁用注释行）——
    // 每个变量只保留单一来源，zshenv 旧值不再以加载顺序覆盖 zshrc 新值
    if (valueWritten) {
      for (const def of ENV_FILE_DEFS) {
        if (def.id === fileId) continue
        const p = envFilePath(def.id)
        if (!fs.existsSync(p)) continue
        const other = fs.readFileSync(p, 'utf8').split('\n')
        const idx = other.findIndex((l) => {
          const m = l.match(EXPORT_RE)
          if (m) return m[1] === key
          const cm = l.match(COMMENTED_EXPORT_RE)
          return cm !== null && cm[1] === key
        })
        if (idx >= 0) {
          other.splice(idx, 1)
          backupAndWrite(p, `${cleanupEmptyManagedBlock(other).join('\n')}\n`, { backup: false })
        }
      }
    }

    // 托管区块为空时移除区块标记，保持文件整洁
    const content = cleanupEmptyManagedBlock(lines).join('\n')
    // 行级精确写入（只动托管区块/匹配行）不做 .maozi.bak 备份，避免已删除的密钥明文残留
    backupAndWrite(filePath, `${content}\n`, { backup: false })
    // 已打开的终端经 preexec/precmd 钩子自动同步，无需重开
    installLiveHook()
    syncLiveEnv()
  },

  readHosts(): HostsEntry[] {
    return parseHosts(fs.readFileSync(HOSTS_PATH, 'utf8'))
  },

  readHostsRaw(): string {
    return fs.readFileSync(HOSTS_PATH, 'utf8')
  },

  async writeHosts(entries: HostsEntry[]): Promise<void> {
    await writeHostsText(renderHosts(entries))
  },

  async writeHostsRaw(text: string): Promise<void> {
    await writeHostsText(text)
  },



  /** 刷新 DNS 缓存：优先走免密助手，未安装时弹授权框执行 */
  async flushDns(): Promise<void> {
    if (await helperReady()) {
      await exec('/usr/bin/sudo', ['-n', HELPER_PATH, '--flush'], { timeout: 15_000 })
      return
    }
    await runAsAdmin(
      'dscacheutil -flushcache && killall -HUP mDNSResponder',
      '开发者控制台 请求刷新 DNS 缓存'
    )
  },

  async launchctlSet(params: LaunchctlParams): Promise<void> {
    if (params.action === 'set') {
      if (!params.value) throw new Error('缺少变量值')
      await exec('launchctl', ['setenv', params.key, params.value])
    } else {
      await exec('launchctl', ['unsetenv', params.key])
    }
  }
}

/** 免密 hosts 写入助手（root 属主 + sudoers NOPASSWD），安装后写入不再弹授权框 */
const HELPER_PATH = '/usr/local/bin/maozi-hosts-helper'
const SUDOERS_PATH = '/etc/sudoers.d/maozi-hosts-helper'
const SUDOERS_LINE = '%admin ALL=(root) NOPASSWD: /usr/local/bin/maozi-hosts-helper'

/** 助手版本号：--check 输出与之不一致视为旧版，触发授权重装 */
const HELPER_VERSION = 'v2'
/** 助手脚本内容：仅接受文件名以 maozi-hosts-* 开头的文件（按 basename 校验），写入后刷新 DNS（不备份）；另支持 --check 返回版本、--flush 仅刷新 DNS */
function helperScript(): string {
  return [
    '#!/bin/bash',
    '# maozi-cloud-develop-admin hosts 写入助手（root 执行，由 sudoers 免密调用）',
    'set -euo pipefail',
    'HOSTS="/etc/hosts"',
    `if [ "\${1:-}" = "--check" ]; then echo ${HELPER_VERSION}; exit 0; fi`,
    'if [ "${1:-}" = "--flush" ]; then',
    '  dscacheutil -flushcache && killall -HUP mDNSResponder',
    '  exit 0',
    'fi',
    'SRC="${1:-}"',
    'case "$(basename "$SRC")" in',
    '  maozi-hosts-*) ;;',
    '  *) echo "拒绝非法路径: $SRC" >&2; exit 2 ;;',
    'esac',
    '[ -f "$SRC" ] || { echo "文件不存在: $SRC" >&2; exit 2; }',
    'cp "$SRC" "$HOSTS"',
    'chown root:wheel "$HOSTS" && chmod 644 "$HOSTS"',
    'rm -f "$SRC"',
    'dscacheutil -flushcache && killall -HUP mDNSResponder'
  ].join('\n')
}

/** 免密助手是否可用（sudo -n 不弹密码且版本一致；旧版助手返回空，触发一次授权重装升级） */
async function helperReady(): Promise<boolean> {
  try {
    const { stdout } = await exec('/usr/bin/sudo', ['-n', HELPER_PATH, '--check'], { timeout: 8_000 })
    return stdout.trim() === HELPER_VERSION
  } catch {
    return false
  }
}

/**
 * 按原文写入 hosts：临时文件 + 覆盖 + 刷新 DNS 缓存（不备份）。
 * 已安装免密助手时静默写入；否则弹一次授权框，并在同一次授权中安装助手，之后不再弹窗。
 */
async function writeHostsText(text: string): Promise<void> {
  const tmp = path.join(tmpdir(), `maozi-hosts-${Date.now()}`)
  fs.writeFileSync(tmp, text, { mode: 0o644 })
  try {
    if (await helperReady()) {
      await exec('/usr/bin/sudo', ['-n', HELPER_PATH, tmp], { timeout: 30_000 })
      return
    }
    const helperTmp = path.join(tmpdir(), `maozi-helper-${Date.now()}.sh`)
    fs.writeFileSync(helperTmp, helperScript() + '\n', { mode: 0o755 })
    try {
      // 同一次授权内：覆盖 hosts、刷新 DNS、安装免密助手；sudoers 先写临时文件经 visudo -c 校验后再落位，避免写坏 sudo 配置
      await runAsAdmin(
        `cp ${q(tmp)} ${q(HOSTS_PATH)} && ` +
          `chown root:wheel ${q(HOSTS_PATH)} && chmod 644 ${q(HOSTS_PATH)} && ` +
          `dscacheutil -flushcache && killall -HUP mDNSResponder && ` +
          `mkdir -p /usr/local/bin && ` +
          `cp ${q(helperTmp)} ${q(HELPER_PATH)} && chown root:wheel ${q(HELPER_PATH)} && chmod 755 ${q(HELPER_PATH)} && ` +
          `umask 022 && echo '${SUDOERS_LINE}' > /etc/sudoers.d/.maozi-tmp && ` +
          `visudo -cf /etc/sudoers.d/.maozi-tmp && ` +
          `mv /etc/sudoers.d/.maozi-tmp ${q(SUDOERS_PATH)} && chmod 440 ${q(SUDOERS_PATH)}`,
        '开发者控制台 请求修改 hosts 文件（首次授权后将安装免密写入助手，后续不再弹窗）'
      )
    } finally {
      fs.rmSync(helperTmp, { force: true })
    }
  } finally {
    if (fs.existsSync(tmp)) fs.rmSync(tmp, { force: true })
  }
}

/** 实时环境同步：环境文件与 ~/.zshrc 钩子区块的标记（live-env） */
const LIVE_ENV_FILE = '.maozi-cloud-develop-admin-env.sh'
const LIVE_BEGIN = '# >>> maozi-cloud-develop-admin (live-env) >>>'
const LIVE_END = '# <<< maozi-cloud-develop-admin (live-env) <<<'

/**
 * 生成实时环境文件：启用的变量 export、禁用的 unset。
 * ~/.zshrc 的 preexec/precmd 钩子按文件内容 md5 检测变化并 source 它，已打开的终端无需重开即可生效。
 * 同名变量按"后加载覆盖先加载"（Map 保序覆盖）：与 shell 逐文件 source 的覆盖语义一致，
 * 例如 .zshrc 末尾追加的 export PATH="$JAVA_HOME/bin:$PATH" 必须覆盖前面的同名 PATH 行。
 * 行序：值含变量引用（$...）的行排在纯值行之后——引用行必须在其依赖定义之后执行，
 * 否则 source 时 PATH="$JAVA_HOME/bin:$PATH" 会以旧 JAVA_HOME 展开，在线终端拿到旧 JDK。
 * 文件内容全量生成、可随时重建，不做 .maozi.bak 备份（避免已删除的密钥明文残留）。
 */
function syncLiveEnv(): void {
  const map = new Map<string, string>()
  for (const v of darwinPlatform.readEnvVars()) {
    map.set(v.key, v.enabled === false ? `unset ${v.key} 2>/dev/null || true` : serializeEnvLine(v.key, v.value))
  }
  const plain: string[] = []
  const refs: string[] = []
  for (const line of map.values()) (line.includes('$') ? refs : plain).push(line)
  const lines = ['# 由 maozi-cloud-develop-admin 维护，请勿手工编辑', ...plain, ...refs]
  backupAndWrite(path.join(homedir(), LIVE_ENV_FILE), `${lines.join('\n')}\n`, { backup: false })
}

/** 在 ~/.zshrc 安装实时同步钩子（幂等；同时挂 preexec/precmd：改动后的第一条命令即用新环境） */
function installLiveHook(): void {
  const zshrc = envFilePath('zshrc')
  const lines = fs.existsSync(zshrc) ? fs.readFileSync(zshrc, 'utf8').split('\n') : []
  const beginIdx = lines.findIndex((l) => l.trim() === LIVE_BEGIN)
  if (beginIdx >= 0) {
    const endIdx = lines.findIndex((l, i) => i > beginIdx && l.trim() === LIVE_END)
    if (endIdx < 0) return // 区块残缺，不动它
    const block = lines.slice(beginIdx, endIdx)
    // 已是新版（含 preexec + md5 检测）则跳过；旧版区块移除后按新版重装
    if (block.some((l) => l.includes('preexec_functions')) && block.some((l) => l.includes('md5'))) return
    lines.splice(beginIdx, endIdx - beginIdx + 1)
  }
  while (lines.length > 0 && lines[lines.length - 1].trim() === '') lines.pop()
  lines.push(
    '',
    LIVE_BEGIN,
    '_maozi_env_sync() {',
    '  local f="$HOME/.maozi-cloud-develop-admin-env.sh"',
    '  [[ -r "$f" ]] || return',
    '  local m; m=$(md5 -q "$f" 2>/dev/null || echo 0)',
    '  [[ "$m" == "${_MAOZI_ENV_M:-}" ]] && return',
    '  _MAOZI_ENV_M="$m"',
    '  source "$f"',
    '}',
    '# preexec: 命令提交时（执行前）同步——改动后的第一条命令即生效',
    '[[ " ${preexec_functions[*]:-} " == *" _maozi_env_sync "* ]] || preexec_functions+=(_maozi_env_sync)',
    '# precmd: 出提示符前同步——覆盖空回车等场景',
    '[[ " ${precmd_functions[*]:-} " == *" _maozi_env_sync "* ]] || precmd_functions+=(_maozi_env_sync)',
    LIVE_END
  )
  backupAndWrite(zshrc, `${lines.join('\n')}\n`, { backup: false })
}

/** 将新行写入托管区块（不存在时在文件末尾创建） */
function appendManaged(lines: string[], newLine: string): void {
  const beginIdx = lines.findIndex((l) => l.trim() === MANAGED_BEGIN)
  if (beginIdx < 0) {
    if (lines.length > 0) lines.push('')
    lines.push(MANAGED_BEGIN, newLine, MANAGED_END)
    return
  }
  const endIdx = lines.findIndex((l) => l.trim() === MANAGED_END)
  lines.splice(endIdx >= 0 ? endIdx : lines.length, 0, newLine)
}

/** 拖拽排序：命中 orderedKeys 的 export 行按新顺序回填到原位置槽位，其余行不动 */
function reorderExportLines(lines: string[], orderedKeys: string[]): void {
  const keySet = new Set(orderedKeys)
  const keyOf: (string | null)[] = lines.map((l) => {
    const m = l.match(EXPORT_RE)
    return m && keySet.has(m[1]) ? m[1] : null
  })
  const slots: number[] = []
  keyOf.forEach((k, i) => {
    if (k !== null) slots.push(i)
  })
  const lineByKey = new Map<string, string>()
  keyOf.forEach((k, i) => {
    if (k !== null && !lineByKey.has(k)) lineByKey.set(k, lines[i])
  })
  orderedKeys.forEach((key, k) => {
    const slot = slots[k]
    const line = lineByKey.get(key)
    if (slot === undefined || line === undefined) return
    lines[slot] = line
  })
}

/** 托管区块内已无变量时移除区块标记 */
function cleanupEmptyManagedBlock(lines: string[]): string[] {
  const beginIdx = lines.findIndex((l) => l.trim() === MANAGED_BEGIN)
  const endIdx = lines.findIndex((l) => l.trim() === MANAGED_END)
  if (beginIdx < 0 || endIdx < 0 || endIdx <= beginIdx) return lines
  const inner = lines.slice(beginIdx + 1, endIdx)
  // 注释掉的 export 行（禁用态）同样是区块内容，不能让区块被判定为空而整体删除
  const hasExport = inner.some((l) => EXPORT_RE.test(l) || COMMENTED_EXPORT_RE.test(l))
  if (hasExport) return lines
  const result = [...lines]
  result.splice(beginIdx, endIdx - beginIdx + 1)
  // 移除区块后可能残留的连续空行
  while (result.length > 0 && result[result.length - 1].trim() === '') result.pop()
  return result
}

/** 单引号包裹 shell 参数（路径由本程序生成，仅包含安全字符） */
function q(s: string): string {
  return `'${s}'`
}

/**
 * 通过 osascript 弹出系统管理员授权框执行特权命令
 * 命令内容只包含本程序生成的安全路径，不拼接用户输入的任意 shell
 */
async function runAsAdmin(shellCmd: string, prompt: string): Promise<void> {
  // 转义为 AppleScript 双引号字符串
  const escaped = shellCmd.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  const script = `do shell script "${escaped}" with administrator privileges with prompt "${prompt}"`
  try {
    await exec('osascript', ['-e', script])
  } catch (err) {
    const e = err as { stderr?: string; message?: string }
    // -128 为 AppleScript 用户取消的错误码
    if (e.stderr?.includes('User canceled') || e.message?.includes('User canceled') ||
        e.message?.includes('(-128)')) {
      throw new Error('已取消授权')
    }
    throw new Error(`执行失败: ${e.stderr || e.message || '未知错误'}`)
  }
}
