import axios from 'axios'

export interface Notification {
  id: string
  type: 'FRIEND_REQUEST' | 'FRIEND_ACCEPTED' | string
  fromUserId: string
  fromUsername: string
  read: boolean
  metadata: string | null
  createdAt: string
}

const base = axios.create({ baseURL: '/api/notifications' })

const auth = (token: string) => ({ headers: { Authorization: `Bearer ${token}` } })

export const notificationApi = {
  list:       (token: string) =>
    base.get<Notification[]>('', auth(token)),

  unreadCount: (token: string) =>
    base.get<{ count: number }>('/unread-count', auth(token)),

  markRead:   (token: string, id: string) =>
    base.post(`/${id}/read`, {}, auth(token)),

  markAllRead: (token: string) =>
    base.post('/read-all', {}, auth(token)),
}
