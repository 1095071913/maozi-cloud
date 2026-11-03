<script setup lang="ts">
// JSON 树的单个节点行：字段名/类型/值的编辑与增删，容器节点递归渲染子级
import { computed, inject } from 'vue'
import { ArrowRight, Delete, Plus } from '@element-plus/icons-vue'
import { TYPE_COLORS, VALUE_TYPE_OPTIONS, type JsonEditorOps, type JsonNodeData } from './types'

const props = defineProps<{
  node: JsonNodeData
  depth: number
  index: number
}>()

const ops = inject<JsonEditorOps>('jsonEditorOps')

const isContainer = computed(() => props.node.type === 'object' || props.node.type === 'array')
const isArrayChild = computed(() => props.node.parent?.type === 'array')
const isRoot = computed(() => props.depth === 0)
const nodeColor = computed(() => TYPE_COLORS[props.node.type])
const boolValue = computed({
  get: () => props.node.value === 'true',
  set: (v: boolean) => {
    props.node.value = String(v)
  }
})
</script>

<template>
  <div class="json-node" :class="[`t-${node.type}`, { 'is-root': isRoot }]">
    <div class="node-row" :class="{ 'is-container': isContainer }">
      <el-icon
        v-if="isContainer"
        class="node-caret"
        :class="{ 'is-open': node.expanded }"
        @click="node.expanded = !node.expanded"
      >
        <ArrowRight />
      </el-icon>
      <span v-else class="node-caret-holder" />

      <!-- 数组子节点按位置序列化，仅展示下标 -->
      <span v-if="isArrayChild" class="node-index">{{ index }}</span>
      <span v-else-if="isRoot" class="node-root-chip">{ }</span>
      <el-input
        v-else
        v-model="node.key"
        size="small"
        class="node-key"
        placeholder="字段名"
        maxlength="60"
      />

      <el-select
        :model-value="node.type"
        size="small"
        class="node-type"
        @update:model-value="ops?.setType(node, $event)"
      >
        <el-option v-for="o in VALUE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value">
          <span class="type-option">
            <i class="type-dot" :style="{ background: TYPE_COLORS[o.value] }" />
            {{ o.label }}
          </span>
        </el-option>
      </el-select>

      <div class="node-value">
        <el-input
          v-if="node.type === 'string' || node.type === 'number'"
          v-model="node.value"
          size="small"
          :placeholder="node.type === 'number' ? '数字值' : '字符串值'"
        />
        <div v-else-if="node.type === 'boolean'" class="bool-wrap">
          <el-switch v-model="boolValue" size="small" />
          <span class="bool-text" :class="{ 'is-on': boolValue }">{{ boolValue }}</span>
        </div>
        <span v-else-if="node.type === 'null'" class="null-text">null</span>
        <span v-else class="count-text">
          {{ node.type === 'object' ? `共 ${node.children.length} 个字段` : `共 ${node.children.length} 个元素` }}
        </span>
      </div>

      <el-tooltip
        v-if="isContainer"
        :content="node.type === 'array' ? '添加元素' : '添加字段'"
        placement="top"
      >
        <el-button
          size="small"
          link
          type="primary"
          :icon="Plus"
          class="node-btn"
          @click="ops?.addChild(node)"
        />
      </el-tooltip>
      <span v-else class="node-btn-holder" />

      <el-tooltip v-if="!isRoot" content="删除" placement="top">
        <el-button
          size="small"
          link
          type="danger"
          :icon="Delete"
          class="node-btn"
          @click="ops?.removeNode(node)"
        />
      </el-tooltip>
      <span v-else class="node-btn-holder" />
    </div>

    <div v-if="isContainer && node.expanded" class="node-children">
      <JsonNode
        v-for="(child, i) in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :index="i"
      />
      <div v-if="node.children.length === 0" class="node-empty">暂无内容</div>
      <button type="button" class="node-add" @click="ops?.addChild(node)">
        <el-icon><Plus /></el-icon>
        {{ node.type === 'array' ? '添加元素' : '添加字段' }}
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.json-node {
  --node-color: var(--el-text-color-regular);
  --key-color: #6f42c1;
}

// ============ 行 ============
.node-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 1px 6px rgba(0, 0, 0, 0.05);

    .node-btn {
      opacity: 1;
    }
  }

  // 对象 / 数组行
  &.is-container {
    border-color: color-mix(in srgb, var(--key-color) 22%, transparent);
    background: color-mix(in srgb, var(--key-color) 5%, var(--el-bg-color));
  }
}

.is-root > .node-row {
  border-color: color-mix(in srgb, var(--key-color) 35%, transparent);
  box-shadow: 0 1px 6px rgba(111, 66, 193, 0.08);
}

.node-caret {
  width: 16px;
  flex-shrink: 0;
  color: var(--key-color);
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.2s;

  &.is-open {
    transform: rotate(90deg);
  }
}

.node-caret-holder {
  width: 16px;
  flex-shrink: 0;
}

.node-root-chip {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--key-color) 10%, transparent);
  color: var(--key-color);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

// 数组下标
.node-index {
  width: 130px;
  flex-shrink: 0;
  text-align: center;
  padding: 2px 0;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 12px;
  line-height: 20px;
}

// 字段名：下划线式输入
.node-key {
  width: 130px;
  flex-shrink: 0;

  :deep(.el-input__wrapper) {
    padding: 0 4px;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
    border-bottom: 1px dashed var(--el-border-color-darker);
    transition: border-color 0.2s;
  }

  :deep(.el-input__wrapper:hover),
  :deep(.el-input__wrapper.is-focus) {
    box-shadow: none;
  }

  :deep(.el-input__wrapper:hover) {
    border-bottom-color: var(--key-color);
  }

  :deep(.el-input__wrapper.is-focus) {
    border-bottom: 1px solid var(--key-color);
  }

  :deep(.el-input__inner) {
    color: var(--key-color);
    font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
    font-weight: 600;
  }
}

// 类型选择
.node-type {
  width: 94px;
  flex-shrink: 0;

  :deep(.el-select__wrapper) {
    background: var(--el-fill-color-light);
    font-weight: 600;
  }

  :deep(.el-select__selected-item) {
    color: var(--node-color);
  }
}

.type-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.type-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: 50%;
}

// 值区
.node-value {
  flex: 1;
  min-width: 0;

  :deep(.el-input__inner) {
    color: var(--node-color);
    font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
    font-weight: 500;
  }
}

.bool-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bool-text {
  color: var(--el-text-color-placeholder);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 12px;

  &.is-on {
    color: #e36209;
    font-weight: 600;
  }
}

.null-text {
  color: var(--el-text-color-placeholder);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 12px;
  font-style: italic;
}

.count-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

// 操作按钮：默认弱化，悬停行时凸显
.node-btn {
  width: 26px;
  flex-shrink: 0;
  margin: 0;
  padding: 0 !important;
  opacity: 0.45;
  transition: opacity 0.2s, transform 0.2s;

  &:hover {
    opacity: 1;
    transform: scale(1.12);
  }
}

.node-btn-holder {
  width: 26px;
  flex-shrink: 0;
}

// ============ 子级 ============
.node-children {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 6px 0 2px 15px;
  padding-left: 15px;
  border-left: 2px solid color-mix(in srgb, var(--key-color) 20%, transparent);
}

.node-empty {
  padding: 4px 0;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  text-align: center;
}

.node-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  width: 100%;
  height: 30px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }
}
</style>
