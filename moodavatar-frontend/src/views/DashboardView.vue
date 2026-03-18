<template>
  <AppLayout :flush="true">
    <div style="display:flex;height:100%;overflow:hidden;position:relative">

      <!-- ── Room (fills remaining space) ── -->
      <div style="flex:1;height:100%;overflow:hidden;position:relative">
        <AvatarRoom
          :emotion="avatar?.emotion ?? null"
          :config="avatar?.config ?? null"
          :visitors="realtime.myRoomVisitors"
          :needs="needs"
          :fill-height="true"
        />

        <!-- Visitor toasts (floating over room) -->
        <TransitionGroup name="toast" tag="div"
          style="position:absolute;top:12px;left:12px;display:flex;flex-direction:column;gap:8px;pointer-events:none;z-index:10;max-width:280px"
        >
          <div v-for="t in visitorToasts" :key="t.id"
            style="display:flex;align-items:center;gap:8px;background:#0f3b2ddd;backdrop-filter:blur(8px);border:1px solid #10b98155;border-radius:12px;padding:10px 14px"
          >
            <span style="font-size:16px">🚶</span>
            <span style="font-size:12px;color:#6ee7b7"><strong>{{ t.username }}</strong> hat deinen Raum betreten</span>
          </div>
        </TransitionGroup>

        <!-- Knock notifications (floating over room) -->
        <div style="position:absolute;top:12px;right:12px;display:flex;flex-direction:column;gap:8px;pointer-events:none;z-index:10;max-width:280px">
          <div v-for="knock in realtime.pendingKnocks" :key="knock.fromUserId"
            style="display:flex;align-items:center;justify-content:space-between;background:#1e3a5fdd;backdrop-filter:blur(8px);border:1px solid #3b82f633;border-radius:12px;padding:10px 14px;pointer-events:all"
          >
            <div style="font-size:13px;color:#93c5fd">
              🚪 <strong>{{ knock.fromUsername }}</strong> klopft an deiner Tür
            </div>
            <button @click="realtime.dismissKnock(knock.fromUserId)"
              style="font-size:11px;color:#64748b;background:none;border:none;cursor:pointer;padding:2px 6px"
            >✕</button>
          </div>
        </div>

        <!-- Greeting (floating bottom-left) -->
        <div style="position:absolute;bottom:48px;left:16px;pointer-events:none">
          <div style="font-size:18px;font-weight:700;color:#f8fafc;text-shadow:0 2px 8px #00000088">
            Hallo, {{ auth.user?.username }} 👋
          </div>
          <p style="color:#94a3b8;font-size:12px;margin-top:2px;text-shadow:0 1px 4px #00000088">
            {{ avatar ? emotionLabel(avatar.emotion) + ' · Intensität ' + avatar.intensity + '/10' : 'Noch keine Stimmung gesetzt' }}
          </p>
        </div>
      </div>

      <!-- ── Sidebar toggle tab ── -->
      <button @click="sidebarOpen = !sidebarOpen"
        style="position:absolute;right:0;top:50%;transform:translateY(-50%);z-index:20;
               background:#1e293b;border:1px solid #334155;border-right:none;
               border-radius:8px 0 0 8px;padding:10px 6px;cursor:pointer;
               color:#94a3b8;font-size:14px;line-height:1;
               transition:background 0.15s,color 0.15s"
        :style="sidebarOpen ? 'right:340px;' : ''"
        onmouseover="this.style.color='#f1f5f9';this.style.background='#334155'"
        onmouseout="this.style.color='#94a3b8';this.style.background='#1e293b'"
      >{{ sidebarOpen ? '›' : '‹' }}</button>

      <!-- ── Sidebar ── -->
      <Transition name="sidebar">
        <div v-show="sidebarOpen"
          style="width:340px;height:100%;overflow-y:auto;
                 background:#0f172a;border-left:1px solid #334155;
                 display:flex;flex-direction:column;gap:14px;padding:20px;
                 flex-shrink:0"
        >

          <!-- Needs panel -->
          <div v-if="needs" style="background:#1e293b;border-radius:14px;padding:16px;border:1px solid #334155">
            <div style="font-size:12px;font-weight:700;color:#94a3b8;margin-bottom:12px">Bedürfnisse</div>
            <div style="display:flex;flex-direction:column;gap:8px">
              <div v-for="need in needsList" :key="need.key" style="display:flex;align-items:center;gap:8px">
                <span style="font-size:14px;width:20px;text-align:center">{{ need.icon }}</span>
                <div style="flex:1">
                  <div style="display:flex;justify-content:space-between;margin-bottom:3px">
                    <span style="font-size:10px;color:#64748b">{{ need.label }}</span>
                    <span style="font-size:10px;font-weight:600" :style="{color: needColor(need.value)}">{{ need.value }}%</span>
                  </div>
                  <div style="height:5px;background:#0f172a;border-radius:99px;overflow:hidden">
                    <div style="height:100%;border-radius:99px;transition:width 0.6s ease"
                      :style="{width: need.value + '%', background: needColor(need.value)}"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Mood Picker -->
          <div style="background:#1e293b;border-radius:14px;padding:18px;border:1px solid #334155">
            <div style="font-size:12px;font-weight:700;color:#94a3b8;margin-bottom:12px">Stimmung setzen</div>
            <MoodPicker :avatar-config="avatar?.config ?? null" :compact="true" @updated="onAvatarUpdated" />
          </div>

          <!-- Current mood -->
          <div v-if="avatar" style="background:#1e293b;border-radius:14px;padding:18px;border:1px solid #334155">
            <div style="font-size:12px;font-weight:700;color:#94a3b8;margin-bottom:10px">Aktuelle Stimmung</div>
            <div style="font-size:20px;font-weight:700;color:#f1f5f9">{{ emotionLabel(avatar.emotion) }}</div>
            <div style="font-size:12px;color:#64748b;margin-top:4px">Intensität {{ avatar.intensity }}/10</div>
            <div v-if="avatar.note" style="font-size:12px;color:#94a3b8;margin-top:8px;font-style:italic">"{{ avatar.note }}"</div>
            <div style="font-size:11px;color:#475569;margin-top:8px">{{ timeAgo(avatar.updatedAt) }}</div>
          </div>

          <!-- Stats -->
          <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px">
            <div v-for="card in cards" :key="card.label"
              style="background:#1e293b;border-radius:12px;padding:14px 10px;border:1px solid #334155;text-align:center"
            >
              <div style="font-size:20px;margin-bottom:4px">{{ card.icon }}</div>
              <div style="font-size:16px;font-weight:700;color:#f8fafc">{{ card.value }}</div>
              <div style="font-size:10px;color:#64748b;margin-top:2px">{{ card.label }}</div>
            </div>
          </div>

          <!-- Mood chart -->
          <div style="background:#1e293b;border-radius:14px;padding:16px;border:1px solid #334155">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
              <span style="font-size:12px;font-weight:700;color:#94a3b8">Stimmungsverlauf</span>
              <button @click="chartExpanded = true"
                style="font-size:11px;color:#64748b;background:none;border:none;cursor:pointer;padding:2px 6px;border-radius:4px;border:1px solid #334155"
                onmouseover="this.style.color='#f1f5f9';this.style.borderColor='#475569'"
                onmouseout="this.style.color='#64748b';this.style.borderColor='#334155'"
              >⛶ Vergrößern</button>
            </div>
            <MoodChart :history="history" />
          </div>

          <!-- Mood history tags -->
          <div v-if="history.length > 0"
            style="background:#1e293b;border-radius:14px;padding:16px;border:1px solid #334155"
          >
            <div style="font-size:12px;font-weight:700;color:#94a3b8;margin-bottom:12px">Verlauf</div>
            <div style="display:flex;flex-wrap:wrap;gap:6px">
              <div v-for="(entry, i) in history.slice(0, 8)" :key="i"
                :title="`${emotionLabel(entry.emotion)} — ${entry.intensity}/10${entry.note ? ` — ${entry.note}` : ''}`"
                style="display:flex;align-items:center;gap:4px;padding:4px 8px;border-radius:20px;font-size:11px;font-weight:600;cursor:default"
                :style="{
                  background: emotionColor(entry.emotion) + '22',
                  border: `1px solid ${emotionColor(entry.emotion)}55`,
                  color: emotionColor(entry.emotion),
                }"
              >
                <span>{{ emotionEmoji(entry.emotion) }}</span>
                <span>{{ emotionLabel(entry.emotion) }}</span>
                <span style="opacity:.6">{{ entry.intensity }}</span>
              </div>
            </div>
          </div>

          <!-- Friends -->
          <div style="background:#1e293b;border-radius:14px;padding:16px;border:1px solid #334155">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
              <div style="font-size:12px;font-weight:700;color:#94a3b8">Freunde</div>
              <RouterLink to="/friends" style="font-size:11px;color:#10b981">Alle →</RouterLink>
            </div>
            <div v-if="friends.length === 0" style="color:#64748b;font-size:12px">
              Noch keine Freunde. <RouterLink to="/friends" style="color:#10b981">Leute finden →</RouterLink>
            </div>
            <div v-else style="display:flex;flex-direction:column;gap:2px">
              <div v-for="f in friends.slice(0, 5)" :key="f.id"
                style="display:flex;align-items:center;gap:10px;padding:7px 0;border-bottom:1px solid #1e293b"
              >
                <div style="position:relative;width:28px;height:28px;flex-shrink:0">
                  <div style="width:28px;height:28px;border-radius:50%;background:#334155;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;color:#94a3b8">
                    {{ f.username.charAt(0).toUpperCase() }}
                  </div>
                  <div :style="`position:absolute;bottom:0;right:0;width:8px;height:8px;border-radius:50%;border:2px solid #1e293b;background:${realtime.isOnline(f.id) ? '#10b981' : '#475569'}`"/>
                </div>
                <RouterLink :to="`/u/${f.username}`" style="flex:1;text-decoration:none">
                  <div style="font-size:12px;font-weight:600;color:#f1f5f9">{{ f.displayName ?? f.username }}</div>
                  <div style="font-size:10px;color:#64748b">
                    <span v-if="realtime.friendMoods.get(f.id)">
                      {{ emotionEmoji(realtime.friendMoods.get(f.id)!.emotion as Emotion) }}
                      {{ emotionLabel(realtime.friendMoods.get(f.id)!.emotion as Emotion) }}
                    </span>
                    <span v-else-if="realtime.isOnline(f.id)" style="color:#10b981">online</span>
                    <span v-else>@{{ f.username }}</span>
                  </div>
                </RouterLink>
                <RouterLink :to="`/room/${f.username}`"
                  style="font-size:11px;color:#60a5fa;text-decoration:none;padding:3px 7px;border-radius:6px;border:1px solid #1e3a5f;background:#0f172a"
                >🚪</RouterLink>
              </div>
            </div>
          </div>

        </div>
      </Transition>

    </div>

    <!-- Chart modal -->
    <Transition name="fade">
      <div v-if="chartExpanded"
        style="position:fixed;inset:0;z-index:100;background:#000000bb;backdrop-filter:blur(6px);display:flex;align-items:center;justify-content:center;padding:32px"
        @click.self="chartExpanded = false"
      >
        <div style="background:#1e293b;border:1px solid #334155;border-radius:20px;padding:28px;width:100%;max-width:860px;max-height:90vh;overflow-y:auto">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
            <span style="font-size:15px;font-weight:700;color:#f1f5f9">Stimmungsverlauf</span>
            <button @click="chartExpanded = false"
              style="font-size:18px;color:#64748b;background:none;border:none;cursor:pointer;line-height:1;padding:4px 8px;border-radius:6px"
              onmouseover="this.style.color='#f1f5f9'" onmouseout="this.style.color='#64748b'"
            >✕</button>
          </div>
          <MoodChart :history="history" />
        </div>
      </div>
    </Transition>

  </AppLayout>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import AvatarRoom from '../components/AvatarRoom.vue'
import MoodPicker from '../components/MoodPicker.vue'
import MoodChart from '../components/MoodChart.vue'
import { useAuthStore } from '../stores/auth'
import { useRealtimeStore } from '../stores/realtime'
import { userApi, type Profile } from '../api/user'
import { avatarApi, type Avatar, type MoodEntry, type Emotion, type Needs } from '../api/avatar'

const auth      = useAuthStore()
const realtime  = useRealtimeStore()
const friends   = ref<Profile[]>([])
const avatar    = ref<Avatar | null>(null)
const history   = ref<MoodEntry[]>([])
const needs     = ref<Needs | null>(null)
const sidebarOpen   = ref(true)
const chartExpanded = ref(false)

interface VisitorToast { id: number; username: string }
const visitorToasts = ref<VisitorToast[]>([])
let toastId = 0

watch(
  () => realtime.myRoomVisitors.length,
  (newLen, oldLen) => {
    if (newLen > (oldLen ?? 0)) {
      const newest = realtime.myRoomVisitors[newLen - 1]
      if (newest) {
        const id = ++toastId
        visitorToasts.value.push({ id, username: newest.username })
        setTimeout(() => { visitorToasts.value = visitorToasts.value.filter(t => t.id !== id) }, 4000)
      }
    }
  }
)

const EMOTION_META: Record<string, { label: string; emoji: string; color: string }> = {
  HAPPY:   { label: 'Glücklich',  emoji: '😊', color: '#10b981' },
  SAD:     { label: 'Traurig',    emoji: '😢', color: '#3b82f6' },
  ANGRY:   { label: 'Wütend',     emoji: '😠', color: '#ef4444' },
  NEUTRAL: { label: 'Neutral',    emoji: '😐', color: '#64748b' },
  EXCITED: { label: 'Aufgeregt',  emoji: '🤩', color: '#f59e0b' },
  TIRED:   { label: 'Müde',       emoji: '😴', color: '#8b5cf6' },
  ANXIOUS: { label: 'Ängstlich',  emoji: '😰', color: '#f97316' },
  CONTENT: { label: 'Zufrieden',  emoji: '😌', color: '#06b6d4' },
}

const emotionLabel = (e: Emotion) => EMOTION_META[e]?.label ?? e
const emotionEmoji = (e: Emotion) => EMOTION_META[e]?.emoji ?? '😶'
const emotionColor = (e: Emotion) => EMOTION_META[e]?.color ?? '#64748b'

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1)  return 'gerade eben'
  if (m < 60) return `vor ${m} Min.`
  const h = Math.floor(m / 60)
  if (h < 24) return `vor ${h} Std.`
  return `vor ${Math.floor(h / 24)} Tag(en)`
}

const cards = computed(() => [
  { icon: avatar.value ? emotionEmoji(avatar.value.emotion) : '😶', value: avatar.value ? emotionLabel(avatar.value.emotion) : '—', label: 'Stimmung' },
  { icon: '👥', value: friends.value.length, label: 'Freunde' },
  { icon: '📊', value: history.value.length, label: 'Einträge' },
])

const needsList = computed(() => needs.value ? [
  { key: 'mood',     icon: '💚', label: 'Stimmung',  value: needs.value!.mood },
  { key: 'energy',   icon: '⚡', label: 'Energie',   value: needs.value!.energy },
  { key: 'social',   icon: '👥', label: 'Soziales',  value: needs.value!.social },
  { key: 'activity', icon: '🏃', label: 'Aktivität', value: needs.value!.activity },
] : [])

function needColor(v: number): string {
  if (v >= 60) return '#10b981'
  if (v >= 30) return '#f59e0b'
  return '#ef4444'
}

function onAvatarUpdated(updated: Avatar) {
  avatar.value = updated
  history.value = [
    { emotion: updated.emotion, intensity: updated.intensity, note: updated.note, timestamp: updated.updatedAt, config: updated.config },
    ...history.value,
  ].slice(0, 50)
  avatarApi.getNeeds(auth.accessToken).then(r => { needs.value = r.data }).catch(() => {})
}

onMounted(async () => {
  const token = auth.accessToken
  const [friendRes, avatarRes, histRes, needsRes] = await Promise.allSettled([
    userApi.getFriends(token),
    avatarApi.getMyAvatar(token),
    avatarApi.getMoodHistory(token, 30),
    avatarApi.getNeeds(token),
  ])
  if (friendRes.status === 'fulfilled') friends.value = friendRes.value.data
  if (avatarRes.status  === 'fulfilled') avatar.value  = avatarRes.value.data
  if (histRes.status    === 'fulfilled') history.value = histRes.value.data
  if (needsRes.status   === 'fulfilled') needs.value   = needsRes.value.data
})

</script>

<style scoped>
.sidebar-enter-active, .sidebar-leave-active {
  transition: width 0.25s ease, opacity 0.2s ease;
  overflow: hidden;
}
.sidebar-enter-from, .sidebar-leave-to {
  width: 0 !important;
  opacity: 0;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to        { opacity: 0; }
.toast-enter-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.toast-leave-active { transition: opacity 0.4s ease, transform 0.4s ease; }
.toast-enter-from   { opacity: 0; transform: translateX(-16px); }
.toast-leave-to     { opacity: 0; transform: translateX(-16px); }
</style>
