<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearGrayVersion, getGrayVersion, setGrayVersion } from '@/utils/gray'
import { logoutToLogin } from '@/utils/logout'

/** 当前灰度标识（空串表示未开启灰度测试） */
const grayVersion = ref(getGrayVersion())

/** 开启灰度：录入灰度标识，全局请求头携带 X-Version */
async function handleEnable() {
  let version = ''
  try {
    const { value } = await ElMessageBox.prompt(
      '开启后所有后端请求将携带灰度标识并路由到对应灰度服务，需重新登录',
      '灰度测试',
      {
        confirmButtonText: '开启',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入灰度标识',
        inputValidator: (v: string) => !!v?.trim() || '灰度标识不能为空'
      }
    )
    version = value.trim()
  } catch {
    // 用户取消录入
    return
  }
  setGrayVersion(version)
  grayVersion.value = version
  ElMessage.success(`灰度测试已开启：${version}`)
  await logoutToLogin()
}

/** 取消灰度：清除标识，恢复正式链路 */
async function handleDisable() {
  try {
    await ElMessageBox.confirm(
      `确定取消灰度测试（${grayVersion.value}）吗？`,
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
  clearGrayVersion()
  grayVersion.value = ''
  ElMessage.success('灰度测试已取消')
  await logoutToLogin()
}
</script>

<template>
  <div class="gray-release">
    <!-- 灰度开启时点击标识即可取消灰度 -->
    <el-tag
      v-if="grayVersion"
      type="warning"
      effect="dark"
      size="large"
      class="gray-tag"
      @click="handleDisable"
    >
      灰度测试：{{ grayVersion }}
    </el-tag>
    <el-button v-else size="large" type="warning" plain @click="handleEnable">
      灰度测试
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.gray-release {
  display: flex;
  gap: 8px;
  align-items: center;
}

// 标识与按钮统一宽度，与环境标识外观一致
.gray-tag,
.gray-release :deep(.el-button) {
  justify-content: center;
  min-width: 132px;
}

.gray-tag {
  // 与 large 按钮同高（el-tag large 默认 32px）
  height: 40px;
  max-width: 220px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;

  &:hover {
    opacity: 0.8;
  }

  :deep(.el-tag__content) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
