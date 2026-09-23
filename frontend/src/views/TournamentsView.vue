<template>
  <div class="page-header">
    <div>
      <h1>Tournois</h1>
      <p class="subtitle">{{ filtered.length }} tournoi(s)</p>
    </div>
  </div>

  <div class="filters">
    <input v-model="search" aria-label="Rechercher un tournoi" placeholder="Rechercher un tournoi..." />
    <select v-model="seasonFilter" aria-label="Filtrer par saison">
      <option value="">Toutes les saisons</option>
      <option v-for="s in seasons" :key="s" :value="s">{{ s }}</option>
    </select>
    <select v-model="categoryFilter" aria-label="Filtrer par catégorie">
      <option value="">Toutes les catégories</option>
      <option v-for="c in categories" :key="c" :value="c">{{ categoryLabel(c) }}</option>
    </select>
  </div>

  <div class="legend">
    <span class="legend-item"><span class="legend-dot status-completed"></span>Terminé</span>
    <span class="legend-item"><span class="legend-dot status-live"></span>En cours — chaque tournoi a sa propre couleur (la même que dans le classement)</span>
  </div>

  <div v-if="filtered.length" class="table-card">
    <table>
      <thead>
        <tr>
          <th>Semaine</th>
          <th>Nom</th>
          <th>Catégorie</th>
          <th>Pays</th>
          <th>Saison</th>
          <th>Tableau</th>
          <th>Vainqueur</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="t in filtered" :key="t.id" :class="{ 'status-completed': t.status === 'COMPLETED', 'status-in-progress': isLive(t) }" :style="inProgressRowStyle(t)">
          <td class="week-cell">{{ t.weekNumber ?? '—' }}</td>
          <td class="name-cell"><router-link :to="`/tournaments/${t.id}`">{{ t.name }}</router-link></td>
          <td>
            <span class="tag" :class="categoryTagClass(t.category)">{{ categoryLabel(t.category) }}</span>
            <span v-if="mandatorySlotLabel(t.mandatorySlot)" class="tag tag-grass" style="margin-left:4px">{{ mandatorySlotLabel(t.mandatorySlot) }}</span>
          </td>
          <td class="nation-cell">
            <span v-if="countryFlagIso(t.country)" class="fi" :class="`fi-${countryFlagIso(t.country)}`"></span>
            {{ t.country ?? '—' }}
          </td>
          <td>{{ t.season }}</td>
          <td class="nowrap">{{ t.drawSize }} ({{ t.drawSlots }} cases)</td>
          <td class="nation-cell winner-cell">
            <template v-if="t.winner">
              <span class="trophy" aria-hidden="true">🏆</span>
              <span v-if="countryFlagIso(t.winner.nationality)" class="fi" :class="`fi-${countryFlagIso(t.winner.nationality)}`"></span>
              {{ t.winner.lastName }} {{ t.winner.firstName ?? '' }}
            </template>
            <span v-else-if="isLive(t)" class="status-pill live">
              <span class="legend-dot" :style="{ background: hslColor(t.colorHue) }"></span>En cours
            </span>
            <span v-else>—</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <div v-else-if="loaded" class="empty-state">
    <div class="icon">🎾</div>
    <p>Aucun tournoi pour l'instant.</p>
  </div>

  <h2 class="section-title">Ajouter un tournoi</h2>
  <form class="card" @submit.prevent="submit">
    <div class="inline">
      <input v-model="form.name" aria-label="Nom du tournoi" placeholder="Nom du tournoi" required />
      <select v-model="form.category" aria-label="Catégorie" required>
        <option disabled value="">Catégorie</option>
        <option v-for="c in categories" :key="c" :value="c">{{ categoryLabel(c) }}</option>
      </select>
      <input v-model.number="form.season" aria-label="Saison" type="number" placeholder="Saison" required />
      <input v-model.number="form.weekNumber" aria-label="Semaine ATP" type="number" placeholder="Semaine ATP" />
      <select v-model="form.country" aria-label="Pays">
        <option value="">Pays non renseigné</option>
        <option v-for="c in countryNames" :key="c" :value="c">{{ c }}</option>
      </select>
    </div>
    <div class="inline" style="margin-top:8px">
      <input v-model.number="form.drawSize" aria-label="Taille réelle du tableau" type="number" min="2" placeholder="Taille réelle du tableau (ex: 32, 96...)" required />
      <select v-model="form.mandatorySlot" aria-label="Case obligatoire">
        <option value="">Pas une case obligatoire</option>
        <option v-for="m in mandatorySlots" :key="m" :value="m">{{ mandatorySlotLabel(m) }}</option>
      </select>
      <input v-model.number="form.runnerUpPoints" aria-label="Points du finaliste" type="number" placeholder="Points du finaliste (optionnel)" />
    </div>
    <p class="field-hint">
      Le barème de points par tour est pré-rempli automatiquement selon la catégorie (modifiable ensuite sur la page du tournoi).
    </p>
    <button type="submit" style="margin-top:8px">Créer le tournoi</button>
    <p v-if="error" class="form-error">{{ error }}</p>
  </form>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import { categoryLabel, categoryTagClass, mandatorySlotLabel, countryFlagIso, COUNTRY_NAMES, hslColor } from '../labels'

const countryNames = COUNTRY_NAMES

const tournaments = ref([])
const loaded = ref(false)
const error = ref('')
const search = ref('')
const seasonFilter = ref('')
const categoryFilter = ref('')

const categories = ['GRAND_SLAM', 'MASTERS_1000', 'ATP_500', 'ATP_250', 'ATP_175', 'ATP_125', 'ATP_100', 'ATP_75', 'ATP_50']
const mandatorySlots = ['AUSTRALIAN_OPEN', 'ROLAND_GARROS', 'WIMBLEDON', 'US_OPEN', 'ATP_FINALS',
  'INDIAN_WELLS', 'MIAMI', 'MONTE_CARLO', 'MADRID', 'ROME', 'CANADA', 'CINCINNATI', 'SHANGHAI', 'PARIS_BERCY']

const form = reactive({
  name: '', category: '', season: new Date().getFullYear(), weekNumber: null,
  country: '', drawSize: 32, mandatorySlot: '', runnerUpPoints: null
})

const seasons = computed(() => [...new Set(tournaments.value.map(t => t.season))].sort((a, b) => b - a))

function isLive(t) {
  return t.status === 'IN_PROGRESS' && t.colorHue != null
}

// Tournoi en cours : bandeau de sa couleur a gauche + fond tres leger de la
// meme teinte (voir tbody tr.status-in-progress dans style.css).
function inProgressRowStyle(t) {
  if (!isLive(t)) return {}
  return { '--row-accent': hslColor(t.colorHue), '--row-tint': hslColor(t.colorHue, 70, 95) }
}

const filtered = computed(() => tournaments.value.filter(t => {
  if (search.value && !t.name.toLowerCase().includes(search.value.toLowerCase())) return false
  if (seasonFilter.value && t.season !== Number(seasonFilter.value)) return false
  if (categoryFilter.value && t.category !== categoryFilter.value) return false
  return true
}))

async function load() {
  tournaments.value = await api.getTournaments()
  loaded.value = true
}

async function submit() {
  error.value = ''
  try {
    await api.createTournament({
      name: form.name,
      category: form.category,
      season: form.season,
      weekNumber: form.weekNumber || null,
      country: form.country || null,
      drawSize: form.drawSize,
      mandatorySlot: form.mandatorySlot || null,
      runnerUpPoints: form.runnerUpPoints || null,
      qualifyingRound1Points: null,
      qualifyingRound2Points: null,
      rounds: null
    })
    form.name = ''
    form.weekNumber = null
    form.country = ''
    form.mandatorySlot = ''
    form.runnerUpPoints = null
    await load()
  } catch (e) {
    error.value = e.response?.data?.error ?? 'Erreur lors de la création du tournoi.'
  }
}

onMounted(load)
</script>
