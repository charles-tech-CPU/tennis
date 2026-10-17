package com.charles.tennisresults.dto;

/**
 * Un match marquant de la saison (plus grosse surprise, match le plus
 * dispute...). winnerRanking/loserRanking = classements figes au debut du
 * tournoi, null si inconnus ; games = nombre total de jeux du match.
 */
public record MatchHighlightDto(
        Long matchId,
        PlayerDto winner,
        Integer winnerRanking,
        PlayerDto loser,
        Integer loserRanking,
        Long tournamentId,
        String tournamentName,
        boolean qualifying,
        String roundLabel,
        String score,
        int games) {}
