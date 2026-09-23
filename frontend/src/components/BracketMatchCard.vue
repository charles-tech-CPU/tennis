<template>
  <div
    class="bracket-match"
    :class="{ completed: isDecided, clickable, pending: clickable && !isDecided }"
    :tabindex="clickable ? 0 : undefined"
    :role="clickable ? 'button' : undefined"
    @click="handleClick"
    @keydown.enter="handleClick"
  >
    <div v-if="match?.isQualifierRoot" class="bm-round-tag bm-round-tag-qualifier">✓ Qualifié</div>
    <div v-else-if="match?.roundLabel" class="bm-round-tag">
      {{ match.roundLabel }} <span class="pts">{{ match.roundPoints }}p</span>
    </div>
    <div class="bracket-slot" :class="slotClass(match?.entry1)">
      <span v-if="isFirstRound" class="slot-pos">{{ pos1 }}</span>
      <span class="name">
        <span class="fi-slot"><span v-if="flagIso(match?.entry1)" class="fi" :class="`fi-${flagIso(match?.entry1)}`"></span></span>
        {{ slotLabel(match?.entry1) }}
      </span>
      <span class="badges">
        <span v-if="match?.entry1?.seed" class="tag tag-seed">{{ match.entry1.seed }}</span>
        <span v-if="match?.entry1?.entryType" class="tag">{{ entryTypeShortLabel(match.entry1.entryType) }}</span>
      </span>
      <button v-if="isFirstRound && match?.entry1" class="slot-remove" title="Retirer du tableau" @click.stop="emit('remove-entry', match.entry1)">×</button>
    </div>
    <div class="bracket-slot" :class="slotClass(match?.entry2)">
      <span v-if="isFirstRound" class="slot-pos">{{ pos2 }}</span>
      <span class="name">
        <span class="fi-slot"><span v-if="flagIso(match?.entry2)" class="fi" :class="`fi-${flagIso(match?.entry2)}`"></span></span>
        {{ slotLabel(match?.entry2) }}
      </span>
      <span class="badges">
        <span v-if="match?.entry2?.seed" class="tag tag-seed">{{ match.entry2.seed }}</span>
        <span v-if="match?.entry2?.entryType" class="tag">{{ entryTypeShortLabel(match.entry2.entryType) }}</span>
      </span>
      <button v-if="isFirstRound && match?.entry2" class="slot-remove" title="Retirer du tableau" @click.stop="emit('remove-entry', match.entry2)">×</button>
    </div>
    <div class="bracket-score">{{ match?.score ?? '' }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { entryTypeShortLabel, countryFlagIso } from '../labels'

const props = defineProps({
  match: { type: Object, default: null }
})
const emit = defineEmits(['select', 'remove-entry'])

const isDecided = computed(() => props.match?.status === 'COMPLETED' || props.match?.status === 'BYE')
const clickable = computed(() => {
  const m = props.match
  return !!(m && m.entry1 && m.entry2 && !m.entry1.bye && !m.entry2.bye)
})

// Le numero de position dans le tableau n'a de sens qu'au 1er tour (c'est la
// que les entrees sont placees) ; au-dela, entry1/entry2 sont des vainqueurs
// qui ont avance, pas des positions de depart.
const isFirstRound = computed(() => props.match?.roundOrder === 1)
const pos1 = computed(() => props.match ? props.match.positionInRound * 2 - 1 : null)
const pos2 = computed(() => props.match ? props.match.positionInRound * 2 : null)

function slotLabel(entry) {
  if (!entry) return '?'
  if (entry.bye) return 'BYE'
  return entry.playerLastName ?? '?'
}

function flagIso(entry) {
  if (!entry || entry.bye) return ''
  return countryFlagIso(entry.playerNationality)
}

function isWinner(entry) {
  return entry && props.match?.winnerEntryId === entry.id
}

// Vainqueur mis en avant, perdant estompe - seulement une fois le match
// tranche (sinon les deux joueurs restent a egalite visuelle).
function slotClass(entry) {
  if (!props.match?.winnerEntryId || !entry) return {}
  return isWinner(entry) ? { winner: true } : { loser: true }
}

function handleClick() {
  if (clickable.value) emit('select', props.match)
}
</script>
