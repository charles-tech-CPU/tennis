package com.charles.tennisresults.dto;

/**
 * Statut calcule (jamais stocke) d'un tournoi, deduit de l'etat de ses
 * matchs - sert uniquement a colorer la liste des tournois cote frontend.
 */
public enum TournamentStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
