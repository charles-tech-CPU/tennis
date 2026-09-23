package com.charles.tennisresults.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Saisie d'un match de rencontre par equipes. status : PENDING, COMPLETED ou
 * NOT_PLAYED ; winner (1 ou 2) obligatoire si COMPLETED. score du point de vue
 * team1 - team2 de la rencontre, texte libre ("6-4 7-6(3)").
 */
public record TeamRubberUpdateDto(
        String team1Players,
        String team2Players,
        String score,
        Integer winner,
        @NotBlank String status) {}
