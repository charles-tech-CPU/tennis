<template>
  <router-link to="/players" class="back-link">← Tous les joueurs</router-link>

  <div class="page-header player-hero">
    <div>
      <h1 class="player-name">
        {{ profile ? fullName(profile.player) : '...' }}
        <FavoriteStar v-if="profile" :player="profile.player" @update="p => (profile.player = p)" />
      </h1>
      <div v-if="profile" class="hero-meta">
        <span class="hero-chip nation-cell">
          <span v-if="countryFlagIso(profile.player.nationality)" class="fi" :class="`fi-${countryFlagIso(profile.player.nationality)}`"></span>
          {{ profile.player.nationality ?? 'Nationalité non renseignée' }}
        </span>
        <span v-if="profile.rankingPosition" class="hero-chip">
          N° {{ profile.rankingPosition }} au classement · {{ profile.rankingTotal }} pts
        </span>
        <span v-else class="hero-chip">Non classé</span>
        <span v-if="profile.player.legacySnapshotPoints != null" class="hero-chip">
          Points historiques : {{ profile.player.legacySnapshotPoints }}
        </span>
      </div>
    </div>
  </div>

  <p v-if="error" class="callout">{{ error }}</p>

  <template v-if="profile">
    <div class="profile-stats">
      <div class="card profile-stat">
        <span class="value">{{ profile.matchRecord.played }}</span>
        <span class="label">Matchs joués</span>
      </div>
      <div class="card profile-stat">
        <span class="value">{{ profile.matchRecord.wins }}</span>
        <span class="label">Victoires</span>
      </div>
      <div class="card profile-stat">
        <span class="value">{{ profile.matchRecord.losses }}</span>
        <span class="label">Défaites</span>
      </div>
      <div class="card profile-stat">
        <span class="value">{{ profile.matchRecord.winRate == null ? '—' : Math.round(profile.matchRecord.winRate * 100) + ' %' }}</span>
        <span class="label">% de victoires</span>
      </div>
      <div class="card profile-stat">
        <span class="value">{{ profile.titles.length }}</span>
        <span class="label">Titre{{ profile.titles.length > 1 ? 's' : '' }}</span>
      </div>
    </div>
    <p class="field-hint">Bilan toutes saisons confondues, qualifications comprises (les byes ne comptent pas comme des matchs joués).</p>

    <h2 class="section-title">Titres</h2>
    <div v-if="profile.titles.length" class="profile-titles">
      <router-link v-for="t in profile.titles" :key="t.tournamentId" :to="`/tournaments/${t.tournamentId}`" class="card profile-title">
        <span class="tag" :class="categoryTagClass(t.category)">{{ categoryLabel(t.category) }}</span>
        <strong>{{ t.tournamentName }}</strong>
        <span class="season">{{ t.season }}</span>
      </router-link>
    </div>
    <p v-else class="field-hint">Aucun titre pour l'instant.</p>

    <h2 class="section-title">Meilleur résultat par catégorie</h2>
    <div v-if="profile.bestByCategory.length" class="table-card">
      <table>
        <thead>
          <tr><th>Catégorie</th><th>Meilleur résultat</th><th>Dernière fois</th><th class="num">Nombre de fois</th></tr>
        </thead>
        <tbody>
          <tr v-for="b in profile.bestByCategory" :key="b.category">
            <td><span class="tag" :class="categoryTagClass(b.category)">{{ categoryLabel(b.category) }}</span></td>
            <td><strong>{{ resultLabel(b.best) }}</strong></td>
            <td><router-link :to="`/tournaments/${b.best.tournamentId}`">{{ b.best.tournamentName }}</router-link> {{ b.best.season }}</td>
            <td class="num">{{ b.times }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="field-hint">Aucun tournoi disputé.</p>

    <h2 class="section-title">Historique des tournois</h2>
    <template v-if="profile.tournaments.length">
      <div class="draw-tabs">
        <button v-for="s in seasons" :key="s" class="tab" :class="{ active: s === selectedSeason }" @click="selectedSeason = s">
          {{ s ?? 'Saison inconnue' }}
        </button>
      </div>
      <p class="season-summary">
        {{ seasonSummary.tournaments }} tournoi{{ seasonSummary.tournaments > 1 ? 's' : '' }}
        · {{ seasonSummary.wins }} V – {{ seasonSummary.losses }} D
        · {{ seasonSummary.points }} pts gagnés
      </p>
      <div v-for="t in seasonTournaments" :key="t.tournamentId" class="table-card activity-block">
        <div class="activity-header">
          <div class="activity-title">
            <router-link :to="`/tournaments/${t.tournamentId}`" class="activity-name">{{ t.tournamentName }}</router-link>
            <span class="tag" :class="categoryTagClass(t.category)">{{ categoryLabel(t.category) }}</span>
            <span v-if="t.champion" class="tag tag-gold">Vainqueur 🏆</span>
            <span v-if="t.viaQualifying" class="tag tag-neutral" title="Passé par les qualifications">Issu des qualifs</span>
            <span v-if="t.inProgress" class="tag tag-grass">en cours</span>
          </div>
          <div class="activity-meta">
            <span v-if="t.country" class="nation-cell">
              <span v-if="countryFlagIso(t.country)" class="fi" :class="`fi-${countryFlagIso(t.country)}`"></span>{{ t.country }}
            </span>
            <span>{{ t.week != null ? `Semaine ${t.week} · ` : '' }}{{ t.season }}</span>
            <span v-if="surfaceLabel(t.surface, t.indoor)">{{ surfaceLabel(t.surface, t.indoor) }}</span>
            <span v-if="t.rankingAtEntry" title="Classement figé au début du tournoi">Classement : <strong>N° {{ t.rankingAtEntry }}</strong></span>
            <span>Résultat : <strong>{{ resultLabel(t) }}</strong></span>
          </div>
        </div>
        <table v-if="t.matches.length" class="activity-table">
          <thead>
            <tr><th class="round-col">Tour</th><th>Adversaire</th><th>Score</th><th class="result-col">V/D</th></tr>
          </thead>
          <tbody>
            <tr v-for="m in t.matches" :key="m.matchId">
              <td class="round-col" :title="roundTitle(m.roundLabel)">{{ m.roundLabel ?? '—' }}</td>
              <td v-if="m.bye" class="bye-cell">Exempté (bye)</td>
              <td v-else class="nation-cell">
                <span v-if="countryFlagIso(m.opponent.nationality)" class="fi" :class="`fi-${countryFlagIso(m.opponent.nationality)}`" :title="m.opponent.nationality"></span>
                <router-link :to="`/players/${m.opponent.id}`">{{ fullName(m.opponent) }}</router-link>
                <span v-if="opponentTag(m)" class="seed">{{ opponentTag(m) }}</span>
                <span v-if="m.opponentRanking" class="opp-rank" title="Classement de l'adversaire au début du tournoi">#{{ m.opponentRanking }}</span>
              </td>
              <td class="score-cell">{{ m.bye ? '—' : (m.score || '—') }}</td>
              <td class="result-col">
                <span v-if="m.bye" class="result-mark result-bye" title="Bye">–</span>
                <span v-else-if="m.won" class="result-mark result-win" title="Victoire" aria-label="Victoire">✓</span>
                <span v-else class="result-mark result-loss" title="Défaite" aria-label="Défaite">✗</span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else class="activity-empty">Aucun match décidé pour l'instant.</p>
        <div class="activity-footer">
          Points de classement gagnés : <strong>{{ t.points }}</strong>
          <span v-if="t.inProgress" class="field-hint-inline">(minimum garanti, tournoi en cours)</span>
        </div>
      </div>
    </template>
    <p v-else class="field-hint">Aucun tournoi disputé.</p>
  </template>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import api from '../services/api'
import { categoryLabel, categoryTagClass, countryFlagIso, entryTypeShortLabel, surfaceLabel } from '../labels'
import FavoriteStar from '../components/FavoriteStar.vue'

const props = defineProps({ id: { type: String, required: true } })

const profile = ref(null)
const error = ref('')
const selectedSeason = ref(null)

// Saisons jouees, la plus recente d'abord (tournaments est deja trie ainsi par le backend).
const seasons = computed(() => [...new Set((profile.value?.tournaments ?? []).map(t => t.season))])

const seasonTournaments = computed(() =>
  (profile.value?.tournaments ?? []).filter(t => t.season === selectedSeason.value))

// Bilan de la saison affichee : les byes ne comptent pas comme des matchs joues.
const seasonSummary = computed(() => {
  const played = seasonTournaments.value.flatMap(t => t.matches).filter(m => !m.bye)
  const wins = played.filter(m => m.won).length
  return {
    tournaments: seasonTournaments.value.length,
    wins,
    losses: played.length - wins,
    points: seasonTournaments.value.reduce((sum, t) => sum + t.points, 0)
  }
})

const ROUND_NAMES = {
  F: 'Finale',
  SF: 'Demi-finale',
  QF: 'Quart de finale',
  R16: '8e de finale',
  R32: '16e de finale',
  R64: '32e de finale',
  R128: '1er tour (R128)'
}

function fullName(p) {
  return p.firstName ? `${p.firstName} ${p.lastName}` : p.lastName
}

function resultLabel(result) {
  if (result.champion) return 'Vainqueur 🏆'
  const label = result.roundLabel
  if (!label) return '—'
  if (/^Q\d+$/.test(label)) return `Qualifications (${label})`
  return ROUND_NAMES[label] ?? label
}

function roundTitle(label) {
  if (!label) return ''
  if (/^Q\d+$/.test(label)) return `Qualifications, ${label.slice(1)}e tour`
  return ROUND_NAMES[label] ?? label
}

// Comme sur le site ATP : "(3)" pour une tete de serie, "(WC)", "(Q)"... pour un statut d'entree.
function opponentTag(m) {
  const parts = [m.opponentSeed, entryTypeShortLabel(m.opponentEntryType)].filter(Boolean)
  return parts.length ? `(${parts.join(' ')})` : ''
}

async function load() {
  profile.value = null
  error.value = ''
  try {
    profile.value = await api.getPlayerProfile(props.id)
    selectedSeason.value = seasons.value[0] ?? null
  } catch (e) {
    error.value = e.response?.status === 404 ? 'Joueur introuvable.' : 'Impossible de charger la fiche du joueur.'
  }
}

// Recharge si on passe d'une fiche joueur a une autre sans quitter la vue.
watch(() => props.id, load, { immediate: true })
</script>
