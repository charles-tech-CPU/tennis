<template>
  <router-link to="/" class="back-link">← Tous les tournois</router-link>

  <div class="page-header">
    <div>
      <h1>{{ tournament?.name ?? '...' }}</h1>
      <p v-if="tournament" class="subtitle inline">
        <span class="tag" :class="categoryTagClass(tournament.category)">{{ categoryLabel(tournament.category) }}</span>
        <span v-if="mandatorySlotLabel(tournament.mandatorySlot)" class="tag tag-grass">{{ mandatorySlotLabel(tournament.mandatorySlot) }}</span>
        Saison {{ tournament.season }}
        <span v-if="tournament.weekNumber">· Semaine {{ tournament.weekNumber }}</span>
        · {{ tournament.country ?? 'Pays non renseigné' }}
        · Tableau {{ tournament.drawSize }} ({{ tournament.drawSlots }} cases)
      </p>
    </div>
    <div class="actions">
      <button class="secondary" @click="toggleEdit">{{ editing ? 'Fermer' : 'Modifier les réglages' }}</button>
    </div>
  </div>

  <div v-if="editing" class="card">
    <h3>Réglages du tournoi</h3>
    <p class="field-hint" style="margin-top:0">
      Le nom, la saison et la taille du tableau sont fixés à la création (ils conditionnent le tableau déjà généré). Tout le reste est modifiable ici.
    </p>
    <div class="inline" style="margin-top:12px">
      <select v-model="editForm.category">
        <option v-for="c in categories" :key="c" :value="c">{{ categoryLabel(c) }}</option>
      </select>
      <input v-model.number="editForm.weekNumber" type="number" placeholder="Semaine ATP" />
      <select v-model="editForm.country">
        <option value="">Pays non renseigné</option>
        <option v-for="c in countryNames" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="editForm.mandatorySlot">
        <option value="">Pas une case obligatoire</option>
        <option v-for="m in mandatorySlots" :key="m" :value="m">{{ mandatorySlotLabel(m) }}</option>
      </select>
    </div>
    <div class="inline" style="margin-top:8px">
      <input v-model.number="editForm.qualifyingRound1Points" type="number" placeholder="Points qualif. tour 1" />
      <input v-model.number="editForm.qualifyingRound2Points" type="number" placeholder="Points qualif. tour 2" />
      <input v-model.number="editForm.runnerUpPoints" type="number" placeholder="Points du finaliste battu" />
    </div>

    <h3 style="margin-top:20px">Barème de points par tour</h3>
    <div class="inline" style="margin-top:8px">
      <label v-for="r in editForm.rounds" :key="r.roundOrder" style="flex-direction:column;align-items:flex-start;gap:4px">
        <span style="font-size:11px;color:var(--text-muted);text-transform:uppercase">{{ r.roundLabel }}</span>
        <input v-model.number="r.points" type="number" min="0" style="width:90px" />
      </label>
    </div>

    <div class="inline" style="margin-top:16px">
      <button @click="saveEdit">Enregistrer</button>
      <button class="secondary" @click="toggleEdit">Annuler</button>
    </div>
    <p v-if="editError" class="form-error">{{ editError }}</p>
  </div>

  <div class="draw-tabs">
    <button class="tab" :class="{ active: activeTab === 'main' }" @click="switchTab('main')">Tableau principal</button>
    <button class="tab" :class="{ active: activeTab === 'qualifying' }" @click="switchTab('qualifying')">Qualifs</button>
  </div>

  <template v-if="activeTab === 'qualifying' && !tournament?.qualifyingTournamentId">
    <div class="card">
      <h3>Créer le tableau de qualifications</h3>
      <p class="field-hint" style="margin-top:0">
        Taille du tableau de qualifs (doit être divisible par 2^nombre de tours, ex: 24 joueurs sur 2 tours = 6 qualifiés en groupes de 4).
      </p>
      <div class="inline" style="margin-top:10px">
        <input v-model.number="qualifForm.drawSize" type="number" min="2" placeholder="Nb de joueurs en quali" />
        <input v-model.number="qualifRoundsCount" type="number" min="1" max="4" placeholder="Nb de tours" />
      </div>
      <div class="inline" style="margin-top:10px">
        <label v-for="r in qualifForm.rounds" :key="r.roundOrder" style="flex-direction:column;align-items:flex-start;gap:4px">
          <span style="font-size:11px;color:var(--text-muted);text-transform:uppercase">{{ r.roundLabel }}</span>
          <input v-model.number="r.points" type="number" min="0" style="width:90px" />
        </label>
      </div>
      <button style="margin-top:12px" @click="createQualifying">Créer le tableau de qualifs</button>
      <p v-if="qualifError" class="form-error">{{ qualifError }}</p>
    </div>
  </template>

  <template v-else>
    <h2 class="section-title">Tableau</h2>
    <BracketView v-if="drawMatches.length" :rounds="drawTournament?.rounds ?? []" :matches="drawMatches" @select-match="openScoreEditor" @remove-entry="removeEntry" />
    <div v-else-if="drawLoaded" class="empty-state">
      <div class="icon">🎾</div>
      <p>Ajoute des joueurs ci-dessous pour faire apparaître le tableau du 1er tour.</p>
    </div>

    <div class="card" style="margin-top:20px">
      <h3>Ajouter au tableau ({{ drawEntries.length }} / {{ drawTournament?.drawSlots ?? '?' }})</h3>
      <form class="inline" style="margin-top:10px" @submit.prevent="submitEntry">
        <input v-model.number="entryForm.drawPosition" type="number" min="1" :max="drawTournament?.drawSlots" placeholder="Position" required />
        <label><input type="checkbox" v-model="entryForm.bye" /> Bye</label>

        <template v-if="!entryForm.bye">
          <template v-if="!entryForm.newPlayer">
            <select v-model.number="entryForm.playerId" required>
              <option disabled value="">Joueur</option>
              <option v-for="p in players" :key="p.id" :value="p.id">{{ p.lastName }} {{ p.firstName ?? '' }}</option>
            </select>
            <button type="button" class="secondary" @click="entryForm.newPlayer = true">+ Nouveau joueur</button>
          </template>
          <template v-else>
            <input v-model="newPlayerForm.lastName" placeholder="Nom" required />
            <input v-model="newPlayerForm.firstName" placeholder="Prénom" required />
            <select v-model="newPlayerForm.nationality" required>
              <option disabled value="">Pays</option>
              <option v-for="c in countryNames" :key="c" :value="c">{{ c }}</option>
            </select>
            <button type="button" class="secondary" @click="entryForm.newPlayer = false">Annuler</button>
          </template>
        </template>

        <input v-if="!entryForm.bye" v-model.number="entryForm.seed" type="number" placeholder="Tête de série" />
        <select v-if="!entryForm.bye" v-model="entryForm.entryType">
          <option value="">Entrée directe</option>
          <option value="WILD_CARD">Wild card (WC)</option>
          <option value="QUALIFIER">Qualifié (Q)</option>
          <option value="LUCKY_LOSER">Lucky loser (LL)</option>
          <option value="ALTERNATE">Remplaçant (ALT)</option>
          <option value="NEW_GENERATION">New Generation (NG)</option>
          <option value="SPECIAL_EXEMPT">Special exempt (SE)</option>
          <option value="PROTECTED_RANKING">Classement protégé (PR)</option>
          <option value="CUT_OFF">Repêché / cut-off (CO)</option>
        </select>
        <button type="submit">Ajouter</button>
      </form>
      <p v-if="error" class="form-error">{{ error }}</p>
    </div>
  </template>

  <div v-if="scoreEditorMatch" class="score-editor" @click.self="scoreEditorMatch = null">
    <div class="card">
      <h3>Score du match</h3>
      <p class="matchup">{{ scoreEditorMatch.entry1.playerLastName }} vs {{ scoreEditorMatch.entry2.playerLastName }}</p>
      <label>
        <input type="radio" :value="scoreEditorMatch.entry1.id" v-model="scoreForm.winnerEntryId" />
        {{ scoreEditorMatch.entry1.playerLastName }} gagne
      </label>
      <label>
        <input type="radio" :value="scoreEditorMatch.entry2.id" v-model="scoreForm.winnerEntryId" />
        {{ scoreEditorMatch.entry2.playerLastName }} gagne
      </label>
      <input v-model="scoreForm.score" placeholder="Score set par set, ex: 63 46 63" />
      <p v-if="scoreForm.score" class="field-hint" style="margin-top:4px">Enregistré comme : {{ formatMatchScore(scoreForm.score) }}</p>
      <div class="inline">
        <button @click="submitScore">Enregistrer</button>
        <button class="secondary" @click="scoreEditorMatch = null">Annuler</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import BracketView from '../components/BracketView.vue'
import { categoryLabel, categoryTagClass, mandatorySlotLabel, formatMatchScore, COUNTRY_NAMES } from '../labels'

const countryNames = COUNTRY_NAMES

const props = defineProps({ id: { type: [String, Number], required: true } })

const tournament = ref(null)
const players = ref([])
const error = ref('')

const activeTab = ref('main')
const drawTournament = ref(null)
const drawEntries = ref([])
const drawMatches = ref([])
const drawLoaded = ref(false)

const entryForm = reactive({ drawPosition: null, bye: false, playerId: '', seed: null, entryType: '', newPlayer: false })
const newPlayerForm = reactive({ lastName: '', firstName: '', nationality: '' })
const scoreEditorMatch = ref(null)
const scoreForm = reactive({ score: '', winnerEntryId: null })

const mandatorySlots = ['AUSTRALIAN_OPEN', 'ROLAND_GARROS', 'WIMBLEDON', 'US_OPEN', 'ATP_FINALS',
  'INDIAN_WELLS', 'MIAMI', 'MONTE_CARLO', 'MADRID', 'ROME', 'CANADA', 'CINCINNATI', 'SHANGHAI', 'PARIS_BERCY']
const categories = ['GRAND_SLAM', 'MASTERS_1000', 'ATP_500', 'ATP_250', 'ATP_175', 'ATP_125', 'ATP_100', 'ATP_75', 'ATP_50']

const editing = ref(false)
const editError = ref('')
const editForm = reactive({
  category: '', weekNumber: null, country: '', mandatorySlot: '',
  qualifyingRound1Points: null, qualifyingRound2Points: null, runnerUpPoints: null,
  rounds: []
})

const qualifForm = reactive({ drawSize: 24, rounds: [{ roundOrder: 1, roundLabel: 'Q1', points: 7 }, { roundOrder: 2, roundLabel: 'Q2', points: 13 }] })
const qualifRoundsCount = ref(2)
const qualifError = ref('')

watch(qualifRoundsCount, (n) => {
  n = Math.max(1, Math.min(4, n || 1))
  const rounds = []
  for (let i = 1; i <= n; i++) {
    const existing = qualifForm.rounds.find(r => r.roundOrder === i)
    rounds.push({ roundOrder: i, roundLabel: `Q${i}`, points: existing ? existing.points : 0 })
  }
  qualifForm.rounds = rounds
})

async function loadMain() {
  tournament.value = await api.getTournament(Number(props.id))
  players.value = await api.getPlayers()
}

async function loadDraw(tid) {
  drawLoaded.value = false
  if (!tid) {
    drawTournament.value = null
    drawEntries.value = []
    drawMatches.value = []
    drawLoaded.value = true
    return
  }
  const [t, e, m] = await Promise.all([api.getTournament(tid), api.getEntries(tid), api.getMatches(tid)])
  drawTournament.value = t
  drawEntries.value = e
  drawMatches.value = m
  drawLoaded.value = true
}

async function switchTab(tab) {
  activeTab.value = tab
  const tid = tab === 'main' ? Number(props.id) : tournament.value?.qualifyingTournamentId
  await loadDraw(tid)
}

async function createQualifying() {
  qualifError.value = ''
  try {
    await api.createQualifyingDraw(Number(props.id), { drawSize: qualifForm.drawSize, rounds: qualifForm.rounds })
    await loadMain()
    await switchTab('qualifying')
  } catch (e) {
    qualifError.value = e.response?.data?.error ?? 'Erreur lors de la création des qualifs.'
  }
}

function toggleEdit() {
  if (!editing.value && tournament.value) {
    editForm.category = tournament.value.category
    editForm.weekNumber = tournament.value.weekNumber
    editForm.country = tournament.value.country ?? ''
    editForm.mandatorySlot = tournament.value.mandatorySlot ?? ''
    editForm.qualifyingRound1Points = tournament.value.qualifyingRound1Points
    editForm.qualifyingRound2Points = tournament.value.qualifyingRound2Points
    editForm.runnerUpPoints = tournament.value.runnerUpPoints
    editForm.rounds = tournament.value.rounds.map(r => ({ ...r }))
    editError.value = ''
  }
  editing.value = !editing.value
}

async function saveEdit() {
  editError.value = ''
  try {
    tournament.value = await api.updateTournament(Number(props.id), {
      category: editForm.category,
      weekNumber: editForm.weekNumber || null,
      country: editForm.country || null,
      mandatorySlot: editForm.mandatorySlot || null,
      qualifyingRound1Points: editForm.qualifyingRound1Points,
      qualifyingRound2Points: editForm.qualifyingRound2Points,
      runnerUpPoints: editForm.runnerUpPoints,
      rounds: editForm.rounds
    })
    editing.value = false
    if (activeTab.value === 'main') await loadDraw(Number(props.id))
  } catch (e) {
    editError.value = e.response?.data?.error ?? 'Erreur lors de la mise à jour du tournoi.'
  }
}

async function submitEntry() {
  error.value = ''
  try {
    let playerId = entryForm.playerId
    if (!entryForm.bye && entryForm.newPlayer) {
      const created = await api.createPlayer({
        lastName: newPlayerForm.lastName,
        firstName: newPlayerForm.firstName,
        nationality: newPlayerForm.nationality
      })
      playerId = created.id
      players.value = await api.getPlayers()
    }

    await api.createEntry(drawTournament.value.id, {
      playerId: entryForm.bye ? null : playerId,
      drawPosition: entryForm.drawPosition,
      seed: entryForm.seed || null,
      entryType: entryForm.entryType || null,
      bye: entryForm.bye
    })
    entryForm.drawPosition = null
    entryForm.bye = false
    entryForm.playerId = ''
    entryForm.seed = null
    entryForm.entryType = ''
    entryForm.newPlayer = false
    newPlayerForm.lastName = ''
    newPlayerForm.firstName = ''
    newPlayerForm.nationality = ''
    await loadDraw(drawTournament.value.id)
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors de l'ajout au tableau."
  }
}

async function removeEntry(entry) {
  const label = entry.bye ? 'ce bye' : `${entry.playerLastName} ${entry.playerFirstName ?? ''}`.trim()
  if (!confirm(`Retirer ${label} de la position ${entry.drawPosition} ?`)) return
  error.value = ''
  try {
    await api.deleteEntry(drawTournament.value.id, entry.id)
    await loadDraw(drawTournament.value.id)
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors du retrait de l'entrée."
  }
}

function openScoreEditor(match) {
  scoreEditorMatch.value = match
  scoreForm.score = match.score ?? ''
  scoreForm.winnerEntryId = match.winnerEntryId ?? null
}

async function submitScore() {
  if (!scoreForm.winnerEntryId) return
  const score = formatMatchScore(scoreForm.score) || null
  await api.recordScore(scoreEditorMatch.value.id, { score, winnerEntryId: scoreForm.winnerEntryId })
  scoreEditorMatch.value = null
  await loadDraw(drawTournament.value.id)
}

async function init() {
  await loadMain()
  await switchTab('main')
}

watch(() => props.id, init)
onMounted(init)
</script>
