import {defineStore} from 'pinia'
import {ref} from 'vue'
import {getSystemInfo} from '@/api/system'
import type {SystemInfo} from '@/types/api'

/** 应用全局 store：维护系统详情（项目信息） */
export const useAppStore = defineStore('app', () => {
  /** 系统详情（项目信息） */
  const systemInfo = ref<SystemInfo | null>(null)

  /** 拉取系统详情 */
  async function fetchSystemInfo() {
    const res = await getSystemInfo()
    systemInfo.value = res.data
    return res.data
  }

  /** 重置 */
  function reset() {
    systemInfo.value = null
  }

  return { systemInfo, fetchSystemInfo, reset }
})
