<template>
  <div class="page-header">
    <div>
      <h1>Face à face</h1>
      <p class="subtitle">Toutes les confrontations entre deux joueurs, tous tournois confondus</p>
    </div>
  </div>

  <div class="filters h2h-pickers">
    <div v-for="side in sides" :key="side.key" class="h2h-picker">
      <div v-if="side.selected.value" class="h2h-selected">
        <span v-if="countryFlagIso(side.selected.value.nationality)" class="fi" :class="`fi-${countryFlagIso(side.selected.value.nationality)}`"></span>
        <strong>{{ fullName(side.selected.value) }}</strong>
        <button type="button" class="secondary" @click="clear(side)">Changer</button>
      </div>
      <template v-else>
        <input
          v-model="side.search.value"
          :aria-label="side.label"
          :placeholder="side.label + '...'"
        />
        <ul v-if="side.search.value.trim()" class="h2h-suggestions">
          <li v-for="p in suggestions(side)" :key="p.id">
            <button type="button" @click="select(side, p)">
              <span v-if="countryFlagIso(p.nationality)" class="fi" :class="`fi-${countryFlagIso(p.nationality)}`"></span>
              {{ fullName(p) }}
            </button>
          </li>
          <li v-if="!suggestions(side).length" class="h2h-none">Aucun joueur trouvé</li>
        </ul>
      </template>
    </div>
  </div>

  <template v-if="h2h">
    <div class="card h2h-summary">
      <span>{{ fullName(h2h.player1) }}</span>
      <span>—</span>
      <strong>{{ winsLabel(h2h.player1Wins) }} / {{ winsLabel(h2h.player2Wins) }}</strong>
      <span>—</span>
      <span>{{ fullName(h2h.player2) }}</span>
    </div>

    <div v-if="h2h.matches.length" class="table-card">
      <table>
        <thead>
          <tr><th>Tournoi</th><th>Saison</th><th>Tour</th><th>Vainqueur</th><th>Score</th></tr>
        </thead>
        <tbody>
          <tr v-for="m in h2h.matches" :key="m.matchId">
            <td><router-link :to="`/tournaments/${m.tournamentId}`">{{ m.tournamentName }}</router-link></td>
            <td>{{ m.season }}</td>
            <td>{{ m.roundLabel ?? '—' }}</td>
            <td>{{ fullName(m.winnerPlayerId === h2h.player1.id ? h2h.player1 : h2h.player2) }}</td>
            <td>{{ m.score ?? '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="empty-state">
      <div class="icon">🎾</div>
      <p>{{ fullName(h2h.player1) }} et {{ fullName(h2h.player2) }} ne se sont encore jamais affrontés.</p>
    </div>
  </template>
  <p v-else-if="error" class="callout">{{ error }}</p>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import api from '../services/api'
import { countryFlagIso } from '../labels'

const players = ref([])
const h2h = ref(null)
const error = ref('')

function makeSide(key, label) {
  return { key, label, search: ref(''), selected: ref(null) }
}
const sides = [makeSide('p1', 'Joueur 1'), makeSide('p2', 'Joueur 2')]

// Insensible a la casse et aux accents, comme la recherche du classement.
function normalize(text) {
  return (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

function fullName(p) {
  return p.firstName ? `${p.firstName} ${p.lastName}` : p.lastName
}

function winsLabel(n) {
  return `${n} victoire${n > 1 ? 's' : ''}`
}

// L'autre joueur deja choisi est exclu : un face a face contre soi-meme n'a pas de sens.
function suggestions(side) {
  const q = normalize(side.search.value.trim())
  const otherId = sides.find(s => s !== side).selected.value?.id
  return players.value
    .filter(p => p.id !== otherId && normalize(`${p.lastName} ${p.firstName ?? ''}`).includes(q))
    .slice(0, 10)
}

function select(side, player) {
  side.selected.value = player
  side.search.value = ''
}

function clear(side) {
  side.selected.value = null
}

watch(
  () => sides.map(s => s.selected.value?.id),
  async ([id1, id2]) => {
    h2h.value = null
    error.value = ''
    if (!id1 || !id2) return
    try {
      const result = await api.getHeadToHead(id1, id2)
      // Ignore une reponse arrivee apres un changement de joueur entre-temps.
      if (sides[0].selected.value?.id === id1 && sides[1].selected.value?.id === id2) h2h.value = result
    } catch {
      error.value = 'Impossible de charger le face à face.'
    }
  }
)

onMounted(async () => {
  players.value = await api.getPlayers()
})
</script>
