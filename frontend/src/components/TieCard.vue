<template>
  <button type="button" class="tie-card" :class="{ compact, pending: !played }" @click="$emit('open', tie)">
    <div v-for="side in [1, 2]" :key="side" class="tie-row" :class="{ won: tie.winner === side, lost: tie.winner && tie.winner !== side }">
      <span class="tie-team">
        <span v-if="countryFlagIso(team(side))" class="fi" :class="`fi-${countryFlagIso(team(side))}`"></span>
        <span :class="{ placeholder: !team(side) }">{{ team(side) ?? placeholder(side) ?? 'À déterminer' }}</span>
      </span>
      <span v-if="played" class="tie-score">{{ side === 1 ? tie.team1Score : tie.team2Score }}</span>
    </div>
    <div v-if="!compact" class="tie-meta">{{ [tie.dates, tie.city].filter(Boolean).join(' · ') }}</div>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { countryFlagIso } from '../labels'

const props = defineProps({
  tie: { type: Object, required: true },
  compact: { type: Boolean, default: false }
})
defineEmits(['open'])

// Score affiche des qu'au moins un match a ete joue (rencontre en cours ou terminee).
const played = computed(() => props.tie.rubbers.some(r => r.status === 'COMPLETED'))

function team(side) {
  return side === 1 ? props.tie.team1 : props.tie.team2
}

function placeholder(side) {
  return side === 1 ? props.tie.team1Placeholder : props.tie.team2Placeholder
}
</script>
