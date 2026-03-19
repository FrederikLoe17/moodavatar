import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'

export interface FriendMoodUpdate {
  type: 'friend_mood_update'
  userId: string
  username: string
  emotion: string
  intensity: number
  note?: string
}

export interface FriendOnlineStatus {
  type: 'friend_online'
  userId: string
  username: string
  online: boolean
}

export interface RoomVisitorInfo {
  userId: string
  username: string
  emotion?: string | null
  skinColor?: string | null
  clothesColor?: string | null
  hairStyle?: string | null
  hairColor?: string | null
}

export interface RoomStateMessage {
  type: 'room_state'
  visitors: RoomVisitorInfo[]
}

export interface VisitorEntered {
  type: 'visitor_entered'
  userId: string
  username: string
  emotion?: string | null
  skinColor?: string | null
  clothesColor?: string | null
  hairStyle?: string | null
  hairColor?: string | null
}

export interface VisitorLeft {
  type: 'visitor_left'
  userId: string
  username: string
}

export interface RoomReactionReceived {
  type: 'room_reaction_received'
  fromUserId: string
  fromUsername: string
  reaction: string
}

export interface RoomKnocked {
  type: 'room_knocked'
  fromUserId: string
  fromUsername: string
}

type ServerMessage =
  | FriendMoodUpdate
  | FriendOnlineStatus
  | RoomStateMessage
  | VisitorEntered
  | VisitorLeft
  | RoomReactionReceived
  | RoomKnocked
  | { type: string }

export const useRealtimeStore = defineStore('realtime', () => {
  const auth = useAuthStore()

  const connected      = ref(false)
  const onlineUsers    = ref<Set<string>>(new Set())
  const friendMoods    = ref<Map<string, FriendMoodUpdate>>(new Map())

  // Room state — visitors in MY room (I'm the owner)
  const myRoomVisitors = ref<RoomVisitorInfo[]>([])

  // Room state — when I'm visiting someone else's room
  const visitingRoomOwnerId = ref<string | null>(null)
  const visitingRoomState   = ref<RoomVisitorInfo[]>([])

  // Knock notifications for the room owner
  const pendingKnocks = ref<RoomKnocked[]>([])

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let pingInterval: ReturnType<typeof setInterval> | null = null

  const listeners: Array<(msg: ServerMessage) => void> = []

  function onMessage(handler: (msg: ServerMessage) => void) {
    listeners.push(handler)
    return () => {
      const idx = listeners.indexOf(handler)
      if (idx >= 0) listeners.splice(idx, 1)
    }
  }

  function connect() {
    if (ws && ws.readyState === WebSocket.OPEN) return
    if (!auth.accessToken) return

    const apiBase = import.meta.env.VITE_API_BASE_URL
    const wsBase = apiBase
      ? apiBase.replace(/^https/, 'wss').replace(/^http/, 'ws')
      : ''
    const url = `${wsBase}/ws?token=${encodeURIComponent(auth.accessToken)}`
    ws = new WebSocket(url)

    ws.onopen = () => {
      connected.value = true
      pingInterval = setInterval(() => ws?.send(JSON.stringify({ type: 'ping' })), 25_000)
    }

    ws.onmessage = (event) => {
      try {
        const msg: ServerMessage = JSON.parse(event.data)

        switch (msg.type) {
          case 'friend_online': {
            const m = msg as FriendOnlineStatus
            if (m.online) onlineUsers.value.add(m.userId)
            else          onlineUsers.value.delete(m.userId)
            break
          }
          case 'friend_mood_update': {
            const m = msg as FriendMoodUpdate
            friendMoods.value.set(m.userId, m)
            break
          }
          case 'room_state': {
            const m = msg as RoomStateMessage
            visitingRoomState.value = m.visitors
            break
          }
          case 'visitor_entered': {
            const m = msg as VisitorEntered
            const info: RoomVisitorInfo = { userId: m.userId, username: m.username, emotion: m.emotion, skinColor: m.skinColor, clothesColor: m.clothesColor, hairStyle: m.hairStyle, hairColor: m.hairColor }
            if (visitingRoomOwnerId.value !== null) {
              // We're a visitor — update visiting room state
              visitingRoomState.value = [
                ...visitingRoomState.value.filter(v => v.userId !== m.userId),
                info,
              ]
            } else {
              // We're the owner — update our room visitors
              myRoomVisitors.value = [
                ...myRoomVisitors.value.filter(v => v.userId !== m.userId),
                info,
              ]
            }
            break
          }
          case 'visitor_left': {
            const m = msg as VisitorLeft
            if (visitingRoomOwnerId.value !== null) {
              visitingRoomState.value = visitingRoomState.value.filter(v => v.userId !== m.userId)
            } else {
              myRoomVisitors.value = myRoomVisitors.value.filter(v => v.userId !== m.userId)
            }
            break
          }
          case 'room_knocked': {
            const m = msg as RoomKnocked
            pendingKnocks.value = [...pendingKnocks.value, m]
            break
          }
        }

        listeners.forEach(fn => fn(msg))
      } catch { /* ignore parse errors */ }
    }

    ws.onclose = () => {
      connected.value = false
      clearInterval(pingInterval!)
      pingInterval = null
      if (auth.accessToken) {
        reconnectTimer = setTimeout(connect, 3_000)
      }
    }

    ws.onerror = () => ws?.close()
  }

  function disconnect() {
    clearTimeout(reconnectTimer!)
    clearInterval(pingInterval!)
    reconnectTimer = null
    pingInterval   = null
    ws?.close()
    ws = null
    connected.value = false
    onlineUsers.value.clear()
    friendMoods.value.clear()
    myRoomVisitors.value = []
    visitingRoomOwnerId.value = null
    visitingRoomState.value = []
  }

  function sendMoodUpdate(emotion: string, intensity: number, note?: string) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    ws.send(JSON.stringify({
      type: 'mood_update',
      payload: { emotion, intensity, ...(note ? { note } : {}) },
    }))
  }

  function joinRoom(roomOwnerId: string, emotion?: string | null, skinColor?: string | null, clothesColor?: string | null, hairStyle?: string | null, hairColor?: string | null) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    visitingRoomOwnerId.value = roomOwnerId
    visitingRoomState.value = []
    ws.send(JSON.stringify({
      type: 'join_room',
      payload: {
        roomOwnerId,
        ...(emotion      ? { emotion }      : {}),
        ...(skinColor    ? { skinColor }    : {}),
        ...(clothesColor ? { clothesColor } : {}),
        ...(hairStyle    ? { hairStyle }    : {}),
        ...(hairColor    ? { hairColor }    : {}),
      },
    }))
  }

  function leaveRoom() {
    if (!ws || ws.readyState !== WebSocket.OPEN || !visitingRoomOwnerId.value) return
    ws.send(JSON.stringify({
      type: 'leave_room',
      payload: { roomOwnerId: visitingRoomOwnerId.value },
    }))
    visitingRoomOwnerId.value = null
    visitingRoomState.value = []
  }

  function sendReaction(roomOwnerId: string, reaction: string) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    ws.send(JSON.stringify({
      type: 'room_reaction',
      payload: { roomOwnerId, reaction },
    }))
  }

  function knockOnDoor(roomOwnerId: string) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    ws.send(JSON.stringify({
      type: 'room_knock',
      payload: { roomOwnerId },
    }))
  }

  function dismissKnock(fromUserId: string) {
    pendingKnocks.value = pendingKnocks.value.filter(k => k.fromUserId !== fromUserId)
  }

  function isOnline(userId: string) {
    return onlineUsers.value.has(userId)
  }

  return {
    connected, onlineUsers, friendMoods,
    myRoomVisitors, visitingRoomOwnerId, visitingRoomState, pendingKnocks,
    connect, disconnect,
    sendMoodUpdate, joinRoom, leaveRoom, sendReaction, knockOnDoor, dismissKnock,
    isOnline, onMessage,
  }
})
