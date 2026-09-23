-- Fusion du doublon Alvarez L / ALVAREZ VALDES LUIS CARLOS (Charles, 2026-09-22 :
-- "Alvarez L est un doublon de ALVAREZ VALDES LUIS CARLOS, tu gardes ce dernier").
-- Ce doublon avait echappe a la detection automatique de V14 (nom de famille
-- different : "Alvarez" vs "Alvarez Valdes").
--
-- id 525  ALVAREZ VALDES LUIS CARLOS MEXIQUE (fiche etablie) -> conservee
-- id 1644 Alvarez                 L   MEXIQUE (nom abrege)   -> fusionnee puis supprimee
DO $$
DECLARE
    keep_id CONSTANT INT := 525;
    drop_id CONSTANT INT := 1644;
BEGIN
    UPDATE entry e SET player_id = keep_id
    WHERE e.player_id = drop_id
      AND NOT EXISTS (
          SELECT 1 FROM entry e2 WHERE e2.tournament_id = e.tournament_id AND e2.player_id = keep_id
      );

    DELETE FROM entry WHERE player_id = drop_id;
    DELETE FROM player WHERE id = drop_id;
END $$;
