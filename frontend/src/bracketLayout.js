/*
 * Geometrie du tableau a elimination directe (vue "vrai bracket" avec lignes
 * de connexion). MATCH_H/ROUND_GAP/V_GAP doivent rester synchronises avec les
 * variables CSS --match-h/--round-gap/--v-gap definies sur .bracket-tree et
 * .bracket-groups (style.css) : c'est ce qui permet de positionner les
 * connecteurs en pur calcul, sans jamais mesurer le DOM.
 *
 * Seule la LARGEUR des cartes (--match-w) est dynamique (voir fitMatchWidth) :
 * on ne veut jamais de scroll horizontal, mais la hauteur/police doivent
 * rester confortables - le defilement vertical, lui, ne pose aucun probleme.
 */
export const MATCH_H = 66
export const ROUND_GAP = 20
export const V_GAP = 18
export const MATCH_W_MAX = 210
export const MATCH_W_MIN = 132

/** Hauteur totale (px) d'un sous-arbre de profondeur `depth` (0 = un seul match). */
export function subtreeHeight(depth) {
  let h = MATCH_H
  for (let i = 0; i < depth; i++) h = h * 2 + V_GAP
  return h
}

/**
 * Largeur de carte (px) qui fait tenir les 2 moities + la finale dans
 * `availableWidth`, pour des sous-arbres de profondeur `depth` (= nombre de
 * tours par moitie - 1). Le gap entre tours reste fixe (ROUND_GAP) ; seule la
 * largeur des cartes se resserre, entre MATCH_W_MIN et MATCH_W_MAX.
 */
export function fitMatchWidth(depth, availableWidth) {
  const widthUnits = 2 * depth + 3
  const gapCount = 2 * depth + 2
  const raw = (availableWidth - gapCount * ROUND_GAP) / widthUnits
  return Math.min(MATCH_W_MAX, Math.max(MATCH_W_MIN, raw))
}

/**
 * Construit recursivement le sous-arbre du tour `roundOrder` (le plus proche
 * de la finale) jusqu'au 1er tour, pour la position `positionInRound` donnee.
 * Chaque noeud interne a exactement 2 enfants (round-1, position*2-1 et *2),
 * miroir exact de l'avancement des vainqueurs cote backend (BracketService).
 */
export function buildBracketNode(matchesByKey, roundOrder, positionInRound) {
  const match = matchesByKey.get(`${roundOrder}-${positionInRound}`) ?? null
  if (roundOrder === 1) {
    return { match, children: null, depth: 0 }
  }
  const left = buildBracketNode(matchesByKey, roundOrder - 1, positionInRound * 2 - 1)
  const right = buildBracketNode(matchesByKey, roundOrder - 1, positionInRound * 2)
  return { match, children: [left, right], depth: Math.max(left.depth, right.depth) + 1 }
}
