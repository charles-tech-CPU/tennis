<template>
  <header class="topbar">
    <div class="brand">
      <span class="brand-mark">🎾</span>
      <h1>Tennis <span>Results</span></h1>
    </div>
    <nav>
      <router-link to="/">Tournois</router-link>
      <router-link to="/ranking">Classement</router-link>
      <router-link to="/players">Joueurs</router-link>
      <router-link to="/stats">Stats</router-link>
      <router-link to="/coupe-davis">Coupe Davis</router-link>
      <router-link to="/united-cup">United Cup</router-link>
    </nav>
  </header>
  <main :class="{ wide: route.meta.wide, full: route.meta.full }">
    <router-view />
  </main>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
