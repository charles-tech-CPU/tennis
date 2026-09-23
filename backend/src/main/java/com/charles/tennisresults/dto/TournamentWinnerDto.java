package com.charles.tennisresults.dto;

/** Vainqueur d'un tournoi termine (joueur ayant gagne la finale du tableau principal). */
public record TournamentWinnerDto(Long playerId, String lastName, String firstName, String nationality) {}
