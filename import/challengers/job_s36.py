"""
Semaine 36 (7-13 septembre 2026) : Genoa ATP_125 (218), Tulln ATP_100 (219),
Shanghai ATP_100 (220), Istanbul 3 ATP_75 (222), Phan Thiet 3 ATP_50 (223),
tableau principal + qualifs, depuis fr.tennistemple.com (Charles, 2026-09-23).

Consigne de Charles : "tu ne touches pas aux points donnes ni aux semaines, tu
inscris juste les resultats". Contrairement aux job_*.py precedents, on
n'appelle donc PAS tt.process_main / process_qualifying (qui reecrivent le
bareme des tours via ensure_points a partir du panneau Dotations) :
  - tableau principal : inscriptions + scores seulement, bareme et semaine
    laisses tels quels ;
  - qualifs (inexistantes pour ces 5 tournois) : creees via POST
    /tournaments/{id}/qualifying avec le bareme de qualifs deja utilise par la
    majorite des tournois de la meme categorie en base (runnerUpPoints nul -
    meme resultat au classement que la variante Q1=0/runnerUp, cf
    RankingService.eliminationValue). La semaine est calculee par le backend.

Donnees : s36_data.json (extraction .competition-draw-match des pages /draw et
/draw-qualifications), s36_slugs.txt (id joueur tennistemple -> slug complet
"prenom-nom", lu sur la page de chaque match du 1er tour). Chaque joueur est
rattache a une fiche explicitement : correspondance par nom verifiee contre le
slug, surcharges ci-dessous pour les homonymes, et creation en MAJUSCULE avec
prenom complet pour les inconnus.
"""
import json
import re

import tt_push as tt

QUALI_ROUNDS = {  # (Q1, Q2) - bareme majoritaire des qualifs existantes par categorie
    "ATP_125": (3, 5), "ATP_100": (2, 4), "ATP_75": (2, 4), "ATP_50": (1, 3),
}

# id tennistemple -> id fiche, quand le nom seul est ambigu ou trompeur
OVERRIDES = {
    "7018": 496,   # jake-delaney   (Shanghai main)  - DELANEY JAKE
    "8278": 1138,  # jesse-delaney  (Shanghai quali) - DELANEY JESSE
    "7931": 12,    # dong-ju-kim    (Phan Thiet quali) - KIM DONGJU (pas KIM DONGJAE)
}

# Joueurs absents de la base (ou homonymes d'une fiche existante qui n'est pas eux)
NEW_PLAYERS = {
    "11755": ("ARINC", "KAYA", "TURQUIE"),
    "15571": ("AYDIN", "ZIYA AYBERK", "TURQUIE"),
    "15540": ("BASTOLA", "ABHISHEK", "NEPAL"),
    "14823": ("BULUT", "EMIRHAN", "TURQUIE"),
    "7630": ("FERRARI", "FRANCESCO", "ITALIE"),      # pas FERRARI GIANMARCO (1126)
    "7907": ("HSIEH", "CHENG PENG", "TAIWAN"),
    "2441": ("ILHAN", "MARSEL", "TURQUIE"),
    "11217": ("KARAHAN", "ATAKAN", "TURQUIE"),
    "12416": ("KOIZUMI", "NORITAKA", "JAPON"),
    "8169": ("NAKAMURA", "REN", "JAPON"),
    "15569": ("NGUYEN", "NAM", "VIETNAM"),           # pas NGUYEN MINH PHAT (1224)
    "15573": ("NGUYEN", "MINH THIEN", "VIETNAM"),    # pas NGUYEN MINH PHAT (1224)
    "15541": ("VIGORITI", "ALESSIO", "ITALIE"),
}

# Corrections de fiches existantes confirmees par Charles
PLAYER_FIXES = {
    1761: ("GARBERO", "FEDERICO", "ITALIE"),   # tennistemple federico-garbero (V19 avait mis FILIPPO FRANCESCO)
    574: ("AZKARA", "ARDA", "TURQUIE"),        # etait TUNISIE
    564: ("PANKIN", "SEMEN", "RUSSIE"),        # sans nationalite
    942: ("ZENG", "YAOJIE", "CHINE"),          # etait TAIWAN
    425: ("PRIHODKO", "OLEG", "MACEDOINE"),    # etait UKRAINE
}

ENTRY_TYPE = {k.upper(): v for k, v in tt.ENTRY_TYPE.items()}  # "Alt" -> ALTERNATE aussi


def tokens(s):
    return set(re.sub(r"[^A-Z ]", " ", tt.norm(s).replace("-", " ")).split())


def resolve_players(data, slugs):
    """Retourne {id tennistemple: id fiche} pour tous les joueurs des 10 tableaux."""
    by_id = {p["id"]: p for p in tt.load_players()}
    seen = {}
    for t in data.values():
        for part in ("main", "quali"):
            for m in t[part]:
                for p in m[2]:
                    if p[0] and p[0] != "2000":
                        seen[p[0]] = (p[3], p[2])
    mapping = {}
    for tt_id, (name, flag) in seen.items():
        if tt_id in OVERRIDES:
            mapping[tt_id] = OVERRIDES[tt_id]
        elif tt_id in NEW_PLAYERS:
            continue
        else:
            cands = tt.find_candidates(name, flag)
            assert len(cands) == 1, f"{name} ({tt_id}, {slugs[tt_id]}): candidats {cands}"
            mapping[tt_id] = cands[0]
        p = by_id[mapping[tt_id]]
        glued = lambda s: re.sub(r"[^A-Z]", "", tt.norm(s))  # O'CONNELL <-> oconnell
        assert tokens(p["lastName"]) & tokens(slugs[tt_id]) or glued(p["lastName"]) in glued(slugs[tt_id]), \
            f"{name} -> {p}"
    return mapping


def create_new_players(mapping):
    existing = {(tt.norm(p["lastName"]), tt.norm(p["firstName"] or "")): p["id"] for p in tt.load_players()}
    for tt_id, (last, first, nat) in NEW_PLAYERS.items():
        key = (last, first)
        if key in existing:  # relance du script : deja cree
            mapping[tt_id] = existing[key]
            continue
        p = tt.call("POST", "/players", {"lastName": last, "firstName": first, "nationality": nat})
        mapping[tt_id] = p["id"]
        print(f"  joueur cree: {last} {first} ({nat}) -> id {p['id']}")


def apply_player_fixes():
    for pid, (last, first, nat) in PLAYER_FIXES.items():
        tt.call("PUT", f"/players/{pid}", {"lastName": last, "firstName": first, "nationality": nat})
        print(f"  fiche {pid} -> {last} {first} ({nat})")


def push_draw(tid, matches, mapping):
    """Inscriptions (positions 2k-1 / 2k du match k du 1er tour) puis scores,
    tour par tour. Ne touche ni au bareme ni a la semaine du tournoi."""
    matches = tt.drop_phantom_matches({"count": len(matches), "data": matches})["data"]
    existing = {e["drawPosition"] for e in tt.call("GET", f"/tournaments/{tid}/entries")}
    for _rnd, ordre, players, _score in sorted((m for m in matches if m[0] == 1), key=lambda m: m[1]):
        for side, pos in ((players[0], ordre * 2 - 1), (players[1], ordre * 2)):
            if pos in existing:
                continue
            tt_id, seed, _flag, name, _win = side
            if not tt_id or tt_id == "2000" or tt.norm(name) == "BYE":
                tt.call("POST", f"/tournaments/{tid}/entries",
                        {"playerId": None, "drawPosition": pos, "seed": None, "entryType": None, "bye": True})
                continue
            tt.call("POST", f"/tournaments/{tid}/entries", {
                "playerId": mapping[tt_id], "drawPosition": pos, "seed": tt.seed_for(seed),
                "entryType": ENTRY_TYPE.get((seed or "").upper()), "bye": False,
            })
    for round_order in range(1, max(m[0] for m in matches) + 1):
        by_pos = {mm["positionInRound"]: mm for mm in tt.call("GET", f"/tournaments/{tid}/matches")
                  if mm["roundOrder"] == round_order}
        for _rnd, ordre, players, score in sorted((m for m in matches if m[0] == round_order), key=lambda m: m[1]):
            winner = next((p for p in players if p[4] == 1), None)
            mm = by_pos.get(ordre)
            if winner is None or mm is None or mm["status"] == "COMPLETED":
                continue
            e1, e2 = mm["entry1"], mm["entry2"]
            if e1 is None or e2 is None or e1.get("bye") or e2.get("bye"):
                continue
            wid = mapping[winner[0]]
            winner_entry = e1["id"] if e1["playerId"] == wid else e2["id"] if e2["playerId"] == wid else None
            if winner_entry is None:
                print(f"    tour {round_order} match {ordre}: vainqueur {winner[3]} absent du match - SKIP")
                continue
            tt.call("PUT", f"/matches/{mm['id']}/score", {"score": tt.fmt_score(score), "winnerEntryId": winner_entry})


def ensure_qualifying(tid, draw_size):
    main = tt.call("GET", f"/tournaments/{tid}")
    if main.get("qualifyingTournamentId"):
        return main["qualifyingTournamentId"]
    q1, q2 = QUALI_ROUNDS[main["category"]]
    q = tt.call("POST", f"/tournaments/{tid}/qualifying", {"drawSize": draw_size, "rounds": [
        {"roundOrder": 1, "roundLabel": "Q1", "points": q1},
        {"roundOrder": 2, "roundLabel": "Q2", "points": q2},
    ]})
    print(f"  qualifs creees: id {q['id']} (taille {draw_size}, Q1={q1} Q2={q2}, semaine {q['weekNumber']})")
    return q["id"]


if __name__ == "__main__":
    data = json.load(open("s36_data.json", encoding="utf-8"))
    slugs = dict(x.split(":", 1) for x in open("s36_slugs.txt").read().strip().split(","))
    apply_player_fixes()
    mapping = resolve_players(data, slugs)
    create_new_players(mapping)
    for name, t in data.items():
        tid = t["tid"]
        print(f"=== {name} (id {tid}) ===")
        qtid = ensure_qualifying(tid, 2 * sum(1 for m in t["quali"] if m[0] == 1))
        push_draw(qtid, t["quali"], mapping)
        push_draw(tid, t["main"], mapping)
        print(f"  ok: {len(t['quali'])} matchs de qualifs, {len(t['main'])} matchs du tableau principal")
