package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.MandatorySlot;
import com.charles.tennisresults.domain.TournamentCategory;
import java.util.List;

public record TournamentDto(
        Long id,
        String name,
        TournamentCategory category,
        Integer season,
        Integer weekNumber,
        String country,
        MandatorySlot mandatorySlot,
        Integer drawSize,
        Integer drawSlots,
        Integer qualifyingRound1Points,
        Integer qualifyingRound2Points,
        Integer runnerUpPoints,
        List<RoundPointsDto> rounds,
        boolean qualifying,
        Long mainTournamentId,
        Long qualifyingTournamentId,
        TournamentStatus status,
        Integer colorHue,
        TournamentWinnerDto winner) {}
