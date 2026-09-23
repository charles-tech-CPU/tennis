"""
Genere backend/src/main/resources/db/migration/V22__seed_team_competitions_2026.sql
a partir des extractions navigateur (Charles, 2026-09-23) :
  - daviscup_2026_raw.json  : daviscup.com/en/draws-results/2026 (finals,
    qualifiers tours 1 et 2, world-group-i play-off), une entree par page
    /en/tie/<uuid> : "h" = en-tete (phase, dates, equipes, statut, score,
    lieu, surface), "m" = matchs [numero, statut, duree, [joueurs, gagne, sets], [..]]
    (cote equipe 1 puis equipe 2, sets "7(7) 6" = jeux + points de tie-break).
  - unitedcup_2026_raw.json : unitedcup.com/en/scores/results, une entree par
    rencontre (jour, codes equipes, score, ville, matchs). ATTENTION : dans un
    match, le VAINQUEUR est liste en premier (pas l'equipe 1) - l'equipe de
    chaque joueur est donc deduite par propagation (voir assign_uc_teams).

Usage : python build_seed.py  (depuis import/teams/)
"""
import json
import math
import os
import re

OUT = os.path.join("..", "..", "backend", "src", "main", "resources", "db", "migration",
                   "V22__seed_team_competitions_2026.sql")

DC_COUNTRY = {
    "Argentina": "ARGENTINE", "Australia": "AUSTRALIE", "Austria": "AUTRICHE", "Belgium": "BELGIQUE",
    "Bosnia and Herzegovina": "BOSNIE", "Brazil": "BRESIL", "Bulgaria": "BULGARIE", "Canada": "CANADA",
    "Chile": "CHILI", "China, P.R.": "CHINE", "Chinese Taipei": "TAIWAN", "Colombia": "COLOMBIE",
    "Croatia": "CROATIE", "Czechia": "TCHEQUIE", "Denmark": "DANEMARK", "Ecuador": "EQUATEUR",
    "Egypt": "EGYPTE", "Finland": "FINLANDE", "France": "FRANCE", "Germany": "ALLEMAGNE",
    "Great Britain": "GRANDE BRETAGNE", "Greece": "GRECE", "Hong Kong, China": "HONG KONG",
    "Hungary": "HONGRIE", "India": "INDE", "Israel": "ISRAEL", "Italy": "ITALIE", "Japan": "JAPON",
    "Kazakhstan": "KAZAKHSTAN", "Korea, Rep.": "COREE", "Lebanon": "LIBAN", "Lithuania": "LITUANIE",
    "Luxembourg": "LUXEMBOURG", "Mexico": "MEXIQUE", "Monaco": "MONACO", "Morocco": "MAROC",
    "Netherlands": "HOLLANDE", "New Zealand": "NEW ZELAND", "Norway": "NORVEGE", "Paraguay": "PARAGUAY",
    "Peru": "PEROU", "Poland": "POLOGNE", "Portugal": "PORTUGAL", "Romania": "ROUMANIE",
    "Serbia": "SERBIE", "Slovakia": "SLOVAQUIE", "Slovenia": "SLOVENIE", "Spain": "ESPAGNE",
    "Sweden": "SUEDE", "Switzerland": "SUISSE", "Tunisia": "TUNISIE", "Turkiye": "TURQUIE",
    "USA": "USA", "Ukraine": "UKRAINE",
}
UC_COUNTRY = {
    "ARG": "ARGENTINE", "AUS": "AUSTRALIE", "BEL": "BELGIQUE", "CAN": "CANADA", "CHN": "CHINE",
    "CZE": "TCHEQUIE", "ESP": "ESPAGNE", "FRA": "FRANCE", "GBR": "GRANDE BRETAGNE", "GER": "ALLEMAGNE",
    "GRE": "GRECE", "ITA": "ITALIE", "JPN": "JAPON", "NED": "HOLLANDE", "NOR": "NORVEGE",
    "POL": "POLOGNE", "SUI": "SUISSE", "USA": "USA",
}
# unitedcup.com/en/scores/group-standings
UC_GROUPS = {
    "A": ["USA", "ARG", "ESP"], "B": ["BEL", "CAN", "CHN"], "C": ["SUI", "ITA", "FRA"],
    "D": ["AUS", "CZE", "NOR"], "E": ["GRE", "GBR", "JPN"], "F": ["POL", "GER", "NED"],
}
# Ordre du tableau final (quart n -> demi ceil(n/2)), releve sur les pages officielles
DC_FINALS_QF = [("Italy", "Korea, Rep."), ("Czechia", "Canada"), ("Great Britain", "Germany"), ("Austria", "Spain")]
UC_KO = {
    "QF": [("USA", "GRE"), ("AUS", "POL"), ("SUI", "ARG"), ("BEL", "CZE")],
    "SF": [("USA", "POL"), ("SUI", "BEL")],
    "F": [("POL", "SUI")],
}

MONTHS = {"January": "janvier", "February": "février", "March": "mars", "April": "avril", "May": "mai",
          "June": "juin", "July": "juillet", "August": "août", "September": "septembre",
          "October": "octobre", "November": "novembre", "December": "décembre"}


def fr_dates(s):
    for en, fr in MONTHS.items():
        s = s.replace(en, fr)
    return s


def fr_uc_day(s):  # "Sunday, January 11, 2026" -> "11 janvier 2026"
    m = re.match(r"\w+, (\w+) (\d+), (\d+)", s)
    return f"{int(m.group(2))} {MONTHS[m.group(1)]} {m.group(3)}"


def fr_surface(s):
    m = re.match(r"(\w+) \((\w+)\)", s or "")
    if not m:
        return s
    kind = {"Hard": "Dur", "Clay": "Terre battue", "Grass": "Gazon"}.get(m.group(1), m.group(1))
    where = {"Indoor": "intérieur", "Outdoor": "extérieur"}.get(m.group(2), m.group(2))
    return f"{kind} ({where})"


def parse_sets(s):
    return [(int(g), int(tb) if tb else None) for g, tb in re.findall(r"(\d+)(?:\((\d+)\))?", s or "")]


def set_complete(a, b):
    hi, lo = max(a, b), min(a, b)
    if hi >= 10:  # super tie-break (10 points)
        return hi - lo >= 2
    return (hi == 6 and hi - lo >= 2) or (hi == 7 and lo in (5, 6))


def build_rubber(sets1, sets2, flag1, flag2):
    """-> (score du point de vue equipe 1, vainqueur 1/2, None si non joue)."""
    s1, s2 = parse_sets(sets1), parse_sets(sets2)
    if not s1 or len(s1) != len(s2):
        return None, None
    parts = []
    for (g1, tb1), (g2, tb2) in zip(s1, s2):
        tbs = [t for t in (tb1, tb2) if t is not None]
        parts.append(f"{g1}-{g2}" + (f"({min(tbs)})" if tbs else ""))
    won1 = sum(1 for (g1, _), (g2, _) in zip(s1, s2) if g1 > g2 and set_complete(g1, g2))
    won2 = sum(1 for (g1, _), (g2, _) in zip(s1, s2) if g2 > g1 and set_complete(g1, g2))
    if flag1 or flag2:
        winner = 1 if flag1 else 2
    else:  # vainqueur non marque cote source (abandon, ou oubli) : deduit du score
        last1, last2 = s1[-1][0], s2[-1][0]
        winner = 1 if (won1, last1) > (won2, last2) else 2
    score = " ".join(parts)
    if not set_complete(s1[-1][0], s2[-1][0]):
        score += " ab."
    return score, winner


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, int):
        return str(v)
    return "'" + str(v).replace("'", "''") + "'"


def dc_ties():
    raw = json.load(open("daviscup_2026_raw.json", encoding="utf-8"))
    stage_of = {("FINALS", "QF"): "FINALS_QF", ("QUALIFIERS", "R1"): "QUALIFIERS_R1",
                ("QUALIFIERS", "R2"): "QUALIFIERS_R2", ("WG1", "PO"): "WORLD_GROUP_I_PO"}
    ties, counters = [], {}
    for t in raw:
        h = t["h"]
        stage = stage_of[(t["s"], t["r"])]
        team1, team2 = h[2], h[h.index("Tie Information") - 1]
        venue, surface = h[h.index("Venue") + 1], h[h.index("Surface") + 1]
        if stage == "FINALS_QF":
            position = DC_FINALS_QF.index((team1, team2)) + 1
        else:
            counters[stage] = counters.get(stage, 0) + 1
            position = counters[stage]
        rubbers = []
        for num, state, _dur, side1, side2 in t["m"]:
            score, winner = build_rubber(side1[2], side2[2], side1[1], side2[1])
            played = state == "Complete" and score is not None
            rubbers.append({"order": int(num), "doubles": " / " in side1[0], "p1": side1[0], "p2": side2[0],
                            "score": score if played else None, "winner": winner if played else None,
                            "status": "COMPLETED" if played else "NOT_PLAYED"})
        if not rubbers:  # Final 8 pas encore joue : 2 simples + 1 double a saisir
            rubbers = [{"order": i, "doubles": i == 3, "p1": None, "p2": None, "score": None, "winner": None,
                        "status": "PENDING"} for i in (1, 2, 3)]
        tie = {"competition": "DAVIS_CUP", "stage": stage, "group": None, "position": position,
               "team1": DC_COUNTRY[team1], "team2": DC_COUNTRY[team2], "ph1": None, "ph2": None,
               "dates": fr_dates(h[1]), "city": venue.split(", ")[-2] if venue.count(",") >= 1 else None,
               "venue": venue, "surface": fr_surface(surface), "rubbers": rubbers}
        if h[3] == "Played and completed":
            check_score(tie, int(h[4]), int(h[5]))
        ties.append(tie)
    for pos, (ph1, ph2), dates in ((1, ("Vainqueur QF1", "Vainqueur QF2"), "24 - 29 novembre 2026"),
                                   (2, ("Vainqueur QF3", "Vainqueur QF4"), "24 - 29 novembre 2026")):
        ties.append(dc_finals_slot("FINALS_SF", pos, ph1, ph2, dates))
    ties.append(dc_finals_slot("FINALS_F", 1, "Vainqueur SF1", "Vainqueur SF2", "29 novembre 2026"))
    return ties


def dc_finals_slot(stage, position, ph1, ph2, dates):
    return {"competition": "DAVIS_CUP", "stage": stage, "group": None, "position": position,
            "team1": None, "team2": None, "ph1": ph1, "ph2": ph2, "dates": dates, "city": "Bologna",
            "venue": "SuperTennis Arena, Bologna, Italy", "surface": "Dur (intérieur)",
            "rubbers": [{"order": i, "doubles": i == 3, "p1": None, "p2": None, "score": None, "winner": None,
                         "status": "PENDING"} for i in (1, 2, 3)]}


def assign_uc_teams(raw):
    """Chaque match liste le vainqueur en premier : l'equipe d'un joueur (ou
    d'une paire) = l'intersection des equipes des rencontres ou il apparait,
    affinee par elimination (dans un match, les deux cotes sont forcement
    d'equipes differentes)."""
    cand = {}
    for t in raw:
        for m in t["m"]:
            for side in (m[2], m[3]):
                for p in side[0].split(" / "):
                    cand[p] = cand.get(p, set(t["t"])) & set(t["t"])
    changed = True
    while changed:
        changed = False
        for t in raw:
            for m in t["m"]:
                a, b = m[2][0].split(" / "), m[3][0].split(" / ")
                for mine, other in ((a, b), (b, a)):
                    known = [cand[p] for p in other if len(cand[p]) == 1]
                    if known:
                        opp = next(iter(known[0]))
                        for p in mine:
                            new = cand[p] - {opp}
                            if new != cand[p] and new:
                                cand[p] = new
                                changed = True
    unresolved = [p for p, c in cand.items() if len(c) != 1]
    assert not unresolved, f"equipe inconnue pour {unresolved}"
    return {p: next(iter(c)) for p, c in cand.items()}


def uc_ties():
    raw = json.load(open("unitedcup_2026_raw.json", encoding="utf-8"))
    team_of = assign_uc_teams(raw)
    group_of = {code: g for g, codes in UC_GROUPS.items() for code in codes}
    ties = []
    group_counter = {}
    for t in sorted(raw, key=lambda t: re.sub(r"\D", "", fr_uc_day(t["d"]))):
        c1, c2 = t["t"]
        phase = t["m"][0][0].split(" - ")[0]
        venue = t["m"][0][0].split(" - ", 1)[1].strip()
        stage = {"Round Robin": "GROUP", "Quarterfinal": "QF", "Semifinal": "SF", "Final": "F"}[phase]
        group = group_of[c1] if stage == "GROUP" else None
        if stage == "GROUP":
            assert group_of[c2] == group
            group_counter[group] = group_counter.get(group, 0) + 1
            position = "ABCDEF".index(group) * 3 + group_counter[group]
        else:
            position = UC_KO[stage].index((c1, c2)) + 1
        rubbers = []
        for i, (_head, _dur, first, second, _note) in enumerate(t["m"], start=1):
            # remet chaque cote dans l'ordre equipe 1 / equipe 2 de la rencontre
            side1, side2 = (first, second) if team_of[first[0].split(" / ")[0]] == c1 else (second, first)
            score, winner = build_rubber(side1[2], side2[2], side1[1], side2[1])
            rubbers.append({"order": i, "doubles": " / " in side1[0], "p1": side1[0], "p2": side2[0],
                            "score": score, "winner": winner, "status": "COMPLETED"})
        tie = {"competition": "UNITED_CUP", "stage": stage, "group": group, "position": position,
               "team1": UC_COUNTRY[c1], "team2": UC_COUNTRY[c2], "ph1": None, "ph2": None,
               "dates": fr_uc_day(t["d"]), "city": t["c"], "venue": f"{venue}, {t['c']}",
               "surface": "Dur (extérieur)", "rubbers": rubbers}
        s1, s2 = (int(x) for x in t["sc"].split(" - "))
        check_score(tie, s1, s2)
        ties.append(tie)
    return ties


def check_score(tie, s1, s2):
    """Le score recalcule depuis les matchs doit etre celui du site officiel."""
    w1 = sum(1 for r in tie["rubbers"] if r["winner"] == 1)
    w2 = sum(1 for r in tie["rubbers"] if r["winner"] == 2)
    assert (w1, w2) == (s1, s2), f"{tie['team1']}-{tie['team2']} {tie['stage']}: {w1}-{w2} au lieu de {s1}-{s2}"


def finalize(tie):
    w1 = sum(1 for r in tie["rubbers"] if r["winner"] == 1)
    w2 = sum(1 for r in tie["rubbers"] if r["winner"] == 2)
    needed = len(tie["rubbers"]) // 2 + 1
    tie["s1"], tie["s2"] = w1, w2
    tie["winner"] = 1 if w1 >= needed else 2 if w2 >= needed else None
    tie["status"] = "COMPLETED" if tie["winner"] else "SCHEDULED"


def to_sql(ties):
    out = ["-- Genere par import/teams/build_seed.py - ne pas modifier a la main.",
           "-- Coupe Davis 2026 (qualifs tours 1-2, barrages du Groupe mondial I, Final 8 de",
           "-- Bologne) et United Cup 2026, sources : daviscup.com et unitedcup.com.", ""]
    for t in ties:
        finalize(t)
        out.append(
            "INSERT INTO team_tie (competition, season, stage, group_name, position, team1, team2, "
            "team1_placeholder, team2_placeholder, team1_score, team2_score, winner, status, dates, city, venue, surface) "
            f"VALUES ({q(t['competition'])}, 2026, {q(t['stage'])}, {q(t['group'])}, {t['position']}, "
            f"{q(t['team1'])}, {q(t['team2'])}, {q(t['ph1'])}, {q(t['ph2'])}, {t['s1']}, {t['s2']}, "
            f"{q(t['winner'])}, {q(t['status'])}, {q(t['dates'])}, {q(t['city'])}, {q(t['venue'])}, {q(t['surface'])});")
        for r in t["rubbers"]:
            out.append(
                "INSERT INTO team_rubber (tie_id, rubber_order, doubles, team1_players, team2_players, score, winner, status) "
                f"SELECT id, {r['order']}, {q(r['doubles'])}, {q(r['p1'])}, {q(r['p2'])}, {q(r['score'])}, "
                f"{q(r['winner'])}, {q(r['status'])} FROM team_tie WHERE competition = {q(t['competition'])} "
                f"AND season = 2026 AND stage = {q(t['stage'])} AND position = {t['position']};")
        out.append("")
    return "\n".join(out)


if __name__ == "__main__":
    all_ties = dc_ties() + uc_ties()
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write(to_sql(all_ties))
    by = {}
    for t in all_ties:
        by[(t["competition"], t["stage"])] = by.get((t["competition"], t["stage"]), 0) + 1
    for k, v in sorted(by.items()):
        print(k, v)
    print("->", OUT)
