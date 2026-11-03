<script setup lang="ts">
// 首页 AI 对话工作台：会话管理、流式对话（打字机效果）、图片附件与图片生成
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Brush,
  ChatDotRound as ChatIcon,
  Check,
  Close,
  Delete,
  Edit,
  MagicStick,
  Picture,
  Plus,
  Promotion,
  VideoPause
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import {
  chatStream,
  createConversation,
  generateImage,
  getAfterMessages,
  getChatList,
  getConversationList,
  removeChatMessage,
  removeConversation,
  stopChat,
  updateConversationTitle
} from '@/api/ai'
import { getConfigDropDownList } from '@/api/config'
import { uploadImage } from '@/api/image'
import {
  ChatMessageType,
  type ChatMessageItem,
  type ConfigOptionItem,
  type ConversationItem
} from '@/types/api'

const userStore = useUserStore()

// ============ 对话示例 ============
/** 对话示例的配置类型编码 */
const AI_CHAT_EXAMPLE_TYPE = 'ai_chat_example'
/** 空状态快捷提问（从配置 ai_chat_example 加载） */
const suggestions = ref<string[]>([])

/** 加载对话示例，失败不阻塞对话功能 */
function loadChatExamples() {
  getConfigDropDownList(AI_CHAT_EXAMPLE_TYPE)
    .then((res) => {
      suggestions.value = (res.data || [])
        .map((item) => item.alias || item.name)
        .filter(Boolean)
    })
    .catch(() => {})
}

function handleSuggestion(text: string) {
  input.value = text
}

// ============ 会话列表 ============
const conversations = ref<ConversationItem[]>([])
const total = ref(0)
const current = ref(1)
const conversationLoading = ref(false)

const hasMore = computed(() => conversations.value.length < total.value)

async function loadConversations(page = 1) {
  conversationLoading.value = true
  try {
    const res = await getConversationList({ current: page, size: 100 })
    const pageData = res.data
    total.value = Number(pageData.total) || 0
    current.value = Number(pageData.current) || page
    const items = pageData.data || []
    conversations.value = page === 1 ? items : [...conversations.value, ...items]
  } finally {
    conversationLoading.value = false
  }
}

function handleLoadMore() {
  loadConversations(current.value + 1)
}

/** 新建对话：清空输入区，首次发送时再真正创建会话 */
function handleCreate() {
  stopStreamSilently()
  activeId.value = ''
  messages.value = []
}

// ============ 会话选中 / 更新标题 / 删除 ============
const activeId = ref<string | number>('')

// ============ 系统提示词 ============
/** 系统提示词的配置类型编码 */
const AI_PROMPT_TYPE = 'ai_system_prompt'
const promptOptions = ref<ConfigOptionItem[]>([])
/** 当前选中的提示词配置名称（作为聊天接口 promptConfig 传入），为空表示不使用 */
const activePrompt = ref('')
const promptPopoverVisible = ref(false)

/** 当前选中提示词的展示名（别名为空回退名称） */
const activePromptLabel = computed(
  () => promptOptions.value.find((p) => p.name === activePrompt.value)?.alias || activePrompt.value
)

function selectPrompt(name: string) {
  activePrompt.value = name
  promptPopoverVisible.value = false
}

/** 加载系统提示词选项，失败不阻塞对话功能 */
function loadPromptOptions() {
  getConfigDropDownList(AI_PROMPT_TYPE)
    .then((res) => {
      promptOptions.value = res.data || []
    })
    .catch(() => {})
}

onMounted(async () => {
  loadPromptOptions()
  loadChatExamples()
  await loadConversations(1)
  // 默认选中最近一个会话，直接进入可对话状态
  if (conversations.value.length) {
    activeId.value = conversations.value[0].id
    loadMessages()
  }
})

async function handleSelect(row: ConversationItem) {
  if (row.id === activeId.value) return
  stopStreamSilently()
  activeId.value = row.id
  await loadMessages()
}

async function handleUpdateTitle(row: ConversationItem) {
  const { value } = await ElMessageBox.prompt('请输入会话标题', '更新标题', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: row.title
  }).catch(() => ({ value: '' }) as { value: string })
  const title = (value || '').trim()
  if (!title) return
  await updateConversationTitle(row.id, title)
  row.title = title
  ElMessage.success('更新成功')
}

async function handleRemove(row: ConversationItem) {
  await ElMessageBox.confirm(`确定删除会话「${row.title}」吗？`, '提示', {
    type: 'warning'
  })
  await removeConversation(row.id)
  ElMessage.success('删除成功')
  if (row.id === activeId.value) {
    activeId.value = ''
    messages.value = []
  }
  await loadConversations(1)
}

// ============ 对话消息 ============
interface ChatBubble extends ChatMessageItem {
  /** 流式输出中 */
  streaming?: boolean
  /** 图片展示地址（发送时为上传后的 URL，历史消息为后端返回的 URL） */
  imageUrls?: string[]
}

const messages = ref<ChatBubble[]>([])
const messageLoading = ref(false)
const scrollRef = ref<HTMLElement>()

const activeTitle = computed(
  () => conversations.value.find((c) => c.id === activeId.value)?.title || 'AI 对话'
)

/**
 * 消息时间统一为毫秒时间戳（与后端列表接口一致）：本地新增消息的时间展示与轮询入参
 */
function nowTimeString(): string {
  return String(Date.now())
}

/** 消息时间展示文案（HH:mm，兼容毫秒时间戳与旧的 yyyy-MM-dd HH:mm:ss 格式） */
function msgTimeText(item: ChatBubble): string {
  const value = item.createTime || ''
  // 毫秒时间戳（后端 Long 序列化为数字或纯数字字符串）
  if (/^\d{12,}$/.test(value)) {
    const d = new Date(Number(value))
    const p = (n: number) => String(n).padStart(2, '0')
    return `${p(d.getHours())}:${p(d.getMinutes())}`
  }
  return value.slice(11, 16)
}

/**
 * 拉取操作开始时间之后新增的消息列表（含 ID 与时间，升序、最新在最后），
 * 按序回填到本次新增的本地气泡（ID 供删除使用，时间以后端为准）；失败静默不影响展示
 */
async function syncNewMessageIds(
  conversationId: string | number,
  since: number,
  bubbles: ChatBubble[],
  /** head=从头对齐（中止场景：仅剩用户消息），tail=从尾对齐（默认，用户+AI 消息成对） */
  align: 'head' | 'tail' = 'tail'
) {
  if (!bubbles.length) return
  try {
    const res = await getAfterMessages(conversationId, since)
    const list = res.data || []
    if (list.length < bubbles.length) return
    const start = align === 'head' ? 0 : list.length - bubbles.length
    bubbles.forEach((b, i) => {
      const item = list[start + i]
      if (item?.id) {
        b.id = item.id
        if (item.createTime) b.createTime = item.createTime
      }
    })
  } catch {
    // 静默：回填失败不影响消息展示
  }
}

/** 删除单条消息（依赖消息 ID，历史消息自带、本地新增消息在操作完成后回填） */
async function handleRemoveMessage(item: ChatBubble) {
  if (!item.id || !activeId.value) return
  await removeChatMessage(activeId.value, item.id)
  messages.value = messages.value.filter((m) => m !== item)
  ElMessage.success('已删除')
}

/** 当前会话是否对话中（/ai/chat/{id}/list 的 isLocked），本地发送时也会置真（仅控制停止按钮） */
const chatLocked = ref(false)
/**
 * 是否需要轮询增量消息：仅在进入会话（loadMessages）发现服务端 isLocked 时置真，
 * 本地点击发送置位的 chatLocked 不启用轮询（自身流式输出驱动渲染）
 */
const lockPolling = ref(false)

/**
 * 以服务端锁定状态同步本地"对话中"标记：
 * 生成请求异常收尾时调用——后端仍在生成则保持停止按钮（可中断），已结束则恢复发送按钮
 */
async function refreshLockState() {
  if (!activeId.value) return
  try {
    const res = await getChatList(activeId.value)
    if (res.data) chatLocked.value = !!res.data.isLocked
  } catch {
    // 查询失败保持当前状态
  }
}

// ============ 对话中轮询 ============
/** 对话中（isLocked）时每 5 秒轮询一次"之后消息列表"接口，增量渲染新消息 */
const LOCK_POLL_INTERVAL = 5000
let lockPollTimer: ReturnType<typeof setInterval> | null = null
/** 轮询请求进行中标记，避免慢请求导致轮询重叠 */
let lockPollInFlight = false
/** 轮询窗口起点：已展示的最后一条消息时间（毫秒） */
let lockPollSince = 0

function stopLockPolling() {
  if (lockPollTimer) {
    clearInterval(lockPollTimer)
    lockPollTimer = null
  }
}

/**
 * 消息时间转毫秒时间戳，非法返回 0。兼容：
 * - 纯数字时间戳（秒/毫秒）
 * - yyyy-MM-dd HH:mm:ss[.SSS]（后端当前格式，连字符为任意破折号字符也能匹配）
 * 不依赖 new Date 的字符串解析（该格式在部分引擎下会得到 NaN -> 0）
 */
function timeStringToEpoch(value?: string): number {
  if (!value) return 0
  // 纯数字时间戳直接取值
  if (/^\d{10,}$/.test(value)) return Number(value)
  // 显式按分量解析日期时间串（\D 兼容普通连字符与各类破折号）
  const m = value.match(
    /^(\d{4})\D(\d{1,2})\D(\d{1,2})\D(\d{1,2}):(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?/
  )
  if (m) {
    const epoch = new Date(
      Number(m[1]),
      Number(m[2]) - 1,
      Number(m[3]),
      Number(m[4]),
      Number(m[5]),
      Number(m[6]),
      Number(m[7] || 0)
    ).getTime()
    if (!Number.isNaN(epoch)) return epoch
  }
  // 兜底：标准格式交给引擎解析
  const time = new Date(value.replace(/-/g, '/')).getTime()
  return Number.isNaN(time) ? 0 : time
}

/** 取当前已展示消息中的最大创建时间：轮询窗口应取"最后一条消息的时间戳" */
function latestMessageEpoch(): number {
  return messages.value.reduce((max, m) => Math.max(max, timeStringToEpoch(m.createTime)), 0)
}

function startLockPolling() {
  if (lockPollTimer) return
  // 以当前已展示的最后一条消息时间为窗口起点，轮询其后新增的消息
  lockPollSince = latestMessageEpoch()
  lockPollTimer = setInterval(async () => {
    const id = activeId.value
    if (!id || lockPollInFlight) return
    lockPollInFlight = true
    try {
      // 每轮请求前用最新展示状态校正窗口：启动时消息未就位（算出 0）也能自愈，
      // 避免一直以 0 从会话起点全量拉取
      lockPollSince = Math.max(lockPollSince, latestMessageEpoch())
      const res = await getAfterMessages(id, lockPollSince)
      // 轮询期间已切换会话：丢弃本次结果（activeId 变化会重启轮询）
      if (id !== activeId.value) return
      const items = (res.data || []).filter(
        (m) => m.type === ChatMessageType.USER || m.type === ChatMessageType.AI
      )
      let aiArrived = false
      items.forEach((item) => {
        if (!item.id || messages.value.some((m) => m.id === item.id)) return
        const bubble: ChatBubble = {
          id: item.id,
          type: item.type,
          message: item.message,
          createTime: item.createTime,
          imageUrls: item.images || []
        }
        // 插到"输出中"占位气泡之前，保持时间顺序
        const placeholderIndex = messages.value.findIndex((m) => m.streaming && !m.id)
        if (placeholderIndex === -1) {
          messages.value.push(bubble)
        } else {
          messages.value.splice(placeholderIndex, 0, bubble)
        }
        if (item.type === ChatMessageType.AI) aiArrived = true
        // 入参起点推进到本条消息创建时间，下一轮窗口随之收紧
        lockPollSince = Math.max(lockPollSince, timeStringToEpoch(item.createTime))
      })
      scrollToBottom()
      // AI 回复落库 = 本轮生成结束：直接以已渲染数据为准，仅移除"输出中"占位气泡，
      // 不再回查对话列表接口；自身流式输出进行中时不打扰（由流结束逻辑收尾）
      if (aiArrived && !sending.value) {
        chatLocked.value = false
        // 本轮生成结束：复位轮询标记，watch 停止轮询
        lockPolling.value = false
        messages.value = messages.value.filter((m) => !(m.streaming && !m.id))
      }
    } catch {
      // 单次轮询失败静默，等待下一轮
    } finally {
      lockPollInFlight = false
    }
  }, LOCK_POLL_INTERVAL)
}

// 进入会话时服务端锁定（isLocked）或会话变化时启停轮询（会话切换时 true -> true 也能触发重启）；
// 本地发送置位的 chatLocked 不在此列，避免发送后立即轮询
watch([lockPolling, activeId], () => {
  if (lockPolling.value && activeId.value) {
    startLockPolling()
  } else {
    stopLockPolling()
  }
})

async function loadMessages() {
  if (!activeId.value) return
  messageLoading.value = true
  try {
    const res = await getChatList(activeId.value)
    chatLocked.value = !!res.data?.isLocked
    // 进入会话时服务端仍在生成才启用轮询（本地发送置位的 chatLocked 不启用）
    lockPolling.value = !!res.data?.isLocked
    // 响应为 { isLocked, items }：items 内 images 为图片 URL 地址，直接回显
    messages.value = (res.data?.items || []).map((m) => ({
      ...m,
      imageUrls: m.images || []
    }))
    // 刷新/切换到对话中的会话：追加 AI 输出中的占位气泡（打字特效），
    // 锁定结束（轮询）后 loadMessages 重建列表时自然移除
    if (chatLocked.value) {
      messages.value.push(
        reactive({ type: ChatMessageType.AI, message: '', streaming: true })
      )
    }
    scrollToBottom()
  } finally {
    messageLoading.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// ============ 发送 / 停止 ============
const input = ref('')
const sending = ref(false)
let abortStream: (() => void) | null = null
/** 正在生成中的会话 ID（用于切换/新建时通知服务端停止） */
let streamingConversationId: string | number = ''
/** 最近一次流式对话的开始时间：停止后用于拉取最新消息 ID 列表 */
let lastStreamOpStart = 0

// ============ 图片附件 ============
/** 单次最多携带的图片数 */
const MAX_IMAGES = 5
/** 单图体积上限：图片上传接口 multipart 未配置、走 Spring 默认单文件 1MB（实测 696KB 可过、1.39MB 被拒） */
const MAX_IMAGE_BYTES = 700 * 1024
/** 压缩起始长边（像素），未达标时逐轮降至 0.7 倍 */
const MAX_IMAGE_DIM = 1600
const imageInputRef = ref<HTMLInputElement>()

interface PendingImage {
  id: number
  file: File
  /** 本地预览地址 */
  url: string
}

const pendingImages = ref<PendingImage[]>([])
let imageSeq = 0

/** canvas.toBlob 的 Promise 包装 */
function canvasToBlob(canvas: HTMLCanvasElement, quality: number): Promise<Blob | null> {
  return new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality))
}

/**
 * 图片压缩（白底转 JPEG，避免透明通道变黑）：
 * 超过体积上限时按「降质量 → 降分辨率」双重递减，直至达标；
 * 返回 null 表示无法压到目标体积（解码失败或极小图压不动），由调用方拒绝该图，
 * 杜绝把超限原文件发给后端（会触发 multipart 解析失败）
 */
async function compressImage(file: File): Promise<File | null> {
  if (file.size <= MAX_IMAGE_BYTES) return file
  try {
    const bitmap = await createImageBitmap(file)
    let blob: Blob | null = null
    let scale = Math.min(1, MAX_IMAGE_DIM / Math.max(bitmap.width, bitmap.height))

    while (scale > 0) {
      const width = Math.max(1, Math.round(bitmap.width * scale))
      const height = Math.max(1, Math.round(bitmap.height * scale))
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      if (!ctx) return null
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, width, height)
      ctx.drawImage(bitmap, 0, 0, width, height)

      for (let quality = 0.85; quality >= 0.35; quality -= 0.15) {
        blob = await canvasToBlob(canvas, quality)
        if (blob && blob.size <= MAX_IMAGE_BYTES) break
      }
      if (blob && blob.size <= MAX_IMAGE_BYTES) break
      // 当前分辨率压不到目标体积：降低分辨率重试（过小则放弃）
      scale = width <= 400 ? 0 : scale * 0.7
    }

    if (!blob || blob.size > MAX_IMAGE_BYTES) return null
    return new File([blob], `${file.name.replace(/\.[^.]+$/, '')}.jpg`, { type: 'image/jpeg' })
  } catch {
    return null
  }
}

/** 追加图片附件（超限提示并截断，加入前压缩到后端可接收的体积，压不动则拒绝） */
async function addImages(files: FileList | File[] | null | undefined) {
  if (!files) return
  const images = Array.from(files).filter((f) => f.type.startsWith('image/'))
  if (!images.length) return
  const remain = MAX_IMAGES - pendingImages.value.length
  if (remain <= 0) {
    ElMessage.warning(`最多携带 ${MAX_IMAGES} 张图片`)
    return
  }
  if (images.length > remain) {
    ElMessage.warning(`最多携带 ${MAX_IMAGES} 张图片，已忽略多余的 ${images.length - remain} 张`)
  }
  for (const file of images.slice(0, remain)) {
    const compressed = await compressImage(file)
    if (!compressed) {
      ElMessage.error(`图片「${file.name}」无法压缩至可发送体积，已忽略`)
      continue
    }
    pendingImages.value.push({
      id: imageSeq++,
      file: compressed,
      url: URL.createObjectURL(compressed)
    })
  }
  // 清空 value，允许重复选择同一文件
  if (imageInputRef.value) imageInputRef.value.value = ''
}

function handleImagePick(event: Event) {
  addImages((event.target as HTMLInputElement).files)
}

function removePendingImage(item: PendingImage) {
  URL.revokeObjectURL(item.url)
  pendingImages.value = pendingImages.value.filter((i) => i.id !== item.id)
}

/** 输入框粘贴图片：剪贴板含图片时拦截默认粘贴并转为附件 */
function handlePaste(event: ClipboardEvent) {
  const files = event.clipboardData?.files
  if (files && Array.from(files).some((f) => f.type.startsWith('image/'))) {
    event.preventDefault()
    addImages(files)
  }
}

// ============ 打字机效果 ============
/** 打字机帧间隔（毫秒），逐字上屏节奏 */
const TYPE_INTERVAL = 20
let typingTimer: ReturnType<typeof setInterval> | null = null
/** 已接收但尚未上屏的内容（打字机缓冲） */
let pendingText = ''
/** 流是否已结束（结束后缓冲打完即可收尾） */
let streamFinished = false
/** 打字机正在输出的 AI 气泡 */
let typingTarget: ChatBubble | null = null

/** 启动打字机：流内容先入缓冲，由定时器逐字上屏 */
function startTyping(target: ChatBubble) {
  stopTypingTimer()
  typingTarget = target
  pendingText = ''
  streamFinished = false
  typingTimer = setInterval(() => {
    if (!typingTarget) return
    if (!pendingText.length) {
      // 缓冲已空：流结束后收尾，否则继续等待后续内容
      if (streamFinished) finishTyping()
      return
    }
    // 按积压量自适应每帧字数，保证上屏速度追得上流速度
    const step = Math.max(1, Math.ceil(pendingText.length / 30))
    typingTarget.message += pendingText.slice(0, step)
    pendingText = pendingText.slice(step)
    scrollToBottom()
  }, TYPE_INTERVAL)
}

function stopTypingTimer() {
  if (typingTimer) {
    clearInterval(typingTimer)
    typingTimer = null
  }
}

/** 打字机收尾：结束本条消息输出 */
function finishTyping() {
  stopTypingTimer()
  if (typingTarget) {
    typingTarget.streaming = false
    typingTarget = null
  }
  pendingText = ''
  sending.value = false
  abortStream = null
  streamingConversationId = ''
}

/** 立即输出缓冲中的全部内容并收尾（停止/异常/切换会话时） */
function flushTyping() {
  if (!typingTarget) return
  typingTarget.message += pendingText
  finishTyping()
}

/** 移除无任何内容的 AI 气泡（请求异常、未收到任何输出时残留的空气泡） */
function removeEmptyAiBubbles() {
  messages.value = messages.value.filter(
    (m) => m.type !== ChatMessageType.AI || m.message || (m.imageUrls && m.imageUrls.length)
  )
}

/** 中断当前流（切换/新建会话时静默处理，不打扰用户） */
function stopStreamSilently() {
  if (abortStream) {
    const id = streamingConversationId
    stopChat(id).catch(() => {})
    abortStream()
    abortStream = null
    streamingConversationId = ''
  }
  flushTyping()
  messages.value.forEach((m) => (m.streaming = false))
  sending.value = false
}

async function handleSend() {
  // 图片生成模式选中时：发送走图片生成
  if (imageGenActive.value) {
    await handleGenerateImage()
    return
  }

  const message = input.value.trim()
  const images = [...pendingImages.value]
  // 后端 message 必填：纯图片时使用默认文案
  const finalMessage = message || (images.length ? '请分析图片' : '')
  if (!finalMessage || sending.value) return
  // 对话中（会话被锁定）时禁止发送，等待生成结束
  if (chatLocked.value) return

  let conversationId = activeId.value
  // 本次操作开始时间：结束后用 getAfterMessages 回填新增消息 ID 与时间
  const opStart = Date.now()
  try {
    sending.value = true
    // 选中图片时：先上传获取 URL 地址，再随对话参数（ChatParam.images）发给后端
    let imageUrls: string[] = []
    if (images.length) {
      const uploaded = await uploadImage('ai/chat', images.map((i) => i.file))
      imageUrls = (uploaded.data || []).map((i) => i.url)
      if (imageUrls.length !== images.length) {
        ElMessage.error('图片上传失败，请重试')
        sending.value = false
        return
      }
    }
    // 无选中会话：先以首条消息创建会话（后端以该消息生成标题）
    if (!conversationId) {
      const created = await createConversation(finalMessage)
      conversationId = created.data
      activeId.value = conversationId
      await loadConversations(1)
    }

    input.value = ''
    pendingImages.value = []
    // 本地预览地址已完成使命（气泡改用上传后的 URL），及时释放
    images.forEach((i) => URL.revokeObjectURL(i.url))
    // 开始生成：本地即时置为对话中（结束/停止后由 loadMessages 校正）
    chatLocked.value = true
    const userMessage: ChatBubble = reactive({
      type: ChatMessageType.USER,
      message: finalMessage,
      createTime: nowTimeString(),
      imageUrls
    })
    messages.value.push(userMessage)
    // 必须为 reactive 代理对象：打字机定时器逐字修改 message 时才能触发视图更新
    const aiMessage: ChatBubble = reactive({
      type: ChatMessageType.AI,
      message: '',
      createTime: nowTimeString(),
      streaming: true
    })
    messages.value.push(aiMessage)
    scrollToBottom()
    startTyping(aiMessage)

    abortStream = await chatStream(conversationId, finalMessage, {
      onMessage: (chunk) => {
        // 流内容先进缓冲，由打字机逐字上屏
        pendingText += chunk
      },
      onFinish: () => {
        // 流结束：待缓冲内容逐字打完后自动收尾
        streamFinished = true
        aiMessage.createTime = nowTimeString()
        chatLocked.value = false
        // 拉取本次新增消息 ID（最新在最后），回填到本地气泡供删除使用
        syncNewMessageIds(conversationId, opStart, [userMessage, aiMessage])
        // 刷新会话列表（标题等可能有更新）
        loadConversations(1)
      },
      onError: (msg) => {
        // 异常：已收到的内容立即完整展示，并复位对话状态（停止按钮恢复为发送按钮）
        flushTyping()
        // 未收到任何输出时移除残留的空 AI 气泡
        removeEmptyAiBubbles()
        messages.value.forEach((m) => (m.streaming = false))
        sending.value = false
        chatLocked.value = false
        ElMessage.error(msg)
      }
    }, activePrompt.value || undefined, imageUrls.length ? imageUrls : undefined)
    streamingConversationId = conversationId
    lastStreamOpStart = opStart
  } catch {
    // 请求异常（建立流失败等）：复位对话状态，停止按钮恢复为发送按钮
    flushTyping()
    // 未收到任何输出时移除残留的空 AI 气泡
    removeEmptyAiBubbles()
    messages.value.forEach((m) => (m.streaming = false))
    sending.value = false
    chatLocked.value = false
  }
}

// ============ 图片生成 ============
/** 图片生成模式：点击按钮选中，再次点击取消；选中后发送即走图片生成 */
const imageGenActive = ref(false)
const imageGenLoading = ref(false)
const imageGenForm = reactive({ count: 1, width: 1024, height: 1024 })
/** 生成请求的中止控制器：点"停止"时中止前端等待 */
let imageGenAbort: AbortController | null = null
/** 本次生成操作开始时间与仍展示的气泡：停止后用于拉取最新消息列表回填 ID */
let imageGenOpStart = 0
let imageGenBubbles: ChatBubble[] = []

function toggleImageGen() {
  imageGenActive.value = !imageGenActive.value
}

/**
 * 图片生成：以输入框内容为描述，按当前配置的数量/尺寸调用生成接口，
 * 生成的图片 URL 以 AI 气泡展示
 */
async function handleGenerateImage() {
  const prompt = input.value.trim()
  if (!prompt) {
    ElMessage.warning('请先输入图片描述')
    return
  }
  if (pendingImages.value.length) {
    ElMessage.warning('图片生成模式不支持携带附件，请先移除图片')
    return
  }
  if (imageGenLoading.value) return
  // 对话中（会话被锁定）时禁止生成，等待生成结束
  if (chatLocked.value) {
    ElMessage.warning('对话进行中，请稍后再试')
    return
  }

  let conversationId = activeId.value
  // 本次操作开始时间：结束后用 getAfterMessages 回填新增消息 ID 与时间
  const opStart = Date.now()
  imageGenLoading.value = true
  // 生成中同步置为对话中（停止/结束后校正）
  chatLocked.value = true
  imageGenAbort = new AbortController()
  try {
    // 无选中会话：先以描述创建会话
    if (!conversationId) {
      const created = await createConversation(prompt)
      conversationId = created.data
      activeId.value = conversationId
      await loadConversations(1)
    }

    const userMessage: ChatBubble = reactive({
      type: ChatMessageType.USER,
      message: prompt,
      createTime: nowTimeString()
    })
    messages.value.push(userMessage)
    input.value = ''
    scrollToBottom()
    // 记录本次操作信息：中止时仅剩用户消息仍展示，用于停止后回填 ID
    imageGenOpStart = opStart
    imageGenBubbles = [userMessage]

    // 生成中的 AI 气泡：打字指示特效，完成后原位填充结果
    const generatingBubble: ChatBubble = reactive({
      type: ChatMessageType.AI,
      message: '',
      createTime: nowTimeString(),
      streaming: true
    })
    messages.value.push(generatingBubble)
    scrollToBottom()

    try {
      const res = await generateImage(
        {
          message: prompt,
          conversationId,
          promptConfig: activePrompt.value || undefined,
          count: imageGenForm.count,
          width: imageGenForm.width,
          height: imageGenForm.height
        },
        imageGenAbort.signal
      )
      // 新响应结构 { message, images }，成功后原位替换生成中气泡
      const urls = res.data?.images || []
      if (!urls.length) {
        messages.value = messages.value.filter((m) => m !== generatingBubble)
        ElMessage.error('图片生成失败，请稍后重试')
        // 报错后以服务端锁定状态决定按钮：仍在生成保持停止，已结束恢复发送
        await refreshLockState()
        return
      }
      generatingBubble.message = res.data?.message || ''
      generatingBubble.imageUrls = urls
      generatingBubble.streaming = false
      generatingBubble.createTime = nowTimeString()
      // 成功即本轮结束
      chatLocked.value = false
      scrollToBottom()
      // 拉取本次新增消息 ID（最新在最后），回填到本地气泡供删除使用
      syncNewMessageIds(conversationId, opStart, [userMessage, generatingBubble])
    } catch {
      // 主动中止或异常：移除生成中的气泡（错误提示由请求层统一处理，中止静默），
      // 按钮去留以服务端锁定状态为准（主动中止后通常已解锁→恢复发送按钮）
      messages.value = messages.value.filter((m) => m !== generatingBubble)
      await refreshLockState()
    }
  } catch {
    // 创建会话等前置异常：直接恢复发送按钮
    chatLocked.value = false
  } finally {
    imageGenLoading.value = false
    imageGenAbort = null
    imageGenOpStart = 0
    imageGenBubbles = []
  }
}

async function handleStop() {
  if (!activeId.value) return
  // 图片生成中：中止前端等待并释放服务端锁
  if (imageGenLoading.value) {
    // 先捕获本次操作信息（中止会触发生成的 finally 清理）
    const opStart = imageGenOpStart
    const bubbles = [...imageGenBubbles]
    imageGenAbort?.abort()
    await stopChat(activeId.value).catch(() => {})
    imageGenLoading.value = false
    imageGenAbort = null
    chatLocked.value = false
    // 停止后拉取本次新增的消息列表（含 ID，最新在最后），从头对齐回填到仍展示的用户消息，
    // 使其可删除；服务端稍后落库的生成结果在下次加载时自带 ID
    if (opStart && bubbles.length) {
      await syncNewMessageIds(activeId.value, opStart, bubbles, 'head')
    }
    return
  }
  // 服务端停止生成并落库已生成的部分内容，随后本地断开流通道
  await stopChat(activeId.value).catch(() => {})
  stopStreamSilently()
  // 停止后拉取本次流式期间新增的消息列表（含 ID，最新在最后），失败静默
  if (lastStreamOpStart) {
    await getAfterMessages(activeId.value, lastStreamOpStart).catch(() => {})
    lastStreamOpStart = 0
  }
  // 服务端保存已生成内容存在轻微异步延迟，稍候再拉取最终消息
  await new Promise((resolve) => setTimeout(resolve, 300))
  await loadMessages()
  // 刷新会话列表，更新"对话中"锁定状态
  loadConversations(1)
}

onBeforeUnmount(() => {
  if (abortStream) {
    stopChat(streamingConversationId).catch(() => {})
    abortStream()
  }
  stopTypingTimer()
  stopLockPolling()
  // 释放未发送图片的预览地址
  pendingImages.value.forEach((i) => URL.revokeObjectURL(i.url))
})

// ============ 输入法组合输入状态 ============
/** 是否处于输入法组合输入中（由 compositionstart/end 维护） */
let composing = false
/** 组合输入刚结束的时间戳（Safari 的选词回车 keydown 晚于 compositionend 触发，用时间差兜底） */
let compositionEndTime = 0

function handleCompositionStart() {
  composing = true
}

function handleCompositionEnd() {
  composing = false
  compositionEndTime = performance.now()
}

function handleInputKeydown(event: Event) {
  const e = event as KeyboardEvent
  if (e.key !== 'Enter' || e.shiftKey) return
  // 输入法选词/上屏的回车不发送：Chrome 等可由 isComposing 识别；
  // Safari 的 compositionend 先于 keydown（isComposing 已为 false），用自维护状态与刚结束时间兜底
  if (
    e.isComposing ||
    e.keyCode === 229 ||
    composing ||
    performance.now() - compositionEndTime < 30
  ) {
    return
  }
  e.preventDefault()
  handleSend()
}
</script>

<template>
  <div class="chat-page">
    <!-- ============ 左侧会话面板 ============ -->
    <aside class="conversation-panel">
      <div class="panel-header">
        <div class="brand">
          <span class="brand-logo">
            <el-icon :size="16"><MagicStick /></el-icon>
          </span>
          <span class="brand-name">AI 对话</span>
        </div>
        <button class="create-btn" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          <span>新建对话</span>
        </button>
      </div>

      <div v-loading="conversationLoading" class="conversation-list">
        <div
          v-for="row in conversations"
          :key="row.id"
          class="conversation-item"
          :class="{ active: row.id === activeId }"
          @click="handleSelect(row)"
        >
          <el-icon class="item-icon"><ChatIcon /></el-icon>
          <span class="item-title" :title="row.title">{{ row.title }}</span>
          <span class="item-actions" @click.stop>
            <button class="action-btn" title="更新标题" @click="handleUpdateTitle(row)">
              <el-icon><Edit /></el-icon>
            </button>
            <button class="action-btn" title="删除会话" @click="handleRemove(row)">
              <el-icon><Delete /></el-icon>
            </button>
          </span>
        </div>
        <div v-if="hasMore" class="load-more" @click="handleLoadMore">加载更多</div>
        <div v-if="!conversationLoading && !conversations.length" class="list-empty">
          <el-icon :size="22"><ChatIcon /></el-icon>
          <p>暂无会话</p>
        </div>
      </div>

      <div class="panel-footer">共 {{ total }} 个会话</div>
    </aside>

    <!-- ============ 右侧对话面板 ============ -->
    <section class="chat-panel">
      <header class="chat-header">
        <div class="header-info">
          <h3 class="header-title">{{ activeTitle }}</h3>
          <div class="header-sub" :class="{ locked: chatLocked }">
            <span class="status-dot" />
            {{ chatLocked ? '对话中 ...' : 'AI 助手在线' }}
          </div>
        </div>
      </header>

      <div ref="scrollRef" v-loading="messageLoading" class="message-list">
        <!-- 空状态欢迎页 -->
        <div v-if="!messages.length" class="welcome">
          <div class="welcome-icon">
            <el-icon :size="30"><MagicStick /></el-icon>
          </div>
          <h2 class="welcome-title">欢迎回来{{ userStore.name }}，有什么可以帮助您 !</h2>
          <p class="welcome-desc">输入问题开始对话，或从下面的话题开始</p>
          <div v-if="suggestions.length" class="welcome-suggestions">
            <button v-for="s in suggestions" :key="s" @click="handleSuggestion(s)">
              {{ s }}
            </button>
          </div>
        </div>

        <!-- 消息气泡 -->
        <div
          v-for="(item, index) in messages"
          :key="index"
          class="message-row"
          :class="item.type === ChatMessageType.USER ? 'self' : 'ai'"
        >
          <div class="avatar">
            <template v-if="item.type === ChatMessageType.USER">
              <img v-if="userStore.icon" class="avatar-img" :src="userStore.icon" alt="" />
              <template v-else>{{ userStore.name.slice(0, 1) }}</template>
            </template>
            <el-icon v-else :size="17"><MagicStick /></el-icon>
          </div>

          <div class="bubble" :class="{ markdown: item.type === ChatMessageType.AI }">
            <!-- 消息携带的图片：相册式容器，点击放大预览（可左右切换） -->
            <div
              v-if="item.imageUrls && item.imageUrls.length"
              class="bubble-images"
              :class="{ 'with-text': !!item.message }"
            >
              <el-image
                v-for="(img, i) in item.imageUrls"
                :key="i"
                class="bubble-image"
                :src="img"
                :preview-src-list="item.imageUrls"
                :initial-index="i"
                fit="contain"
                preview-teleported
                hide-on-click-modal
              />
            </div>
            <!-- 等待首个内容时显示打字指示 -->
            <span v-if="item.streaming && !item.message" class="typing">
              <i /><i /><i />
            </span>
            <template v-else>
              <!-- AI 输出按 Markdown 渲染，用户消息保持纯文本；空内容不渲染，避免多余占位 -->
              <ChatMarkdown
                v-if="item.type === ChatMessageType.AI && item.message"
                :content="item.message"
              />
              <span v-else-if="item.message" class="plain">{{ item.message }}</span>
              <span v-if="item.streaming" class="cursor">▍</span>
            </template>
          </div>

          <!-- 消息时间与操作（悬停显示删除，依赖消息 ID） -->
          <span v-if="msgTimeText(item)" class="msg-time">{{ msgTimeText(item) }}</span>
          <span v-if="item.id" class="msg-actions" @click.stop>
            <button class="msg-del" type="button" title="删除消息" @click="handleRemoveMessage(item)">
              <el-icon :size="13"><Delete /></el-icon>
            </button>
          </span>
        </div>
      </div>

      <!-- ============ 输入区 ============ -->
      <footer class="input-area">
        <div class="input-card">
          <!-- 待发送图片预览 -->
          <div v-if="pendingImages.length" class="attach-bar">
            <div
              v-for="(img, i) in pendingImages"
              :key="img.id"
              class="attach-item"
            >
              <el-image
                class="attach-img"
                :src="img.url"
                fit="cover"
                :preview-src-list="pendingImages.map((p) => p.url)"
                :initial-index="i"
                preview-teleported
                hide-on-click-modal
              />
              <button type="button" class="attach-remove" title="移除" @click="removePendingImage(img)">
                <el-icon :size="10"><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 图片生成模式配置条 -->
          <div v-if="imageGenActive" class="gen-bar">
            <span class="gen-bar-label">
              <el-icon :size="12"><Brush /></el-icon>
              图片生成模式
            </span>
            <div class="gen-bar-item">
              <span>数量</span>
              <el-input-number v-model="imageGenForm.count" :min="1" :max="4" size="small" controls-position="right" />
            </div>
            <div class="gen-bar-item">
              <span>宽度</span>
              <el-input-number v-model="imageGenForm.width" :min="256" :max="2048" :step="128" size="small" controls-position="right" />
            </div>
            <div class="gen-bar-item">
              <span>高度</span>
              <el-input-number v-model="imageGenForm.height" :min="256" :max="2048" :step="128" size="small" controls-position="right" />
            </div>
            <span class="gen-bar-tip">发送将以输入内容生成图片</span>
          </div>

          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            resize="none"
            :placeholder="imageGenActive ? '描述要生成的图片，Enter 生成 ...' : '可以根据您的权限操作所有数据，请输入您的问题，可粘贴或选择图片 ...'"
            @keydown="handleInputKeydown"
            @compositionstart="handleCompositionStart"
            @compositionend="handleCompositionEnd"
            @paste="handlePaste"
          />
          <div class="input-toolbar">
            <div class="toolbar-left">
              <button
                type="button"
                class="skill-btn"
                :class="{ active: pendingImages.length > 0 }"
                title="选择图片（可多选，也可直接粘贴）"
                @click="imageInputRef?.click()"
              >
                <el-icon :size="13"><Picture /></el-icon>
                <span>选择图片</span>
              </button>
              <input
                ref="imageInputRef"
                type="file"
                accept="image/*"
                multiple
                hidden
                @change="handleImagePick"
              />
              <!-- 图片生成技能：点击选中，再点取消选中 -->
              <button
                type="button"
                class="skill-btn"
                :class="{ active: imageGenActive }"
                :title="imageGenActive ? '取消图片生成模式' : '选中后发送即生成图片'"
                @click="toggleImageGen"
              >
                <el-icon :size="13"><Brush /></el-icon>
                <span>图片生成</span>
              </button>
              <el-popover
                v-if="promptOptions.length"
                v-model:visible="promptPopoverVisible"
                placement="top-start"
                :width="320"
                trigger="click"
                popper-class="prompt-popper"
              >
                <template #reference>
                  <button
                    type="button"
                    class="prompt-trigger"
                    :class="{ active: !!activePrompt }"
                  >
                    <el-icon :size="13"><MagicStick /></el-icon>
                    <span class="prompt-label">{{ activePrompt ? activePromptLabel : '系统提示词' }}</span>
                    <el-icon class="prompt-caret" :size="12"><ArrowDown /></el-icon>
                  </button>
                </template>

                <div class="prompt-panel">
                  <div class="prompt-panel-title">系统提示词</div>
                  <button
                    type="button"
                    class="prompt-option"
                    :class="{ active: !activePrompt }"
                    @click="selectPrompt('')"
                  >
                    <span class="option-name">不使用</span>
                    <el-icon v-if="!activePrompt" class="option-check" :size="14"><Check /></el-icon>
                  </button>
                  <button
                    v-for="p in promptOptions"
                    :key="p.id"
                    type="button"
                    class="prompt-option"
                    :class="{ active: activePrompt === p.name }"
                    @click="selectPrompt(p.name)"
                  >
                    <span class="option-name">{{ p.alias || p.name }}</span>
                    <el-icon v-if="activePrompt === p.name" class="option-check" :size="14"><Check /></el-icon>
                    <p v-if="p.value" class="option-preview">{{ p.value }}</p>
                  </button>
                </div>
              </el-popover>
              <span class="input-hint">Enter 发送 · Shift + Enter 换行</span>
            </div>
            <el-button
              v-if="sending || imageGenLoading || chatLocked"
              class="stop-btn"
              type="danger"
              plain
              :icon="VideoPause"
              @click="handleStop"
            >
              停止
            </el-button>
            <el-button
              v-else
              class="send-btn"
              type="primary"
              :icon="imageGenActive ? Brush : Promotion"
              :disabled="imageGenActive ? !input.trim() : !input.trim() && !pendingImages.length"
              @click="handleSend"
            >
              {{ imageGenActive ? '生成图片' : '发送' }}
            </el-button>
          </div>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped lang="scss">
/** 品牌渐变主色 */
$accent: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
$accent-hover: linear-gradient(135deg, #5558e8 0%, #7c4df2 100%);
$border-color: #eceef3;
$text-primary: #303133;
$text-secondary: #606266;

.chat-page {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: 0;
  background: linear-gradient(180deg, #f8f9fc 0%, #f1f3f9 100%);
}

// ============ 会话面板 ============
.conversation-panel {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 264px;
  background: #fff;
  border: 1px solid $border-color;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(31, 35, 41, 0.04);
  overflow: hidden;
}

.panel-header {
  padding: 16px 14px 12px;
  border-bottom: 1px solid #f2f3f7;
}

.brand {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: $accent;
  color: #fff;
  box-shadow: 0 3px 8px rgba(99, 102, 241, 0.35);
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
  letter-spacing: 0.5px;
}

.create-btn {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 38px;
  border: none;
  border-radius: 10px;
  background: $accent;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: $accent-hover;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

.conversation-list {
  flex: 1;
  overflow: auto;
  padding: 8px;
}

.conversation-item {
  position: relative;
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
  padding: 10px 10px 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f5f6fa;

    .item-actions {
      visibility: visible;
    }
  }

  &.active {
    background: linear-gradient(135deg, #eef2ff 0%, #f3efff 100%);

    .item-icon {
      color: #6366f1;
    }

    .item-title {
      color: #4f46e5;
      font-weight: 600;
    }

    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 0;
      width: 3px;
      height: 18px;
      border-radius: 2px;
      background: $accent;
      transform: translateY(-50%);
    }
  }
}

.item-icon {
  flex-shrink: 0;
  color: #a8abb2;
  transition: color 0.2s;
}

.item-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13.5px;
  color: $text-primary;
}

@keyframes lock-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.75);
  }
}

.item-actions {
  display: flex;
  gap: 4px;
  visibility: hidden;
  flex-shrink: 0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: #fff;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(31, 35, 41, 0.12);

  &:hover {
    color: #6366f1;
  }
}

.load-more {
  padding: 8px 0;
  color: #6366f1;
  font-size: 13px;
  text-align: center;
  cursor: pointer;

  &:hover {
    opacity: 0.8;
  }
}

.list-empty {
  padding: 40px 0;
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;

  p {
    margin: 8px 0 0;
  }
}

.panel-footer {
  flex-shrink: 0;
  padding: 10px;
  border-top: 1px solid #f2f3f7;
  color: #a8abb2;
  font-size: 12px;
  text-align: center;
}

// ============ 对话面板 ============
.chat-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid $border-color;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(31, 35, 41, 0.04);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding: 12px 20px;
  border-bottom: 1px solid #f2f3f7;
}

.header-title {
  margin: 0;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.header-sub {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-left: auto;
  color: #909399;
  font-size: 12px;

  // 对话中（/ai/chat/{id}/list 的 isLocked）：圆点切主题色快闪
  &.locked {
    color: var(--el-color-primary);

    .status-dot {
      background: var(--el-color-primary);
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
      animation: lock-pulse 1.2s ease-in-out infinite;
    }
  }
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.18);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  50% {
    box-shadow: 0 0 0 5px rgba(103, 194, 58, 0.08);
  }
}

// ============ 消息区 ============
.message-list {
  flex: 1;
  overflow: auto;
  padding: 24px;
  background: linear-gradient(180deg, #fbfcfe 0%, #f7f8fc 100%);

  &::-webkit-scrollbar,
  .conversation-list::-webkit-scrollbar {
    width: 5px;
  }

  &::-webkit-scrollbar-thumb,
  .conversation-list::-webkit-scrollbar-thumb {
    border-radius: 3px;
    background: #dcdfe6;

    &:hover {
      background: #c0c4cc;
    }
  }
}

// ---- 空状态欢迎 ----
.welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  text-align: center;
}

.welcome-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  border-radius: 20px;
  background: $accent;
  color: #fff;
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.35);
}

.welcome-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  background: $accent;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.welcome-desc {
  margin: 10px 0 24px;
  color: #909399;
  font-size: 14px;
}

.welcome-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 520px;

  button {
    padding: 8px 16px;
    border: 1px solid $border-color;
    border-radius: 999px;
    background: #fff;
    color: $text-secondary;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #6366f1;
      color: #6366f1;
      background: #f5f6ff;
      box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
    }
  }
}

// ---- 消息气泡 ----
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
  animation: rise 0.25s ease-out;

  // 消息时间：气泡外侧底部对齐
  .msg-time {
    align-self: flex-end;
    flex-shrink: 0;
    font-size: 11px;
    color: #c0c4cc;
    white-space: nowrap;
    line-height: 1;
    padding-bottom: 2px;
  }

  // 消息操作：悬停行时显示
  .msg-actions {
    align-self: flex-end;
    flex-shrink: 0;
    visibility: hidden;
    padding-bottom: 2px;

    .msg-del {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 22px;
      height: 22px;
      border: none;
      border-radius: 6px;
      background: transparent;
      color: #909399;
      cursor: pointer;
      transition: all 0.15s;

      &:hover {
        background: #fef0f0;
        color: #f56c6c;
      }
    }
  }

  &:hover .msg-actions {
    visibility: visible;
  }

  .avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 34px;
    height: 34px;
    border-radius: 10px;
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    overflow: hidden;
    white-space: nowrap;
  }

  .avatar-img {
    width: 100%;
    height: 100%;
    border-radius: inherit;
    object-fit: cover;
  }

  .bubble {
    max-width: 72%;
    padding: 10px 14px;
    font-size: 14px;
    line-height: 1.7;
    word-break: break-word;

    /** Markdown 气泡由内容组件自行排版，禁用 pre-wrap 避免多余空行 */
    &.markdown {
      white-space: normal;
      min-width: 0;
    }
  }

  &.ai {
    .avatar {
      background: $accent;
      box-shadow: 0 3px 8px rgba(139, 92, 246, 0.3);
    }

    .bubble {
      border: 1px solid $border-color;
      border-radius: 2px 12px 12px 12px;
      background: #fff;
      color: $text-primary;
      box-shadow: 0 1px 6px rgba(31, 35, 41, 0.05);

      // 图片相册底色（浅灰衬托白底气泡）
      .bubble-images {
        background: #f6f7fb;
      }
    }
  }

  &.self {
    flex-direction: row-reverse;

    .avatar {
      background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
      box-shadow: 0 3px 8px rgba(59, 130, 246, 0.3);
    }

    .bubble {
      border-radius: 12px 2px 12px 12px;
      background: linear-gradient(135deg, #4f7cf0 0%, #6366f1 100%);
      color: #fff;
      box-shadow: 0 3px 10px rgba(79, 124, 240, 0.3);
      // 含图消息适当加大内边距，避免图文顶到气泡边缘
      padding: 12px 14px;

      /** 用户消息保持纯文本按原始换行展示；轻投影提升渐变底上的可读性 */
      .plain {
        white-space: pre-wrap;
        text-shadow: 0 1px 2px rgba(30, 41, 92, 0.22);
      }

      // 图片相册：磨砂白框与气泡分层，图片缩略图描白边
      .bubble-images {
        background: rgba(255, 255, 255, 0.3);
        border: 1px solid rgba(255, 255, 255, 0.38);

        .bubble-image {
          border-color: rgba(255, 255, 255, 0.55);
        }
      }
    }
  }
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}

/** 打字指示（等待首个内容） */
.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 4px 0;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #a5b4fc;
    animation: bounce 1.2s infinite ease-in-out;

    &:nth-child(2) {
      animation-delay: 0.15s;
    }

    &:nth-child(3) {
      animation-delay: 0.3s;
    }
  }
}

@keyframes bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }

  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.cursor {
  margin-left: 1px;
  color: #6366f1;
  animation: blink 1s infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

// ============ 输入区 ============
.input-area {
  flex-shrink: 0;
  padding: 14px 18px 16px;
  border-top: 1px solid #f2f3f7;
  background: #fff;
}

.input-card {
  border: 1px solid $border-color;
  border-radius: 12px;
  background: #fafbfd;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:focus-within {
    border-color: #a5b4fc;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
  }

  :deep(.el-textarea__inner) {
    border: none;
    box-shadow: none !important;
    background: transparent;
    padding: 12px 14px 4px;
    font-size: 14px;
    line-height: 1.6;
  }
}

// ============ 图片附件 ============
.attach-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 14px 0;
}

// 图片生成模式配置条
.gen-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 10px 14px 0;
  padding: 8px 12px;
  border: 1px dashed #c7cdf8;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.06) 0%, rgba(139, 92, 246, 0.08) 100%);
}

.gen-bar-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #6366f1;
}

.gen-bar-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  span {
    font-size: 12px;
    color: $text-secondary;
  }

  :deep(.el-input-number) {
    width: 96px;
  }
}

.gen-bar-tip {
  margin-left: auto;
  font-size: 12px;
  color: #c0c4cc;
}

.attach-item {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 8px;
  border: 1px solid $border-color;
  overflow: visible;

  .attach-img {
    width: 100%;
    height: 100%;
    border-radius: 8px;
    cursor: zoom-in;
  }
}

.attach-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f56c6c;
  }
}

// 技能按钮：带文字的醒目胶囊（淡紫底 + 悬停/激活渐变高亮），选择图片 / 图片生成共用
.skill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12) 0%, rgba(139, 92, 246, 0.14) 100%);
  color: #6366f1;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: #fff;
    background: $accent;
    box-shadow: 0 2px 10px rgba(99, 102, 241, 0.35);
  }

  &.active {
    color: #fff;
    background: $accent;
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  }
}

// 图片相册：块级独占一行（图片在上、文字在下），宽度贴合内容不整行铺满
.bubble-images {
  display: flex;
  width: fit-content;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  max-width: 100%;
  padding: 6px;
  border-radius: 10px;

  // 图文混排时与正文拉开间距（呼吸感），纯图消息无多余间距
  &.with-text {
    margin-bottom: 12px;
  }
}

// 气泡缩略图：等高、宽度按图片比例自适应，无裁切无留白格子
.bubble-image {
  height: 140px;
  max-width: 240px;
  border-radius: 6px;
  cursor: zoom-in;

  :deep(.el-image__inner) {
    height: 100%;
    width: auto;
    max-width: 100%;
    border-radius: 6px;
  }
}

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px 8px 14px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.prompt-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 12px;
  border: 1px solid $border-color;
  border-radius: 999px;
  background: #fff;
  color: $text-secondary;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #a5b4fc;
    color: #6366f1;
  }

  &.active {
    border-color: transparent;
    color: #fff;
    background: $accent;
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  }
}

.prompt-label {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prompt-caret {
  transition: transform 0.2s;
}

.input-hint {
  color: #c0c4cc;
  font-size: 12px;
}

.chat-page .el-button.send-btn {
  border: none;
  border-radius: 10px;
  background: $accent;
  box-shadow: 0 3px 10px rgba(99, 102, 241, 0.35);
  font-weight: 500;

  &:hover,
  &:focus {
    background: $accent-hover;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.45);
  }

  &.is-disabled {
    background: #c8c9cc;
    box-shadow: none;
  }
}

// 停止按钮：胶囊形浅红底（对话中 isLocked 时替换发送按钮出现，可中断生成）
.chat-page .el-button.stop-btn {
  border-radius: 999px;
  border: 1px solid rgba(245, 108, 108, 0.4);
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
  font-weight: 500;
  padding: 8px 20px;

  &:hover,
  &:focus {
    background: rgba(245, 108, 108, 0.18);
    border-color: rgba(245, 108, 108, 0.55);
    color: #f56c6c;
  }

  &.is-disabled {
    opacity: 0.55;
  }
}
</style>

<!-- 系统提示词下拉面板：内容经 popover 传送至 body，需全局样式 -->
<style lang="scss">
.prompt-popper {
  padding: 8px;

  .prompt-panel-title {
    padding: 2px 6px 8px;
    font-size: 12px;
    font-weight: 600;
    color: #303133;
  }

  .prompt-panel {
    max-height: 320px;
    overflow: auto;
  }

  .prompt-option {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 2px 8px;
    width: 100%;
    padding: 8px 10px;
    border: none;
    border-radius: 8px;
    background: transparent;
    text-align: left;
    cursor: pointer;
    transition: background 0.15s;

    &:hover {
      background: #f5f5fb;
    }

    &.active {
      background: rgba(99, 102, 241, 0.08);
    }

    .option-name {
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      font-weight: 500;
      color: #303133;
    }

    .option-check {
      color: #6366f1;
    }

    .option-preview {
      flex-basis: 100%;
      margin: 2px 0 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      font-size: 12px;
      line-height: 1.5;
      color: #909399;
    }
  }
}
</style>
