import json
import urllib.request

BASE = "http://localhost:8082/api"

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

def submit(tid, round_order, pos, winner_name, score_raw):
    matches = call("GET", f"/tournaments/{tid}/matches")
    m = next(mm for mm in matches if mm["roundOrder"] == round_order and mm["positionInRound"] == pos)
    e1, e2 = m["entry1"], m["entry2"]
    wn = norm(winner_name)
    if norm(e1["playerLastName"]) == wn:
        wid = e1["id"]
    elif norm(e2["playerLastName"]) == wn:
        wid = e2["id"]
    else:
        print(f"  MISMATCH tid={tid} r{round_order}p{pos}: {winner_name} vs {e1['playerLastName']}/{e2['playerLastName']}")
        return
    call("PUT", f"/matches/{m['id']}/score", {"score": fmt(score_raw), "winnerEntryId": wid})
    print(f"  tid={tid} r{round_order}p{pos}: {e1['playerLastName']} vs {e2['playerLastName']} -> {winner_name} ({fmt(score_raw)})")

# tid: (left_SF_winner, left_SF_score, right_SF_winner, right_SF_score, final_winner, final_score)
DATA = {
    4:  ('BLOCKX', '63 67 63', 'JODAR', '63 00AB', 'BLOCKX', '64 64'),        # Canberra
    5:  ('MARTINEZ', '76 61', 'SKATOV', '64 63', 'MARTINEZ', '76 63'),        # Bangalore
    2:  ('MUSETTI', '67 75 64', 'BUBLIK', '36 64 62', 'BUBLIK', '76 63'),     # Hong Kong
    6:  ('RODIONOV', '64 67 63', 'GEA', '46 61 64', 'GEA', '63 46 75'),       # Noumea
    7:  ('GENGEL', '62 75', 'NOGUCHI', '62 62', 'NOGUCHI', '63 64'),          # Nonthaburi 1
    8:  ('MONDAY', '64 62', 'CHIDEKH', '63 57 63', 'CHIDEKH', '57 62 76'),    # Nottingham
}

for tid, (lw, ls, rw, rs, fw, fs) in DATA.items():
    print(f"=== tournament {tid} ===")
    submit(tid, 4, 1, lw, ls)
    submit(tid, 4, 2, rw, rs)
    submit(tid, 5, 1, fw, fs)
