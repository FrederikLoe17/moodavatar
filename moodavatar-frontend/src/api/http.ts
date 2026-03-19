import axios from 'axios'

// In dev: Vite proxies /api/* → backend (prefix stripped)
// In prod: VITE_API_BASE_URL points directly to the backend (e.g. https://your-app.koyeb.app)
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

// ngrok free tier shows an interstitial page — this header bypasses it
const extraHeaders = import.meta.env.VITE_API_BASE_URL
  ? { 'ngrok-skip-browser-warning': '1' }
  : {}

export const http = axios.create({ baseURL, headers: extraHeaders })

// Separate instance for the refresh call itself — no interceptor, avoids infinite loop
const plain = axios.create({ baseURL, headers: extraHeaders })

// Single in-flight refresh promise — prevents multiple simultaneous refresh calls
let refreshing: Promise<string> | null = null

http.interceptors.response.use(
  res => res,
  async (error) => {
    const original = error.config

    // Only handle 401s, skip auth endpoints and already-retried requests
    if (
      error.response?.status !== 401 ||
      original._retry ||
      original.url?.startsWith('/auth/')
    ) {
      return Promise.reject(error)
    }

    original._retry = true

    // Lazy import avoids circular dependency (auth store imports authApi)
    const { useAuthStore } = await import('../stores/auth')
    const auth = useAuthStore()

    if (!auth.refreshToken) {
      auth.logout()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (!refreshing) {
      const rt = auth.refreshToken
      refreshing = plain
        .post<{ accessToken: string }>('/auth/refresh', { refreshToken: rt })
        .then(res => {
          auth.setAccessToken(res.data.accessToken)
          return res.data.accessToken
        })
        .catch(async e => {
          await auth.logout()
          window.location.href = '/login'
          throw e
        })
        .finally(() => { refreshing = null })
    }

    const newToken = await refreshing
    original.headers['Authorization'] = `Bearer ${newToken}`
    return http(original)
  }
)
