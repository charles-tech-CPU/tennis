package com.charles.tennisresults.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cree le tableau de qualifications lie a un tournoi principal. drawSize doit etre
 * divisible par 2^(nombre de tours) : ex. 24 joueurs / 2 tours = 6 qualifies
 * (groupes independants de 4), comme pour un ATP250 classique. rounds donne le
 * bareme de points par tour de qualification (Q1, Q2, ...) - typiquement 2 tours.
 */
public record QualifyingCreateDto(
        @NotNull Integer drawSize,
        @NotEmpty List<RoundPointsDto> rounds
) {
}
