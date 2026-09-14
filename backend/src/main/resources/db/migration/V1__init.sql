CREATE TABLE player (
    id BIGSERIAL PRIMARY KEY,
    last_name VARCHAR(120) NOT NULL,
    first_name VARCHAR(120),
    nationality VARCHAR(60),
    legacy_snapshot_points INTEGER
);
CREATE INDEX idx_player_last_name ON player (last_name);

CREATE TABLE tournament (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(30) NOT NULL,
    season INTEGER NOT NULL,
    week_number INTEGER,
    country VARCHAR(80),
    mandatory_slot VARCHAR(30),
    draw_size INTEGER NOT NULL,
    qualifying_round1_points INTEGER,
    qualifying_round2_points INTEGER,
    runner_up_points INTEGER
);
CREATE INDEX idx_tournament_season ON tournament (season);
-- Au plus un tournoi par "case obligatoire" et par saison (si renseignee).
CREATE UNIQUE INDEX uq_tournament_mandatory_slot_season
    ON tournament (season, mandatory_slot)
    WHERE mandatory_slot IS NOT NULL;

CREATE TABLE tournament_round (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    round_order INTEGER NOT NULL,
    round_label VARCHAR(10) NOT NULL,
    points INTEGER NOT NULL,
    UNIQUE (tournament_id, round_order)
);

CREATE TABLE entry (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    player_id BIGINT REFERENCES player (id),
    draw_position INTEGER NOT NULL,
    seed INTEGER,
    entry_type VARCHAR(20),
    bye BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (tournament_id, draw_position),
    CONSTRAINT chk_entry_player_or_bye CHECK (bye = TRUE OR player_id IS NOT NULL)
);

CREATE TABLE match_entry (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournament (id) ON DELETE CASCADE,
    round_order INTEGER NOT NULL,
    position_in_round INTEGER NOT NULL,
    entry1_id BIGINT REFERENCES entry (id),
    entry2_id BIGINT REFERENCES entry (id),
    score VARCHAR(60),
    winner_entry_id BIGINT REFERENCES entry (id),
    status VARCHAR(20) NOT NULL,
    UNIQUE (tournament_id, round_order, position_in_round),
    CONSTRAINT chk_match_entries_distinct CHECK (entry1_id IS NULL OR entry2_id IS NULL OR entry1_id <> entry2_id)
);
CREATE INDEX idx_match_entry1 ON match_entry (entry1_id);
CREATE INDEX idx_match_entry2 ON match_entry (entry2_id);
