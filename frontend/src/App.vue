<template>
  <header class="topbar">
    <div class="brand">
      <span class="brand-mark">🎾</span>
      <h1>Tennis Results</h1>
    </div>
    <nav>
      <router-link to="/">Tournois</router-link>
      <router-link to="/ranking">Classement</router-link>
      <router-link to="/players">Joueurs</router-link>
    </nav>
  </header>
  <main :class="{ wide: route.meta.wide }">
    <router-view />
  </main>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
