package com.charles.tennisresults.dto;

/** Meilleur parcours d'un joueur issu des qualifs dans un tableau principal. */
public record QualifierRunDto(
        Long playerId,
        String lastName,
        String firstName,
        String nationality,
        String tournamentName,
        String roundReached) {}
