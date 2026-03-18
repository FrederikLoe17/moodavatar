# Deployment Guide – Hetzner VPS

## 1. Hetzner Server einrichten

### Server erstellen
1. [hetzner.com/cloud](https://www.hetzner.com/cloud) → Projekt erstellen
2. Server hinzufügen:
   - **Typ**: CX22 (2 vCPU, 4 GB RAM, ~3,79 €/Monat)
   - **Image**: Ubuntu 24.04
   - **SSH-Key**: eigenen Public Key hinzufügen
3. Server-IP notieren

### Domain einrichten
- Domain-DNS: A-Record auf die Server-IP setzen
- Alternativ kostenlose Subdomain: [duckdns.org](https://duckdns.org)

### Server-Setup (einmalig, per SSH)

```bash
ssh root@<SERVER_IP>

# Docker installieren
curl -fsSL https://get.docker.com | sh
systemctl enable docker

# Repo klonen
git clone https://github.com/FrederikLoe17/moodavatar.git /opt/moodavatar
cd /opt/moodavatar

# Produktion .env anlegen (Werte anpassen!)
cat > .env << 'EOF'
DOMAIN=deine-domain.de

POSTGRES_USER=moodavatar
POSTGRES_PASSWORD=<sicheres-passwort>

JWT_SECRET=<langer-zufaelliger-string-min-32-zeichen>
JWT_ISSUER=moodavatar
JWT_AUDIENCE=moodavatar-users
JWT_ACCESS_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=2592000000

RABBITMQ_USER=moodavatar
RABBITMQ_PASS=<sicheres-passwort>

# SMTP für E-Mails (z.B. Brevo kostenlos: 300 E-Mails/Tag)
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USER=deine@email.de
SMTP_PASSWORD=<brevo-smtp-passwort>
EOF

# Packages von ghcr.io ziehen und starten
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

Caddy holt sich automatisch ein TLS-Zertifikat (Let's Encrypt). Nach ~30 Sekunden ist die App unter `https://deine-domain.de` erreichbar.

---

## 2. GitHub Secrets für CD-Pipeline

Unter **GitHub → Repository → Settings → Secrets → Actions** folgende Secrets anlegen:

| Secret | Wert |
|--------|------|
| `SERVER_HOST` | IP-Adresse des Hetzner-Servers |
| `SERVER_USER` | `root` |
| `SERVER_SSH_KEY` | Inhalt des privaten SSH-Keys (`~/.ssh/id_rsa`) |

Nach dem ersten manuellen Setup deployed die CD-Pipeline bei jedem Push auf `main` automatisch.

---

## 3. E-Mail (SMTP) einrichten

Für echte E-Mails (Registrierung, Passwort-Reset) empfehle ich **Brevo** (kostenlos, 300 E-Mails/Tag):

1. [brevo.com](https://brevo.com) → Account erstellen
2. **SMTP & API → SMTP** → SMTP-Zugangsdaten kopieren
3. In `.env` auf dem Server eintragen und Services neu starten:
   ```bash
   cd /opt/moodavatar
   docker compose -f docker-compose.prod.yml up -d auth-service
   ```

---

## 4. Nützliche Befehle auf dem Server

```bash
# Logs eines Services anzeigen
docker compose -f docker-compose.prod.yml logs -f auth-service

# Alle Services neu starten
docker compose -f docker-compose.prod.yml restart

# Status aller Container
docker compose -f docker-compose.prod.yml ps

# Manuell deployen (ohne CD-Pipeline)
cd /opt/moodavatar && git pull && docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d
```

---

## 5. Ressourcen-Check

```bash
# RAM-Auslastung
free -h

# CPU + Prozesse
top

# Docker-Statistiken
docker stats --no-stream
```

CX22 (4 GB RAM) reicht für die Beta. Bei mehr Last → Upgrade auf CX32 (8 GB, ~7,57 €/Monat) per Hetzner-Resize (kein Datenverlust).
