package com.charles.tennisresults.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Nouvelle rencontre dans une phase "a liste" (qualifs, barrages, poule United
 * Cup). Les phases a elimination directe se creent d'un bloc via
 * TeamBracketCreateDto. rubberCount = nombre de matchs (3 ou 5) : ils sont crees
 * vides, le 3e etant toujours le double.
 */
public record TeamTieCreateDto(
        @NotBlank String competition,
        @NotNull Integer season,
        @NotBlank String stage,
        String groupName,
        @NotBlank String team1,
        @NotBlank String team2,
        String dates,
        String city,
        String venue,
        String surface,
        @NotNull Integer rubberCount) {}
