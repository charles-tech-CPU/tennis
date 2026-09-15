<template>
  <div v-if="node.children" class="bn-node" :class="{ mirror }">
    <div class="bn-children" :style="{ '--conn-offset': connectorOffset }">
      <BracketTreeNode :node="node.children[0]" :mirror="mirror" @select="$emit('select', $event)" @remove-entry="$emit('remove-entry', $event)" />
      <BracketTreeNode :node="node.children[1]" :mirror="mirror" @select="$emit('select', $event)" @remove-entry="$emit('remove-entry', $event)" />
    </div>
    <div class="bn-self">
      <BracketMatchCard :match="node.match" @select="$emit('select', $event)" @remove-entry="$emit('remove-entry', $event)" />
    </div>
  </div>
  <div v-else class="bn-leaf">
    <BracketMatchCard :match="node.match" @select="$emit('select', $event)" @remove-entry="$emit('remove-entry', $event)" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BracketMatchCard from './BracketMatchCard.vue'
import { subtreeHeight } from '../bracketLayout'

const props = defineProps({
  node: { type: Object, required: true },
  mirror: { type: Boolean, default: false }
})
defineEmits(['select', 'remove-entry'])

const connectorOffset = computed(() => `${subtreeHeight(props.node.depth - 1) / 2}px`)
</script>
