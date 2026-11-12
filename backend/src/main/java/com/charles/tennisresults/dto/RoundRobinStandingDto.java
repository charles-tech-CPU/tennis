package com.charles.tennisresults.dto;

/**
 * Ligne du classement d'une poule. withdrawn = joueur de la poule remplace
 * (forfait) pour ses matchs restants ; ses matchs deja joues restent comptes.
 */
public record RoundRobinStandingDto(
        EntryDto entry,
        int rank,
        int wins,
        int losses,
        int setsWon,
        int setsLost,
        int gamesWon,
        int gamesLost,
        boolean qualified,
        boolean withdrawn) {}
