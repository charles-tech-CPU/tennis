package com.charles.tennisresults.dto;

/**
 * Correction d'une rencontre existante (pays, poule, dates, lieu, surface). Les
 * matchs et le score ne changent pas ici - voir TeamRubberUpdateDto.
 */
public record TeamTieUpdateDto(
        String team1, String team2, String groupName, String dates, String city, String venue, String surface) {}
