<script setup lang="ts">
// 个人中心页：当前登录用户维护头像、名称与登录密码
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { CameraFilled, UserFilled } from '@element-plus/icons-vue'
import { updateUserInfo } from '@/api/user'
import { uploadImage } from '@/api/image'
import { useUserStore } from '@/store/modules/user'
import type { UserIndividualUpdateParam } from '@/types/api'

const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const uploadingIcon = ref(false)

const defaultForm = (): UserIndividualUpdateParam => ({
  name: '',
  icon: '',
  password: '',
  newPassword: ''
})

const form = reactive(defaultForm())

/** 旧密码与新密码必须成对填写：填了其中一个，另一个也必填 */
const pairedPasswordValidator = (field: 'password' | 'newPassword') => (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const other = field === 'password' ? form.newPassword : form.password
  if (value && !other) {
    callback(new Error(field === 'password' ? '请同时填写新密码' : '请同时填写旧密码'))
  } else {
    callback()
  }
}

const rules = computed<FormRules<typeof form>>(() => ({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  password: [{ validator: pairedPasswordValidator('password'), trigger: 'blur' }],
  newPassword: [{ validator: pairedPasswordValidator('newPassword'), trigger: 'blur' }]
}))

/** 用当前登录用户信息回显表单 */
function fillForm() {
  Object.assign(form, defaultForm(), {
    name: userStore.name,
    icon: userStore.icon
  })
}

function handleReset() {
  fillForm()
  formRef.value?.clearValidate()
}

/** 头像上传前校验：仅允许图片类型，且不超过 5MB */
function beforeIconUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('仅支持上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

/** 自定义上传：调用图片上传接口，取返回的 url 作为头像地址 */
async function handleIconUpload(options: UploadRequestOptions) {
  uploadingIcon.value = true
  try {
    const res = await uploadImage('icon', [options.file])
    const url = res.data?.[0]?.url
    if (!url) {
      ElMessage.error('上传成功但未返回图片地址')
      return
    }
    form.icon = url
    ElMessage.success('头像上传成功，保存后生效')
  } finally {
    uploadingIcon.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      // 密码成对留空则不提交密码字段，表示不修改密码
      const changePassword = !!(form.password && form.newPassword)
      await updateUserInfo({
        name: form.name,
        icon: form.icon,
        password: changePassword ? form.password : undefined,
        newPassword: changePassword ? form.newPassword : undefined
      })
      ElMessage.success('保存成功')
      form.password = ''
      form.newPassword = ''
      formRef.value?.clearValidate()
      // 刷新顶栏展示的名称与头像
      await userStore.fetchUserInfo()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(fillForm)
</script>

<template>
  <div class="individual-page">
    <el-card shadow="never" class="profile-card">
      <!-- 顶部品牌渐变横幅 -->
      <div class="profile-banner">
        <div class="banner-text">
          <div class="banner-title">个人信息</div>
          <div class="banner-sub">维护您的头像、名称与登录密码</div>
        </div>
      </div>

      <div class="profile-body">
        <!-- 头像：悬浮于横幅边缘，悬停出现更换遮罩 -->
        <div class="avatar-wrap">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            accept="image/*"
            :before-upload="beforeIconUpload"
            :http-request="handleIconUpload"
          >
            <div
              v-loading="uploadingIcon"
              class="avatar-ring"
              element-loading-background="rgba(255, 255, 255, 0.6)"
            >
              <div class="avatar-inner">
                <el-avatar :size="90" :src="form.icon">
                  <el-icon :size="30"><UserFilled /></el-icon>
                </el-avatar>
                <div class="avatar-mask">
                  <el-icon :size="20"><CameraFilled /></el-icon>
                  <span>更换头像</span>
                </div>
              </div>
            </div>
          </el-upload>
        </div>

        <div class="profile-name">{{ form.name || '未设置名称' }}</div>
        <div class="profile-sub">
          <span class="perm-tag">权限 {{ userStore.permissions.length }} 项</span>
          <span class="profile-hint">支持 JPG / PNG 等图片，大小不超过 5MB</span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="profile-form">
          <div class="form-section">
            <div class="section-title"><span class="section-dot" />基本信息</div>
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入名称" />
            </el-form-item>
          </div>

          <div class="form-section">
            <div class="section-title"><span class="section-dot" />修改密码</div>
            <el-form-item label="旧密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                autocomplete="off"
                placeholder="不修改密码请留空"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="form.newPassword"
                type="password"
                show-password
                autocomplete="off"
                placeholder="不修改密码请留空"
              />
            </el-form-item>
          </div>
        </el-form>

        <div class="form-footer">
          <el-button type="primary" class="save-btn" :loading="submitting" @click="handleSubmit">
            保存修改
          </el-button>
          <el-button class="reset-btn" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.individual-page {
  display: flex;
  justify-content: center;
  padding: 24px 16px;
}

/* 卡片：overflow 隐藏以便横幅贴合圆角 */
.profile-card {
  width: 560px;
  max-width: 100%;
  overflow: hidden;
  border-radius: 12px;
  border-color: #eceef3;
  box-shadow: 0 2px 12px rgb(31 35 41 / 4%);

  :deep(.el-card__body) {
    padding: 0;
  }
}

/* 顶部品牌渐变横幅：白色标题 + 装饰圆 */
.profile-banner {
  position: relative;
  height: 108px;
  padding: 20px 24px;
  overflow: hidden;
  background: linear-gradient(120deg, #6366f1, #8b5cf6);

  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    background-color: rgb(255 255 255 / 12%);
  }

  &::before {
    width: 180px;
    height: 180px;
    top: -90px;
    right: -30px;
  }

  &::after {
    width: 120px;
    height: 120px;
    bottom: -70px;
    right: 110px;
  }

  .banner-text {
    position: relative;
    z-index: 1;
  }

  .banner-title {
    font-size: 17px;
    font-weight: 600;
    color: #fff;
  }

  .banner-sub {
    margin-top: 4px;
    font-size: 12px;
    color: rgb(255 255 255 / 80%);
  }
}

.profile-body {
  padding: 0 36px 30px;
}

/* 头像：悬浮于横幅边缘 */
.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-top: -52px;
}

.avatar-ring {
  box-sizing: border-box;
  width: 104px;
  height: 104px;
  padding: 4px;
  border-radius: 50%;
  cursor: pointer;
  background: #fff;
  box-shadow: 0 6px 18px rgb(99 102 241 / 28%);
}

.avatar-inner {
  position: relative;
  box-sizing: border-box;
  width: 96px;
  height: 96px;
  padding: 3px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);

  .avatar-mask {
    position: absolute;
    inset: 3px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    border-radius: 50%;
    color: #fff;
    font-size: 12px;
    background-color: rgb(0 0 0 / 45%);
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .avatar-mask {
    opacity: 1;
  }
}

.profile-name {
  margin-top: 10px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.profile-sub {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}

.perm-tag {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #6366f1;
  background-color: #eef2ff;
}

.profile-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 分区：品牌色圆点 + 标题 */
.form-section {
  margin-top: 26px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);

  .section-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
  }
}

.profile-form {
  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}

/* 底部按钮 */
.form-footer {
  display: flex;
  justify-content: center;
  margin-top: 28px;

  .save-btn {
    min-width: 120px;
    border: none;
    border-radius: 8px;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    box-shadow: 0 4px 12px rgb(99 102 241 / 30%);

    &:hover,
    &:focus {
      background: linear-gradient(135deg, #5558e8, #7c4df2);
    }
  }

  .reset-btn {
    min-width: 120px;
    border-radius: 8px;
  }
}
</style>
