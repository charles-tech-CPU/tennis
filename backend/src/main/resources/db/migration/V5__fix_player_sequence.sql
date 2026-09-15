-- Le script d'import original (V2) resynchronisait player_id_seq avec le NOMBRE
-- de joueurs importes (player_count) plutot qu'avec le plus grand id reellement
-- utilise - or les ids suivent le numero de ligne du fichier Excel, qui peut
-- comporter des lignes sautees (cellule "nom" vide) et donc des ids non
-- contigus superieurs au nombre de joueurs. Resultat : la sequence pouvait
-- pointer en dessous d'ids deja pris, provoquant une erreur de cle dupliquee
-- des la creation du joueur suivant depuis l'application (voir aussi le fix
-- correspondant dans import/import_excel.py).
SELECT setval('player_id_seq', (SELECT MAX(id) FROM player), true);
