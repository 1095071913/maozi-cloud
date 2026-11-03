<script setup lang="ts">
// 角色管理页：角色列表（前端关键字过滤）、新增/编辑并勾选权限树、启用禁用与删除
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getRoleList,
  getRoleDetail,
  saveRole,
  updateRole,
  updateRoleStatus,
  removeRole
} from '@/api/role'
import { getPermissionDropDownList } from '@/api/permission'
import { useUserStore } from '@/store/modules/user'
import { buildFullPermissionTree } from '@/utils/permission'
import {
  PermissionType,
  type PermissionOptionItem,
  type RoleListItem,
  type RoleSaveParam
} from '@/types/api'

const userStore = useUserStore()
const canUpdate = computed(() => userStore.permissions.includes('system:role:update'))

// ============ 列表 ============
const list = ref<RoleListItem[]>([])
const loading = ref(false)
const keyword = ref('')

const filteredList = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return list.value
  return list.value.filter(
    (r) => r.name.includes(kw) || (r.description || '').includes(kw)
  )
})

async function loadList() {
  loading.value = true
  try {
    const res = await getRoleList()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

// ============ 状态切换 ============
async function handleStatusChange(row: RoleListItem) {
  try {
    await updateRoleStatus(row.id, row.status)
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

// ============ 删除 ============
async function handleRemove(row: RoleListItem) {
  await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '提示', {
    type: 'warning'
  })
  await removeRole(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ============ 新增/编辑 ============
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<string | number>('')
const formRef = ref<FormInstance>()
const treeRef = ref()
const originPermissionIds = ref<(string | number)[]>([])

const permissionList = ref<PermissionOptionItem[]>([])
const permissionTree = computed(() => buildFullPermissionTree(permissionList.value))

const defaultForm = (): RoleSaveParam => ({
  name: '',
  description: '',
  status: 1
})
const form = reactive(defaultForm())
const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const treeProps = { label: 'name', children: 'children' }

async function loadPermissions() {
  if (permissionList.value.length) return
  const res = await getPermissionDropDownList()
  permissionList.value = res.data || []
}

async function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  originPermissionIds.value = []
  dialogVisible.value = true
  await loadPermissions()
  await nextTick()
  treeRef.value?.setCheckedKeys([])
}

async function handleEdit(row: RoleListItem) {
  isEdit.value = true
  editId.value = row.id
  const res = await getRoleDetail(row.id)
  const d = res.data
  Object.assign(form, {
    name: d.name,
    description: d.description || '',
    status: d.status
  })
  originPermissionIds.value = d.permissionIds ? [...d.permissionIds] : []
  dialogVisible.value = true
  await loadPermissions()
  await nextTick()
  treeRef.value?.setCheckedKeys([...originPermissionIds.value])
}

function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const checked = (treeRef.value?.getCheckedKeys() || []) as (string | number)[]
      const halfChecked = (treeRef.value?.getHalfCheckedKeys() || []) as (
        | string
        | number
      )[]
      // 勾选的权限全集（半选的父节点也计入）
      const currentSelected = new Set([...checked, ...halfChecked])
      // 与打开弹窗时的权限做差集：新增勾选的走绑定，取消勾选的走解绑
      const bindPermissionIds = [...currentSelected].filter(
        (id) => !originPermissionIds.value.includes(id)
      )
      const unbindPermissionIds = originPermissionIds.value.filter(
        (id) => !currentSelected.has(id)
      )

      // 编辑时按差集增量绑定/解绑，新增时直接绑定全量勾选
      if (isEdit.value) {
        await updateRole(editId.value, {
          ...form,
          bindPermissionIds,
          unbindPermissionIds
        })
        ElMessage.success('更新成功')
      } else {
        await saveRole({ ...form, bindPermissionIds: [...currentSelected] })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitting.value = false
    }
  })
}

function typeText(t: PermissionType) {
  return { [PermissionType.DIRECTORY]: '目录', [PermissionType.MENU]: '菜单', [PermissionType.BUTTON]: '按钮' }[t]
}

onMounted(loadList)
</script>

<template>
  <div class="list-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-card-header">
          <div class="header-left">
            <span class="card-title">角色列表</span>
            <span class="card-subtitle">共 {{ filteredList.length }} 条</span>
          </div>
          <el-button v-auth="'system:role:save'" type="primary" @click="handleAdd">新增</el-button>
        </div>
      </template>

      <div class="list-toolbar">
        <el-form inline @submit.prevent>
          <el-form-item label="名称">
            <el-input v-model="keyword" placeholder="请输入名称/描述" clearable />
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="filteredList" border stripe>
        <el-table-column label="名称" prop="name" min-width="140" />
        <el-table-column label="描述" prop="description" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="canUpdate"
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row as RoleListItem)"
            />
            <span v-else :style="{ color: row.status === 1 ? '#67c23a' : '#909399' }">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="'system:role:update'" link type="primary" @click="handleEdit(row as RoleListItem)">编辑</el-button>
            <el-button v-auth="'system:role:remove'" link type="danger" @click="handleRemove(row as RoleListItem)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="560px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="权限">
          <div class="perm-tree-wrap">
            <el-tree
              ref="treeRef"
              :data="permissionTree"
              :props="treeProps"
              node-key="id"
              show-checkbox
              check-strictly
              default-expand-all
            >
              <template #default="{ data }">
                <span>{{ data.name }}</span>
                <el-tag size="small" type="info" class="node-tag">
                  {{ typeText(data.type) }}
                </el-tag>
              </template>
            </el-tree>
          </div>
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
.perm-tree-wrap {
  max-height: 320px;
  overflow: auto;
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
}

.node-tag {
  margin: 0 8px;
}
</style>
