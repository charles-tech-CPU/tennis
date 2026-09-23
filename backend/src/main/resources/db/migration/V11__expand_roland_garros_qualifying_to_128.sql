-- Les qualifs de Roland Garros (tournoi 433) avaient ete creees en tableau
-- de 64 (32 matchs Q1 -> 16 Q2 -> 8 Q3 -> 8 qualifies) au lieu du vrai
-- tableau de 128 (Charles, 2026-09-20 : "il faut ajouter 64 autres joueurs
-- en dessous de ceux deja la"). On agrandit vers un vrai tableau de 128 :
-- Q1 64 matchs -> Q2 32 -> Q3 16 -> 16 qualifies pour le tableau principal.
--
-- Les positions/matchs existants (1-64 en Q1, joueurs deja saisis et
-- matchs deja joues) ne sont pas touches : on ajoute uniquement les
-- nouveaux matchs vides necessaires pour la deuxieme moitie du tableau
-- (positions 65-128), en respectant le meme schema d'avancement
-- positionnel que BracketService (tour N+1, position ceil(pos/2)) :
--   - Q1 (round_order=1) : positions 33-64 (32 nouveaux matchs, 64 nouvelles
--     places de joueurs, 65 a 128)
--   - Q2 (round_order=2) : positions 17-32 (16 nouveaux matchs, vainqueurs
--     des nouveaux matchs Q1)
--   - Q3 (round_order=3) : positions 9-16 (8 nouveaux matchs, vainqueurs
--     des nouveaux matchs Q2)
-- Ces matchs restent vides (PENDING) : les joueurs seront saisis a la main
-- via l'interface, qui remplira automatiquement les matchs Q1 correspondants.
UPDATE tournament SET draw_size = 128 WHERE id = 433;

INSERT INTO match_entry (tournament_id, round_order, position_in_round, status)
SELECT 433, 1, gs, 'PENDING' FROM generate_series(33, 64) AS gs;

INSERT INTO match_entry (tournament_id, round_order, position_in_round, status)
SELECT 433, 2, gs, 'PENDING' FROM generate_series(17, 32) AS gs;

INSERT INTO match_entry (tournament_id, round_order, position_in_round, status)
SELECT 433, 3, gs, 'PENDING' FROM generate_series(9, 16) AS gs;
