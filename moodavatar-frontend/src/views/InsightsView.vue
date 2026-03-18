<template>
  <AppLayout>
    <div class="max-w-5xl mx-auto px-4 py-8 space-y-6">

      <!-- Header -->
      <div class="relative overflow-hidden rounded-2xl p-6 bg-gradient-to-br from-violet-900/40 via-slate-800 to-slate-800/80 border border-violet-500/20">
        <div class="absolute -top-12 -right-12 w-56 h-56 bg-violet-500/10 rounded-full blur-3xl pointer-events-none" />
        <div class="absolute -bottom-8 -left-8 w-40 h-40 bg-indigo-500/10 rounded-full blur-2xl pointer-events-none" />
        <div class="relative">
          <h1 class="text-3xl font-bold text-white tracking-tight">Insights & Journal</h1>
          <p class="text-slate-400 text-sm mt-1">Deine Stimmungsgeschichte auf einen Blick</p>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-24 gap-4">
        <div class="w-10 h-10 rounded-full border-2 border-violet-500 border-t-transparent animate-spin" />
        <span class="text-slate-400 text-sm">Lade Daten …</span>
      </div>

      <template v-else>

        <!-- Stats Cards -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">

          <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/50 hover:border-slate-600 transition-all hover:bg-slate-800">
            <div class="text-2xl mb-3">📓</div>
            <div class="text-3xl font-bold text-white tabular-nums">{{ insights?.totalEntries ?? 0 }}</div>
            <div class="text-slate-500 text-xs mt-1.5">Einträge gesamt</div>
          </div>

          <div class="bg-gradient-to-br from-orange-950/50 to-slate-800/80 rounded-2xl p-5 border border-orange-500/20 hover:border-orange-500/40 transition-all">
            <div class="text-2xl mb-3">🔥</div>
            <div class="flex items-baseline gap-1">
              <span class="text-3xl font-bold text-orange-400 tabular-nums">{{ insights?.currentStreak ?? 0 }}</span>
              <span class="text-sm text-slate-400">Tage</span>
            </div>
            <div class="text-slate-500 text-xs mt-1.5">
              Streak
              <span v-if="insights && insights.longestStreak > 0" class="text-slate-600 ml-1">· Best: {{ insights.longestStreak }}</span>
            </div>
          </div>

          <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/50 hover:border-slate-600 transition-all hover:bg-slate-800">
            <div class="text-2xl mb-3">💫</div>
            <div
              class="text-xl font-bold mt-1 truncate"
              :style="{ color: emotionColor(insights?.mostCommonEmotion) }"
            >
              {{ EMOTION_LABELS[insights?.mostCommonEmotion ?? ''] ?? '—' }}
            </div>
            <div class="text-slate-500 text-xs mt-1.5">Häufigste Emotion</div>
          </div>

          <div class="bg-slate-800/80 rounded-2xl p-5 border border-slate-700/50 hover:border-slate-600 transition-all hover:bg-slate-800">
            <div class="text-2xl mb-3">⚡</div>
            <div class="flex items-baseline gap-1">
              <span class="text-3xl font-bold text-white tabular-nums">{{ insights ? insights.avgIntensity.toFixed(1) : '—' }}</span>
              <span class="text-sm text-slate-400">/ 10</span>
            </div>
            <div class="text-slate-500 text-xs mt-1.5">Ø Intensität</div>
          </div>

        </div>

        <!-- Calendar Heatmap -->
        <div class="bg-slate-800/80 rounded-2xl p-6 border border-slate-700/50">
          <div class="flex items-center justify-between mb-5">
            <h2 class="font-semibold text-white">Aktivitäts-Kalender</h2>
            <span class="text-xs text-slate-500 bg-slate-700/50 px-3 py-1 rounded-full">letztes Jahr</span>
          </div>
          <MoodCalendar :days="calendarDays" />
        </div>

        <!-- Emotion Distribution + Weekday Pattern -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

          <!-- Emotion Distribution -->
          <div class="bg-slate-800/80 rounded-2xl p-6 border border-slate-700/50">
            <h2 class="font-semibold text-white mb-5">Emotions-Verteilung</h2>
            <div v-if="!insights || insights.emotionDistribution.length === 0" class="text-slate-500 text-sm py-4 text-center">
              Noch keine Daten
            </div>
            <div v-else class="space-y-4">
              <div v-for="e in insights.emotionDistribution" :key="e.emotion" class="space-y-1.5">
                <div class="flex items-center justify-between text-xs">
                  <div class="flex items-center gap-2">
                    <div class="w-2 h-2 rounded-full shrink-0" :style="{ background: emotionColor(e.emotion) }" />
                    <span class="text-slate-300 font-medium">{{ EMOTION_LABELS[e.emotion] ?? e.emotion }}</span>
                  </div>
                  <div class="flex items-center gap-2 text-slate-500">
                    <span>{{ e.count }}x</span>
                    <span class="font-semibold w-8 text-right" :style="{ color: emotionColor(e.emotion) }">{{ e.percentage.toFixed(0) }}%</span>
                  </div>
                </div>
                <div class="h-1.5 bg-slate-700 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-700"
                    :style="{ width: e.percentage + '%', background: emotionColor(e.emotion) }"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Weekday Pattern -->
          <div class="bg-slate-800/80 rounded-2xl p-6 border border-slate-700/50">
            <h2 class="font-semibold text-white mb-5">Wochentags-Muster</h2>
            <div v-if="!insights" class="text-slate-500 text-sm py-4 text-center">Keine Daten</div>
            <div v-else class="space-y-4">
              <div v-for="w in insights.weekdayPattern" :key="w.day" class="space-y-1.5">
                <div class="flex items-center justify-between text-xs">
                  <span class="text-slate-300 font-medium">{{ DAY_LABELS[w.day] }}</span>
                  <div class="flex items-center gap-2 text-slate-500">
                    <span>{{ w.count }}x</span>
                    <span class="font-semibold w-8 text-right text-violet-400">{{ w.count > 0 ? w.avgIntensity.toFixed(1) : '—' }}</span>
                  </div>
                </div>
                <div class="h-1.5 bg-slate-700 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full bg-gradient-to-r from-violet-600 to-violet-400 transition-all duration-700"
                    :style="{ width: (w.avgIntensity / 10 * 100) + '%' }"
                  />
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- Journal -->
        <div class="bg-slate-800/80 rounded-2xl border border-slate-700/50 overflow-hidden">
          <div class="px-6 py-4 border-b border-slate-700/50 flex items-center justify-between">
            <h2 class="font-semibold text-white">Journal</h2>
            <span v-if="history.length > 0" class="text-xs text-slate-500 bg-slate-700/50 px-3 py-1 rounded-full">
              {{ history.length }} Einträge
            </span>
          </div>
          <div v-if="history.length === 0" class="px-6 py-12 text-center text-slate-500 text-sm">
            Noch keine Einträge. Setze deine erste Stimmung!
          </div>
          <div v-else class="divide-y divide-slate-700/40">
            <div
              v-for="entry in history"
              :key="entry.setAt"
              class="flex items-center gap-4 px-6 py-3 hover:bg-slate-700/20 transition-colors"
            >
              <!-- Date -->
              <div class="text-xs text-slate-500 w-28 shrink-0 font-mono">
                {{ formatDate(entry.setAt) }}
              </div>

              <!-- Emotion Badge -->
              <div
                class="text-xs font-semibold px-2.5 py-0.5 rounded-full shrink-0 border"
                :style="{
                  background: emotionColor(entry.emotion) + '18',
                  color: emotionColor(entry.emotion),
                  borderColor: emotionColor(entry.emotion) + '40',
                }"
              >
                {{ EMOTION_LABELS[entry.emotion] ?? entry.emotion }}
              </div>

              <!-- Intensity Dots -->
              <div class="flex gap-0.5 shrink-0">
                <div
                  v-for="i in 10"
                  :key="i"
                  class="w-1.5 h-1.5 rounded-full transition-all"
                  :class="i <= entry.intensity ? 'opacity-100' : 'opacity-10'"
                  :style="{ background: emotionColor(entry.emotion) }"
                />
              </div>

              <!-- Note -->
              <div v-if="entry.note" class="text-xs text-slate-400 italic flex-1 truncate">
                "{{ entry.note }}"
              </div>
            </div>
          </div>
        </div>

      </template>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import MoodCalendar from '../components/MoodCalendar.vue'
import { avatarApi, type CalendarDay, type Insights } from '../api/avatar'
import type { MoodEntry } from '../api/avatar'
import { useAuthStore } from '../stores/auth'

const auth    = useAuthStore()
const loading = ref(true)

const insights    = ref<Insights | null>(null)
const calendarDays = ref<CalendarDay[]>([])
const history     = ref<MoodEntry[]>([])

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

const DAY_LABELS: Record<string, string> = {
  MON: 'Montag', TUE: 'Dienstag', WED: 'Mittwoch',
  THU: 'Donnerstag', FRI: 'Freitag', SAT: 'Samstag', SUN: 'Sonntag',
}

function emotionColor(e: string | null | undefined) {
  return EMOTION_COLORS[e ?? ''] ?? '#6b7280'
}

function formatDate(iso: string) {
  const d = new Date(iso)
  return d.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: '2-digit' })
    + ' ' + d.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  const token = auth.accessToken
  if (!token) return

  try {
    const [insightsRes, calRes, histRes] = await Promise.all([
      avatarApi.getInsights(token),
      avatarApi.getCalendarData(token, 365),
      avatarApi.getMoodHistory(token, 50),
    ])
    insights.value     = insightsRes.data
    calendarDays.value = calRes.data
    history.value      = histRes.data
  } catch (e) {
    console.error('Insights load failed', e)
  } finally {
    loading.value = false
  }
})
</script>
