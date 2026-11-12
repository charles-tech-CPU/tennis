package com.charles.tennisresults.dto;

import jakarta.validation.constraints.NotNull;

/** Forfait : le remplacant prend la place du joueur dans ses matchs de poule pas encore joues. */
public record ReplacementDto(
        @NotNull Long withdrawnEntryId, @NotNull Long alternateEntryId) {}
