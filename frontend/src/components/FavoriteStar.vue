<template>
  <button
    type="button"
    class="favorite-star"
    :class="{ active: player.favorite }"
    :disabled="saving"
    :aria-pressed="player.favorite"
    :aria-label="player.favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'"
    :title="player.favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'"
    @click="toggle"
  >{{ player.favorite ? '★' : '☆' }}</button>
</template>

<script setup>
import { ref } from 'vue'
import api from '../services/api'

// Bascule le favori cote serveur puis renvoie le joueur a jour au parent
// (evenement "update"), qui le remplace dans sa liste sans tout recharger.
const props = defineProps({ player: { type: Object, required: true } })
const emit = defineEmits(['update'])
const saving = ref(false)

async function toggle() {
  saving.value = true
  try {
    emit('update', await api.togglePlayerFavorite(props.player.id))
  } finally {
    saving.value = false
  }
}
</script>
