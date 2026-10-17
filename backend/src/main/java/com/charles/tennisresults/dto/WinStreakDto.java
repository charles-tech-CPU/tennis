package com.charles.tennisresults.dto;

/**
 * Plus longue serie de victoires consecutives d'un joueur, toutes saisons
 * confondues : du tournoi de la premiere victoire (from) a celui de la
 * derniere (to). ongoing = aucune defaite depuis, la serie est en cours.
 */
public record WinStreakDto(
        int length,
        Long fromTournamentId,
        String fromTournamentName,
        Integer fromSeason,
        Long toTournamentId,
        String toTournamentName,
        Integer toSeason,
        boolean ongoing) {}
