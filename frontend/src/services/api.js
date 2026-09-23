import axios from 'axios'

const client = axios.create({ baseURL: 'http://localhost:8082/api' })

export default {
  // Joueurs
  getPlayers() {
    return client.get('/players').then(r => r.data)
  },
  createPlayer(payload) {
    return client.post('/players', payload).then(r => r.data)
  },
  updatePlayer(id, payload) {
    return client.put(`/players/${id}`, payload).then(r => r.data)
  },

  // Tournois
  getTournaments() {
    return client.get('/tournaments').then(r => r.data)
  },
  getTournament(id) {
    return client.get(`/tournaments/${id}`).then(r => r.data)
  },
  createTournament(payload) {
    return client.post('/tournaments', payload).then(r => r.data)
  },
  updateTournament(id, payload) {
    return client.put(`/tournaments/${id}`, payload).then(r => r.data)
  },
  deleteTournament(id) {
    return client.delete(`/tournaments/${id}`)
  },
  createQualifyingDraw(id, payload) {
    return client.post(`/tournaments/${id}/qualifying`, payload).then(r => r.data)
  },

  // Entrees (tableau)
  getEntries(tournamentId) {
    return client.get(`/tournaments/${tournamentId}/entries`).then(r => r.data)
  },
  createEntry(tournamentId, payload) {
    return client.post(`/tournaments/${tournamentId}/entries`, payload).then(r => r.data)
  },
  deleteEntry(tournamentId, entryId) {
    return client.delete(`/tournaments/${tournamentId}/entries/${entryId}`)
  },

  // Matchs
  getMatches(tournamentId) {
    return client.get(`/tournaments/${tournamentId}/matches`).then(r => r.data)
  },
  recordScore(matchId, payload) {
    return client.put(`/matches/${matchId}/score`, payload).then(r => r.data)
  },

  // Classement
  getRanking() {
    return client.get('/ranking').then(r => r.data)
  },

  // Stats
  getStats(season) {
    return client.get('/stats', { params: { season } }).then(r => r.data)
  },

  // Coupe Davis / United Cup (competition = DAVIS_CUP ou UNITED_CUP)
  getTeamTies(competition, season) {
    return client.get('/team-ties', { params: { competition, season } }).then(r => r.data)
  },
  updateTeamRubber(tieId, rubberOrder, payload) {
    return client.put(`/team-ties/${tieId}/rubbers/${rubberOrder}`, payload).then(r => r.data)
  },
  getTeamSeasons(competition) {
    return client.get('/team-ties/seasons', { params: { competition } }).then(r => r.data)
  },
  createTeamTie(payload) {
    return client.post('/team-ties', payload).then(r => r.data)
  },
  updateTeamTie(tieId, payload) {
    return client.put(`/team-ties/${tieId}`, payload).then(r => r.data)
  },
  deleteTeamTie(tieId) {
    return client.delete(`/team-ties/${tieId}`)
  },
  createTeamBracket(payload) {
    return client.post('/team-ties/bracket', payload).then(r => r.data)
  },
  deleteTeamBracket(competition, season) {
    return client.delete('/team-ties/bracket', { params: { competition, season } })
  }
}
