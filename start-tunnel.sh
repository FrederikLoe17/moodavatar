#!/bin/bash
# Startet den Cloudflare Tunnel und updated automatisch:
#   - moodavatar-frontend/vercel.json (Rewrite-Destination)
#   - Vercel Env Vars: BACKEND_URL + VITE_WS_URL
#   - Pusht zu git -> Vercel deployed automatisch neu

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Credentials aus .env laden
if [ ! -f "$SCRIPT_DIR/.env" ]; then
  echo "❌ .env nicht gefunden in $SCRIPT_DIR"
  exit 1
fi
source "$SCRIPT_DIR/.env"

VERCEL_JSON="$SCRIPT_DIR/moodavatar-frontend/vercel.json"

echo "🚀 Cloudflare Tunnel wird gestartet..."
TUNNEL_LOG=$(mktemp)
cloudflared tunnel --url localhost:8080 >"$TUNNEL_LOG" 2>&1 &
TUNNEL_PID=$!

# Auf URL warten
echo "⏳ Warte auf Tunnel-URL..."
TUNNEL_URL=""
for i in $(seq 1 30); do
  TUNNEL_URL=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' "$TUNNEL_LOG" 2>/dev/null | head -1)
  if [ -n "$TUNNEL_URL" ]; then
    break
  fi
  sleep 1
done

if [ -z "$TUNNEL_URL" ]; then
  echo "❌ Tunnel-URL konnte nicht ermittelt werden (30s Timeout)"
  kill "$TUNNEL_PID" 2>/dev/null
  exit 1
fi

WS_URL="${TUNNEL_URL/https:\/\//wss://}"

echo "✅ Tunnel-URL: $TUNNEL_URL"
echo "✅ WebSocket-URL: $WS_URL"

# vercel.json updaten
echo ""
echo "📝 vercel.json wird aktualisiert..."
sed -i "s|https://[a-z0-9-]*\.trycloudflare\.com|$TUNNEL_URL|g" "$VERCEL_JSON"
echo "   ✅ vercel.json aktualisiert"

# Vercel Env Vars updaten
echo ""
echo "🔄 Vercel Env Vars werden aktualisiert..."

update_env() {
  local id=$1
  local value=$2
  local key=$3
  local response
  response=$(curl -s -X PATCH \
    "https://api.vercel.com/v9/projects/$VERCEL_PROJECT_ID/env/$id" \
    -H "Authorization: Bearer $VERCEL_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"value\": \"$value\"}")
  if echo "$response" | grep -q '"key"'; then
    echo "   ✅ $key aktualisiert"
  else
    echo "   ❌ $key fehlgeschlagen: $response"
  fi
}

update_env "$VERCEL_BACKEND_URL_ID" "$TUNNEL_URL" "BACKEND_URL"
update_env "$VERCEL_WS_URL_ID" "$WS_URL" "VITE_WS_URL"

# Git commit & push -> Vercel Deploy
echo ""
echo "📤 Push zu git (löst Vercel-Deploy aus)..."
cd "$SCRIPT_DIR"
git add moodavatar-frontend/vercel.json
git commit -m "chore: update cloudflare tunnel url"
git push origin main

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Fertig! Vercel deployt jetzt neu (~1-2 Min)"
echo "🔗 Tunnel:    $TUNNEL_URL"
echo "🔗 WebSocket: $WS_URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Ctrl+C um den Tunnel zu stoppen"

wait "$TUNNEL_PID"
