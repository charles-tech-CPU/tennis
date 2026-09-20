package com.charles.tennisresults.dto;

/**
 * `substituted` = true quand ce resultat ne vient pas du tournoi obligatoire
 * lui-meme (jamais joue, faute de classement suffisant pour y etre accepte)
 * mais d'un "autre" tournoi excedentaire (au-dela des 5 meilleurs + le
 * remplacement Monte-Carlo/6e) recycle dans cette case vide - voir
 * RankingService.substituteMissingMandatorySlots.
 */
public record TournamentPointsDto(Long tournamentId, String tournamentName, Integer points, boolean substituted) {
    public TournamentPointsDto(Long tournamentId, String tournamentName, Integer points) {
        this(tournamentId, tournamentName, points, false);
    }
}
