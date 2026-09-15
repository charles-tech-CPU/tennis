import json
import re
import sys
import urllib.request
sys.path.insert(0, r'C:\Users\UTILIS~1\AppData\Local\Temp\claude\C--Users-Utilisateur-IdeaProjects-tennis-results\7cee9972-7e4a-4470-b42d-7c4efca461ca\scratchpad')
from decode_bracket import decode_main_draw, decode_qualifying
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

def fmt_score(raw):
    if raw is None:
        return None
    raw = str(raw).strip()
    if raw.upper() == 'WO':
        return 'w.o.'
    # "63 12AB" -> "6-3 1-2 ab."
    parts = raw.split()
    out = []
    for p in parts:
        m = re.match(r'^(\d)(\d)(AB)?$', p)
        if m:
            out.append(f"{m.group(1)}-{m.group(2)}" + (" ab." if m.group(3) else ""))
        else:
            out.append(p)
    return " ".join(out)

def entry_type_for(tag):
    if tag == 'WC':
        return 'WILD_CARD'
    if tag == 'Q':
        return 'QUALIFIER'
    if tag == 'LL':
        return 'LUCKY_LOSER'
    if tag == 'ALT':
        return 'ALTERNATE'
    if tag == 'NG':
        return 'NEW_GENERATION'
    if tag == 'SE':
        return 'SPECIAL_EXEMPT'
    if tag == 'PR':
        return 'PROTECTED_RANKING'
    if tag == 'CO':
        return 'CUT_OFF'
    return None

def seed_for(tag):
    return tag if isinstance(tag, int) else None

def build_draw(names_left, names_right):
    draw = []
    for i, n in enumerate(names_left):
        draw.append((i + 1, n['name'], seed_for(n['tag']), entry_type_for(n['tag'])))
    off = len(names_left)
    for i, n in enumerate(names_right):
        draw.append((off + i + 1, n['name'], seed_for(n['tag']), entry_type_for(n['tag'])))
    return draw

def push_entries(tid, draw, players_by_name):
    existing = call("GET", f"/tournaments/{tid}/entries")
    used_positions = {e["drawPosition"] for e in existing}
    for pos, name, seed, etype in draw:
        if pos in used_positions or name is None:
            continue
        if norm(name) == 'BYE':
            call("POST", f"/tournaments/{tid}/entries", {
                "playerId": None, "drawPosition": pos, "seed": seed, "entryType": None, "bye": True
            })
            continue
        cands = players_by_name.get(norm(name))
        if not cands:
            # joueur absent du snapshot ATP importe (ex: qualifie tres bas classe) - on le cree
            new_player = call("POST", "/players", {"lastName": name, "firstName": None, "nationality": None})
            players_by_name.setdefault(norm(name), []).append(new_player)
            cands = players_by_name[norm(name)]
            print(f"  joueur cree: {name!r} -> id {new_player['id']}")
        pid = cands[0]["id"]
        if len(cands) > 1:
            print(f"  (ambigu {name}: {[c['id'] for c in cands]}, on prend {pid})")
        call("POST", f"/tournaments/{tid}/entries", {
            "playerId": pid, "drawPosition": pos, "seed": seed, "entryType": etype, "bye": False
        })

def push_rounds(tid, rounds_by_order):
    """rounds_by_order: {round_order: [(position_in_round, winner_name, score_raw), ...]}"""
    for round_order in sorted(rounds_by_order.keys()):
        matches = call("GET", f"/tournaments/{tid}/matches")
        by_pos = {m["positionInRound"]: m for m in matches if m["roundOrder"] == round_order}
        for pos, winner_name, score_raw in rounds_by_order[round_order]:
            m = by_pos.get(pos)
            if not m:
                print(f"  round{round_order} pos{pos}: match introuvable - SKIP")
                continue
            e1, e2 = m["entry1"], m["entry2"]
            if e1 is None or e2 is None or m["status"] == "COMPLETED":
                continue
            if e1.get("bye") or e2.get("bye"):
                continue
            wn = norm(winner_name)
            if norm(e1["playerLastName"] or "") == wn:
                winner_id = e1["id"]
            elif norm(e2["playerLastName"] or "") == wn:
                winner_id = e2["id"]
            else:
                print(f"  round{round_order} pos{pos}: winner {winner_name!r} ne correspond ni a {e1['playerLastName']} ni a {e2['playerLastName']} - SKIP")
                continue
            call("PUT", f"/matches/{m['id']}/score", {"score": fmt_score(score_raw), "winnerEntryId": winner_id})

def rounds_from_decoded(rounds_dict, max_round):
    out = {}
    for rnd, results in rounds_dict.items():
        if rnd > max_round:
            continue
        lst = []
        for i, res in enumerate(results):
            lst.append((i + 1, res['winner_name'], res['score']))
        out[rnd] = lst
    return out

def merge_rounds(a, b):
    out = dict(a)
    for k, v in b.items():
        out.setdefault(k, [])
        # renumerote les positions en continuant celles de a pour ce round
        base = len(out[k])
        for j, (pos, w, s) in enumerate(v):
            out[k].append((base + pos, w, s))
    return out

def process(tname, sheet, header_row, X, tid, max_main_round, do_qualifying=True):
    print(f"=== {tname} (tournament id {tid}) ===")
    wb = openpyxl.load_workbook(XLSX, data_only=True)
    ws = wb[sheet]
    d = decode_main_draw(ws, header_row, X)

    players = call("GET", "/players")
    by_name = {}
    for p in players:
        by_name.setdefault(norm(p["lastName"]), []).append(p)

    draw = build_draw(d['left_names'], d['right_names'])
    push_entries(tid, draw, by_name)

    left_r = rounds_from_decoded(d['left_rounds'], max_main_round)
    right_r = rounds_from_decoded(d['right_rounds'], max_main_round)
    all_rounds = merge_rounds(left_r, right_r)
    push_rounds(tid, all_rounds)
    print(f"  main draw: rounds 1..{max_main_round} pousses.")

    if do_qualifying:
        q = decode_qualifying(ws, header_row, X + 12)
        qpts = [{"roundOrder": 1, "roundLabel": "Q1", "points": q['points'][0]},
                {"roundOrder": 2, "roundLabel": "Q2", "points": q['points'][1]}]
        main_t = call("GET", f"/tournaments/{tid}")
        qtid = main_t.get("qualifyingTournamentId")
        if qtid is None:
            qt = call("POST", f"/tournaments/{tid}/qualifying", {"drawSize": q['size'], "rounds": qpts})
            qtid = qt['id']
            print(f"  qualifs creees (id {qtid}, size {q['size']})")
        else:
            print(f"  qualifs deja presentes (id {qtid})")

        qdraw = [(i + 1, n['name'], seed_for(n['tag']), entry_type_for(n['tag'])) for i, n in enumerate(q['names'])]
        push_entries(qtid, qdraw, by_name)
        qrounds = rounds_from_decoded(q['rounds'], 2)
        push_rounds(qtid, qrounds)
        print("  qualifs: rounds 1..2 pousses.")

if __name__ == '__main__':
    import sys as _sys
    # (nom, feuille, ligne d'en-tete, colonne X, id tournoi en base, dernier tour principal fiable)
    # Semaine 1 - garde comme reference/historique. Pour une semaine suivante, dupliquer
    # ce fichier ou remplacer cette liste (voir README section 9 pour la methode).
    jobs = [
        ('CANBERRA', '1', 1, 19, 4, 4),
        ('BANGALORE', '1', 19, 19, 5, 4),
        ('HONG KONG', '1', 27, 1, 2, 3),
        ('NOUMEA', '1', 37, 19, 6, 3),
        ('NONTHABURI 1', '1', 55, 19, 7, 3),
        ('NOTTINGHAM', '1', 73, 19, 8, 3),
    ]
    filt = _sys.argv[1] if len(_sys.argv) > 1 else None
    for tname, sheet, hr, X, tid, max_round in jobs:
        if filt and filt.upper() not in tname:
            continue
        process(tname, sheet, hr, X, tid, max_round)
        print()
