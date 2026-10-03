package com.charles.tennisresults.dto;

import java.util.List;

/** Confrontations directes entre deux joueurs, de la plus recente a la plus ancienne. */
public record HeadToHeadDto(
        PlayerDto player1, PlayerDto player2, int player1Wins, int player2Wins, List<HeadToHeadMatchDto> matches) {}
