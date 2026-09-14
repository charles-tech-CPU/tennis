package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.MandatorySlot;
import com.charles.tennisresults.domain.TournamentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Si "rounds" est omis ou vide, le backend remplit automatiquement le bareme de
 * points par defaut de la categorie (voir CategoryDefaults) - Charles peut ensuite
 * l'ajuster tournoi par tournoi (les points reels different parfois legerement
 * d'un tournoi a l'autre au sein d'une meme categorie).
 */
public record TournamentCreateDto(
        @NotBlank String name,
        @NotNull TournamentCategory category,
        @NotNull Integer season,
        Integer weekNumber,
        String country,
        MandatorySlot mandatorySlot,
        @NotNull Integer drawSize,
        Integer qualifyingRound1Points,
        Integer qualifyingRound2Points,
        Integer runnerUpPoints,
        List<RoundPointsDto> rounds
) {
}
