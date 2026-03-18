import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type AuthResponse } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken  = ref(localStorage.getItem('accessToken') ?? '')
  const refreshToken = ref(localStorage.getItem('refreshToken') ?? '')
  const user = ref<AuthResponse['user'] | null>(
    JSON.parse(localStorage.getItem('user') ?? 'null')
  )

  const isLoggedIn = computed(() => !!accessToken.value)

  async function login(email: string, password: string) {
    const { data } = await authApi.login(email, password)
    accessToken.value  = data.accessToken
    refreshToken.value = data.refreshToken
    user.value         = data.user
    localStorage.setItem('accessToken',  data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user',         JSON.stringify(data.user))
  }

  async function register(email: string, username: string, password: string) {
    await authApi.register(email, username, password)
    await login(email, password)
  }

  function setAccessToken(token: string) {
    accessToken.value = token
    localStorage.setItem('accessToken', token)
  }

  function markVerified() {
    if (user.value) {
      user.value = { ...user.value, isVerified: true }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  async function logout() {
    if (accessToken.value) {
      await authApi.logout(refreshToken.value, accessToken.value).catch(() => {})
    }
    accessToken.value  = ''
    refreshToken.value = ''
    user.value         = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  return { accessToken, refreshToken, user, isLoggedIn, login, register, logout, setAccessToken, markVerified }
})
