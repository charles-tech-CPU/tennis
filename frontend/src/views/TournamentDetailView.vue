<template>
  <p><router-link to="/">← Tous les tournois</router-link></p>
  <h1>{{ tournament?.name ?? '...' }}</h1>
  <p v-if="tournament" style="color:#9fb0c0">
    {{ tournament.category }} · Saison {{ tournament.season }}
    <span v-if="tournament.weekNumber">· Semaine {{ tournament.weekNumber }}</span>
    · Tableau {{ tournament.drawSize }} ({{ tournament.drawSlots }} cases)
    <span v-if="tournament.mandatorySlot" class="tag">{{ tournament.mandatorySlot }}</span>
  </p>

  <h2>Tableau</h2>
  <BracketView v-if="matches.length" :rounds="tournament?.rounds ?? []" :matches="matches" @select-match="openScoreEditor" />
  <p v-else-if="loaded">Ajoute des joueurs ci-dessous pour faire apparaitre le tableau du 1er tour.</p>

  <h2>Joueurs du tableau ({{ entries.length }} / {{ tournament?.drawSlots ?? '?' }})</h2>
  <table v-if="entries.length">
    <thead>
      <tr><th>Position</th><th>Joueur</th><th>Tete de serie</th><th>Statut</th><th></th></tr>
    </thead>
    <tbody>
      <tr v-for="e in entries" :key="e.id">
        <td>{{ e.drawPosition }}</td>
        <td>{{ e.bye ? 'BYE' : `${e.playerLastName} ${e.playerFirstName ?? ''}` }}</td>
        <td>{{ e.seed ?? '—' }}</td>
        <td>{{ e.entryType ?? '—' }}</td>
        <td><button class="secondary" @click="removeEntry(e)">Retirer</button></td>
      </tr>
    </tbody>
  </table>

  <form class="card inline" @submit.prevent="submitEntry">
    <input v-model.number="entryForm.drawPosition" type="number" min="1" :max="tournament?.drawSlots" placeholder="Position" required />
    <label class="inline"><input type="checkbox" v-model="entryForm.bye" /> Bye</label>
    <select v-if="!entryForm.bye" v-model.number="entryForm.playerId" required>
      <option disabled value="">Joueur</option>
      <option v-for="p in players" :key="p.id" :value="p.id">{{ p.lastName }} {{ p.firstName ?? '' }}</option>
    </select>
    <input v-if="!entryForm.bye" v-model.number="entryForm.seed" type="number" placeholder="Tete de serie" />
    <select v-if="!entryForm.bye" v-model="entryForm.entryType">
      <option value="">Entree directe</option>
      <option value="WILD_CARD">Wild card (WC)</option>
      <option value="QUALIFIER">Qualifie (Q)</option>
      <option value="LUCKY_LOSER">Lucky loser (LL)</option>
    </select>
    <button type="submit">Ajouter au tableau</button>
  </form>
  <p v-if="error" style="color:#ff6b6b">{{ error }}</p>

  <div v-if="scoreEditorMatch" class="score-editor" @click.self="scoreEditorMatch = null">
    <div class="card">
      <h3>Score du match</h3>
      <p>{{ scoreEditorMatch.entry1.playerLastName }} vs {{ scoreEditorMatch.entry2.playerLastName }}</p>
      <div class="inline" style="margin-bottom:8px">
        <label><input type="radio" :value="scoreEditorMatch.entry1.id" v-model="scoreForm.winnerEntryId" /> {{ scoreEditorMatch.entry1.playerLastName }} gagne</label>
      </div>
      <div class="inline" style="margin-bottom:8px">
        <label><input type="radio" :value="scoreEditorMatch.entry2.id" v-model="scoreForm.winnerEntryId" /> {{ scoreEditorMatch.entry2.playerLastName }} gagne</label>
      </div>
      <input v-model="scoreForm.score" placeholder="Score (ex: 6-3 6-4)" style="width:100%;margin-bottom:8px" />
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

const props = defineProps({ id: { type: [String, Number], required: true } })

const tournament = ref(null)
const entries = ref([])
const matches = ref([])
const players = ref([])
const loaded = ref(false)
const error = ref('')

const entryForm = reactive({ drawPosition: null, bye: false, playerId: '', seed: null, entryType: '' })
const scoreEditorMatch = ref(null)
const scoreForm = reactive({ score: '', winnerEntryId: null })

async function load() {
  const tid = Number(props.id)
  const [t, e, m, p] = await Promise.all([
    api.getTournament(tid), api.getEntries(tid), api.getMatches(tid), api.getPlayers()
  ])
  tournament.value = t
  entries.value = e
  matches.value = m
  players.value = p
  loaded.value = true
}

async function submitEntry() {
  error.value = ''
  try {
    await api.createEntry(Number(props.id), {
      playerId: entryForm.bye ? null : entryForm.playerId,
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
    await load()
  } catch (e) {
    error.value = e.response?.data?.error ?? "Erreur lors de l'ajout au tableau."
  }
}

async function removeEntry(entry) {
  await api.deleteEntry(Number(props.id), entry.id)
  await load()
}

function openScoreEditor(match) {
  scoreEditorMatch.value = match
  scoreForm.score = match.score ?? ''
  scoreForm.winnerEntryId = match.winnerEntryId ?? null
}

async function submitScore() {
  if (!scoreForm.winnerEntryId) return
  await api.recordScore(scoreEditorMatch.value.id, { score: scoreForm.score || null, winnerEntryId: scoreForm.winnerEntryId })
  scoreEditorMatch.value = null
  await load()
}

watch(() => props.id, load)
onMounted(load)
</script>
