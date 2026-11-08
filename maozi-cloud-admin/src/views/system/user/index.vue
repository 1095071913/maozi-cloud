<script setup lang="ts">
// 账号管理页：用户分页查询、新增/编辑（含角色绑定）、启用禁用与删除
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getUserList,
  getUserDetail,
  saveUser,
  updateUser,
  updateUserStatus,
  removeUser,
  type UserListParams
} from '@/api/user'
import { getClientDropdown, getRoleDropdown } from '@/api/common'
import { useUserStore } from '@/store/modules/user'
import { Status, type OptionItem, type UserListItem, type UserSaveParam } from '@/types/api'

const userStore = useUserStore()
/** 是否有更新权限（决定状态列展示开关还是纯文本） */
const canUpdate = computed(() =>
  userStore.permissions.includes('system:user:update')
)

// ============ 列表 ============
const list = ref<UserListItem[]>([])
const loading = ref(false)
const query = reactive<Pick<UserListParams, 'name'>>({ name: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })

async function loadList() {
  loading.value = true
  try {
    const res = await getUserList({
      current: pagination.current,
      size: pagination.size,
      name: query.name
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

// ============ 状态切换 ============
async function handleStatusChange(row: UserListItem) {
  try {
    await updateUserStatus(row.id, row.status)
    ElMessage.success(row.status === Status.ENABLE ? '已启用' : '已禁用')
  } catch {
    // 失败回滚
    row.status = row.status === Status.ENABLE ? Status.DISABLE : Status.ENABLE
  }
}

// ============ 删除 ============
async function handleRemove(row: UserListItem) {
  await ElMessageBox.confirm(`确定删除用户「${row.name}」吗？`, '提示', {
    type: 'warning'
  })
  await removeUser(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ============ 新增/编辑 ============
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<string | number>('')
const formRef = ref<FormInstance>()
/** 打开弹窗时用户已有的角色 ID 列表（提交时据此计算解绑差集） */
const originRoleIds = ref<(string | number)[]>([])

/** 客户端/角色下拉选项 */
const clientOptions = ref<OptionItem[]>([])
const roleOptions = ref<OptionItem[]>([])

/** 表单默认值（新增场景） */
const defaultForm = (): UserSaveParam & { bindRoleIds: (string | number)[] } => ({
  username: '',
  name: '',
  password: '',
  icon: '',
  status: Status.ENABLE,
  clientId: '',
  bindRoleIds: []
})

const form = reactive(defaultForm())

const rules = computed<FormRules<typeof form>>(() => ({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  // 编辑时密码可留空（留空表示不修改），仅新增时必填
  password: isEdit.value
    ? []
    : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  clientId: [{ required: true, message: '请选择客户端', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}))

/** 并行拉取客户端与角色下拉选项 */
async function loadDropdowns() {
  const [c, r] = await Promise.all([getClientDropdown(), getRoleDropdown()])
  clientOptions.value = c.data || []
  roleOptions.value = r.data || []
}

async function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  originRoleIds.value = []
  dialogVisible.value = true
  if (clientOptions.value.length === 0) await loadDropdowns()
}

async function handleEdit(row: UserListItem) {
  isEdit.value = true
  editId.value = row.id
  const res = await getUserDetail(row.id)
  const d = res.data
  Object.assign(form, {
    username: d.username,
    name: d.name,
    icon: d.icon || '',
    password: '',
    status: d.status,
    clientId: d.client?.id ?? '',
    bindRoleIds: d.roleIds ? [...d.roleIds] : []
  })
  originRoleIds.value = d.roleIds ? [...d.roleIds] : []
  dialogVisible.value = true
  if (clientOptions.value.length === 0) await loadDropdowns()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        // 与打开弹窗时的角色做差集：取消勾选的角色走解绑
        const current = new Set(form.bindRoleIds)
        const bindRoleIds = form.bindRoleIds
        const unbindRoleIds = originRoleIds.value.filter((id) => !current.has(id))
        await updateUser(editId.value, {
          ...form,
          bindRoleIds,
          unbindRoleIds
        })
        ElMessage.success('更新成功')
      } else {
        await saveUser(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(loadList)
</script>

<template>
  <div class="list-page">
    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="page-card-header">
          <div class="header-left">
            <span class="card-title">账号列表</span>
            <span class="card-subtitle">共 {{ pagination.total }} 条</span>
          </div>
          <el-button v-auth="'system:user:save'" type="primary" @click="handleAdd">
            新增
          </el-button>
        </div>
      </template>

      <div class="list-toolbar">
        <el-form inline @submit.prevent>
          <el-form-item label="名称">
            <el-input
              v-model="query.name"
              placeholder="请输入名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="名称" prop="name" min-width="120" />
        <el-table-column label="客户端" min-width="120">
          <template #default="{ row }">{{ row.client?.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="canUpdate"
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row as UserListItem)"
            />
            <span v-else :style="{ color: row.status === 1 ? '#67c23a' : '#909399' }">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-auth="'system:user:update'"
              link
              type="primary"
              @click="handleEdit(row as UserListItem)"
            >
              编辑
            </el-button>
            <el-button
              v-auth="'system:user:remove'"
              link
              type="danger"
              @click="handleRemove(row as UserListItem)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="520px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="客户端" prop="clientId">
          <el-select v-model="form.clientId" placeholder="请选择客户端" style="width: 100%">
            <el-option
              v-for="o in clientOptions"
              :key="o.id"
              :label="o.name"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="form.bindRoleIds"
            multiple
            placeholder="请选择角色（可多选）"
            style="width: 100%"
          >
            <el-option
              v-for="o in roleOptions"
              :key="o.id"
              :label="o.name"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
