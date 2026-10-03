package com.charles.tennisresults.dto;

public record HeadToHeadMatchDto(
        Long matchId,
        Long tournamentId,
        String tournamentName,
        Integer season,
        boolean qualifying,
        Integer roundOrder,
        String roundLabel,
        String score,
        Long winnerPlayerId) {}
