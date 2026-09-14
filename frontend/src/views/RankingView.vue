<template>
  <h2>Classement {{ season }}</h2>
  <div class="filters">
    <input v-model.number="season" type="number" />
    <button class="secondary" @click="load">Actualiser</button>
  </div>
  <p style="font-size:12px;color:#9fb0c0">
    Calcul automatique : 4 Grand Chelem + ATP Finals + 8 des 9 Masters 1000 (hors Monte-Carlo) +
    somme des 5 meilleurs autres tournois + le meilleur entre Monte-Carlo et le 6e meilleur autre tournoi.
    Seuls les tournois avec un resultat acte (joueur elimine ou vainqueur) comptent.
  </p>
  <table v-if="rows.length">
    <thead>
      <tr>
        <th>#</th><th>Joueur</th><th>Nation</th>
        <th>Obligatoires</th><th>5 autres</th><th>Remplacement/MC</th><th>Total</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(r, i) in rows" :key="r.playerId">
        <td>{{ i + 1 }}</td>
        <td>{{ r.lastName }} {{ r.firstName ?? '' }}</td>
        <td>{{ r.nationality ?? '—' }}</td>
        <td>{{ r.mandatoryTotal }}</td>
        <td>{{ r.othersTotal }}</td>
        <td>{{ r.replacementValue }}</td>
        <td><strong>{{ r.total }}</strong></td>
      </tr>
    </tbody>
  </table>
  <p v-else-if="loaded">Aucun resultat acte pour cette saison encore.</p>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const season = ref(new Date().getFullYear())
const rows = ref([])
const loaded = ref(false)

async function load() {
  rows.value = await api.getRanking(season.value)
  loaded.value = true
}

onMounted(load)
</script>
