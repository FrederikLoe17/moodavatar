import { http } from './http'

const auth = (token: string) => ({ headers: { Authorization: `Bearer ${token}` } })

export interface Needs {
  mood:     number  // 0-100
  energy:   number
  social:   number
  activity: number
}

export type Emotion = 'HAPPY' | 'SAD' | 'ANGRY' | 'NEUTRAL' | 'EXCITED' | 'TIRED' | 'ANXIOUS' | 'CONTENT'

export interface AvatarConfig {
  // Mood-driven
  primaryColor:   string
  expression:     string
  aura:           string
  // Character customization
  secondaryColor: string   // hair color
  hairStyle:      string   // 'none'|'short'|'medium'|'long'|'curly'|'spiky'
  accessories:    string[] // ['glasses','sunglasses','hat']
  skinColor:      string   // face & body skin color
  clothesColor:   string   // shirt/top color
  // Room customization
  roomWallColor:  string
  roomFloorColor: string
  roomItems:      string[] // ['plant','bookshelf','lamp','rug']
}

// Backend response shape
interface BackendAvatar {
  userId:      string
  currentMood: { emotion: string; intensity: number; note: string | null; setAt: string } | null
  config:      AvatarConfig
  updatedAt:   string
}

// Frontend-friendly shape (flattened)
export interface Avatar {
  userId:    string
  emotion:   Emotion
  intensity: number
  note:      string | null
  config:    AvatarConfig
  updatedAt: string
}

export interface CalendarDay {
  date: string           // YYYY-MM-DD
  count: number
  avgIntensity: number
  dominantEmotion: string | null
}

export interface EmotionDistribution {
  emotion: string
  count: number
  percentage: number
}

export interface WeekdayStats {
  day: string            // MON, TUE, …
  avgIntensity: number
  count: number
}

export interface Insights {
  totalEntries: number
  currentStreak: number
  longestStreak: number
  mostCommonEmotion: string | null
  avgIntensity: number
  emotionDistribution: EmotionDistribution[]
  weekdayPattern: WeekdayStats[]
}

export interface MoodEntry {
  emotion:   Emotion
  intensity: number
  note:      string | null
  timestamp: string
  config:    AvatarConfig
}

function flattenAvatar(data: BackendAvatar): Avatar {
  return {
    userId:    data.userId,
    emotion:   (data.currentMood?.emotion as Emotion) ?? 'NEUTRAL',
    intensity: data.currentMood?.intensity ?? 5,
    note:      data.currentMood?.note ?? null,
    config: {
      primaryColor:   data.config?.primaryColor   ?? '#64748b',
      expression:     data.config?.expression     ?? 'neutral',
      aura:           data.config?.aura           ?? 'gray',
      secondaryColor: data.config?.secondaryColor ?? '#94a3b8',
      hairStyle:      data.config?.hairStyle      ?? 'short',
      accessories:    data.config?.accessories    ?? [],
      skinColor:      data.config?.skinColor      ?? '#f0c98b',
      clothesColor:   data.config?.clothesColor   ?? '#3b82f6',
      roomWallColor:  data.config?.roomWallColor  ?? '#1e293b',
      roomFloorColor: data.config?.roomFloorColor ?? '#0f172a',
      roomItems:      data.config?.roomItems      ?? [],
    },
    updatedAt: data.updatedAt,
  }
}

export const avatarApi = {
  getMyAvatar: async (token: string) => {
    const res = await http.get<BackendAvatar>('/avatars/me', auth(token))
    return { data: flattenAvatar(res.data) }
  },

  setMood: async (token: string, emotion: Emotion, intensity: number, note?: string) => {
    const res = await http.put<BackendAvatar>('/avatars/me/mood', { emotion, intensity, note }, auth(token))
    return { data: flattenAvatar(res.data) }
  },

  updateConfig: async (token: string, config: Partial<Omit<AvatarConfig, 'primaryColor' | 'expression' | 'aura'>>) => {
    const res = await http.put<BackendAvatar>('/avatars/me/config', config, auth(token))
    return { data: flattenAvatar(res.data) }
  },

  getMoodHistory: (token: string, limit = 20) =>
    http.get<MoodEntry[]>(`/avatars/me/history?limit=${limit}`, auth(token)),

  getNeeds: (token: string) =>
    http.get<Needs>('/avatars/me/needs', auth(token)),

  getUserAvatar: async (token: string, userId: string) => {
    const res = await http.get<BackendAvatar>(`/avatars/${userId}`, auth(token))
    return { data: flattenAvatar(res.data) }
  },

  getCalendarData: (token: string, days = 90) =>
    http.get<CalendarDay[]>(`/avatars/me/history/calendar?days=${days}`, auth(token)),

  getInsights: (token: string) =>
    http.get<Insights>('/avatars/me/insights', auth(token)),

  getPublicAvatar: async (userId: string) => {
    const res = await http.get<BackendAvatar>(`/avatars/public/${userId}`)
    return { data: flattenAvatar(res.data) }
  },
}
