package com.charles.tennisresults.dto;

import java.util.List;

/**
 * Point de depart de l'onglet Stats : concu pour etre etendu facilement (une
 * nouvelle liste nommee par stat) au fur et a mesure des demandes de Charles,
 * pas une liste figee.
 */
public record StatsDto(
        int season,
        List<PlayerCountDto> topTournamentWinners,
        List<PlayerCountDto> topMatchWinners,
        List<PlayerCountDto> topGrandSlamWinners,
        List<PlayerCountDto> topMasters1000Winners,
        List<NationCountDto> topNationsByTitles,
        List<PlayerCountDto> topRunnersUp,
        List<QualifierRunDto> bestQualifierRuns,
        List<WinRateDto> topWinRate,
        List<StreakDto> longestWinStreaks,
        List<PlayerCountDto> topBagelsInflicted,
        List<PlayerCountDto> topEpicWins,
        List<PlayerCountDto> mostActivePlayers) {}
