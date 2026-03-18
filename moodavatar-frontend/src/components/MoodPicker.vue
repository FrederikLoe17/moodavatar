<template>
  <div>
    <div style="font-size:13px;font-weight:700;color:#94a3b8;margin-bottom:14px;text-transform:uppercase;letter-spacing:.05em">
      Wie geht es dir?
    </div>

    <!-- Emotion grid — mini avatars instead of emojis -->
    <div :style="`display:grid;grid-template-columns:repeat(4,1fr);gap:${compact ? 5 : 8}px;margin-bottom:20px`">
      <button
        v-for="e in emotions" :key="e.value"
        @click="selected = e.value"
        :style="`
          border-radius:10px;padding:${compact ? '6px 3px' : '10px 6px'};border:1.5px solid;cursor:pointer;
          transition:all .15s;display:flex;flex-direction:column;align-items:center;gap:4px;
          ${selected === e.value
            ? `background:${e.color}22;border-color:${e.color}`
            : 'background:#0f172a;border-color:#334155'}
        `"
      >
        <AvatarDisplay
          :emotion="e.value"
          :config="avatarConfig ?? null"
          :size="compact ? 42 : 60"
        />
        <span :style="`font-size:${compact ? 9 : 10}px;font-weight:600;letter-spacing:.03em;color:${selected === e.value ? e.color : '#64748b'}`">
          {{ e.label }}
        </span>
      </button>
    </div>

    <!-- Intensity slider -->
    <div style="margin-bottom:18px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
        <span style="font-size:12px;color:#94a3b8;font-weight:500">Intensität</span>
        <span style="font-size:13px;font-weight:700;color:#f1f5f9">{{ intensity }} / 10</span>
      </div>
      <input
        type="range" min="1" max="10" v-model.number="intensity"
        :style="`
          width:100%;height:5px;border-radius:99px;outline:none;cursor:pointer;
          appearance:none;-webkit-appearance:none;
          background:linear-gradient(to right, ${selectedColor} ${(intensity-1)/9*100}%, #334155 ${(intensity-1)/9*100}%);
        `"
      />
    </div>

    <!-- Note -->
    <div style="margin-bottom:18px">
      <textarea
        v-model="note"
        placeholder="Notiz (optional)..."
        rows="2"
        style="width:100%;background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 12px;color:#e2e8f0;font-size:13px;resize:none;outline:none;font-family:inherit;box-sizing:border-box"
        @focus="(e:any) => e.target.style.borderColor = selectedColor"
        @blur="(e:any) => e.target.style.borderColor = '#334155'"
      />
    </div>

    <!-- Submit -->
    <button
      @click="submit"
      :disabled="loading"
      :style="`
        width:100%;padding:11px;border-radius:9px;border:none;cursor:pointer;
        font-weight:600;font-size:14px;transition:opacity .15s;
        background:${selectedColor};color:white;
        opacity:${loading ? 0.6 : 1};
      `"
    >
      {{ loading ? 'Speichern...' : 'Stimmung setzen' }}
    </button>

    <div v-if="error" style="margin-top:10px;font-size:12px;color:#ef4444">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AvatarDisplay from './AvatarDisplay.vue'
import { avatarApi, type Emotion, type Avatar, type AvatarConfig } from '../api/avatar'
import { useAuthStore } from '../stores/auth'
import { useRealtimeStore } from '../stores/realtime'

const props = defineProps<{ avatarConfig?: AvatarConfig | null; compact?: boolean }>()
const emit  = defineEmits<{ updated: [avatar: Avatar] }>()

const auth      = useAuthStore()
const realtime  = useRealtimeStore()
const selected  = ref<Emotion>('NEUTRAL')
const intensity = ref(5)
const note      = ref('')
const loading   = ref(false)
const error     = ref('')

const emotions: { value: Emotion; label: string; color: string }[] = [
  { value: 'HAPPY',   label: 'Glücklich',  color: '#10b981' },
  { value: 'SAD',     label: 'Traurig',    color: '#3b82f6' },
  { value: 'EXCITED', label: 'Aufgeregt',  color: '#f59e0b' },
  { value: 'ANGRY',   label: 'Wütend',     color: '#ef4444' },
  { value: 'ANXIOUS', label: 'Ängstlich',  color: '#f97316' },
  { value: 'TIRED',   label: 'Müde',       color: '#8b5cf6' },
  { value: 'CONTENT', label: 'Zufrieden',  color: '#06b6d4' },
  { value: 'NEUTRAL', label: 'Neutral',    color: '#64748b' },
]

const selectedColor = computed(
  () => emotions.find(e => e.value === selected.value)?.color ?? '#64748b'
)

async function submit() {
  loading.value = true
  error.value   = ''
  try {
    const { data } = await avatarApi.setMood(
      auth.accessToken, selected.value, intensity.value, note.value || undefined
    )
    emit('updated', data)
    realtime.sendMoodUpdate(selected.value, intensity.value, note.value || undefined)
    note.value = ''
  } catch {
    error.value = 'Fehler beim Speichern der Stimmung.'
  } finally {
    loading.value = false
  }
}
</script>
