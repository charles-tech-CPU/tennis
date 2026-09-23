import { createRouter, createWebHistory } from 'vue-router'
import TournamentsView from '../views/TournamentsView.vue'
import TournamentDetailView from '../views/TournamentDetailView.vue'
import PlayersView from '../views/PlayersView.vue'
import RankingView from '../views/RankingView.vue'
import StatsView from '../views/StatsView.vue'
import TeamCompetitionView from '../views/TeamCompetitionView.vue'

const routes = [
  { path: '/', component: TournamentsView },
  { path: '/tournaments/:id', component: TournamentDetailView, props: true, meta: { wide: true } },
  { path: '/players', component: PlayersView },
  { path: '/ranking', component: RankingView, meta: { full: true } },
  { path: '/stats', component: StatsView, meta: { wide: true } },
  { path: '/coupe-davis', component: TeamCompetitionView, props: { competition: 'DAVIS_CUP' }, meta: { wide: true } },
  { path: '/united-cup', component: TeamCompetitionView, props: { competition: 'UNITED_CUP' }, meta: { wide: true } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
