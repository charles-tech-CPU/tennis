<template>
  <div class="bracket-scroll">
    <div class="bracket">
      <div v-for="round in roundsWithMatches" :key="round.roundOrder" class="bracket-round">
        <div class="bracket-round-title">{{ round.roundLabel }} · {{ round.points }} pts</div>
        <div
          v-for="m in round.matches"
          :key="m.id"
          class="bracket-match"
          :class="{ completed: m.status === 'COMPLETED' || m.status === 'BYE' }"
          @click="onClickMatch(m)"
        >
          <div class="bracket-slot" :class="{ winner: isWinner(m, m.entry1) }">
            <span class="name">
              <span v-if="m.entry1?.seed" class="seed">[{{ m.entry1.seed }}]</span>
              {{ slotLabel(m.entry1) }}
            </span>
            <span v-if="m.entry1?.entryType" class="tag">{{ shortTag(m.entry1.entryType) }}</span>
          </div>
          <div class="bracket-slot" :class="{ winner: isWinner(m, m.entry2) }">
            <span class="name">
              <span v-if="m.entry2?.seed" class="seed">[{{ m.entry2.seed }}]</span>
              {{ slotLabel(m.entry2) }}
            </span>
            <span v-if="m.entry2?.entryType" class="tag">{{ shortTag(m.entry2.entryType) }}</span>
          </div>
          <div v-if="m.score" style="padding:4px 8px;font-size:12px;color:#9fb0c0">{{ m.score }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  rounds: { type: Array, default: () => [] },
  matches: { type: Array, default: () => [] }
})
const emit = defineEmits(['select-match'])

const roundsWithMatches = computed(() => props.rounds.map(r => ({
  ...r,
  matches: props.matches
    .filter(m => m.roundOrder === r.roundOrder)
    .sort((a, b) => a.positionInRound - b.positionInRound)
})))

function slotLabel(entry) {
  if (!entry) return '?'
  if (entry.bye) return 'BYE'
  return entry.playerLastName ?? '?'
}

function shortTag(type) {
  return { WILD_CARD: 'WC', QUALIFIER: 'Q', LUCKY_LOSER: 'LL', DIRECT: '' }[type] ?? ''
}

function isWinner(match, entry) {
  return entry && match.winnerEntryId === entry.id
}

function onClickMatch(match) {
  if (match.entry1 && match.entry2 && !match.entry1.bye && !match.entry2.bye) {
    emit('select-match', match)
  }
}
</script>
