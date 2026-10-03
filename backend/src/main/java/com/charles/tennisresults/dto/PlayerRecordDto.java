package com.charles.tennisresults.dto;

/** Bilan d'un joueur sur des matchs reellement joues ; winRate (0 a 1) est null sans aucun match. */
public record PlayerRecordDto(int played, int wins, int losses, Double winRate) {}
