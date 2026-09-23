package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.MatchStatus;

public record MatchDto(
        Long id,
        Long tournamentId,
        Integer roundOrder,
        String roundLabel,
        Integer positionInRound,
        EntryDto entry1,
        EntryDto entry2,
        String score,
        Long winnerEntryId,
        MatchStatus status) {}
