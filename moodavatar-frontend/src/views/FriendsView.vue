<template>
  <AppLayout>
    <div>
      <h2 style="font-size:18px;font-weight:700;color:#f8fafc;margin-bottom:6px">Freunde</h2>
      <p style="font-size:13px;color:#64748b;margin-bottom:24px">Freundesliste verwalten und neue Leute finden</p>

      <!-- Tabs -->
      <div style="display:flex;gap:4px;background:#1e293b;border-radius:10px;padding:4px;width:fit-content;margin-bottom:24px">
        <button v-for="tab in tabs" :key="tab.id" @click="activeTab = tab.id"
          style="padding:6px 16px;border-radius:7px;font-size:13px;font-weight:500;border:none;cursor:pointer;transition:all 0.15s"
          :style="activeTab === tab.id
            ? 'background:#10b981;color:#fff'
            : 'background:transparent;color:#64748b'"
        >{{ tab.label }} <span v-if="tab.badge" style="margin-left:4px;background:#ef4444;color:#fff;border-radius:99px;padding:1px 6px;font-size:10px">{{ tab.badge }}</span></button>
      </div>

      <!-- Tab: Freunde -->
      <div v-if="activeTab === 'friends'">
        <div v-if="friends.length === 0" style="color:#64748b;font-size:13px;padding:20px 0">
          Noch keine Freunde. Suche nach Leuten im Tab "Suchen".
        </div>
        <div v-else class="flex flex-col gap-3">
          <FriendCard v-for="f in friends" :key="f.id" :profile="f"
            :online="realtime.isOnline(f.id)"
            :mood="realtime.friendMoods.get(f.id) ? `${realtime.friendMoods.get(f.id)!.emotion} · ${realtime.friendMoods.get(f.id)!.intensity}/10` : ''"
          >
            <button @click="removeFriend(f.id)"
              style="font-size:12px;color:#64748b;border:1px solid #334155;border-radius:6px;padding:4px 10px;background:transparent;cursor:pointer"
              onmouseover="this.style.color='#ef4444';this.style.borderColor='#ef4444'" onmouseout="this.style.color='#64748b';this.style.borderColor='#334155'"
            >Entfernen</button>
          </FriendCard>
        </div>
      </div>

      <!-- Tab: Anfragen -->
      <div v-if="activeTab === 'requests'">
        <div v-if="requests.length === 0" style="color:#64748b;font-size:13px;padding:20px 0">
          Keine offenen Anfragen.
        </div>
        <div v-else class="flex flex-col gap-3">
          <div v-for="req in requests" :key="req.id"
            style="background:#1e293b;border-radius:10px;padding:14px 16px;border:1px solid #334155;display:flex;align-items:center;justify-content:space-between"
          >
            <RouterLink v-if="req.senderUsername" :to="`/u/${req.senderUsername}`"
              style="display:flex;align-items:center;gap:12px;text-decoration:none;flex:1"
            >
              <div style="width:36px;height:36px;border-radius:50%;background:#334155;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:700;color:#94a3b8">
                {{ req.senderUsername.charAt(0).toUpperCase() }}
              </div>
              <div>
                <div style="font-size:13px;font-weight:600;color:#f1f5f9">{{ req.senderUsername }}</div>
                <div style="font-size:11px;color:#64748b">Freundschaftsanfrage</div>
              </div>
            </RouterLink>
            <div v-else style="display:flex;align-items:center;gap:12px;flex:1">
              <div style="width:36px;height:36px;border-radius:50%;background:#334155;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:700;color:#94a3b8">?</div>
              <div>
                <div style="font-size:13px;font-weight:600;color:#f1f5f9">{{ req.senderId }}</div>
                <div style="font-size:11px;color:#64748b">Freundschaftsanfrage</div>
              </div>
            </div>
            <div class="flex gap-2">
              <button @click="respond(req.id, 'ACCEPT')"
                style="font-size:12px;font-weight:600;color:#fff;background:#10b981;border:none;border-radius:6px;padding:5px 12px;cursor:pointer"
              >Annehmen</button>
              <button @click="respond(req.id, 'DECLINE')"
                style="font-size:12px;color:#64748b;border:1px solid #334155;border-radius:6px;padding:5px 12px;background:transparent;cursor:pointer"
              >Ablehnen</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab: Suchen -->
      <div v-if="activeTab === 'search'">
        <div style="display:flex;gap:8px;margin-bottom:16px">
          <input v-model="searchQuery" @keyup.enter="doSearch" placeholder="Username suchen..."
            style="flex:1;background:#1e293b;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
            @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
            @blur="($event.target as HTMLElement).style.borderColor='#334155'"
          />
          <button @click="doSearch"
            style="background:#10b981;color:#fff;font-weight:600;font-size:13px;padding:10px 16px;border-radius:8px;border:none;cursor:pointer"
          >Suchen</button>
        </div>

        <div v-if="searchResults.length > 0" class="flex flex-col gap-3">
          <FriendCard v-for="p in searchResults" :key="p.id" :profile="p">
            <button @click="sendRequest(p.id)"
              style="font-size:12px;font-weight:500;color:#10b981;border:1px solid #10b981;border-radius:6px;padding:4px 10px;background:transparent;cursor:pointer"
            >Anfrage senden</button>
          </FriendCard>
        </div>
        <div v-else-if="searched && searchResults.length === 0"
          style="color:#64748b;font-size:13px;padding:20px 0"
        >Keine Ergebnisse für "{{ searchQuery }}"</div>
      </div>

      <!-- Toast -->
      <Transition name="fade">
        <div v-if="toast" style="position:fixed;bottom:24px;right:24px;background:#1e293b;border:1px solid #334155;border-radius:10px;padding:12px 18px;font-size:13px;color:#e2e8f0;box-shadow:0 4px 20px #00000066">
          {{ toast }}
        </div>
      </Transition>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineComponent, h } from 'vue'
import { RouterLink } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { useAuthStore } from '../stores/auth'
import { useRealtimeStore } from '../stores/realtime'
import { userApi, type Profile, type FriendRequest } from '../api/user'

const FriendCard = defineComponent({
  props: {
    profile: Object as () => Profile,
    online:  { type: Boolean, default: false },
    mood:    { type: String,  default: '' },
  },
  setup(props, { slots }) {
    return () => h('div', {
      style: 'background:#1e293b;border-radius:10px;padding:14px 16px;border:1px solid #334155;display:flex;align-items:center;gap:12px'
    }, [
      h(RouterLink, { to: `/u/${props.profile!.username}`, style: 'display:flex;align-items:center;gap:12px;text-decoration:none;flex:1;min-width:0' }, () => [
        h('div', { style: 'position:relative;width:36px;height:36px;flex-shrink:0' }, [
          h('div', { style: 'width:36px;height:36px;border-radius:50%;background:#334155;display:flex;align-items:center;justify-content:center;font-size:15px;font-weight:700;color:#94a3b8' },
            props.profile!.username.charAt(0).toUpperCase()),
          h('div', { style: `position:absolute;bottom:0;right:0;width:10px;height:10px;border-radius:50%;border:2px solid #1e293b;background:${props.online ? '#10b981' : '#475569'}` }),
        ]),
        h('div', {}, [
          h('div', { style: 'font-size:13px;font-weight:600;color:#f1f5f9' }, props.profile!.displayName ?? props.profile!.username),
          h('div', { style: 'font-size:11px;color:#64748b' },
            props.mood ? props.mood : ('@' + props.profile!.username)),
        ])
      ]),
      slots.default?.()
    ])
  }
})

const auth          = useAuthStore()
const realtime      = useRealtimeStore()
const friends       = ref<Profile[]>([])
const requests      = ref<FriendRequest[]>([])
const searchQuery   = ref('')
const searchResults = ref<Profile[]>([])
const searched      = ref(false)
const activeTab     = ref<'friends' | 'requests' | 'search'>('friends')
const toast         = ref('')

const tabs = computed(() => [
  { id: 'friends'  as const, label: 'Freunde',   badge: null },
  { id: 'requests' as const, label: 'Anfragen',  badge: requests.value.length || null },
  { id: 'search'   as const, label: 'Suchen',    badge: null },
])

function showToast(msg: string) {
  toast.value = msg
  setTimeout(() => toast.value = '', 3000)
}

onMounted(async () => {
  const token = auth.accessToken
  const [f, r] = await Promise.all([
    userApi.getFriends(token).catch(() => ({ data: [] })),
    userApi.getRequests(token).catch(() => ({ data: [] })),
  ])
  friends.value  = f.data
  requests.value = r.data
})

async function doSearch() {
  if (!searchQuery.value.trim()) return
  searched.value = true
  const { data } = await userApi.search(auth.accessToken, searchQuery.value)
  searchResults.value = data.filter(p => p.id !== auth.user?.id)
}

async function sendRequest(receiverId: string) {
  try {
    await userApi.sendRequest(auth.accessToken, receiverId)
    showToast('Anfrage gesendet ✓')
  } catch (e: any) {
    showToast(e.response?.data?.error === 'REQUEST_ALREADY_SENT' ? 'Anfrage bereits gesendet' : 'Fehler beim Senden')
  }
}

async function respond(requestId: string, action: 'ACCEPT' | 'DECLINE') {
  await userApi.respondRequest(auth.accessToken, requestId, action)
  requests.value = requests.value.filter(r => r.id !== requestId)
  if (action === 'ACCEPT') {
    const { data } = await userApi.getFriends(auth.accessToken)
    friends.value = data
    showToast('Freundschaft angenommen ✓')
  } else {
    showToast('Anfrage abgelehnt')
  }
}

async function removeFriend(friendId: string) {
  await userApi.removeFriend(auth.accessToken, friendId)
  friends.value = friends.value.filter(f => f.id !== friendId)
  showToast('Freund entfernt')
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(8px); }
</style>
