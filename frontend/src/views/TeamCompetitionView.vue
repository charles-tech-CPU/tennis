<template>
  <div class="page-header">
    <div>
      <h1>{{ config.title }} {{ season }}</h1>
      <p class="subtitle">{{ config.subtitle }}</p>
    </div>
    <div class="actions">
      <select v-model.number="season" aria-label="Saison">
        <option v-for="s in seasonOptions" :key="s" :value="s">Saison {{ s }}</option>
      </select>
      <button class="secondary" @click="load">Actualiser</button>
    </div>
  </div>

  <div class="draw-tabs">
    <button
      v-for="p in config.phases"
      :key="p.key"
      class="tab"
      :class="{ active: phaseKey === p.key }"
      @click="phaseKey = p.key"
    >{{ p.label }}</button>
  </div>

  <p v-if="phase.note" class="callout team-note">{{ phase.note }}</p>

  <div class="phase-toolbar">
    <template v-if="phase.kind === 'bracket'">
      <button v-if="!phaseHasTies" @click="openBracketForm">Créer le tableau</button>
      <button v-else class="danger" @click="deleteBracket">Supprimer le tableau</button>
    </template>
    <button v-else @click="openTieForm(null)">Nouvelle rencontre</button>
  </div>

  <div v-if="loaded && !phaseHasTies" class="empty-state">
    <div class="icon">🏆</div>
    <p v-if="phase.kind === 'bracket'">
      Aucun tableau final pour {{ season }}. « Créer le tableau » : tu choisis les pays des 4 quarts,
      les demies et la finale sont créées en attente et se remplissent au fil des résultats.
    </p>
    <p v-else>Aucune rencontre pour {{ season }} dans cette phase. Ajoute-les avec « Nouvelle rencontre ».</p>
  </div>

  <template v-else-if="loaded">
    <!-- Tableau a elimination directe (Final 8 Coupe Davis, phase finale United Cup) -->
    <div v-if="phase.kind === 'bracket'" class="ko-bracket">
      <div v-for="col in phase.stages" :key="col.stage" class="ko-column">
        <h3 class="ko-title">{{ col.label }}</h3>
        <div class="ko-ties">
          <TieCard v-for="t in tiesOf(col.stage)" :key="t.id" :tie="t" @open="openTie" />
        </div>
      </div>
    </div>

    <!-- Poules United Cup : classement calcule + rencontres -->
    <div v-else-if="phase.kind === 'groups'" class="group-grid">
      <div v-for="g in groups" :key="g.name" class="card group-card">
        <h3 class="group-title">Poule {{ g.name }} <span>· {{ g.city }}</span></h3>
        <table class="group-table">
          <thead>
            <tr><th>#</th><th>Équipe</th><th class="num">Renc.</th><th class="num">Matchs</th><th class="num">Sets</th></tr>
          </thead>
          <tbody>
            <tr v-for="(s, i) in g.standings" :key="s.team" :class="{ qualified: i === 0 }">
              <td class="rank">{{ i + 1 }}</td>
              <td class="nation-cell">
                <span v-if="countryFlagIso(s.team)" class="fi" :class="`fi-${countryFlagIso(s.team)}`"></span>
                {{ s.team }}
              </td>
              <td class="num">{{ s.tieW }}-{{ s.tieL }}</td>
              <td class="num">{{ s.matchW }}-{{ s.matchL }}</td>
              <td class="num">{{ s.setW }}-{{ s.setL }}</td>
            </tr>
          </tbody>
        </table>
        <div class="group-ties">
          <TieCard v-for="t in g.ties" :key="t.id" :tie="t" compact @open="openTie" />
        </div>
      </div>
    </div>

    <!-- Liste de rencontres (qualifs, barrages) -->
    <div v-else class="tie-grid">
      <TieCard v-for="t in tiesOf(phase.stage)" :key="t.id" :tie="t" @open="openTie" />
    </div>
  </template>

  <!-- Detail d'une rencontre + saisie des matchs -->
  <div v-if="selected" class="score-editor" @click.self="closeTie" @keydown.esc="closeTie">
    <div class="card tie-detail">
      <div class="tie-detail-head">
        <div class="tie-detail-team" :class="{ won: selected.winner === 1 }">
          <span v-if="countryFlagIso(selected.team1)" class="fi" :class="`fi-${countryFlagIso(selected.team1)}`"></span>
          {{ selected.team1 ?? selected.team1Placeholder }}
        </div>
        <div class="tie-detail-score">{{ selected.team1Score }} – {{ selected.team2Score }}</div>
        <div class="tie-detail-team right" :class="{ won: selected.winner === 2 }">
          {{ selected.team2 ?? selected.team2Placeholder }}
          <span v-if="countryFlagIso(selected.team2)" class="fi" :class="`fi-${countryFlagIso(selected.team2)}`"></span>
        </div>
      </div>
      <p class="tie-detail-meta">{{ [selected.dates, selected.venue, selected.surface].filter(Boolean).join(' · ') }}</p>

      <div v-for="r in selected.rubbers" :key="r.order" class="rubber">
        <template v-if="editing?.order !== r.order">
          <div class="rubber-label">
            Match {{ r.order }} · {{ rubberKind(r) }}
            <span v-if="r.status === 'NOT_PLAYED'" class="tag">Non disputé</span>
            <button v-if="canEdit" class="secondary small" @click="startEdit(r)">Saisir</button>
          </div>
          <div class="rubber-line">
            <span class="rubber-players" :class="{ won: r.winner === 1 }">{{ r.team1Players ?? '—' }}</span>
            <span class="rubber-score">{{ r.score ?? (r.status === 'PENDING' ? 'à jouer' : '') }}</span>
            <span class="rubber-players right" :class="{ won: r.winner === 2 }">{{ r.team2Players ?? '—' }}</span>
          </div>
        </template>
        <form v-else class="rubber-form" @submit.prevent="saveRubber">
          <div class="rubber-label">Match {{ r.order }} · {{ rubberKind(r) }}</div>
          <div class="rubber-form-grid">
            <input v-model="editing.team1Players" aria-label="Joueur(s) équipe 1" :placeholder="`Joueur(s) ${selected.team1 ?? 'équipe 1'}`" />
            <input v-model="editing.team2Players" aria-label="Joueur(s) équipe 2" :placeholder="`Joueur(s) ${selected.team2 ?? 'équipe 2'}`" />
          </div>
          <input v-model="editing.score" aria-label="Score" placeholder="Score côté équipe 1, ex : 6-4 3-6 7-6(5)" />
          <div class="inline">
            <select v-model="editing.status" aria-label="Statut">
              <option value="PENDING">À jouer</option>
              <option value="COMPLETED">Joué</option>
              <option value="NOT_PLAYED">Non disputé</option>
            </select>
            <template v-if="editing.status === 'COMPLETED'">
              <label><input v-model="editing.winner" type="radio" :value="1" /> {{ selected.team1 ?? 'Équipe 1' }}</label>
              <label><input v-model="editing.winner" type="radio" :value="2" /> {{ selected.team2 ?? 'Équipe 2' }}</label>
            </template>
          </div>
          <p v-if="editError" class="form-error">{{ editError }}</p>
          <div class="inline rubber-actions">
            <button type="submit">Enregistrer</button>
            <button type="button" class="secondary" @click="editing = null">Annuler</button>
          </div>
        </form>
      </div>

      <div class="inline rubber-actions">
        <button v-if="!isBracketStage(selected.stage)" class="danger" @click="deleteTie(selected)">Supprimer la rencontre</button>
        <button class="secondary" @click="openTieForm(selected)">Modifier la rencontre</button>
        <button class="secondary" @click="closeTie">Fermer</button>
      </div>
    </div>
  </div>

  <!-- Creation / modification d'une rencontre (pays, poule, dates, lieu) -->
  <div v-if="tieForm" class="score-editor" @click.self="tieForm = null" @keydown.esc="tieForm = null">
    <form class="card tie-detail" @submit.prevent="saveTieForm">
      <h3>{{ tieForm.id ? 'Modifier la rencontre' : `Nouvelle rencontre · ${phase.label}` }}</h3>
      <div class="team-form-grid">
        <label class="stacked">Équipe 1 (à gauche, scores de son point de vue)
          <select v-model="tieForm.team1">
            <option value="">{{ tieForm.id ? 'À déterminer' : 'Choisir un pays' }}</option>
            <option v-for="c in COUNTRY_NAMES" :key="c" :value="c">{{ c }}</option>
          </select>
        </label>
        <label class="stacked">Équipe 2
          <select v-model="tieForm.team2">
            <option value="">{{ tieForm.id ? 'À déterminer' : 'Choisir un pays' }}</option>
            <option v-for="c in COUNTRY_NAMES" :key="c" :value="c">{{ c }}</option>
          </select>
        </label>
        <label v-if="tieForm.stage === 'GROUP'" class="stacked">Poule
          <select v-model="tieForm.groupName">
            <option v-for="g in 'ABCDEFGH'" :key="g" :value="g">Poule {{ g }}</option>
          </select>
        </label>
        <label v-if="!tieForm.id" class="stacked">Nombre de matchs
          <select v-model.number="tieForm.rubberCount">
            <option :value="5">5 (2 simples, double, 2 simples)</option>
            <option :value="3">3 (2 simples, double)</option>
          </select>
        </label>
        <label class="stacked">Dates <input v-model="tieForm.dates" placeholder="ex : 6 - 7 février 2027" /></label>
        <label class="stacked">Ville <input v-model="tieForm.city" placeholder="ex : Le Portel" /></label>
        <label class="stacked">Lieu <input v-model="tieForm.venue" placeholder="ex : Le Chaudron, Le Portel, France" /></label>
        <label class="stacked">Surface
          <select v-model="tieForm.surface">
            <option value="">—</option>
            <option v-for="s in SURFACES" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
      </div>
      <p v-if="formError" class="form-error">{{ formError }}</p>
      <div class="inline rubber-actions">
        <button type="button" class="secondary" @click="tieForm = null">Annuler</button>
        <button type="submit">{{ tieForm.id ? 'Enregistrer' : 'Créer la rencontre' }}</button>
      </div>
    </form>
  </div>

  <!-- Creation du tableau final (4 quarts, demies et finale en attente) -->
  <div v-if="bracketForm" class="score-editor" @click.self="bracketForm = null" @keydown.esc="bracketForm = null">
    <form class="card tie-detail bracket-form" @submit.prevent="saveBracketForm">
      <h3>Créer le tableau · {{ phase.label }} {{ season }}</h3>
      <p class="field-hint">Dans l'ordre du tableau : les vainqueurs des quarts 1 et 2 se retrouvent en demi-finale 1, ceux des quarts 3 et 4 en demi-finale 2.</p>
      <div v-for="(q, i) in bracketForm.quarters" :key="i" class="quarter-row">
        <span class="quarter-label">QF{{ i + 1 }}</span>
        <select v-model="q.team1" aria-label="Équipe 1">
          <option value="">Pays</option>
          <option v-for="c in COUNTRY_NAMES" :key="c" :value="c">{{ c }}</option>
        </select>
        <span>vs</span>
        <select v-model="q.team2" aria-label="Équipe 2">
          <option value="">Pays</option>
          <option v-for="c in COUNTRY_NAMES" :key="c" :value="c">{{ c }}</option>
        </select>
        <input v-model="q.dates" aria-label="Date" placeholder="Date (facultatif)" />
      </div>
      <div class="team-form-grid">
        <label class="stacked">Dates de l'événement <input v-model="bracketForm.dates" placeholder="ex : 23 - 28 novembre 2027" /></label>
        <label class="stacked">Ville <input v-model="bracketForm.city" /></label>
        <label class="stacked">Lieu <input v-model="bracketForm.venue" /></label>
        <label class="stacked">Surface
          <select v-model="bracketForm.surface">
            <option value="">—</option>
            <option v-for="s in SURFACES" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
        <label class="stacked">Matchs par rencontre
          <select v-model.number="bracketForm.rubberCount">
            <option :value="3">3 (2 simples, double)</option>
            <option :value="5">5 (2 simples, double, 2 simples)</option>
          </select>
        </label>
      </div>
      <p v-if="formError" class="form-error">{{ formError }}</p>
      <div class="inline rubber-actions">
        <button type="button" class="secondary" @click="bracketForm = null">Annuler</button>
        <button type="submit">Créer le tableau</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import { countryFlagIso, COUNTRY_NAMES } from '../labels'
import TieCard from '../components/TieCard.vue'

const props = defineProps({ competition: { type: String, required: true } })

const SURFACES = ['Dur (intérieur)', 'Dur (extérieur)', 'Terre battue (intérieur)', 'Terre battue (extérieur)', 'Gazon (extérieur)']

const season = ref(2026)
const seasons = ref([])
// Saisons deja en base + la suivante, pour pouvoir commencer une nouvelle annee.
const seasonOptions = computed(() => {
  const set = new Set([...seasons.value, season.value])
  set.add(Math.max(...set) + 1)
  return [...set].sort((a, b) => b - a)
})

const CONFIGS = {
  DAVIS_CUP: {
    title: 'Coupe Davis',
    subtitle: 'Rencontres par nations - qualifications, barrages du Groupe mondial I et Final 8.',
    phases: [
      {
        key: 'finals', label: 'Final 8', kind: 'bracket',
        note: 'Final 8 de novembre : le pays hôte et les 7 vainqueurs du 2e tour des qualifications. Rencontres en 3 matchs (2 simples + 1 double) ; le vainqueur passe automatiquement au tour suivant dès que tu saisis ses résultats.',
        stages: [
          { stage: 'FINALS_QF', label: 'Quarts de finale' },
          { stage: 'FINALS_SF', label: 'Demi-finales' },
          { stage: 'FINALS_F', label: 'Finale' }
        ]
      },
      { key: 'r2', label: 'Qualifs · 2e tour', kind: 'list', stage: 'QUALIFIERS_R2', note: 'Septembre : les 7 vainqueurs se qualifient pour le Final 8.' },
      { key: 'r1', label: 'Qualifs · 1er tour', kind: 'list', stage: 'QUALIFIERS_R1', note: 'Février : les vainqueurs accèdent au 2e tour des qualifications.' },
      { key: 'wg1', label: 'Groupe mondial I · barrages', kind: 'list', stage: 'WORLD_GROUP_I_PO', note: 'Barrages du Groupe mondial I disputés en février.' }
    ]
  },
  UNITED_CUP: {
    title: 'United Cup',
    subtitle: 'Compétition mixte par nations de début janvier : un simple messieurs, un simple dames et un double mixte par rencontre.',
    phases: [
      { key: 'groups', label: 'Poules', kind: 'groups', note: '6 poules de 3 : les premiers de poule et les 2 meilleurs deuxièmes (un par ville) rejoignent les quarts de finale.' },
      {
        key: 'ko', label: 'Phase finale', kind: 'bracket',
        stages: [
          { stage: 'QF', label: 'Quarts de finale' },
          { stage: 'SF', label: 'Demi-finales' },
          { stage: 'F', label: 'Finale' }
        ]
      }
    ]
  }
}

const ties = ref([])
const loaded = ref(false)
const phaseKey = ref('')
const selectedId = ref(null)
const editing = ref(null)
const editError = ref('')
const tieForm = ref(null)
const bracketForm = ref(null)
const formError = ref('')

const config = computed(() => CONFIGS[props.competition])
const phase = computed(() => config.value.phases.find(p => p.key === phaseKey.value) ?? config.value.phases[0])
const selected = computed(() => ties.value.find(t => t.id === selectedId.value) ?? null)
// Saisie possible des qu'une rencontre a ses deux equipes (Final 8 a venir, corrections).
const canEdit = computed(() => selected.value && selected.value.team1 && selected.value.team2)

// Etapes de la phase affichee (une seule pour les qualifs, 3 pour un tableau final).
function phaseStages(p) {
  if (p.kind === 'bracket') return p.stages.map(s => s.stage)
  return [p.kind === 'groups' ? 'GROUP' : p.stage]
}

const phaseHasTies = computed(() => {
  const stages = phaseStages(phase.value)
  return ties.value.some(t => stages.includes(t.stage))
})

function isBracketStage(stage) {
  return config.value.phases.some(p => p.kind === 'bracket' && phaseStages(p).includes(stage))
}

function openTieForm(tie) {
  formError.value = ''
  if (tie) {
    tieForm.value = {
      id: tie.id, stage: tie.stage, team1: tie.team1 ?? '', team2: tie.team2 ?? '',
      groupName: tie.groupName ?? 'A', dates: tie.dates ?? '', city: tie.city ?? '',
      venue: tie.venue ?? '', surface: tie.surface ?? ''
    }
    return
  }
  const stage = phaseStages(phase.value)[0]
  tieForm.value = {
    id: null, stage, team1: '', team2: '', groupName: 'A', dates: '', city: '', venue: '', surface: '',
    // Coupe Davis (qualifs, barrages) : 5 matchs ; United Cup : 3.
    rubberCount: props.competition === 'DAVIS_CUP' ? 5 : 3
  }
}

async function saveTieForm() {
  formError.value = ''
  const f = tieForm.value
  try {
    if (f.id) {
      await api.updateTeamTie(f.id, {
        team1: f.team1, team2: f.team2, groupName: f.groupName,
        dates: f.dates, city: f.city, venue: f.venue, surface: f.surface
      })
    } else {
      await api.createTeamTie({
        competition: props.competition, season: season.value, stage: f.stage,
        groupName: f.stage === 'GROUP' ? f.groupName : null,
        team1: f.team1, team2: f.team2, dates: f.dates, city: f.city, venue: f.venue,
        surface: f.surface, rubberCount: f.rubberCount
      })
    }
    tieForm.value = null
    await load()
  } catch (e) {
    formError.value = e.response?.data?.error ?? 'Enregistrement impossible (vérifie les deux pays).'
  }
}

async function deleteTie(tie) {
  if (!window.confirm(`Supprimer la rencontre ${tie.team1 ?? '?'} - ${tie.team2 ?? '?'} et tous ses matchs ?`)) return
  try {
    await api.deleteTeamTie(tie.id)
    closeTie()
    await load()
  } catch (e) {
    editError.value = e.response?.data?.error ?? 'Suppression impossible.'
  }
}

function openBracketForm() {
  formError.value = ''
  bracketForm.value = {
    quarters: [1, 2, 3, 4].map(() => ({ team1: '', team2: '', dates: '' })),
    dates: '', city: '', venue: '', surface: '', rubberCount: 3
  }
}

async function saveBracketForm() {
  formError.value = ''
  try {
    await api.createTeamBracket({ competition: props.competition, season: season.value, ...bracketForm.value })
    bracketForm.value = null
    await load()
  } catch (e) {
    formError.value = e.response?.data?.error ?? 'Création impossible (vérifie les pays des 4 quarts).'
  }
}

async function deleteBracket() {
  if (!window.confirm(`Supprimer tout le tableau final ${season.value} (quarts, demies, finale) et leurs résultats ?`)) return
  await api.deleteTeamBracket(props.competition, season.value)
  await load()
}

function tiesOf(stage) {
  return ties.value.filter(t => t.stage === stage).sort((a, b) => a.position - b.position)
}

function rubberKind(r) {
  if (!r.doubles) return 'Simple'
  return props.competition === 'UNITED_CUP' ? 'Double mixte' : 'Double'
}

// Jeux et sets d'un score "6-4 6-7(5) 10-8 ab." du point de vue equipe 1. Un
// super tie-break (10 points) compte comme un set.
function setsOf(score) {
  return (score ?? '').split(' ')
    .map(s => s.match(/^(\d+)-(\d+)/))
    .filter(Boolean)
    .map(m => [Number(m[1]), Number(m[2])])
}

// a = ligne de l'equipe 1, b = ligne de l'equipe 2 du match r (termine).
function addRubber(a, b, r) {
  if (r.winner === 1) { a.matchW++; b.matchL++ } else { b.matchW++; a.matchL++ }
  for (const [g1, g2] of setsOf(r.score)) addSet(a, b, g1, g2)
}

// Un super tie-break (10 points) compte comme un seul jeu, pour son vainqueur.
function addSet(a, b, g1, g2) {
  if (g1 > g2) { a.setW++; b.setL++ } else if (g2 > g1) { b.setW++; a.setL++ }
  const superTb = g1 >= 10 || g2 >= 10
  const games1 = superTb ? Number(g1 > g2) : g1
  const games2 = superTb ? Number(g2 > g1) : g2
  a.gameW += games1
  a.gameL += games2
  b.gameW += games2
  b.gameL += games1
}

// Classement de poule : rencontres gagnees, puis matchs, puis % de sets et de
// jeux (meme ordre de departage que le site officiel).
function standingsOf(groupTies) {
  const rows = new Map()
  const row = team => {
    if (!rows.has(team)) rows.set(team, { team, tieW: 0, tieL: 0, matchW: 0, matchL: 0, setW: 0, setL: 0, gameW: 0, gameL: 0 })
    return rows.get(team)
  }
  for (const t of groupTies) {
    const a = row(t.team1)
    const b = row(t.team2)
    if (t.winner === 1) { a.tieW++; b.tieL++ } else if (t.winner === 2) { b.tieW++; a.tieL++ }
    for (const r of t.rubbers.filter(r => r.status === 'COMPLETED')) addRubber(a, b, r)
  }
  const pct = (w, l) => (w + l ? w / (w + l) : 0)
  return [...rows.values()].sort((x, y) =>
    y.tieW - x.tieW || y.matchW - x.matchW
    || pct(y.setW, y.setL) - pct(x.setW, x.setL) || pct(y.gameW, y.gameL) - pct(x.gameW, x.gameL))
}

const groups = computed(() => {
  const byName = new Map()
  for (const t of ties.value.filter(t => t.stage === 'GROUP')) {
    if (!byName.has(t.groupName)) byName.set(t.groupName, [])
    byName.get(t.groupName).push(t)
  }
  return [...byName.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([name, list]) => ({
    name,
    city: list[0].city,
    ties: list.sort((a, b) => a.position - b.position),
    standings: standingsOf(list)
  }))
})

function openTie(tie) {
  selectedId.value = tie.id
  editing.value = null
}

function closeTie() {
  selectedId.value = null
  editing.value = null
}

function startEdit(r) {
  editError.value = ''
  editing.value = {
    order: r.order,
    team1Players: r.team1Players ?? '',
    team2Players: r.team2Players ?? '',
    score: r.score ?? '',
    winner: r.winner,
    status: r.status === 'PENDING' ? 'COMPLETED' : r.status
  }
}

async function saveRubber() {
  editError.value = ''
  try {
    const { order, ...payload } = editing.value
    const updated = await api.updateTeamRubber(selected.value.id, order, payload)
    editing.value = null
    // Le vainqueur a pu etre reporte dans la rencontre du tour suivant : on recharge tout.
    await load()
    selectedId.value = updated.id
  } catch (e) {
    editError.value = e.response?.data?.error ?? 'Enregistrement impossible.'
  }
}

async function load() {
  ties.value = await api.getTeamTies(props.competition, season.value)
  seasons.value = await api.getTeamSeasons(props.competition)
  loaded.value = true
}

watch(() => props.competition, async () => {
  phaseKey.value = config.value.phases[0].key
  closeTie()
  // Ouvre sur la derniere saison saisie.
  const known = await api.getTeamSeasons(props.competition)
  const target = known.length ? Math.max(...known) : new Date().getFullYear()
  if (target === season.value) load()
  else season.value = target // le watch(season) recharge
}, { immediate: true })

watch(season, () => {
  closeTie()
  load()
})

onMounted(() => {
  if (!phaseKey.value) phaseKey.value = config.value.phases[0].key
})
</script>
