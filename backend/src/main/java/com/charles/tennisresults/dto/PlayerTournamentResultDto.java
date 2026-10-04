package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.Surface;
import com.charles.tennisresults.domain.TournamentCategory;
import java.util.List;

/**
 * Resultat d'un joueur dans un tournoi (tableau principal + ses qualifs,
 * fusionnes comme dans le classement). roundLabel = tour atteint ("QF", "F",
 * "Q2"...) ; champion = finale du tableau principal gagnee ; inProgress =
 * pas encore elimine d'un tournoi pas termine. matches = ses matchs decides
 * du plus recent au plus ancien (tableau principal puis qualifs) ; points =
 * points de classement gagnes (qualifs + tableau principal, bareme du tournoi) ;
 * rankingAtEntry = son classement fige au demarrage du tournoi (qualifs si
 * c'est par la qu'il est entre), null si inconnu.
 */
public record PlayerTournamentResultDto(
        Long tournamentId,
        String tournamentName,
        String country,
        Surface surface,
        Boolean indoor,
        Integer season,
        Integer week,
        TournamentCategory category,
        String roundLabel,
        boolean champion,
        boolean inProgress,
        boolean viaQualifying,
        Integer rankingAtEntry,
        int points,
        List<PlayerMatchDto> matches) {}
