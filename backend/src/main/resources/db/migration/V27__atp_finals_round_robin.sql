-- ATP Finals (Turin) : format "round robin" officiel (nittoatpfinals.com,
-- Rules & Format / Points & Prize Money) - 2 poules de 4, les 2 premiers de
-- chaque poule en demi-finales (1er A - 2e B, 1er B - 2e A), puis finale.
--
-- Dans un tournoi round robin, le bareme d'un tour = points PAR VICTOIRE a ce
-- tour (cumules) : 200 par victoire en poule, 400 la demi, 500 la finale
-- (vainqueur invaincu = 3 x 200 + 400 + 500 = 1500).
--
-- Positions des entrees : 1-4 = poule A, 5-8 = poule B, 9 et plus = remplacants.
-- Matchs : tour 1 = poules (positions 1-6 poule A, 7-12 poule B), tour 2 =
-- demi-finales, tour 3 = finale.

ALTER TABLE tournament ADD COLUMN round_robin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tournament ADD COLUMN group_a_name VARCHAR(60);
ALTER TABLE tournament ADD COLUMN group_b_name VARCHAR(60);

-- Turin n'a encore aucun joueur place : on remplace son tableau de 96 par le format poules.
DELETE FROM match_entry WHERE tournament_id = 243;
DELETE FROM tournament_round WHERE tournament_id = 243;

UPDATE tournament
SET round_robin = TRUE,
    draw_size = 8,
    runner_up_points = NULL,
    group_a_name = 'John McEnroe',
    group_b_name = 'Ivan Lendl'
WHERE id = 243;

INSERT INTO tournament_round (tournament_id, round_order, round_label, points) VALUES (243, 1, 'RR', 200);
INSERT INTO tournament_round (tournament_id, round_order, round_label, points) VALUES (243, 2, 'SF', 400);
INSERT INTO tournament_round (tournament_id, round_order, round_label, points) VALUES (243, 3, 'F', 500);

INSERT INTO match_entry (tournament_id, round_order, position_in_round, status)
SELECT 243, 1, p, 'PENDING' FROM generate_series(1, 12) AS p;
INSERT INTO match_entry (tournament_id, round_order, position_in_round, status)
SELECT 243, 2, p, 'PENDING' FROM generate_series(1, 2) AS p;
INSERT INTO match_entry (tournament_id, round_order, position_in_round, status)
VALUES (243, 3, 1, 'PENDING');
