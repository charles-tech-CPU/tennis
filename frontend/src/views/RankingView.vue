<template>
  <div class="page-header">
    <div>
      <h1>Classement</h1>
      <p class="subtitle">{{ rows.length }} joueur(s) avec au moins un résultat acté</p>
    </div>
    <div class="actions">
      <button class="secondary" @click="load">Actualiser</button>
    </div>
  </div>

  <div class="filters">
    <input v-model="search" aria-label="Rechercher un joueur" placeholder="Nom, prénom ou nationalité..." />
    <select v-model="countryFilter" aria-label="Filtrer par pays">
      <option value="">Tous les pays</option>
      <option v-for="c in countries" :key="c.name" :value="c.name">{{ c.name }} ({{ c.count }})</option>
    </select>
  </div>

  <p class="callout">
    Classement glissant sur 52 semaines (comme le vrai circuit ATP) : pour une semaine donnée,
    seule l'édition la plus récente ayant déjà commencé compte, l'édition de l'année précédente
    à cette même semaine est automatiquement retirée dès que la nouvelle a débuté.
    Calcul automatique : 4 Grand Chelem + 8 des 9 Masters 1000 (hors Monte-Carlo) +
    somme des 5 meilleurs autres tournois + le meilleur entre Monte-Carlo et le 6e meilleur autre tournoi.
    L'ATP Finals ne compte pas dans le total : ses points sont affichés à part, en fin de tableau.
    Seuls les tournois avec un résultat acté (joueur éliminé ou vainqueur) comptent.
    Une case obligatoire jamais disputée (faute de classement suffisant) est comblée par le meilleur
    résultat excédentaire disponible (au-delà des 5 + remplacement) : affiché <em>en italique</em> avec
    le nom du tournoi d'origine.
    <span class="fr-dot"></span> = joueur français.
    <span v-if="liveTournamentsLegend.length"> · En direct :
      <span v-for="lt in liveTournamentsLegend" :key="lt.tournamentId" class="live-legend-item">
        <span class="live-dot" :style="{ background: hslColor(lt.colorHue) }"></span>{{ lt.tournamentName }}
      </span>
      (points minimum garantis, tournoi pas encore terminé)
    </span>
  </p>

  <template v-if="filteredRows.length">
    <div v-if="pageCount > 1" class="pagination pagination-top">
      <button class="secondary" :disabled="page === 1" @click="page--">Précédent</button>
      <span>Page {{ page }} / {{ pageCount }}</span>
      <button class="secondary" :disabled="page === pageCount" @click="page++">Suivant</button>
    </div>
    <div ref="scrollTopEl" class="scroll-top" @scroll="onTopScroll">
      <div :style="{ width: contentWidth + 'px', height: '1px' }"></div>
    </div>
    <div ref="scrollBottomEl" class="table-card ranking-scroll" @scroll="onBottomScroll">
      <table class="ranking-table">
        <thead>
          <tr>
            <th rowspan="2" class="rank sticky-col sticky-1">#</th>
            <th rowspan="2" class="sticky-col sticky-2">Joueur</th>
            <th rowspan="2" class="sticky-col sticky-3">Nation</th>
            <th rowspan="2" class="num total-col sticky-col sticky-4">Total</th>
            <th :colspan="grandSlamSlots.length" class="group-header gs">Grand Chelem</th>
            <th :colspan="mastersSlots.length" class="group-header masters">Masters 1000</th>
            <th rowspan="2" class="group-header mc">Monte-Carlo</th>
            <th :colspan="5" class="group-header others">5 meilleurs autres tournois</th>
            <th rowspan="2" class="group-header repl">Remplacement</th>
            <th rowspan="2" class="group-header nc">Non comptabilisés</th>
            <th rowspan="2" class="group-header finals" title="Tournoi des qualifiés de fin de saison : non compté dans le total">ATP Finals<br><small>(hors total)</small></th>
          </tr>
          <tr>
            <th v-for="s in grandSlamSlots" :key="s" scope="col" class="num small-head">{{ slotHeader(s) }}</th>
            <th v-for="s in mastersSlots" :key="s" scope="col" class="num small-head">{{ slotHeader(s) }}</th>
            <th v-for="i in 5" :key="i" scope="col" class="num small-head">Meilleur {{ i }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="{ r, rank } in pagedRows" :key="r.playerId" :class="{ 'rank-1': rank === 1 }">
            <td
              class="rank sticky-col sticky-1"
              :style="rowAccentStyle(r)"
              :title="r.liveTournaments?.length ? `Encore en jeu : ${r.liveTournaments.map(lt => lt.tournamentName).join(', ')} (points minimum garantis)` : null"
            >{{ rank }}</td>
            <td class="sticky-col sticky-2" :class="{ 'player-fr': isFrench(r) }">
              {{ r.lastName }} {{ r.firstName ?? '' }}
            </td>
            <td class="nation-cell sticky-col sticky-3">
              <span v-if="countryFlagIso(r.nationality)" class="fi" :class="`fi-${countryFlagIso(r.nationality)}`"></span>
              {{ r.nationality ?? '—' }}
            </td>
            <td class="num total-col sticky-col sticky-4"><strong>{{ r.total }}</strong></td>
            <td v-for="s in grandSlamSlots" :key="s" class="num" :class="{ 'cell-named substituted': slotDto(r, s)?.substituted }" :style="liveCellStyle(r, slotDto(r, s))" :title="slotDto(r, s)?.substituted ? `Pas de classement pour disputer ce tournoi : remplace par un resultat excedentaire (${slotDto(r, s).tournamentName})` : null">
              <template v-if="slotDto(r, s)?.substituted">
                <span class="pts">{{ slotDto(r, s).points }}</span>
                <span class="name">{{ slotDto(r, s).tournamentName }}</span>
              </template>
              <template v-else>{{ slotDto(r, s)?.points ?? '—' }}</template>
            </td>
            <td v-for="s in mastersSlots" :key="s" class="num" :class="{ 'cell-named substituted': slotDto(r, s)?.substituted }" :style="liveCellStyle(r, slotDto(r, s))" :title="slotDto(r, s)?.substituted ? `Pas de classement pour disputer ce tournoi : remplace par un resultat excedentaire (${slotDto(r, s).tournamentName})` : null">
              <template v-if="slotDto(r, s)?.substituted">
                <span class="pts">{{ slotDto(r, s).points }}</span>
                <span class="name">{{ slotDto(r, s).tournamentName }}</span>
              </template>
              <template v-else>{{ slotDto(r, s)?.points ?? '—' }}</template>
            </td>
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
            <td class="num" :style="liveCellStyle(r, r.atpFinals)">{{ r.atpFinals?.points ?? '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="pageCount > 1" class="pagination">
      <button class="secondary" :disabled="page === 1" @click="page--">Précédent</button>
      <span>Page {{ page }} / {{ pageCount }}</span>
      <button class="secondary" :disabled="page === pageCount" @click="page++">Suivant</button>
    </div>
  </template>
  <div v-else-if="loaded && rows.length" class="empty-state">
    <div class="icon">🎾</div>
    <p>Aucun joueur ne correspond à la recherche ou au pays choisi.</p>
  </div>
  <div v-else-if="loaded" class="empty-state">
    <div class="icon">🏆</div>
    <p>Aucun résultat acté pour cette saison encore.</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import { mandatorySlotLabel, countryFlagIso, hslColor } from '../labels'

const rows = ref([])
const search = ref('')
const countryFilter = ref('')
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

// Recherche insensible a la casse et aux accents ("Monfils" = "MONFILS",
// "Muller" = "Müller", "espana" = "España").
function normalize(text) {
  return (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

// Le rang affiche doit rester le vrai rang au classement, pas la position
// dans la liste filtree - on l'attache a chaque ligne avant de filtrer.
const filteredRows = computed(() => {
  const q = normalize(search.value.trim())
  return rows.value
    .map((r, i) => ({ r, rank: i + 1 }))
    .filter(({ r }) => !q || normalize(`${r.lastName} ${r.firstName ?? ''} ${r.nationality ?? ''}`).includes(q))
    .filter(({ r }) => !countryFilter.value || (r.nationality ?? '') === countryFilter.value)
})

// Pagination appliquee apres les filtres : le rang attache a chaque ligne
// reste celui du classement complet (la page 2 non filtree commence a #101).
const PAGE_SIZE = 100
const page = ref(1)
const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / PAGE_SIZE)))
const pagedRows = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredRows.value.slice(start, start + PAGE_SIZE)
})

// Tout changement de recherche ou de pays ramene a la premiere page ; un
// rechargement qui reduit le nombre de pages ne doit pas laisser sur une page vide.
watch([search, countryFilter], () => { page.value = 1 })
watch(pageCount, (count) => { if (page.value > count) page.value = count })

// Pays presents au classement (et non la liste complete COUNTRY_NAMES), avec
// le nombre de joueurs classes pour chacun.
const countries = computed(() => {
  const counts = new Map()
  for (const r of rows.value) {
    if (r.nationality) counts.set(r.nationality, (counts.get(r.nationality) ?? 0) + 1)
  }
  return [...counts.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => a.name.localeCompare(b.name))
})

const liveTournamentsLegend = computed(() => {
  const byId = new Map()
  for (const r of rows.value) {
    for (const lt of r.liveTournaments ?? []) byId.set(lt.tournamentId, lt)
  }
  return [...byId.values()].sort((a, b) => a.colorHue - b.colorHue)
})

async function load() {
  rows.value = await api.getRanking()
  loaded.value = true
  await nextTick()
  contentWidth.value = scrollBottomEl.value ? scrollBottomEl.value.scrollWidth : 0
}

onMounted(load)
</script>
