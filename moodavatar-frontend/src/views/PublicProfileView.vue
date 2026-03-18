<template>
  <div class="min-h-screen" style="background:#0f172a">
    <!-- Nav -->
    <nav style="background:#1e293b;border-bottom:1px solid #334155" class="px-6 py-3 flex items-center justify-between">
      <RouterLink to="/" style="display:flex;align-items:center;gap:6px;text-decoration:none">
        <span style="font-size:20px">🎭</span>
        <span style="font-weight:700;color:#f8fafc;font-size:15px">MoodAvatar</span>
      </RouterLink>
      <div v-if="auth.isLoggedIn" class="flex items-center gap-1">
        <RouterLink to="/dashboard"
          class="px-3 py-1.5 rounded-lg text-sm"
          style="color:#94a3b8"
        >Dashboard</RouterLink>
        <RouterLink to="/friends"
          class="px-3 py-1.5 rounded-lg text-sm"
          style="color:#94a3b8"
        >Freunde</RouterLink>
      </div>
      <div v-else class="flex items-center gap-2">
        <RouterLink to="/login"
          class="px-3 py-1.5 rounded-lg text-sm"
          style="color:#94a3b8;border:1px solid #334155"
        >Anmelden</RouterLink>
        <RouterLink to="/register"
          class="px-3 py-1.5 rounded-lg text-sm"
          style="background:#10b981;color:#fff"
        >Registrieren</RouterLink>
      </div>
    </nav>

    <main class="max-w-lg mx-auto px-6 py-10">
      <!-- Loading -->
      <div v-if="loading" style="text-align:center;color:#64748b;padding:60px 0">
        Lade Profil...
      </div>

      <!-- Not found -->
      <div v-else-if="notFound" style="text-align:center;padding:60px 0">
        <div style="font-size:40px;margin-bottom:16px">🤷</div>
        <div style="font-size:18px;font-weight:700;color:#f1f5f9;margin-bottom:8px">Nutzer nicht gefunden</div>
        <div style="font-size:14px;color:#64748b">@{{ route.params.username }} existiert nicht.</div>
        <RouterLink to="/" style="display:inline-block;margin-top:20px;font-size:13px;color:#10b981">← Zurück</RouterLink>
      </div>

      <!-- Profile -->
      <template v-else-if="profile">
        <!-- Avatar -->
        <div style="display:flex;justify-content:center;margin-bottom:24px">
          <AvatarDisplay
            :emotion="avatar?.emotion ?? null"
            :config="avatar?.config ?? null"
            :size="220"
          />
        </div>

        <!-- Info card -->
        <div style="background:#1e293b;border-radius:16px;padding:28px;border:1px solid #334155;text-align:center">
          <div style="font-size:22px;font-weight:700;color:#f8fafc">
            {{ profile.displayName ?? profile.username }}
          </div>
          <div style="font-size:14px;color:#64748b;margin-top:4px">@{{ profile.username }}</div>

          <!-- Mood badge -->
          <div v-if="avatar"
            style="display:inline-flex;align-items:center;gap:6px;margin-top:14px;padding:6px 14px;border-radius:99px;font-size:13px;font-weight:600"
            :style="{
              background: emotionColor(avatar.emotion) + '22',
              border: `1px solid ${emotionColor(avatar.emotion)}55`,
              color: emotionColor(avatar.emotion),
            }"
          >
            {{ emotionEmoji(avatar.emotion) }}
            {{ emotionLabel(avatar.emotion) }}
            <span style="opacity:.65">· {{ avatar.intensity }}/10</span>
          </div>
          <div v-else-if="!avatarLoading"
            style="display:inline-block;margin-top:14px;padding:5px 12px;border-radius:99px;font-size:12px;color:#475569;border:1px solid #334155"
          >Noch keine Stimmung gesetzt</div>

          <!-- Bio -->
          <div v-if="profile.bio"
            style="margin-top:16px;color:#94a3b8;font-size:14px;line-height:1.6;white-space:pre-wrap"
          >{{ profile.bio }}</div>

          <div style="border-top:1px solid #334155;margin-top:20px;padding-top:20px">
            <!-- Own profile hint -->
            <div v-if="isOwnProfile" style="font-size:13px;color:#64748b">
              Das ist dein Profil.
              <RouterLink to="/profile" style="color:#10b981;margin-left:4px">Bearbeiten →</RouterLink>
            </div>

            <!-- Friend status (logged in, not own profile) -->
            <div v-else-if="auth.isLoggedIn">
              <div v-if="isFriend"
                style="display:flex;align-items:center;justify-content:center;gap:12px;flex-wrap:wrap"
              >
                <span style="color:#10b981;font-size:13px;font-weight:600">✓ Ihr seid Freunde</span>
                <RouterLink
                  :to="`/room/${profile.username}`"
                  style="font-size:13px;font-weight:600;padding:6px 16px;border-radius:8px;background:#1e3a5f;color:#60a5fa;border:1px solid #3b82f633;text-decoration:none"
                >🚪 Raum besuchen</RouterLink>
              </div>
              <div v-else-if="requestSent"
                style="font-size:13px;color:#64748b"
              >Anfrage gesendet</div>
              <button v-else
                @click="addFriend"
                :disabled="addingFriend"
                style="background:#10b981;color:#fff;border:none;border-radius:8px;padding:8px 22px;font-size:13px;font-weight:600;cursor:pointer;transition:opacity .15s"
                :style="{ opacity: addingFriend ? '0.6' : '1' }"
              >{{ addingFriend ? '...' : 'Freund hinzufügen' }}</button>
            </div>

            <!-- Not logged in -->
            <div v-else style="font-size:13px;color:#64748b">
              <RouterLink to="/login" style="color:#10b981">Anmelden</RouterLink>
              , um Freundschaftsanfragen zu senden.
            </div>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AvatarDisplay from '../components/AvatarDisplay.vue'
import { useAuthStore } from '../stores/auth'
import { userApi, type Profile } from '../api/user'
import { avatarApi, type Avatar, type Emotion } from '../api/avatar'

const route = useRoute()
const auth  = useAuthStore()

const profile      = ref<Profile | null>(null)
const avatar       = ref<Avatar | null>(null)
const loading      = ref(true)
const avatarLoading = ref(true)
const notFound     = ref(false)
const isFriend     = ref(false)
const requestSent  = ref(false)
const addingFriend = ref(false)

const isOwnProfile = computed(() => auth.isLoggedIn && profile.value?.id === auth.user?.id)

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

async function addFriend() {
  if (!profile.value || addingFriend.value) return
  addingFriend.value = true
  try {
    await userApi.sendRequest(auth.accessToken, profile.value.id)
    requestSent.value = true
  } catch { /* ignore */ } finally {
    addingFriend.value = false
  }
}

onMounted(async () => {
  const username = route.params.username as string

  // 1. Load public profile
  try {
    const res = await userApi.getPublicProfile(username)
    profile.value = res.data
  } catch {
    notFound.value = true
    loading.value  = false
    return
  }
  loading.value = false

  const userId = profile.value!.id

  // 2. Load avatar (public, no auth needed)
  avatarApi.getPublicAvatar(userId)
    .then(res => { avatar.value = res.data })
    .catch(() => { /* no avatar yet */ })
    .finally(() => { avatarLoading.value = false })

  // 3. If logged in, check friendship status
  if (auth.isLoggedIn && !isOwnProfile.value) {
    userApi.getFriends(auth.accessToken)
      .then(res => {
        isFriend.value = res.data.some(f => f.id === userId)
      })
      .catch(() => {})
  }
})
</script>
