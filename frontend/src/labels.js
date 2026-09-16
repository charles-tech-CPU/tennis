export const CATEGORY_LABELS = {
  GRAND_SLAM: 'Grand Chelem',
  MASTERS_1000: 'Masters 1000',
  ATP_500: 'ATP 500',
  ATP_250: 'ATP 250',
  ATP_175: 'ATP 175',
  ATP_125: 'ATP 125',
  ATP_100: 'ATP 100',
  ATP_75: 'ATP 75',
  ATP_50: 'ATP 50'
}

export const CATEGORY_TAG_CLASS = {
  GRAND_SLAM: 'tag-gold',
  MASTERS_1000: 'tag-clay',
  ATP_500: 'tag-blue',
  ATP_250: 'tag-teal',
  ATP_175: 'tag-purple',
  ATP_125: 'tag-rose',
  ATP_100: 'tag-olive',
  ATP_75: 'tag-slate',
  ATP_50: 'tag-neutral'
}

export function categoryLabel(category) {
  return CATEGORY_LABELS[category] ?? category
}

export function categoryTagClass(category) {
  return CATEGORY_TAG_CLASS[category] ?? ''
}

export const MANDATORY_SLOT_LABELS = {
  AUSTRALIAN_OPEN: "Open d'Australie",
  ROLAND_GARROS: 'Roland-Garros',
  WIMBLEDON: 'Wimbledon',
  US_OPEN: 'US Open',
  ATP_FINALS: 'ATP Finals',
  INDIAN_WELLS: 'Indian Wells',
  MIAMI: 'Miami',
  MONTE_CARLO: 'Monte-Carlo',
  MADRID: 'Madrid',
  ROME: 'Rome',
  CANADA: 'Canada',
  CINCINNATI: 'Cincinnati',
  SHANGHAI: 'Shanghai',
  PARIS_BERCY: 'Paris-Bercy'
}

export function mandatorySlotLabel(slot) {
  if (!slot) return null
  return MANDATORY_SLOT_LABELS[slot] ?? slot
}

export const ENTRY_TYPE_SHORT_LABELS = {
  WILD_CARD: 'WC',
  QUALIFIER: 'Q',
  LUCKY_LOSER: 'LL',
  ALTERNATE: 'ALT',
  NEW_GENERATION: 'NG',
  SPECIAL_EXEMPT: 'SE',
  PROTECTED_RANKING: 'PR',
  CUT_OFF: 'CO'
}

export const ENTRY_TYPE_LABELS = {
  WILD_CARD: 'Wild card',
  QUALIFIER: 'Qualifié',
  LUCKY_LOSER: 'Lucky loser',
  ALTERNATE: 'Remplaçant (ALT)',
  NEW_GENERATION: 'New Generation',
  SPECIAL_EXEMPT: 'Special exempt',
  PROTECTED_RANKING: 'Classement protégé (PR)',
  CUT_OFF: 'Repêché / cut-off (CO)'
}

export function entryTypeShortLabel(type) {
  return ENTRY_TYPE_SHORT_LABELS[type] ?? ''
}

export function entryTypeLabel(type) {
  return ENTRY_TYPE_LABELS[type] ?? type
}

/*
 * Codes ISO 3166-1 alpha-2 (utilises par le package "flag-icons" pour afficher
 * un vrai drapeau SVG - contrairement aux emoji drapeaux, illisibles sur beaucoup
 * de configurations Windows faute de glyphes dans la police systeme).
 */
const COUNTRY_ISO = {
  'AFRIQUE SUD': 'za', 'ALGERIE': 'dz', 'ALLEMAGNE': 'de', 'ANGLETERRE': 'gb-eng',
  'ARGENTINE': 'ar', 'ARUBA': 'aw', 'AUSTRALIE': 'au', 'AUTRICHE': 'at',
  'BAHAMAS': 'bs', 'BAHREIN': 'bh', 'BARBADE': 'bb', 'BELGIQUE': 'be', 'BIELORUSSIE': 'by',
  'BOLIVIE': 'bo', 'BOSNIE': 'ba', 'BRESIL': 'br', 'BULGARIE': 'bg',
  'BURUNDI': 'bi', 'CANADA': 'ca', 'CHILI': 'cl', 'CHINE': 'cn',
  'CHYPRE': 'cy', 'COLOMBIE': 'co', 'COREE': 'kr', 'COSTA RICA': 'cr',
  'COTE IVOIRE': 'ci', 'CROATIE': 'hr', 'DANEMARK': 'dk', 'DOMINIQUE': 'dm',
  'DOMINQUE': 'dm', 'EAU': 'ae', 'ECOSSE': 'gb-sct', 'EGYPTE': 'eg', 'EQUATEUR': 'ec',
  'ESPAGNE': 'es', 'ESTONIE': 'ee', 'FINLANDE': 'fi', 'FRANCE': 'fr',
  'GEORGIE': 'ge', 'GRECE': 'gr', 'HOLLANDE': 'nl', 'HONG KONG': 'hk',
  'HONGRIE': 'hu', 'INDE': 'in', 'INDONESIE': 'id', 'IRAN': 'ir', 'IRLANDE': 'ie',
  'ISRAEL': 'il', 'ITALIE': 'it', 'JAMAIQUE': 'jm', 'JAPON': 'jp',
  'KAZAKHSTAN': 'kz', 'LETTONIE': 'lv', 'LIBAN': 'lb', 'LITUANIE': 'lt',
  'LUXEMBOURG': 'lu', 'MALAISIE': 'my', 'MAROC': 'ma', 'MEXIQUE': 'mx',
  'MOLDAVIE': 'md', 'NEW ZELAND': 'nz', 'NIGERIA': 'ng', 'NORVEGE': 'no',
  'OUZBEKISTAN': 'uz', 'PAKISTAN': 'pk', 'PARAGUAY': 'py', 'PEROU': 'pe',
  'POLOGNE': 'pl', 'PORTUGAL': 'pt', 'QATAR': 'qa', 'RD CONGO': 'cd', 'ROUMANIE': 'ro',
  'RUSSIE': 'ru', 'SERBIE': 'rs', 'SLOVAQUIE': 'sk', 'SLOVENIE': 'si',
  'SUEDE': 'se', 'SUISSE': 'ch', 'TAIWAN': 'tw', 'TCHEQUIE': 'cz',
  'THAILANDE': 'th', 'TUNISIE': 'tn', 'TUNSIE': 'tn', 'TURQUIE': 'tr',
  'UKRAINE': 'ua', 'URUGUAY': 'uy', 'USA': 'us', 'VENEZUELA': 've', 'VIETNAM': 'vn',
  'ZIMBABWE': 'zw', 'ILES VIERGES': 'vg'
}

/** Code ISO (pour classe CSS "fi fi-xx" du package flag-icons), ou '' si inconnu. */
export function countryFlagIso(nationality) {
  if (!nationality) return ''
  return COUNTRY_ISO[nationality.trim().toUpperCase()] ?? ''
}

/*
 * Liste triee des pays connus, pour un <select> plutot qu'un champ libre - un
 * pays mal orthographie ne matcherait pas COUNTRY_ISO et n'afficherait jamais
 * de drapeau. DOMINQUE/TUNSIE sont des fautes de frappe deja presentes dans
 * les donnees importees (gardees dans COUNTRY_ISO pour matcher l'existant),
 * pas de vrais pays a part entiere - exclues de la liste proposee ici.
 */
export const COUNTRY_NAMES = Object.keys(COUNTRY_ISO)
  .filter(name => name !== 'DOMINQUE' && name !== 'TUNSIE')
  .sort()

/**
 * Couleur pastel HSL a partir d'une teinte (0-359) - utilisee pour colorer un
 * tournoi en cours de la meme couleur partout (liste des tournois, classement),
 * voir TournamentDto.colorHue / LiveTournamentDto.colorHue cote backend.
 */
export function hslColor(hue, sat = 65, light = 55) {
  return `hsl(${hue}, ${sat}%, ${light}%)`
}

/**
 * Accepte une saisie rapide set par set ("63 46 63") et la transforme dans le
 * format d'affichage standard ("6-3 4-6 6-3"). Un set qui contient deja un
 * tiret, ou qui ne fait pas exactement 2 chiffres (ex: un super tie-break
 * "10-8"), est laisse tel quel plutot que de risquer un mauvais decoupage.
 */
export function formatMatchScore(raw) {
  if (!raw) return raw
  return raw.trim().split(/\s+/)
    .map(set => /^\d{2}$/.test(set) ? `${set[0]}-${set[1]}` : set)
    .join(' ')
}
