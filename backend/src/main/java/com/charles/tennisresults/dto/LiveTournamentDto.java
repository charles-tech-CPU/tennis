package com.charles.tennisresults.dto;

/**
 * Un tournoi en cours (non termine) dans lequel ce joueur est encore en jeu
 * (pas encore elimine) - sert a le colorer dans le classement. colorHue est
 * une teinte HSL (0-359) stable pour ce tournoi, partagee par tous les
 * joueurs encore en jeu dedans, calculee une fois par appel de classement.
 */
public record LiveTournamentDto(Long tournamentId, String tournamentName, int colorHue) {
}
