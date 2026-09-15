package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.MandatorySlot;

import java.util.List;
import java.util.Map;

/**
 * Detail du classement d'un joueur, colonne par colonne comme dans le fichier
 * Excel : une case par tournoi obligatoire (Grand Chelem + Masters 1000, hors
 * Monte-Carlo), Monte-Carlo a part (car remplacable), les 5 meilleurs "autres"
 * tournois nommes, le remplacement retenu, et le detail de ce qui n'a PAS ete
 * comptabilise (au-dela des 5 + remplacement) - a titre informatif seulement.
 */
public record RankingRowDto(
        Long playerId,
        String lastName,
        String firstName,
        String nationality,
        Map<MandatorySlot, TournamentPointsDto> mandatorySlots,
        TournamentPointsDto monteCarlo,
        List<TournamentPointsDto> bestOthers,
        TournamentPointsDto replacement,
        List<TournamentPointsDto> nonCounted,
        int mandatoryTotal,
        int othersTotal,
        int replacementValue,
        int total,
        List<LiveTournamentDto> liveTournaments
) {
}
