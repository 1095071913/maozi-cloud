<script setup lang="ts">
// JSON 表单编辑器弹窗：以树形表单编辑 JSON，保存时序列化为规范 JSON 输出
import { computed, provide, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import JsonNode from './JsonNode.vue'
import {
  TYPE_COLORS,
  VALUE_TYPE_OPTIONS,
  createNode,
  fromJsonTree,
  toJsonTree,
  uniqueKey,
  validateKeys,
  type JsonEditorOps,
  type JsonNodeData,
  type JsonValueType
} from './types'

const props = defineProps<{
  /** 弹窗显隐 v-model */
  modelValue: boolean
  /** 打开时用于初始化的 JSON 字符串 */
  source: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', json: string): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const root = ref<JsonNodeData>(toJsonTree({}))

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    const src = props.source?.trim()
    if (!src) {
      root.value = toJsonTree({})
      return
    }
    try {
      root.value = toJsonTree(JSON.parse(src))
    } catch {
      root.value = toJsonTree({})
    }
  }
)

// ============ 树操作（子组件通过 inject 调用） ============
function setType(node: JsonNodeData, type: JsonValueType) {
  node.type = type
  if (type === 'boolean') {
    node.value = node.value === 'true' ? 'true' : 'false'
  }
  if (type === 'object' || type === 'array') {
    // 切换容器类型时保留已有子节点，仅确保展开
    node.expanded = true
  }
}

function addChild(parent: JsonNodeData) {
  const child = createNode(uniqueKey(parent.children))
  child.parent = parent
  parent.children.push(child)
  parent.expanded = true
}

function removeNode(node: JsonNodeData) {
  const siblings = node.parent?.children
  if (!siblings) return
  const i = siblings.findIndex((c) => c.id === node.id)
  if (i > -1) siblings.splice(i, 1)
}

provide<JsonEditorOps>('jsonEditorOps', { setType, addChild, removeNode })

function handleConfirm() {
  if (!validateKeys(root.value)) {
    ElMessage.warning('存在空的字段名，请补全后再保存')
    return
  }
  emit('confirm', JSON.stringify(fromJsonTree(root.value), null, 2))
  visible.value = false
}
</script>

<template>
  <el-dialog v-model="visible" width="760px" append-to-body destroy-on-close>
    <template #header>
      <div class="json-editor-header">
        <span class="json-editor-title">JSON 表单编辑</span>
        <span class="json-editor-sub">以表单方式填写，保存后自动生成规范 JSON</span>
      </div>
    </template>

    <div class="json-editor-toolbar">
      <div class="legend">
        <span
          v-for="o in VALUE_TYPE_OPTIONS"
          :key="o.value"
          class="legend-item"
          :style="{ color: TYPE_COLORS[o.value] }"
        >
          <i class="legend-dot" :style="{ background: TYPE_COLORS[o.value] }" />
          {{ o.label }}
        </span>
      </div>
      <span class="toolbar-hint">悬停行显示操作，字段名与值支持直接编辑</span>
    </div>

    <div class="json-editor-body">
      <JsonNode :node="root" :depth="0" :index="0" />
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.json-editor-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.json-editor-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.json-editor-sub {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.json-editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 500;
}

.legend-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: 50%;
}

.toolbar-hint {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  white-space: nowrap;
}

.json-editor-body {
  max-height: 56vh;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  overflow: auto;
}
</style>
