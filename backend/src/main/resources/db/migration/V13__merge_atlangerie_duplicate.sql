-- Fusion du doublon ATLANGERIE (Charles, 2026-09-22) : "j'ai trouvé un doublon
-- ATLANGERIE et ATLANGERIEV, tu fusionnes les 2, tu fusionnes leurs résultats,
-- et tu mets ATLANGERIEV BEKKHAN RUSSIE".
--
-- id 774  ATLANGERIE  BEKKHAN RUSSIE (fiche établie, legacySnapshotPoints renseigné) -> conservée
-- id 1452 ATLANGERIEV B        RUSSIE (nom complet correct mais fiche "à plat")     -> fusionnée puis supprimée
--
-- Meme logique de reprise des inscriptions que V12 : on reaffecte les
-- inscriptions du doublon vers la fiche conservee, sauf conflit (meme
-- tournoi deja pris par les 2 fiches) ou l'inscription en trop est
-- simplement supprimee.
DO $$
DECLARE
    keep_id CONSTANT INT := 774;
    drop_id CONSTANT INT := 1452;
BEGIN
    UPDATE entry e SET player_id = keep_id
    WHERE e.player_id = drop_id
      AND NOT EXISTS (
          SELECT 1 FROM entry e2 WHERE e2.tournament_id = e.tournament_id AND e2.player_id = keep_id
      );

    DELETE FROM entry WHERE player_id = drop_id;
    DELETE FROM player WHERE id = drop_id;
END $$;

UPDATE player SET last_name = 'ATLANGERIEV', first_name = 'BEKKHAN', nationality = 'RUSSIE' WHERE id = 774;
