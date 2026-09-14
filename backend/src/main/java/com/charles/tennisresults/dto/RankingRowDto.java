package com.charles.tennisresults.dto;

public record RankingRowDto(
        Long playerId,
        String lastName,
        String firstName,
        String nationality,
        int mandatoryTotal,
        int othersTotal,
        int replacementValue,
        int total
) {
}
