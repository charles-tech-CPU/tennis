<template>
  <!-- Tournoi en poules (ATP Finals) : 2 poules de 4, demi-finales croisees
       (1er A - 2e B, 1er B - 2e A), finale. Voir RoundRobinService cote backend. -->
  <div class="rr">
    <div class="rr-groups">
      <div v-for="g in overview.groups" :key="g.group" class="card rr-group">
        <h3>{{ g.name }} <span class="rr-group-letter">poule {{ g.group === 1 ? 'A' : 'B' }}</span></h3>
        <table class="rr-table">
          <thead>
            <tr><th>#</th><th>Joueur</th><th title="Victoires - défaites">V-D</th><th>Sets</th><th>Jeux</th></tr>
          </thead>
          <tbody>
            <tr v-for="s in g.standings" :key="s.entry.id" :class="{ qualified: s.qualified, withdrawn: s.withdrawn }">
              <td>{{ s.rank }}</td>
              <td class="nation-cell">
                <span v-if="countryFlagIso(s.entry.playerNationality)" class="fi" :class="`fi-${countryFlagIso(s.entry.playerNationality)}`"></span>
                {{ s.entry.playerLastName }}
                <span v-if="s.entry.seed" class="tag tag-seed">{{ s.entry.seed }}</span>
                <span v-if="s.entry.drawPosition > 8" class="tag">ALT</span>
                <span v-if="s.withdrawn" class="tag tag-neutral">Forfait</span>
                <span v-if="s.qualified" class="tag tag-grass">Qualifié</span>
                <button class="slot-remove" title="Retirer du tableau" @click="emit('remove-entry', s.entry)">×</button>
              </td>
              <td>{{ s.wins }}-{{ s.losses }}</td>
              <td>{{ s.setsWon }}-{{ s.setsLost }}</td>
              <td>{{ s.gamesWon }}-{{ s.gamesLost }}</td>
            </tr>
            <tr v-if="!g.standings.length"><td colspan="5" class="muted">Positions {{ g.group === 1 ? '1 à 4' : '5 à 8' }} à remplir</td></tr>
          </tbody>
        </table>

        <div class="rr-matches">
          <div
            v-for="m in groupMatches(g.group)"
            :key="m.id"
            class="rr-match"
            :class="{ clickable: m.entry1 && m.entry2, done: m.status === 'COMPLETED' }"
            :role="m.entry1 && m.entry2 ? 'button' : undefined"
            :tabindex="m.entry1 && m.entry2 ? 0 : undefined"
            @click="select(m)"
            @keydown.enter="select(m)"
          >
            <span :class="sideClass(m, m.entry1)">{{ m.entry1?.playerLastName ?? '?' }}</span>
            <span class="vs">vs</span>
            <span :class="sideClass(m, m.entry2)">{{ m.entry2?.playerLastName ?? '?' }}</span>
            <span class="score">{{ m.score ?? '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <h2 class="section-title">Phase finale</h2>
    <div class="rr-knockout">
      <div class="rr-ko-col">
        <span class="rr-ko-label">Demi-finales <span class="pts">{{ pointsOf(2) }} pts / victoire</span></span>
        <BracketMatchCard :match="koMatch(2, 1)" @select="emit('select-match', $event)" />
        <BracketMatchCard :match="koMatch(2, 2)" @select="emit('select-match', $event)" />
      </div>
      <div class="rr-ko-col">
        <span class="rr-ko-label">🏆 Finale <span class="pts">{{ pointsOf(3) }} pts / victoire</span></span>
        <BracketMatchCard :match="koMatch(3, 1)" @select="emit('select-match', $event)" />
        <div v-if="champion" class="rr-champion">
          🏆 {{ champion.playerFirstName ?? '' }} {{ champion.playerLastName }}
        </div>
      </div>
    </div>
    <p class="field-hint">
      Points : {{ pointsOf(1) }} par victoire en poule, {{ pointsOf(2) }} pour une demi gagnée, {{ pointsOf(3) }} pour la finale gagnée
      (vainqueur invaincu : {{ 3 * pointsOf(1) + pointsOf(2) + pointsOf(3) }}). Demi-finales : 1er A - 2e B et 1er B - 2e A,
      remplies automatiquement quand une poule est terminée.
    </p>

    <div class="card" style="margin-top:16px">
      <h3>Forfait : faire entrer un remplaçant</h3>
      <p class="field-hint" style="margin-top:0">
        Le remplaçant (inscrit en position 9 ou plus) reprend les matchs de poule pas encore joués du joueur forfait ;
        les matchs déjà joués par ce dernier restent les siens. Pour un forfait sur un seul match, saisis simplement « w.o. » comme score.
      </p>
      <div class="inline">
        <select v-model.number="replacement.withdrawnEntryId" aria-label="Joueur forfait">
          <option disabled value="">Joueur forfait</option>
          <option v-for="e in groupPlayers" :key="e.id" :value="e.id">{{ e.playerLastName }} {{ e.playerFirstName ?? '' }}</option>
        </select>
        <select v-model.number="replacement.alternateEntryId" aria-label="Remplaçant">
          <option disabled value="">Remplaçant</option>
          <option v-for="e in overview.alternates" :key="e.id" :value="e.id">{{ e.playerLastName }} {{ e.playerFirstName ?? '' }} (pos. {{ e.drawPosition }})</option>
        </select>
        <button :disabled="!replacement.withdrawnEntryId || !replacement.alternateEntryId" @click="submitReplacement">Remplacer</button>
      </div>
      <p v-if="!overview.alternates.length" class="field-hint">Aucun remplaçant inscrit : ajoute-le d'abord en position 9 (ou 10).</p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import BracketMatchCard from './BracketMatchCard.vue'
import { countryFlagIso } from '../labels'

const props = defineProps({
  overview: { type: Object, required: true },
  matches: { type: Array, required: true },
  rounds: { type: Array, default: () => [] }
})
const emit = defineEmits(['select-match', 'remove-entry', 'replace'])

const replacement = reactive({ withdrawnEntryId: '', alternateEntryId: '' })

function groupMatches(group) {
  return props.matches
    .filter(m => m.roundOrder === 1 && Math.floor((m.positionInRound - 1) / 6) + 1 === group)
}

function koMatch(round, position) {
  const m = props.matches.find(x => x.roundOrder === round && x.positionInRound === position)
  return m ? { ...m, roundPoints: pointsOf(round) } : null
}

function pointsOf(round) {
  return props.rounds.find(r => r.roundOrder === round)?.points ?? 0
}

const champion = computed(() => {
  const f = koMatch(3, 1)
  if (!f?.winnerEntryId) return null
  return f.entry1?.id === f.winnerEntryId ? f.entry1 : f.entry2
})

const groupPlayers = computed(() => props.overview.groups
  .flatMap(g => g.standings)
  .filter(s => !s.withdrawn)
  .map(s => s.entry))

function sideClass(m, entry) {
  if (!m.winnerEntryId || !entry) return 'side'
  return m.winnerEntryId === entry.id ? 'side winner' : 'side loser'
}

function select(m) {
  if (m.entry1 && m.entry2) emit('select-match', m)
}

function submitReplacement() {
  emit('replace', { ...replacement })
  replacement.withdrawnEntryId = ''
  replacement.alternateEntryId = ''
}
</script>

<style scoped>
.rr-groups { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 16px; }
.rr-group h3 { display: flex; align-items: baseline; gap: 8px; }
.rr-group-letter { font-size: 12px; color: var(--text-muted); font-weight: 400; text-transform: uppercase; }
.rr-table th, .rr-table td { padding: 6px 8px; font-size: 13px; }
.rr-table tr.qualified td { background: var(--clay-soft); }
.rr-table tr.withdrawn td { color: var(--text-muted); }
.rr-table .tag { margin-left: 4px; }
.rr-table .slot-remove { margin-left: 4px; border: none; background: none; color: var(--text-muted); cursor: pointer; padding: 0 4px; }
.rr-matches { margin-top: 12px; display: flex; flex-direction: column; gap: 4px; }
.rr-match { display: grid; grid-template-columns: 1fr auto 1fr auto; gap: 8px; align-items: center; padding: 6px 10px; border: 1px solid var(--border); border-radius: 6px; font-size: 13px; }
.rr-match.clickable { cursor: pointer; }
.rr-match.clickable:not(.done) { border-style: dashed; border-color: var(--border-strong); }
.rr-match .vs { color: var(--text-muted); font-size: 11px; }
.rr-match .side.winner { font-weight: 700; }
.rr-match .side.loser { color: var(--text-muted); }
.rr-match .score { font-variant-numeric: tabular-nums; color: var(--text-muted); min-width: 90px; text-align: right; }
.rr-knockout { display: flex; gap: 32px; flex-wrap: wrap; align-items: flex-start; --match-w: 260px; }
.rr-ko-col { display: flex; flex-direction: column; gap: 12px; width: 260px; }
.rr-ko-label { font-size: 12px; text-transform: uppercase; color: var(--text-muted); font-weight: 600; }
.rr-ko-label .pts { font-weight: 400; text-transform: none; }
.rr-champion { font-weight: 700; padding: 8px 12px; border-radius: 6px; background: var(--clay-soft); }
.muted { color: var(--text-muted); }
</style>
