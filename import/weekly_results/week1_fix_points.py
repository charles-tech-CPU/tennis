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

# Barème original lu dans Excel et applique tel quel (FAUX : decale d'un cran).
# Le vrai sens : la valeur sous "R32" est en fait les points pour une elimination
# en R16, etc. R32 (1er tour) vaut toujours 0, et la valeur la plus haute est le
# "F" = points du FINALISTE BATTU (pas du tour F lui-meme).
MAIN_DRAWS = {
    3: [25, 50, 100, 165, 250],   # Brisbane
    4: [8, 16, 35, 64, 125],      # Canberra
    5: [8, 16, 35, 64, 125],      # Bangalore
    2: [25, 50, 100, 165, 250],   # Hong Kong
    6: [6, 12, 22, 44, 75],       # Noumea
    7: [4, 8, 14, 25, 50],        # Nonthaburi 1
    8: [4, 8, 14, 25, 50],        # Nottingham
}

QUALIFYING = {
    245: [7, 13],   # Brisbane quals
    246: [3, 5],    # Canberra quals
    247: [3, 5],    # Bangalore quals
    248: [7, 13],   # Hong Kong quals
    249: [2, 4],    # Noumea quals
    250: [1, 3],    # Nonthaburi 1 quals
    251: [1, 3],    # Nottingham quals
}

def fix_main(tid, orig):
    p1, p2, p3, p4, p5 = orig
    t = call("GET", f"/tournaments/{tid}")
    rounds = [{"roundOrder": 1, "roundLabel": t['rounds'][0]['roundLabel'], "points": 0},
              {"roundOrder": 2, "roundLabel": t['rounds'][1]['roundLabel'], "points": p1},
              {"roundOrder": 3, "roundLabel": t['rounds'][2]['roundLabel'], "points": p2},
              {"roundOrder": 4, "roundLabel": t['rounds'][3]['roundLabel'], "points": p3},
              {"roundOrder": 5, "roundLabel": t['rounds'][4]['roundLabel'], "points": p5}]
    call("PUT", f"/tournaments/{tid}", {
        "weekNumber": t['weekNumber'], "country": t['country'], "mandatorySlot": t['mandatorySlot'],
        "qualifyingRound1Points": t['qualifyingRound1Points'], "qualifyingRound2Points": t['qualifyingRound2Points'],
        "runnerUpPoints": p4, "rounds": rounds
    })
    print(f"tournament {tid}: rounds={[r['points'] for r in rounds]} runnerUp={p4}")

def fix_qualifying(tid, orig):
    q1, q2 = orig
    t = call("GET", f"/tournaments/{tid}")
    rounds = [{"roundOrder": 1, "roundLabel": t['rounds'][0]['roundLabel'], "points": 0},
              {"roundOrder": 2, "roundLabel": t['rounds'][1]['roundLabel'], "points": q2}]
    call("PUT", f"/tournaments/{tid}", {
        "weekNumber": t['weekNumber'], "country": t['country'], "mandatorySlot": t['mandatorySlot'],
        "qualifyingRound1Points": t['qualifyingRound1Points'], "qualifyingRound2Points": t['qualifyingRound2Points'],
        "runnerUpPoints": q1, "rounds": rounds
    })
    print(f"qualifying {tid}: rounds={[r['points'] for r in rounds]} runnerUp={q1}")

if __name__ == '__main__':
    for tid, orig in MAIN_DRAWS.items():
        fix_main(tid, orig)
    for tid, orig in QUALIFYING.items():
        fix_qualifying(tid, orig)
