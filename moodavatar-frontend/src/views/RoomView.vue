<template>
  <AppLayout>
    <div v-if="loading" style="color:#64748b;font-size:14px;padding:40px;text-align:center">
      Lade Raum...
    </div>

    <div v-else-if="!ownerProfile" style="color:#64748b;font-size:14px;padding:40px;text-align:center">
      Raum nicht gefunden.
    </div>

    <div v-else>
      <!-- Header -->
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
        <div>
          <h1 style="font-size:18px;font-weight:700;color:#f8fafc">
            {{ ownerProfile.displayName ?? ownerProfile.username }}'s Raum
          </h1>
          <div style="font-size:12px;color:#64748b;margin-top:2px">
            <span :style="`color:${realtime.isOnline(ownerProfile.id) ? '#10b981' : '#475569'}`">
              {{ realtime.isOnline(ownerProfile.id) ? '● online' : '○ offline' }}
            </span>
            <span v-if="allVisitors.length > 0" style="margin-left:10px">
              · {{ allVisitors.length }} {{ allVisitors.length === 1 ? 'Besucher' : 'Besucher' }}
            </span>
          </div>
        </div>
        <RouterLink to="/dashboard" style="font-size:12px;color:#64748b;text-decoration:none">← Zurück</RouterLink>
      </div>

      <!-- Room -->
      <div style="position:relative;margin-bottom:16px">
        <AvatarRoom
          :emotion="ownerAvatar?.emotion ?? null"
          :config="ownerAvatar?.config ?? null"
          :visitors="allVisitors"
        />

        <!-- Floating reactions overlay -->
        <div
          v-for="r in floatingReactions" :key="r.id"
          class="floating-reaction"
          :style="{ left: r.x + 'px', bottom: r.y + 'px' }"
        >{{ r.emoji }}</div>
      </div>

      <!-- Action buttons -->
      <div style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap">
        <button
          v-for="emoji in REACTIONS" :key="emoji"
          @click="sendReaction(emoji)"
          style="font-size:20px;padding:8px 14px;border-radius:12px;border:1px solid #334155;background:#1e293b;cursor:pointer;transition:background 0.15s"
          :style="{ background: lastReaction === emoji ? '#334155' : '#1e293b' }"
        >{{ emoji }}</button>

        <button
          @click="knock"
          :disabled="knockSent"
          style="font-size:13px;padding:8px 16px;border-radius:12px;border:1px solid #334155;background:#1e293b;color:#94a3b8;cursor:pointer;margin-left:auto"
          :style="{ opacity: knockSent ? 0.5 : 1 }"
        >
          {{ knockSent ? '✓ Angeklopft' : '🚪 Anklopfen' }}
        </button>
      </div>

      <!-- Visitor list -->
      <div v-if="allVisitors.length > 0"
        style="background:#1e293b;border-radius:12px;padding:16px;border:1px solid #334155"
      >
        <div style="font-size:12px;font-weight:700;color:#64748b;margin-bottom:10px">Im Raum</div>
        <div style="display:flex;flex-wrap:wrap;gap:8px">
          <div v-for="v in allVisitors" :key="v.userId"
            style="display:flex;align-items:center;gap:6px;padding:5px 10px;border-radius:20px;background:#0f172a;border:1px solid #334155;font-size:12px;color:#94a3b8"
          >
            <span style="width:8px;height:8px;border-radius:50%;background:#10b981;display:inline-block"/>
            {{ v.username }}
            <span v-if="v.userId === myUserId" style="color:#475569">(ich)</span>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import AvatarRoom from '../components/AvatarRoom.vue'
import { useAuthStore } from '../stores/auth'
import { useRealtimeStore } from '../stores/realtime'
import { userApi, type Profile } from '../api/user'
import { avatarApi, type Avatar } from '../api/avatar'

const REACTIONS = ['❤️', '🤗', '💪', '✨', '😂']

const route        = useRoute()
const auth         = useAuthStore()
const realtime     = useRealtimeStore()

const ownerProfile = ref<Profile | null>(null)
const ownerAvatar  = ref<Avatar | null>(null)
const myAvatar     = ref<Avatar | null>(null)
const loading      = ref(true)
const knockSent    = ref(false)
const lastReaction = ref<string | null>(null)

interface FloatingReaction { id: number; emoji: string; x: number; y: number }
const floatingReactions = ref<FloatingReaction[]>([])
let reactionId = 0

const myUserId = computed(() => auth.user?.id ?? '')

// Combine visitors from realtime store: visitors in the owner's room from our perspective
const allVisitors = computed(() => {
  if (!ownerProfile.value) return []
  // We're visiting → use visitingRoomState + include ourselves
  const others = realtime.visitingRoomState.filter(v => v.userId !== myUserId.value)
  const me = {
    userId:      myUserId.value,
    username:    auth.user?.username ?? '',
    emotion:     myAvatar.value?.emotion ?? null,
    skinColor:   myAvatar.value?.config?.skinColor ?? null,
    clothesColor: myAvatar.value?.config?.clothesColor ?? null,
    hairStyle:   myAvatar.value?.config?.hairStyle ?? null,
    hairColor:   myAvatar.value?.config?.secondaryColor ?? null,
  }
  return [...others, me]
})

function sendReaction(emoji: string) {
  if (!ownerProfile.value) return
  realtime.sendReaction(ownerProfile.value.id, emoji)
  lastReaction.value = emoji
  setTimeout(() => { lastReaction.value = null }, 1000)
}

function knock() {
  if (!ownerProfile.value || knockSent.value) return
  realtime.knockOnDoor(ownerProfile.value.id)
  knockSent.value = true
  // Reset after 30 seconds so they can knock again
  setTimeout(() => { knockSent.value = false }, 30_000)
}

// Handle incoming room_reaction_received to show floating emoji
let unsubscribe: (() => void) | null = null

onMounted(async () => {
  const username = route.params.username as string

  const [profileRes, avatarRes, myAvatarRes] = await Promise.allSettled([
    userApi.getPublicProfile(username),
    (async () => {
      const p = await userApi.getPublicProfile(username)
      return avatarApi.getPublicAvatar(p.data.id)
    })(),
    avatarApi.getMyAvatar(auth.accessToken),
  ])

  if (profileRes.status === 'fulfilled') {
    ownerProfile.value = profileRes.value.data
  }
  if (avatarRes.status === 'fulfilled') {
    ownerAvatar.value = avatarRes.value.data
  }
  if (myAvatarRes.status === 'fulfilled') {
    myAvatar.value = myAvatarRes.value.data
  }

  loading.value = false

  if (ownerProfile.value) {
    // Join the room via WebSocket
    realtime.joinRoom(
      ownerProfile.value.id,
      myAvatar.value?.emotion ?? null,
      myAvatar.value?.config?.skinColor ?? null,
      myAvatar.value?.config?.clothesColor ?? null,
      myAvatar.value?.config?.hairStyle ?? null,
      myAvatar.value?.config?.secondaryColor ?? null,
    )

    // Listen for reactions
    unsubscribe = realtime.onMessage((msg) => {
      if (msg.type === 'room_reaction_received') {
        const m = msg as { type: string; reaction: string }
        const id = ++reactionId
        const x = 80 + Math.random() * 200
        const y = 60 + Math.random() * 80
        floatingReactions.value.push({ id, emoji: m.reaction, x, y })
        setTimeout(() => {
          floatingReactions.value = floatingReactions.value.filter(r => r.id !== id)
        }, 2000)
      }
    })
  }
})

onUnmounted(() => {
  realtime.leaveRoom()
  unsubscribe?.()
})
</script>

<style scoped>
.floating-reaction {
  position: absolute;
  font-size: 28px;
  pointer-events: none;
  animation: floatUp 2s ease-out forwards;
}

@keyframes floatUp {
  0%   { opacity: 1;   transform: translateY(0) scale(1);    }
  70%  { opacity: 0.9; transform: translateY(-60px) scale(1.2); }
  100% { opacity: 0;   transform: translateY(-120px) scale(0.8); }
}
</style>
