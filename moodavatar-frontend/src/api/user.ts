import { http } from './http'

const auth = (token: string) => ({ headers: { Authorization: `Bearer ${token}` } })

export interface Profile {
  id: string; username: string; displayName: string | null
  bio: string | null; avatarUrl: string | null
}

export interface FriendRequest {
  id: string; senderId: string; receiverId: string; status: string; senderUsername?: string
}

export const userApi = {
  createProfile: (id: string, username: string, token: string) =>
    http.post<Profile>('/users/internal/profile', { id, username }, auth(token)),

  getMe: (token: string) =>
    http.get<Profile>('/users/me', auth(token)),

  updateMe: (token: string, data: Partial<Pick<Profile, 'displayName' | 'bio' | 'avatarUrl'>>) =>
    http.put<Profile>('/users/me', data, auth(token)),

  search: (token: string, q: string) =>
    http.get<Profile[]>(`/users/search?q=${encodeURIComponent(q)}`, auth(token)),

  getFriends: (token: string) =>
    http.get<Profile[]>('/friends', auth(token)),

  getRequests: (token: string) =>
    http.get<FriendRequest[]>('/friends/requests', auth(token)),

  sendRequest: (token: string, receiverId: string) =>
    http.post<FriendRequest>(`/friends/requests/${receiverId}`, {}, auth(token)),

  respondRequest: (token: string, requestId: string, action: 'ACCEPT' | 'DECLINE') =>
    http.patch<FriendRequest>(`/friends/requests/${requestId}`, { action }, auth(token)),

  removeFriend: (token: string, friendId: string) =>
    http.delete(`/friends/${friendId}`, auth(token)),

  getPublicProfile: (username: string) =>
    http.get<Profile>(`/users/public/${encodeURIComponent(username)}`),
}
