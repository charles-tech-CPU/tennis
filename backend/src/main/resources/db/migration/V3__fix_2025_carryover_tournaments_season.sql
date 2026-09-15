-- Les tournois de fin de calendrier (semaine 38 a 46 dans le fichier Excel source)
-- sont en realite les editions 2025 de ces tournois, conservees dans le fichier
-- uniquement pour le calcul du classement ATP glissant sur 52 semaines - ce ne
-- sont PAS des tournois de la saison 2026 a jouer/saisir. Le seed initial (V2) les
-- avait tous tagues season=2026 par erreur (le script d'import ne distinguait pas
-- ce cas). On les retague en season=2025 pour qu'ils n'apparaissent plus dans le
-- classement 2026 (RankingService filtre strictement par tournament.season).
UPDATE tournament SET season = 2025 WHERE id BETWEEN 224 AND 243;
