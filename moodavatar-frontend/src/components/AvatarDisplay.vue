<template>
  <div :style="`width:${size}px;height:${size}px;display:inline-block;position:relative`">
    <svg viewBox="0 0 200 200" :width="size" :height="size" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient :id="`skin-${uid}`" cx="35%" cy="28%" r="70%">
          <stop offset="0%"   :stop-color="skinLight" />
          <stop offset="100%" :stop-color="skin" />
        </radialGradient>
        <radialGradient :id="`aura-${uid}`" cx="50%" cy="60%" r="55%">
          <stop offset="0%"   :stop-color="moodColor" stop-opacity="0.35" />
          <stop offset="100%" :stop-color="moodColor" stop-opacity="0"    />
        </radialGradient>
        <filter :id="`glow-${uid}`" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="5" />
        </filter>
        <!-- Window clip on back wall (right side) -->
        <clipPath :id="`win-${uid}`">
          <rect x="118" y="17" width="44" height="51" rx="3"/>
        </clipPath>
        <!-- Floor clip for perspective lines -->
        <clipPath :id="`floor-${uid}`">
          <polygon points="0,168 35,125 165,125 200,168 200,200 0,200"/>
        </clipPath>
      </defs>

      <!-- ══ ROOM (3D perspective) ══════════════════════════════════════════════ -->

      <!-- Ceiling -->
      <polygon points="0,0 200,0 165,12 35,12" :fill="ceiling" />

      <!-- Back wall (center) -->
      <rect x="35" y="12" width="130" height="113" :fill="wall" />

      <!-- Left side wall (slightly darker for depth) -->
      <polygon points="0,0 35,12 35,125 0,168" :fill="wallLeft" />

      <!-- Right side wall -->
      <polygon points="165,12 200,0 200,168 165,125" :fill="wallRight" />

      <!-- Floor -->
      <polygon points="0,168 35,125 165,125 200,168 200,200 0,200" :fill="floor" />

      <!-- Corner definition lines -->
      <line x1="35"  y1="12"  x2="35"  y2="125" stroke="rgba(0,0,0,0.3)"  stroke-width="1.2"/>
      <line x1="165" y1="12"  x2="165" y2="125" stroke="rgba(0,0,0,0.3)"  stroke-width="1.2"/>
      <line x1="0"   y1="0"   x2="35"  y2="12"  stroke="rgba(0,0,0,0.18)" stroke-width="0.8"/>
      <line x1="165" y1="12"  x2="200" y2="0"   stroke="rgba(0,0,0,0.18)" stroke-width="0.8"/>
      <line x1="0"   y1="168" x2="35"  y2="125" stroke="rgba(0,0,0,0.15)" stroke-width="0.8"/>
      <line x1="165" y1="125" x2="200" y2="168" stroke="rgba(0,0,0,0.15)" stroke-width="0.8"/>

      <!-- Ambient occlusion at back wall corners -->
      <rect x="35" y="12" width="3" height="113" fill="rgba(0,0,0,0.1)"/>
      <rect x="162" y="12" width="3" height="113" fill="rgba(0,0,0,0.1)"/>
      <rect x="35" y="122" width="130" height="3" fill="rgba(0,0,0,0.12)"/>

      <!-- Baseboard -->
      <rect x="35" y="122" width="130" height="3" :fill="floorEdge"/>
      <polygon points="1,166 3,165 37,122 35,122 35,125 0,168"         :fill="floorEdge" opacity="0.6"/>
      <polygon points="163,122 165,122 165,125 200,168 197,169 163,125" :fill="floorEdge" opacity="0.6"/>

      <!-- Floor shadow strip near back wall -->
      <polygon points="35,125 165,125 165,138 35,138" fill="rgba(0,0,0,0.07)"/>
      <polygon points="0,168 35,125 37,131 3,170"     fill="rgba(0,0,0,0.05)"/>
      <polygon points="163,131 165,125 200,168 197,170" fill="rgba(0,0,0,0.05)"/>

      <!-- Floor perspective lines -->
      <g :clip-path="`url(#floor-${uid})`">
        <line x1="100" y1="125" x2="0"   y2="200" stroke="rgba(0,0,0,0.07)" stroke-width="0.8"/>
        <line x1="100" y1="125" x2="50"  y2="200" stroke="rgba(0,0,0,0.05)" stroke-width="0.6"/>
        <line x1="100" y1="125" x2="100" y2="200" stroke="rgba(0,0,0,0.05)" stroke-width="0.6"/>
        <line x1="100" y1="125" x2="150" y2="200" stroke="rgba(0,0,0,0.05)" stroke-width="0.6"/>
        <line x1="100" y1="125" x2="200" y2="200" stroke="rgba(0,0,0,0.07)" stroke-width="0.8"/>
        <!-- Horizontal floor divisions (plank lines) -->
        <line x1="0" y1="148" x2="200" y2="148" stroke="rgba(0,0,0,0.04)" stroke-width="0.5"/>
        <line x1="0" y1="162" x2="200" y2="162" stroke="rgba(0,0,0,0.04)" stroke-width="0.5"/>
        <line x1="0" y1="178" x2="200" y2="178" stroke="rgba(0,0,0,0.04)" stroke-width="0.5"/>
        <line x1="0" y1="192" x2="200" y2="192" stroke="rgba(0,0,0,0.04)" stroke-width="0.5"/>
      </g>

      <!-- Window on back wall (right side) – detail only -->
      <template v-if="isDetailed">
        <rect x="118" y="17" width="44" height="51" rx="3" :fill="skyColor" />
        <g :clip-path="`url(#win-${uid})`">
          <!-- Sun for happy/content -->
          <template v-if="emotion === 'HAPPY' || emotion === 'CONTENT'">
            <circle cx="155" cy="32" r="8" fill="#fbbf24" />
            <line v-for="a in [0,45,90,135,180,225,270,315]" :key="a"
              :x1="155 + 11*Math.cos(a*Math.PI/180)"
              :y1="32  + 11*Math.sin(a*Math.PI/180)"
              :x2="155 + 15*Math.cos(a*Math.PI/180)"
              :y2="32  + 15*Math.sin(a*Math.PI/180)"
              stroke="#fbbf24" stroke-width="2" stroke-linecap="round"/>
          </template>
          <!-- Sparkles for excited -->
          <template v-if="emotion === 'EXCITED'">
            <circle cx="126" cy="25" r="2"   fill="white" opacity="0.9"/>
            <circle cx="135" cy="38" r="1.5" fill="white" opacity="0.7"/>
            <circle cx="148" cy="22" r="1.5" fill="white" opacity="0.8"/>
            <circle cx="158" cy="35" r="2"   fill="white" opacity="0.9"/>
          </template>
          <!-- Moon for tired -->
          <template v-if="emotion === 'TIRED'">
            <path d="M 150,22 Q 159,32 150,42 Q 141,32 150,22 Z" fill="#e2e8f0"/>
            <circle cx="130" cy="24" r="1"   fill="white" opacity="0.8"/>
            <circle cx="138" cy="38" r="1.2" fill="white" opacity="0.6"/>
            <circle cx="128" cy="36" r="0.8" fill="white" opacity="0.5"/>
          </template>
          <!-- Rain for sad -->
          <template v-if="emotion === 'SAD'">
            <ellipse cx="134" cy="28" rx="11" ry="6" fill="#94a3b8" opacity="0.8"/>
            <ellipse cx="148" cy="26" rx="9"  ry="5" fill="#94a3b8" opacity="0.9"/>
            <line x1="129" y1="36" x2="127" y2="46" stroke="#64748b" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="137" y1="36" x2="135" y2="46" stroke="#64748b" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="145" y1="36" x2="143" y2="46" stroke="#64748b" stroke-width="1.5" stroke-linecap="round"/>
          </template>
          <!-- Fire/lightning for angry -->
          <template v-if="emotion === 'ANGRY'">
            <circle cx="143" cy="32" r="14" fill="#ff7043" opacity="0.5"/>
            <path d="M 140,22 L 136,31 L 141,31 L 136,43 L 148,28 L 142,28 Z"
              fill="#fbbf24" opacity="0.9"/>
          </template>
          <!-- Storm for anxious -->
          <template v-if="emotion === 'ANXIOUS'">
            <ellipse cx="132" cy="30" rx="10" ry="6" fill="#78716c" opacity="0.8"/>
            <ellipse cx="147" cy="28" rx="9"  ry="5" fill="#57534e" opacity="0.8"/>
            <line x1="137" y1="38" x2="135" y2="50" stroke="#475569" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="145" y1="40" x2="144" y2="52" stroke="#475569" stroke-width="1.5" stroke-linecap="round"/>
          </template>
        </g>
        <!-- Window frame -->
        <rect x="118" y="17" width="44" height="51" rx="3" fill="none" stroke="#475569" stroke-width="2.5"/>
        <line x1="140" y1="17" x2="140" y2="68" stroke="#475569" stroke-width="1.5"/>
        <line x1="118" y1="42" x2="162" y2="42" stroke="#475569" stroke-width="1.5"/>
        <!-- Window sill -->
        <rect x="116" y="66" width="48" height="4" rx="1" fill="#334155"/>
      </template>

      <!-- Room items (detail only) -->
      <template v-if="isDetailed">

        <!-- Plant (back-left corner, against back wall) -->
        <template v-if="hasRoomItem('plant')">
          <rect x="37"  y="112" width="13" height="14" rx="2" fill="#92400e"/>
          <rect x="35"  y="110" width="17" height="5"  rx="2" fill="#78350f"/>
          <ellipse cx="44" cy="100" rx="6" ry="10" fill="#16a34a" transform="rotate(-20 44 100)"/>
          <ellipse cx="51" cy="97"  rx="5" ry="9"  fill="#22c55e" transform="rotate(15 51 97)"/>
          <ellipse cx="38" cy="99"  rx="5" ry="8"  fill="#15803d" transform="rotate(-35 38 99)"/>
        </template>

        <!-- Bookshelf (right side wall area) -->
        <template v-if="hasRoomItem('bookshelf')">
          <rect x="165" y="55" width="28" height="70" rx="2" fill="#92400e"/>
          <rect x="165" y="73" width="28" height="3"      fill="#78350f"/>
          <rect x="165" y="93" width="28" height="3"      fill="#78350f"/>
          <rect x="165" y="113" width="28" height="3"     fill="#78350f"/>
          <!-- Books row 1 -->
          <rect x="167" y="57"  width="5"  height="16" fill="#3b82f6"/>
          <rect x="173" y="59"  width="4"  height="14" fill="#ef4444"/>
          <rect x="178" y="57"  width="5"  height="16" fill="#10b981"/>
          <rect x="184" y="60"  width="4"  height="13" fill="#f59e0b"/>
          <!-- Books row 2 -->
          <rect x="167" y="76"  width="4"  height="17" fill="#8b5cf6"/>
          <rect x="172" y="77"  width="5"  height="16" fill="#ec4899"/>
          <rect x="178" y="76"  width="4"  height="17" fill="#06b6d4"/>
          <rect x="183" y="78"  width="5"  height="15" fill="#f59e0b"/>
          <!-- Books row 3 -->
          <rect x="167" y="96"  width="6"  height="17" fill="#f59e0b"/>
          <rect x="174" y="97"  width="4"  height="16" fill="#3b82f6"/>
          <rect x="179" y="96"  width="5"  height="17" fill="#ef4444"/>
        </template>

        <!-- Lamp (left side of back wall, only if no plant) -->
        <template v-if="hasRoomItem('lamp') && !hasRoomItem('plant')">
          <rect x="42" y="90" width="4" height="36" rx="2" fill="#94a3b8"/>
          <ellipse cx="44" cy="126" rx="9" ry="3.5" fill="#94a3b8"/>
          <path d="M 33,90 Q 44,80 55,90 L 53,100 Q 44,94 35,100 Z" fill="#fbbf24"/>
          <ellipse cx="44" cy="95" rx="13" ry="7" fill="#fbbf24" opacity="0.12"/>
        </template>

        <!-- Rug (on floor, under character) -->
        <template v-if="hasRoomItem('rug')">
          <ellipse cx="100" cy="184" rx="48" ry="13" :fill="moodColor" opacity="0.22"/>
          <ellipse cx="100" cy="184" rx="48" ry="13" fill="none" :stroke="moodColor" stroke-width="1.5" opacity="0.4"/>
          <ellipse cx="100" cy="184" rx="38" ry="10" fill="none" :stroke="moodColor" stroke-width="1"   opacity="0.25"/>
        </template>

      </template>

      <!-- ══ ATMOSPHERE ═════════════════════════════════════════════════════════ -->
      <ellipse cx="100" cy="130" rx="70" ry="80" :fill="`url(#aura-${uid})`" class="aura"/>

      <!-- ══ CHARACTER ══════════════════════════════════════════════════════════ -->

      <!-- Shadow on floor -->
      <ellipse cx="100" cy="197" rx="22" ry="5" fill="rgba(0,0,0,0.25)"/>

      <!-- Arms (behind torso) -->
      <path :d="pose.armL" :stroke="clothes" stroke-width="11" fill="none" stroke-linecap="round"/>
      <path :d="pose.armR" :stroke="clothes" stroke-width="11" fill="none" stroke-linecap="round"/>
      <!-- Hands -->
      <circle :cx="pose.handL[0]" :cy="pose.handL[1]" r="5.5" :fill="skin"/>
      <circle :cx="pose.handR[0]" :cy="pose.handR[1]" r="5.5" :fill="skin"/>

      <!-- Legs -->
      <path :d="pose.legL" :stroke="pants" stroke-width="12" fill="none" stroke-linecap="round"/>
      <path :d="pose.legR" :stroke="pants" stroke-width="12" fill="none" stroke-linecap="round"/>
      <!-- Shoes -->
      <ellipse :cx="pose.shoeL[0]" :cy="pose.shoeL[1]" rx="13" ry="5" fill="#1e293b"/>
      <ellipse :cx="pose.shoeR[0]" :cy="pose.shoeR[1]" rx="13" ry="5" fill="#1e293b"/>

      <!-- Torso -->
      <path d="M 80,114 Q 100,111 120,114 L 122,150 Q 100,154 78,150 Z" :fill="clothes"/>
      <!-- Neck -->
      <rect x="94" y="107" width="12" height="9" rx="3" :fill="skin"/>

      <!-- ── Hair back layer ── -->

      <template v-if="hair === 'short'">
        <ellipse cx="100" cy="81" rx="25" ry="23" :fill="hairCol"/>
      </template>
      <template v-else-if="hair === 'medium'">
        <ellipse cx="100" cy="81" rx="27" ry="24" :fill="hairCol"/>
        <path d="M 76,92 Q 71,102 72,114 Q 76,122 80,118 Q 78,108 78,96 Z" :fill="hairCol"/>
        <path d="M 124,92 Q 129,102 128,114 Q 124,122 120,118 Q 122,108 122,96 Z" :fill="hairCol"/>
      </template>
      <template v-else-if="hair === 'long'">
        <ellipse cx="100" cy="81" rx="27" ry="24" :fill="hairCol"/>
        <path d="M 76,92 Q 68,114 70,140 Q 74,152 80,148 Q 76,130 76,114 Q 76,100 78,94 Z" :fill="hairCol"/>
        <path d="M 124,92 Q 132,114 130,140 Q 126,152 120,148 Q 124,130 124,114 Q 124,100 122,94 Z" :fill="hairCol"/>
      </template>
      <template v-else-if="hair === 'curly'">
        <circle cx="100" cy="72" r="16" :fill="hairCol"/>
        <circle cx="88"  cy="77" r="14" :fill="hairCol"/>
        <circle cx="112" cy="77" r="14" :fill="hairCol"/>
        <circle cx="94"  cy="68" r="11" :fill="hairCol"/>
        <circle cx="106" cy="68" r="11" :fill="hairCol"/>
        <circle cx="100" cy="65" r="9"  :fill="hairCol"/>
      </template>
      <template v-else-if="hair === 'spiky'">
        <polygon points="100,52 104,67 96,67"   :fill="hairCol"/>
        <polygon points="111,55 113,70 107,69"  :fill="hairCol"/>
        <polygon points="89,55 93,69 87,70"     :fill="hairCol"/>
        <polygon points="118,60 118,74 112,73"  :fill="hairCol"/>
        <polygon points="82,60 88,73 82,74"     :fill="hairCol"/>
        <ellipse cx="100" cy="80" rx="25" ry="21" :fill="hairCol"/>
      </template>

      <!-- ── Head / Face ── -->
      <circle cx="100" cy="87" r="22" :fill="`url(#skin-${uid})`"/>
      <!-- Mood color rim/glow on face -->
      <circle cx="100" cy="87" r="22" fill="none" :stroke="moodColor" stroke-width="1.5" opacity="0.4"/>

      <!-- ── Eyes ── -->

      <!-- HAPPY / CONTENT: closed ^ arcs -->
      <template v-if="emotion === 'HAPPY' || emotion === 'CONTENT'">
        <path d="M 90,83 Q 93,79 96,83" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <path d="M 104,83 Q 107,79 110,83" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>
      </template>

      <!-- EXCITED: sparkle eyes -->
      <template v-else-if="emotion === 'EXCITED'">
        <circle cx="93"  cy="82" r="2.5" fill="white" opacity="0.95"/>
        <circle cx="93"  cy="82" r="1.5" fill="#f59e0b"/>
        <circle cx="107" cy="82" r="2.5" fill="white" opacity="0.95"/>
        <circle cx="107" cy="82" r="1.5" fill="#f59e0b"/>
        <circle cx="90"  cy="79" r="1"   fill="white" opacity="0.8"/>
        <circle cx="96"  cy="78" r="0.8" fill="white" opacity="0.6"/>
        <circle cx="104" cy="78" r="0.8" fill="white" opacity="0.6"/>
        <circle cx="110" cy="79" r="1"   fill="white" opacity="0.8"/>
      </template>

      <!-- TIRED: droopy eyes -->
      <template v-else-if="emotion === 'TIRED'">
        <ellipse cx="93"  cy="83" rx="3"   ry="2"   fill="white" opacity="0.9"/>
        <ellipse cx="93"  cy="83" rx="3"   ry="1.2" fill="rgba(0,0,0,0.18)"/>
        <ellipse cx="107" cy="83" rx="3"   ry="2"   fill="white" opacity="0.9"/>
        <ellipse cx="107" cy="83" rx="3"   ry="1.2" fill="rgba(0,0,0,0.18)"/>
        <text x="116" y="73" fill="white" font-size="5" opacity="0.8" font-family="sans-serif">z</text>
        <text x="120" y="69" fill="white" font-size="4" opacity="0.6" font-family="sans-serif">z</text>
      </template>

      <!-- ANGRY: slanted brows + narrow eyes -->
      <template v-else-if="emotion === 'ANGRY'">
        <path d="M 89,82 L 97,84" stroke="white" stroke-width="2" stroke-linecap="round"/>
        <path d="M 111,82 L 103,84" stroke="white" stroke-width="2" stroke-linecap="round"/>
        <ellipse cx="93"  cy="84" rx="3" ry="2" fill="white" opacity="0.9"/>
        <ellipse cx="107" cy="84" rx="3" ry="2" fill="white" opacity="0.9"/>
        <circle  cx="93"  cy="84" r="1.5" fill="#ef4444"/>
        <circle  cx="107" cy="84" r="1.5" fill="#ef4444"/>
      </template>

      <!-- ANXIOUS: wide eyes + raised brows -->
      <template v-else-if="emotion === 'ANXIOUS'">
        <path d="M 90,83 Q 93,80 96,83" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
        <path d="M 104,83 Q 107,80 110,83" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
        <circle cx="93"  cy="84" r="3"   fill="white" opacity="0.9"/>
        <circle cx="107" cy="84" r="3"   fill="white" opacity="0.9"/>
        <circle cx="93"  cy="84" r="2"   fill="#0f172a"/>
        <circle cx="107" cy="84" r="2"   fill="#0f172a"/>
        <circle cx="94"  cy="83" r="0.8" fill="white"/>
        <circle cx="108" cy="83" r="0.8" fill="white"/>
      </template>

      <!-- DEFAULT (SAD, NEUTRAL): normal eyes -->
      <template v-else>
        <circle cx="93"  cy="83" r="3"   fill="white" opacity="0.9"/>
        <circle cx="107" cy="83" r="3"   fill="white" opacity="0.9"/>
        <circle cx="93"  cy="83" r="1.8" fill="#0f172a"/>
        <circle cx="107" cy="83" r="1.8" fill="#0f172a"/>
        <circle cx="94"  cy="82" r="0.8" fill="white"/>
        <circle cx="108" cy="82" r="0.8" fill="white"/>
      </template>

      <!-- ── Mouth ── -->
      <path v-if="emotion === 'HAPPY' || emotion === 'CONTENT'"
        d="M 91,93 Q 100,99 109,93" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>

      <template v-else-if="emotion === 'EXCITED'">
        <path d="M 91,92 Q 100,99 109,92" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <ellipse cx="100" cy="96" rx="6" ry="3" fill="white" opacity="0.2"/>
      </template>

      <path v-else-if="emotion === 'SAD'"
        d="M 92,96 Q 100,91 108,96" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>

      <path v-else-if="emotion === 'ANGRY'"
        d="M 93,96 Q 100,91 107,96" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>

      <path v-else-if="emotion === 'ANXIOUS'"
        d="M 92,94 Q 96,91 98,94 Q 101,97 104,94 Q 107,91 108,94"
        stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>

      <path v-else-if="emotion === 'TIRED'"
        d="M 94,94 Q 100,97 106,94" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>

      <path v-else d="M 93,94 L 107,94" stroke="white" stroke-width="2" stroke-linecap="round"/>

      <!-- SAD teardrop -->
      <ellipse v-if="emotion === 'SAD'" cx="95" cy="89" rx="1" ry="2" fill="white" opacity="0.6"/>

      <!-- ── Hair front / bangs ── -->
      <template v-if="hair === 'short' || hair === 'medium' || hair === 'long'">
        <path d="M 80,86 Q 90,73 100,71 Q 110,73 120,86 Q 112,78 100,76 Q 88,78 80,86 Z"
          :fill="hairCol"/>
      </template>

      <!-- ── Accessories ── -->

      <!-- Glasses -->
      <template v-if="hasAcc('glasses')">
        <line x1="95"  y1="84" x2="105" y2="84" stroke="#1e293b" stroke-width="1.5"/>
        <rect x="84"   y="80"  width="11" height="8"  rx="2.5" fill="none" stroke="#1e293b" stroke-width="1.5"/>
        <rect x="105"  y="80"  width="11" height="8"  rx="2.5" fill="none" stroke="#1e293b" stroke-width="1.5"/>
        <line x1="84"  y1="84" x2="79"  y2="84" stroke="#1e293b" stroke-width="1.2"/>
        <line x1="116" y1="84" x2="121" y2="84" stroke="#1e293b" stroke-width="1.2"/>
      </template>

      <!-- Sunglasses -->
      <template v-else-if="hasAcc('sunglasses')">
        <line x1="95"  y1="84" x2="105" y2="84" stroke="rgba(0,0,0,0.8)" stroke-width="1.5"/>
        <rect x="84"   y="80"  width="11" height="8" rx="2.5" fill="rgba(0,0,0,0.55)" stroke="rgba(0,0,0,0.8)" stroke-width="1.5"/>
        <rect x="105"  y="80"  width="11" height="8" rx="2.5" fill="rgba(0,0,0,0.55)" stroke="rgba(0,0,0,0.8)" stroke-width="1.5"/>
        <line x1="84"  y1="84" x2="79"  y2="84" stroke="rgba(0,0,0,0.8)" stroke-width="1.2"/>
        <line x1="116" y1="84" x2="121" y2="84" stroke="rgba(0,0,0,0.8)" stroke-width="1.2"/>
      </template>

      <!-- Hat -->
      <template v-if="hasAcc('hat')">
        <ellipse cx="100" cy="68" rx="26" ry="6"   :fill="hairColDark"/>
        <path d="M 76,70 Q 74,55 100,52 Q 126,55 124,70 Z" :fill="hairCol"/>
        <ellipse cx="100" cy="68" rx="26" ry="5.5" :fill="hairCol"/>
      </template>

    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Emotion, AvatarConfig } from '../api/avatar'

const props = withDefaults(defineProps<{
  emotion?: Emotion | null
  config?:  AvatarConfig | null
  size?:    number
}>(), {
  emotion: 'NEUTRAL',
  config:  null,
  size:    160,
})

const uid = Math.random().toString(36).slice(2, 8)

// ── Emotion ───────────────────────────────────────────────────────────────────
const MOOD_COLORS: Record<string, string> = {
  HAPPY:   '#10b981', SAD:     '#3b82f6', ANGRY:   '#ef4444',
  NEUTRAL: '#64748b', EXCITED: '#f59e0b', TIRED:   '#8b5cf6',
  ANXIOUS: '#f97316', CONTENT: '#06b6d4',
}
const SKY_COLORS: Record<string, string> = {
  HAPPY:   '#87ceeb', SAD:     '#b0bec5', ANGRY:   '#ff8a65',
  NEUTRAL: '#7dd3fc', EXCITED: '#60a5fa', TIRED:   '#1e3a5f',
  ANXIOUS: '#90a4ae', CONTENT: '#bfdbfe',
}

const emotion   = computed(() => props.emotion ?? 'NEUTRAL')
const moodColor = computed(() => MOOD_COLORS[emotion.value] ?? '#64748b')
const skyColor  = computed(() => SKY_COLORS[emotion.value]  ?? '#7dd3fc')

// ── Config values (with defaults) ─────────────────────────────────────────────
const skin    = computed(() => props.config?.skinColor      ?? '#f0c98b')
const hairCol = computed(() => props.config?.secondaryColor ?? '#94a3b8')
const hair    = computed(() => props.config?.hairStyle      ?? 'short')
const clothes = computed(() => props.config?.clothesColor   ?? '#3b82f6')
const wall    = computed(() => props.config?.roomWallColor  ?? '#1e293b')
const floor   = computed(() => props.config?.roomFloorColor ?? '#0f172a')

// ── Derived colors ────────────────────────────────────────────────────────────
const skinLight  = computed(() => lighten(skin.value, 50))
const floorEdge  = computed(() => darken(floor.value, 15))
const pants      = computed(() => darken(clothes.value, 40))
const hairColDark = computed(() => darken(hairCol.value, 35))
const ceiling    = computed(() => lighten(wall.value, 18))
const wallLeft   = computed(() => darken(wall.value, 22))
const wallRight  = computed(() => darken(wall.value, 14))

function lighten(hex: string, amount: number) {
  const h = hex.replace('#', '')
  const r = Math.min(255, parseInt(h.slice(0,2),16) + amount)
  const g = Math.min(255, parseInt(h.slice(2,4),16) + amount)
  const b = Math.min(255, parseInt(h.slice(4,6),16) + amount)
  return `rgb(${r},${g},${b})`
}
function darken(hex: string, amount: number) {
  const h = hex.replace('#', '')
  const r = Math.max(0, parseInt(h.slice(0,2),16) - amount)
  const g = Math.max(0, parseInt(h.slice(2,4),16) - amount)
  const b = Math.max(0, parseInt(h.slice(4,6),16) - amount)
  return `rgb(${r},${g},${b})`
}

// ── Accessory / room item helpers ─────────────────────────────────────────────
const hasAcc      = (name: string) => props.config?.accessories?.includes(name) ?? false
const hasRoomItem = (name: string) => props.config?.roomItems?.includes(name)    ?? false

// ── Size threshold for detailed rendering ─────────────────────────────────────
const isDetailed = computed(() => props.size >= 90)

// ── Mood-based arm & leg poses ────────────────────────────────────────────────
interface Pose {
  armL: string; armR: string
  handL: [number, number]; handR: [number, number]
  legL: string; legR: string
  shoeL: [number, number]; shoeR: [number, number]
}

const POSES: Record<string, Pose> = {
  EXCITED: {
    armL: 'M 80,118 Q 68,102 62,84',  armR: 'M 120,118 Q 132,102 138,84',
    handL: [60, 81],                   handR: [140, 81],
    legL: 'M 88,150 L 85,172 L 80,193', legR: 'M 112,150 L 115,172 L 120,193',
    shoeL: [77, 196], shoeR: [123, 196],
  },
  SAD: {
    armL: 'M 80,120 Q 68,140 64,158',  armR: 'M 120,120 Q 132,140 136,158',
    handL: [62, 160],                   handR: [138, 160],
    legL: 'M 90,150 L 87,172 L 82,193', legR: 'M 110,150 L 113,172 L 118,193',
    shoeL: [79, 196], shoeR: [121, 196],
  },
  ANGRY: {
    armL: 'M 80,118 Q 74,128 76,140',  armR: 'M 120,118 Q 126,128 124,140',
    handL: [75, 142],                   handR: [125, 142],
    legL: 'M 88,150 L 84,172 L 80,193', legR: 'M 112,150 L 116,172 L 121,193',
    shoeL: [77, 196], shoeR: [123, 196],
  },
  ANXIOUS: {
    armL: 'M 80,118 Q 76,132 80,147',  armR: 'M 120,118 Q 124,132 120,147',
    handL: [80, 149],                   handR: [120, 149],
    legL: 'M 88,150 L 86,172 L 82,193', legR: 'M 112,150 L 114,172 L 118,193',
    shoeL: [79, 196], shoeR: [121, 196],
  },
  HAPPY: {
    armL: 'M 80,118 Q 65,126 57,144',  armR: 'M 120,118 Q 135,126 143,144',
    handL: [55, 146],                   handR: [145, 146],
    legL: 'M 88,150 L 85,172 L 80,193', legR: 'M 112,150 L 115,172 L 120,193',
    shoeL: [77, 196], shoeR: [123, 196],
  },
  TIRED: {
    armL: 'M 80,120 Q 70,138 66,156',  armR: 'M 120,120 Q 128,137 130,152',
    handL: [64, 158],                   handR: [131, 154],
    legL: 'M 90,150 L 87,172 L 82,193', legR: 'M 110,150 L 113,172 L 118,193',
    shoeL: [79, 196], shoeR: [121, 196],
  },
  CONTENT: {
    armL: 'M 80,118 Q 65,130 59,148',  armR: 'M 120,118 Q 135,130 141,148',
    handL: [57, 150],                   handR: [143, 150],
    legL: 'M 88,150 L 85,172 L 80,193', legR: 'M 112,150 L 115,172 L 120,193',
    shoeL: [77, 196], shoeR: [123, 196],
  },
  NEUTRAL: {
    armL: 'M 80,118 Q 67,134 61,150',  armR: 'M 120,118 Q 133,134 139,150',
    handL: [59, 152],                   handR: [141, 152],
    legL: 'M 88,150 L 85,172 L 80,193', legR: 'M 112,150 L 115,172 L 120,193',
    shoeL: [77, 196], shoeR: [123, 196],
  },
}

const pose = computed(() => POSES[emotion.value] ?? POSES.NEUTRAL)
</script>

<style scoped>
.aura {
  animation: pulse 3s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 0.7; }
  50%       { opacity: 1;   }
}
</style>
