<template>
  <h2>Tournois</h2>

  <div class="filters">
    <input v-model="search" placeholder="Rechercher un tournoi..." />
    <select v-model="seasonFilter">
      <option value="">Toutes les saisons</option>
      <option v-for="s in seasons" :key="s" :value="s">{{ s }}</option>
    </select>
    <select v-model="categoryFilter">
      <option value="">Toutes les categories</option>
      <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
    </select>
  </div>

  <table v-if="filtered.length">
    <thead>
      <tr>
        <th>Semaine</th>
        <th>Nom</th>
        <th>Categorie</th>
        <th>Pays</th>
        <th>Saison</th>
        <th>Tableau</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="t in filtered" :key="t.id">
        <td>{{ t.weekNumber ?? '—' }}</td>
        <td><router-link :to="`/tournaments/${t.id}`">{{ t.name }}</router-link></td>
        <td>{{ t.category }}<span v-if="t.mandatorySlot" class="tag">{{ t.mandatorySlot }}</span></td>
        <td>{{ t.country ?? '—' }}</td>
        <td>{{ t.season }}</td>
        <td>{{ t.drawSize }} ({{ t.drawSlots }} cases)</td>
      </tr>
    </tbody>
  </table>
  <p v-else-if="loaded">Aucun tournoi pour l'instant.</p>

  <h3>Ajouter un tournoi</h3>
  <form class="card" @submit.prevent="submit">
    <div class="inline">
      <input v-model="form.name" placeholder="Nom du tournoi" required />
      <select v-model="form.category" required>
        <option disabled value="">Categorie</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <input v-model.number="form.season" type="number" placeholder="Saison" required />
      <input v-model.number="form.weekNumber" type="number" placeholder="Semaine ATP" />
      <input v-model="form.country" placeholder="Pays" />
    </div>
    <div class="inline" style="margin-top:8px">
      <input v-model.number="form.drawSize" type="number" min="2" placeholder="Taille reelle du tableau (ex: 32, 96...)" required />
      <select v-model="form.mandatorySlot">
        <option value="">Pas une case obligatoire</option>
        <option v-for="m in mandatorySlots" :key="m" :value="m">{{ m }}</option>
      </select>
      <input v-model.number="form.runnerUpPoints" type="number" placeholder="Points du finaliste (optionnel)" />
    </div>
    <p style="font-size:12px;color:#9fb0c0;margin-top:8px">
      Le bareme de points par tour est pre-rempli automatiquement selon la categorie (modifiable ensuite sur la page du tournoi).
    </p>
    <button type="submit" style="margin-top:8px">Creer le tournoi</button>
    <p v-if="error" style="color:#ff6b6b">{{ error }}</p>
  </form>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'

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
    error.value = e.response?.data?.error ?? 'Erreur lors de la creation du tournoi.'
  }
}

onMounted(load)
</script>
