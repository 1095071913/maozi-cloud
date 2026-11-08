/** JSON 值类型 */
export type JsonValueType = 'string' | 'number' | 'boolean' | 'null' | 'object' | 'array'

/** 可编辑的 JSON 节点（树形结构） */
export interface JsonNodeData {
  /** 节点自增 ID（列表渲染 key 与删除定位用） */
  id: number
  /** 字段名（数组子节点不使用，序列化时按位置生成） */
  key: string
  /** 值类型 */
  type: JsonValueType
  /** 基础类型值的文本形式，boolean 用 'true' / 'false' */
  value: string
  /** 是否展开子级 */
  expanded: boolean
  /** 子节点（object 为字段、array 为元素） */
  children: JsonNodeData[]
  /** 父节点引用（仅运行时使用，根节点为 null） */
  parent: JsonNodeData | null
}

/** 编辑器提供给子节点组件的操作接口 */
export interface JsonEditorOps {
  /** 切换节点类型 */
  setType(node: JsonNodeData, type: JsonValueType): void
  /** 添加子节点 */
  addChild(parent: JsonNodeData): void
  /** 删除节点 */
  removeNode(node: JsonNodeData): void
}

/** 类型下拉选项 */
export const VALUE_TYPE_OPTIONS: { label: string; value: JsonValueType }[] = [
  { label: '字符串', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: '空值', value: 'null' },
  { label: '对象', value: 'object' },
  { label: '数组', value: 'array' }
]

/** 各类型对应的主题色（编辑器展示用） */
export const TYPE_COLORS: Record<JsonValueType, string> = {
  string: '#22863a',
  number: '#005cc5',
  boolean: '#e36209',
  null: '#909399',
  object: '#6f42c1',
  array: '#6f42c1'
}

/** 节点 ID 自增计数器 */
let seed = 0

function nextId(): number {
  seed += 1
  return seed
}

/** 创建一个新的空节点 */
export function createNode(key: string, type: JsonValueType = 'string'): JsonNodeData {
  return {
    id: nextId(),
    key,
    type,
    value: type === 'boolean' ? 'false' : '',
    expanded: true,
    children: [],
    parent: null
  }
}

/** 任意 JSON 值 → 可编辑节点树 */
export function toJsonTree(val: unknown, key = 'root'): JsonNodeData {
  const node = createNode(key)
  if (val === null || val === undefined) {
    node.type = 'null'
  } else if (Array.isArray(val)) {
    node.type = 'array'
    node.children = val.map((item, i) => {
      const child = toJsonTree(item, String(i))
      child.parent = node
      return child
    })
  } else if (typeof val === 'object') {
    node.type = 'object'
    node.children = Object.entries(val).map(([k, v]) => {
      const child = toJsonTree(v, k)
      child.parent = node
      return child
    })
  } else if (typeof val === 'number') {
    node.type = 'number'
    node.value = String(val)
  } else if (typeof val === 'boolean') {
    node.type = 'boolean'
    node.value = String(val)
  } else {
    node.type = 'string'
    node.value = String(val)
  }
  return node
}

/** 节点树 → JSON 值（数字非法时按 0 处理） */
export function fromJsonTree(node: JsonNodeData): unknown {
  switch (node.type) {
    case 'object': {
      const obj: Record<string, unknown> = {}
      node.children.forEach((c) => {
        obj[c.key] = fromJsonTree(c)
      })
      return obj
    }
    case 'array':
      return node.children.map((c) => fromJsonTree(c))
    case 'number': {
      const n = Number(node.value)
      return Number.isFinite(n) ? n : 0
    }
    case 'boolean':
      return node.value === 'true'
    case 'null':
      return null
    default:
      return node.value
  }
}

/** 在同级子节点中生成不重复的字段名 */
export function uniqueKey(siblings: JsonNodeData[], base = 'field'): string {
  const keys = new Set(siblings.map((c) => c.key))
  if (!keys.has(base)) return base
  let i = 1
  while (keys.has(`${base}${i}`)) i += 1
  return `${base}${i}`
}

/** 校验树中所有对象层级的字段名均非空 */
export function validateKeys(node: JsonNodeData): boolean {
  for (const child of node.children) {
    if (node.type === 'object' && !child.key.trim()) return false
    if (!validateKeys(child)) return false
  }
  return true
}
