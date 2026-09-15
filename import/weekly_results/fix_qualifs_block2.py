import json
import re
import sys
import urllib.request
sys.path.insert(0, r'C:\Users\Utilisateur\IdeaProjects\tennis-results\import\weekly_results')
from decode_bracket import decode_qualifying, cell
import openpyxl

BASE = "http://localhost:8082/api"
XLSX = r'C:\Users\Utilisateur\Desktop\EXCEL\TENNIS\2026\20260914TENNIS 2026.xlsx'

def call(method, path, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                  headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        raw = resp.read()
        return json.loads(raw) if raw else None

def norm(s):
    return (s or "").strip().upper()

def fmt(raw):
    if raw is None:
        return None
    if str(raw).upper() == 'WO':
        return 'w.o.'
    parts = str(raw).split()
    out = []
    for p in parts:
        if len(p) >= 2 and p[:2].isdigit():
            out.append(f"{p[0]}-{p[1]}" + (" ab." if p[2:] == 'AB' else ""))
        else:
            out.append(p)
    return " ".join(out)

def entry_type_for(tag):
    return {
        'WC': 'WILD_CARD', 'Q': 'QUALIFIER', 'LL': 'LUCKY_LOSER',
        'ALT': 'ALTERNATE', 'NG': 'NEW_GENERATION', 'SE': 'SPECIAL_EXEMPT'
    }.get(tag)

def seed_for(tag):
    return tag if isinstance(tag, int) else None

# (nom, header_row, X, id_tournoi_principal, id_ancien_tableau_quali_a_supprimer)
JOBS = [
    ('CANBERRA', 1, 19, 4, 246),
    ('BANGALORE', 19, 19, 5, 247),
    ('NOUMEA', 37, 19, 6, 249),
    ('NONTHABURI 1', 55, 19, 7, 250),
    ('NOTTINGHAM', 73, 19, 8, 251),
]

def main():
    wb = openpyxl.load_workbook(XLSX, data_only=True)
    ws = wb['1']
    players = call("GET", "/players")
    by_name = {}
    for p in players:
        by_name.setdefault(norm(p["lastName"]), []).append(p)

    for tname, hr, X, main_tid, old_qtid in JOBS:
        print(f"=== {tname} ===")
        q1 = decode_qualifying(ws, hr, X + 12)
        q2 = decode_qualifying(ws, hr, X + 17)
        print(f"  bloc1 size={q1['size']} pts={q1['points']}  bloc2 size={q2['size']} pts={q2['points']}")

        # 1. ancien tableau de qualifs deja supprime en base (psql, avant ce script)

        # 2. recree avec la taille combinee
        total_size = q1['size'] + q2['size']
        qpts = [{"roundOrder": 1, "roundLabel": "Q1", "points": 0},
                {"roundOrder": 2, "roundLabel": "Q2", "points": q1['points'][1]}]
        qt = call("POST", f"/tournaments/{main_tid}/qualifying", {"drawSize": total_size, "rounds": qpts})
        # runnerUpPoints = perdant Q2
        t = call("GET", f"/tournaments/{qt['id']}")
        call("PUT", f"/tournaments/{qt['id']}", {
            "weekNumber": t['weekNumber'], "country": t['country'], "mandatorySlot": t['mandatorySlot'],
            "qualifyingRound1Points": None, "qualifyingRound2Points": None,
            "runnerUpPoints": q1['points'][0], "rounds": qpts
        })
        qtid = qt['id']
        print(f"  nouveau tableau {qtid} cree (size={total_size})")

        # 3. entrees : bloc1 -> positions 1..size1, bloc2 -> size1+1..size1+size2
        draw = []
        for i, n in enumerate(q1['names']):
            draw.append((i + 1, n['name'], seed_for(n['tag']), entry_type_for(n['tag'])))
        off = q1['size']
        for i, n in enumerate(q2['names']):
            draw.append((off + i + 1, n['name'], seed_for(n['tag']), entry_type_for(n['tag'])))

        for pos, name, seed, etype in draw:
            if name is None:
                continue
            cands = by_name.get(norm(name))
            if not cands:
                new_player = call("POST", "/players", {"lastName": name, "firstName": None, "nationality": None})
                by_name.setdefault(norm(name), []).append(new_player)
                cands = by_name[norm(name)]
                print(f"    joueur cree: {name!r} -> id {new_player['id']}")
            pid = cands[0]["id"]
            if len(cands) > 1:
                print(f"    (ambigu {name}: {[c['id'] for c in cands]}, on prend {pid})")
            call("POST", f"/tournaments/{qtid}/entries", {
                "playerId": pid, "drawPosition": pos, "seed": seed, "entryType": etype, "bye": False
            })

        # 4. scores : bloc1 rounds 1-2 (positions inchangees), bloc2 rounds 1-2 (positions decalees)
        for round_order in (1, 2):
            matches = call("GET", f"/tournaments/{qtid}/matches")
            by_pos = {m["positionInRound"]: m for m in matches if m["roundOrder"] == round_order}
            results = []
            for i, res in enumerate(q1['rounds'][round_order]):
                results.append((i + 1, res['winner_name'], res['score']))
            off2 = len(q1['rounds'][round_order])
            for i, res in enumerate(q2['rounds'][round_order]):
                results.append((off2 + i + 1, res['winner_name'], res['score']))
            for pos, winner_name, score_raw in results:
                m = by_pos.get(pos)
                if not m:
                    print(f"    round{round_order} pos{pos}: match introuvable - SKIP")
                    continue
                e1, e2 = m["entry1"], m["entry2"]
                if e1 is None or e2 is None or winner_name is None:
                    print(f"    round{round_order} pos{pos}: donnees incompletes - SKIP")
                    continue
                wn = norm(winner_name)
                if norm(e1["playerLastName"] or "") == wn:
                    wid = e1["id"]
                elif norm(e2["playerLastName"] or "") == wn:
                    wid = e2["id"]
                else:
                    print(f"    round{round_order} pos{pos}: {winner_name} ne correspond pas a {e1['playerLastName']}/{e2['playerLastName']} - SKIP")
                    continue
                call("PUT", f"/matches/{m['id']}/score", {"score": fmt(score_raw), "winnerEntryId": wid})
        print(f"  qualifs completes (24 joueurs, 6 qualifies).")

if __name__ == '__main__':
    main()
