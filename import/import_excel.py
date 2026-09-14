#!/usr/bin/env python3
"""
Import du fichier Excel "TENNIS 2026" de Charles vers un fichier SQL Flyway
(V2__seed_data.sql) pour le projet tennis-results.

Ce qui est importe (fiable, structure a plat) :
  - la feuille "ATP" : les joueurs et leur points de classement ACTUELS, stockes
    comme simple photo informative (player.legacy_snapshot_points) - pas utilises
    par le calcul de classement "vivant" de l'appli (RankingService), qui repart
    de zero et se construit uniquement a partir des tournois saisis dans l'appli.
  - la feuille "CALENDRIER ATP 2026" : la liste des tournois de la saison (nom,
    categorie, semaine), avec un tableau (bracket) vide pret a etre rempli dans
    l'appli.

Ce qui n'est PAS importe (delibrement, apres analyse - voir le README) :
  - les 48 feuilles de semaine (les vrais tableaux a elimination directe deja
    joues) : mise en page trop irreguliere pour un parsing automatique fiable.
    A ressaisir a la main dans l'appli pour les tournois qui interessent Charles.
"""
import re
import sys

import openpyxl

SEASON = 2026

CATEGORY_COLUMNS = [
    (4, "GRAND_SLAM"),
    (5, "MASTERS_1000"),
    (6, "ATP_500"),
    (7, "ATP_250"),
    (8, "ATP_175"),
    (9, "ATP_125"),
    (10, "ATP_100"),
    (11, "ATP_75"),
    (12, "ATP_50"),
]

DEFAULT_DRAW_SIZE = {
    "GRAND_SLAM": 128,
    "MASTERS_1000": 96,
    "ATP_500": 48,
    "ATP_250": 32,
    "ATP_175": 32,
    "ATP_125": 32,
    "ATP_100": 28,
    "ATP_75": 24,
    "ATP_50": 24,
}

# Points par defaut par (categorie, drawSlots = plus petite puissance de 2 >= drawSize),
# repris de CategoryDefaults.java pour que la base seedee soit coherente avec ce que
# l'appli proposerait si on creait le tournoi a la main.
DEFAULTS = {
    ("GRAND_SLAM", 128): [10, 45, 90, 180, 360, 720, 2000],
    ("MASTERS_1000", 128): [10, 30, 45, 90, 180, 360, 1000],
    ("MASTERS_1000", 64): [25, 45, 90, 180, 360, 1000],
    ("ATP_500", 64): [20, 45, 90, 180, 500],
    ("ATP_500", 32): [45, 90, 180, 500],
    ("ATP_250", 32): [25, 50, 100, 165, 250],
    ("ATP_250", 16): [50, 100, 165, 250],
    ("ATP_175", 32): [18, 35, 70, 115, 175],
    ("ATP_125", 32): [13, 25, 50, 80, 125],
    ("ATP_100", 32): [10, 20, 40, 65, 100],
    ("ATP_75", 32): [8, 15, 30, 48, 75],
    ("ATP_50", 32): [5, 10, 20, 32, 50],
}

BASE_POINTS = {
    "GRAND_SLAM": 2000, "MASTERS_1000": 1000, "ATP_500": 500, "ATP_250": 250,
    "ATP_175": 175, "ATP_125": 125, "ATP_100": 100, "ATP_75": 75, "ATP_50": 50,
}

MANDATORY_SLOTS = {
    "AUSTRALIAN OPEN": "AUSTRALIAN_OPEN",
    "ROLAND GARROS": "ROLAND_GARROS",
    "WIMBLEDON": "WIMBLEDON",
    "US OPEN": "US_OPEN",
    "TURIN": "ATP_FINALS",
    "INDIAN WELLS": "INDIAN_WELLS",
    "MIAMI": "MIAMI",
    "MONTE CARLO": "MONTE_CARLO",
    "MADRID": "MADRID",
    "ROME": "ROME",
    "CANADA": "CANADA",
    "CINCINNATI": "CINCINNATI",
    "SHANGHAI": "SHANGHAI",
    "PARIS": "PARIS_BERCY",
}


def next_pow2(n):
    p = 1
    while p < n:
        p *= 2
    return p


def round_count(slots):
    r = 0
    while slots > 1:
        slots //= 2
        r += 1
    return r


def round_label(round_order, total_rounds):
    remaining = total_rounds - round_order
    if remaining == 0:
        return "F"
    if remaining == 1:
        return "SF"
    if remaining == 2:
        return "QF"
    return f"R{1 << (remaining + 1)}"


def points_for(category, draw_slots):
    exact = DEFAULTS.get((category, draw_slots))
    if exact:
        return exact
    rounds = round_count(draw_slots)
    base = BASE_POINTS[category]
    pts = []
    for i in range(rounds, 0, -1):
        pts.append(max(1, base >> (rounds - i)))
    return pts


def sql_str(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def sql_int(value):
    return "NULL" if value is None else str(int(value))


def main():
    if len(sys.argv) != 2:
        print("Usage: python3 import_excel.py <chemin_vers_TENNIS.xlsx>", file=sys.stderr)
        sys.exit(1)

    wb = openpyxl.load_workbook(sys.argv[1], data_only=True)
    out = []

    # ---------- Joueurs (feuille ATP) ----------
    ws = wb["ATP"]
    player_count = 0
    for r in range(3, ws.max_row + 1):
        cla = ws.cell(row=r, column=1).value
        last_name = ws.cell(row=r, column=2).value
        first_name = ws.cell(row=r, column=3).value
        nation = ws.cell(row=r, column=4).value
        total = ws.cell(row=r, column=5).value
        if not last_name:
            continue
        out.append(
            f"INSERT INTO player (id, last_name, first_name, nationality, legacy_snapshot_points) "
            f"VALUES ({r - 2}, {sql_str(last_name)}, {sql_str(first_name)}, {sql_str(nation)}, {sql_int(total)});"
        )
        player_count += 1
    out.append(f"SELECT setval('player_id_seq', {player_count});")

    # ---------- Tournois (feuille calendrier) ----------
    ws = wb["CALENDRIER ATP 2026"]
    tournament_id = 0
    match_id = 0
    week_number = None
    skipped_empty = 0

    for r in range(2, ws.max_row + 1):
        week_cell = ws.cell(row=r, column=1).value
        if week_cell is not None:
            week_number = int(week_cell)

        for col, category in CATEGORY_COLUMNS:
            cell_value = ws.cell(row=r, column=col).value
            if not cell_value:
                continue
            names = [n.strip() for n in str(cell_value).split("\n") if n.strip()]
            for raw_name in names:
                name = re.sub(r"\s+20\d\d$", "", raw_name).strip()  # enleve un eventuel "2026" en suffixe
                if not name:
                    skipped_empty += 1
                    continue

                tournament_id += 1
                draw_size = DEFAULT_DRAW_SIZE[category]
                draw_slots = next_pow2(draw_size)
                total_rounds = round_count(draw_slots)
                # Le meme nom de ville peut designer un petit tournoi ATP_75/100/etc a
                # cote du grand tournoi (ex: "MADRID" existe aussi comme petit Challenger,
                # "SHANGHAI" comme petit ATP_100) : la case obligatoire ne s'applique
                # qu'aux vraies categories GRAND_SLAM / MASTERS_1000.
                mandatory_slot = (
                    MANDATORY_SLOTS.get(name.upper())
                    if category in ("GRAND_SLAM", "MASTERS_1000")
                    else None
                )

                out.append(
                    f"INSERT INTO tournament (id, name, category, season, week_number, country, "
                    f"mandatory_slot, draw_size) VALUES ({tournament_id}, {sql_str(name)}, "
                    f"{sql_str(category)}, {SEASON}, {sql_int(week_number)}, NULL, "
                    f"{sql_str(mandatory_slot)}, {draw_size});"
                )

                points = points_for(category, draw_slots)
                for ro in range(1, total_rounds + 1):
                    pts = points[ro - 1] if ro - 1 < len(points) else points[-1]
                    label = round_label(ro, total_rounds)
                    out.append(
                        f"INSERT INTO tournament_round (tournament_id, round_order, round_label, points) "
                        f"VALUES ({tournament_id}, {ro}, {sql_str(label)}, {pts});"
                    )
                    matches_in_round = draw_slots >> ro
                    for pos in range(1, matches_in_round + 1):
                        match_id += 1
                        out.append(
                            f"INSERT INTO match_entry (id, tournament_id, round_order, position_in_round, status) "
                            f"VALUES ({match_id}, {tournament_id}, {ro}, {pos}, 'PENDING');"
                        )

    out.append(f"SELECT setval('tournament_id_seq', {tournament_id});")
    out.append(f"SELECT setval('tournament_round_id_seq', (SELECT COALESCE(MAX(id),1) FROM tournament_round));")
    out.append(f"SELECT setval('match_entry_id_seq', {match_id});")
    out.append(f"SELECT setval('entry_id_seq', 1);")

    print("\n".join(out))
    print(
        f"-- Import termine : {player_count} joueurs, {tournament_id} tournois, {match_id} matchs (vides) crees.",
        file=sys.stderr,
    )
    if skipped_empty:
        print(f"-- {skipped_empty} cellules de tournoi vides ignorees.", file=sys.stderr)


if __name__ == "__main__":
    main()
