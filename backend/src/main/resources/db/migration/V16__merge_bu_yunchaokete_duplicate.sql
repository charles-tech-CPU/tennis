-- Fusion du doublon BU Y / YUNCHAOKETE BU (Charles, 2026-09-22 : "BU Y et
-- YUNCHAOKETE BU c'est le meme joueur, fusionne"). Le nom complet du joueur
-- ("Bu Yunchaokete") a ete importe a l'envers dans la fiche etablie
-- (lastName=YUNCHAOKETE, firstName=BU), ce qui a empeche la detection
-- automatique de V14 (elle ne comparait que le nom de famille).
--
-- id 121  YUNCHAOKETE BU CHINE (fiche etablie, legacySnapshotPoints renseigne) -> conservee
-- id 1423 BU          Y  CHINE (nom abrege)                                    -> fusionnee puis supprimee
DO $$
DECLARE
    keep_id CONSTANT INT := 121;
    drop_id CONSTANT INT := 1423;
BEGIN
    UPDATE entry e SET player_id = keep_id
    WHERE e.player_id = drop_id
      AND NOT EXISTS (
          SELECT 1 FROM entry e2 WHERE e2.tournament_id = e.tournament_id AND e2.player_id = keep_id
      );

    DELETE FROM entry WHERE player_id = drop_id;
    DELETE FROM player WHERE id = drop_id;
END $$;
