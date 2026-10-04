-- Classement du joueur fige au demarrage du tournoi (premier match saisi),
-- comme le classement affiche par l'ATP a cote de chaque joueur d'un tableau.
-- Null = joueur non classe a ce moment-la, ou tournoi demarre avant cette
-- colonne (Charles, 2026-10-04 : pas de rattrapage, on laisse vide).
ALTER TABLE entry ADD COLUMN ranking_at_entry INTEGER;

-- True une fois les classements du tableau figes. Les tournois deja demarres
-- (au moins un match saisi) sont marques figes d'office : ils ne seront donc
-- jamais remplis avec un classement posterieur au tournoi.
ALTER TABLE tournament ADD COLUMN rankings_frozen BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE tournament t SET rankings_frozen = TRUE
WHERE EXISTS (SELECT 1 FROM match_entry m WHERE m.tournament_id = t.id AND m.status = 'COMPLETED');

-- Surface (HARD, CLAY, GRASS, CARPET) et indoor/outdoor, a renseigner a la main :
-- null = non renseigne.
ALTER TABLE tournament ADD COLUMN surface VARCHAR(20);
ALTER TABLE tournament ADD COLUMN indoor BOOLEAN;
