<template>
  <div class="page-header">
    <div>
      <h1>Classement {{ season }}</h1>
      <p class="subtitle">{{ rows.length }} joueur(s) avec au moins un résultat acté</p>
    </div>
    <div class="actions">
      <input v-model.number="season" type="number" style="width:100px" />
      <button class="secondary" @click="load">Actualiser</button>
    </div>
  </div>

  <p class="callout">
    Calcul automatique : 4 Grand Chelem + ATP Finals + 8 des 9 Masters 1000 (hors Monte-Carlo) +
    somme des 5 meilleurs autres tournois + le meilleur entre Monte-Carlo et le 6e meilleur autre tournoi.
    Seuls les tournois avec un résultat acté (joueur éliminé ou vainqueur) comptent.
    <span class="fr-dot"></span> = joueur français.
    <span v-if="liveTournamentsLegend.length"> · En direct :
      <span v-for="lt in liveTournamentsLegend" :key="lt.tournamentId" class="live-legend-item">
        <span class="live-dot" :style="{ background: hslColor(lt.colorHue) }"></span>{{ lt.tournamentName }}
      </span>
      (points minimum garantis, tournoi pas encore terminé)
    </span>
  </p>

  <div v-if="rows.length" class="scroll-top" ref="scrollTopEl" @scroll="onTopScroll">
    <div :style="{ width: contentWidth + 'px', height: '1px' }"></div>
  </div>
  <div v-if="rows.length" class="table-card ranking-scroll" ref="scrollBottomEl" @scroll="onBottomScroll">
    <table class="ranking-table">
      <thead>
        <tr>
          <th rowspan="2" class="rank sticky-col sticky-1">#</th>
          <th rowspan="2" class="sticky-col sticky-2">Joueur</th>
          <th rowspan="2" class="sticky-col sticky-3">Nation</th>
          <th rowspan="2" class="num total-col sticky-col sticky-4">Total</th>
          <th :colspan="grandSlamSlots.length" class="group-header gs">Grand Chelem</th>
          <th rowspan="2" class="group-header finals">ATP Finals</th>
          <th :colspan="mastersSlots.length" class="group-header masters">Masters 1000</th>
          <th rowspan="2" class="group-header mc">Monte-Carlo</th>
          <th :colspan="5" class="group-header others">5 meilleurs autres tournois</th>
          <th rowspan="2" class="group-header repl">Remplacement</th>
          <th rowspan="2" class="group-header nc">Non comptabilisés</th>
        </tr>
        <tr>
          <th v-for="s in grandSlamSlots" :key="s" class="num small-head">{{ slotHeader(s) }}</th>
          <th v-for="s in mastersSlots" :key="s" class="num small-head">{{ slotHeader(s) }}</th>
          <th v-for="i in 5" :key="i" class="num small-head">Meilleur {{ i }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(r, i) in rows" :key="r.playerId" :class="{ 'rank-1': i === 0 }">
          <td
            class="rank sticky-col sticky-1"
            :style="rowAccentStyle(r)"
            :title="r.liveTournaments?.length ? `Encore en jeu : ${r.liveTournaments.map(lt => lt.tournamentName).join(', ')} (points minimum garantis)` : null"
          >{{ i + 1 }}</td>
          <td class="sticky-col sticky-2" :class="{ 'player-fr': isFrench(r) }">
            {{ r.lastName }} {{ r.firstName ?? '' }}
          </td>
          <td class="nation-cell sticky-col sticky-3">
            <span v-if="countryFlagIso(r.nationality)" class="fi" :class="`fi-${countryFlagIso(r.nationality)}`"></span>
            {{ r.nationality ?? '—' }}
          </td>
          <td class="num total-col sticky-col sticky-4"><strong>{{ r.total }}</strong></td>
          <td v-for="s in grandSlamSlots" :key="s" class="num" :style="liveCellStyle(r, slotDto(r, s))">{{ slotDto(r, s)?.points ?? '—' }}</td>
          <td class="num" :style="liveCellStyle(r, slotDto(r, 'ATP_FINALS'))">{{ slotDto(r, 'ATP_FINALS')?.points ?? '—' }}</td>
          <td v-for="s in mastersSlots" :key="s" class="num" :style="liveCellStyle(r, slotDto(r, s))">{{ slotDto(r, s)?.points ?? '—' }}</td>
          <td class="num" :style="liveCellStyle(r, r.monteCarlo)">{{ r.monteCarlo?.points ?? '—' }}</td>
          <td v-for="i in 5" :key="i" class="num cell-named" :style="liveCellStyle(r, r.bestOthers[i-1])">
            <template v-if="r.bestOthers[i-1]">
              <span class="pts">{{ r.bestOthers[i-1].points }}</span>
              <span class="name">{{ r.bestOthers[i-1].tournamentName }}</span>
            </template>
            <span v-else>—</span>
          </td>
          <td class="num cell-named" :style="liveCellStyle(r, r.replacement)">
            <template v-if="r.replacement">
              <span class="pts">{{ r.replacement.points }}</span>
              <span class="name">{{ r.replacement.tournamentName }}</span>
            </template>
            <span v-else>—</span>
          </td>
          <td class="non-counted">
            <span v-for="(nc, idx) in r.nonCounted" :key="idx" class="tag" :style="liveCellStyle(r, nc)">{{ nc.tournamentName }} ({{ nc.points }})</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <div v-else-if="loaded" class="empty-state">
    <div class="icon">🏆</div>
    <p>Aucun résultat acté pour cette saison encore.</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import api from '../services/api'
import { mandatorySlotLabel, countryFlagIso, hslColor } from '../labels'

const season = ref(new Date().getFullYear())
const rows = ref([])
const loaded = ref(false)
const contentWidth = ref(0)
const scrollTopEl = ref(null)
const scrollBottomEl = ref(null)
let syncing = false

const grandSlamSlots = ['AUSTRALIAN_OPEN', 'ROLAND_GARROS', 'WIMBLEDON', 'US_OPEN']
const mastersSlots = ['INDIAN_WELLS', 'MIAMI', 'MADRID', 'ROME', 'CANADA', 'CINCINNATI', 'SHANGHAI', 'PARIS_BERCY']

function onTopScroll() {
  if (syncing) return
  syncing = true
  scrollBottomEl.value.scrollLeft = scrollTopEl.value.scrollLeft
  syncing = false
}
function onBottomScroll() {
  if (syncing) return
  syncing = true
  scrollTopEl.value.scrollLeft = scrollBottomEl.value.scrollLeft
  syncing = false
}

function slotDto(row, slot) {
  return row.mandatorySlots?.[slot] ?? null
}

function slotHeader(slot) {
  const withName = rows.value.find(r => r.mandatorySlots?.[slot])
  return withName ? withName.mandatorySlots[slot].tournamentName : mandatorySlotLabel(slot)
}

function isFrench(row) {
  return (row.nationality ?? '').toUpperCase() === 'FRANCE'
}

// Un tournoi encore en cours (pas termine) recoit une teinte stable (voir
// RankingService.assignHues cote backend) : les joueurs encore en jeu dedans
// sont reperes avec la meme couleur, et leurs points refletent le minimum
// garanti par leur dernier tour deja gagne (voir le callout au-dessus du
// tableau).
function rowAccentStyle(row) {
  const lt = row.liveTournaments?.[0]
  return lt ? { borderLeft: `7px solid ${hslColor(lt.colorHue)}` } : {}
}

function liveCellStyle(row, dto) {
  if (!dto) return {}
  const lt = row.liveTournaments?.find(x => x.tournamentId === dto.tournamentId)
  return lt ? { background: hslColor(lt.colorHue, 70, 91), fontWeight: 700 } : {}
}

const liveTournamentsLegend = computed(() => {
  const byId = new Map()
  for (const r of rows.value) {
    for (const lt of r.liveTournaments ?? []) byId.set(lt.tournamentId, lt)
  }
  return [...byId.values()].sort((a, b) => a.colorHue - b.colorHue)
})

async function load() {
  rows.value = await api.getRanking(season.value)
  loaded.value = true
  await nextTick()
  contentWidth.value = scrollBottomEl.value ? scrollBottomEl.value.scrollWidth : 0
}

onMounted(load)
</script>
