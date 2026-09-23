<template>
  <div class="page-header">
    <div>
      <h1>Stats {{ season }}</h1>
      <p class="subtitle">Statistiques de la saison, alimentées au fur et à mesure</p>
    </div>
    <div class="actions">
      <input v-model.number="season" aria-label="Saison" type="number" style="width:100px" />
      <button class="secondary" @click="load">Actualiser</button>
    </div>
  </div>

  <div class="stats-grid">
    <div class="table-card">
      <h2 class="section-title">Le plus de tournois gagnés</h2>
      <table v-if="stats?.topTournamentWinners?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Titres</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topTournamentWinners" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun titre acté pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Le plus de matchs gagnés</h2>
      <table v-if="stats?.topMatchWinners?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Victoires</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topMatchWinners" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun match acté pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Le plus de titres en Grand Chelem</h2>
      <table v-if="stats?.topGrandSlamWinners?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Titres</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topGrandSlamWinners" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun titre du Grand Chelem acté pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Le plus de titres en Masters 1000</h2>
      <table v-if="stats?.topMasters1000Winners?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Titres</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topMasters1000Winners" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun titre Masters 1000 acté pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Nations les plus titrées</h2>
      <table v-if="stats?.topNationsByTitles?.length">
        <thead><tr><th>#</th><th>Nation</th><th class="num">Titres</th></tr></thead>
        <tbody>
          <tr v-for="(n, i) in stats.topNationsByTitles" :key="n.nationality">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td class="nation-cell"><flag :nat="n.nationality" /></td>
            <td class="num">{{ n.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun titre acté pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Le plus de finales perdues</h2>
      <table v-if="stats?.topRunnersUp?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Finales perdues</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topRunnersUp" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucune finale perdue actée pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Meilleur % de victoires (min. 10 matchs)</h2>
      <table v-if="stats?.topWinRate?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">V-D</th><th class="num">%</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topWinRate" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.wins }}-{{ p.losses }}</td>
            <td class="num">{{ Math.round(p.winRate * 100) }}%</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Pas assez de matchs actés pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Plus longue série de victoires</h2>
      <table v-if="stats?.longestWinStreaks?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Série</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.longestWinStreaks" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.streakLength }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun match acté pour cette saison encore.</p>
    </div>

    <div class="table-card wide">
      <h2 class="section-title">Meilleur parcours issu des qualifs</h2>
      <table v-if="stats?.bestQualifierRuns?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th>Tournoi</th><th class="num">Parcours</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.bestQualifierRuns" :key="p.playerId + p.tournamentName">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td>{{ p.tournamentName }}</td>
            <td class="num">{{ p.roundReached }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun qualifié n'a encore gagné de match cette saison.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Le plus de "bagels" infligés (sets 6-0)</h2>
      <table v-if="stats?.topBagelsInflicted?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Bagels</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topBagelsInflicted" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucun bagel acté pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Le plus de victoires à la distance (3 ou 5 sets)</h2>
      <table v-if="stats?.topEpicWins?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Victoires</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.topEpicWins" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucune victoire à la distance actée pour cette saison encore.</p>
    </div>

    <div class="table-card">
      <h2 class="section-title">Joueur le plus actif</h2>
      <table v-if="stats?.mostActivePlayers?.length">
        <thead><tr><th>#</th><th>Joueur</th><th>Nation</th><th class="num">Tournois</th></tr></thead>
        <tbody>
          <tr v-for="(p, i) in stats.mostActivePlayers" :key="p.playerId">
            <td class="rank-cell"><span class="rank-badge">{{ i + 1 }}</span></td>
            <td>{{ p.lastName }} {{ p.firstName ?? '' }}</td>
            <td class="nation-cell"><flag :nat="p.nationality" /></td>
            <td class="num">{{ p.count }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else-if="loaded" class="subtitle">Aucune inscription actée pour cette saison encore.</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, h } from 'vue'
import api from '../services/api'
import { countryFlagIso } from '../labels'

const season = ref(new Date().getFullYear())
const stats = ref(null)
const loaded = ref(false)

// Petit composant local : évite de répéter le balisage drapeau + libellé partout.
const flag = {
  props: ['nat'],
  setup(props) {
    return () => {
      const iso = countryFlagIso(props.nat)
      return h('span', [
        iso ? h('span', { class: `fi fi-${iso}` }) : null,
        ' ' + (props.nat ?? '—')
      ])
    }
  }
}

async function load() {
  stats.value = await api.getStats(season.value)
  loaded.value = true
}

onMounted(load)
</script>

