import { http } from './http'

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: { id: string; email: string; username: string; role: string; isVerified: boolean }
}

export interface UserResponse {
  id: string; email: string; username: string; role: string; isVerified: boolean
}

export const authApi = {
  register: (email: string, username: string, password: string) =>
    http.post<UserResponse>('/auth/register', { email, username, password }),

  login: (email: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { email, password }),

  me: (token: string) =>
    http.get<UserResponse>('/auth/me', { headers: { Authorization: `Bearer ${token}` } }),

  logout: (refreshToken: string, token: string) =>
    http.post('/auth/logout', { refreshToken }, { headers: { Authorization: `Bearer ${token}` } }),

  forgotPassword: (email: string) =>
    http.post<{ message: string }>('/auth/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    http.post<{ message: string }>('/auth/reset-password', { token, newPassword }),

  verifyEmail: (token: string) =>
    http.post<{ message: string }>('/auth/verify-email', { token }),

  resendVerification: (email: string) =>
    http.post<{ message: string }>('/auth/resend-verification', { email }),
}
