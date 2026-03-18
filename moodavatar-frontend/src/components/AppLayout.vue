<template>
  <div class="flex flex-col" style="background:#0f172a;height:100vh;overflow:hidden">
    <!-- Navbar -->
    <nav style="background:#1e293b;border-bottom:1px solid #334155" class="px-6 py-3 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <span style="font-size:20px">🎭</span>
        <span style="font-weight:700;color:#f8fafc;font-size:15px">MoodAvatar</span>
      </div>
      <div class="flex items-center gap-1">
        <RouterLink v-for="link in links" :key="link.to" :to="link.to"
          class="px-3 py-1.5 rounded-lg text-sm transition-colors"
          :style="isActive(link.to)
            ? 'background:#10b98122;color:#10b981;font-weight:600'
            : 'color:#94a3b8'"
        >{{ link.label }}</RouterLink>

        <!-- Notification bell -->
        <div data-notifications style="position:relative;margin-left:8px">
          <button @click="toggleNotifications"
            style="position:relative;padding:6px 8px;border-radius:8px;border:none;background:transparent;cursor:pointer;color:#94a3b8;font-size:16px;line-height:1"
            :style="showNotifications ? 'background:#1e293b' : ''"
          >
            🔔
            <span v-if="unreadCount > 0"
              style="position:absolute;top:2px;right:2px;background:#ef4444;color:#fff;border-radius:99px;font-size:9px;font-weight:700;padding:1px 4px;min-width:14px;text-align:center;line-height:14px"
            >{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
          </button>

          <!-- Dropdown -->
          <Transition name="drop">
            <div v-if="showNotifications"
              style="position:absolute;right:0;top:calc(100% + 6px);width:300px;background:#1e293b;border:1px solid #334155;border-radius:12px;box-shadow:0 8px 32px #00000066;z-index:100;overflow:hidden"
            >
              <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 16px;border-bottom:1px solid #334155">
                <span style="font-size:13px;font-weight:700;color:#f1f5f9">Benachrichtigungen</span>
                <button v-if="unreadCount > 0" @click="markAllRead"
                  style="font-size:11px;color:#10b981;background:none;border:none;cursor:pointer"
                >Alle gelesen</button>
              </div>

              <div style="max-height:320px;overflow-y:auto">
                <div v-if="notifications.length === 0"
                  style="padding:20px;text-align:center;color:#64748b;font-size:13px"
                >Keine Benachrichtigungen</div>

                <div v-for="n in notifications" :key="n.id"
                  @click="markRead(n)"
                  :style="`padding:12px 16px;cursor:pointer;border-bottom:1px solid #1e293b;display:flex;gap:10px;align-items:flex-start;background:${n.read ? 'transparent' : '#0f172a'}`"
                  onmouseover="this.style.background='#334155'" onmouseout="this.style.background=this.dataset.bg"
                  :data-bg="n.read ? 'transparent' : '#0f172a'"
                >
                  <span style="font-size:18px;line-height:1.4">{{ notificationIcon(n.type) }}</span>
                  <div style="flex:1">
                    <div style="font-size:12px;font-weight:600;color:#f1f5f9">{{ notificationText(n) }}</div>
                    <div style="font-size:10px;color:#64748b;margin-top:2px">{{ timeAgo(n.createdAt) }}</div>
                  </div>
                  <div v-if="!n.read" style="width:7px;height:7px;border-radius:50%;background:#10b981;margin-top:5px;flex-shrink:0"/>
                </div>
              </div>
            </div>
          </Transition>
        </div>

        <button @click="handleLogout"
          class="ml-2 px-3 py-1.5 rounded-lg text-sm transition-colors"
          style="color:#64748b;border:1px solid #334155"
          onmouseover="this.style.color='#ef4444'" onmouseout="this.style.color='#64748b'"
        >Logout</button>
      </div>
    </nav>

    <!-- Email verification banner -->
    <div v-if="auth.user && !auth.user.isVerified"
      style="background:#451a0322;border-bottom:1px solid #92400e;padding:8px 24px;display:flex;align-items:center;justify-content:space-between;gap:16px"
    >
      <span style="font-size:13px;color:#fbbf24">⚠️ Bitte bestätige deine E-Mail-Adresse. Prüfe dein Postfach.</span>
      <button @click="resendVerification"
        style="font-size:12px;font-weight:600;color:#fbbf24;background:transparent;border:1px solid #92400e;border-radius:6px;padding:3px 10px;cursor:pointer;white-space:nowrap;flex-shrink:0"
        :disabled="resendSent"
      >{{ resendSent ? 'Gesendet ✓' : 'Erneut senden' }}</button>
    </div>

    <!-- Content -->
    <main :class="flush ? 'flex-1 overflow-hidden w-full' : 'flex-1 px-6 py-8 w-full overflow-y-auto'">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { notificationApi, type Notification } from '../api/notification'
import { authApi } from '../api/auth'

const props  = defineProps<{ flush?: boolean }>()
const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()

const links = computed(() => [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/profile',   label: 'Profil' },
  { to: '/friends',   label: 'Freunde' },
  ...(auth.user?.role === 'ADMIN' ? [{ to: '/admin', label: 'Admin' }] : []),
])

const isActive = (path: string) => route.path === path

const resendSent = ref(false)

async function resendVerification() {
  if (!auth.user?.email || resendSent.value) return
  await authApi.resendVerification(auth.user.email).catch(() => {})
  resendSent.value = true
  setTimeout(() => resendSent.value = false, 60_000)
}

const notifications   = ref<Notification[]>([])
const unreadCount     = ref(0)
const showNotifications = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadNotifications() {
  try {
    const [listRes, countRes] = await Promise.all([
      notificationApi.list(auth.accessToken),
      notificationApi.unreadCount(auth.accessToken),
    ])
    notifications.value = listRes.data
    unreadCount.value   = countRes.data.count
  } catch { /* ignore */ }
}

function toggleNotifications() {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value) loadNotifications()
}

async function markRead(n: Notification) {
  if (!n.read) {
    n.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    await notificationApi.markRead(auth.accessToken, n.id)
  }
}

async function markAllRead() {
  notifications.value.forEach(n => n.read = true)
  unreadCount.value = 0
  await notificationApi.markAllRead(auth.accessToken)
}

function notificationIcon(type: string) {
  if (type === 'FRIEND_REQUEST') return '👤'
  if (type === 'FRIEND_ACCEPTED') return '🤝'
  if (type === 'ROOM_VISIT') return '🚶'
  return '🔔'
}

function notificationText(n: Notification) {
  if (n.type === 'FRIEND_REQUEST') return `${n.fromUsername} hat dir eine Freundschaftsanfrage gesendet`
  if (n.type === 'FRIEND_ACCEPTED') return `${n.fromUsername} hat deine Anfrage angenommen`
  if (n.type === 'ROOM_VISIT') return `${n.fromUsername} hat deinen Raum besucht`
  return `Neue Benachrichtigung von ${n.fromUsername}`
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1)  return 'gerade eben'
  if (m < 60) return `vor ${m} Min.`
  const h = Math.floor(m / 60)
  if (h < 24) return `vor ${h} Std.`
  return `vor ${Math.floor(h / 24)} Tag(en)`
}

// Close dropdown on outside click
function onClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('[data-notifications]')) showNotifications.value = false
}

onMounted(() => {
  loadNotifications()
  pollTimer = setInterval(loadNotifications, 30_000)
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  clearInterval(pollTimer!)
  document.removeEventListener('click', onClickOutside)
})

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.drop-enter-active, .drop-leave-active { transition: opacity 0.15s, transform 0.15s; }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
