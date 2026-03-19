# MoodAvatar — Free Deployment Guide

Stack: **Koyeb** (backend) · **Vercel** (frontend) · **Neon** (PostgreSQL) · **MongoDB Atlas** (MongoDB) · **Brevo** (Email)

All services are free. No credit card required.

---

## Step 1 — PostgreSQL on Neon

1. Create account at **neon.tech** (GitHub login works)
2. Create a new project → name it `moodavatar`
3. Copy the connection string — it looks like:
   ```
   postgresql://user:password@ep-xxx.eu-central-1.aws.neon.tech/neondb?sslmode=require
   ```
4. Note down the individual parts you'll need later:
   - `DB_HOST` = `ep-xxx.eu-central-1.aws.neon.tech`
   - `DB_PORT` = `5432`
   - `DB_NAME` = `neondb`
   - `DB_USER` = the user from the connection string
   - `DB_PASSWORD` = the password from the connection string

> Flyway runs automatically on backend startup and creates all tables.

---

## Step 2 — MongoDB on Atlas

1. Create account at **mongodb.com/atlas** (GitHub login works)
2. Create a free **M0** cluster → choose a region close to your Koyeb region
3. Under **Database Access**: create a user with read/write permissions, note the password
4. Under **Network Access**: add `0.0.0.0/0` (allow all IPs — needed for Koyeb)
5. Click **Connect** → **Drivers** → copy the connection string:
   ```
   mongodb+srv://youruser:yourpassword@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```
6. Add the database name: replace `/?` with `/moodavatar?`

---

## Step 3 — Email on Brevo

1. Create account at **brevo.com** (no credit card)
2. Go to **SMTP & API** → **SMTP**
3. Note down:
   - `SMTP_HOST` = `smtp-relay.brevo.com`
   - `SMTP_PORT` = `587`
   - `SMTP_USER` = your Brevo login email
   - `SMTP_PASSWORD` = the SMTP key shown in the dashboard

---

## Step 4 — Backend on Koyeb

1. Create account at **koyeb.com** (GitHub login works)
2. Click **Create App** → **GitHub**
3. Connect your GitHub account and select the `moodavatar` repository
4. Configure the service:
   - **Branch**: `main`
   - **Build directory**: `moodavatar-backend`
   - **Dockerfile path**: `Dockerfile`
   - **Port**: `8080`
   - **Region**: pick one close to your Atlas cluster
5. Add all environment variables (click **Add variable** for each):

| Variable | Value |
|---|---|
| `DB_HOST` | from Neon |
| `DB_PORT` | `5432` |
| `DB_NAME` | from Neon |
| `DB_USER` | from Neon |
| `DB_PASSWORD` | from Neon |
| `MONGO_URI` | full Atlas connection string with `/moodavatar?...` |
| `MONGO_DB` | `moodavatar` |
| `JWT_SECRET` | generate a long random string (e.g. `openssl rand -hex 32`) |
| `JWT_ISSUER` | `moodavatar` |
| `JWT_AUDIENCE` | `moodavatar-users` |
| `JWT_ACCESS_EXPIRY_MS` | `900000` |
| `JWT_REFRESH_EXPIRY_MS` | `2592000000` |
| `SMTP_HOST` | from Brevo |
| `SMTP_PORT` | `587` |
| `SMTP_USER` | from Brevo |
| `SMTP_PASSWORD` | from Brevo |
| `APP_BASE_URL` | `https://your-frontend.vercel.app` (fill in after Step 5) |
| `ALLOWED_ORIGIN` | `https://your-frontend.vercel.app` (fill in after Step 5) |

6. Click **Deploy** — Koyeb builds the Docker image and starts the backend
7. Note down the public URL: `https://your-app-xxx.koyeb.app`

---

## Step 5 — Frontend on Vercel

1. Create account at **vercel.com** (GitHub login works)
2. Click **Add New Project** → import the `moodavatar` repository
3. Configure:
   - **Root Directory**: `moodavatar-frontend`
   - **Build Command**: `npm run build` (auto-detected)
   - **Output Directory**: `dist` (auto-detected)
4. Add environment variable:

| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://your-app-xxx.koyeb.app` (from Step 4) |

5. Click **Deploy**
6. Note down your Vercel URL: `https://your-frontend.vercel.app`

---

## Step 6 — Connect backend ↔ frontend

Go back to Koyeb → your service → **Environment variables** and update:
- `APP_BASE_URL` → `https://your-frontend.vercel.app`
- `ALLOWED_ORIGIN` → `https://your-frontend.vercel.app`

Click **Redeploy** in Koyeb.

---

## Done!

Open `https://your-frontend.vercel.app` — register, verify email (check inbox), done.

From now on: every push to `main` automatically redeploys both frontend (Vercel) and backend (Koyeb).

---

## Local Development

Nothing changes locally:

```bash
# Backend + DBs
docker compose up -d

# Frontend (Vite proxy handles /api and /ws)
cd moodavatar-frontend && npm run dev
```

`VITE_API_BASE_URL` is not set locally → Vite proxy forwards to `localhost:8080` automatically.
