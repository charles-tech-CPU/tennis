-- V9 a change a tort la semaine du tableau PRINCIPAL de MADRID (98) de 16 a
-- 17, en plus de corriger ses qualifs. Erreur : le tableau principal de
-- Madrid a toujours ete semaine 16 (donnee de depart V2__seed_data.sql), et
-- plein de joueurs y sont deja regulierement inscrits en meme temps qu'a
-- d'autres tournois de la semaine 17 (CAGLIARI id 425, AIX id 423) sans
-- aucun conflit reel - ex: LAJOVIC gagne son 1er tour a Madrid ET joue Aix,
-- deux evenements bien distincts sur le calendrier. Deplacer le principal a
-- 17 les a mis en collision avec TOUS les joueurs du tableau principal de
-- Madrid (Charles, 2026-09-20 - "c'est exactement la meme chose pour
-- Cagliari et Madrid, j'ai plein de joueurs qui sont inscrits mais [le
-- systeme] dit non").
--
-- Seules les qualifs doivent se jouer la semaine d'avant le principal, le
-- principal lui-meme ne doit jamais bouger : MADRID reste semaine 16, ses
-- qualifs passent donc a 15 (et non 16 comme fait par erreur en V9).
UPDATE tournament SET week_number = 16 WHERE id = 98;  -- MADRID (principal) : retour a sa valeur d'origine
UPDATE tournament SET week_number = 15 WHERE id = 389; -- MADRID - Qualifs : semaine d'avant le principal (16)
