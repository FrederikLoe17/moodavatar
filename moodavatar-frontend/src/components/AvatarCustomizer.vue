<template>
  <div>
    <!-- Live Preview -->
    <div style="display:flex;justify-content:center;margin-bottom:28px">
      <div>
        <AvatarDisplay :emotion="currentEmotion" :config="previewConfig" :size="180" />
        <div style="text-align:center;margin-top:6px;font-size:11px;color:#64748b">Vorschau</div>
      </div>
    </div>

    <!-- Charakter: Hautfarbe + Kleidung -->
    <SectionBlock label="Charakter">
      <div style="display:flex;gap:20px;flex-wrap:wrap">
        <ColorPicker label="Hautfarbe"   :presets="skinPresets"  v-model="form.skinColor"/>
        <ColorPicker label="Shirt"       :presets="shirtPresets" v-model="form.clothesColor"/>
      </div>
    </SectionBlock>

    <!-- Frisur -->
    <SectionBlock label="Frisur">
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px">
        <button v-for="s in hairStyles" :key="s.value"
          @click="form.hairStyle = s.value"
          :style="`
            padding:8px 4px;border-radius:10px;cursor:pointer;transition:all .15s;
            border:1.5px solid ${form.hairStyle === s.value ? '#10b981' : '#334155'};
            background:${form.hairStyle === s.value ? '#10b98122' : '#0f172a'};
            display:flex;flex-direction:column;align-items:center;gap:4px;
          `"
        >
          <AvatarDisplay :emotion="currentEmotion" :config="previewWith({ hairStyle: s.value })" :size="56"/>
          <span :style="`font-size:10px;font-weight:600;color:${form.hairStyle === s.value ? '#10b981' : '#64748b'}`">
            {{ s.label }}
          </span>
        </button>
      </div>
    </SectionBlock>

    <!-- Haarfarbe -->
    <SectionBlock label="Haarfarbe">
      <ColorPicker :presets="hairPresets" v-model="form.secondaryColor"/>
    </SectionBlock>

    <!-- Accessoires -->
    <SectionBlock label="Accessoires">
      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button v-for="a in accOptions" :key="a.value"
          @click="toggleAcc(a.value)"
          :style="`
            padding:8px 12px;border-radius:10px;cursor:pointer;transition:all .15s;
            border:1.5px solid ${hasAcc(a.value) ? '#10b981' : '#334155'};
            background:${hasAcc(a.value) ? '#10b98122' : '#0f172a'};
            display:flex;flex-direction:column;align-items:center;gap:4px;
          `"
        >
          <AvatarDisplay
            :emotion="currentEmotion"
            :config="previewWith({ accessories: hasAcc(a.value) ? form.accessories : [...form.accessories, a.value] })"
            :size="56"
          />
          <span :style="`font-size:10px;font-weight:600;color:${hasAcc(a.value) ? '#10b981' : '#64748b'}`">
            {{ a.label }}
          </span>
        </button>
      </div>
    </SectionBlock>

    <!-- Raum -->
    <SectionBlock label="Raum">
      <div style="display:flex;gap:20px;flex-wrap:wrap;margin-bottom:14px">
        <ColorPicker label="Wandfarbe"   :presets="wallPresets"  v-model="form.roomWallColor"/>
        <ColorPicker label="Bodenfarbe"  :presets="floorPresets" v-model="form.roomFloorColor"/>
      </div>
      <div style="font-size:11px;font-weight:700;color:#64748b;text-transform:uppercase;letter-spacing:.06em;margin-bottom:8px">
        Einrichtung
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <button v-for="item in roomItemOptions" :key="item.value"
          @click="toggleRoomItem(item.value)"
          :style="`
            padding:6px 14px;border-radius:8px;cursor:pointer;transition:all .15s;font-size:12px;font-weight:600;
            border:1.5px solid ${hasRoom(item.value) ? '#10b981' : '#334155'};
            background:${hasRoom(item.value) ? '#10b98122' : '#0f172a'};
            color:${hasRoom(item.value) ? '#10b981' : '#64748b'};
          `"
        >{{ item.icon }} {{ item.label }}</button>
      </div>
    </SectionBlock>

    <!-- Save -->
    <div v-if="success" style="margin-bottom:10px;background:#10b98122;border:1px solid #10b981;border-radius:8px;padding:10px 14px;color:#10b981;font-size:13px">
      Avatar gespeichert ✓
    </div>
    <div v-if="error" style="margin-bottom:10px;background:#ef444422;border:1px solid #ef4444;border-radius:8px;padding:10px 14px;color:#ef4444;font-size:13px">
      {{ error }}
    </div>
    <button @click="save" :disabled="loading"
      style="width:100%;padding:11px;border-radius:9px;border:none;cursor:pointer;font-weight:600;font-size:14px;background:#10b981;color:white;transition:opacity .15s"
      :style="{ opacity: loading ? 0.6 : 1 }"
    >{{ loading ? 'Speichern...' : 'Avatar speichern' }}</button>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, defineComponent, h } from 'vue'
import AvatarDisplay from './AvatarDisplay.vue'
import { avatarApi, type Emotion, type AvatarConfig } from '../api/avatar'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{
  currentEmotion?: Emotion | null
  initialConfig?:  AvatarConfig | null
}>()
const emit = defineEmits<{ saved: [config: AvatarConfig] }>()

const auth    = useAuthStore()
const loading = ref(false)
const success = ref(false)
const error   = ref('')

const form = reactive({
  skinColor:      props.initialConfig?.skinColor      ?? '#f0c98b',
  clothesColor:   props.initialConfig?.clothesColor   ?? '#3b82f6',
  secondaryColor: props.initialConfig?.secondaryColor ?? '#94a3b8',
  hairStyle:      props.initialConfig?.hairStyle      ?? 'short',
  accessories:    [...(props.initialConfig?.accessories  ?? [])],
  roomWallColor:  props.initialConfig?.roomWallColor  ?? '#1e293b',
  roomFloorColor: props.initialConfig?.roomFloorColor ?? '#0f172a',
  roomItems:      [...(props.initialConfig?.roomItems     ?? [])],
})

// The live preview config (merges form state with existing mood-driven fields)
const previewConfig = computed<AvatarConfig>(() => ({
  primaryColor:   props.initialConfig?.primaryColor ?? '#64748b',
  expression:     props.initialConfig?.expression   ?? 'neutral',
  aura:           props.initialConfig?.aura         ?? 'gray',
  ...form,
}))

// Helper: preview config with one field overridden (for grid buttons)
function previewWith(overrides: Partial<AvatarConfig>): AvatarConfig {
  return { ...previewConfig.value, ...overrides }
}

// ── Presets ───────────────────────────────────────────────────────────────────
const skinPresets  = ['#f0c98b','#e8a882','#c87941','#8d5524','#f1c27d','#ffdbac']
const shirtPresets = ['#3b82f6','#10b981','#ef4444','#f59e0b','#8b5cf6','#ec4899','#64748b','#0f172a']
const hairPresets  = ['#1a1a1a','#7b4f2e','#e8c84a','#c0392b','#94a3b8','#f8fafc','#3b82f6','#ec4899']
const wallPresets  = ['#1e293b','#0f172a','#312e81','#14532d','#7c2d12','#1c1917','#1e3a5f','#374151']
const floorPresets = ['#0f172a','#1e293b','#292524','#1c1917','#134e4a','#1e1b4b','#44403c','#374151']

const hairStyles = [
  { value: 'none',   label: 'Kahl'   },
  { value: 'short',  label: 'Kurz'   },
  { value: 'medium', label: 'Mittel' },
  { value: 'long',   label: 'Lang'   },
  { value: 'curly',  label: 'Locken' },
  { value: 'spiky',  label: 'Spiky'  },
]
const accOptions = [
  { value: 'glasses',    label: 'Brille'        },
  { value: 'sunglasses', label: 'Sonnenbrille'  },
  { value: 'hat',        label: 'Mütze'         },
]
const roomItemOptions = [
  { value: 'plant',      icon: '🌿', label: 'Pflanze'    },
  { value: 'bookshelf',  icon: '📚', label: 'Regal'      },
  { value: 'lamp',       icon: '🪔', label: 'Lampe'      },
  { value: 'rug',        icon: '🟫', label: 'Teppich'    },
]

// ── Helpers ───────────────────────────────────────────────────────────────────
const hasAcc  = (n: string) => form.accessories.includes(n)
const hasRoom = (n: string) => form.roomItems.includes(n)

function toggleAcc(name: string) {
  if (name === 'glasses'    && hasAcc('sunglasses')) form.accessories = form.accessories.filter(a => a !== 'sunglasses')
  if (name === 'sunglasses' && hasAcc('glasses'))    form.accessories = form.accessories.filter(a => a !== 'glasses')
  form.accessories = hasAcc(name) ? form.accessories.filter(a => a !== name) : [...form.accessories, name]
}
function toggleRoomItem(name: string) {
  form.roomItems = hasRoom(name) ? form.roomItems.filter(r => r !== name) : [...form.roomItems, name]
}

// ── Save ──────────────────────────────────────────────────────────────────────
async function save() {
  loading.value = true; error.value = ''; success.value = false
  try {
    const { data } = await avatarApi.updateConfig(auth.accessToken, { ...form })
    success.value = true
    emit('saved', data.config)
    setTimeout(() => success.value = false, 3000)
  } catch {
    error.value = 'Fehler beim Speichern.'
  } finally {
    loading.value = false
  }
}
</script>

<script lang="ts">
import { defineComponent, h } from 'vue'

// Section wrapper
const SectionBlock = defineComponent({
  props: { label: String },
  setup: (p, { slots }) => () => h('div', { style: 'margin-bottom:24px' }, [
    h('div', { style: 'font-size:11px;font-weight:700;color:#64748b;text-transform:uppercase;letter-spacing:.06em;margin-bottom:10px' }, p.label),
    slots.default?.(),
  ])
})

// Reusable color picker with presets
const ColorPicker = defineComponent({
  props: { label: String, presets: Array as () => string[], modelValue: String },
  emits: ['update:modelValue'],
  setup: (props, { emit }) => () => h('div', {}, [
    props.label && h('div', { style: 'font-size:11px;color:#94a3b8;margin-bottom:6px' }, props.label),
    h('div', { style: 'display:flex;gap:6px;flex-wrap:wrap;align-items:center' }, [
      ...(props.presets ?? []).map(c =>
        h('button', {
          onClick: () => emit('update:modelValue', c),
          style: `width:24px;height:24px;border-radius:50%;background:${c};cursor:pointer;border:2.5px solid ${props.modelValue === c ? 'white' : 'transparent'};box-shadow:${props.modelValue === c ? '0 0 0 1px #64748b' : 'none'};transition:all .15s;`,
        })
      ),
      h('label', { style: 'display:flex;align-items:center;gap:4px;cursor:pointer' }, [
        h('input', {
          type: 'color', value: props.modelValue,
          onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
          style: 'width:24px;height:24px;border-radius:50%;border:none;cursor:pointer;padding:0;background:none;',
        }),
        h('span', { style: 'font-size:10px;color:#64748b' }, 'Eigene'),
      ]),
    ]),
  ])
})

export { SectionBlock, ColorPicker }
</script>
