-- Fusion des doublons "en minuscule" (Charles, 2026-09-22 : "il y a un doublon
-- sur ABOIAN VALERIO et Aboian V, met tous les resultats sur ABOIAN VALERIO,
-- et pareil sur tous les joueurs qui sont en minuscule, tu essaies de trouver
-- son doublon en MAJUSCULE").
--
-- Ces fiches "Nom P" (nom en casse mixte, prenom reduit a une initiale) ont ete
-- creees a la volee lors de saisies de tableaux (qualifs...) ou seul un nom
-- abrege etait disponible, alors qu'une fiche complete "NOM Prenom" existait
-- deja. Detection : meme nom de famille (accents/espaces normalises), initiale
-- du prenom concordante, meme nationalite -> 26 paires identifiees sans
-- ambiguite (verifie qu'aucune fiche majuscule n'a plusieurs candidats
-- minuscules compatibles et inversement).
--
-- On garde la fiche complete (MAJUSCULE), on reaffecte les inscriptions de la
-- fiche minuscule vers elle - meme logique que V12/V13 (skip si conflit sur un
-- meme tournoi, l'inscription en trop est alors simplement supprimee).
DO $$
DECLARE
    pairs CONSTANT INT[][] := ARRAY[
        ARRAY[345, 1637], -- ABOIAN VALERIO      / Aboian V
        ARRAY[552, 1638], -- ANDRADE DA SILVA LUCAS / Andrade Da Silva L
        ARRAY[782, 1590], -- BOCCHI LORENZO       / Bocchi L
        ARRAY[356, 1641], -- CASANOVA HERNAN      / Casanova H
        ARRAY[731, 1624], -- CAZACU DRAGOS NICOLAE / Cazacu D
        ARRAY[434, 1622], -- COMPAGNUCCI TOMMASO  / Compagnucci T
        ARRAY[284, 1616], -- DALLA VALLE ENRICO   / Dalla Valle E
        ARRAY[327, 1620], -- ERHARD MATHYS        / Erhard M
        ARRAY[565, 1640], -- FERNANDEZ BRUNO      / Fernandez B
        ARRAY[712, 1639], -- GOITY ZAPICO SEGUNDO / Goity Zapico S
        ARRAY[692, 1649], -- HAYEN ALEJANDRO      / Hayen A
        ARRAY[426, 1630], -- HERNANDEZ ALEX       / Hernandez A
        ARRAY[283, 1619], -- KUZMANOV DIMITAR     / Kuzmanov D
        ARRAY[265, 1629], -- LA SERNA JUAN MANUEL / La Serna J
        ARRAY[691, 1633], -- MARTINEZ TOMAS       / Martinez T
        ARRAY[391, 1618], -- MICHALSKI DANIEL     / Michalski D
        ARRAY[437, 1617], -- MILIC OGNIJEN        / Milic O
        ARRAY[585, 1631], -- MONZON IGNACIO       / Monzon I
        ARRAY[400, 1621], -- NESTEROV PETR        / Nesterov P
        ARRAY[876, 1651], -- PRICE SALVADOR       / Price S
        ARRAY[280, 1636], -- PUCINELLI DE ALMEIDA MATHEUS / Pucinelli De Almeida M
        ARRAY[388, 1634], -- RIBEIRO EDUARDO      / Ribeiro E
        ARRAY[728, 1632], -- RIBEIRO DE ALMEIDA GUSTAVO / Ribeiro De Almeida G
        ARRAY[516, 1615], -- SANTAMARTA ROIG ANDRES / Santamarta Roig A
        ARRAY[718, 1628], -- SINHA NITIN KUMAR    / Sinha N
        ARRAY[260, 1646]  -- SOTO MATIAS          / Soto M
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

        DELETE FROM entry WHERE player_id = drop_id;
        DELETE FROM player WHERE id = drop_id;
    END LOOP;
END $$;
