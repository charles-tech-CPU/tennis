package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.Surface;
import java.util.List;
import java.util.Map;

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
        List<PlayerCountDto> mostActivePlayers,
        /** Le plus de victoires par surface (terre battue, gazon, dur ; toujours les 3 cles). */
        Map<Surface, List<PlayerCountDto>> topWinsBySurface,
        /** Victoires sur un joueur bien mieux classe (rapport des classements le plus grand). */
        List<MatchHighlightDto> biggestUpsets,
        /** Victoires sur un joueur du top 10 au debut du tournoi. */
        List<PlayerCountDto> topTop10Wins,
        List<PlayerCountDto> topTiebreaksWon,
        /** Matchs avec le plus de jeux. */
        List<MatchHighlightDto> longestMatches) {}
