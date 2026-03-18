<template>
  <div class="room-wrap" :class="{ 'fill-height': fillHeight }">
    <svg viewBox="0 0 900 380" class="room-svg" :preserveAspectRatio="fillHeight ? 'xMidYMid slice' : 'xMidYMid meet'" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <radialGradient :id="`skin-${uid}`" cx="35%" cy="28%" r="70%">
          <stop offset="0%" :stop-color="skinLight"/>
          <stop offset="100%" :stop-color="skin"/>
        </radialGradient>
        <radialGradient :id="`aura-${uid}`" cx="50%" cy="55%" r="55%">
          <stop offset="0%"   :stop-color="moodColor" stop-opacity="0.3"/>
          <stop offset="100%" :stop-color="moodColor" stop-opacity="0"/>
        </radialGradient>
        <clipPath :id="`win-${uid}`">
          <rect x="610" y="48" width="140" height="98" rx="4"/>
        </clipPath>
        <clipPath :id="`floor-${uid}`">
          <polygon points="0,320 140,250 760,250 900,320 900,380 0,380"/>
        </clipPath>
      </defs>

      <!-- ══ ROOM ══════════════════════════════════════════════════════════════ -->

      <polygon points="0,0 900,0 760,32 140,32" :fill="ceiling"/>
      <rect x="140" y="32" width="620" height="218" :fill="wall"/>
      <polygon points="0,0 140,32 140,250 0,320"      :fill="wallLeft"/>
      <polygon points="760,32 900,0 900,320 760,250"  :fill="wallRight"/>
      <polygon points="0,320 140,250 760,250 900,320 900,380 0,380" :fill="floor"/>

      <!-- Corner lines -->
      <line x1="140" y1="32"  x2="140" y2="250" stroke="rgba(0,0,0,0.3)"  stroke-width="1.5"/>
      <line x1="760" y1="32"  x2="760" y2="250" stroke="rgba(0,0,0,0.3)"  stroke-width="1.5"/>
      <line x1="0"   y1="0"   x2="140" y2="32"  stroke="rgba(0,0,0,0.18)" stroke-width="1"/>
      <line x1="760" y1="32"  x2="900" y2="0"   stroke="rgba(0,0,0,0.18)" stroke-width="1"/>
      <line x1="0"   y1="320" x2="140" y2="250" stroke="rgba(0,0,0,0.15)" stroke-width="1"/>
      <line x1="760" y1="250" x2="900" y2="320" stroke="rgba(0,0,0,0.15)" stroke-width="1"/>

      <!-- Ambient occlusion -->
      <rect x="140" y="32" width="5"   height="218" fill="rgba(0,0,0,0.1)"/>
      <rect x="755" y="32" width="5"   height="218" fill="rgba(0,0,0,0.1)"/>
      <rect x="140" y="245" width="620" height="5"  fill="rgba(0,0,0,0.12)"/>

      <!-- Baseboard -->
      <rect x="140" y="246" width="620" height="4" :fill="floorEdge"/>
      <polygon points="1,318 4,317 144,246 140,246 140,250 0,320" :fill="floorEdge" opacity="0.6"/>
      <polygon points="756,246 760,246 760,250 900,320 896,321 756,250" :fill="floorEdge" opacity="0.6"/>

      <!-- Floor perspective lines -->
      <g :clip-path="`url(#floor-${uid})`">
        <line x1="450" y1="250" x2="0"   y2="380" stroke="rgba(0,0,0,0.07)" stroke-width="1"/>
        <line x1="450" y1="250" x2="150" y2="380" stroke="rgba(0,0,0,0.05)" stroke-width="0.7"/>
        <line x1="450" y1="250" x2="300" y2="380" stroke="rgba(0,0,0,0.05)" stroke-width="0.7"/>
        <line x1="450" y1="250" x2="450" y2="380" stroke="rgba(0,0,0,0.05)" stroke-width="0.7"/>
        <line x1="450" y1="250" x2="600" y2="380" stroke="rgba(0,0,0,0.05)" stroke-width="0.7"/>
        <line x1="450" y1="250" x2="750" y2="380" stroke="rgba(0,0,0,0.05)" stroke-width="0.7"/>
        <line x1="450" y1="250" x2="900" y2="380" stroke="rgba(0,0,0,0.07)" stroke-width="1"/>
        <line x1="0" y1="295" x2="900" y2="295" stroke="rgba(0,0,0,0.04)" stroke-width="0.6"/>
        <line x1="0" y1="322" x2="900" y2="322" stroke="rgba(0,0,0,0.04)" stroke-width="0.6"/>
        <line x1="0" y1="350" x2="900" y2="350" stroke="rgba(0,0,0,0.04)" stroke-width="0.6"/>
        <line x1="0" y1="370" x2="900" y2="370" stroke="rgba(0,0,0,0.04)" stroke-width="0.6"/>
      </g>

      <!-- ══ ROOM ITEMS ══════════════════════════════════════════════════════ -->

      <!-- Window (back wall right) -->
      <rect x="610" y="48" width="140" height="98" rx="4" :fill="skyColor"/>
      <g :clip-path="`url(#win-${uid})`">
        <template v-if="emotion === 'HAPPY' || emotion === 'CONTENT'">
          <circle cx="720" cy="82" r="18" fill="#fbbf24"/>
          <line v-for="a in [0,45,90,135,180,225,270,315]" :key="a"
            :x1="720 + 24*Math.cos(a*Math.PI/180)" :y1="82 + 24*Math.sin(a*Math.PI/180)"
            :x2="720 + 30*Math.cos(a*Math.PI/180)" :y2="82 + 30*Math.sin(a*Math.PI/180)"
            stroke="#fbbf24" stroke-width="3" stroke-linecap="round"/>
        </template>
        <template v-if="emotion === 'TIRED'">
          <path d="M 708,58 Q 728,82 708,106 Q 688,82 708,58 Z" fill="#e2e8f0"/>
          <circle cx="636" cy="66" r="2.5" fill="white" opacity="0.8"/>
          <circle cx="652" cy="92" r="3"   fill="white" opacity="0.6"/>
          <circle cx="630" cy="88" r="2"   fill="white" opacity="0.5"/>
        </template>
        <template v-if="emotion === 'SAD'">
          <ellipse cx="656" cy="70" rx="22" ry="12" fill="#94a3b8" opacity="0.8"/>
          <ellipse cx="692" cy="65" rx="18" ry="10" fill="#94a3b8" opacity="0.9"/>
          <line x1="645" y1="86" x2="641" y2="112" stroke="#64748b" stroke-width="2.5" stroke-linecap="round"/>
          <line x1="662" y1="86" x2="658" y2="112" stroke="#64748b" stroke-width="2.5" stroke-linecap="round"/>
          <line x1="679" y1="86" x2="675" y2="112" stroke="#64748b" stroke-width="2.5" stroke-linecap="round"/>
        </template>
        <template v-if="emotion === 'ANGRY'">
          <circle cx="678" cy="78" r="30" fill="#ff7043" opacity="0.5"/>
          <path d="M 672,55 L 660,75 L 674,75 L 660,102 L 696,70 L 680,70 Z" fill="#fbbf24" opacity="0.9"/>
        </template>
        <template v-if="emotion === 'ANXIOUS'">
          <ellipse cx="648" cy="72" rx="20" ry="12" fill="#78716c" opacity="0.8"/>
          <ellipse cx="688" cy="68" rx="18" ry="10" fill="#57534e" opacity="0.8"/>
          <line x1="655" y1="88" x2="651" y2="114" stroke="#475569" stroke-width="2.5" stroke-linecap="round"/>
          <line x1="676" y1="90" x2="674" y2="116" stroke="#475569" stroke-width="2.5" stroke-linecap="round"/>
        </template>
        <template v-if="emotion === 'EXCITED'">
          <circle cx="640" cy="62" r="4.5" fill="white" opacity="0.9"/>
          <circle cx="663" cy="86" r="3.5" fill="white" opacity="0.7"/>
          <circle cx="692" cy="58" r="4"   fill="white" opacity="0.8"/>
          <circle cx="717" cy="79" r="4.5" fill="white" opacity="0.9"/>
        </template>
      </g>
      <rect x="610" y="48" width="140" height="98" rx="4" fill="none" stroke="#475569" stroke-width="3"/>
      <line x1="680" y1="48"  x2="680" y2="146" stroke="#475569" stroke-width="2"/>
      <line x1="610" y1="97"  x2="750" y2="97"  stroke="#475569" stroke-width="2"/>
      <rect x="606" y="143"  width="148" height="7" rx="2" fill="#334155"/>

      <!-- Plant (back-left corner) -->
      <template v-if="hasRoomItem('plant')">
        <rect x="144" y="232" width="22" height="20" rx="3" fill="#92400e"/>
        <rect x="141" y="228" width="28" height="8"  rx="3" fill="#78350f"/>
        <ellipse cx="156" cy="208" rx="10" ry="16" fill="#16a34a" transform="rotate(-20 156 208)"/>
        <ellipse cx="170" cy="204" rx="9"  ry="14" fill="#22c55e" transform="rotate(15 170 204)"/>
        <ellipse cx="143" cy="206" rx="8"  ry="13" fill="#15803d" transform="rotate(-35 143 206)"/>
      </template>

      <!-- Bookshelf (right wall) -->
      <template v-if="hasRoomItem('bookshelf')">
        <rect x="762" y="78"  width="52" height="172" rx="3" fill="#92400e"/>
        <rect x="762" y="108" width="52" height="4"        fill="#78350f"/>
        <rect x="762" y="140" width="52" height="4"        fill="#78350f"/>
        <rect x="762" y="172" width="52" height="4"        fill="#78350f"/>
        <rect x="762" y="204" width="52" height="4"        fill="#78350f"/>
        <rect x="764" y="80"  width="9"  height="28" fill="#3b82f6"/>
        <rect x="774" y="83"  width="7"  height="25" fill="#ef4444"/>
        <rect x="782" y="80"  width="9"  height="28" fill="#10b981"/>
        <rect x="792" y="84"  width="7"  height="24" fill="#f59e0b"/>
        <rect x="800" y="82"  width="8"  height="26" fill="#8b5cf6"/>
        <rect x="764" y="112" width="8"  height="28" fill="#ec4899"/>
        <rect x="773" y="114" width="9"  height="26" fill="#06b6d4"/>
        <rect x="783" y="112" width="7"  height="28" fill="#f59e0b"/>
        <rect x="791" y="115" width="9"  height="25" fill="#3b82f6"/>
        <rect x="764" y="144" width="9"  height="28" fill="#ef4444"/>
        <rect x="774" y="146" width="7"  height="26" fill="#8b5cf6"/>
        <rect x="782" y="144" width="9"  height="28" fill="#10b981"/>
        <rect x="792" y="147" width="9"  height="25" fill="#f59e0b"/>
        <rect x="764" y="176" width="7"  height="28" fill="#3b82f6"/>
        <rect x="772" y="178" width="9"  height="26" fill="#ec4899"/>
        <rect x="782" y="176" width="7"  height="28" fill="#ef4444"/>
        <rect x="790" y="179" width="9"  height="25" fill="#06b6d4"/>
      </template>

      <!-- Lamp (left side, only if no plant) -->
      <template v-if="hasRoomItem('lamp') && !hasRoomItem('plant')">
        <rect x="158" y="148" width="6" height="104" rx="3" fill="#94a3b8"/>
        <ellipse cx="161" cy="252" rx="18" ry="6" fill="#94a3b8"/>
        <path d="M 138,148 Q 161,130 184,148 L 180,168 Q 161,156 142,168 Z" fill="#fbbf24"/>
        <ellipse cx="161" cy="158" rx="26" ry="14" fill="#fbbf24" opacity="0.1"/>
      </template>

      <!-- Rug -->
      <template v-if="hasRoomItem('rug')">
        <ellipse cx="450" cy="346" rx="130" ry="26" :fill="moodColor" opacity="0.18"/>
        <ellipse cx="450" cy="346" rx="130" ry="26" fill="none" :stroke="moodColor" stroke-width="2" opacity="0.3"/>
        <ellipse cx="450" cy="346" rx="100" ry="20" fill="none" :stroke="moodColor" stroke-width="1.2" opacity="0.18"/>
      </template>

      <!-- ══ VISITORS ════════════════════════════════════════════════════════ -->

      <template v-for="(v, i) in visitorSlots" :key="v.userId">
        <!-- Visitor shadow -->
        <ellipse :cx="VISITOR_POSITIONS[i]" cy="300" rx="18" ry="4.5" fill="rgba(0,0,0,0.18)"/>
        <!-- Visitor character (scale 0.62, ty 164) -->
        <g :transform="`translate(${(VISITOR_POSITIONS[i] ?? 0) - 62}, 164) scale(0.62, 0.62)`">
          <!-- Arms -->
          <path d="M 80,118 Q 67,134 61,150" :stroke="v.clothesColor ?? '#3b82f6'" stroke-width="11" fill="none" stroke-linecap="round"/>
          <path d="M 120,118 Q 133,134 139,150" :stroke="v.clothesColor ?? '#3b82f6'" stroke-width="11" fill="none" stroke-linecap="round"/>
          <!-- Legs -->
          <path d="M 88,150 L 85,172 L 80,193" stroke="#334155" stroke-width="12" fill="none" stroke-linecap="round"/>
          <path d="M 112,150 L 115,172 L 120,193" stroke="#334155" stroke-width="12" fill="none" stroke-linecap="round"/>
          <ellipse cx="77"  cy="196" rx="13" ry="5" fill="#1e293b"/>
          <ellipse cx="123" cy="196" rx="13" ry="5" fill="#1e293b"/>
          <!-- Torso -->
          <path d="M 80,114 Q 100,111 120,114 L 122,150 Q 100,154 78,150 Z" :fill="v.clothesColor ?? '#3b82f6'"/>
          <rect x="94" y="107" width="12" height="9" rx="3" :fill="v.skinColor ?? '#f0c98b'"/>
          <!-- Head -->
          <circle cx="100" cy="87" r="22" :fill="v.skinColor ?? '#f0c98b'"/>
          <!-- Hair -->
          <template v-if="!v.hairStyle || v.hairStyle === 'short'">
            <ellipse cx="100" cy="72" rx="22" ry="10" :fill="v.hairColor ?? '#94a3b8'"/>
          </template>
          <template v-else-if="v.hairStyle === 'medium'">
            <ellipse cx="100" cy="72" rx="22" ry="11" :fill="v.hairColor ?? '#94a3b8'"/>
            <path d="M 78,80 Q 73,98 74,112" :stroke="v.hairColor ?? '#94a3b8'" stroke-width="9" fill="none" stroke-linecap="round"/>
            <path d="M 122,80 Q 127,98 126,112" :stroke="v.hairColor ?? '#94a3b8'" stroke-width="9" fill="none" stroke-linecap="round"/>
          </template>
          <template v-else-if="v.hairStyle === 'long'">
            <ellipse cx="100" cy="72" rx="22" ry="11" :fill="v.hairColor ?? '#94a3b8'"/>
            <path d="M 78,80 Q 70,110 72,140" :stroke="v.hairColor ?? '#94a3b8'" stroke-width="10" fill="none" stroke-linecap="round"/>
            <path d="M 122,80 Q 130,110 128,140" :stroke="v.hairColor ?? '#94a3b8'" stroke-width="10" fill="none" stroke-linecap="round"/>
          </template>
          <template v-else-if="v.hairStyle === 'curly'">
            <circle cx="100" cy="67" r="11" :fill="v.hairColor ?? '#94a3b8'"/>
            <circle cx="86"  cy="72" r="10" :fill="v.hairColor ?? '#94a3b8'"/>
            <circle cx="114" cy="72" r="10" :fill="v.hairColor ?? '#94a3b8'"/>
            <circle cx="93"  cy="65" r="8"  :fill="v.hairColor ?? '#94a3b8'"/>
            <circle cx="107" cy="65" r="8"  :fill="v.hairColor ?? '#94a3b8'"/>
          </template>
          <template v-else-if="v.hairStyle === 'spiky'">
            <ellipse cx="100" cy="74" rx="21" ry="9" :fill="v.hairColor ?? '#94a3b8'"/>
            <polygon points="100,58 103,68 97,68" :fill="v.hairColor ?? '#94a3b8'"/>
            <polygon points="111,60 113,71 107,70" :fill="v.hairColor ?? '#94a3b8'"/>
            <polygon points="89,60 93,70 87,71" :fill="v.hairColor ?? '#94a3b8'"/>
          </template>
          <!-- Eyes — expression based on emotion -->
          <template v-if="v.emotion === 'HAPPY' || v.emotion === 'EXCITED' || v.emotion === 'CONTENT'">
            <path d="M 90,82 Q 93,79 96,82" stroke="#0f172a" stroke-width="2" fill="none" stroke-linecap="round"/>
            <path d="M 104,82 Q 107,79 110,82" stroke="#0f172a" stroke-width="2" fill="none" stroke-linecap="round"/>
            <path d="M 93,93 Q 100,99 107,93" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
          </template>
          <template v-else-if="v.emotion === 'SAD' || v.emotion === 'TIRED'">
            <circle cx="93"  cy="84" r="2.5" fill="white" opacity="0.9"/>
            <circle cx="107" cy="84" r="2.5" fill="white" opacity="0.9"/>
            <circle cx="93"  cy="84" r="1.5" fill="#0f172a"/>
            <circle cx="107" cy="84" r="1.5" fill="#0f172a"/>
            <path d="M 93,96 Q 100,93 107,96" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
          </template>
          <template v-else-if="v.emotion === 'ANGRY'">
            <path d="M 90,80 Q 93,83 96,80" stroke="#0f172a" stroke-width="2.5" fill="none" stroke-linecap="round"/>
            <path d="M 104,80 Q 107,83 110,80" stroke="#0f172a" stroke-width="2.5" fill="none" stroke-linecap="round"/>
            <circle cx="93"  cy="84" r="2" fill="#0f172a"/>
            <circle cx="107" cy="84" r="2" fill="#0f172a"/>
            <path d="M 93,96 Q 100,93 107,96" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
          </template>
          <template v-else>
            <circle cx="93"  cy="83" r="2.5" fill="white" opacity="0.9"/>
            <circle cx="107" cy="83" r="2.5" fill="white" opacity="0.9"/>
            <circle cx="93"  cy="83" r="1.5" fill="#0f172a"/>
            <circle cx="107" cy="83" r="1.5" fill="#0f172a"/>
            <path d="M 93,94 Q 100,99 107,94" stroke="white" stroke-width="2" fill="none" stroke-linecap="round"/>
          </template>
        </g>
        <!-- Username label -->
        <text
          :x="VISITOR_POSITIONS[i]" y="318"
          text-anchor="middle" font-size="13" fill="#94a3b8"
          font-family="sans-serif" font-weight="600"
        >{{ v.username }}</text>
      </template>

      <!-- Overflow indicator -->
      <text v-if="visitors.length > 3" x="470" y="318" font-size="13" fill="#64748b" font-family="sans-serif">+{{ visitors.length - 3 }}</text>

      <!-- ══ AVATAR ══════════════════════════════════════════════════════════ -->

      <!-- Floor shadow -->
      <ellipse
        :cx="displayX" cy="358" rx="28" ry="7"
        fill="rgba(0,0,0,0.2)"
      />

      <!-- Character group — 200×200 coordinate space, scaled + translated -->
      <g :transform="avatarTransform">

        <!-- Aura -->
        <ellipse cx="100" cy="130" rx="70" ry="80" :fill="`url(#aura-${uid})`" class="aura"/>

        <!-- Arms behind torso -->
        <path :d="pose.armL" :stroke="clothes" stroke-width="11" fill="none" stroke-linecap="round"/>
        <path :d="pose.armR" :stroke="clothes" stroke-width="11" fill="none" stroke-linecap="round"/>
        <circle :cx="pose.handL[0]" :cy="pose.handL[1]" r="5.5" :fill="skin"/>
        <circle :cx="pose.handR[0]" :cy="pose.handR[1]" r="5.5" :fill="skin"/>

        <!-- Legs -->
        <path :d="pose.legL" :stroke="pants" stroke-width="12" fill="none" stroke-linecap="round"/>
        <path :d="pose.legR" :stroke="pants" stroke-width="12" fill="none" stroke-linecap="round"/>
        <ellipse :cx="pose.shoeL[0]" :cy="pose.shoeL[1]" rx="13" ry="5" fill="#1e293b"/>
        <ellipse :cx="pose.shoeR[0]" :cy="pose.shoeR[1]" rx="13" ry="5" fill="#1e293b"/>

        <!-- Torso -->
        <path d="M 80,114 Q 100,111 120,114 L 122,150 Q 100,154 78,150 Z" :fill="clothes"/>
        <rect x="94" y="107" width="12" height="9" rx="3" :fill="skin"/>

        <!-- Hair back -->
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

        <!-- Head -->
        <circle cx="100" cy="87" r="22" :fill="`url(#skin-${uid})`"/>
        <circle cx="100" cy="87" r="22" fill="none" :stroke="moodColor" stroke-width="1.5" opacity="0.4"/>

        <!-- Eyes -->
        <template v-if="emotion === 'HAPPY' || emotion === 'CONTENT'">
          <path d="M 90,83 Q 93,79 96,83" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>
          <path d="M 104,83 Q 107,79 110,83" stroke="white" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        </template>
        <template v-else-if="emotion === 'EXCITED'">
          <circle cx="93"  cy="82" r="2.5" fill="white" opacity="0.95"/>
          <circle cx="93"  cy="82" r="1.5" fill="#f59e0b"/>
          <circle cx="107" cy="82" r="2.5" fill="white" opacity="0.95"/>
          <circle cx="107" cy="82" r="1.5" fill="#f59e0b"/>
          <circle cx="90"  cy="79" r="1"   fill="white" opacity="0.8"/>
          <circle cx="110" cy="79" r="1"   fill="white" opacity="0.8"/>
        </template>
        <template v-else-if="emotion === 'TIRED'">
          <ellipse cx="93"  cy="83" rx="3" ry="2"   fill="white" opacity="0.9"/>
          <ellipse cx="93"  cy="83" rx="3" ry="1.2" fill="rgba(0,0,0,0.18)"/>
          <ellipse cx="107" cy="83" rx="3" ry="2"   fill="white" opacity="0.9"/>
          <ellipse cx="107" cy="83" rx="3" ry="1.2" fill="rgba(0,0,0,0.18)"/>
          <text x="116" y="73" fill="white" font-size="5" opacity="0.8" font-family="sans-serif">z</text>
          <text x="120" y="69" fill="white" font-size="4" opacity="0.6" font-family="sans-serif">z</text>
        </template>
        <template v-else-if="emotion === 'ANGRY'">
          <path d="M 89,82 L 97,84" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <path d="M 111,82 L 103,84" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <ellipse cx="93"  cy="84" rx="3" ry="2" fill="white" opacity="0.9"/>
          <ellipse cx="107" cy="84" rx="3" ry="2" fill="white" opacity="0.9"/>
          <circle  cx="93"  cy="84" r="1.5" fill="#ef4444"/>
          <circle  cx="107" cy="84" r="1.5" fill="#ef4444"/>
        </template>
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
        <template v-else>
          <circle cx="93"  cy="83" r="3"   fill="white" opacity="0.9"/>
          <circle cx="107" cy="83" r="3"   fill="white" opacity="0.9"/>
          <circle cx="93"  cy="83" r="1.8" fill="#0f172a"/>
          <circle cx="107" cy="83" r="1.8" fill="#0f172a"/>
          <circle cx="94"  cy="82" r="0.8" fill="white"/>
          <circle cx="108" cy="82" r="0.8" fill="white"/>
        </template>

        <!-- Mouth -->
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
        <ellipse v-if="emotion === 'SAD'" cx="95" cy="89" rx="1" ry="2" fill="white" opacity="0.6"/>

        <!-- Hair front -->
        <template v-if="hair === 'short' || hair === 'medium' || hair === 'long'">
          <path d="M 80,86 Q 90,73 100,71 Q 110,73 120,86 Q 112,78 100,76 Q 88,78 80,86 Z" :fill="hairCol"/>
        </template>

        <!-- Glasses -->
        <template v-if="hasAcc('glasses')">
          <line x1="95"  y1="84" x2="105" y2="84" stroke="#1e293b" stroke-width="1.5"/>
          <rect x="84" y="80"  width="11" height="8" rx="2.5" fill="none" stroke="#1e293b" stroke-width="1.5"/>
          <rect x="105" y="80" width="11" height="8" rx="2.5" fill="none" stroke="#1e293b" stroke-width="1.5"/>
          <line x1="84"  y1="84" x2="79"  y2="84" stroke="#1e293b" stroke-width="1.2"/>
          <line x1="116" y1="84" x2="121" y2="84" stroke="#1e293b" stroke-width="1.2"/>
        </template>
        <template v-else-if="hasAcc('sunglasses')">
          <line x1="95"  y1="84" x2="105" y2="84" stroke="rgba(0,0,0,0.8)" stroke-width="1.5"/>
          <rect x="84" y="80"  width="11" height="8" rx="2.5" fill="rgba(0,0,0,0.55)" stroke="rgba(0,0,0,0.8)" stroke-width="1.5"/>
          <rect x="105" y="80" width="11" height="8" rx="2.5" fill="rgba(0,0,0,0.55)" stroke="rgba(0,0,0,0.8)" stroke-width="1.5"/>
          <line x1="84"  y1="84" x2="79"  y2="84" stroke="rgba(0,0,0,0.8)" stroke-width="1.2"/>
          <line x1="116" y1="84" x2="121" y2="84" stroke="rgba(0,0,0,0.8)" stroke-width="1.2"/>
        </template>
        <template v-if="hasAcc('hat')">
          <ellipse cx="100" cy="68" rx="26" ry="6"   :fill="hairColDark"/>
          <path d="M 76,70 Q 74,55 100,52 Q 126,55 124,70 Z" :fill="hairCol"/>
          <ellipse cx="100" cy="68" rx="26" ry="5.5" :fill="hairCol"/>
        </template>

      </g>
    </svg>

    <!-- Activity label -->
    <div class="activity-label">
      <span class="dot" :style="{ background: moodColor }"/>
      {{ activityLabel }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { Emotion, AvatarConfig } from '../api/avatar'
import type { RoomVisitorInfo } from '../stores/realtime'

const props = withDefaults(defineProps<{
  emotion?:    Emotion | null
  config?:     AvatarConfig | null
  visitors?:   RoomVisitorInfo[]
  fillHeight?: boolean
  needs?:      { mood: number; energy: number; social: number; activity: number } | null
}>(), {
  emotion:    'NEUTRAL',
  config:     null,
  visitors:   () => [],
  fillHeight: false,
  needs:      null,
})

const visitorSlots   = computed(() => props.visitors.slice(0, 3))
const VISITOR_POSITIONS = [240, 320, 400] as const

const uid = Math.random().toString(36).slice(2, 8)

// ── Colours ───────────────────────────────────────────────────────────────────
const MOOD_COLORS: Record<string, string> = {
  HAPPY:'#10b981', SAD:'#3b82f6', ANGRY:'#ef4444', NEUTRAL:'#64748b',
  EXCITED:'#f59e0b', TIRED:'#8b5cf6', ANXIOUS:'#f97316', CONTENT:'#06b6d4',
}
const SKY_COLORS: Record<string, string> = {
  HAPPY:'#87ceeb', SAD:'#b0bec5', ANGRY:'#ff8a65', NEUTRAL:'#7dd3fc',
  EXCITED:'#60a5fa', TIRED:'#1e3a5f', ANXIOUS:'#90a4ae', CONTENT:'#bfdbfe',
}

const emotion   = computed(() => props.emotion  ?? 'NEUTRAL')
const moodColor = computed(() => MOOD_COLORS[emotion.value] ?? '#64748b')
const skyColor  = computed(() => SKY_COLORS[emotion.value]  ?? '#7dd3fc')

const skin     = computed(() => props.config?.skinColor      ?? '#f0c98b')
const hairCol  = computed(() => props.config?.secondaryColor ?? '#94a3b8')
const hair     = computed(() => props.config?.hairStyle      ?? 'short')
const clothes  = computed(() => props.config?.clothesColor   ?? '#3b82f6')
const wall     = computed(() => props.config?.roomWallColor  ?? '#1e293b')
const floor    = computed(() => props.config?.roomFloorColor ?? '#0f172a')

const skinLight   = computed(() => lighten(skin.value, 50))
const floorEdge   = computed(() => darken(floor.value, 15))
const pants       = computed(() => darken(clothes.value, 40))
const hairColDark = computed(() => darken(hairCol.value, 35))
const ceiling     = computed(() => lighten(wall.value, 18))
const wallLeft    = computed(() => darken(wall.value, 22))
const wallRight   = computed(() => darken(wall.value, 14))

function lighten(hex: string, a: number) {
  const h = hex.replace('#','')
  return `rgb(${Math.min(255,parseInt(h.slice(0,2),16)+a)},${Math.min(255,parseInt(h.slice(2,4),16)+a)},${Math.min(255,parseInt(h.slice(4,6),16)+a)})`
}
function darken(hex: string, a: number) {
  const h = hex.replace('#','')
  return `rgb(${Math.max(0,parseInt(h.slice(0,2),16)-a)},${Math.max(0,parseInt(h.slice(2,4),16)-a)},${Math.max(0,parseInt(h.slice(4,6),16)-a)})`
}

const hasAcc      = (n: string) => props.config?.accessories?.includes(n) ?? false
const hasRoomItem = (n: string) => props.config?.roomItems?.includes(n)    ?? false

// ── Poses ─────────────────────────────────────────────────────────────────────
interface Pose {
  armL:string; armR:string
  handL:[number,number]; handR:[number,number]
  legL:string; legR:string
  shoeL:[number,number]; shoeR:[number,number]
}

const EMOTION_POSES: Record<string, Pose> = {
  EXCITED: { armL:'M 80,118 Q 68,102 62,84',  armR:'M 120,118 Q 132,102 138,84',  handL:[60,81],   handR:[140,81],  legL:'M 88,150 L 85,172 L 80,193', legR:'M 112,150 L 115,172 L 120,193', shoeL:[77,196], shoeR:[123,196] },
  SAD:     { armL:'M 80,120 Q 68,140 64,158',  armR:'M 120,120 Q 132,140 136,158', handL:[62,160],  handR:[138,160], legL:'M 90,150 L 87,172 L 82,193', legR:'M 110,150 L 113,172 L 118,193', shoeL:[79,196], shoeR:[121,196] },
  ANGRY:   { armL:'M 80,118 Q 74,128 76,140',  armR:'M 120,118 Q 126,128 124,140', handL:[75,142],  handR:[125,142], legL:'M 88,150 L 84,172 L 80,193', legR:'M 112,150 L 116,172 L 121,193', shoeL:[77,196], shoeR:[123,196] },
  ANXIOUS: { armL:'M 80,118 Q 76,132 80,147',  armR:'M 120,118 Q 124,132 120,147', handL:[80,149],  handR:[120,149], legL:'M 88,150 L 86,172 L 82,193', legR:'M 112,150 L 114,172 L 118,193', shoeL:[79,196], shoeR:[121,196] },
  HAPPY:   { armL:'M 80,118 Q 65,126 57,144',  armR:'M 120,118 Q 135,126 143,144', handL:[55,146],  handR:[145,146], legL:'M 88,150 L 85,172 L 80,193', legR:'M 112,150 L 115,172 L 120,193', shoeL:[77,196], shoeR:[123,196] },
  TIRED:   { armL:'M 80,120 Q 70,138 66,156',  armR:'M 120,120 Q 128,137 130,152', handL:[64,158],  handR:[131,154], legL:'M 90,150 L 87,172 L 82,193', legR:'M 110,150 L 113,172 L 118,193', shoeL:[79,196], shoeR:[121,196] },
  CONTENT: { armL:'M 80,118 Q 65,130 59,148',  armR:'M 120,118 Q 135,130 141,148', handL:[57,150],  handR:[143,150], legL:'M 88,150 L 85,172 L 80,193', legR:'M 112,150 L 115,172 L 120,193', shoeL:[77,196], shoeR:[123,196] },
  NEUTRAL: { armL:'M 80,118 Q 67,134 61,150',  armR:'M 120,118 Q 133,134 139,150', handL:[59,152],  handR:[141,152], legL:'M 88,150 L 85,172 L 80,193', legR:'M 112,150 L 115,172 L 120,193', shoeL:[77,196], shoeR:[123,196] },
}

// Two alternating walk steps
const WALK_POSES: [Pose, Pose] = [
  { armL:'M 80,118 Q 68,126 62,144', armR:'M 120,118 Q 130,128 132,146', handL:[60,146], handR:[134,148], legL:'M 88,150 L 79,172 L 73,193', legR:'M 112,150 L 118,170 L 124,190', shoeL:[70,196], shoeR:[127,193] },
  { armL:'M 80,118 Q 73,128 73,146', armR:'M 120,118 Q 132,126 138,144', handL:[71,148], handR:[140,146], legL:'M 88,150 L 87,172 L 87,192', legR:'M 112,150 L 120,172 L 127,192', shoeL:[84,195], shoeR:[130,195] },
]

// Sitting pose override
const SITTING_POSE: Partial<Pose> = {
  armL:'M 80,118 Q 74,138 78,155', armR:'M 120,118 Q 126,138 122,155',
  handL:[77,157], handR:[123,157],
  legL:'M 90,152 Q 80,158 70,160', legR:'M 110,152 Q 120,158 130,160',
  shoeL:[67,163], shoeR:[133,163],
}

// ── Room spots ────────────────────────────────────────────────────────────────
type SpotName = 'window'|'bookshelf'|'plant'|'rug'|'left'|'leftmid'|'center'|'rightmid'|'right'
type Activity  = 'idle'|'sitting'

const SPOTS: Record<SpotName, { x: number; activity: Activity }> = {
  window:    { x: 695, activity: 'idle' },
  bookshelf: { x: 780, activity: 'idle' },
  plant:     { x: 200, activity: 'idle' },
  rug:       { x: 450, activity: 'sitting' },
  left:      { x: 240, activity: 'idle' },
  leftmid:   { x: 330, activity: 'idle' },
  center:    { x: 450, activity: 'idle' },
  rightmid:  { x: 570, activity: 'idle' },
  right:     { x: 660, activity: 'idle' },
}

const MOOD_SPOTS: Record<string, SpotName[]> = {
  HAPPY:   ['center','right','leftmid','rightmid','window','center','right','leftmid'],
  SAD:     ['window','window','rug','center','plant','left','window'],
  TIRED:   ['rug','rug','rug','center','leftmid','rug'],
  ANGRY:   ['left','right','left','right','leftmid','rightmid','left'],
  ANXIOUS: ['left','right','leftmid','rightmid','center','left','right'],
  CONTENT: ['rug','center','window','bookshelf','rightmid','rug'],
  EXCITED: ['left','right','center','leftmid','rightmid','left','right','center'],
  NEUTRAL: ['center','left','right','leftmid','rightmid','window','rug'],
}

// Walk speed in SVG units/frame — mood dependent
const WALK_SPEEDS: Record<string, number> = {
  EXCITED:2.8, HAPPY:2.4, ANGRY:2.8, ANXIOUS:3.2,
  CONTENT:1.6, NEUTRAL:2.0, SAD:1.4, TIRED:1.0,
}

const effectiveSpeed = computed(() => {
  const base = WALK_SPEEDS[emotion.value] ?? 2.0
  if (props.needs && props.needs.energy < 30) return base * 0.5
  return base
})

// ── State ─────────────────────────────────────────────────────────────────────
const displayX   = ref(450)      // current animated X (SVG units)
const displayY   = ref(188)      // current animated Y offset (for sitting)
const targetX    = ref(450)
const activity   = ref<Activity>('idle')
const walkStep   = ref(0)
const isMoving   = ref(false)
const currentSpot = ref<SpotName>('center')

let rafId:      number | null = null
let walkStepId: ReturnType<typeof setInterval> | null = null
let behaviorId: ReturnType<typeof setTimeout>  | null = null

// ── Activity label ────────────────────────────────────────────────────────────
const SPOT_LABELS: Record<SpotName, string> = {
  window:    'schaut gedankenverloren aus dem Fenster',
  bookshelf: 'stöbert im Regal',
  plant:     'bewundert die Pflanze',
  rug:       'entspannt sich auf dem Teppich',
  left:      'erkundet die linke Seite',
  leftmid:   'schlendert umher',
  center:    'steht in der Mitte des Raums',
  rightmid:  'schlendert umher',
  right:     'erkundet die rechte Seite',
}

const activityLabel = computed(() =>
  isMoving.value ? 'ist unterwegs...' : SPOT_LABELS[currentSpot.value]
)

// ── Pose ──────────────────────────────────────────────────────────────────────
const pose = computed((): Pose => {
  const base = (EMOTION_POSES[emotion.value] ?? EMOTION_POSES.NEUTRAL)!
  if (isMoving.value) return WALK_POSES[walkStep.value as 0 | 1]
  if (activity.value === 'sitting') return { ...base, ...SITTING_POSE } as Pose
  return base
})

const TARGET_Y_SITTING = 223
const TARGET_Y_NORMAL  = 188

const avatarTransform = computed(() => {
  const scale = 0.85
  const tx = (displayX.value - 100 * scale).toFixed(1)
  const ty = displayY.value.toFixed(1)
  return `translate(${tx}, ${ty}) scale(${scale}, ${scale})`
})

// ── Animation loop ────────────────────────────────────────────────────────────
function tick() {
  const speed  = effectiveSpeed.value
  const dx     = targetX.value - displayX.value
  const targetY = (activity.value === 'sitting' && !isMoving.value)
    ? TARGET_Y_SITTING : TARGET_Y_NORMAL
  const dy = targetY - displayY.value

  if (Math.abs(dx) > speed) {
    displayX.value += Math.sign(dx) * speed
  } else {
    displayX.value = targetX.value
    if (isMoving.value) onArrived()
  }

  // Smooth sit/stand
  if (Math.abs(dy) > 0.5) displayY.value += dy * 0.06

  rafId = requestAnimationFrame(tick)
}

function onArrived() {
  isMoving.value = false
  if (walkStepId) { clearInterval(walkStepId); walkStepId = null }
  walkStep.value = 0
  scheduleNext(3500 + Math.random() * 5000)
}

function scheduleNext(delay: number) {
  if (behaviorId) clearTimeout(behaviorId)
  behaviorId = setTimeout(pickNextSpot, delay)
}

function pickNextSpot() {
  const pool  = MOOD_SPOTS[emotion.value] ?? MOOD_SPOTS.NEUTRAL
  // needs influence: low energy → more rug/sitting; low social → more window
  let adjustedPool = [...(pool ?? [])]
  if (props.needs) {
    if (props.needs.energy < 30) {
      adjustedPool = [...adjustedPool, 'rug', 'rug', 'rug']
    }
    if (props.needs.social < 30) {
      adjustedPool = [...adjustedPool, 'window', 'window']
    }
    if (props.needs.mood > 80 && props.needs.energy > 80 && props.needs.social > 80) {
      adjustedPool = [...adjustedPool, 'center', 'center']
    }
  }
  const name  = adjustedPool[Math.floor(Math.random() * adjustedPool.length)] as SpotName
  const spot  = SPOTS[name]
  // slight random offset so it doesn't always land exactly the same
  const newX  = Math.max(210, Math.min(750, spot.x + (Math.random() - 0.5) * 80))

  currentSpot.value = name
  activity.value    = spot.activity

  if (Math.abs(newX - displayX.value) < 25) {
    // Already there – skip walk
    scheduleNext(3000 + Math.random() * 4000)
    return
  }

  isMoving.value = true
  targetX.value  = newX

  if (walkStepId) clearInterval(walkStepId)
  walkStepId = setInterval(() => { walkStep.value = 1 - walkStep.value }, 340)
}

// ── Emotion change → repick sooner ────────────────────────────────────────────
watch(() => props.emotion, () => {
  if (behaviorId) clearTimeout(behaviorId)
  scheduleNext(800)
})

// ── Lifecycle ─────────────────────────────────────────────────────────────────
onMounted(() => {
  rafId = requestAnimationFrame(tick)
  scheduleNext(1500)
})

onUnmounted(() => {
  if (rafId)      cancelAnimationFrame(rafId)
  if (walkStepId) clearInterval(walkStepId)
  if (behaviorId) clearTimeout(behaviorId)
})
</script>

<style scoped>
.room-wrap {
  position: relative;
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: #0f172a;
  border: 1px solid #334155;
}

.room-svg {
  width: 100%;
  height: auto;
  display: block;
}

.fill-height {
  border-radius: 0;
  border: none;
  height: 100%;
}

.fill-height .room-svg {
  height: 100%;
  width: 100%;
}

.activity-label {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  font-size: 12px;
  color: #cbd5e1;
  white-space: nowrap;
  pointer-events: none;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.aura {
  animation: pulse 3s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50%       { opacity: 1;   }
}
</style>
