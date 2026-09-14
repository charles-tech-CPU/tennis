package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.EntryType;

public record EntryDto(
        Long id,
        Long tournamentId,
        Long playerId,
        String playerLastName,
        String playerFirstName,
        Integer drawPosition,
        Integer seed,
        EntryType entryType,
        boolean bye
) {
}
