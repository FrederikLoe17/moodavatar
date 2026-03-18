<template>
  <RouterView />
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { RouterView } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useRealtimeStore } from './stores/realtime'

const auth     = useAuthStore()
const realtime = useRealtimeStore()

// Connect WebSocket when logged in, disconnect on logout
watch(() => auth.accessToken, (token) => {
  if (token) realtime.connect()
  else       realtime.disconnect()
}, { immediate: true })
</script>
