-- Photo hebdomadaire du classement de l'appli (celui calcule par RankingService
-- a partir des matchs saisis, jamais une source externe) : classement "du
-- lundi" de la semaine week_number, c'est-a-dire calcule avec les seuls
-- tournois des semaines precedentes (RankingService.computeRankingBefore).
-- Sert a tracer l'evolution du classement d'un joueur sur sa fiche.
--
-- Simple cache : une semaine est calculee a la demande la premiere fois, puis
-- effacee (et recalculee plus tard) des qu'un resultat d'une semaine
-- anterieure change - voir RankingSnapshotService.

-- Semaines deja calculees (y compris celles ou personne n'est encore classe).
CREATE TABLE ranking_snapshot_week (
    season INTEGER NOT NULL,
    week_number INTEGER NOT NULL,
    computed_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (season, week_number)
);

CREATE TABLE ranking_snapshot (
    season INTEGER NOT NULL,
    week_number INTEGER NOT NULL,
    player_id BIGINT NOT NULL REFERENCES player (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    points INTEGER NOT NULL,
    PRIMARY KEY (season, week_number, player_id),
    FOREIGN KEY (season, week_number) REFERENCES ranking_snapshot_week (season, week_number) ON DELETE CASCADE
);
CREATE INDEX idx_ranking_snapshot_player ON ranking_snapshot (player_id, season);
