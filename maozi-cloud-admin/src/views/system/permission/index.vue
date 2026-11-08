<script setup lang="ts">
// 权限管理页：树表展示目录/菜单/按钮层级，支持新增子级、编辑与删除
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getPermissionList,
  getPermissionDetail,
  savePermission,
  updatePermission,
  removePermission
} from '@/api/permission'
import { buildFullPermissionTree, buildPermissionLevelMap } from '@/utils/permission'
import {
  PermissionType,
  type PermissionItem,
  type PermissionSaveParam
} from '@/types/api'

// ============ 列表（树表） ============
const rawList = ref<PermissionItem[]>([])
const loading = ref(false)
const treeData = computed(() => buildFullPermissionTree(rawList.value))

/** 树表默认仅展开目录节点，菜单/按钮保持折叠 */
const expandRowKeys = computed(() =>
  rawList.value
    .filter((item) => item.type === PermissionType.DIRECTORY)
    .map((item) => String(item.id))
)

async function loadList() {
  loading.value = true
  try {
    const res = await getPermissionList()
    rawList.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 权限类型标签映射（表格展示用） */
const typeTagMap: Record<number, { text: string; type: 'warning' | 'success' | 'info' }> = {
  [PermissionType.DIRECTORY]: { text: '目录', type: 'warning' },
  [PermissionType.MENU]: { text: '菜单', type: 'success' },
  [PermissionType.BUTTON]: { text: '按钮', type: 'info' }
}

// ============ 新增/编辑 ============
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<string | number>('')
const formRef = ref<FormInstance>()

/** 类型单选选项 */
const typeOptions = [
  { value: PermissionType.DIRECTORY, label: '目录' },
  { value: PermissionType.MENU, label: '菜单' },
  { value: PermissionType.BUTTON, label: '按钮' }
]

/** 表单默认值（新增场景） */
const defaultForm = (): PermissionSaveParam => ({
  name: '',
  mark: '',
  type: PermissionType.MENU,
  parentId: '',
  icon: '',
  route: '',
  serviceUri: '',
  sort: 99,
  level: 0
})
const form = reactive(defaultForm())

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  mark: [{ required: true, message: '请输入标识', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  parentId: [{ required: true, message: '请选择上级', trigger: 'change' }]
}

/**
 * 上级可选树，按当前权限类型过滤：
 * - 目录：仅可挂在根目录下，只展示“根目录”
 * - 菜单：上级只显示目录类型
 * - 按钮：上级显示目录与菜单（不含按钮）
 */
const parentTreeData = computed(() => {
  if (form.type === PermissionType.DIRECTORY) {
    return [
      { id: '0', name: '根目录', type: -1, parentId: '0', mark: '', icon: '', children: [] }
    ]
  }
  const allowed =
    form.type === PermissionType.MENU
      ? [PermissionType.DIRECTORY]
      : [PermissionType.DIRECTORY, PermissionType.MENU]
  return buildFullPermissionTree(
    rawList.value.filter((item) => allowed.includes(item.type))
  )
})

/** 类型变化时校验已选上级是否仍合法，不合法则重置，避免提交无效上级 */
watch(
  () => form.type,
  (newType) => {
    if (newType === PermissionType.DIRECTORY) {
      form.parentId = '0'
      return
    }
    const allowed =
      newType === PermissionType.MENU
        ? [PermissionType.DIRECTORY]
        : [PermissionType.DIRECTORY, PermissionType.MENU]
    const current = rawList.value.find((i) => String(i.id) === String(form.parentId))
    if (!current || !allowed.includes(current.type)) {
      form.parentId = ''
    }
  }
)

/** 当前所选上级对应的深度 → 新节点 level */
const levelMap = computed(() => buildPermissionLevelMap(rawList.value))
const computedLevel = computed(() => {
  const pid = form.parentId
  if (pid === '0' || pid === 0) return 0
  const parentLevel = levelMap.value.get(pid)
  return parentLevel === undefined ? 0 : parentLevel + 1
})

const treeProps = { label: 'name', children: 'children' }

function handleAdd(parent?: PermissionItem) {
  isEdit.value = false
  Object.assign(form, defaultForm())
  if (parent) {
    form.parentId = parent.id
    // 按上级类型推导子级默认类型：菜单的子级只能是按钮，目录的子级默认菜单，
    // 确保所选上级存在于可选树中，回显名称而非 ID
    form.type =
      parent.type === PermissionType.MENU ? PermissionType.BUTTON : PermissionType.MENU
  }
  dialogVisible.value = true
}

async function handleEdit(row: PermissionItem) {
  isEdit.value = true
  editId.value = row.id
  const res = await getPermissionDetail(row.id)
  const d = res.data
  Object.assign(form, {
    name: d.name,
    mark: d.mark,
    type: d.type,
    parentId: String(d.parentId ?? '0'),
    icon: d.icon || '',
    route: d.route || '',
    serviceUri: d.serviceUri || '',
    sort: d.sort ?? 99,
    level: d.level ?? 0
  })
  dialogVisible.value = true
}

function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = { ...form, level: computedLevel.value }
      if (isEdit.value) {
        await updatePermission(editId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await savePermission(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitting.value = false
    }
  })
}

async function handleRemove(row: PermissionItem) {
  await ElMessageBox.confirm(`确定删除权限「${row.name}」吗？`, '提示', {
    type: 'warning'
  })
  await removePermission(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="list-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-card-header">
          <div class="header-left">
            <span class="card-title">权限列表</span>
            <span class="card-subtitle">共 {{ rawList.length }} 条</span>
          </div>
          <el-button v-auth="'system:permission:save'" type="primary" @click="handleAdd()">
            新增
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        border
        stripe
        :expand-row-keys="expandRowKeys"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column label="名称" prop="name" min-width="200" />
        <el-table-column label="标识(mark)" prop="mark" min-width="180" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type]?.type" size="small">
              {{ typeTagMap[row.type]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图标" prop="icon" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.type !== PermissionType.BUTTON" v-auth="'system:permission:save'" link type="primary" @click="handleAdd(row as PermissionItem)">新增子级</el-button>
            <el-button v-auth="'system:permission:update'" link type="primary" @click="handleEdit(row as PermissionItem)">编辑</el-button>
            <el-button v-auth="'system:permission:remove'" link type="danger" @click="handleRemove(row as PermissionItem)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑权限' : '新增权限'"
      width="600px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTreeData"
            :props="treeProps"
            node-key="id"
            check-strictly
            default-expand-all
            placeholder="请选择上级"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="标识" prop="mark">
          <el-input v-model="form.mark" placeholder="如 system:user" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标 URL 或标识" />
        </el-form-item>
        <el-form-item label="路由">
          <el-input v-model="form.route" placeholder="前端路由路径" />
        </el-form-item>
        <el-form-item label="服务地址">
          <el-input v-model="form.serviceUri" placeholder="后端服务 URI" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" controls-position="right" />
          <span class="hint">深度：{{ computedLevel }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
