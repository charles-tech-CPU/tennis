-- Les qualifs des Masters 1000 doivent se jouer la semaine PRECEDENT le
-- tableau principal (comme les Grand Chelem), y compris pour les Masters
-- 1000 sur 1 semaine (Madrid, Monte Carlo, ...) : la regle n'etait cablee
-- en dur que pour Indian Wells/Miami (V4). Deux tournois etaient donc
-- desynchronises :
--   - MADRID (id 98) : le tableau principal a ete deplace de la semaine 17 a
--     16 sans repercuter le changement sur ses qualifs (id 389), restees a
--     17 - soit APRES le tableau principal au lieu d'avant, ce qui bloquait
--     a tort l'inscription d'un joueur elimine des qualifs a un tournoi de
--     la semaine du tableau principal (ex: AIX, semaine 17).
--   - MONTE CARLO (id 84/342) : qualifs restees a la meme semaine (14) que
--     le tableau principal au lieu de la semaine d'avant (13).
UPDATE tournament SET week_number = 17 WHERE id = 98;  -- MADRID (principal)
UPDATE tournament SET week_number = 16 WHERE id = 389; -- MADRID - Qualifs
UPDATE tournament SET week_number = 13 WHERE id = 342; -- MONTE CARLO - Qualifs
