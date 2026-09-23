"""
Import des resultats Challenger depuis TennisTemple (fr.tennistemple.com) vers
l'API tennis-results. Voir README.md section 12 pour la methode complete
(extraction navigateur + format attendu ici).

Entree attendue pour chaque tournoi : un dict "scraped" avec la forme exacte
produite par le snippet JS d'extraction (voir README) :
    {"count": N, "data": [[round, ordre, [[ttPlayerId, seed, flag, name, isWinner], ...], score_str], ...]}
et un dict "dotations" ordonne du tour le plus haut (Vainqueur) au plus bas
(1er tour), forme : [("Vainqueur", 125), ("Finale", 64), ("1/2 finale", 35),
("1/4 finale", 16), ("2e tour", 8), ("1e tour", 0)] (copier tel quel depuis
la page, dans l'ordre d'affichage).
"""
import json
import re
import sys
import unicodedata
import urllib.error
import urllib.request

BASE = "http://localhost:8082/api"

FLAG_TO_NATIONALITY = {
    "fr": "FRANCE", "it": "ITALIE", "es": "ESPAGNE", "de": "ALLEMAGNE",
    "gb": "ANGLETERRE", "gb-eng": "ANGLETERRE", "gb-sct": "ECOSSE", "gb-wls": "PAYS DE GALLES",
    "us": "USA", "au": "AUSTRALIE", "cn": "CHINE", "jp": "JAPON", "kr": "COREE",
    "in": "INDE", "br": "BRESIL", "ar": "ARGENTINE", "cl": "CHILI", "mx": "MEXIQUE",
    "pt": "PORTUGAL", "ch": "SUISSE", "at": "AUTRICHE", "cz": "TCHEQUIE", "sk": "SLOVAQUIE",
    "pl": "POLOGNE", "hu": "HONGRIE", "hr": "CROATIE", "rs": "SERBIE", "ro": "ROUMANIE",
    "bg": "BULGARIE", "gr": "GRECE", "tr": "TURQUIE", "ru": "RUSSIE", "by": "BIELORUSSIE",
    "ua": "UKRAINE", "kz": "KAZAKHSTAN", "uz": "OUZBEKISTAN", "co": "COLOMBIE",
    "py": "PARAGUAY", "bo": "BOLIVIE", "ec": "EQUATEUR", "pe": "PEROU", "ve": "VENEZUELA",
    "ca": "CANADA", "za": "AFRIQUE SUD", "ma": "MAROC", "tn": "TUNISIE", "eg": "EGYPTE",
    "ci": "COTE IVOIRE", "cd": "RD CONGO", "cg": "CONGO", "tg": "TOGO", "ke": "KENYA",
    "th": "THAILANDE", "vn": "VIETNAM", "id": "INDONESIE", "hk": "HONG KONG",
    "tw": "TAIWAN", "il": "ISRAEL", "ge": "GEORGIE", "md": "MOLDAVIE", "lv": "LETTONIE",
    "lt": "LITUANIE", "ee": "ESTONIE", "fi": "FINLANDE", "se": "SUEDE", "no": "NORVEGE",
    "dk": "DANEMARK", "is": "ISLANDE", "ie": "IRLANDE", "nl": "HOLLANDE", "be": "BELGIQUE",
    "lu": "LUXEMBOURG", "mc": "MONACO", "si": "SLOVENIE", "me": "MONTENEGRO",
    "mk": "MACEDOINE", "al": "ALBANIE", "ba": "BOSNIE", "cy": "CHYPRE", "mt": "MALTE",
    "nz": "NOUVELLE ZELANDE", "sg": "SINGAPOUR", "my": "MALAISIE", "ph": "PHILIPPINES",
    "bh": "BAHREIN", "qa": "QATAR", "ae": "EMIRATS ARABES UNIS", "sa": "ARABIE SAOUDITE",
    "uy": "URUGUAY", "pr": "PORTO RICO", "cr": "COSTA RICA", "gt": "GUATEMALA",
    "sv": "EL SALVADOR", "do": "REPUBLIQUE DOMINICAINE", "cu": "CUBA", "pa": "PANAMA",
    "ec": "EQUATEUR", "bz": "BELIZE", "np": "NEPAL", "lb": "LIBAN", "dz": "ALGERIE",
}


def call(method, path, body=None, quiet_errors=False):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                  headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        msg = e.read().decode(errors="replace")
        if not quiet_errors:
            print(f"  !! HTTP {e.code} {method} {path}: {msg}")
        e.body_text = msg
        raise


def strip_accents(s):
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")


def norm(s):
    return strip_accents((s or "").strip()).upper()


def split_tt_name(name):
    """'Pinnington Jones J.' -> ('PINNINGTON JONES', 'J')"""
    name = name.strip()
    parts = name.rsplit(" ", 1)
    if len(parts) == 2 and re.match(r"^[A-Z]\.?$", parts[1]):
        return norm(parts[0]), parts[1].rstrip(".")
    return norm(name), None


_ROUND_LABEL_RE = [
    (re.compile(r"vainqueur", re.I), "W"),
    (re.compile(r"^finale$", re.I), "RU"),  # 1 tier below "Vainqueur" -> runner-up
    (re.compile(r"1/2", re.I), "SF"),
    (re.compile(r"1/4", re.I), "QF"),
    (re.compile(r"1/8", re.I), "R16"),
    (re.compile(r"1/16", re.I), "R32"),
    (re.compile(r"1/32", re.I), "R64"),
    (re.compile(r"2e tour|q\s*2", re.I), "R2"),
    (re.compile(r"1er? tour|1e tour|q\s*1", re.I), "R1"),
    (re.compile(r"3e tour|q\s*3", re.I), "R3"),
]


def dotations_to_rounds(dotations, total_rounds):
    """dotations: [(label, points), ...] ordered Vainqueur first (highest) to
    1er tour last (lowest). Returns (rounds_list, runner_up_points) where
    rounds_list = [{"roundOrder": r, "roundLabel": ..., "points": p}, ...]
    for r in 1..total_rounds, and runner_up_points is the tier just below
    champion. Uses README's rule: last dotations tier = champion, 2nd-to-last
    style label ('Finale') = runner-up, everything else maps to its round by
    position counted from the top (round totalRounds-1 = "1/2 finale" etc.)
    for a 5-round bracket, or from the bottom (round1='1er tour', round2=
    '2e tour', ...) for shorter (qualifying) brackets - both give the same
    answer when dotations has exactly total_rounds+1 entries.
    """
    entries = list(dotations)
    if not entries:
        # Aucun panneau Dotations du tout cote TennisTemple (ex: Samsun S31
        # quali) - pas de bareme connu, tout a 0 plutot que planter.
        default_labels = {5: ["R32", "R16", "QF", "SF", "F"], 4: ["R16", "QF", "SF", "F"],
                           3: ["QF", "SF", "F"], 2: ["Q1", "F"], 1: ["Q1"]}
        lbls = default_labels.get(total_rounds, [f"R{i+1}" for i in range(total_rounds)])
        return [{"roundOrder": i + 1, "roundLabel": lbls[i], "points": 0} for i in range(total_rounds)], 0
    if len(entries) == total_rounds:
        # Tableau tronque cote source (ex: Piracicaba S26, quali - un seul
        # tour reellement joue dans le DOM, cf drop_phantom_matches) : pas de
        # palier "finaliste" distinct du palier "vainqueur/qualifie". On
        # applique quand meme le tour 1 = 0 pts plus bas ; pas de runner-up.
        champion_points = entries[0][1]
        runner_up_points = None
        rounds = [None] * total_rounds
        rounds[total_rounds - 1] = champion_points
        remaining = entries[1:]
        for i, (label, pts) in enumerate(remaining):
            round_order = total_rounds - 1 - i
            rounds[round_order - 1] = pts
        rounds[0] = 0
        default_labels = {5: ["R32", "R16", "QF", "SF", "F"], 4: ["R16", "QF", "SF", "F"],
                           3: ["QF", "SF", "F"], 2: ["Q1", "F"], 1: ["Q1"]}
        lbls = default_labels.get(total_rounds, [f"R{i+1}" for i in range(total_rounds)])
        round_list = [{"roundOrder": i + 1, "roundLabel": lbls[i], "points": rounds[i]}
                      for i in range(total_rounds)]
        return round_list, runner_up_points
    assert len(entries) == total_rounds + 1, (
        f"attendu {total_rounds + 1} paliers de dotations pour {total_rounds} tours, "
        f"recu {len(entries)}: {entries}"
    )
    champion_points = entries[0][1]
    runner_up_points = entries[1][1]
    rounds = [None] * total_rounds
    rounds[total_rounds - 1] = champion_points
    # entries[2:] are the remaining rounds from totalRounds-1 down to 1
    remaining = entries[2:]
    for i, (label, pts) in enumerate(remaining):
        round_order = total_rounds - 1 - i
        rounds[round_order - 1] = pts
    rounds[0] = 0  # 1er tour = toujours 0 (regle constante de l'appli)
    labels = {1: "R1"}
    round_list = []
    default_labels = {5: ["R32", "R16", "QF", "SF", "F"], 4: ["R16", "QF", "SF", "F"],
                       3: ["QF", "SF", "F"], 2: ["Q1", "F"], 1: ["F"]}
    lbls = default_labels.get(total_rounds, [f"R{i+1}" for i in range(total_rounds)])
    for i in range(total_rounds):
        round_list.append({"roundOrder": i + 1, "roundLabel": lbls[i], "points": rounds[i]})
    return round_list, runner_up_points


def fmt_score(raw):
    if raw is None or raw == "":
        return "w.o."
    raw = raw.strip()
    if raw.lower() in ("w.o.", "wo"):
        return "w.o."
    out = []
    for tok in raw.split():
        m = re.match(r"^(\d+)(\((\d+)\))?$", tok)
        if not m:
            out.append(tok)
            continue
        digits, tb = m.group(1), m.group(3)
        if len(digits) == 2:
            s = f"{digits[0]}-{digits[1]}"
        elif len(digits) == 3 and digits[0] == "1":
            s = f"10-{digits[2]}"
        elif len(digits) == 3 and digits[-1] == "1" and digits[:2] != "10":
            s = f"{digits[:-1]}-10"
        else:
            s = "-".join(list(digits))
        if tb:
            s += f"({tb})"
        out.append(s)
    return " ".join(out)


_DOTATION_RE = re.compile(r"([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ0-9/ ]*?)\s+(\d+)\s+Points")


def parse_dotations(text):
    """'Dotations Vainqueur 100 Points 20 630 € Finale 50 Points ...' ->
    [('Vainqueur', 100), ('Finale', 50), ...] (ordre d'affichage = du
    champion vers le 1er tour, tel qu'attendu par dotations_to_rounds)."""
    return [(label.strip(), int(pts)) for label, pts in _DOTATION_RE.findall(text)]


def process_main_scrape(tid, blob, country=None, country_nationality=None):
    dotations = parse_dotations(blob["dotations"])
    matches = drop_phantom_matches(blob["matches"])
    process_main(tid, matches, dotations, country=country, country_nationality=country_nationality)


def drop_phantom_matches(scraped):
    """Certains tableaux (ex: Piracicaba S26, quali) ont un tour complet dans
    le DOM mais le(s) tour(s) suivant(s) entierement vides (les deux entrees
    a null, aucun score) - un vrai trou de donnees cote TennisTemple, pas
    juste un tour "pas encore joue" (sinon on verrait les noms des vainqueurs
    du tour precedent deja positionnes). On les retire avant de calculer
    totalRounds/dotations, plutot que de deviner qui aurait du s'affronter -
    coherent avec le principe de l'appli (voir README section 2) : un
    resultat non constate ne rapporte jamais de points. Le tournoi reste
    simplement IN_PROGRESS au tour ou les vraies donnees s'arretent."""
    real = [m for m in scraped["data"] if any(p[0] for p in m[2])]
    dropped = len(scraped["data"]) - len(real)
    if dropped:
        print(f"    (tour(s) fantome(s) ignores: {dropped} match(es) sans joueurs dans le DOM)")
    return {"count": len(real), "data": real}


def process_qualifying_scrape(tid, blob, country_nationality=None):
    dotations = parse_dotations(blob["dotations"])
    matches = drop_phantom_matches(blob["matches"])
    process_qualifying(tid, matches, dotations, country_nationality=country_nationality)


_player_cache = None


def load_players():
    global _player_cache
    if _player_cache is None:
        players = call("GET", "/players")
        _player_cache = players
    return _player_cache


def find_candidates(tt_name, flag, exclude_ids=frozenset()):
    """Match a tennistemple 'Lastname X.' entry to existing players. Returns
    an ORDERED list of candidate ids (best guess first) - may be empty.
    `exclude_ids` are player ids already placed elsewhere in the SAME draw
    being pushed right now: two different draw slots are always two
    different people, so a candidate already used this draw is never a
    valid match here (caught two homonym collisions this way: Bocci F. vs
    Bocchi L., Mazzola F. vs Mazza M. - real distinct players wrongly
    fuzzy-matched to an id already assigned to their near-namesake in the
    same tournament)."""
    players = load_players()
    last, initial = split_tt_name(tt_name)
    cands = [p for p in players if norm(p["lastName"]) == last and p["id"] not in exclude_ids]
    if len(cands) > 1 and initial:
        narrowed = [p for p in cands if (p.get("firstName") or "").strip().upper().startswith(initial.upper())]
        if narrowed:
            cands = narrowed
    if len(cands) > 1:
        nat = FLAG_TO_NATIONALITY.get(flag)
        if nat:
            narrowed = [p for p in cands if norm(p.get("nationality") or "") == nat]
            if narrowed:
                cands = narrowed
    if len(cands) > 1:
        print(f"    (ambigu {tt_name!r}: ids {[c['id'] for c in cands]}, on prend {cands[0]['id']} d'abord)")
    # Pas de fuzzy matching ici : deux homonymes tres proches mais distincts
    # (Bocci/Bocchi, Mazza/Mazzola - deux vrais joueurs italiens differents,
    # tous deux presents la meme semaine) se sont fait fusionner a tort avec
    # un cutoff difflib a 0.9 - un faux "match" corrompt silencieusement la
    # fiche d'un joueur existant, alors qu'un nouveau joueur cree a tort pour
    # une simple faute de frappe reste un doublon isole, visible et corrigible
    # a l'audit (cf README section 11). Risque juge trop asymetrique : on
    # exige donc un lastName exact, sinon on cree.
    return [c["id"] for c in cands]


def create_player(tt_name, flag, tournament_country_nationality):
    last, initial = split_tt_name(tt_name)
    nat = FLAG_TO_NATIONALITY.get(flag) or tournament_country_nationality
    # Toujours en MAJUSCULE (Charles, 2026-09-23 : les fiches "Nom X" en casse
    # mixte creees ici ont du etre nettoyees a la main, cf V19/V20). Le prenom
    # reste une initiale : a completer depuis l'ATP apres l'import.
    new_player = call("POST", "/players", {
        "lastName": last, "firstName": initial, "nationality": nat,
    })
    load_players().append(new_player)
    print(f"    joueur cree: {tt_name!r} ({nat}) -> id {new_player['id']}")
    return new_player["id"]


def _is_same_week_conflict(http_error):
    return http_error.code == 400 and "meme semaine" in getattr(http_error, "body_text", "")


def post_entry_with_fallback(tid, tt_name, flag, pos, seed, etype, country_nationality, exclude_ids=frozenset()):
    """Tries each name-matched candidate in turn; on the backend's 'deja
    inscrit cette semaine' conflict (homonyme distinct, cf Blanch D. -
    Darwin vs Dali), moves on to the next candidate. Creates a new player
    only if every candidate conflicts (or there was no candidate at all)."""
    candidates = find_candidates(tt_name, flag, exclude_ids=exclude_ids)
    tried = []
    for pid in candidates:
        tried.append(pid)
        try:
            return call("POST", f"/tournaments/{tid}/entries", {
                "playerId": pid, "drawPosition": pos, "seed": seed_for(seed),
                "entryType": etype, "bye": False,
            }, quiet_errors=True)
        except urllib.error.HTTPError as e:
            if _is_same_week_conflict(e):
                print(f"    {tt_name!r}: id {pid} deja inscrit ailleurs cette semaine, homonyme suivant...")
                continue
            raise
    if candidates:
        print(f"    {tt_name!r}: tous les candidats {tried} en conflit - creation d'un nouveau joueur")
    pid = create_player(tt_name, flag, country_nationality)
    return call("POST", f"/tournaments/{tid}/entries", {
        "playerId": pid, "drawPosition": pos, "seed": seed_for(seed),
        "entryType": etype, "bye": False,
    })


def seed_for(seed):
    if seed and re.match(r"^\d+$", seed):
        return int(seed)
    return None


ENTRY_TYPE = {
    "WC": "WILD_CARD", "Q": "QUALIFIER", "LL": "LUCKY_LOSER", "ALT": "ALTERNATE",
    "NG": "NEW_GENERATION", "SE": "SPECIAL_EXEMPT", "PR": "PROTECTED_RANKING",
    "CO": "CUT_OFF", "WO": "WILD_CARD",
}


def push_entries_and_rounds(tid, scraped, country_nationality):
    """scraped: {"count": N, "data": [[round, ordre, [[id,seed,flag,name,win],...], score], ...]}"""
    existing = call("GET", f"/tournaments/{tid}/entries")
    by_pos = {e["drawPosition"]: e for e in existing}
    used_ids = {e["playerId"] for e in existing if e.get("playerId")}
    round1 = [m for m in scraped["data"] if m[0] == 1]
    round1.sort(key=lambda m: m[1])
    for m in round1:
        ordre = m[1]
        players = m[2]
        left, right = players[0], players[1]
        # Le backend construit le match round1 #k a partir des positions
        # (2k-1, 2k) (BracketService.syncRound1FromEntries) - PAS d'un
        # decoupage "moitie gauche/moitie droite" comme dans l'import Excel.
        for side, pos in ((left, ordre * 2 - 1), (right, ordre * 2)):
            if pos in by_pos:
                continue
            pid_tt, seed, flag, name, _win = side
            if norm(name) == "BYE" or not pid_tt:
                # pid_tt manquant (ex: Saint-Marin S31 quali round1 ordre3,
                # nom affiche "—") - meme traitement qu'un bye explicite :
                # l'autre cote gagne par defaut (walkover), pas un vrai
                # adversaire a creer.
                call("POST", f"/tournaments/{tid}/entries", {
                    "playerId": None, "drawPosition": pos, "seed": seed_for(seed),
                    "entryType": None, "bye": True,
                })
                continue
            entry = post_entry_with_fallback(tid, name, flag, pos, seed, ENTRY_TYPE.get(seed),
                                              country_nationality, exclude_ids=used_ids)
            if entry and entry.get("playerId"):
                used_ids.add(entry["playerId"])
    max_round = max(m[0] for m in scraped["data"])
    for round_order in range(1, max_round + 1):
        matches = call("GET", f"/tournaments/{tid}/matches")
        by_rpos = {mm["positionInRound"]: mm for mm in matches if mm["roundOrder"] == round_order}
        round_matches = sorted([m for m in scraped["data"] if m[0] == round_order], key=lambda m: m[1])
        for m in round_matches:
            _rnd, ordre, players, score = m
            winner = next((p for p in players if p[4] == 1), None)
            if winner is None:
                continue
            mm = by_rpos.get(ordre)
            if not mm:
                print(f"    round{round_order} pos{ordre}: match introuvable - SKIP")
                continue
            if mm["status"] == "COMPLETED":
                continue
            e1, e2 = mm["entry1"], mm["entry2"]
            if e1 is None or e2 is None or e1.get("bye") or e2.get("bye"):
                continue
            wlast, _wi = split_tt_name(winner[3])
            if norm(e1["playerLastName"] or "") == wlast:
                winner_id = e1["id"]
            elif norm(e2["playerLastName"] or "") == wlast:
                winner_id = e2["id"]
            else:
                print(f"    round{round_order} pos{ordre}: vainqueur {winner[3]!r} ne correspond ni a "
                      f"{e1['playerLastName']} ni a {e2['playerLastName']} - SKIP")
                continue
            call("PUT", f"/matches/{mm['id']}/score", {"score": fmt_score(score), "winnerEntryId": winner_id})


def ensure_points(tid, dotations, total_rounds, extra_update=None):
    rounds, runner_up = dotations_to_rounds(dotations, total_rounds)
    body = {"rounds": rounds, "runnerUpPoints": runner_up}
    if extra_update:
        body.update(extra_update)
    current = call("GET", f"/tournaments/{tid}")
    full = {
        "category": current["category"], "weekNumber": current["weekNumber"],
        "country": current["country"], "mandatorySlot": current["mandatorySlot"],
        "qualifyingRound1Points": current["qualifyingRound1Points"],
        "qualifyingRound2Points": current["qualifyingRound2Points"],
        "runnerUpPoints": runner_up, "rounds": rounds, "drawSize": current["drawSize"],
    }
    full.update(body)
    call("PUT", f"/tournaments/{tid}", full)


def reset_tournament(tid):
    """Supprime toutes les entrees (et donc les matchs/scores associes,
    voir EntryService.delete -> clearEntryFromMatches) d'un tournoi. Utile
    pour corriger une erreur de placement avant de repousser proprement."""
    entries = call("GET", f"/tournaments/{tid}/entries")
    for e in entries:
        call("DELETE", f"/tournaments/{tid}/entries/{e['id']}")
    print(f"  tournoi {tid}: {len(entries)} entrees supprimees (reset).")


def total_rounds_for(dotations, scraped):
    """Le nombre de tours REEL d'un tableau peut depasser le tour max present
    dans les donnees scrapees (round(s) fantome(s) retires par
    drop_phantom_matches, cf Targu Mures S26 quali : round2 vide cote
    TennisTemple alors que le panneau Dotations liste bien 3 paliers = 2
    tours). Le panneau Dotations reflete toujours la vraie structure du
    tableau (il existe meme quand le round correspondant n'a jamais ete
    peuple cote scores) ; on prend le maximum des deux sources."""
    match_max = max((m[0] for m in scraped["data"]), default=0)
    dot_rounds = max(len(dotations) - 1, 1) if dotations else 0
    return max(match_max, dot_rounds)


def process_main(tid, scraped, dotations, country=None, country_nationality=None):
    print(f"=== tournoi id {tid} : tableau principal ===")
    if country is not None:
        cur = call("GET", f"/tournaments/{tid}")
        call("PUT", f"/tournaments/{tid}", {
            "category": cur["category"], "weekNumber": cur["weekNumber"], "country": country,
            "mandatorySlot": cur["mandatorySlot"],
            "qualifyingRound1Points": cur["qualifyingRound1Points"],
            "qualifyingRound2Points": cur["qualifyingRound2Points"],
            "runnerUpPoints": cur["runnerUpPoints"], "rounds": cur["rounds"], "drawSize": cur["drawSize"],
        })
    total_rounds = total_rounds_for(dotations, scraped)
    ensure_points(tid, dotations, total_rounds)
    push_entries_and_rounds(tid, scraped, country_nationality)
    print(f"  {len(scraped['data'])} matchs pousses (tours 1..{total_rounds}).")


def process_qualifying(tid, scraped, dotations, country_nationality=None):
    print(f"=== tournoi id {tid} : qualifs ===")
    total_rounds = total_rounds_for(dotations, scraped)
    main_t = call("GET", f"/tournaments/{tid}")
    qtid = main_t.get("qualifyingTournamentId")
    round1 = sorted([m for m in scraped["data"] if m[0] == 1], key=lambda m: m[1])
    draw_size = len(round1) * 2
    rounds, runner_up = dotations_to_rounds(dotations, total_rounds)
    if qtid is None:
        qt = call("POST", f"/tournaments/{tid}/qualifying", {"drawSize": draw_size, "rounds": rounds})
        qtid = qt["id"]
        print(f"  qualifs creees (id {qtid}, size {draw_size})")
        call("PUT", f"/tournaments/{qtid}", {
            "category": qt.get("category"), "weekNumber": qt.get("weekNumber"), "country": qt.get("country"),
            "mandatorySlot": None, "qualifyingRound1Points": None, "qualifyingRound2Points": None,
            "runnerUpPoints": runner_up, "rounds": rounds, "drawSize": draw_size,
        })
    else:
        print(f"  qualifs deja presentes (id {qtid})")
        ensure_points(qtid, dotations, total_rounds)
    push_entries_and_rounds(qtid, scraped, country_nationality)
    print(f"  {len(scraped['data'])} matchs pousses (tours 1..{total_rounds}).")


if __name__ == "__main__":
    print("Module a importer, voir README.md section 12 pour un exemple de job.")
