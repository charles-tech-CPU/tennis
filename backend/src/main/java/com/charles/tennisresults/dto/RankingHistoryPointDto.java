package com.charles.tennisresults.dto;

/**
 * Classement d'un joueur au debut de la semaine `week` de la saison `season`
 * (photo hebdomadaire, voir RankingSnapshotService). Une semaine ou le joueur
 * n'etait pas classe n'a pas de point.
 */
public record RankingHistoryPointDto(int season, int week, int position, int points) {}
