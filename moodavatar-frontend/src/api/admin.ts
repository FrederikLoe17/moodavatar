import { http } from './http'

const auth = (token: string) => ({ headers: { Authorization: `Bearer ${token}` } })

export interface AdminUser {
  id: string
  email: string
  username: string
  role: 'USER' | 'ADMIN'
  isVerified: boolean
  createdAt: string
}

export interface AdminUsersResponse {
  users: AdminUser[]
  total: number
  page: number
  pageSize: number
}

export interface AdminAuthStats {
  totalUsers: number
  usersToday: number
  usersThisWeek: number
  adminCount: number
  verifiedCount: number
}

export interface AdminUserStats {
  totalProfiles: number
  totalFriendships: number
  pendingRequests: number
}

export interface EmotionCount {
  emotion: string
  count: number
}

export interface RecentMoodEntry {
  userId: string
  emotion: string
  intensity: number
  setAt: string
}

export interface MoodAdminStats {
  emotionDistribution: EmotionCount[]
  totalMoodEntries: number
  activeUsers: number
  averageIntensity: number
  recentMoods: RecentMoodEntry[]
}

export interface ServiceHealth {
  name: string
  ok: boolean
  responseMs: number
}

export interface SystemHealth {
  services: ServiceHealth[]
  timestamp: string
}

export const adminApi = {
  getAuthStats: (token: string) =>
    http.get<AdminAuthStats>('/auth/admin/stats', auth(token)),

  getUserStats: (token: string) =>
    http.get<AdminUserStats>('/users/admin/stats', auth(token)),

  listUsers: (token: string, page = 1, pageSize = 20, search = '') =>
    http.get<AdminUsersResponse>(
      `/auth/admin/users?page=${page}&pageSize=${pageSize}&search=${encodeURIComponent(search)}`,
      auth(token)
    ),

  updateRole: (token: string, userId: string, role: 'USER' | 'ADMIN') =>
    http.put<AdminUser>(`/auth/admin/users/${userId}/role`, { role }, auth(token)),

  getMoodStats: (token: string) =>
    http.get<MoodAdminStats>('/avatars/admin/stats', auth(token)),

  getSystemHealth: () =>
    http.get<SystemHealth>('/system/health'),
}
