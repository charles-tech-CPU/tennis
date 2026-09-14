package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.TournamentCategory;

import java.util.List;
import java.util.Map;

/**
 * Bareme de points par defaut, propose a la creation d'un tournoi (Charles peut
 * toujours l'ajuster ensuite - chaque tournoi garde ses propres points en base).
 * Cle = (categorie, nombre de cases du tableau = plus petite puissance de 2 >= drawSize).
 * Le dernier element du tableau est le point du VAINQUEUR ; le finaliste battu
 * utilise Tournament.runnerUpPoints (a defaut, l'avant-dernier element = points de
 * demi-finaliste).
 */
public final class CategoryDefaults {

    private CategoryDefaults() {
    }

    private static final Map<String, List<Integer>> DEFAULTS = Map.ofEntries(
            Map.entry(key(TournamentCategory.GRAND_SLAM, 128), List.of(10, 45, 90, 180, 360, 720, 2000)),
            Map.entry(key(TournamentCategory.MASTERS_1000, 128), List.of(10, 30, 45, 90, 180, 360, 1000)),
            Map.entry(key(TournamentCategory.MASTERS_1000, 64), List.of(25, 45, 90, 180, 360, 1000)),
            Map.entry(key(TournamentCategory.ATP_500, 64), List.of(20, 45, 90, 180, 500)),
            Map.entry(key(TournamentCategory.ATP_500, 32), List.of(45, 90, 180, 500)),
            Map.entry(key(TournamentCategory.ATP_250, 32), List.of(25, 50, 100, 165, 250)),
            Map.entry(key(TournamentCategory.ATP_250, 16), List.of(50, 100, 165, 250)),
            Map.entry(key(TournamentCategory.ATP_175, 32), List.of(18, 35, 70, 115, 175)),
            Map.entry(key(TournamentCategory.ATP_125, 32), List.of(13, 25, 50, 80, 125)),
            Map.entry(key(TournamentCategory.ATP_100, 32), List.of(10, 20, 40, 65, 100)),
            Map.entry(key(TournamentCategory.ATP_75, 32), List.of(8, 15, 30, 48, 75)),
            Map.entry(key(TournamentCategory.ATP_50, 32), List.of(5, 10, 20, 32, 50))
    );

    private static String key(TournamentCategory category, int drawSlots) {
        return category.name() + "-" + drawSlots;
    }

    /** Renvoie le bareme par defaut, ou un bareme generique (progression x2) si aucun defaut connu. */
    public static List<Integer> pointsFor(TournamentCategory category, int drawSlots) {
        List<Integer> exact = DEFAULTS.get(key(category, drawSlots));
        if (exact != null) {
            return exact;
        }
        int rounds = Integer.numberOfTrailingZeros(drawSlots);
        int base = switch (category) {
            case GRAND_SLAM -> 2000;
            case MASTERS_1000 -> 1000;
            case ATP_500 -> 500;
            case ATP_250 -> 250;
            case ATP_175 -> 175;
            case ATP_125 -> 125;
            case ATP_100 -> 100;
            case ATP_75 -> 75;
            case ATP_50 -> 50;
        };
        Integer[] points = new Integer[rounds];
        for (int i = rounds; i >= 1; i--) {
            points[i - 1] = Math.max(1, base >> (rounds - i));
        }
        return List.of(points);
    }
}
