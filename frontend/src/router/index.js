import { createRouter, createWebHistory } from 'vue-router'
import TournamentsView from '../views/TournamentsView.vue'
import TournamentDetailView from '../views/TournamentDetailView.vue'
import PlayersView from '../views/PlayersView.vue'
import RankingView from '../views/RankingView.vue'

const routes = [
  { path: '/', component: TournamentsView },
  { path: '/tournaments/:id', component: TournamentDetailView, props: true },
  { path: '/players', component: PlayersView },
  { path: '/ranking', component: RankingView }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
