package com.charles.tennisresults.dto;

import jakarta.validation.constraints.NotNull;

/**
 * winnerEntryId doit correspondre a entry1 ou entry2 du match. score est du texte
 * libre ("6-3 6-4", "w.o.", "6-7(4) 7-6(2) 6-2"...).
 */
public record ScoreUpdateDto(String score, @NotNull Long winnerEntryId) {}
