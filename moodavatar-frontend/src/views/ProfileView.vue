<template>
  <AppLayout>
    <div class="max-w-lg">
      <h2 style="font-size:18px;font-weight:700;color:#f8fafc;margin-bottom:6px">Profil & Avatar</h2>
      <p style="font-size:13px;color:#64748b;margin-bottom:24px">Passe dein Profil und deinen Avatar an</p>

      <!-- Avatar Customizer Card -->
      <div style="background:#1e293b;border-radius:16px;padding:24px;border:1px solid #334155;margin-bottom:16px">
        <div style="font-size:13px;font-weight:700;color:#94a3b8;margin-bottom:20px;text-transform:uppercase;letter-spacing:.05em">
          Avatar anpassen
        </div>
        <AvatarCustomizer
          v-if="!avatarLoading"
          :current-emotion="avatar?.emotion ?? null"
          :initial-config="avatar?.config ?? null"
          @saved="onConfigSaved"
        />
        <div v-else style="display:flex;justify-content:center;padding:40px 0">
          <div style="color:#64748b;font-size:13px">Lade Avatar...</div>
        </div>
      </div>

      <!-- Profile Info Card -->
      <div style="background:#1e293b;border-radius:16px;padding:24px;border:1px solid #334155">
        <div style="font-size:13px;font-weight:700;color:#94a3b8;margin-bottom:20px;text-transform:uppercase;letter-spacing:.05em">
          Profildaten
        </div>

        <form @submit.prevent="handleSave" class="flex flex-col gap-4">
          <div v-if="profileSuccess" style="background:#10b98122;border:1px solid #10b981;border-radius:8px;padding:10px 14px;color:#10b981;font-size:13px">
            Profil gespeichert ✓
          </div>
          <div v-if="profileError" style="background:#ef444422;border:1px solid #ef4444;border-radius:8px;padding:10px 14px;color:#ef4444;font-size:13px">
            {{ profileError }}
          </div>

          <Field label="Benutzername" :model-value="auth.user?.username ?? ''" :disabled="true" />
          <Field label="E-Mail"       :model-value="auth.user?.email    ?? ''" :disabled="true" />
          <Field label="Anzeigename"  v-model="form.displayName" placeholder="Dein Name" />
          <Field label="Bio"          v-model="form.bio"         placeholder="Kurze Beschreibung über dich" :textarea="true" />

          <button type="submit" :disabled="profileLoading"
            style="background:#10b981;color:#fff;font-weight:600;font-size:14px;padding:10px;border-radius:8px;border:none;cursor:pointer;margin-top:4px"
          >{{ profileLoading ? 'Speichern...' : 'Speichern' }}</button>
        </form>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, defineComponent, h } from 'vue'
import AppLayout       from '../components/AppLayout.vue'
import AvatarCustomizer from '../components/AvatarCustomizer.vue'
import { useAuthStore } from '../stores/auth'
import { userApi }      from '../api/user'
import { avatarApi, type Avatar, type AvatarConfig } from '../api/avatar'

// ── Reusable field component ──────────────────────────────────────────────────
const Field = defineComponent({
  props: { label: String, modelValue: String, placeholder: String, type: String, textarea: Boolean, disabled: Boolean },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const base = 'background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:14px;outline:none;width:100%;box-sizing:border-box'
    const disabled = 'opacity:.5;cursor:not-allowed'
    return () => h('div', { style: 'display:flex;flex-direction:column;gap:6px' }, [
      h('label', { style: 'font-size:12px;color:#94a3b8;font-weight:500' }, props.label),
      props.textarea
        ? h('textarea', {
            style: base + ';resize:vertical;min-height:80px' + (props.disabled ? ';' + disabled : ''),
            value: props.modelValue, placeholder: props.placeholder, disabled: props.disabled,
            onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
          })
        : h('input', {
            style: base + (props.disabled ? ';' + disabled : ''),
            type: props.type ?? 'text', value: props.modelValue, placeholder: props.placeholder, disabled: props.disabled,
            onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value)
          })
    ])
  }
})

// ── State ─────────────────────────────────────────────────────────────────────
const auth         = useAuthStore()
const avatar       = ref<Avatar | null>(null)
const avatarLoading = ref(true)
const profileLoading = ref(false)
const profileSuccess = ref(false)
const profileError   = ref('')
const form = ref({ displayName: '', bio: '' })

// ── Lifecycle ─────────────────────────────────────────────────────────────────
onMounted(async () => {
  try {
    const [profileRes, avatarRes] = await Promise.allSettled([
      userApi.getMe(auth.accessToken),
      avatarApi.getMyAvatar(auth.accessToken),
    ])
    if (profileRes.status === 'fulfilled') {
      form.value.displayName = profileRes.value.data.displayName ?? ''
      form.value.bio         = profileRes.value.data.bio         ?? ''
    }
    if (avatarRes.status === 'fulfilled') {
      avatar.value = avatarRes.value.data
    }
  } finally {
    avatarLoading.value = false
  }
})

// ── Handlers ──────────────────────────────────────────────────────────────────
function onConfigSaved(config: AvatarConfig) {
  if (avatar.value) avatar.value.config = config
}

async function handleSave() {
  profileError.value   = ''
  profileSuccess.value = false
  profileLoading.value = true
  try {
    await userApi.updateMe(auth.accessToken, {
      displayName: form.value.displayName || undefined,
      bio:         form.value.bio         || undefined,
    })
    profileSuccess.value = true
    setTimeout(() => profileSuccess.value = false, 3000)
  } catch {
    profileError.value = 'Fehler beim Speichern'
  } finally {
    profileLoading.value = false
  }
}
</script>
