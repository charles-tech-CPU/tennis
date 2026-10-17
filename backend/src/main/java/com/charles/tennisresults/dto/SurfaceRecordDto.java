package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.Surface;

/** Bilan d'un joueur sur une surface (matchs reellement joues) ; winRate (0 a 1) est null sans aucun match. */
public record SurfaceRecordDto(Surface surface, int played, int wins, int losses, Double winRate) {}
