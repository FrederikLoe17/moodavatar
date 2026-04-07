#!/usr/bin/env bash
set -euo pipefail

# Load .env
if [[ -f ".env" ]]; then
  export $(grep -v '^#' .env | grep -v '^$' | xargs)
fi

echo "🚇 Starting Cloudflare Tunnel..."
cloudflared tunnel --url localhost:8080 &> /tmp/cloudflared.log &
CLOUDFLARED_PID=$!

# Wait for tunnel URL
echo "⏳ Waiting for tunnel URL..."
TUNNEL_URL=""
for i in $(seq 1 30); do
  TUNNEL_URL=$(grep -o 'https://[a-zA-Z0-9-]*\.trycloudflare\.com' /tmp/cloudflared.log 2>/dev/null | head -1 || true)
  if [[ -n "$TUNNEL_URL" ]]; then
    break
  fi
  sleep 2
done

if [[ -z "$TUNNEL_URL" ]]; then
  echo "❌ Could not extract tunnel URL. Check /tmp/cloudflared.log"
  kill $CLOUDFLARED_PID 2>/dev/null || true
  exit 1
fi

echo "✅ Tunnel URL: $TUNNEL_URL"

WS_URL=$(echo "$TUNNEL_URL" | sed 's/https:/wss:/')

# Update vercel.json
echo "📝 Updating vercel.json..."
cd moodavatar-frontend
node -e "
const fs = require('fs');
const v = JSON.parse(fs.readFileSync('vercel.json', 'utf8'));
v.rewrites = v.rewrites.map(r => {
  if (r.source === '/api/:path*') r.destination = '$TUNNEL_URL/api/:path*';
  return r;
});
fs.writeFileSync('vercel.json', JSON.stringify(v, null, 2) + '\n');
"
cd ..

# Update Vercel Env Vars
echo "🔧 Updating Vercel env vars..."
curl -s -X PATCH "https://api.vercel.com/v9/projects/$VERCEL_PROJECT_ID/env/$VERCEL_BACKEND_URL_ID" \
  -H "Authorization: Bearer $VERCEL_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"value\": \"$TUNNEL_URL\"}" > /dev/null

curl -s -X PATCH "https://api.vercel.com/v9/projects/$VERCEL_PROJECT_ID/env/$VERCEL_WS_URL_ID" \
  -H "Authorization: Bearer $VERCEL_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"value\": \"$WS_URL\"}" > /dev/null

echo "✅ Vercel env vars updated"

# Git commit + push
echo "🚀 Pushing to trigger Vercel redeploy..."
git add moodavatar-frontend/vercel.json
git commit -m "chore(deploy): update tunnel URL to $TUNNEL_URL"
git push origin main

echo ""
echo "🎉 Done! Vercel will redeploy shortly."
echo "   Tunnel: $TUNNEL_URL"
echo "   WS:     $WS_URL"
echo "   Frontend: https://moodavatar.vercel.app"
echo ""
echo "Tunnel is running (PID $CLOUDFLARED_PID). Press Ctrl+C to stop."
wait $CLOUDFLARED_PID
