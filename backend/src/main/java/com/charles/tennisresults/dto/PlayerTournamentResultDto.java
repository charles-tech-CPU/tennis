package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.TournamentCategory;

/**
 * Resultat d'un joueur dans un tournoi (tableau principal + ses qualifs,
 * fusionnes comme dans le classement). roundLabel = tour atteint ("QF", "F",
 * "Q2"...) ; champion = finale du tableau principal gagnee ; inProgress =
 * pas encore elimine d'un tournoi pas termine.
 */
public record PlayerTournamentResultDto(
        Long tournamentId,
        String tournamentName,
        Integer season,
        Integer week,
        TournamentCategory category,
        String roundLabel,
        boolean champion,
        boolean inProgress,
        boolean viaQualifying,
        int points) {}
