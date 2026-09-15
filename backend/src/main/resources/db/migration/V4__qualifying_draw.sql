-- Un tableau de qualifications est modelise comme un tournoi a part entiere (meme
-- structure bracket, juste plus petit et sans "finale" unique : le squelette s'arrete
-- au nombre de tours des qualifs, laissant plusieurs vainqueurs de tour simultanes =
-- les qualifies), relie au tournoi principal via main_tournament_id.
ALTER TABLE tournament ADD COLUMN is_qualifying BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tournament ADD COLUMN main_tournament_id BIGINT REFERENCES tournament (id);
CREATE UNIQUE INDEX uq_tournament_main_tournament_id ON tournament (main_tournament_id)
    WHERE main_tournament_id IS NOT NULL;
