<template>
  <AppLayout>
    <div>
      <div style="margin-bottom:24px">
        <h1 style="font-size:20px;font-weight:700;color:#f8fafc">Admin Panel</h1>
        <p style="font-size:13px;color:#64748b;margin-top:4px">Nutzerverwaltung & Systemübersicht</p>
      </div>

      <!-- Tabs -->
      <div style="display:flex;gap:4px;background:#1e293b;border-radius:10px;padding:4px;width:fit-content;margin-bottom:24px">
        <button v-for="tab in tabs" :key="tab.id" @click="activeTab = tab.id"
          style="padding:6px 18px;border-radius:7px;font-size:13px;font-weight:500;border:none;cursor:pointer;transition:all 0.15s"
          :style="activeTab === tab.id
            ? 'background:#8b5cf6;color:#fff'
            : 'background:transparent;color:#64748b'"
        >{{ tab.label }}</button>
      </div>

      <!-- ── Übersicht ─────────────────────────────────────────── -->
      <div v-if="activeTab === 'overview'">
        <div v-if="statsLoading" style="color:#64748b;font-size:13px;padding:20px 0">Lade Statistiken...</div>
        <template v-else>
          <!-- Stats Cards -->
          <div style="display:grid;gap:12px;margin-bottom:20px"
               :style="{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }"
          >
            <div v-for="card in statCards" :key="card.label"
              style="background:#1e293b;border-radius:12px;padding:20px;border:1px solid #334155"
            >
              <div style="font-size:22px;margin-bottom:8px">{{ card.icon }}</div>
              <div style="font-size:24px;font-weight:700;color:#f8fafc">{{ card.value }}</div>
              <div style="font-size:12px;color:#64748b;margin-top:2px">{{ card.label }}</div>
            </div>
          </div>

          <!-- Recent registrations -->
          <div style="background:#1e293b;border-radius:12px;border:1px solid #334155;overflow:hidden">
            <div style="padding:16px 20px;border-bottom:1px solid #334155">
              <span style="font-size:13px;font-weight:700;color:#94a3b8">Neueste Registrierungen</span>
            </div>
            <table style="width:100%;border-collapse:collapse">
              <thead>
                <tr style="background:#0f172a">
                  <th v-for="h in ['Nutzer', 'E-Mail', 'Rolle', 'Bestätigt', 'Registriert']" :key="h"
                    style="padding:10px 16px;text-align:left;font-size:11px;font-weight:600;color:#64748b;text-transform:uppercase;letter-spacing:.05em"
                  >{{ h }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in recentUsers" :key="u.id"
                  style="border-top:1px solid #1e293b"
                  onmouseover="this.style.background='#1e293b'" onmouseout="this.style.background='transparent'"
                >
                  <td style="padding:12px 16px">
                    <RouterLink :to="`/u/${u.username}`"
                      style="font-size:13px;font-weight:600;color:#f1f5f9;text-decoration:none"
                      onmouseover="this.style.color='#8b5cf6'" onmouseout="this.style.color='#f1f5f9'"
                    >{{ u.username }}</RouterLink>
                  </td>
                  <td style="padding:12px 16px;font-size:12px;color:#64748b">{{ u.email }}</td>
                  <td style="padding:12px 16px">
                    <span :style="roleBadgeStyle(u.role)">{{ u.role }}</span>
                  </td>
                  <td style="padding:12px 16px;font-size:12px" :style="{ color: u.isVerified ? '#10b981' : '#64748b' }">
                    {{ u.isVerified ? '✓' : '—' }}
                  </td>
                  <td style="padding:12px 16px;font-size:12px;color:#64748b">{{ formatDate(u.createdAt) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>

      <!-- ── Nutzerverwaltung ──────────────────────────────────── -->
      <div v-if="activeTab === 'users'">
        <!-- Search -->
        <div style="display:flex;gap:8px;margin-bottom:16px">
          <input v-model="search" @keyup.enter="loadUsers(1)" placeholder="Username oder E-Mail suchen..."
            style="flex:1;background:#1e293b;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
            @focus="($event.target as HTMLElement).style.borderColor='#8b5cf6'"
            @blur="($event.target as HTMLElement).style.borderColor='#334155'"
          />
          <button @click="loadUsers(1)"
            style="background:#8b5cf6;color:#fff;font-weight:600;font-size:13px;padding:10px 16px;border-radius:8px;border:none;cursor:pointer"
          >Suchen</button>
        </div>

        <!-- Table -->
        <div v-if="usersLoading" style="color:#64748b;font-size:13px;padding:20px 0">Lade Nutzer...</div>
        <template v-else>
          <div style="background:#1e293b;border-radius:12px;border:1px solid #334155;overflow:hidden">
            <table style="width:100%;border-collapse:collapse">
              <thead>
                <tr style="background:#0f172a">
                  <th v-for="h in ['Nutzer', 'E-Mail', 'Rolle', 'Bestätigt', 'Registriert', 'Aktion']" :key="h"
                    style="padding:10px 16px;text-align:left;font-size:11px;font-weight:600;color:#64748b;text-transform:uppercase;letter-spacing:.05em"
                  >{{ h }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in users" :key="u.id"
                  style="border-top:1px solid #0f172a"
                  onmouseover="this.style.background='#0f172a44'" onmouseout="this.style.background='transparent'"
                >
                  <td style="padding:12px 16px">
                    <RouterLink :to="`/u/${u.username}`"
                      style="font-size:13px;font-weight:600;color:#f1f5f9;text-decoration:none"
                      onmouseover="this.style.color='#8b5cf6'" onmouseout="this.style.color='#f1f5f9'"
                    >{{ u.username }}</RouterLink>
                  </td>
                  <td style="padding:12px 16px;font-size:12px;color:#64748b">{{ u.email }}</td>
                  <td style="padding:12px 16px">
                    <span :style="roleBadgeStyle(u.role)">{{ u.role }}</span>
                  </td>
                  <td style="padding:12px 16px;font-size:12px" :style="{ color: u.isVerified ? '#10b981' : '#64748b' }">
                    {{ u.isVerified ? '✓' : '—' }}
                  </td>
                  <td style="padding:12px 16px;font-size:12px;color:#64748b">{{ formatDate(u.createdAt) }}</td>
                  <td style="padding:12px 16px">
                    <button
                      v-if="u.id !== auth.user?.id"
                      @click="toggleRole(u)"
                      :disabled="togglingId === u.id"
                      style="font-size:11px;font-weight:600;border-radius:6px;padding:4px 10px;border:1px solid;cursor:pointer;background:transparent;transition:all .15s"
                      :style="u.role === 'ADMIN'
                        ? 'color:#ef4444;border-color:#ef444455'
                        : 'color:#8b5cf6;border-color:#8b5cf655'"
                    >
                      {{ togglingId === u.id ? '...' : (u.role === 'ADMIN' ? 'Zu USER' : 'Zu ADMIN') }}
                    </button>
                    <span v-else style="font-size:11px;color:#334155">Du</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Pagination -->
          <div v-if="totalUsers > pageSize" style="display:flex;align-items:center;justify-content:space-between;margin-top:12px;font-size:13px;color:#64748b">
            <span>{{ totalUsers }} Nutzer gesamt</span>
            <div style="display:flex;gap:6px">
              <button @click="loadUsers(currentPage - 1)" :disabled="currentPage <= 1"
                style="padding:5px 12px;border-radius:6px;border:1px solid #334155;background:transparent;color:#94a3b8;cursor:pointer;disabled:opacity-.4"
                :style="{ opacity: currentPage <= 1 ? '0.4' : '1' }"
              >←</button>
              <span style="padding:5px 10px;color:#f1f5f9">{{ currentPage }} / {{ totalPages }}</span>
              <button @click="loadUsers(currentPage + 1)" :disabled="currentPage >= totalPages"
                style="padding:5px 12px;border-radius:6px;border:1px solid #334155;background:transparent;color:#94a3b8;cursor:pointer"
                :style="{ opacity: currentPage >= totalPages ? '0.4' : '1' }"
              >→</button>
            </div>
          </div>
        </template>
      </div>

      <!-- ── Stimmungs-Monitoring ────────────────────────────────── -->
      <div v-if="activeTab === 'moods'">
        <div v-if="moodStatsLoading" style="color:#64748b;font-size:13px;padding:20px 0">Lade Stimmungsdaten...</div>
        <template v-else-if="moodStats">
          <!-- Stats -->
          <div style="display:grid;gap:12px;margin-bottom:20px"
               :style="{ gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }"
          >
            <div v-for="c in moodStatCards" :key="c.label"
              style="background:#1e293b;border-radius:12px;padding:20px;border:1px solid #334155"
            >
              <div style="font-size:22px;margin-bottom:8px">{{ c.icon }}</div>
              <div style="font-size:24px;font-weight:700;color:#f8fafc">{{ c.value }}</div>
              <div style="font-size:12px;color:#64748b;margin-top:2px">{{ c.label }}</div>
            </div>
          </div>

          <!-- Emotion-Verteilung -->
          <div style="background:#1e293b;border-radius:12px;padding:20px;border:1px solid #334155;margin-bottom:16px">
            <div style="font-size:13px;font-weight:700;color:#94a3b8;margin-bottom:16px">Emotion-Verteilung</div>
            <div v-for="e in moodStats.emotionDistribution" :key="e.emotion" style="margin-bottom:10px">
              <div style="display:flex;justify-content:space-between;margin-bottom:4px">
                <span style="font-size:12px;font-weight:600" :style="{ color: emotionColor(e.emotion) }">
                  {{ emotionEmoji(e.emotion) }} {{ emotionLabel(e.emotion) }}
                </span>
                <span style="font-size:12px;color:#64748b">
                  {{ e.count }} ({{ moodStats.totalMoodEntries > 0 ? Math.round(e.count / moodStats.totalMoodEntries * 100) : 0 }}%)
                </span>
              </div>
              <div style="background:#0f172a;border-radius:99px;height:6px;overflow:hidden">
                <div style="height:100%;border-radius:99px;transition:width .4s"
                  :style="{
                    width: moodStats.totalMoodEntries > 0 ? (e.count / moodStats.totalMoodEntries * 100) + '%' : '0%',
                    background: emotionColor(e.emotion),
                  }"
                />
              </div>
            </div>
          </div>

          <!-- Neueste Stimmungen -->
          <div style="background:#1e293b;border-radius:12px;border:1px solid #334155;overflow:hidden">
            <div style="padding:16px 20px;border-bottom:1px solid #334155">
              <span style="font-size:13px;font-weight:700;color:#94a3b8">Neueste Stimmungs-Einträge</span>
            </div>
            <div v-for="(m, i) in moodStats.recentMoods" :key="i"
              style="display:flex;align-items:center;justify-content:space-between;padding:10px 16px;border-top:1px solid #0f172a"
            >
              <div style="display:flex;align-items:center;gap:10px">
                <span style="font-size:18px">{{ emotionEmoji(m.emotion) }}</span>
                <div>
                  <span style="font-size:13px;font-weight:600" :style="{ color: emotionColor(m.emotion) }">
                    {{ emotionLabel(m.emotion) }}
                  </span>
                  <span style="font-size:11px;color:#64748b;margin-left:6px">Intensität {{ m.intensity }}/10</span>
                </div>
              </div>
              <div style="font-size:11px;color:#475569">{{ formatDate(m.setAt) }}</div>
            </div>
          </div>
        </template>
      </div>

      <!-- ── System ───────────────────────────────────────────────── -->
      <div v-if="activeTab === 'system'">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13px;color:#64748b">Letzte Aktualisierung: {{ systemLastCheck }}</span>
          <button @click="loadSystemHealth"
            :disabled="systemLoading"
            style="font-size:12px;color:#8b5cf6;border:1px solid #8b5cf655;border-radius:6px;padding:5px 12px;background:transparent;cursor:pointer"
          >{{ systemLoading ? '...' : 'Aktualisieren' }}</button>
        </div>

        <div v-if="systemLoading && !systemHealth" style="color:#64748b;font-size:13px;padding:20px 0">Prüfe Services...</div>
        <template v-else-if="systemHealth">
          <!-- Services -->
          <div style="background:#1e293b;border-radius:12px;border:1px solid #334155;overflow:hidden;margin-bottom:16px">
            <div style="padding:16px 20px;border-bottom:1px solid #334155">
              <span style="font-size:13px;font-weight:700;color:#94a3b8">Services</span>
            </div>
            <div v-for="s in systemHealth.services" :key="s.name"
              style="display:flex;align-items:center;justify-content:space-between;padding:12px 16px;border-top:1px solid #0f172a"
            >
              <div style="display:flex;align-items:center;gap:10px">
                <div style="width:10px;height:10px;border-radius:50%;flex-shrink:0"
                  :style="{ background: s.ok ? '#10b981' : '#ef4444' }"
                />
                <span style="font-size:13px;font-weight:600;color:#f1f5f9">{{ serviceLabel(s.name) }}</span>
              </div>
              <div style="display:flex;align-items:center;gap:12px">
                <span v-if="s.responseMs > 0" style="font-size:11px;color:#64748b">{{ s.responseMs }}ms</span>
                <span style="font-size:11px;font-weight:600;padding:2px 8px;border-radius:99px"
                  :style="s.ok
                    ? 'background:#10b98122;color:#10b981;border:1px solid #10b98144'
                    : 'background:#ef444422;color:#ef4444;border:1px solid #ef444444'"
                >{{ s.ok ? 'UP' : 'DOWN' }}</span>
              </div>
            </div>
          </div>

        </template>
      </div>

      <!-- Toast -->
      <Transition name="fade">
        <div v-if="toast"
          style="position:fixed;bottom:24px;right:24px;background:#1e293b;border:1px solid #334155;border-radius:10px;padding:12px 18px;font-size:13px;color:#e2e8f0;box-shadow:0 4px 20px #00000066"
        >{{ toast }}</div>
      </Transition>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'
import { adminApi, type AdminUser, type AdminAuthStats, type AdminUserStats, type MoodAdminStats, type SystemHealth } from '../../api/admin'

const auth = useAuthStore()

const activeTab   = ref<'overview' | 'users' | 'moods' | 'system'>('overview')
const tabs = [
  { id: 'overview' as const, label: 'Übersicht' },
  { id: 'users'    as const, label: 'Nutzer' },
  { id: 'moods'    as const, label: 'Stimmungen' },
  { id: 'system'   as const, label: 'System' },
]

// ── Overview ──────────────────────────────────────────────────────────────────
const statsLoading = ref(true)
const authStats    = ref<AdminAuthStats | null>(null)
const userStats    = ref<AdminUserStats | null>(null)
const recentUsers  = ref<AdminUser[]>([])

const statCards = computed(() => [
  { icon: '👥', value: authStats.value?.totalUsers   ?? '—', label: 'Nutzer gesamt' },
  { icon: '🆕', value: authStats.value?.usersToday   ?? '—', label: 'Heute neu' },
  { icon: '📅', value: authStats.value?.usersThisWeek ?? '—', label: 'Diese Woche' },
  { icon: '🔗', value: userStats.value?.totalFriendships ?? '—', label: 'Freundschaften' },
  { icon: '🛡️', value: authStats.value?.adminCount   ?? '—', label: 'Admins' },
  { icon: '✅', value: authStats.value?.verifiedCount ?? '—', label: 'Verifiziert' },
])

// ── Users ─────────────────────────────────────────────────────────────────────
const usersLoading = ref(false)
const users        = ref<AdminUser[]>([])
const search       = ref('')
const currentPage  = ref(1)
const totalUsers   = ref(0)
const pageSize     = 20
const togglingId   = ref<string | null>(null)
const toast        = ref('')

const totalPages = computed(() => Math.ceil(totalUsers.value / pageSize))

function showToast(msg: string) {
  toast.value = msg
  setTimeout(() => toast.value = '', 3000)
}

// ── Stimmungs-Monitoring ──────────────────────────────────────────────────────
const moodStatsLoading = ref(false)
const moodStats        = ref<MoodAdminStats | null>(null)

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
const emotionLabel = (e: string) => EMOTION_META[e]?.label ?? e
const emotionEmoji = (e: string) => EMOTION_META[e]?.emoji ?? '😶'
const emotionColor = (e: string) => EMOTION_META[e]?.color ?? '#64748b'

const moodStatCards = computed(() => moodStats.value ? [
  { icon: '📊', value: moodStats.value.totalMoodEntries, label: 'Stimmungs-Einträge' },
  { icon: '👤', value: moodStats.value.activeUsers,      label: 'Aktive Nutzer' },
  { icon: '⚡', value: moodStats.value.averageIntensity.toFixed(1), label: 'Ø Intensität' },
] : [])

async function loadMoodStats() {
  moodStatsLoading.value = true
  try {
    const res = await adminApi.getMoodStats(auth.accessToken)
    moodStats.value = res.data
  } finally {
    moodStatsLoading.value = false
  }
}

// ── System Health ─────────────────────────────────────────────────────────────
const systemLoading   = ref(false)
const systemHealth    = ref<SystemHealth | null>(null)
const systemLastCheck = ref('—')

const SERVICE_LABELS: Record<string, string> = {
  'moodavatar-backend': 'Backend (Monolith)',
}
const serviceLabel = (name: string) => SERVICE_LABELS[name] ?? name

async function loadSystemHealth() {
  systemLoading.value = true
  try {
    const res = await adminApi.getSystemHealth()
    systemHealth.value    = res.data
    systemLastCheck.value = new Date().toLocaleTimeString('de-DE')
  } finally {
    systemLoading.value = false
  }
}

function roleBadgeStyle(role: string) {
  return role === 'ADMIN'
    ? 'font-size:11px;font-weight:700;padding:2px 8px;border-radius:99px;background:#8b5cf622;color:#8b5cf6;border:1px solid #8b5cf655'
    : 'font-size:11px;font-weight:600;padding:2px 8px;border-radius:99px;background:#33415522;color:#64748b;border:1px solid #33415555'
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

async function loadUsers(page: number) {
  usersLoading.value = true
  currentPage.value  = page
  try {
    const res = await adminApi.listUsers(auth.accessToken, page, pageSize, search.value)
    users.value      = res.data.users
    totalUsers.value = res.data.total
  } finally {
    usersLoading.value = false
  }
}

async function toggleRole(user: AdminUser) {
  togglingId.value = user.id
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    const res = await adminApi.updateRole(auth.accessToken, user.id, newRole)
    const idx = users.value.findIndex(u => u.id === user.id)
    if (idx !== -1) users.value[idx] = res.data
    // Update recent users list too
    const ridx = recentUsers.value.findIndex(u => u.id === user.id)
    if (ridx !== -1) recentUsers.value[ridx] = res.data
    showToast(`${res.data.username} ist jetzt ${newRole}`)
  } catch {
    showToast('Fehler beim Ändern der Rolle')
  } finally {
    togglingId.value = null
  }
}

onMounted(async () => {
  const token = auth.accessToken
  const [authRes, userRes, usersRes] = await Promise.allSettled([
    adminApi.getAuthStats(token),
    adminApi.getUserStats(token),
    adminApi.listUsers(token, 1, 5),
  ])
  if (authRes.status  === 'fulfilled') authStats.value  = authRes.value.data
  if (userRes.status  === 'fulfilled') userStats.value  = userRes.value.data
  if (usersRes.status === 'fulfilled') recentUsers.value = usersRes.value.data.users
  statsLoading.value = false

  // Preload other tabs
  loadUsers(1)
  loadMoodStats()
  loadSystemHealth()
})
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(8px); }
</style>
