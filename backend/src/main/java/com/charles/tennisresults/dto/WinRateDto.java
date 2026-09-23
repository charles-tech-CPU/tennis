package com.charles.tennisresults.dto;

/** Bilan victoires/defaites d'un joueur sur la saison, avec son taux de victoire. */
public record WinRateDto(
        Long playerId, String lastName, String firstName, String nationality, long wins, long losses, double winRate) {}
