-- Nettoyage des joueurs (Charles, 2026-09-20 : "je veux que tu cherches s'il
-- y a des doublons, des absences de prenom, des absences de pays etc").
--
-- 1) Doublons detectes (meme nom+prenom) :
--    - HURRION MILLEN : 615 (fiche etablie) / 1023
--    - MANSILLA DIEZ MARIO : 993 (fiche etablie) / 1302
--    - PARIZZIA NICOLAS : 1069 (fiche etablie) / 1289
--    - WU HA MINH DUC : 1209 (fiche etablie) / 1225
--    - WU TUNG LIN : 321 (fiche etablie) / 1165 (nom "aplati" sans prenom)
--    - WU YIBING : 99 (fiche etablie) / 1151 (nom "aplati" sans prenom)
-- On garde la fiche la plus etablie, on reaffecte les inscriptions de l'autre
-- vers elle - sauf si un conflit (meme tournoi deja pris par les 2 fiches)
-- existe, auquel cas on supprime plutot l'inscription en trop (evite de
-- planter sur la contrainte unique implicite "1 joueur = 1 place par
-- tournoi" que l'appli impose deja cote metier).
DO $$
DECLARE
    pairs CONSTANT INT[][] := ARRAY[
        ARRAY[615, 1023],
        ARRAY[993, 1302],
        ARRAY[1069, 1289],
        ARRAY[1209, 1225],
        ARRAY[321, 1165],
        ARRAY[99, 1151]
    ];
    pair INT[];
    keep_id INT;
    drop_id INT;
BEGIN
    FOREACH pair SLICE 1 IN ARRAY pairs LOOP
        keep_id := pair[1];
        drop_id := pair[2];

        UPDATE entry e SET player_id = keep_id
        WHERE e.player_id = drop_id
          AND NOT EXISTS (
              SELECT 1 FROM entry e2 WHERE e2.tournament_id = e.tournament_id AND e2.player_id = keep_id
          );

        DELETE FROM entry WHERE player_id = drop_id; -- inscriptions en double restantes (meme tournoi que keep_id)
        DELETE FROM player WHERE id = drop_id;
    END LOOP;
END $$;

-- 2) Nationalites incoherentes en casse (ex: "Italie" vs "ITALIE") ou mal
--    orthographiees (ex: "TUNSIE"), qui fragmentaient les stats par pays.
--    La convention dominante dans la base est TOUT MAJUSCULE.
UPDATE player SET nationality = 'ALLEMAGNE'  WHERE nationality = 'Allemagne';
UPDATE player SET nationality = 'AUTRICHE'   WHERE nationality = 'Autriche';
UPDATE player SET nationality = 'BELGIQUE'   WHERE nationality = 'Belgique';
UPDATE player SET nationality = 'ESPAGNE'    WHERE nationality = 'Espagne';
UPDATE player SET nationality = 'ITALIE'     WHERE nationality = 'Italie';
UPDATE player SET nationality = 'MEXIQUE'    WHERE nationality = 'Mexique';
UPDATE player SET nationality = 'PORTUGAL'   WHERE nationality = 'Portugal';
UPDATE player SET nationality = 'IRLANDE'    WHERE nationality = 'Irlande';
UPDATE player SET nationality = 'LUXEMBOURG' WHERE nationality = 'Luxembourg';
UPDATE player SET nationality = 'DOMINIQUE'  WHERE nationality = 'DOMINQUE'; -- coquille
UPDATE player SET nationality = 'TUNISIE'    WHERE nationality = 'TUNSIE';   -- coquille
