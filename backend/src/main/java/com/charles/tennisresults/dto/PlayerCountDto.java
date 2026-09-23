package com.charles.tennisresults.dto;

/** Un joueur associe a un compteur (titres, matchs gagnes, ...) pour une stat donnee. */
public record PlayerCountDto(Long playerId, String lastName, String firstName, String nationality, long count) {}
