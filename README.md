# MoodAvatar

> Customizable SVG avatars that change with your mood — living in a real-time room you can share with friends.

MoodAvatar is a full-stack microservices application where users create and personalize animated avatars that reflect their current mood. Avatars live in a 3D room, move autonomously, and have needs that decay over time (Tamagotchi-style). Friends can visit each other's rooms in real time, react with emojis, and stay connected through live mood updates.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Services](#services)
- [API Overview](#api-overview)
- [Development Workflow](#development-workflow)
- [Planned Features](#planned-features)

---

## Features

### Current
- **Mood Tracking** — Set your mood from 8 emotions with intensity; history chart shows trends over time
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
                        │ HTTP (REST)
                        ▼
┌───────────────────────────────────────┐
│              API Gateway              │  Port 8080
│           Ktor + JWT Auth             │
└──┬────────┬────────┬─────────┬────────┘
   │        │        │         │
   ▼        ▼        ▼         ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────────────┐
│ Auth │ │ User │ │Avatar│ │Notification  │
│ 8081 │ │ 8082 │ │ 8083 │ │    8085      │
│  PG  │ │  PG  │ │Mongo │ │  PG + AMQP   │
└──────┘ └──┬───┘ └──────┘ └──────────────┘
            │ RabbitMQ events
            ▼
     ┌─────────────┐
     │  Realtime   │  Port 8084  ← WebSocket (direct, no gateway)
     │  Service    │
     │Redis + AMQP │
     └─────────────┘
```

**Message Flow (friend events):**
`user-service` → RabbitMQ (`moodavatar.events`) → `notification-service` (queue: `notification-service`)

**WebSocket Auth:** Connect directly to port 8084 with `?token=<JWT>` query param.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, TypeScript, Tailwind CSS v4, Vite, Pinia, Axios |
| Backend | Ktor (Kotlin), Gradle (multi-stage Docker builds) |
| Databases | PostgreSQL 16, MongoDB 7, Redis 7 |
| Messaging | RabbitMQ 3 (topic exchange `moodavatar.events`) |
| Auth | JWT (access 15 min / refresh 30 days), bcrypt passwords |
| Email | Jakarta Mail → Mailhog (dev) |
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

### 2. Start all services
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
docker compose build <service-name> && docker compose up -d --force-recreate <service-name>
```

> `docker compose up -d` alone does **not** rebuild — always run `build` first.

---

## Services

| Service | Port | Database | Purpose |
|---|---|---|---|
| `api-gateway` | 8080 | — | JWT validation, reverse proxy, CORS |
| `auth-service` | 8081 | PostgreSQL | Registration, login, JWT, email flows |
| `user-service` | 8082 | PostgreSQL | Profiles, friends, RabbitMQ publisher |
| `avatar-service` | 8083 | MongoDB | Avatar config, mood history, needs decay |
| `realtime-service` | 8084 | Redis | WebSocket hub, room visits, presence |
| `notification-service` | 8085 | PostgreSQL | RabbitMQ consumer, in-app notifications |
| Mailhog | 8025 | — | Dev email UI |
| RabbitMQ UI | 15672 | — | Message broker management |

### JWT Configuration
All services share:
```
Issuer:   moodavatar
Audience: moodavatar-users
```
Secret is set via `JWT_SECRET` in `.env`. JWT uses a custom `userId` claim (not standard `sub`).

---

## API Overview

All endpoints are routed through the gateway at `http://localhost:8080`.

| Prefix | Service | Key Endpoints |
|---|---|---|
| `POST /auth/register` | Auth | Register new user |
| `POST /auth/login` | Auth | Returns access + refresh tokens |
| `POST /auth/refresh` | Auth | Rotate refresh token |
| `GET /users/profile/me` | User | Own profile |
| `GET /users/profile/:username` | User | Public profile |
| `GET /friends` | User | Friend list |
| `POST /friends/request` | User | Send friend request |
| `GET /avatars/me` | Avatar | Own avatar config |
| `POST /avatars/me/mood` | Avatar | Set current mood |
| `GET /avatars/me/needs` | Avatar | Current needs (0–100) |
| `GET /notifications` | Notification | All notifications |
| `GET /notifications/unread-count` | Notification | Unread badge count |

WebSocket (direct, port 8084):
```
ws://localhost:8084/ws?token=<JWT>
```
Messages: `ping`, `mood_update`, `join_room`, `leave_room`, `room_reaction`, `room_knock`

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
- **Backend**: `./gradlew ktlintCheck` + `./gradlew test` per service
- **Frontend**: `vue-tsc --noEmit` + `npm run build`

### Local quality checks
```bash
# Backend (run inside a service directory)
./gradlew ktlintCheck   # lint
./gradlew ktlintFormat  # auto-fix formatting
./gradlew test          # unit tests

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

### Phase 8 — Mood Journaling & Insights
Track mood patterns over time with a rich journaling interface.
- GitHub-style calendar heatmap showing mood history
- Insights page: average emotion per weekday, best/worst days, streak tracking
- Extended mood notes (attach text to each mood entry)
- Export mood history as CSV

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
