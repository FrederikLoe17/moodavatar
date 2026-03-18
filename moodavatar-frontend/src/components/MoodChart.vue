<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px">
      <div style="font-size:13px;font-weight:700;color:#94a3b8;text-transform:uppercase;letter-spacing:.05em">
        Stimmungsverlauf
      </div>
      <!-- Legend -->
      <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:flex-end">
        <div v-for="e in activEmotions" :key="e.value"
          style="display:flex;align-items:center;gap:4px"
        >
          <div :style="`width:8px;height:8px;border-radius:50%;background:${e.color}`"/>
          <span style="font-size:10px;color:#64748b">{{ e.label }}</span>
        </div>
      </div>
    </div>

    <!-- Chart -->
    <svg
      :viewBox="`0 0 ${W} ${H}`"
      preserveAspectRatio="xMidYMid meet"
      style="width:100%;height:auto;overflow:visible"
    >
      <!-- Y axis grid lines + labels -->
      <g v-for="tick in yTicks" :key="tick">
        <line
          :x1="PAD_L" :y1="yPos(tick)"
          :x2="W - PAD_R" :y2="yPos(tick)"
          stroke="#334155" stroke-width="1"
        />
        <text
          :x="PAD_L - 6" :y="yPos(tick) + 4"
          text-anchor="end" font-size="10" fill="#475569"
        >{{ tick }}</text>
      </g>

      <!-- Gradient fill under the line -->
      <defs>
        <linearGradient id="chartFill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#10b981" stop-opacity="0.15"/>
          <stop offset="100%" stop-color="#10b981" stop-opacity="0"/>
        </linearGradient>
      </defs>

      <!-- Filled area -->
      <path v-if="points.length > 1"
        :d="fillPath"
        fill="url(#chartFill)"
      />

      <!-- Colored line segments between points -->
      <line
        v-for="(seg, i) in segments" :key="i"
        :x1="seg.x1" :y1="seg.y1"
        :x2="seg.x2" :y2="seg.y2"
        :stroke="seg.color"
        stroke-width="2"
        stroke-linecap="round"
      />

      <!-- Points -->
      <g v-for="(p, i) in points" :key="i" style="cursor:pointer"
        @mouseenter="hovered = i" @mouseleave="hovered = null"
      >
        <!-- Glow ring on hover -->
        <circle v-if="hovered === i"
          :cx="p.x" :cy="p.y" :r="10"
          :fill="p.color + '33'"
        />
        <!-- Dot -->
        <circle
          :cx="p.x" :cy="p.y"
          :r="hovered === i ? 6 : 4"
          :fill="p.color"
          stroke="#0f172a" stroke-width="2"
          style="transition:r 0.1s"
        />

        <!-- X axis label -->
        <text
          :x="p.x" :y="H - 4"
          text-anchor="middle" font-size="9" fill="#475569"
        >{{ p.timeLabel }}</text>

        <!-- Tooltip on hover -->
        <g v-if="hovered === i">
          <!-- Tooltip box -->
          <rect
            :x="tooltipX(p.x)" :y="p.y - 46"
            width="90" height="40"
            rx="6" ry="6"
            fill="#1e293b" stroke="#334155" stroke-width="1"
          />
          <text :x="tooltipX(p.x) + 8" :y="p.y - 30" font-size="11" font-weight="600" :fill="p.color">
            {{ p.label }}
          </text>
          <text :x="tooltipX(p.x) + 8" :y="p.y - 16" font-size="10" fill="#94a3b8">
            Intensität {{ p.intensity }}/10
          </text>
        </g>
      </g>
    </svg>

    <!-- Empty state -->
    <div v-if="points.length === 0"
      style="text-align:center;color:#475569;font-size:13px;padding:32px 0"
    >
      Noch keine Stimmungen aufgezeichnet.
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { MoodEntry, Emotion } from '../api/avatar'

const props = defineProps<{ history: MoodEntry[] }>()

// ── Layout constants ──────────────────────────────────────────────────────────
const W     = 560
const H     = 160
const PAD_L = 28
const PAD_R = 12
const PAD_T = 16
const PAD_B = 22

const plotW = W - PAD_L - PAD_R
const plotH = H - PAD_T - PAD_B

// ── Emotion meta ──────────────────────────────────────────────────────────────
const EMOTION_META: Record<string, { label: string; color: string }> = {
  HAPPY:   { label: 'Glücklich',  color: '#10b981' },
  SAD:     { label: 'Traurig',    color: '#3b82f6' },
  ANGRY:   { label: 'Wütend',     color: '#ef4444' },
  NEUTRAL: { label: 'Neutral',    color: '#64748b' },
  EXCITED: { label: 'Aufgeregt',  color: '#f59e0b' },
  TIRED:   { label: 'Müde',       color: '#8b5cf6' },
  ANXIOUS: { label: 'Ängstlich',  color: '#f97316' },
  CONTENT: { label: 'Zufrieden',  color: '#06b6d4' },
}

const yTicks = [2, 4, 6, 8, 10]
const hovered = ref<number | null>(null)

// ── Helpers ───────────────────────────────────────────────────────────────────
function yPos(intensity: number) {
  return PAD_T + plotH * (1 - (intensity - 1) / 9)
}

function timeLabel(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 60)   return `${m}m`
  const h = Math.floor(m / 60)
  if (h < 24)   return `${h}h`
  return `${Math.floor(h / 24)}d`
}

// ── Computed points ───────────────────────────────────────────────────────────
const points = computed(() => {
  // Show oldest → newest, max 20 entries
  const entries = [...props.history].reverse().slice(-20)
  if (entries.length === 0) return []

  return entries.map((e, i) => {
    const meta = EMOTION_META[e.emotion] ?? { label: e.emotion, color: '#64748b' }
    const x = entries.length === 1
      ? PAD_L + plotW / 2
      : PAD_L + (i / (entries.length - 1)) * plotW
    return {
      x,
      y:          yPos(e.intensity),
      color:      meta.color,
      label:      meta.label,
      intensity:  e.intensity,
      timeLabel:  timeLabel(e.setAt),
    }
  })
})

const segments = computed(() =>
  points.value.slice(0, -1).map((p, i) => ({
    x1: p.x, y1: p.y,
    x2: points.value[i + 1]!.x, y2: points.value[i + 1]!.y,
    color: p.color,
  }))
)

const fillPath = computed(() => {
  if (points.value.length < 2) return ''
  const first = points.value[0]
  const last  = points.value[points.value.length - 1]
  const lineD = points.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ')
  return `${lineD} L${last!.x},${H - PAD_B} L${first!.x},${H - PAD_B} Z`
})

const activEmotions = computed(() => {
  const seen = new Set(props.history.map(e => e.emotion))
  return Object.entries(EMOTION_META)
    .filter(([k]) => seen.has(k as Emotion))
    .map(([value, meta]) => ({ value, ...meta }))
})

function tooltipX(px: number): number {
  // Keep tooltip inside chart
  return px + 90 > W - PAD_R ? px - 98 : px + 8
}
</script>
