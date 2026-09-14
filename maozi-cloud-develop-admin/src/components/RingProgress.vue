<template>
  <div class="ring-wrap" :style="{ width: size + 'px', height: size + 'px' }">
    <svg :width="size" :height="size">
      <defs>
        <linearGradient :id="gradId" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" :stop-color="colors[0]" />
          <stop offset="100%" :stop-color="colors[1]" />
        </linearGradient>
      </defs>
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        stroke="#eef2f7"
        :stroke-width="stroke"
      />
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke="`url(#${gradId})`"
        :stroke-width="stroke"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        class="ring-progress"
        :transform="`rotate(-90 ${center} ${center})`"
      />
    </svg>
    <div class="ring-center">
      <template v-if="unsupported">
        <div class="ring-value mono-text ring-na">—</div>
        <div class="ring-label">暂不支持</div>
      </template>
      <template v-else>
        <div class="ring-value mono-text">{{ displayValue }}<span class="ring-unit">%</span></div>
        <div class="ring-label">{{ label }}</div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    size?: number
    stroke?: number
    gradId: string
    label?: string
    unsupported?: boolean
    /** 正常档主题渐变色 [from, to]；>=70% 橙、>=85% 红自动分档 */
    theme?: [string, string]
  }>(),
  { size: 128, stroke: 11, label: '使用率', unsupported: false, theme: undefined }
)

const displayValue = computed(() => Math.round(props.value))

const center = computed(() => props.size / 2)
const radius = computed(() => (props.size - props.stroke) / 2 - 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const dashOffset = computed(() =>
  props.unsupported ? circumference.value : circumference.value * (1 - Math.min(props.value, 100) / 100)
)

const colors = computed<[string, string]>(() => {
  if (props.value >= 85) return ['#f87171', '#dc2626']
  if (props.value >= 70) return ['#fbbf24', '#d97706']
  return props.theme ?? ['#60a5fa', '#2563eb']
})
</script>

<style scoped>
.ring-wrap {
  position: relative;
  flex-shrink: 0;
}

.ring-progress {
  transition: stroke-dashoffset 0.6s ease, stroke 0.4s ease;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-value {
  font-size: 23px;
  font-weight: 700;
  color: #1f2d3d;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.ring-unit {
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  margin-left: 1px;
}

.ring-label {
  margin-top: 7px;
  font-size: 11px;
  color: #9ca3af;
}

.ring-na {
  color: #c0c4cc;
}
</style>
