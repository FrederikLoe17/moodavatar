# MoodAvatar

> Customizable SVG avatars that change with your mood — living in a real-time room you can share with friends.

MoodAvatar is a full-stack application where users create and personalize animated avatars that reflect their current mood. Avatars live in a 3D room, move autonomously, and have needs that decay over time (Tamagotchi-style). Friends can visit each other's rooms in real time, react with emojis, and stay connected through live mood updates.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Overview](#api-overview)
- [Development Workflow](#development-workflow)
- [Planned Features](#planned-features)

---

## Features

### Current
- **Mood Tracking** — Set your mood from 8 emotions with intensity; history chart shows trends over time
- **Mood Journaling & Insights** — GitHub-style calendar heatmap, average emotion per weekday, best/worst day stats
- **Living Avatar** — Full-body SVG character with mood-based poses, expressions, and movement in a 3D perspective room
- **Avatar Customization** — Skin color, hair style & color, shirt color, accessories, room colors and items
- **Avatar Needs** — Four stats (Mood, Energy, Social, Activity) that decay over time; avatar behavior adapts to low needs
- **Real-Time Presence** — WebSocket-based live mood updates and online status for friends
- **Room Visits** — Visit friends' rooms, see their avatar live, send emoji reactions, knock on the door
- **Friends System** — Send/accept friend requests, view friends' public profiles
- **Notifications** — In-app notification bell with unread badge; room visit and friend request notifications
- **Public Profiles** — Shareable profile pages at `/u/:username`
- **Email Flows** — Email verification, password reset via Mailhog (dev) / SMTP (prod)
- **Admin Panel** — User management, mood statistics, service health dashboard

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        Frontend                         │
│              Vue 3 + TypeScript + Tailwind              │
│                      Port 5173                          │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP (REST) + WebSocket (/ws)
                        ▼
┌───────────────────────────────────────────────────────────┐
│                    Backend Monolith                       │  Port 8080
│                  Ktor (Kotlin) + JWT                      │
│    auth · users · avatars · notifications · realtime      │
└──────────────┬────────────────────────┬───────────────────┘
               │                        │
               ▼                        ▼
   ┌───────────────────┐    ┌───────────────────┐
   │   PostgreSQL 16   │    │    MongoDB 7      │
   │  auth · users ·   │    │  avatars & mood   │
   │   notifications   │    │     history       │
   └───────────────────┘    └───────────────────┘
```

**WebSocket:** Connect to `ws://localhost:8080/ws?token=<JWT>`
Messages: `ping`, `mood_update`, `join_room`, `leave_room`, `room_reaction`, `room_knock`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, TypeScript, Tailwind CSS v4, Vite, Pinia, Axios |
| Backend | Ktor (Kotlin), Gradle (multi-stage Docker builds) |
| Databases | PostgreSQL 16, MongoDB 7 |
| Auth | JWT (access 15 min / refresh 30 days), bcrypt passwords |
| Email | Jakarta Mail → Mailhog (dev) / SMTP (prod) |
| CI/CD | GitHub Actions — ktlint + tests (backend), vue-tsc + build (frontend) |
| Code Quality | ktlint 1.3.1 (Kotlin standard rules, max line length 120) |

---

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Node.js 20+ (frontend dev only)
- Git

### 1. Clone & configure
```bash
git clone https://github.com/FrederikLoe17/moodavatar.git
cd moodavatar
cp .env.example .env
# Edit .env and set a strong JWT_SECRET and POSTGRES_PASSWORD
```

### 2. Start backend services
```bash
docker compose up -d
```

### 3. Start the frontend
```bash
cd moodavatar-frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173)

### 4. Create your first user
Register at `/register` — email verification is sent to **Mailhog** at [http://localhost:8025](http://localhost:8025).

### After code changes (backend)
```bash
docker compose build backend && docker compose up -d --force-recreate backend
```

> `docker compose up -d` alone does **not** rebuild — always run `build` first.

---

## API Overview

All endpoints are served by the monolith at `http://localhost:8080`.

| Endpoint | Purpose |
|---|---|
| `POST /auth/register` | Register new user |
| `POST /auth/login` | Returns access + refresh tokens |
| `POST /auth/refresh` | Rotate refresh token |
| `GET /users/profile/me` | Own profile |
| `GET /users/profile/:username` | Public profile |
| `GET /friends` | Friend list |
| `POST /friends/request` | Send friend request |
| `GET /avatars/me` | Own avatar config |
| `POST /avatars/me/mood` | Set current mood |
| `GET /avatars/me/needs` | Current needs (0–100) |
| `GET /notifications` | All notifications |
| `GET /notifications/unread-count` | Unread badge count |

### JWT
All requests (except `/auth/register`, `/auth/login`) require `Authorization: Bearer <token>`.
JWT uses a custom `userId` claim (not standard `sub`).

---

## Development Workflow

### Branches
```
main      ← protected, production-ready, merge via PR only
develop   ← integration branch, base for all feature branches
feature/* ← new features
fix/*     ← bug fixes
```

### CI checks (run on every PR)
- **Backend**: `./gradlew ktlintCheck` + `./gradlew test` (in `moodavatar-backend/`)
- **Frontend**: `vue-tsc --noEmit` + `npm run build`

### Local quality checks
```bash
# Backend
cd moodavatar-backend
./gradlew ktlintCheck   # lint
./gradlew ktlintFormat  # auto-fix formatting
./gradlew test          # unit + integration tests (Docker must be running)

# Frontend
cd moodavatar-frontend
npx vue-tsc --noEmit    # type-check
```

### Commit convention
```
feat(scope): short description
fix(scope): short description
refactor / test / chore / docs
```

---

## Planned Features

### Phase 9 — Advanced Social Features
Deepen the social layer beyond simple friend connections.
- **Mood Matching** — get matched with friends who share your current emotion
- **Group Rooms** — shared spaces where multiple users can hang out simultaneously
- **Mood Streaks** — track consecutive days of logging moods with friends

### Phase 10 — Gamification & Achievements
Reward engagement and self-expression.
- Achievement system (e.g. "Logged 7 days in a row", "Visited 10 friend rooms")
- XP and level progression
- Rare avatar items unlocked through achievements
- Seasonal avatar accessories

---

*Built with Ktor · Vue 3 · Docker · GitHub Actions*
