# MoodAvatar — CLAUDE.md

## Projekt-Übersicht
Full-Stack-App mit anpassbaren SVG-Avataren die sich mit der Stimmung verändern.
Root: `C:\Users\loehmanf\Documents\Claude\`

## Stack
- **Backend**: Ktor (Kotlin), PostgreSQL, MongoDB (`moodavatar-backend/`)
- **Frontend**: Vue 3 + TypeScript + Tailwind + Vite (`moodavatar-frontend/`)
- **Infra**: Docker Compose

## Services & Ports
| Service              | Port  | Verzeichnis              |
|----------------------|-------|--------------------------|
| Backend (Monolith)   | 8080  | `moodavatar-backend/`    |
| Frontend (Vite)      | 5173  | `moodavatar-frontend/`   |
| Mailhog (UI)         | 8025  | —                        |

## Häufige Befehle
```bash
# Alles starten
docker compose up -d

# Nach Codeänderungen (Backend)
docker compose build backend && docker compose up -d --force-recreate backend

# Frontend
cd moodavatar-frontend && npm run dev

# Alice zum Admin machen
docker exec moodavatar-postgres psql -U moodavatar -d moodavatar \
  -c "UPDATE users SET role = 'ADMIN' WHERE username = 'alice';"
```

> `docker compose up -d` allein baut NICHT neu — immer erst `build` wenn Code geändert wurde.

## JWT-Konfiguration
```
Secret:   super-secret-jwt-key-change-in-production
Issuer:   moodavatar
Audience: moodavatar-users
```
JWT nutzt custom claim `userId` (nicht standard `sub`) — beim Parsen `getClaim("userId").asString()` verwenden.

## Test-Nutzer
Credentials in `test-users.json`. IDs ändern sich nach `docker compose down -v`.
Nach Volume-Reset: Nutzer neu registrieren, `test-users.json` aktualisieren.

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
4. **Vor dem PR — Pflicht-Checks** (Docker muss laufen für Testcontainers):
   ```bash
   cd moodavatar-backend && ./gradlew ktlintCheck test
   ```
5. PR gegen `develop` mit `gh pr create` — du reviewst, mergst, done

> `gh` liegt unter `C:\Program Files\GitHub CLI\` und ist nicht im Standard-PATH.
> Immer `export PATH="$PATH:/c/Program Files/GitHub CLI"` voranstellen.

### Commits
- Format: `<type>(<scope>): <kurze Beschreibung>` (Conventional Commits)
- Typen: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`
- Beispiel: `feat(avatars): add needs decay for social stat`

### Wichtige Befehle
```bash
# Feature starten
git checkout develop && git pull && git checkout -b feature/<name>

# PR erstellen (nach gh auth login)
gh pr create --base develop --title "..." --body "..."
```

### CI-Status
Jeder PR triggert automatisch:
- **Backend**: `ktlintCheck` + `test` (bei Änderungen in `moodavatar-backend/`)
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
- Dockerfile ist multi-stage (Gradle baut inside Docker)

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
- Benennung: `<KlassenName>Test.kt`, Testmethoden als `` @Test fun `beschreibung`() ``
- Tests laufen mit `./gradlew test` — **Docker muss laufen** (Testcontainers für PostgreSQL)
- Beim Hinzufügen von Features: zuerst prüfen ob bestehende Tests betroffen sind, dann neue Tests ergänzen

#### Bekannte Test-Gotchas (hart gelernt)

**JUnit 4: `@BeforeTest`/`@AfterTest` müssen `Unit` zurückgeben**
JUnit 4 verlangt `void`-Rückgabe. Expression Body mit `deleteAll()` gibt `Int` zurück → `InvalidTestClassError`.
```kotlin
// FALSCH — gibt Int zurück
@BeforeTest
fun cleanDb() = transaction { Users.deleteAll() }

// RICHTIG — expliziter Block-Body
@BeforeTest
fun cleanDb() {
    transaction { Users.deleteAll() }
}
```

**`testApplication` lädt echte `application.conf` — `${JWT_SECRET}` nicht auflösbar**
Ktor-Route-Tests mit `configureSecurity()` schlagen fehl mit `ConfigException$UnresolvedSubstitution`.
Fix: Test-Config als Environment übergeben, am besten via Helper:
```kotlin
private fun routeTest(block: suspend ApplicationTestBuilder.() -> Unit) =
    testApplication {
        environment { config = TestDatabase.config }
        application { installTestApp() }
        block()
    }
```

**MockK: Kotlin-stdlib Extension-Funktionen nicht direkt stubbbar**
`every { mockIterable.firstOrNull() } returns doc` → `ClassCastException` zur Laufzeit.
Stattdessen die zugrundeliegende Interface-Methode mocken:
```kotlin
val mockCursor = mockk<MongoCursor<Document>>()
every { mockIterable.iterator() } returns mockCursor
every { mockCursor.hasNext() } returns true
every { mockCursor.next() } returns doc
```

**ktlint bricht vollqualifizierte Funktionsnamen über Zeilengrenzen auf**
`io.ktor.serialization.kotlinx.json.json(...)` wird zu zwei Zeilen → Compiler-Fehler, weil Packages keine Werte sind.
Fix: Expliziten Import hinzufügen statt vollqualifizierten Namen:
```kotlin
import io.ktor.serialization.kotlinx.json.json
// dann einfach:
json(Json { ignoreUnknownKeys = true })
```

**Testcontainers: Singleton-Pattern für geteilte DB**
Container nur einmal starten (teuer!), nicht pro Test-Klasse. Pattern: Kotlin `object` mit `init`-Block.
Siehe `TestDatabase.kt` in `moodavatar-backend/` als Referenz.

**H2 vs PostgreSQL: `customEnumeration` inkompatibel**
Tabellen mit `customEnumeration` (PGobject für Enums) laufen nicht auf H2.
→ Immer Testcontainers (echtes PostgreSQL) verwenden.

### Dependency-Management (Backend)
- Versionen als Variablen in `moodavatar-backend/build.gradle.kts`
- Ktor, Kotlin, Exposed etc. regelmäßig aktualisieren — breaking changes vor Upgrade prüfen
- Aktuelle Basis-Versionen: Kotlin 1.9.23, Ktor 2.3.10, Exposed 0.44.1, ktlint 1.3.1

### Frontend (Vue 3)
- State Management: Pinia Stores
- HTTP: axios mit Auto-Refresh Interceptor in `src/api/http.ts`
- WebSocket Store: `stores/realtime.ts`
- Router: `router/index.ts`

### Docker
- Dockerfile ist multi-stage — lokale JARs werden nicht kopiert
- Nach jeder Backend-Änderung: `docker compose build backend && docker compose up -d --force-recreate backend`
