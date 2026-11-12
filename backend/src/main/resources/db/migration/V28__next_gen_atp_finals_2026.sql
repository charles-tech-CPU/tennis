-- Next Gen ATP Finals 2026 (nextgenatpfinals.com) : 9-13 decembre, Pala
-- Calafiore, Reggio Calabria (Italie), salle couverte. Meme format que les
-- ATP Finals (2 poules de 4, demi-finales croisees, finale, voir V27) mais
-- sans aucun point ATP : bareme a 0 a tous les tours.
INSERT INTO tournament (name, category, season, week_number, country, surface, indoor, draw_size, round_robin)
VALUES ('REGGIO CALABRIA', 'NEXT_GEN', 2026, 50, 'ITALIE', 'HARD', TRUE, 8, TRUE);

INSERT INTO tournament_round (tournament_id, round_order, round_label, points)
SELECT id, r.round_order, r.round_label, 0
FROM tournament,
     (VALUES (1, 'RR'), (2, 'SF'), (3, 'F')) AS r (round_order, round_label)
WHERE category = 'NEXT_GEN' AND season = 2026;

INSERT INTO match_entry (tournament_id, round_order, position_in_round, status)
SELECT t.id, m.round_order, m.position_in_round, 'PENDING'
FROM tournament t,
     (SELECT 1 AS round_order, p AS position_in_round FROM generate_series(1, 12) AS p
      UNION ALL SELECT 2, p FROM generate_series(1, 2) AS p
      UNION ALL SELECT 3, 1) AS m
WHERE t.category = 'NEXT_GEN' AND t.season = 2026;
