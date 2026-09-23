package com.charles.tennisresults.dto;

/** Plus longue serie de victoires consecutives d'un joueur sur la saison. */
public record StreakDto(Long playerId, String lastName, String firstName, String nationality, int streakLength) {}
