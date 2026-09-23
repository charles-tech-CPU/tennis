-- V3 avait retague en season=2025 les tournois de fin de calendrier (ids 224 a
-- 243), consideres comme les editions 2025 conservees pour le classement
-- glissant. Mais ceux de la semaine 37 (Szczecin, Tiburon, Guangzhou, Rennes,
-- Biella, Phan Thiet 4) ont bien ete joues et saisis comme editions 2026
-- (Charles, 2026-09-23) : on les remet en 2026, qualifs comprises (une
-- qualif partage toujours la saison de son tableau principal).
UPDATE tournament SET season = 2026
WHERE id IN (224, 225, 226, 227, 228, 229)
   OR main_tournament_id IN (224, 225, 226, 227, 228, 229);
