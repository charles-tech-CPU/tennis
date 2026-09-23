package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.MandatorySlot;
import com.charles.tennisresults.domain.TournamentCategory;
import java.util.List;

/**
 * Champs modifiables apres creation du tournoi (v1) : le nom et la saison
 * restent figes. La categorie, elle, ne sert qu'a l'affichage/au bareme par
 * defaut a la creation : elle reste modifiable (ex: correction d'une erreur
 * de saisie ATP75 -> ATP50), le bareme reel de chaque tour (rounds) est
 * ajustable independamment ici.
 *
 * `drawSize` (taille reelle du tableau principal) reste egalement modifiable,
 * mais UNIQUEMENT tant qu'aucun joueur n'est encore place dans ce tournoi
 * (voir TournamentService.resizeDraw) - sert a corriger une erreur de saisie
 * a la creation (ex: Rotterdam cree a tort en 48 au lieu de 32, Charles,
 * 2026-09-16), pas a agrandir un tableau deja en cours de remplissage.
 */
public record TournamentUpdateDto(
        TournamentCategory category,
        Integer weekNumber,
        String country,
        MandatorySlot mandatorySlot,
        Integer qualifyingRound1Points,
        Integer qualifyingRound2Points,
        Integer runnerUpPoints,
        List<RoundPointsDto> rounds,
        Integer drawSize) {}
