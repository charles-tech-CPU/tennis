-- Joueurs marques comme favoris (etoile dans la liste des joueurs et sur la
-- fiche joueur), pour y acceder rapidement. Aucun joueur favori au depart.
ALTER TABLE player ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE;
