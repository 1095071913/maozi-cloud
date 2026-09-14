<template>
  <div>
    <div class="ct-hero">
      <div class="ct-deco ct-deco-1"></div>
      <div class="ct-deco ct-deco-2"></div>
      <div class="ct-hero-mark">🧰</div>
      <div class="ct-hero-left">
        <div class="ct-hero-name">🧰 编程工具</div>
        <div class="ct-hero-sub">开发常用小工具，数据均在本机处理</div>
        <div class="ct-hero-chips">
          <span class="ct-chip">🧩 JSON 格式化 · 支持注释</span>
          <span class="ct-chip">🕐 时间戳双向转换</span>
        </div>
      </div>
    </div>

    <div class="tools-card">
      <div class="seg" role="tablist" aria-label="工具切换">
        <div class="seg-thumb" :class="{ right: activeTab === 'ts' }"></div>
        <button type="button" role="tab" class="seg-item" :class="{ active: activeTab === 'json' }" :aria-selected="activeTab === 'json'" @click="activeTab = 'json'">
          <span class="seg-ico blue"><span class="tt-emoji">🧩</span></span>
          <span>JSON 格式化</span>
        </button>
        <button type="button" role="tab" class="seg-item" :class="{ active: activeTab === 'ts' }" :aria-selected="activeTab === 'ts'" @click="activeTab = 'ts'">
          <span class="seg-ico amber"><span class="tt-emoji tt-emoji-clock">🕐</span></span>
          <span>时间戳转换</span>
        </button>
      </div>

      <!-- ===== JSON 格式化编辑器 ===== -->
      <div v-show="activeTab === 'json'" class="pane-box">
          <div class="json-toolbar">
            <el-button type="primary" @click="formatJson">格式化</el-button>
            <el-select v-model="indent" style="width: 128px">
              <el-option label="缩进 2 空格" value="2" />
              <el-option label="缩进 4 空格" value="4" />
              <el-option label="缩进 Tab" value="tab" />
            </el-select>
            <el-button @click="copyMinified">压缩复制</el-button>
            <el-button @click="copyEscaped">压缩转义并复制</el-button>
            <el-button @click="copyJson">复制</el-button>
            <el-button @click="clearJson">清空</el-button>
            <el-button text type="primary" @click="loadSample">示例</el-button>
          </div>

          <div class="json-editor">
            <pre ref="highlightRef" class="json-highlight" aria-hidden="true"><code v-html="highlightedJson"></code></pre>
            <el-input
              ref="jsonInputRef"
              v-model="jsonText"
              type="textarea"
              :rows="16"
              spellcheck="false"
              class="json-area"
              placeholder="粘贴或输入 JSON，自动校验；手写支持 { [ &quot; 自动配对、回车缩进"
              @keydown="onJsonKeydown"
            />
          </div>

          <div class="json-status" :class="jsonText.trim() ? (jsonError ? 'bad' : 'ok') : 'idle'">
            <span class="status-dot"></span>
            <span class="status-text">
              <template v-if="!jsonText.trim()">等待输入…</template>
              <template v-else-if="jsonError">
                ✗ {{ jsonError.msg }}<template v-if="jsonError.line">（第 {{ jsonError.line }} 行 第 {{ jsonError.col }} 列）</template>
              </template>
              <template v-else>✓ 有效 JSON{{ hasComments ? '（含注释）' : '' }}</template>
            </span>
            <span v-if="jsonText.trim()" class="status-chips">
              <span class="st-chip">{{ jsonText.length }} 字符</span>
              <span class="st-chip">{{ jsonLineCount }} 行</span>
            </span>
          </div>
      </div>

      <!-- ===== 时间戳转换 ===== -->
      <div v-show="activeTab === 'ts'" class="pane-box">
          <!-- 当前时间 -->
          <div class="now-bar">
            <div class="now-deco now-deco-1"></div>
            <div class="now-deco now-deco-2"></div>
            <div class="now-left">
              <div class="now-time mono-cell">{{ fmtDateTime(now) }}</div>
              <div class="now-meta">{{ fmtDay(now) }} · 星期{{ weekdayOf(now) }} · {{ tzLabel }} · 实时</div>
            </div>
            <div class="now-ts">
              <div class="ts-chip" title="点击复制秒级时间戳" @click="copyText(String(secOf(now)))">
                <span class="chip-label">秒级</span>
                <span class="mono-cell">{{ secOf(now) }}</span>
              </div>
              <div class="ts-chip" title="点击复制毫秒级时间戳" @click="copyText(String(now.getTime()))">
                <span class="chip-label">毫秒</span>
                <span class="mono-cell">{{ now.getTime() }}</span>
              </div>
            </div>
          </div>

          <div class="ts-grid">
            <!-- 时间戳 → 时间 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico to-time">🕒</span>
                <span class="ts-card-title">时间戳 → 时间</span>
              </div>
              <div class="ts-input-row">
                <el-input
                  v-model="tsInput"
                  placeholder="输入 10 位秒级 / 13 位毫秒级时间戳"
                  class="mono-cell"
                  clearable
                />
                <el-button text type="primary" @click="tsInput = String(secOf(now))">此刻</el-button>
              </div>

              <template v-if="tsDate">
                <div class="ts-row" title="点击复制" @click="copyText(fmtDateTime(tsDate))">
                  <span class="ts-row-label">本地时间</span>
                  <span class="ts-row-value mono-cell">{{ fmtDateTime(tsDate) }}</span>
                </div>
                <div class="ts-row" title="点击复制" @click="copyText(tsDate.toISOString())">
                  <span class="ts-row-label">ISO 8601</span>
                  <span class="ts-row-value mono-cell">{{ tsDate.toISOString() }}</span>
                </div>
                <div class="ts-row" title="点击复制" @click="copyText(fmtUtc(tsDate))">
                  <span class="ts-row-label">UTC 时间</span>
                  <span class="ts-row-value mono-cell">{{ fmtUtc(tsDate) }}</span>
                </div>
                <div class="ts-row-inline">
                  <span>星期{{ weekdayOf(tsDate) }}</span>
                  <span class="ts-rel">{{ relativeTo(tsDate, now) }}</span>
                </div>
              </template>
              <div v-else-if="tsInput.trim()" class="ts-hint bad-hint">请输入纯数字时间戳</div>
              <div v-else class="ts-hint">输入后自动识别秒级 / 毫秒级</div>
            </div>

            <!-- 时间 → 时间戳 -->
            <div class="ts-card">
              <div class="ts-card-head">
                <span class="ts-card-ico to-ts">📅</span>
                <span class="ts-card-title">时间 → 时间戳</span>
              </div>
              <el-date-picker
                v-model="pickDate"
                type="datetime"
                placeholder="选择日期时间"
                style="width: 100%"
                :shortcuts="dateShortcuts"
              />

              <template v-if="pickDate">
                <div class="ts-row" title="点击复制秒级时间戳" @click="copyText(String(secOf(pickDate)))">
                  <span class="ts-row-label">秒级时间戳</span>
                  <span class="ts-row-value mono-cell">{{ secOf(pickDate) }}</span>
                </div>
                <div class="ts-row" title="点击复制毫秒级时间戳" @click="copyText(String(pickDate.getTime()))">
                  <span class="ts-row-label">毫秒级时间戳</span>
                  <span class="ts-row-value mono-cell">{{ pickDate.getTime() }}</span>
                </div>
                <div class="ts-row" title="点击复制" @click="copyText(fmtDateTime(pickDate))">
                  <span class="ts-row-label">本地时间</span>
                  <span class="ts-row-value mono-cell">{{ fmtDateTime(pickDate) }}</span>
                </div>
              </template>
              <div v-else class="ts-hint" style="margin-top: 12px">请选择日期时间</div>
            </div>
          </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const activeTab = ref('json')

/* ===================== JSON 格式化编辑器 ===================== */
const jsonText = ref('')
const indent = ref<'2' | '4' | 'tab'>('2')

interface JsonError {
  msg: string
  line?: number
  col?: number
}

/** 去除行注释与块注释（跳过字符串字面量，保留换行以维持行号），用于校验与压缩 */
function stripComments(text: string): string {
  let out = ''
  let i = 0
  const n = text.length
  while (i < n) {
    const c = text[i]
    if (c === '"') {
      out += c
      i++
      while (i < n) {
        out += text[i]
        if (text[i] === '\\' && i + 1 < n) {
          out += text[i + 1]
          i += 2
          continue
        }
        if (text[i] === '"') {
          i++
          break
        }
        i++
      }
      continue
    }
    if (c === '/' && text[i + 1] === '/') {
      while (i < n && text[i] !== '\n') i++
      continue
    }
    if (c === '/' && text[i + 1] === '*') {
      i += 2
      while (i < n && !(text[i] === '*' && text[i + 1] === '/')) {
        if (text[i] === '\n') out += '\n'
        i++
      }
      i += 2
      continue
    }
    out += c
    i++
  }
  return out
}

const jsonError = computed<JsonError | null>(() => {
  const text = jsonText.value
  if (!text.trim()) return null
  const stripped = stripComments(text)
  try {
    JSON.parse(stripped)
    return null
  } catch (err) {
    const msg = (err as Error).message
    const posMatch = msg.match(/position (\d+)/)
    if (posMatch) {
      const pos = Number(posMatch[1])
      const before = stripped.slice(0, pos)
      return { msg, line: before.split('\n').length, col: pos - before.lastIndexOf('\n') }
    }
    return { msg }
  }
})

const hasComments = computed(() => {
  const text = jsonText.value
  return text.trim() ? stripComments(text) !== text : false
})

const jsonLineCount = computed(() => jsonText.value.split('\n').length)

const jsonStats = computed(
  () => `${jsonText.value.length} 字符 · ${jsonText.value.split('\n').length} 行`
)

/* ===== 语法高亮：注释绿 / key 红 / 字符串值蓝 / 数值橙 / true·false·null 紫 ===== */
const jsonInputRef = ref()
const highlightRef = ref<HTMLElement>()

const highlightedJson = computed(() => {
  const text = jsonText.value
  const n = text.length
  let out = ''
  let i = 0
  const escHtml = (s: string): string =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const wrap = (cls: string, s: string): string => `<span class="tok-${cls}">${escHtml(s)}</span>`

  while (i < n) {
    const c = text[i]

    if (c === '/' && (text[i + 1] === '/' || text[i + 1] === '*')) {
      const isLine = text[i + 1] === '/'
      let end: number
      if (isLine) {
        end = text.indexOf('\n', i)
        if (end < 0) end = n
      } else {
        end = text.indexOf('*/', i + 2)
        end = end < 0 ? n : end + 2
      }
      out += wrap('comment', text.slice(i, end))
      i = end
      continue
    }

    if (c === '"') {
      let j = i + 1
      while (j < n) {
        if (text[j] === '\\') {
          j += 2
          continue
        }
        if (text[j] === '"') {
          j++
          break
        }
        j++
      }
      // 字符串后（跳过空白）跟着冒号即为 key
      let k = j
      while (k < n && /\s/.test(text[k])) k++
      out += wrap(text[k] === ':' ? 'key' : 'string', text.slice(i, j))
      i = j
      continue
    }

    if (/[0-9]/.test(c) || (c === '-' && /[0-9]/.test(text[i + 1] ?? ''))) {
      let j = i + 1
      while (j < n && /[0-9.eE]/.test(text[j])) j++
      out += wrap('number', text.slice(i, j))
      i = j
      continue
    }

    if (/[a-z]/.test(c)) {
      let j = i
      while (j < n && /[a-z]/.test(text[j])) j++
      const word = text.slice(i, j)
      out +=
        word === 'true' || word === 'false' || word === 'null' ? wrap('literal', word) : escHtml(word)
      i = j
      continue
    }

    out += escHtml(c)
    i++
  }
  // 补一个换行，保证 textarea 末行为空时两层高度一致
  return `${out}\n`
})

/** textarea 滚动时同步高亮层（在挂载后绑定到原生 textarea） */
onMounted(() => {
  const inst = jsonInputRef.value as
    | { textarea?: HTMLTextAreaElement; $el?: HTMLElement }
    | undefined
  const ta = inst?.textarea ?? inst?.$el?.querySelector('textarea')
  ta?.addEventListener('scroll', () => {
    if (highlightRef.value && ta) {
      highlightRef.value.scrollTop = ta.scrollTop
      highlightRef.value.scrollLeft = ta.scrollLeft
    }
  })
})

/**
 * 重新排版 JSONC（保留注释）：括号层级换行缩进、冒号后补空格、逗号后换行；
 * 行尾注释与内容隔两个空格，块注释内部行跟随缩进
 */
function prettyJsonc(text: string, unit: string): string {
  let out = ''
  let depth = 0
  let pendingBreak = false
  let i = 0
  const n = text.length
  const indent = (): string => unit.repeat(Math.max(0, depth))
  const place = (): void => {
    if (pendingBreak) {
      out += `\n${indent()}`
      pendingBreak = false
    }
  }

  while (i < n) {
    const c = text[i]

    if (c === '\n' || c === '\r' || c === ' ' || c === '\t') {
      i++
      continue
    }

    if (c === '{' || c === '[') {
      const close = c === '{' ? '}' : ']'
      let j = i + 1
      while (j < n && (text[j] === ' ' || text[j] === '\t' || text[j] === '\n' || text[j] === '\r')) j++
      place()
      if (text[j] === close) {
        out += c + close
        i = j + 1
        continue
      }
      out += c
      depth++
      pendingBreak = true
      i++
      continue
    }

    if (c === '}' || c === ']') {
      depth--
      pendingBreak = true
      place()
      out += c
      i++
      continue
    }

    if (c === ',') {
      out += ','
      pendingBreak = true
      i++
      continue
    }

    if (c === ':') {
      out += ': '
      i++
      continue
    }

    if (c === '/' && (text[i + 1] === '/' || text[i + 1] === '*')) {
      const isLine = text[i + 1] === '/'
      let end: number
      if (isLine) {
        end = text.indexOf('\n', i)
        if (end < 0) end = n
      } else {
        end = text.indexOf('*/', i + 2)
        end = end < 0 ? n : end + 2
      }
      let comment = text.slice(i, end)
      comment = isLine ? comment.replace(/\r$/, '') : comment.split('\n').join(`\n${indent()}`)
      if (pendingBreak) {
        out += `\n${indent()}${comment}`
      } else {
        out += `  ${comment}`
      }
      pendingBreak = true
      i = end
      continue
    }

    place()
    if (c === '"') {
      let j = i + 1
      while (j < n) {
        if (text[j] === '\\') {
          j += 2
          continue
        }
        if (text[j] === '"') {
          j++
          break
        }
        j++
      }
      out += text.slice(i, j)
      i = j
      continue
    }
    let j = i
    while (j < n && !'{}[]:,\n\r\t /'.includes(text[j])) j++
    out += text.slice(i, j)
    i = j
  }

  return out
}

function formatJson(): void {
  if (!requireValid()) return
  jsonText.value = prettyJsonc(jsonText.value, indentUnitStr())
}

/** 压缩结果（注释会被去除，压缩后的 JSON 无法携带注释） */
function minifiedJson(): string {
  return JSON.stringify(JSON.parse(stripComments(jsonText.value)))
}

async function copyMinified(): Promise<void> {
  if (!requireValid()) return
  await copyText(minifiedJson(), '已复制压缩 JSON（注释已去除）')
}

/** 压缩并整体转义为字符串字面量（引号、反斜杠、换行转义），可直接粘贴进代码 */
async function copyEscaped(): Promise<void> {
  if (!requireValid()) return
  await copyText(JSON.stringify(minifiedJson()), '已复制压缩转义 JSON')
}

function requireValid(): boolean {
  if (!jsonText.value.trim()) {
    ElMessage.warning('请先输入 JSON 内容')
    return false
  }
  if (jsonError.value) {
    ElMessage.error(`JSON 无效：${jsonError.value.msg}`)
    return false
  }
  return true
}

async function copyJson(): Promise<void> {
  if (!requireValid()) return
  await copyText(jsonText.value)
}

function clearJson(): void {
  jsonText.value = ''
}

function loadSample(): void {
  jsonText.value = `{
  // 项目信息（支持注释，格式化后注释保留）
  "name": "maozi-cloud",
  "version": "1.0.0",
  "features": ["微服务", "分布式", "一站式解决方案"],
  "author": { "name": "maozi", "url": "https://github.com/1095071913/maozi-cloud" },
  "openSource": true,
  "stars": 1024 // 星标数
}`
}

/** ===== 编辑器增强：配对/缩进（execCommand 插入以保留撤销栈并同步 v-model） ===== */
function insertAtCursor(ta: HTMLTextAreaElement, text: string): void {
  if (!document.execCommand('insertText', false, text)) {
    const start = ta.selectionStart
    ta.value = ta.value.slice(0, start) + text + ta.value.slice(ta.selectionEnd)
    ta.dispatchEvent(new Event('input', { bubbles: true }))
  }
}

function indentUnitStr(): string {
  return indent.value === 'tab' ? '\t' : ' '.repeat(Number(indent.value))
}

/** 从 pos 向前找最内层未闭合的开括号（'{' 或 '['），跳过字符串字面量（处理转义引号） */
function enclosingOpener(value: string, pos: number): string {
  let depth = 0
  for (let i = pos - 1; i >= 0; i--) {
    const c = value[i]
    if (c === '"') {
      // 统计引号前的连续反斜杠，奇数为转义引号，不是字符串边界
      let bs = 0
      let j = i - 1
      while (j >= 0 && value[j] === '\\') {
        bs++
        j--
      }
      if (bs % 2 === 1) continue
      // 未转义引号：整段跳过（向前找到它的配对开引号）
      let k = i - 1
      while (k >= 0) {
        let bs2 = 0
        let m = k - 1
        while (m >= 0 && value[m] === '\\') {
          bs2++
          m--
        }
        if (value[k] === '"' && bs2 % 2 === 0) break
        k--
      }
      i = k
      continue
    }
    if (c === '}' || c === ']') {
      depth++
      continue
    }
    if (c === '{' || c === '[') {
      if (depth === 0) return c
      depth--
    }
  }
  return ''
}

function onJsonKeydown(e: KeyboardEvent): void {
  // 中文输入法组合中、或按住修饰键的快捷键不做拦截
  if (e.isComposing || e.ctrlKey || e.metaKey || e.altKey) return
  const ta = e.target as HTMLTextAreaElement
  if (!ta || ta.tagName !== 'TEXTAREA') return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const value = ta.value
  const hasSel = start !== end

  // 1. 跨越闭合符：光标紧贴 } ] " 且无选中时，再输入同类符号直接跳过而不是重复插入
  //    （" 既是配对符又是闭合符，此判断必须先于自动配对，否则会被配对分支拦截）
  if (!hasSel && (e.key === '}' || e.key === ']' || e.key === '"') && value[start] === e.key) {
    e.preventDefault()
    ta.setSelectionRange(start + 1, start + 1)
    return
  }

  // 2. 自动配对：输入 { [ " 生成闭合符，光标落在中间；有选中内容时改为包裹选区
  const closeOf: Record<string, string> = { '{': '}', '[': ']', '"': '"' }
  if (closeOf[e.key]) {
    e.preventDefault()
    const sel = value.slice(start, end)
    insertAtCursor(ta, e.key + sel + closeOf[e.key])
    ta.setSelectionRange(start + 1, start + 1 + sel.length)
    return
  }

  // 3. 冒号跳出引号：光标紧贴闭合引号时按 :，先跳出引号再补 ': '（配合 key 自动引号）
  if (e.key === ':' && !hasSel && value[start] === '"') {
    e.preventDefault()
    ta.setSelectionRange(start + 1, start + 1)
    insertAtCursor(ta, ': ')
    return
  }

  // 4. key 自动加引号：在对象成员起始位（前面是 { 或 , 且所在容器是对象）直接输入裸字符时，
  //    自动包上双引号，光标落在引号内继续输入；数组中不拦截（可能是 true/false/null）
  if (
    !hasSel &&
    e.key.length === 1 &&
    /[0-9A-Za-z_$\u4e00-\u9fa5]/.test(e.key) &&
    value[start] !== '"'
  ) {
    let p = start - 1
    while (p >= 0 && /\s/.test(value[p])) p--
    const prevCh = p >= 0 ? value[p] : ''
    if ((prevCh === '{' || prevCh === ',') && enclosingOpener(value, start) === '{') {
      e.preventDefault()
      insertAtCursor(ta, `"${e.key}"`)
      ta.setSelectionRange(start + 2, start + 2)
      return
    }
  }

  const unit = indentUnitStr()

  // 5. Tab：插入缩进（跟随上方缩进设置），而不是把焦点移出编辑框
  if (e.key === 'Tab') {
    e.preventDefault()
    insertAtCursor(ta, unit)
    return
  }

  // 6. 智能退格：空引号对整体删除；行首缩进按单位退格（2/4 空格一次删一个单位），
  //    不足一个单位则删到行首；Tab 缩进用浏览器默认（一次删一个 Tab）
  if (e.key === 'Backspace' && !hasSel) {
    // 光标在 "" 中间：一次删掉一对引号
    if (value[start - 1] === '"' && value[start] === '"') {
      e.preventDefault()
      ta.setSelectionRange(start - 1, start + 1)
      if (!document.execCommand('delete')) {
        ta.value = value.slice(0, start - 1) + value.slice(start + 1)
        ta.dispatchEvent(new Event('input', { bubbles: true }))
        ta.setSelectionRange(start - 1, start - 1)
      }
      return
    }
    const lineStart = value.lastIndexOf('\n', start - 1) + 1
    const before = value.slice(lineStart, start)
    if (before.length > 0 && /^[ \t]+$/.test(before) && indentUnitStr() !== '\t') {
      const delLen = Math.min(indentUnitStr().length, before.length)
      e.preventDefault()
      ta.setSelectionRange(start - delLen, start)
      if (!document.execCommand('delete')) {
        ta.value = value.slice(0, start - delLen) + value.slice(start)
        ta.dispatchEvent(new Event('input', { bubbles: true }))
        ta.setSelectionRange(start - delLen, start - delLen)
      }
      return
    }
  }

  // 7. 回车自动缩进：配对之间三行展开，{ [ 之后加一层缩进，其余继承当前行缩进
  if (e.key === 'Enter' && !hasSel) {
    const before = value.slice(0, start)
    const lineIndent = (before.slice(before.lastIndexOf('\n') + 1).match(/^[ \t]*/) ?? [''])[0]
    const prev = before.slice(-1)
    const next = value[start] ?? ''
    e.preventDefault()

    // 末项回车自动补逗号：光标在值末尾、其后（跳过空白）就是闭合括号时，
    // 先补 ',' 再换行；闭合括号与光标同行时顺带推到独立行。
    // 行内含 // 注释时跳过（逗号会被注释吞掉导致语法错误）
    let p = start - 1
    while (p >= 0 && /\s/.test(value[p])) p--
    const prevCh = p >= 0 ? value[p] : ''
    let q = start
    while (q < value.length && /\s/.test(value[q])) q++
    const nextCh = value[q] ?? ''
    const lineText = before.slice(before.lastIndexOf('\n') + 1)
    const afterValue = prevCh !== '' && !'{[,:'.includes(prevCh)
    if (afterValue && (nextCh === '}' || nextCh === ']') && !lineText.includes('//')) {
      if (/[\n\r]/.test(value.slice(start, q))) {
        // 闭合括号已在独立行：补逗号后光标与同级条目对齐（当前行缩进，不加一层）
        insertAtCursor(ta, `,\n${lineIndent}`)
      } else {
        // 闭合括号与光标同行：三行展开（值行、光标行、闭合行），光标行深一层
        insertAtCursor(ta, `,\n${lineIndent}${unit}\n${lineIndent}`)
        const cursor = start + 2 + lineIndent.length + unit.length
        ta.setSelectionRange(cursor, cursor)
      }
      return
    }

    if ((prev === '{' && next === '}') || (prev === '[' && next === ']')) {
      insertAtCursor(ta, `\n${lineIndent}${unit}\n${lineIndent}`)
      const cursor = start + 1 + lineIndent.length + unit.length
      ta.setSelectionRange(cursor, cursor)
    } else if (prev === '{' || prev === '[') {
      insertAtCursor(ta, `\n${lineIndent}${unit}`)
    } else {
      insertAtCursor(ta, `\n${lineIndent}`)
    }
  }
}

/* ===================== 时间戳转换 ===================== */
const now = ref(new Date())
let timer: ReturnType<typeof setInterval> | null = null

const tzLabel = computed(() => {
  const off = -new Date().getTimezoneOffset() / 60
  return `UTC${off >= 0 ? '+' : ''}${off}`
})

const tsInput = ref('')
const pickDate = ref<Date | null>(new Date())

const dateShortcuts = [
  { text: '此刻', value: () => new Date() },
  {
    text: '今天 00:00:00',
    value: () => {
      const d = new Date()
      d.setHours(0, 0, 0, 0)
      return d
    }
  },
  {
    text: '昨天 00:00:00',
    value: () => {
      const d = new Date()
      d.setDate(d.getDate() - 1)
      d.setHours(0, 0, 0, 0)
      return d
    }
  }
]

/** 纯数字输入自动识别：13 位以上按毫秒（超长截到毫秒），否则按秒 */
const tsDate = computed<Date | null>(() => {
  const raw = tsInput.value.trim()
  if (!/^\d{1,16}$/.test(raw)) return null
  const num = raw.length > 13 ? Number(raw.slice(0, 13)) : Number(raw)
  const ms = raw.length >= 13 ? num : num * 1000
  const d = new Date(ms)
  return isNaN(d.getTime()) ? null : d
})

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function fmtDateTime(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function fmtDay(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function fmtUtc(d: Date): string {
  return `${d.toISOString().replace('T', ' ').slice(0, 19)} UTC`
}

function weekdayOf(d: Date): string {
  return '日一二三四五六'[d.getDay()]
}

function secOf(d: Date): number {
  return Math.floor(d.getTime() / 1000)
}

function relativeTo(d: Date, base: Date): string {
  const diff = (base.getTime() - d.getTime()) / 1000
  const abs = Math.abs(diff)
  const suffix = diff >= 0 ? '前' : '后'
  if (abs < 60) return `${Math.round(abs)} 秒${suffix}`
  if (abs < 3600) return `${Math.round(abs / 60)} 分钟${suffix}`
  if (abs < 86400) return `${Math.round(abs / 3600)} 小时${suffix}`
  if (abs < 86400 * 30) return `${Math.round(abs / 86400)} 天${suffix}`
  if (abs < 86400 * 365) return `${Math.round(abs / 86400 / 30)} 个月${suffix}`
  return `${(abs / 86400 / 365).toFixed(1)} 年${suffix}`
}

async function copyText(text: string, msg = '已复制到剪贴板'): Promise<void> {
  await api.util.copy(text)
  ElMessage.success(msg)
}

onMounted(() => {
  timer = setInterval(() => (now.value = new Date()), 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ===== Hero ===== */
.ct-hero {
  -webkit-app-region: drag;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 32px;
  border-radius: 18px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
  margin-bottom: 14px;
}

.ct-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.ct-deco-1 {
  width: 300px;
  height: 300px;
  right: -70px;
  top: -160px;
}

.ct-deco-2 {
  width: 190px;
  height: 190px;
  right: 150px;
  bottom: -120px;
  border-color: rgba(255, 255, 255, 0.07);
}

.ct-hero-left {
  position: relative;
  min-width: 0;
}

.ct-hero-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.ct-hero-sub {
  margin-top: 7px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.ct-hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.ct-hero-mark {
  position: absolute;
  right: 44px;
  top: 50%;
  transform: translateY(-50%) rotate(-8deg);
  font-size: 86px;
  opacity: 0.14;
  filter: grayscale(0.2);
  pointer-events: none;
  user-select: none;
}

.ct-chip {
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(6px);
}

.tools-card {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 16px;
  padding: 6px 22px 20px;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.05);
}

/* 切换 Tab 时内容轻入 */
.pane-box {
  animation: ct-pane-in 0.28s ease both;
}

@keyframes ct-pane-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 滑块分段导航 ===== */
.seg {
  position: relative;
  display: flex;
  width: fit-content;
  margin-bottom: 22px;
  padding: 4px;
  border-radius: 14px;
  background: linear-gradient(180deg, #e8edf4, #eff3f8);
  box-shadow: inset 0 1px 3px rgba(15, 23, 42, 0.08), inset 0 0 0 1px rgba(15, 23, 42, 0.04);
}

.seg-thumb {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc(50% - 4px);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.14), 0 1px 2px rgba(15, 23, 42, 0.06),
    inset 0 0 0 1px rgba(15, 23, 42, 0.04);
  transition: transform 0.42s cubic-bezier(0.3, 1.25, 0.45, 1);
}

.seg-thumb.right {
  transform: translateX(100%);
}

.seg-item {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 200px;
  height: 38px;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: none;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #64748b;
  cursor: pointer;
  outline: none;
  transition: color 0.25s ease;
}

.seg-item:hover {
  color: #334155;
}

.seg-item.active {
  color: #0f172a;
}

.seg-item:focus-visible {
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.22);
}

.seg-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  transition: transform 0.32s cubic-bezier(0.3, 1.25, 0.45, 1), box-shadow 0.3s ease, background 0.3s ease;
}

.seg-ico.blue {
  background: linear-gradient(135deg, #e3edfd, #d3e3fb);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.16);
}

.seg-ico.amber {
  background: linear-gradient(135deg, #fdf3d9, #fbecc2);
  box-shadow: inset 0 0 0 1px rgba(217, 119, 6, 0.18);
}

.seg-item.active .seg-ico {
  transform: translateY(-1px) scale(1.06);
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.1), inset 0 0 0 1px rgba(15, 23, 42, 0.05);
}

.seg-item.active .seg-ico.blue {
  background: linear-gradient(135deg, #eaf2fe, #dbe9fd);
}

.seg-item.active .seg-ico.amber {
  background: linear-gradient(135deg, #fef6e3, #fcf0cd);
}

.tt-emoji {
  font-size: 13px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 🕐 字形在 emoji 字体里偏上偏大，单独做光学对齐 */
.tt-emoji-clock {
  font-size: 12.5px;
  transform: translateY(0.5px);
}

/* ===== JSON ===== */
.json-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 12px;
}

.json-toolbar :deep(.el-button) {
  border-radius: 9px;
  font-weight: 600;
}

.json-toolbar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #2563eb, #1e3fae);
  border: none;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.32);
}

.json-toolbar :deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(37, 99, 235, 0.42);
}

.json-toolbar :deep(.el-select .el-select__wrapper) {
  border-radius: 9px;
}

/* 高亮编辑器：着色 pre 在底层，透明 textarea 叠加在上层，两层度量必须一致 */
.json-editor {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.json-editor:focus-within {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.25);
}

.json-highlight {
  position: absolute;
  inset: 0;
  margin: 0;
  padding: 10px 12px;
  overflow: hidden auto;
  pointer-events: none;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  word-break: normal;
  tab-size: 4;
  color: #1f2d3d;
}

.json-highlight code {
  font: inherit;
}

/* token 配色（span 由 v-html 生成，无 scoped 属性，需 :deep 命中） */
.json-highlight :deep(.tok-comment) {
  color: #16a34a;
}

.json-highlight :deep(.tok-key) {
  color: #dc2626;
}

.json-highlight :deep(.tok-string) {
  color: #2563eb;
}

.json-highlight :deep(.tok-number) {
  color: #ea580c;
}

.json-highlight :deep(.tok-literal) {
  color: #7c3aed;
}

.json-area :deep(.el-textarea__inner) {
  position: relative;
  z-index: 1;
  display: block;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  padding: 10px 12px;
  border: none;
  box-shadow: none;
  background: transparent;
  color: transparent;
  caret-color: #1f2d3d;
  resize: none;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  word-break: normal;
  tab-size: 4;
}

.json-area :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.json-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 7px 12px;
  border-radius: 9px;
  font-size: 12.5px;
}

.json-status .status-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.json-status .status-chips {
  margin-left: auto;
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.json-status .st-chip {
  padding: 1px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(0, 0, 0, 0.05);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: inherit;
  opacity: 0.85;
}

.json-status .status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.json-status.idle {
  background: #f8fafc;
  color: #94a3b8;
}

.json-status.idle .status-dot {
  background: #cbd5e1;
}

.json-status.ok {
  background: #f0fdf4;
  color: #059669;
}

.json-status.ok .status-dot {
  background: #10b981;
}

.json-status.bad {
  background: #fef2f2;
  color: #dc2626;
}

.json-status.bad .status-dot {
  background: #ef4444;
}

/* ===== 时间戳 ===== */
.now-bar {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, #16283c 0%, #1e3fae 62%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.25);
}

.now-deco {
  position: absolute;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.now-deco-1 {
  width: 200px;
  height: 200px;
  right: -50px;
  top: -110px;
}

.now-deco-2 {
  width: 120px;
  height: 120px;
  right: 100px;
  bottom: -80px;
  border-color: rgba(255, 255, 255, 0.07);
}

.now-left {
  position: relative;
}

.now-time {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
}

.now-meta {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.now-ts {
  position: relative;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ts-chip {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 9px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: background 0.2s;
}

.ts-chip:hover {
  background: rgba(255, 255, 255, 0.22);
}

.chip-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
}

.ts-chip .mono-cell {
  font-size: 14px;
}

.ts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 14px;
}

.ts-card {
  background: #f8fafc;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  padding: 14px 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.ts-card:hover {
  transform: translateY(-2px);
  border-color: #c7d7ee;
  box-shadow: 0 8px 22px rgba(16, 24, 40, 0.08);
}

.ts-card-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 12px;
}

.ts-card-ico {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.1);
}

.ts-card-ico.to-time {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
}

.ts-card-ico.to-ts {
  background: linear-gradient(135deg, #ede9fe, #ddd6fe);
}

.ts-card-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.ts-input-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.ts-input-row .el-input {
  flex: 1;
}

.ts-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 9px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.ts-row:hover {
  border-color: #93c5fd;
}

.ts-row-label {
  font-size: 12px;
  color: #8a94a6;
  flex-shrink: 0;
}

.ts-row-value {
  font-size: 13px;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ts-row-inline {
  display: flex;
  justify-content: space-between;
  padding: 2px 12px 0;
  font-size: 12px;
  color: #64748b;
}

.ts-rel {
  color: #2563eb;
}

.ts-hint {
  margin-top: 4px;
  font-size: 12.5px;
  color: #94a3b8;
}

.bad-hint {
  color: #dc2626;
}

@media (max-width: 760px) {
  .ts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
