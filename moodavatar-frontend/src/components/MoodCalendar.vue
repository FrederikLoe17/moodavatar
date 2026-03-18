<template>
  <div class="relative overflow-x-auto" @mouseleave="tooltip = null">
    <svg
      :width="svgWidth"
      :height="svgHeight"
      class="block"
    >
      <!-- Month labels -->
      <text
        v-for="m in monthLabels"
        :key="m.label + m.x"
        :x="DAY_LABEL_W + m.x"
        :y="11"
        fill="#64748b"
        font-size="10"
        font-family="ui-monospace, monospace"
      >{{ m.label }}</text>

      <!-- Day labels: Mon / Wed / Fri -->
      <text
        v-for="d in DAY_LABELS"
        :key="d.text"
        x="0"
        :y="MONTH_LABEL_H + d.row * CELL_STEP + CELL_SIZE - 1"
        fill="#64748b"
        font-size="9"
        font-family="ui-monospace, monospace"
      >{{ d.text }}</text>

      <!-- Cells -->
      <rect
        v-for="cell in cells"
        :key="cell.date"
        :x="DAY_LABEL_W + cell.col * CELL_STEP"
        :y="MONTH_LABEL_H + cell.row * CELL_STEP"
        :width="CELL_SIZE"
        :height="CELL_SIZE"
        rx="2"
        :fill="cell.fill"
        :fill-opacity="cell.opacity"
        class="cursor-pointer transition-opacity hover:opacity-90"
        @mouseenter="showTooltip(cell, $event)"
      />
    </svg>

    <!-- Legend -->
    <div class="flex items-center gap-2 mt-3 text-xs text-slate-500">
      <span>weniger</span>
      <div
        v-for="n in [0, 1, 2, 3]"
        :key="n"
        class="w-3 h-3 rounded-sm"
        :style="{ background: n === 0 ? '#1e293b' : '#10b981', opacity: n === 0 ? 1 : 0.3 + n * 0.23 }"
      />
      <span>mehr</span>
    </div>
  </div>

  <!-- Tooltip — fixed so it's never clipped by overflow -->
  <Teleport to="body">
    <div
      v-if="tooltip"
      class="fixed z-50 pointer-events-none"
      :style="{ left: tooltip.x + 14 + 'px', top: tooltip.y - 48 + 'px' }"
    >
      <div class="px-3 py-2 rounded-xl text-xs bg-slate-900/95 border border-slate-600 text-slate-200 whitespace-nowrap shadow-2xl backdrop-blur-sm">
        <div class="font-semibold text-slate-100 mb-0.5">{{ formatTooltipDate(tooltip.date) }}</div>
        <template v-if="tooltip.count > 0">
          <div class="flex items-center gap-1.5 mt-1">
            <div
              v-if="tooltip.emotion"
              class="w-2 h-2 rounded-full"
              :style="{ background: emotionColor(tooltip.emotion) }"
            />
            <span v-if="tooltip.emotion" :style="{ color: emotionColor(tooltip.emotion) }">
              {{ EMOTION_LABELS[tooltip.emotion] ?? tooltip.emotion }}
            </span>
          </div>
          <div class="text-slate-400 mt-0.5">
            {{ tooltip.count }} {{ tooltip.count === 1 ? 'Eintrag' : 'Einträge' }}
            · Ø {{ tooltip.avgIntensity.toFixed(1) }}
          </div>
        </template>
        <template v-else>
          <div class="text-slate-500 mt-0.5">Kein Eintrag</div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { CalendarDay } from '../api/avatar'

const props = defineProps<{ days: CalendarDay[] }>()

const CELL_SIZE    = 11
const GAP          = 2
const CELL_STEP    = CELL_SIZE + GAP
const WEEKS        = 52
const DAY_LABEL_W  = 18
const MONTH_LABEL_H = 16

const DAY_LABELS = [
  { text: 'M', row: 0 },
  { text: 'W', row: 2 },
  { text: 'F', row: 4 },
]

const svgWidth  = computed(() => DAY_LABEL_W + WEEKS * CELL_STEP)
const svgHeight = computed(() => MONTH_LABEL_H + 7 * CELL_STEP)

const EMOTION_COLORS: Record<string, string> = {
  HAPPY:   '#f59e0b',
  SAD:     '#3b82f6',
  ANGRY:   '#ef4444',
  NEUTRAL: '#6b7280',
  EXCITED: '#8b5cf6',
  TIRED:   '#1d4ed8',
  ANXIOUS: '#f97316',
  CONTENT: '#10b981',
}

const EMOTION_LABELS: Record<string, string> = {
  HAPPY: 'Glücklich', SAD: 'Traurig', ANGRY: 'Wütend',
  NEUTRAL: 'Neutral', EXCITED: 'Aufgeregt', TIRED: 'Müde',
  ANXIOUS: 'Ängstlich', CONTENT: 'Zufrieden',
}

function emotionColor(e: string) { return EMOTION_COLORS[e] ?? '#6b7280' }

function formatTooltipDate(dateStr: string) {
  const d = new Date(dateStr + 'T00:00:00')
  return d.toLocaleDateString('de-DE', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
}

// Build a lookup: date string → CalendarDay
const dayMap = computed(() => {
  const m: Record<string, CalendarDay> = {}
  for (const d of props.days) m[d.date] = d
  return m
})

// Compute the grid: start = 52 weeks ago, aligned to Monday
const cells = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const dayOfWeek = (today.getDay() + 6) % 7  // Mon=0 .. Sun=6
  const gridEnd = new Date(today)
  gridEnd.setDate(today.getDate() - dayOfWeek + 6)

  const gridStart = new Date(gridEnd)
  gridStart.setDate(gridEnd.getDate() - WEEKS * 7 + 1)

  const result = []
  const cur = new Date(gridStart)

  for (let col = 0; col < WEEKS; col++) {
    for (let row = 0; row < 7; row++) {
      const dateStr = cur.toISOString().substring(0, 10)
      const data = dayMap.value[dateStr]
      const count = data?.count ?? 0
      const emotion = data?.dominantEmotion ?? null
      const fill = count > 0 ? (EMOTION_COLORS[emotion ?? ''] ?? '#10b981') : '#1e293b'
      const opacity = count === 0 ? 1 : count === 1 ? 0.45 : count === 2 ? 0.7 : 1

      result.push({
        date: dateStr,
        col,
        row,
        fill,
        opacity,
        count,
        emotion,
        avgIntensity: data?.avgIntensity ?? 0,
        isFuture: cur > today,
      })
      cur.setDate(cur.getDate() + 1)
    }
  }
  return result
})

// Month label positions
const monthLabels = computed(() => {
  const MONTH_NAMES = ['Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez']
  const seen = new Set<number>()
  const labels: { label: string; x: number }[] = []
  for (const cell of cells.value) {
    if (cell.row !== 0) continue
    const month = new Date(cell.date).getMonth()
    if (!seen.has(month)) {
      seen.add(month)
      labels.push({ label: MONTH_NAMES[month], x: cell.col * CELL_STEP })
    }
  }
  return labels
})

// Tooltip — fixed position via clientX/Y
interface TooltipData {
  date: string; count: number; emotion: string | null
  avgIntensity: number; x: number; y: number
}
const tooltip = ref<TooltipData | null>(null)

function showTooltip(cell: typeof cells.value[0], e: MouseEvent) {
  tooltip.value = {
    date: cell.date,
    count: cell.count,
    emotion: cell.emotion,
    avgIntensity: cell.avgIntensity,
    x: e.clientX,
    y: e.clientY,
  }
}
</script>
