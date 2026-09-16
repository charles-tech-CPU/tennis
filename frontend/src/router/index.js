import { createRouter, createWebHistory } from 'vue-router'
import TournamentsView from '../views/TournamentsView.vue'
import TournamentDetailView from '../views/TournamentDetailView.vue'
import PlayersView from '../views/PlayersView.vue'
import RankingView from '../views/RankingView.vue'
import StatsView from '../views/StatsView.vue'

const routes = [
  { path: '/', component: TournamentsView },
  { path: '/tournaments/:id', component: TournamentDetailView, props: true, meta: { wide: true } },
  { path: '/players', component: PlayersView },
  { path: '/ranking', component: RankingView, meta: { wide: true } },
  { path: '/stats', component: StatsView, meta: { wide: true } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
