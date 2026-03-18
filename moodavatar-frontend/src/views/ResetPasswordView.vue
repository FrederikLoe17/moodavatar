<template>
  <div class="min-h-screen flex items-center justify-center px-4" style="background:#0f172a">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <div style="font-size:48px;margin-bottom:8px">🔒</div>
        <h1 style="font-size:22px;font-weight:700;color:#f8fafc">Neues Passwort</h1>
        <p style="color:#64748b;font-size:13px;margin-top:4px">Wähle ein neues Passwort</p>
      </div>

      <div style="background:#1e293b;border:1px solid #334155;border-radius:16px;padding:28px">
        <!-- No token -->
        <div v-if="!token"
          style="text-align:center;color:#ef4444;font-size:13px;padding:12px 0"
        >
          Ungültiger oder fehlender Reset-Link.
          <RouterLink to="/forgot-password" style="color:#10b981;display:block;margin-top:8px">Neuen Link anfordern</RouterLink>
        </div>

        <!-- Success -->
        <div v-else-if="success" style="text-align:center">
          <div style="font-size:32px;margin-bottom:12px">✅</div>
          <p style="color:#10b981;font-size:14px;font-weight:600;margin-bottom:4px">Passwort geändert!</p>
          <p style="color:#64748b;font-size:13px;margin-bottom:20px">Du kannst dich jetzt einloggen.</p>
          <RouterLink to="/login"
            style="background:#10b981;color:#fff;font-weight:600;font-size:14px;padding:10px 24px;border-radius:8px;text-decoration:none;display:inline-block"
          >Zum Login</RouterLink>
        </div>

        <!-- Form -->
        <form v-else @submit.prevent="handleSubmit" class="flex flex-col gap-4">
          <div v-if="error"
            style="background:#ef444422;border:1px solid #ef4444;border-radius:8px;padding:10px 14px;color:#ef4444;font-size:13px"
          >{{ error }}</div>

          <div class="flex flex-col gap-1.5">
            <label style="font-size:12px;color:#94a3b8;font-weight:500">Neues Passwort</label>
            <input v-model="password" type="password" required placeholder="Mindestens 8 Zeichen"
              style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
              @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
              @blur="($event.target as HTMLElement).style.borderColor='#334155'"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label style="font-size:12px;color:#94a3b8;font-weight:500">Passwort bestätigen</label>
            <input v-model="confirm" type="password" required placeholder="Passwort wiederholen"
              style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
              @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
              @blur="($event.target as HTMLElement).style.borderColor='#334155'"
            />
          </div>

          <button type="submit" :disabled="loading"
            style="background:#10b981;color:#fff;font-weight:600;font-size:14px;padding:11px;border-radius:8px;border:none;cursor:pointer;margin-top:4px"
            :style="loading ? 'opacity:0.6;cursor:not-allowed' : ''"
          >{{ loading ? 'Speichern...' : 'Passwort speichern' }}</button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { authApi } from '../api/auth'

const route   = useRoute()
const token   = route.query.token as string | undefined
const password = ref('')
const confirm  = ref('')
const loading  = ref(false)
const success  = ref(false)
const error    = ref('')

async function handleSubmit() {
  error.value = ''
  if (password.value.length < 8) {
    error.value = 'Passwort muss mindestens 8 Zeichen lang sein.'
    return
  }
  if (password.value !== confirm.value) {
    error.value = 'Passwörter stimmen nicht überein.'
    return
  }
  loading.value = true
  try {
    await authApi.resetPassword(token!, password.value)
    success.value = true
  } catch (e: any) {
    const code = e.response?.data?.error
    if (code === 'TOKEN_EXPIRED')      error.value = 'Der Reset-Link ist abgelaufen. Bitte fordere einen neuen an.'
    else if (code === 'INVALID_TOKEN') error.value = 'Ungültiger Reset-Link.'
    else                               error.value = 'Fehler beim Speichern. Bitte versuche es erneut.'
  } finally {
    loading.value = false
  }
}
</script>
