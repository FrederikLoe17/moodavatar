# MoodAvatar — CLAUDE.md

## Projekt-Übersicht
Microservices-App mit anpassbaren SVG-Avataren die sich mit der Stimmung verändern.
Root: `C:\Users\loehmanf\Documents\Claude\`

## Stack
- **Backend**: Ktor (Kotlin), PostgreSQL, MongoDB, Redis, RabbitMQ
- **Frontend**: Vue 3 + TypeScript + Tailwind + Vite (`moodavatar-frontend/`)
- **Infra**: Docker Compose

## Services & Ports
| Service              | Port  | Verzeichnis              |
|----------------------|-------|--------------------------|
| API Gateway          | 8080  | `api-gateway/`           |
| Auth Service         | 8081  | `AuthserviceClaude/`     |
| User Service         | 8082  | `user-service/`          |
| Avatar Service       | 8083  | `avatar-service/`        |
| Realtime Service     | 8084  | `realtime-service/`      |
| Notification Service | 8085  | `notification-service/`  |
| Frontend (Vite)      | 5173  | `moodavatar-frontend/`   |
| Mailhog (UI)         | 8025  | —                        |
| RabbitMQ (mgmt UI)   | 15672 | —                        |

## Häufige Befehle
```bash
# Alles starten
docker compose up -d

# Nach Codeänderungen (einzelner Service)
docker compose build <service> && docker compose up -d --force-recreate <service>

# Frontend
cd moodavatar-frontend && npm run dev

# Alice zum Admin machen
docker exec moodavatar-postgres-auth psql -U moodavatar -d moodavatar_auth \
  -c "UPDATE users SET role = 'ADMIN' WHERE username = 'alice';"
```

> `docker compose up -d` allein baut NICHT neu — immer erst `build` wenn Code geändert wurde.

## API Gateway Routes
| Prefix          | Ziel  |
|-----------------|-------|
| `/auth`         | 8081  |
| `/users`        | 8082  |
| `/friends`      | 8082  |
| `/avatars`      | 8083  |
| `/notifications`| 8085  |

WebSocket verbindet direkt auf Port 8084 (nicht via Gateway), Auth via `?token=JWT`.

## JWT-Konfiguration (alle Services)
```
Secret:   super-secret-jwt-key-change-in-production
Issuer:   moodavatar
Audience: moodavatar-users
```
JWT nutzt custom claim `userId` (nicht standard `sub`) — beim Parsen `getClaim("userId").asString()` verwenden.

## Test-Nutzer
Credentials in `test-users.json`. IDs ändern sich nach `docker compose down -v`.
Nach Volume-Reset: Nutzer neu registrieren, `POST /users/internal/profile` manuell aufrufen, `test-users.json` aktualisieren.

---

## Git-Workflow

### Branch-Strategie
```
main      ← geschützt, nur via PR, immer deploybar
develop   ← Integrations-Branch, Basis für alle Feature-Branches
feature/* ← neue Features  (z.B. feature/mood-journaling)
fix/*     ← Bug-Fixes      (z.B. fix/avatar-flicker)
```

### Mein Ablauf als Claude
1. Neuen Branch von `develop` erstellen: `git checkout -b feature/<name>`
2. Änderungen umsetzen + ktlint-clean halten
3. Commit(s) mit aussagekräftigen Messages
4. PR gegen `develop` mit `gh pr create` — du reviewst, mergst, done

### Commits
- Format: `<type>(<scope>): <kurze Beschreibung>` (Conventional Commits)
- Typen: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`
- Beispiel: `feat(avatar-service): add needs decay for social stat`

### Wichtige Befehle
```bash
# Repo lokal initialisieren (einmalig)
git init && git remote add origin https://github.com/<user>/<repo>.git

# Feature starten
git checkout develop && git pull && git checkout -b feature/<name>

# PR erstellen (nach gh auth login)
gh pr create --base develop --title "..." --body "..."

# Branch-Schutz für main setzen (einmalig)
gh api repos/<user>/<repo>/branches/main/protection \
  --method PUT --input branch-protection.json
```

### CI-Status
Jeder PR triggert automatisch:
- **Backend**: `ktlintCheck` + `test` pro Service (nur bei Änderungen im jeweiligen Verzeichnis)
- **Frontend**: `vue-tsc --noEmit` + `npm run build`
Merge nur wenn alle Checks grün sind.

---

## Arbeitsregeln

### Allgemein
- Keine unnötigen Änderungen außerhalb des angeforderten Bereichs (kein Gold-Plating)
- Keine Docstrings/Kommentare zu Code hinzufügen, der nicht geändert wurde
- Einfachste Lösung bevorzugen — keine Abstraktion für Einmal-Operationen
- Kein Backwards-Compatibility-Code wenn Sachen einfach geändert werden können

### Ktor (Kotlin)
- In Route-Dateien immer `import io.ktor.server.application.*` — sonst ist `call` unresolved
- `setBody(String)` + ContentNegotiation serialisiert als JSON-String-Literal → stattdessen `TextContent(body, ContentType.Application.Json)` für rohe JSON-Strings
- Suspend-Funktionen nicht in `onSuccess {}` Callbacks aufrufen — `try/catch` statt `runCatching` verwenden
- Alle Dockerfiles sind multi-stage (Gradle baut inside Docker)

### Code-Qualität (Backend)
- **ktlint** wird als Standard für alle Kotlin-Dateien eingehalten — Formatierung, Naming Conventions und Code-Style gemäß [ktlint Standard Rules](https://pinterest.github.io/ktlint/latest/rules/standard/)
- Vor dem Bauen prüfen: `./gradlew ktlintCheck` — Fehler müssen behoben werden, nicht ignoriert
- Autom. Formatierung: `./gradlew ktlintFormat` (danach nochmals `ktlintCheck`)
- Wichtige ktlint-Regeln im Überblick:
  - Einrückung: 4 Spaces (keine Tabs)
  - Max. Zeilenlänge: 120 Zeichen
  - `import`-Reihenfolge: alphabetisch, keine Wildcard-Imports
  - Trailing commas in Argument-/Parameterlisten
  - Funktionen mit einem Ausdruck als Expression Body (`= ...`) statt Block wenn möglich

### Tests (Backend)
- Jede neue Service-Methode (Business-Logik) bekommt Unit-Tests — reine Datenbank-/HTTP-Handler sind optional
- Test-Framework: **Kotlin Test** + **MockK** für Mocks
- Tests liegen unter `src/test/kotlin/` (spiegelt Package-Struktur)
- Benennung: `<KlassenName>Test.kt`, Testmethoden als `@Test fun should<Verhalten>When<Kontext>()`
- Tests laufen mit `./gradlew test`
- Beim Hinzufügen von Features: zuerst prüfen ob bestehende Tests betroffen sind, dann neue Tests ergänzen

### Dependency-Management (Backend)
- Versionen werden in `gradle/libs.versions.toml` (Version Catalog) gepflegt — keine hardcodierten Versionen in `build.gradle.kts`
- Bei neuen Features: prüfen ob benötigte Library bereits im Catalog vorhanden ist, bevor eine neue hinzugefügt wird
- Ktor, Kotlin, Exposed, Koin etc. regelmäßig auf aktuellen Stand bringen — breaking changes vor Upgrade prüfen
- `./gradlew dependencyUpdates` (ben. Plugin `com.github.ben-manes.versions`) zeigt veraltete Dependencies

### RabbitMQ
- Exchange: `moodavatar.events` (topic)
- Events: `friend.request.sent`, `friend.request.accepted`
- Notification-Service konsumiert von Queue `notification-service`
- Health-Check: `/api/overview` verwenden, nicht `/api/health/checks/aliveness-test/%2F` (returns 400 in v3.13)

### Frontend (Vue 3)
- State Management: Pinia Stores
- HTTP: axios mit Auto-Refresh Interceptor in `src/api/http.ts`
- WebSocket Store: `stores/realtime.ts`
- Router: `router/index.ts`

### Docker
- Alle Dockerfiles sind multi-stage — lokale JARs werden nicht kopiert
- Nach jeder Backend-Änderung: `docker compose build <service> && docker compose up -d --force-recreate <service>`
