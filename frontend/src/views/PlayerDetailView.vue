<template>
  <router-link to="/players" class="back-link">← Tous les joueurs</router-link>

  <div class="page-header player-hero">
    <div>
      <h1>{{ profile ? fullName(profile.player) : '...' }}</h1>
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
        <span class="value">{{ profile.record.played }}</span>
        <span class="label">Matchs joués</span>
      </div>
      <div class="card profile-stat">
        <span class="value">{{ profile.record.wins }}</span>
        <span class="label">Victoires</span>
      </div>
      <div class="card profile-stat">
        <span class="value">{{ profile.record.losses }}</span>
        <span class="label">Défaites</span>
      </div>
      <div class="card profile-stat">
        <span class="value">{{ profile.record.winRate == null ? '—' : Math.round(profile.record.winRate * 100) + ' %' }}</span>
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
    <div v-if="profile.tournaments.length" class="table-card">
      <table>
        <thead>
          <tr><th>Saison</th><th>Semaine</th><th>Tournoi</th><th>Catégorie</th><th>Tour atteint</th><th class="num">Points</th></tr>
        </thead>
        <tbody>
          <tr v-for="t in profile.tournaments" :key="t.tournamentId">
            <td>{{ t.season }}</td>
            <td class="week-cell">{{ t.week ?? '—' }}</td>
            <td class="name-cell"><router-link :to="`/tournaments/${t.tournamentId}`">{{ t.tournamentName }}</router-link></td>
            <td><span class="tag" :class="categoryTagClass(t.category)">{{ categoryLabel(t.category) }}</span></td>
            <td>
              {{ resultLabel(t) }}
              <span v-if="t.viaQualifying" class="tag tag-neutral" title="Passé par les qualifications">Q</span>
              <span v-if="t.inProgress" class="tag tag-grass">en cours</span>
            </td>
            <td class="num">{{ t.points }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="field-hint">Aucun tournoi disputé.</p>
  </template>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../services/api'
import { categoryLabel, categoryTagClass, countryFlagIso } from '../labels'

const props = defineProps({ id: { type: String, required: true } })

const profile = ref(null)
const error = ref('')

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

async function load() {
  profile.value = null
  error.value = ''
  try {
    profile.value = await api.getPlayerProfile(props.id)
  } catch (e) {
    error.value = e.response?.status === 404 ? 'Joueur introuvable.' : 'Impossible de charger la fiche du joueur.'
  }
}

// Recharge si on passe d'une fiche joueur a une autre sans quitter la vue.
watch(() => props.id, load, { immediate: true })
</script>
