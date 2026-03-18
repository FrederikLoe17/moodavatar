<template>
  <div class="min-h-screen flex items-center justify-center px-4" style="background:#0f172a">
    <div class="w-full max-w-sm">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div style="font-size:48px;margin-bottom:8px">🎭</div>
        <h1 style="font-size:22px;font-weight:700;color:#f8fafc">MoodAvatar</h1>
        <p style="color:#64748b;font-size:13px;margin-top:4px">Anmelden</p>
      </div>

      <!-- Card -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:16px;padding:28px">
        <form @submit.prevent="handleLogin" class="flex flex-col gap-4">
          <!-- Error -->
          <div v-if="error" style="background:#ef444422;border:1px solid #ef4444;border-radius:8px;padding:10px 14px;color:#ef4444;font-size:13px">
            {{ error }}
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
            <input v-model="password" type="password" required placeholder="••••••••"
              style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none"
              @focus="($event.target as HTMLElement).style.borderColor='#10b981'"
              @blur="($event.target as HTMLElement).style.borderColor='#334155'"
            />
          </div>

          <button type="submit" :disabled="loading"
            style="background:#10b981;color:#fff;font-weight:600;font-size:14px;padding:11px;border-radius:8px;border:none;cursor:pointer;transition:background 0.2s;margin-top:4px"
            :style="loading ? 'opacity:0.6;cursor:not-allowed' : ''"
            onmouseover="if(!this.disabled)this.style.background='#059669'" onmouseout="this.style.background='#10b981'"
          >
            {{ loading ? 'Anmelden...' : 'Anmelden' }}
          </button>
        </form>

        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:20px">
          <RouterLink to="/forgot-password" style="font-size:13px;color:#64748b">Passwort vergessen?</RouterLink>
          <p style="font-size:13px;color:#64748b;margin:0">
            Noch kein Konto?
            <RouterLink to="/register" style="color:#10b981;font-weight:500">Registrieren</RouterLink>
          </p>
        </div>
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

const email    = ref('')
const password = ref('')
const loading  = ref(false)
const error    = ref('')

async function handleLogin() {
  error.value   = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e.response?.data?.message ?? 'Login fehlgeschlagen'
  } finally {
    loading.value = false
  }
}
</script>
