<template>
  <div class="min-h-screen flex items-center justify-center px-4" style="background:#0f172a">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <div style="font-size:48px;margin-bottom:8px">🎭</div>
        <h1 style="font-size:22px;font-weight:700;color:#f8fafc">MoodAvatar</h1>
        <p style="color:#64748b;font-size:13px;margin-top:4px">Konto erstellen</p>
      </div>

      <div style="background:#1e293b;border:1px solid #334155;border-radius:16px;padding:28px">
        <form @submit.prevent="handleRegister" class="flex flex-col gap-4">
          <div v-if="error" style="background:#ef444422;border:1px solid #ef4444;border-radius:8px;padding:10px 14px;color:#ef4444;font-size:13px">
            {{ error }}
          </div>

          <div class="flex flex-col gap-1.5">
            <label style="font-size:12px;color:#94a3b8;font-weight:500">Benutzername</label>
            <input v-model="username" type="text" required placeholder="dein_username" minlength="3"
              style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
              @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
              @blur="($event.target as HTMLElement).style.borderColor='#334155'"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label style="font-size:12px;color:#94a3b8;font-weight:500">E-Mail</label>
            <input v-model="email" type="email" required placeholder="name@example.com"
              style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
              @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
              @blur="($event.target as HTMLElement).style.borderColor='#334155'"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label style="font-size:12px;color:#94a3b8;font-weight:500">Passwort</label>
            <input v-model="password" type="password" required placeholder="Min. 8 Zeichen" minlength="8"
              style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
              @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
              @blur="($event.target as HTMLElement).style.borderColor='#334155'"
            />
          </div>

          <button type="submit" :disabled="loading"
            style="background:#10b981;color:#fff;font-weight:600;font-size:14px;padding:11px;border-radius:8px;border:none;cursor:pointer;transition:background 0.2s;margin-top:4px"
            onmouseover="if(!this.disabled)this.style.background='#059669'" onmouseout="this.style.background='#10b981'"
          >{{ loading ? 'Registrieren...' : 'Konto erstellen' }}</button>
        </form>

        <p style="text-align:center;font-size:13px;color:#64748b;margin-top:20px">
          Bereits Konto?
          <RouterLink to="/login" style="color:#10b981;font-weight:500"> Anmelden</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth   = useAuthStore()

const username = ref('')
const email    = ref('')
const password = ref('')
const loading  = ref(false)
const error    = ref('')

async function handleRegister() {
  error.value   = ''
  loading.value = true
  try {
    await auth.register(email.value, username.value, password.value)
    router.push('/dashboard')
  } catch (e: any) {
    const msg = e.response?.data?.error ?? ''
    if (msg === 'EMAIL_TAKEN')    error.value = 'Diese E-Mail ist bereits vergeben.'
    else if (msg === 'USERNAME_TAKEN') error.value = 'Dieser Benutzername ist bereits vergeben.'
    else error.value = e.response?.data?.message ?? 'Registrierung fehlgeschlagen'
  } finally {
    loading.value = false
  }
}
</script>
