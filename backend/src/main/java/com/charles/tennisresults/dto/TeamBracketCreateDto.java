package com.charles.tennisresults.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Tableau final complet d'une saison (Final 8 Coupe Davis, phase finale United
 * Cup) : les 4 quarts avec leurs pays, dans l'ordre du tableau (le vainqueur du
 * quart n va en demie ceil(n/2)) ; demies et finale sont creees en attente
 * ("Vainqueur QF1"...). dates / city / venue / surface s'appliquent a toutes les
 * rencontres, sauf dates propres a un quart.
 */
public record TeamBracketCreateDto(
        @NotBlank String competition,
        @NotNull Integer season,
        @NotNull @Size(min = 4, max = 4) List<@Valid Quarter> quarters,
        String dates,
        String city,
        String venue,
        String surface,
        @NotNull Integer rubberCount) {
    public record Quarter(@NotBlank String team1, @NotBlank String team2, String dates) {}
}
