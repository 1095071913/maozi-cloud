<template>
  <div class="trend-box" @mouseleave="hoverIdx = null">
    <svg
      class="mini-trend"
      :class="{ dark }"
      :viewBox="`0 0 100 ${height}`"
      :style="{ height: height + 'px' }"
      preserveAspectRatio="none"
      @mousemove="onMove"
    >
      <defs>
        <linearGradient :id="gid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" :stop-color="theme[0]" stop-opacity="0.32" />
          <stop offset="70%" :stop-color="theme[1]" stop-opacity="0.08" />
          <stop offset="100%" :stop-color="theme[1]" stop-opacity="0.01" />
        </linearGradient>
      </defs>

      <!-- 参考网格与底线 -->
      <line v-for="g in gridLines" :key="g" x1="0" :y1="yOf(g)" x2="100" :y2="yOf(g)" />
      <line class="base-line" x1="0" :y1="height - 0.5" x2="100" :y2="height - 0.5" />

      <!-- 面积 + 光晕层 + 主曲线（平滑贝塞尔） -->
      <path v-if="areaPath" :d="areaPath" :fill="`url(#${gid})`" />
      <path
        v-if="linePath"
        class="glow-line"
        :d="linePath"
        fill="none"
        :stroke="theme[0]"
        stroke-width="5"
        stroke-linecap="round"
        vector-effect="non-scaling-stroke"
      />
      <path
        v-if="linePath"
        class="main-line"
        :d="linePath"
        fill="none"
        :stroke="theme[1]"
        stroke-width="2"
        stroke-linecap="round"
        vector-effect="non-scaling-stroke"
      />

      <!-- 悬停：柔和光带 + 十字虚线 -->
      <template v-if="hoverPoint">
        <line
          class="hover-band"
          :x1="hoverPoint.x"
          :y1="0"
          :x2="hoverPoint.x"
          :y2="height"
          :stroke="theme[0]"
          stroke-width="1.6"
        />
        <line
          class="hover-line"
          :x1="hoverPoint.x"
          :y1="0"
          :x2="hoverPoint.x"
          :y2="height"
          vector-effect="non-scaling-stroke"
        />
      </template>
    </svg>

    <!-- 实时末端呼吸点（HTML 覆盖层，保证正圆不被横向拉伸） -->
    <div v-if="lastPoint" class="dot dot-live" :style="dotStyle(lastPoint.x, lastPoint.y)" />

    <!-- 悬停数据点 -->
    <div
      v-if="hoverPoint && hoverPoint.v !== null"
      class="dot dot-hover"
      :style="dotStyle(hoverPoint.x, hoverPoint.y)"
    />

    <!-- 毛玻璃气泡：时间 + 数值 -->
    <div v-if="hoverPoint" class="trend-tip" :style="tipStyle">
      <div class="tip-row">
        <span class="tip-dot" :style="{ background: theme[1] }"></span>
        <span class="tip-time">{{ hoverTime || '—' }}</span>
      </div>
      <div class="tip-value" :class="{ empty: hoverPoint.v === null }" :style="tipColor">
        {{ hoverValueText }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * 卡片底部迷你趋势图：平滑贝塞尔曲线 + 面积渐变 + 光晕 + 实时呼吸点。
 * null 值断开为留白段（如 GPU 不支持采集）；悬停显示光带、十字线与
 * 毛玻璃气泡（采样时间 + 数值），times 与 values 一一对应。
 */
const props = withDefaults(
  defineProps<{
    values: Array<number | null>
    /** 每个采样点的时间戳(ms)；缺省时气泡只显示数值 */
    times?: Array<number>
    theme?: [string, string]
    height?: number
    max?: number
    /** 深色背景模式：网格/底线换成半透明白 */
    dark?: boolean
  }>(),
  { theme: () => ['#60a5fa', '#2563eb'] as [string, string], height: 56, max: 100, dark: false }
)

const gid = `mt-${Math.random().toString(36).slice(2, 8)}`
const gridLines = [25, 50, 75]
const hoverIdx = ref<number | null>(null)

function xOf(i: number, n: number): number {
  return n > 1 ? (i / (n - 1)) * 100 : 100
}

function yOf(v: number): number {
  const ratio = Math.max(0, Math.min(1, v / props.max))
  return props.height - ratio * (props.height - 3) - 1.5
}

const segments = computed(() => {
  const n = props.values.length
  const segs: Array<Array<{ x: number; y: number }>> = []
  let cur: Array<{ x: number; y: number }> = []
  props.values.forEach((v, i) => {
    if (v === null || v === undefined || isNaN(v)) {
      if (cur.length) segs.push(cur)
      cur = []
      return
    }
    cur.push({ x: xOf(i, n), y: yOf(v) })
  })
  if (cur.length) segs.push(cur)
  return segs
})

/** Catmull-Rom 转三次贝塞尔：曲线平滑且严格经过每个采样点 */
function smoothPath(pts: Array<{ x: number; y: number }>): string {
  const f = (v: number): string => v.toFixed(2)
  if (pts.length < 3) return `M${pts.map((p) => `${f(p.x)},${f(p.y)}`).join('L')}`
  let d = `M${f(pts[0].x)},${f(pts[0].y)}`
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] ?? pts[i]
    const p1 = pts[i]
    const p2 = pts[i + 1]
    const p3 = pts[i + 2] ?? p2
    d +=
      `C${f(p1.x + (p2.x - p0.x) / 6)},${f(p1.y + (p2.y - p0.y) / 6)}` +
      ` ${f(p2.x - (p3.x - p1.x) / 6)},${f(p2.y - (p3.y - p1.y) / 6)}` +
      ` ${f(p2.x)},${f(p2.y)}`
  }
  return d
}

const linePath = computed(() =>
  segments.value
    .filter((s) => s.length > 1)
    .map((s) => smoothPath(s))
    .join(' ')
)

const areaPath = computed(() =>
  segments.value
    .filter((s) => s.length > 1)
    .map((s) => {
      const h = props.height
      return `M${s[0].x.toFixed(2)},${h}L${smoothPath(s).slice(1)}L${s[s.length - 1].x.toFixed(2)},${h}Z`
    })
    .join(' ')
)

const lastPoint = computed(() => {
  const n = props.values.length
  for (let i = n - 1; i >= 0; i--) {
    const v = props.values[i]
    if (v !== null && v !== undefined && !isNaN(v)) {
      return { x: xOf(i, n), y: yOf(v) }
    }
  }
  return null
})

const hoverPoint = computed(() => {
  const i = hoverIdx.value
  if (i === null) return null
  const n = props.values.length
  if (n === 0) return null
  const v = props.values[i]
  const valid = v !== null && v !== undefined && !isNaN(v)
  return { x: xOf(i, n), y: valid ? yOf(v) : props.height / 2, v: valid ? v : null }
})

const hoverTime = computed(() => {
  const i = hoverIdx.value
  const t = i !== null ? props.times?.[i] : undefined
  if (!t) return ''
  const d = new Date(t)
  const pad = (x: number): string => String(x).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

const hoverValueText = computed(() => {
  const v = hoverPoint.value?.v
  if (v === null || v === undefined) return '无数据'
  return `${Math.round(v * 10) / 10}%`
})

const tipColor = computed(() =>
  hoverPoint.value?.v === null ? {} : { color: props.theme[1] }
)

/** HTML 覆盖点样式：left 百分比 / top 像素（viewBox 高度与 CSS 像素 1:1） */
function dotStyle(x: number, y: number): Record<string, string> {
  return { '--c': props.theme[1], left: `${x}%`, top: `${y}px` }
}

/** 气泡定位：左右内收防溢出卡片，靠近顶部时压到网格中部以下 */
const tipStyle = computed(() => {
  const p = hoverPoint.value
  if (!p) return {}
  return {
    left: `${Math.max(14, Math.min(86, p.x))}%`,
    top: `${Math.max(30, p.y)}px`
  }
})

function onMove(e: MouseEvent): void {
  const box = e.currentTarget as SVGElement
  const rect = box.getBoundingClientRect()
  if (rect.width === 0) return
  const n = props.values.length
  if (n === 0) return
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  hoverIdx.value = Math.round(ratio * (n - 1))
}
</script>

<style scoped>
.trend-box {
  position: relative;
  cursor: crosshair;
}

.mini-trend {
  display: block;
  width: 100%;
}

.mini-trend line {
  stroke: #f1f5f9;
  stroke-width: 1;
  vector-effect: non-scaling-stroke;
}

.mini-trend.dark line {
  stroke: rgba(148, 163, 184, 0.12);
}

.mini-trend .base-line {
  stroke: #e2e8f0;
}

.mini-trend.dark .base-line {
  stroke: rgba(148, 163, 184, 0.22);
}

.mini-trend .glow-line {
  opacity: 0.22;
}

.mini-trend .hover-band {
  opacity: 0.09;
}

.mini-trend .hover-line {
  stroke: #94a3b8;
  stroke-width: 1;
  stroke-dasharray: 3 3;
  vector-effect: non-scaling-stroke;
}

/* ===== 数据点（HTML 覆盖层，保持正圆） ===== */
.dot {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--c);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--c) 15%, transparent);
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 2;
}

/* 实时末端呼吸光圈 */
.dot-live::after {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  border: 2px solid var(--c);
  animation: trend-pulse 2s ease-out infinite;
}

@keyframes trend-pulse {
  0% {
    transform: scale(0.7);
    opacity: 0.8;
  }
  70% {
    transform: scale(2.8);
    opacity: 0;
  }
  100% {
    transform: scale(2.8);
    opacity: 0;
  }
}

.dot-hover {
  width: 9px;
  height: 9px;
}

/* ===== 毛玻璃气泡 ===== */
.trend-tip {
  position: absolute;
  transform: translate(-50%, calc(-100% - 11px));
  min-width: 88px;
  padding: 7px 12px 8px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.09);
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.35);
  pointer-events: none;
  white-space: nowrap;
  text-align: center;
  z-index: 3;
  transition: left 0.1s ease-out, top 0.1s ease-out;
}

.trend-tip::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -3.5px;
  width: 7px;
  height: 7px;
  background: rgba(15, 23, 42, 0.9);
  border-right: 1px solid rgba(255, 255, 255, 0.09);
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
  transform: translateX(-50%) rotate(45deg);
}

.tip-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-bottom: 4px;
}

.tip-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.tip-time {
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 10.5px;
  letter-spacing: 0.4px;
  color: rgba(226, 232, 240, 0.7);
}

.tip-value {
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: #f8fafc;
}

.tip-value.empty {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
}
</style>
