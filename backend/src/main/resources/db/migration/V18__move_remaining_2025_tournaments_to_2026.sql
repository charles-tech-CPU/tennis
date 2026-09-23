-- V3 avait retague en season=2025 les tournois de fin de calendrier (ids 224 a
-- 243) ; V17 a deja remis la semaine 37 en 2026. Aucun resultat 2025 ne sera
-- saisi dans l'application (Charles, 2026-09-23) : les tournois restants
-- (semaines 39 a 46) sont donc les editions 2026 a jouer. On les remet en 2026,
-- qualifs comprises (une qualif partage toujours la saison de son tableau principal).
UPDATE tournament SET season = 2026
WHERE season = 2025
   OR main_tournament_id IN (SELECT id FROM tournament WHERE season = 2025);
