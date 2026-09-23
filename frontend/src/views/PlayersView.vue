<template>
  <div class="page-header">
    <div>
      <h1>Joueurs</h1>
      <p class="subtitle">{{ filtered.length }} joueur(s)</p>
    </div>
  </div>

  <div class="filters">
    <input v-model="search" placeholder="Rechercher un joueur..." />
  </div>

  <div v-if="filtered.length" class="table-card">
    <table>
      <thead>
        <tr><th>Nom</th><th>Prénom</th><th>Nationalité</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="p in filtered" :key="p.id">
          <template v-if="editingId === p.id">
            <td><input v-model="editForm.lastName" required /></td>
            <td><input v-model="editForm.firstName" /></td>
            <td><input v-model="editForm.nationality" /></td>
            <td class="actions-cell">
              <button type="button" @click="saveEdit(p.id)">Enregistrer</button>
              <button type="button" class="secondary" @click="cancelEdit">Annuler</button>
            </td>
          </template>
          <template v-else>
            <td>{{ p.lastName }}</td>
            <td>{{ p.firstName ?? '—' }}</td>
            <td class="nation-cell">
              <span v-if="countryFlagIso(p.nationality)" class="fi" :class="`fi-${countryFlagIso(p.nationality)}`"></span>
              {{ p.nationality ?? '—' }}
            </td>
            <td class="actions-cell">
              <button type="button" class="secondary" @click="startEdit(p)">Modifier</button>
            </td>
          </template>
        </tr>
      </tbody>
    </table>
  </div>
  <div v-else class="empty-state">
    <div class="icon">🎾</div>
    <p>Aucun joueur ne correspond à la recherche.</p>
  </div>

  <h2 class="section-title">Ajouter un joueur</h2>
  <form class="card inline" @submit.prevent="submit">
    <input v-model="form.lastName" placeholder="Nom" required />
    <input v-model="form.firstName" placeholder="Prénom" />
    <input v-model="form.nationality" placeholder="Nationalité" />
    <button type="submit">Ajouter</button>
  </form>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import { countryFlagIso } from '../labels'

const players = ref([])
const search = ref('')
const form = reactive({ lastName: '', firstName: '', nationality: '' })
const editingId = ref(null)
const editForm = reactive({ lastName: '', firstName: '', nationality: '' })

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

function startEdit(p) {
  editingId.value = p.id
  editForm.lastName = p.lastName
  editForm.firstName = p.firstName ?? ''
  editForm.nationality = p.nationality ?? ''
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(id) {
  await api.updatePlayer(id, { ...editForm })
  editingId.value = null
  await load()
}

onMounted(load)
</script>
