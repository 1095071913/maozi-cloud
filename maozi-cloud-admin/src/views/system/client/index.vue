<script setup lang="ts">
// 客户端管理页：客户端的分页查询、新增/编辑、启用禁用与删除
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getClientList,
  getClientDetail,
  saveClient,
  updateClient,
  updateClientStatus,
  removeClient,
  type ClientListParams
} from '@/api/client'
import { useUserStore } from '@/store/modules/user'
import { AuthType, type ClientListItem, type ClientSaveParam } from '@/types/api'

const userStore = useUserStore()
/** 是否有编辑权限（无则状态列只读展示） */
const canUpdate = computed(() => userStore.permissions.includes('system:client:update'))

// ============ 列表 ============
const list = ref<ClientListItem[]>([])
const loading = ref(false)
const query = reactive<Pick<ClientListParams, 'name'>>({ name: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })

async function loadList() {
  loading.value = true
  try {
    const res = await getClientList({
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

async function handleStatusChange(row: ClientListItem) {
  try {
    await updateClientStatus(row.id, row.status)
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch {
    // 失败时回滚开关状态
    row.status = row.status === 1 ? 0 : 1
  }
}

async function handleRemove(row: ClientListItem) {
  await ElMessageBox.confirm(`确定删除客户端「${row.clientName}」吗？`, '提示', {
    type: 'warning'
  })
  await removeClient(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ============ 新增/编辑 ============
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editId = ref<string | number>('')
const formRef = ref<FormInstance>()

/** 授权模式下拉选项 */
const grantTypeOptions = [
  { value: AuthType.AUTHORIZATION_CODE, label: '授权码模式' },
  { value: AuthType.CLIENT_CREDENTIALS, label: '客户端模式' },
  { value: AuthType.REFRESH_TOKEN, label: '刷新令牌模式' },
  { value: AuthType.PASSWORD, label: '密码模式' }
]

/** 授权模式值转展示文案 */
const grantTypeText = (v: number) =>
  grantTypeOptions.find((o) => o.value === v)?.label || String(v)

/** 表单默认值（新增场景） */
const defaultForm = (): ClientSaveParam => ({
  name: '',
  clientSecret: '',
  accessTokenValiditySeconds: '7200',
  refreshTokenValiditySeconds: '604800',
  authorizationGrantTypes: [AuthType.PASSWORD],
  remark: '',
  status: 1
})
const form = reactive(defaultForm())

const rules = computed<FormRules<typeof form>>(() => ({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  // 编辑时密钥可留空（留空表示不修改），仅新增时必填
  clientSecret: isEdit.value
    ? []
    : [{ required: true, message: '请输入客户端密钥', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  authorizationGrantTypes: [{ required: true, message: '请选择授权模式', trigger: 'change' }]
}))

function handleAdd() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: ClientListItem) {
  isEdit.value = true
  editId.value = row.id
  const res = await getClientDetail(row.id)
  const d = res.data
  Object.assign(form, {
    name: d.name,
    clientSecret: '',
    accessTokenValiditySeconds: d.accessTokenValiditySeconds || '7200',
    refreshTokenValiditySeconds: d.refreshTokenValiditySeconds || '604800',
    authorizationGrantTypes: d.authorizationGrantTypes ? [...d.authorizationGrantTypes] : [],
    remark: d.remark || '',
    status: d.status
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
        await updateClient(editId.value, form)
        ElMessage.success('更新成功')
      } else {
        await saveClient(form)
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
    <el-card shadow="never">
      <template #header>
        <div class="page-card-header">
          <div class="header-left">
            <span class="card-title">客户端列表</span>
            <span class="card-subtitle">共 {{ pagination.total }} 条</span>
          </div>
          <el-button v-auth="'system:client:save'" type="primary" @click="handleAdd">新增</el-button>
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
        <el-table-column label="名称" prop="clientName" min-width="140" />
        <el-table-column label="客户端ID" prop="clientId" min-width="180" show-overflow-tooltip />
        <el-table-column label="令牌有效期(秒)" min-width="130">
          <template #default="{ row }">{{ row.accessTokenValiditySeconds || '-' }}</template>
        </el-table-column>
        <el-table-column label="刷新有效期(秒)" min-width="130">
          <template #default="{ row }">{{ row.refreshTokenValiditySeconds || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="canUpdate"
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row as ClientListItem)"
            />
            <span v-else :style="{ color: row.status === 1 ? '#67c23a' : '#909399' }">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-auth="'system:client:update'" link type="primary" @click="handleEdit(row as ClientListItem)">编辑</el-button>
            <el-button v-auth="'system:client:remove'" link type="danger" @click="handleRemove(row as ClientListItem)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑客户端' : '新增客户端'"
      width="560px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="客户端密钥" prop="clientSecret">
          <el-input
            v-model="form.clientSecret"
            :placeholder="isEdit ? '留空则不修改' : '请输入客户端密钥'"
          />
        </el-form-item>
        <el-form-item label="授权模式" prop="authorizationGrantTypes">
          <el-select
            v-model="form.authorizationGrantTypes"
            multiple
            placeholder="请选择授权模式"
            style="width: 100%"
          >
            <el-option v-for="o in grantTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="令牌有效期">
          <el-input v-model="form.accessTokenValiditySeconds" placeholder="秒，默认 7200">
            <template #append>秒</template>
          </el-input>
        </el-form-item>
        <el-form-item label="刷新令牌有效期">
          <el-input v-model="form.refreshTokenValiditySeconds" placeholder="秒，默认 604800">
            <template #append>秒</template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
