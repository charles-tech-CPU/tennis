<template>
  <!-- Le tableau ne doit jamais scroller horizontalement : la largeur des
       cartes (--match-w) s'adapte a la largeur disponible (fitMatchWidth),
       en gardant la hauteur/police a taille confortable - seul un defilement
       vertical est acceptable. Un transform:scale ne sert plus que de filet
       de securite pour les cas extremes (tres grand tableau, tres petit
       ecran) ou meme la largeur minimale ne suffirait pas. Voir
       recomputeLayout() plus bas. -->
  <div ref="viewportEl" class="bracket-viewport" :style="viewportStyle">
    <div ref="scaledEl" class="bracket-scaled" :style="scaledStyle">
      <!-- Tableau a elimination directe "classique" (1 seul vainqueur en
           finale) : deux moities miroir qui convergent vers la finale au
           centre, avec les lignes de progression du tableau papier. -->
      <div v-if="isSingleElim" class="bracket-tree" :style="{ '--match-w': `${matchW}px` }">
        <BracketTreeNode :node="leftTree" :mirror="false" @select="onClickMatch" @remove-entry="onRemoveEntry" />
        <div class="bracket-final">
          <span class="bracket-final-badge">🏆 Finale</span>
          <BracketMatchCard :match="finalMatch" @select="onClickMatch" @remove-entry="onRemoveEntry" />
        </div>
        <BracketTreeNode :node="rightTree" :mirror="true" @select="onClickMatch" @remove-entry="onRemoveEntry" />
      </div>

      <!-- Tableau de qualifications : plusieurs groupes independants, chacun
           est son propre petit tableau a elimination directe qui produit un
           qualifie (pas de finale commune, donc pas de vue miroir globale -
           mais chaque groupe garde ses lignes de progression). -->
      <div v-else class="bracket-groups">
        <div v-for="g in qualifyingGroups" :key="g.match?.id ?? g.depth" class="bracket-group">
          <BracketTreeNode :node="g" :mirror="false" @select="onClickMatch" @remove-entry="onRemoveEntry" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BracketMatchCard from './BracketMatchCard.vue'
import BracketTreeNode from './BracketTreeNode.vue'
import { buildBracketNode, fitMatchWidth, MATCH_W_MAX } from '../bracketLayout'

const props = defineProps({
  rounds: { type: Array, default: () => [] },
  matches: { type: Array, default: () => [] }
})
const emit = defineEmits(['select-match', 'remove-entry'])

const roundsByOrder = computed(() => Object.fromEntries(props.rounds.map(r => [r.roundOrder, r])))

// Chaque match est enrichi avec le libelle/bareme de son tour, pour l'afficher
// directement sur la carte (utile surtout dans la vue arbre, ou il n'y a plus
// un en-tete de colonne unique par tour).
const enrichedMatches = computed(() => props.matches.map(m => ({
  ...m,
  roundLabel: roundsByOrder.value[m.roundOrder]?.roundLabel,
  roundPoints: roundsByOrder.value[m.roundOrder]?.points
})))

const totalRounds = computed(() => props.rounds.reduce((max, r) => Math.max(max, r.roundOrder), 0))

const lastRoundMatches = computed(() =>
  enrichedMatches.value.filter(m => m.roundOrder === totalRounds.value))

// Un vrai tableau a elimination directe se termine par UNE seule finale ;
// un tableau de qualifs se termine par plusieurs vainqueurs de tour simultanes.
const isSingleElim = computed(() => totalRounds.value >= 2 && lastRoundMatches.value.length === 1)

const matchesByKey = computed(() => {
  const map = new Map()
  for (const m of enrichedMatches.value) map.set(`${m.roundOrder}-${m.positionInRound}`, m)
  return map
})

const finalMatch = computed(() => isSingleElim.value ? matchesByKey.value.get(`${totalRounds.value}-1`) ?? null : null)
const leftTree = computed(() => isSingleElim.value ? buildBracketNode(matchesByKey.value, totalRounds.value - 1, 1) : null)
const rightTree = computed(() => isSingleElim.value ? buildBracketNode(matchesByKey.value, totalRounds.value - 1, 2) : null)

// Qualifs : un mini-arbre independant par vainqueur de tour final (= par
// groupe), marque isQualifierRoot pour que sa carte affiche "Qualifie" plutot
// que son libelle de tour habituel.
const qualifyingGroups = computed(() => {
  if (isSingleElim.value || totalRounds.value < 1) return []
  return lastRoundMatches.value
    .toSorted((a, b) => a.positionInRound - b.positionInRound)
    .map(m => {
      const tree = buildBracketNode(matchesByKey.value, totalRounds.value, m.positionInRound)
      if (tree.match) tree.match = { ...tree.match, isQualifierRoot: true }
      return tree
    })
})

// --- Adaptation pour tenir sur un ecran sans scroll horizontal ---
const viewportEl = ref(null)
const scaledEl = ref(null)
const matchW = ref(MATCH_W_MAX)
const scale = ref(1)
const naturalHeight = ref(0)

async function recomputeLayout() {
  if (!viewportEl.value || !scaledEl.value) return
  const available = viewportEl.value.clientWidth
  matchW.value = isSingleElim.value
    ? fitMatchWidth(leftTree.value?.depth ?? 0, available)
    : MATCH_W_MAX

  await nextTick()
  const naturalWidth = scaledEl.value.scrollWidth
  naturalHeight.value = scaledEl.value.scrollHeight
  scale.value = (available > 0 && naturalWidth > available) ? available / naturalWidth : 1
}

const scaledStyle = computed(() => scale.value < 1
  ? { transform: `scale(${scale.value})`, transformOrigin: 'top left' }
  : {})
const viewportStyle = computed(() => scale.value < 1
  ? { height: `${naturalHeight.value * scale.value}px` }
  : {})

let resizeObserver
onMounted(() => {
  resizeObserver = new ResizeObserver(() => recomputeLayout())
  resizeObserver.observe(viewportEl.value)
  nextTick(recomputeLayout)
})
onBeforeUnmount(() => resizeObserver?.disconnect())
watch([leftTree, rightTree, qualifyingGroups], () => nextTick(recomputeLayout))

function onClickMatch(match) {
  emit('select-match', match)
}

function onRemoveEntry(entry) {
  emit('remove-entry', entry)
}
</script>
