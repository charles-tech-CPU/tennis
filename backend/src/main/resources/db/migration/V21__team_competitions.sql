-- Competitions par equipes nationales (Charles, 2026-09-23 : "la partie Coupe
-- Davis [...] les resultats, les tableaux de l'annee 2026, avec le futur top 8
-- [...] les barrages et [...] la United Cup").
--
-- Volontairement a part du modele tournoi/entry/match : une rencontre oppose
-- deux PAYS (pas deux joueurs) et se decide sur plusieurs matchs (3 ou 5), et
-- ces competitions ne rapportent aucun point au classement de l'appli.
--
-- team_tie    : une rencontre (ex. France - Slovaquie, qualifs 1er tour).
--               stage = phase (QUALIFIERS_R1, FINALS_QF, GROUP, QF...),
--               position = ordre dans la phase ; pour une phase a elimination
--               directe, le vainqueur de la position p alimente la position
--               ceil(p/2) de la phase suivante (equipe 1 si p impair).
-- team_rubber : un match de la rencontre (simple, double ou double mixte).
--               score et vainqueur sont du point de vue team1 / team2 de la
--               rencontre.
CREATE TABLE team_tie (
    id BIGSERIAL PRIMARY KEY,
    competition VARCHAR(20) NOT NULL,
    season INTEGER NOT NULL,
    stage VARCHAR(30) NOT NULL,
    group_name VARCHAR(10),
    position INTEGER NOT NULL,
    team1 VARCHAR(60),
    team2 VARCHAR(60),
    team1_placeholder VARCHAR(60),
    team2_placeholder VARCHAR(60),
    team1_score INTEGER NOT NULL DEFAULT 0,
    team2_score INTEGER NOT NULL DEFAULT 0,
    winner INTEGER,
    status VARCHAR(20) NOT NULL,
    dates VARCHAR(60),
    city VARCHAR(80),
    venue VARCHAR(200),
    surface VARCHAR(40),
    UNIQUE (competition, season, stage, position)
);
CREATE INDEX idx_team_tie_competition_season ON team_tie (competition, season);

CREATE TABLE team_rubber (
    id BIGSERIAL PRIMARY KEY,
    tie_id BIGINT NOT NULL REFERENCES team_tie (id) ON DELETE CASCADE,
    rubber_order INTEGER NOT NULL,
    doubles BOOLEAN NOT NULL,
    team1_players VARCHAR(200),
    team2_players VARCHAR(200),
    score VARCHAR(80),
    winner INTEGER,
    status VARCHAR(20) NOT NULL,
    UNIQUE (tie_id, rubber_order)
);
