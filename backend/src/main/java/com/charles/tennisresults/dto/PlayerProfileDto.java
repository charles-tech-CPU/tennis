package com.charles.tennisresults.dto;

import java.util.List;

/**
 * Fiche joueur. rankingPosition/rankingTotal sont null s'il n'apparait pas au
 * classement (meme calcul que /api/ranking). tournaments est trie de la
 * saison la plus recente a la plus ancienne, titles aussi.
 */
public record PlayerProfileDto(
        PlayerDto player,
        Integer rankingPosition,
        Integer rankingTotal,
        int rankedPlayers,
        PlayerRecordDto matchRecord,
        List<PlayerTournamentResultDto> titles,
        List<CategoryBestResultDto> bestByCategory,
        List<PlayerTournamentResultDto> tournaments) {}
