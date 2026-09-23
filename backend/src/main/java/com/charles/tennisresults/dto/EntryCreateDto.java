package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.EntryType;
import jakarta.validation.constraints.NotNull;

/** playerId est ignore si bye = true. */
public record EntryCreateDto(
        Long playerId, @NotNull Integer drawPosition, Integer seed, EntryType entryType, boolean bye) {}
