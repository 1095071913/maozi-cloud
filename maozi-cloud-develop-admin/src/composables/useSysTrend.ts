import {reactive} from 'vue'
import {api, type SysDynamicInfo} from '../api'

/** 近 5 分钟趋势窗口：1 秒采样 × 300 点 */
export const TREND_POINTS = 300

/**
 * 系统动态采样单例：模块级状态，组件卸载不清除。
 * 页面用 v-if 切换会导致组件销毁、历史丢失，因此采样器首次使用时启动后
 * 常驻后台（每秒一次轻量 IPC），保证趋势窗口跨页面连续；
 * 组件实时显示（圆环/明细）也直接复用 latest，避免双重轮询。
 */
const state = reactive({
  /** 最近一次动态数据（供实时显示绑定） */
  latest: null as SysDynamicInfo | null,
  /** 三条曲线共享的采样时间戳(ms) */
  times: [] as number[],
  /** CPU 使用率序列（%） */
  cpu: [] as number[],
  /** 首块 GPU 利用率序列（%，不支持采集时为 null） */
  gpu: [] as Array<number | null>,
  /** 内存使用率序列（%） */
  mem: [] as number[]
})

/** 采样定时器（启动后常驻，不清除） */
let timer: ReturnType<typeof setInterval> | null = null

/** 追加采样值，超出 TREND_POINTS 时丢弃最旧的点 */
function trim<T>(arr: T[], v: T): void {
  arr.push(v)
  if (arr.length > TREND_POINTS) arr.splice(0, arr.length - TREND_POINTS)
}

/** 拉取一次动态数据，更新 latest 与各趋势序列（失败时跳过本次） */
async function sample(): Promise<void> {
  const r = await api.sysinfo.dynamic()
  if (!r.ok || !r.data) return
  state.latest = r.data
  trim(state.cpu, r.data.cpu.usage)
  trim(state.gpu, r.data.gpuUsages?.[0] ?? null)
  trim(state.mem, r.data.mem.usage)
  trim(state.times, Date.now())
}

/** 首次调用启动采样器（幂等），返回常驻响应式状态 */
export function useSysTrend() {
  if (!timer) {
    timer = setInterval(sample, 1000)
    void sample()
  }
  return state
}
