<template>
  <div class="min-h-screen flex items-center justify-center px-4" style="background:#0f172a">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <div style="font-size:48px;margin-bottom:8px">🔑</div>
        <h1 style="font-size:22px;font-weight:700;color:#f8fafc">Passwort vergessen</h1>
        <p style="color:#64748b;font-size:13px;margin-top:4px">Wir senden dir einen Reset-Link</p>
      </div>

      <div style="background:#1e293b;border:1px solid #334155;border-radius:16px;padding:28px">
        <div v-if="sent"
          style="background:#10b98122;border:1px solid #10b981;border-radius:8px;padding:14px;color:#10b981;font-size:13px;text-align:center;margin-bottom:16px"
        >
          Falls diese E-Mail registriert ist, wurde ein Reset-Link gesendet. Bitte prüfe dein Postfach.
        </div>

        <form v-else @submit.prevent="handleSubmit" class="flex flex-col gap-4">
          <div v-if="error"
            style="background:#ef444422;border:1px solid #ef4444;border-radius:8px;padding:10px 14px;color:#ef4444;font-size:13px"
          >{{ error }}</div>

          <div class="flex flex-col gap-1.5">
            <label style="font-size:12px;color:#94a3b8;font-weight:500">E-Mail</label>
            <input v-model="email" type="email" required placeholder="name@example.com"
              style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
              @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
              @blur="($event.target as HTMLElement).style.borderColor='#334155'"
            />
          </div>

          <button type="submit" :disabled="loading"
            style="background:#10b981;color:#fff;font-weight:600;font-size:14px;padding:11px;border-radius:8px;border:none;cursor:pointer;margin-top:4px"
            :style="loading ? 'opacity:0.6;cursor:not-allowed' : ''"
          >{{ loading ? 'Senden...' : 'Reset-Link senden' }}</button>
        </form>

        <p style="text-align:center;font-size:13px;color:#64748b;margin-top:20px">
          <RouterLink to="/login" style="color:#10b981;font-weight:500">← Zurück zum Login</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { authApi } from '../api/auth'

const email   = ref('')
const loading = ref(false)
const sent    = ref(false)
const error   = ref('')

async function handleSubmit() {
  error.value   = ''
  loading.value = true
  try {
    await authApi.forgotPassword(email.value)
    sent.value = true
  } catch {
    error.value = 'Fehler beim Senden. Bitte versuche es erneut.'
  } finally {
    loading.value = false
  }
}
</script>
