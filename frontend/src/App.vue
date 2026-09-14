<template>
  <header class="topbar">
    <h1>🎾 Tennis Results</h1>
    <nav>
      <router-link to="/">Tournois</router-link>
      <router-link to="/ranking">Classement</router-link>
      <router-link to="/players">Joueurs</router-link>
    </nav>
  </header>
  <main>
    <router-view />
  </main>
</template>

<script setup>
</script>
