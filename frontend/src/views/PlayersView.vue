<template>
  <h2>Joueurs</h2>
  <input v-model="search" placeholder="Rechercher un joueur..." style="margin-bottom:12px" />
  <p>{{ filtered.length }} joueur(s)</p>
  <table v-if="filtered.length">
    <thead>
      <tr><th>Nom</th><th>Prenom</th><th>Nationalite</th><th>Points importés (photo Excel)</th></tr>
    </thead>
    <tbody>
      <tr v-for="p in filtered" :key="p.id">
        <td>{{ p.lastName }}</td>
        <td>{{ p.firstName ?? '—' }}</td>
        <td>{{ p.nationality ?? '—' }}</td>
        <td>{{ p.legacySnapshotPoints ?? '—' }}</td>
      </tr>
    </tbody>
  </table>

  <h3>Ajouter un joueur</h3>
  <form class="card inline" @submit.prevent="submit">
    <input v-model="form.lastName" placeholder="Nom" required />
    <input v-model="form.firstName" placeholder="Prenom" />
    <input v-model="form.nationality" placeholder="Nationalite" />
    <button type="submit">Ajouter</button>
  </form>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'

const players = ref([])
const search = ref('')
const form = reactive({ lastName: '', firstName: '', nationality: '' })

const filtered = computed(() => players.value.filter(p =>
  !search.value || p.lastName.toLowerCase().includes(search.value.toLowerCase())
))

async function load() {
  players.value = await api.getPlayers()
}

async function submit() {
  await api.createPlayer({ ...form })
  form.lastName = ''
  form.firstName = ''
  form.nationality = ''
  await load()
}

onMounted(load)
</script>
