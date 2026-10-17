<template>
  <div class="card ranking-history">
    <div class="rh-summary">
      <span>Actuel : <strong>N° {{ latest.position }}</strong> <span class="rh-week">(S{{ latest.week }})</span></span>
      <span>Meilleur : <strong>N° {{ best.position }}</strong> <span class="rh-week">(S{{ best.week }})</span></span>
      <span>Plus bas : <strong>N° {{ worst.position }}</strong> <span class="rh-week">(S{{ worst.week }})</span></span>
    </div>

    <div class="rh-plot">
      <svg
        :viewBox="`0 0 ${W} ${H}`"
        class="rh-svg"
        role="img"
        :aria-label="`Évolution du classement en ${season}, du N° ${points[0].position} au N° ${latest.position}`"
        @pointermove="onMove"
        @pointerleave="hover = null"
      >
        <!-- Grille : lignes horizontales aux classements reperes (echelle log, N° 1 en haut) -->
        <g class="rh-grid">
          <g v-for="t in yTicks" :key="t">
            <line :x1="PAD.left" :x2="W - PAD.right" :y1="y(t)" :y2="y(t)" />
            <text :x="PAD.left - 8" :y="y(t)" text-anchor="end" dominant-baseline="middle">{{ t }}</text>
          </g>
          <text v-for="w in xTicks" :key="`x${w}`" :x="x(w)" :y="H - PAD.bottom + 18" text-anchor="middle">S{{ w }}</text>
        </g>

        <!-- Une ligne par periode continue : une semaine sans classement coupe la courbe -->
        <path v-for="(seg, i) in segments" :key="i" class="rh-line" :d="linePath(seg)" />
        <circle v-for="p in isolated" :key="`iso${p.week}`" class="rh-dot" :cx="x(p.week)" :cy="y(p.position)" r="3" />

        <!-- Repere permanent sur le meilleur classement -->
        <circle class="rh-best" :cx="x(best.week)" :cy="y(best.position)" r="4" />

        <g v-if="hover">
          <line class="rh-crosshair" :x1="x(hover.week)" :x2="x(hover.week)" :y1="PAD.top" :y2="H - PAD.bottom" />
          <circle class="rh-hover-dot" :cx="x(hover.week)" :cy="y(hover.position)" r="5" />
        </g>
      </svg>

      <div v-if="hover" class="rh-tooltip" :style="tooltipStyle">
        <div class="rh-tooltip-week">Semaine {{ hover.week }}</div>
        <div><strong>N° {{ hover.position }}</strong> · {{ hover.points }} pts</div>
      </div>
    </div>

    <details class="rh-table">
      <summary>Voir le détail semaine par semaine</summary>
      <table>
        <thead><tr><th>Semaine</th><th class="num">Classement</th><th class="num">Points</th></tr></thead>
        <tbody>
          <tr v-for="p in points" :key="p.week">
            <td>S{{ p.week }}</td>
            <td class="num">{{ p.position }}</td>
            <td class="num">{{ p.points }}</td>
          </tr>
        </tbody>
      </table>
    </details>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

// points : [{ week, position, points }] tries par semaine, au moins un element.
const props = defineProps({
  points: { type: Array, required: true },
  season: { type: Number, required: true }
})

const W = 1000
const H = 260
const PAD = { top: 16, right: 20, bottom: 30, left: 48 }

const hover = ref(null)

const latest = computed(() => props.points[props.points.length - 1])
const best = computed(() => props.points.reduce((a, b) => (b.position < a.position ? b : a)))
const worst = computed(() => props.points.reduce((a, b) => (b.position > a.position ? b : a)))

const weekMin = computed(() => props.points[0].week)
const weekMax = computed(() => Math.max(latest.value.week, weekMin.value + 1))

// Echelle log : passer de 250e a 38e est aussi lisible que de 8e a 3e. L'axe
// couvre seulement la plage occupee : du repere juste au-dessus du meilleur
// classement jusqu'au plus bas classement atteint.
const TICKS = [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000]
const posMin = computed(() => [...TICKS].reverse().find(t => t <= best.value.position) ?? 1)
const posMax = computed(() => Math.max(worst.value.position, posMin.value * 2))

function x(week) {
  return PAD.left + ((week - weekMin.value) / (weekMax.value - weekMin.value)) * (W - PAD.left - PAD.right)
}

function y(position) {
  const range = Math.log10(posMax.value) - Math.log10(posMin.value)
  return PAD.top + ((Math.log10(position) - Math.log10(posMin.value)) / range) * (H - PAD.top - PAD.bottom)
}

const yTicks = computed(() => TICKS.filter(t => t >= posMin.value && t <= posMax.value))

// Une douzaine d'etiquettes au plus, pour ne jamais se chevaucher.
const xTicks = computed(() => {
  const span = weekMax.value - weekMin.value
  const step = Math.max(1, Math.ceil(span / 12))
  const ticks = []
  for (let w = weekMin.value; w <= weekMax.value; w += step) ticks.push(w)
  return ticks
})

// Periodes de semaines consecutives ou le joueur etait classe.
const segments = computed(() => {
  const segs = []
  let current = []
  for (const p of props.points) {
    if (current.length && p.week !== current[current.length - 1].week + 1) {
      segs.push(current)
      current = []
    }
    current.push(p)
  }
  if (current.length) segs.push(current)
  return segs
})

const isolated = computed(() => segments.value.filter(s => s.length === 1).map(s => s[0]))

function linePath(seg) {
  return seg.map((p, i) => `${i ? 'L' : 'M'}${x(p.week).toFixed(1)},${y(p.position).toFixed(1)}`).join(' ')
}

// Survol : la semaine classee la plus proche du pointeur.
function onMove(event) {
  const svg = event.currentTarget
  const rect = svg.getBoundingClientRect()
  const svgX = ((event.clientX - rect.left) / rect.width) * W
  let nearest = props.points[0]
  for (const p of props.points) {
    if (Math.abs(x(p.week) - svgX) < Math.abs(x(nearest.week) - svgX)) nearest = p
  }
  hover.value = nearest
}

const tooltipStyle = computed(() => {
  if (!hover.value) return {}
  const left = (x(hover.value.week) / W) * 100
  const top = (y(hover.value.position) / H) * 100
  // Bascule a gauche du point dans la moitie droite pour rester dans le cadre.
  return {
    left: `${left}%`,
    top: `${top}%`,
    transform: left > 60 ? 'translate(calc(-100% - 12px), -50%)' : 'translate(12px, -50%)'
  }
})
</script>
