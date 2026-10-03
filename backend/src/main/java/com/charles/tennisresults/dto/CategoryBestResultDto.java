package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.TournamentCategory;

/** Meilleur resultat d'un joueur dans une categorie, et la derniere fois qu'il l'a atteint. */
public record CategoryBestResultDto(TournamentCategory category, PlayerTournamentResultDto best, int times) {}
