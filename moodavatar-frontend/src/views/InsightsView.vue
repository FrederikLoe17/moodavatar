<template>
  <AppLayout>
    <div class="max-w-5xl mx-auto px-4 py-8 space-y-8">

      <!-- Header -->
      <div>
        <h1 class="text-2xl font-bold text-white">Insights & Journal</h1>
        <p class="text-slate-400 text-sm mt-1">Deine Stimmungsgeschichte auf einen Blick</p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="text-slate-400 text-sm">Lade Daten …</div>

      <template v-else>
        <!-- Stats Cards -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div class="bg-slate-800 rounded-xl p-4 border border-slate-700">
            <div class="text-slate-400 text-xs mb-1">Einträge gesamt</div>
            <div class="text-2xl font-bold text-white">{{ insights?.totalEntries ?? 0 }}</div>
          </div>
          <div class="bg-slate-800 rounded-xl p-4 border border-slate-700">
            <div class="text-slate-400 text-xs mb-1">Aktueller Streak 🔥</div>
            <div class="text-2xl font-bold text-orange-400">{{ insights?.currentStreak ?? 0 }}<span class="text-sm font-normal text-slate-400 ml-1">Tage</span></div>
            <div v-if="insights && insights.longestStreak > 0" class="text-xs text-slate-500 mt-1">Bestes: {{ insights.longestStreak }} Tage</div>
          </div>
          <div class="bg-slate-800 rounded-xl p-4 border border-slate-700">
            <div class="text-slate-400 text-xs mb-1">Häufigste Emotion</div>
            <div class="text-xl font-bold" :style="{ color: emotionColor(insights?.mostCommonEmotion) }">
              {{ EMOTION_LABELS[insights?.mostCommonEmotion ?? ''] ?? '—' }}
            </div>
          </div>
          <div class="bg-slate-800 rounded-xl p-4 border border-slate-700">
            <div class="text-slate-400 text-xs mb-1">Ø Intensität</div>
            <div class="text-2xl font-bold text-white">{{ insights ? insights.avgIntensity.toFixed(1) : '—' }}<span class="text-sm font-normal text-slate-400 ml-1">/ 10</span></div>
          </div>
        </div>

        <!-- Calendar Heatmap -->
        <div class="bg-slate-800 rounded-xl p-5 border border-slate-700">
          <h2 class="text-sm font-semibold text-slate-300 mb-4">Aktivitäts-Kalender (letztes Jahr)</h2>
          <MoodCalendar :days="calendarDays" />
        </div>

        <!-- Emotion Distribution + Weekday Pattern -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

          <!-- Emotion Distribution -->
          <div class="bg-slate-800 rounded-xl p-5 border border-slate-700">
            <h2 class="text-sm font-semibold text-slate-300 mb-4">Emotions-Verteilung</h2>
            <div v-if="!insights || insights.emotionDistribution.length === 0" class="text-slate-500 text-sm">Keine Daten</div>
            <div v-else class="space-y-2">
              <div v-for="e in insights.emotionDistribution" :key="e.emotion" class="flex items-center gap-3">
                <div class="w-20 text-xs text-slate-400 shrink-0">{{ EMOTION_LABELS[e.emotion] ?? e.emotion }}</div>
                <div class="flex-1 bg-slate-700 rounded-full h-2 overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :style="{ width: e.percentage + '%', background: emotionColor(e.emotion) }"
                  />
                </div>
                <div class="w-10 text-right text-xs text-slate-400">{{ e.percentage.toFixed(0) }}%</div>
              </div>
            </div>
          </div>

          <!-- Weekday Pattern -->
          <div class="bg-slate-800 rounded-xl p-5 border border-slate-700">
            <h2 class="text-sm font-semibold text-slate-300 mb-4">Wochentags-Muster</h2>
            <div v-if="!insights" class="text-slate-500 text-sm">Keine Daten</div>
            <div v-else class="space-y-2">
              <div v-for="w in insights.weekdayPattern" :key="w.day" class="flex items-center gap-3">
                <div class="w-8 text-xs text-slate-400 shrink-0">{{ DAY_LABELS[w.day] }}</div>
                <div class="flex-1 bg-slate-700 rounded-full h-2 overflow-hidden">
                  <div
                    class="h-full rounded-full bg-violet-500 transition-all duration-500"
                    :style="{ width: (w.avgIntensity / 10 * 100) + '%' }"
                  />
                </div>
                <div class="w-10 text-right text-xs text-slate-400">
                  {{ w.count > 0 ? w.avgIntensity.toFixed(1) : '—' }}
                </div>
                <div class="w-10 text-right text-xs text-slate-500">{{ w.count }}x</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Journal (letzte Einträge) -->
        <div class="bg-slate-800 rounded-xl p-5 border border-slate-700">
          <h2 class="text-sm font-semibold text-slate-300 mb-4">Journal</h2>
          <div v-if="history.length === 0" class="text-slate-500 text-sm">Noch keine Einträge.</div>
          <div v-else class="space-y-2">
            <div
              v-for="entry in history"
              :key="entry.timestamp"
              class="flex items-start gap-3 py-2 border-b border-slate-700 last:border-0"
            >
              <!-- Datum -->
              <div class="text-xs text-slate-500 w-24 shrink-0 pt-0.5">
                {{ formatDate(entry.timestamp) }}
              </div>
              <!-- Emotion Badge -->
              <div
                class="text-xs font-semibold px-2 py-0.5 rounded-full shrink-0"
                :style="{ background: emotionColor(entry.emotion) + '33', color: emotionColor(entry.emotion) }"
              >
                {{ EMOTION_LABELS[entry.emotion] ?? entry.emotion }}
              </div>
              <!-- Intensity Dots -->
              <div class="flex gap-0.5 pt-1 shrink-0">
                <div
                  v-for="i in 10"
                  :key="i"
                  class="w-1.5 h-1.5 rounded-full"
                  :class="i <= entry.intensity ? 'opacity-100' : 'opacity-20'"
                  :style="{ background: emotionColor(entry.emotion) }"
                />
              </div>
              <!-- Notiz -->
              <div v-if="entry.note" class="text-xs text-slate-400 italic flex-1">
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
  MON: 'Mo', TUE: 'Di', WED: 'Mi', THU: 'Do', FRI: 'Fr', SAT: 'Sa', SUN: 'So',
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
  const token = auth.token
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
