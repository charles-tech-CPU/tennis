package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.MandatorySlot;
import com.charles.tennisresults.domain.TournamentCategory;

import java.util.List;

/**
 * Champs modifiables apres creation du tournoi (v1) : le nom, la saison et la
 * taille du tableau restent figes car ils conditionnent la structure du
 * bracket deja genere - voir README section 2. La categorie, elle, ne sert
 * qu'a l'affichage/au bareme par defaut a la creation : elle reste modifiable
 * (ex: correction d'une erreur de saisie ATP75 -> ATP50), le bareme reel de
 * chaque tour (rounds) est ajustable independamment ici.
 */
public record TournamentUpdateDto(
        TournamentCategory category,
        Integer weekNumber,
        String country,
        MandatorySlot mandatorySlot,
        Integer qualifyingRound1Points,
        Integer qualifyingRound2Points,
        Integer runnerUpPoints,
        List<RoundPointsDto> rounds
) {
}
