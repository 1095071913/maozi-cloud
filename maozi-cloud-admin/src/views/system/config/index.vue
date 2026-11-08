<script setup lang="ts">
// 配置管理页：按配置类型分组展示，支持 JSON 值格式化/放大/表单编辑与同类型配置复制
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CopyDocument, FullScreen } from '@element-plus/icons-vue'
import JsonEditorDialog from '@/components/JsonEditor/index.vue'
import {
  CONFIG_TYPE_NAME,
  getConfigByName,
  getConfigList,
  getConfigDetail,
  saveConfig,
  updateConfig,
  updateConfigStatus,
  removeConfig
} from '@/api/config'
import { useUserStore } from '@/store/modules/user'
import type { ConfigListItem, ConfigSaveParam } from '@/types/api'

const userStore = useUserStore()
/** 是否有编辑权限（无则状态列只读展示） */
const canUpdate = computed(() => userStore.permissions.includes('system:config:update'))

// ============ 配置类型 ============
/** 配置类型选项：code 为类型编码，alias 为展示别名 */
interface ConfigTypeOption {
  code: string
  alias: string
}

const typeList = ref<ConfigTypeOption[]>([])
const typeLoading = ref(false)
const activeType = ref('')

/**
 * 解析配置类型定义：
 * 名为 system_config_type 的配置项，其 value 为 JSON 字符串，
 * key 是类型编码，value 是别名，如 {"oss_config": "OSS 存储"}
 */
function parseTypeOptions(value?: string): ConfigTypeOption[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed).map(([code, alias]) => ({
        code,
        alias: String(alias ?? '')
      }))
    }
  } catch {
    // value 非合法 JSON 时视为无配置类型
  }
  return []
}

async function loadTypes() {
  typeLoading.value = true
  try {
    const res = await getConfigByName(CONFIG_TYPE_NAME)
    typeList.value = parseTypeOptions(res.data?.value)
    // 默认选中第一个配置类型并加载其列表
    const first = typeList.value[0]?.code
    if (first) {
      activeType.value = first
      loadList()
    }
  } finally {
    typeLoading.value = false
  }
}

/** 当前选中类型的名称（无匹配时回退编码本身） */
const activeTypeName = computed(
  () => typeList.value.find((t) => t.code === activeType.value)?.alias || activeType.value
)

/** 点击类型 → 加载该类型下的配置列表 */
function handleTypeClick(type: string) {
  if (activeType.value === type) return
  activeType.value = type
  query.name = ''
  query.alias = ''
  pagination.current = 1
  loadList()
}

// ============ 列表 ============
const list = ref<ConfigListItem[]>([])
const loading = ref(false)
const query = reactive({ name: '', alias: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })

async function loadList() {
  if (!activeType.value) return
  loading.value = true
  try {
    const res = await getConfigList({
      current: pagination.current,
      size: pagination.size,
      type: activeType.value,
      name: query.name,
      alias: query.alias
    })
    list.value = res.data?.data || []
    pagination.total = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadList()
}

function handleReset() {
  query.name = ''
  query.alias = ''
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.current = page
  loadList()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.current = 1
  loadList()
}

/**
 * 尝试将配置值格式化为 JSON：
 * 仅当 value 是合法 JSON 且为对象/数组时返回缩进格式化结果，否则返回 null 按原文展示
 */
function tryFormatJson(value?: string): string | null {
  if (!value) return null
  try {
    const parsed = JSON.parse(value)
    if (parsed && typeof parsed === 'object') {
      return JSON.stringify(parsed, null, 2)
    }
  } catch {
    // 非 JSON 原文展示
  }
  return null
}

async function handleStatusChange(row: ConfigListItem) {
  try {
    await updateConfigStatus(row.id, row.status)
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch {
    // 失败时回滚开关状态
    row.status = row.status === 1 ? 0 : 1
  }
}

async function handleRemove(row: ConfigListItem) {
  await ElMessageBox.confirm(`确定删除配置「${row.alias || row.name}」吗？`, '提示', {
    type: 'warning'
  })
  await removeConfig(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ============ 新增/编辑 ============
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<string | number>('')
const formRef = ref<FormInstance>()

/** 表单默认值（新增场景） */
const defaultForm = (): ConfigSaveParam => ({
  name: '',
  alias: '',
  type: '',
  value: '',
  sort: 99
})
const form = reactive(defaultForm())

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  alias: [{ required: true, message: '请输入别名', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  value: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  // 默认使用当前选中的类型
  form.type = activeType.value
  dialogVisible.value = true
}

async function handleEdit(row: ConfigListItem) {
  isEdit.value = true
  editId.value = row.id
  const res = await getConfigDetail(row.id)
  const d = res.data
  Object.assign(form, {
    name: d.name,
    alias: d.alias,
    type: d.type,
    // JSON 值编辑时预格式化，便于查看修改
    value: tryFormatJson(d.value) ?? d.value ?? '',
    sort: d.sort ?? 99
  })
  dialogVisible.value = true
}

function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateConfig(editId.value, form)
        ElMessage.success('更新成功')
      } else {
        await saveConfig(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitting.value = false
    }
  })
}

// ============ 放大编辑 ============
const zoomVisible = ref(false)
const zoomValue = ref('')

function openZoomEditor() {
  zoomValue.value = form.value
  zoomVisible.value = true
}

function handleZoomConfirm() {
  form.value = zoomValue.value
  zoomVisible.value = false
  formRef.value?.validateField('value').catch(() => {})
}

// ============ JSON 表单编辑 ============
const jsonDialogVisible = ref(false)
const jsonSource = ref('')

/** 当前生效的配置值：放大弹窗打开时以弹窗内容为准 */
function currentValue(): string {
  return zoomVisible.value ? zoomValue.value : form.value
}

/** 回写配置值：放大弹窗打开时写入弹窗，关闭时写入表单并清除校验提示 */
function setCurrentValue(v: string) {
  if (zoomVisible.value) {
    zoomValue.value = v
    return
  }
  form.value = v
  formRef.value?.validateField('value').catch(() => {})
}

async function openJsonEditor() {
  const v = currentValue().trim()
  if (v) {
    try {
      JSON.parse(v)
    } catch {
      try {
        await ElMessageBox.confirm('当前配置值不是合法的 JSON，是否以空对象开始编辑？', '提示', {
          type: 'warning',
          confirmButtonText: '继续编辑',
          cancelButtonText: '返回'
        })
      } catch {
        return
      }
    }
  }
  jsonSource.value = currentValue()
  jsonDialogVisible.value = true
}

function handleJsonConfirm(json: string) {
  setCurrentValue(json)
}

// ============ 复制配置（同类型） ============
const copyVisible = ref(false)
const copyLoading = ref(false)
const copyKeyword = ref('')
const copyList = ref<ConfigListItem[]>([])

/** 弹层打开时按当前类型拉取配置列表 */
async function loadCopyList() {
  if (!form.type) {
    copyList.value = []
    return
  }
  copyLoading.value = true
  try {
    const res = await getConfigList({ current: 1, size: 100, type: form.type })
    // 编辑时排除自身
    copyList.value = (res.data?.data || []).filter((item) => item.id !== editId.value)
  } finally {
    copyLoading.value = false
  }
}

/** 关键字本地过滤（名称 / 别名） */
const copyOptions = computed(() => {
  const kw = copyKeyword.value.trim().toLowerCase()
  if (!kw) return copyList.value
  return copyList.value.filter(
    (item) =>
      item.name.toLowerCase().includes(kw) || (item.alias || '').toLowerCase().includes(kw)
  )
})

function handleCopyShow() {
  copyKeyword.value = ''
  loadCopyList()
}

function handleCopyItem(row: ConfigListItem) {
  form.value = tryFormatJson(row.value) ?? row.value ?? ''
  copyVisible.value = false
  formRef.value?.validateField('value').catch(() => {})
  ElMessage.success(`已复制「${row.alias || row.name}」的配置值`)
}

onMounted(loadTypes)
</script>

<template>
  <div class="list-page config-page">
    <el-card shadow="never" class="type-card">
      <template #header>
        <div class="page-card-header">
          <div class="header-left">
            <span class="card-title">配置类型</span>
            <span class="card-subtitle">共 {{ typeList.length }} 类</span>
          </div>
        </div>
      </template>
      <div v-loading="typeLoading" class="type-list">
        <button
          v-for="t in typeList"
          :key="t.code"
          type="button"
          class="type-item"
          :class="{ active: activeType === t.code }"
          @click="handleTypeClick(t.code)"
        >
          <span class="type-item-dot" />
          {{ t.alias || t.code }}
        </button>
        <el-empty
          v-if="!typeLoading && typeList.length === 0"
          description="暂无配置类型"
          :image-size="70"
        />
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="page-card-header">
          <div class="header-left">
            <span class="card-title">{{ activeType ? activeTypeName : '配置列表' }}</span>
            <el-tag v-if="activeType" size="small" effect="plain">{{ activeType }}</el-tag>
          </div>
        </div>
      </template>

      <div class="list-toolbar">
        <el-form inline @submit.prevent>
          <el-form-item label="名称">
            <el-input
              v-model="query.name"
              placeholder="请输入名称"
              clearable
              :disabled="!activeType"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="别名">
            <el-input
              v-model="query.alias"
              placeholder="请输入别名"
              clearable
              :disabled="!activeType"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!activeType" @click="handleSearch">查询</el-button>
            <el-button :disabled="!activeType" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button
          v-auth="'system:config:save'"
          type="primary"
          :disabled="!activeType"
          @click="handleAdd"
        >
          新增
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="名称" prop="name" min-width="180" show-overflow-tooltip />
        <el-table-column label="别名" prop="alias" min-width="140" show-overflow-tooltip />
        <el-table-column label="排序" prop="sort" width="80" align="center" />
        <el-table-column label="配置值" min-width="260">
          <template #default="{ row }">
            <div v-if="tryFormatJson(row.value)" class="json-wrap">
              <el-tag size="small" type="success" effect="plain" class="json-tag">JSON</el-tag>
              <pre class="value-json">{{ tryFormatJson(row.value) }}</pre>
            </div>
            <code v-else class="value-code" :title="row.value">{{ row.value }}</code>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="canUpdate"
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row as ConfigListItem)"
            />
            <span v-else :style="{ color: row.status === 1 ? '#67c23a' : '#909399' }">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170" align="center" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="'system:config:update'" link type="primary" @click="handleEdit(row as ConfigListItem)">编辑</el-button>
            <el-button v-auth="'system:config:remove'" link type="danger" @click="handleRemove(row as ConfigListItem)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!activeType"
        description="请先在上方选择配置类型"
        class="guide-empty"
      />

      <el-pagination
        v-if="activeType"
        class="list-pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑配置' : '新增配置'"
      width="560px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option
              v-for="t in typeList"
              :key="t.code"
              :label="t.alias || t.code"
              :value="t.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="全局唯一，如 system_config_name" />
        </el-form-item>
        <el-form-item label="别名" prop="alias">
          <el-input v-model="form.alias" placeholder="请输入别名" />
        </el-form-item>
        <el-form-item label="配置值" prop="value">
          <div class="value-editor">
            <div class="value-preview" :class="{ 'is-empty': !form.value }" @click="openZoomEditor">
              <pre class="value-preview-text">{{ form.value || '点击填写配置值，支持 JSON 格式' }}</pre>
              <span class="value-preview-zoom">
                <el-icon :size="12"><FullScreen /></el-icon>
                放大编辑
              </span>
            </div>
            <div class="value-editor-actions">
              <span class="value-editor-hint">点击上方配置值区域可放大编辑</span>
              <div class="value-editor-btns">
                <button type="button" class="json-btn" @click="openJsonEditor">
                  <span class="json-btn-icon">{ }</span>
                  JSON 表单编辑
                </button>
                <el-popover
                  v-model:visible="copyVisible"
                  trigger="click"
                  width="340"
                  placement="top-end"
                  @show="handleCopyShow"
                >
                  <template #reference>
                    <button type="button" class="json-btn json-btn--copy">
                      <span class="json-btn-icon">
                        <el-icon :size="12"><CopyDocument /></el-icon>
                      </span>
                      复制配置
                    </button>
                  </template>
                  <div class="copy-pop">
                    <el-input
                      v-model="copyKeyword"
                      size="small"
                      placeholder="搜索名称 / 别名"
                      clearable
                    />
                    <div v-loading="copyLoading" class="copy-list">
                      <button
                        v-for="item in copyOptions"
                        :key="item.id"
                        type="button"
                        class="copy-item"
                        @click="handleCopyItem(item as ConfigListItem)"
                      >
                        <span class="copy-item-alias">{{ item.alias || item.name }}</span>
                        <span class="copy-item-name">{{ item.name }}</span>
                      </button>
                      <el-empty
                        v-if="!copyLoading && copyOptions.length === 0"
                        :description="form.type ? '暂无可复制的配置' : '请先选择类型'"
                        :image-size="60"
                      />
                    </div>
                    <div class="copy-tip">点击条目将复制其配置值到当前表单</div>
                  </div>
                </el-popover>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" controls-position="right" />
          <span class="sort-hint">数值越小越靠前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="zoomVisible"
      title="编辑配置值"
      width="760px"
      append-to-body
      destroy-on-close
    >
      <el-input
        v-model="zoomValue"
        type="textarea"
        :autosize="{ minRows: 16, maxRows: 26 }"
        class="zoom-textarea"
        placeholder="请输入配置值，支持 JSON 格式"
      />
      <template #footer>
        <div class="zoom-footer">
          <button type="button" class="json-btn" @click="openJsonEditor">
            <span class="json-btn-icon">{ }</span>
            JSON 表单编辑
          </button>
          <div>
            <el-button @click="zoomVisible = false">取消</el-button>
            <el-button type="primary" @click="handleZoomConfirm">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <JsonEditorDialog
      v-model="jsonDialogVisible"
      :source="jsonSource"
      @confirm="handleJsonConfirm"
    />
  </div>
</template>

<style scoped lang="scss">
.sort-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.value-editor {
  width: 100%;
}

// 配置值预览框：点击放大编辑
.value-preview {
  position: relative;
  width: 100%;
  min-height: 92px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-7);

    .value-preview-zoom {
      opacity: 1;
    }
  }
}

.value-preview-text {
  margin: 0;
  padding: 10px 12px;
  max-height: 168px;
  overflow: auto;
  color: var(--el-color-primary);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.is-empty .value-preview-text {
  color: var(--el-text-color-placeholder);
}

.value-preview-zoom {
  position: absolute;
  top: 6px;
  right: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

// 放大编辑弹窗
.zoom-textarea {
  :deep(.el-textarea__inner) {
    font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
    font-size: 13px;
    line-height: 1.7;
  }
}

.zoom-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.value-editor-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
}

.value-editor-hint {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.value-editor-btns {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.json-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 30px;
  padding: 0 14px 0 7px;
  border: 1px solid transparent;
  border-radius: 15px;
  background: linear-gradient(135deg, #8a63d2 0%, #6f42c1 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.5px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(111, 66, 193, 0.25);
  transition: all 0.2s;

  &:hover,
  &:focus-visible {
    outline: none;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(111, 66, 193, 0.4);
    filter: brightness(1.06);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 2px 6px rgba(111, 66, 193, 0.25);
  }
}

.json-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

// 复制配置：描边风格，与主按钮成对
.json-btn--copy {
  background: var(--el-bg-color);
  border-color: color-mix(in srgb, #6f42c1 45%, transparent);
  color: #6f42c1;
  box-shadow: none;

  .json-btn-icon {
    background: color-mix(in srgb, #6f42c1 10%, transparent);
    color: #6f42c1;
  }

  &:hover,
  &:focus-visible {
    background: color-mix(in srgb, #6f42c1 6%, transparent);
    box-shadow: 0 2px 8px rgba(111, 66, 193, 0.18);
    filter: none;
  }
}

// 复制配置弹层
.copy-pop {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.copy-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 264px;
  overflow: auto;
}

.copy-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 7px 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;

  &:hover {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-7);
  }
}

.copy-item-alias {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 500;
}

.copy-item-name {
  color: var(--el-text-color-secondary);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-tip {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  text-align: center;
}

.type-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 40px;
}

.type-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-regular);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }

  &.active {
    color: #fff;
    border-color: var(--el-color-primary);
    background: var(--el-color-primary);
    box-shadow: 0 2px 8px var(--el-color-primary-light-5);
  }
}

.type-item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary-light-5);
  transition: background 0.2s;

  .active & {
    background: rgba(255, 255, 255, 0.9);
  }
}

.value-code {
  display: block;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.json-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.json-tag {
  align-self: flex-start;
}

.value-json {
  margin: 0;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
  font-family: var(--el-font-family-mono, 'SFMono-Regular', Consolas, monospace);
  font-size: 12px;
  line-height: 1.6;
  max-height: 120px;
  overflow: auto;
  white-space: pre;
}

.guide-empty {
  padding: 32px 0;
}
</style>
