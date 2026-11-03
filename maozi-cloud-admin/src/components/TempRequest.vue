<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  clearTempRequestUrl,
  getTempRequestUrl,
  setTempRequestUrl
} from '@/utils/tempRequest'
import { logoutToLogin } from '@/utils/logout'

/** 当前临时请求地址（空串表示未启用临时请求） */
const tempUrl = ref(getTempRequestUrl())

/** 校验请求地址：非空且以 http(s):// 开头 */
function validateUrl(v: string): string | boolean {
  const value = v?.trim() || ''
  if (!value) return '请求地址不能为空'
  if (!/^https?:\/\/.+/.test(value)) return '地址需以 http:// 或 https:// 开头'
  return true
}

/** 启用临时请求：录入地址后所有后端请求改走该地址 */
async function handleEnable() {
  let url = ''
  try {
    const { value } = await ElMessageBox.prompt(
      '开启后所有后端请求将改走该地址，需重新登录',
      '临时请求',
      {
        confirmButtonText: '开启',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：http://localhost:1000/test',
        inputValidator: validateUrl
      }
    )
    url = value.trim()
  } catch {
    // 用户取消录入
    return
  }
  setTempRequestUrl(url)
  tempUrl.value = url
  ElMessage.success('临时请求已开启')
  await logoutToLogin()
}

/** 还原临时请求：清除地址，恢复默认请求地址 */
async function handleRestore() {
  try {
    await ElMessageBox.confirm(
      `确定还原临时请求（${tempUrl.value}）吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    // 用户取消操作
    return
  }
  clearTempRequestUrl()
  tempUrl.value = ''
  ElMessage.success('临时请求已还原')
  await logoutToLogin()
}
</script>

<template>
  <div class="temp-request">
    <el-button
      v-if="tempUrl"
      size="large"
      type="primary"
      plain
      class="temp-button"
      @click="handleRestore"
    >
      <span class="temp-url">{{ tempUrl }}</span>
    </el-button>
    <el-button v-else size="large" type="primary" plain @click="handleEnable">
      临时请求
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.temp-request {
  display: flex;
  align-items: center;
}

.temp-button {
  justify-content: center;
  min-width: 132px;
}

// 地址过长时省略号截断
.temp-url {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
