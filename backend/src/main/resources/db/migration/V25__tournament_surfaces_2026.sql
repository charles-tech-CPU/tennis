-- Surface et indoor/outdoor de chaque tournoi du calendrier 2026, d'apres le
-- calendrier officiel ATP (atptour.com, calendriers Tour et Challenger, 2026-10-17).
-- Toutes les valeurs etaient vides : rien n'est ecrase. Seul "TBD" (semaine 45,
-- tournoi non identifie) reste non renseigne.

UPDATE tournament SET surface = 'CLAY', indoor = FALSE
WHERE id IN (13, 16, 21, 26, 34, 41, 48, 52, 55, 56, 61, 63, 67, 69, 70, 71, 72, 73, 75, 76, 77, 78, 79, 80, 81, 82, 84, 85, 86, 87, 88, 89, 91, 92, 93, 95, 96, 98, 100, 101, 102, 104, 106, 108, 109, 111, 112, 113, 114, 115, 116, 117, 118, 120, 121, 122, 124, 125, 126, 127, 130, 132, 133, 134, 140, 141, 142, 143, 147, 148, 150, 151, 154, 155, 156, 159, 160, 161, 162, 163, 165, 166, 167, 168, 170, 171, 172, 173, 174, 178, 179, 180, 185, 187, 188, 191, 195, 197, 198, 204, 205, 210, 212, 213, 216, 217, 218, 219, 224, 229, 295, 326, 423, 425, 427, 436, 438, 442, 522, 540, 542);

UPDATE tournament SET surface = 'GRASS', indoor = FALSE
WHERE id IN (131, 137, 138, 139, 144, 145, 146, 149, 152, 153, 157, 164, 169);

UPDATE tournament SET surface = 'HARD', indoor = TRUE
WHERE id IN (8, 12, 15, 20, 23, 25, 28, 30, 31, 32, 33, 35, 38, 43, 49, 50, 54, 62, 226, 235, 236, 237, 238, 239, 240, 241, 243, 544, 546);

UPDATE tournament SET surface = 'HARD', indoor = FALSE
WHERE id IN (2, 3, 4, 5, 6, 7, 9, 10, 11, 14, 17, 18, 19, 22, 24, 27, 29, 36, 37, 39, 40, 42, 44, 45, 46, 47, 51, 53, 58, 59, 60, 65, 66, 68, 74, 83, 90, 94, 97, 99, 103, 105, 107, 110, 119, 123, 128, 129, 135, 136, 158, 175, 176, 177, 181, 182, 183, 184, 186, 189, 190, 192, 193, 194, 196, 199, 200, 201, 202, 203, 206, 207, 208, 209, 211, 214, 215, 220, 221, 222, 223, 225, 227, 228, 230, 231, 232, 233, 234, 325, 434, 440, 520, 538, 550);

-- Un tableau de qualifications se joue sur la meme surface que son tableau principal.
UPDATE tournament q SET surface = m.surface, indoor = m.indoor
FROM tournament m
WHERE q.is_qualifying AND q.main_tournament_id = m.id;
