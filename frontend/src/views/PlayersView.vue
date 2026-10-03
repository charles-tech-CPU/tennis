<template>
  <div class="page-header">
    <div>
      <h1>Joueurs</h1>
      <p class="subtitle">{{ filtered.length }} joueur(s)</p>
    </div>
  </div>

  <div v-if="favorites.length" class="card favorites-block">
    <h3>★ Favoris</h3>
    <div class="favorites-list">
      <span v-for="p in favorites" :key="p.id" class="favorite-chip">
        <router-link :to="`/players/${p.id}`">
          <span v-if="countryFlagIso(p.nationality)" class="fi" :class="`fi-${countryFlagIso(p.nationality)}`"></span>
          {{ p.firstName ? `${p.firstName} ${p.lastName}` : p.lastName }}
        </router-link>
        <FavoriteStar :player="p" @update="replacePlayer" />
      </span>
    </div>
  </div>

  <div class="filters">
    <input v-model="search" aria-label="Rechercher un joueur" placeholder="Rechercher un joueur..." />
  </div>

  <div v-if="filtered.length" class="table-card">
    <table>
      <thead>
        <tr><th class="star-col" aria-label="Favori"></th><th>Nom</th><th>Prénom</th><th>Nationalité</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="p in filtered" :key="p.id">
          <td class="star-col"><FavoriteStar :player="p" @update="replacePlayer" /></td>
          <template v-if="editingId === p.id">
            <td><input v-model="editForm.lastName" aria-label="Nom" required /></td>
            <td><input v-model="editForm.firstName" aria-label="Prénom" /></td>
            <td><input v-model="editForm.nationality" aria-label="Nationalité" /></td>
            <td class="actions-cell">
              <button type="button" @click="saveEdit(p.id)">Enregistrer</button>
              <button type="button" class="secondary" @click="cancelEdit">Annuler</button>
            </td>
          </template>
          <template v-else>
            <td class="name-cell"><router-link :to="`/players/${p.id}`">{{ p.lastName }}</router-link></td>
            <td><router-link :to="`/players/${p.id}`">{{ p.firstName ?? '—' }}</router-link></td>
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
    <input v-model="form.lastName" aria-label="Nom" placeholder="Nom" required />
    <input v-model="form.firstName" aria-label="Prénom" placeholder="Prénom" />
    <input v-model="form.nationality" aria-label="Nationalité" placeholder="Nationalité" />
    <button type="submit">Ajouter</button>
  </form>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import { countryFlagIso } from '../labels'
import FavoriteStar from '../components/FavoriteStar.vue'

const players = ref([])
const search = ref('')
const form = reactive({ lastName: '', firstName: '', nationality: '' })
const editingId = ref(null)
const editForm = reactive({ lastName: '', firstName: '', nationality: '' })

// Favoris en premier, l'ordre alphabetique du serveur etant conserve
// a l'interieur de chaque groupe (tri stable).
const filtered = computed(() => players.value
  .filter(p => !search.value || p.lastName.toLowerCase().includes(search.value.toLowerCase()))
  .sort((a, b) => Number(b.favorite) - Number(a.favorite))
)

const favorites = computed(() => players.value.filter(p => p.favorite))

function replacePlayer(updated) {
  players.value = players.value.map(p => (p.id === updated.id ? updated : p))
}

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
