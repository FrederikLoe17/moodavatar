import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', rewrite: (p) => p.replace(/^\/api/, '') },
      '/ws':  { target: 'ws://localhost:8084', ws: true },
    },
  },
})
