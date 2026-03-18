<template>
  <div class="min-h-screen flex items-center justify-center px-4" style="background:#0f172a">
    <div class="w-full max-w-sm text-center">
      <!-- Loading -->
      <div v-if="loading">
        <div style="font-size:48px;margin-bottom:16px">⏳</div>
        <p style="color:#94a3b8;font-size:14px">E-Mail wird verifiziert...</p>
      </div>

      <!-- Success -->
      <div v-else-if="success">
        <div style="font-size:56px;margin-bottom:16px">✅</div>
        <h1 style="font-size:22px;font-weight:700;color:#f8fafc;margin-bottom:8px">E-Mail bestätigt!</h1>
        <p style="color:#64748b;font-size:13px;margin-bottom:28px">Deine E-Mail-Adresse wurde erfolgreich verifiziert.</p>
        <RouterLink :to="isLoggedIn ? '/dashboard' : '/login'"
          style="background:#10b981;color:#fff;font-weight:600;font-size:14px;padding:11px 28px;border-radius:8px;text-decoration:none;display:inline-block"
        >{{ isLoggedIn ? 'Zum Dashboard' : 'Zum Login' }}</RouterLink>
      </div>

      <!-- Error -->
      <div v-else>
        <div style="font-size:56px;margin-bottom:16px">❌</div>
        <h1 style="font-size:22px;font-weight:700;color:#f8fafc;margin-bottom:8px">Verifizierung fehlgeschlagen</h1>
        <p style="color:#64748b;font-size:13px;margin-bottom:8px">{{ errorMsg }}</p>
        <RouterLink to="/login"
          style="color:#10b981;font-size:13px;font-weight:500"
        >Zurück zum Login</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { authApi } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const route     = useRoute()
const auth      = useAuthStore()
const isLoggedIn = auth.isLoggedIn

const loading  = ref(true)
const success  = ref(false)
const errorMsg = ref('')

onMounted(async () => {
  const token = route.query.token as string | undefined
  if (!token) {
    errorMsg.value = 'Kein Verifizierungs-Token gefunden.'
    loading.value  = false
    return
  }
  try {
    await authApi.verifyEmail(token)
    success.value = true
    if (isLoggedIn) auth.markVerified()
  } catch (e: any) {
    const code = e.response?.data?.error
    if (code === 'TOKEN_EXPIRED')      errorMsg.value = 'Der Verifizierungs-Link ist abgelaufen.'
    else if (code === 'TOKEN_ALREADY_USED') errorMsg.value = 'Dieser Link wurde bereits verwendet.'
    else                               errorMsg.value = 'Ungültiger Verifizierungs-Link.'
  } finally {
    loading.value = false
  }
})
</script>
