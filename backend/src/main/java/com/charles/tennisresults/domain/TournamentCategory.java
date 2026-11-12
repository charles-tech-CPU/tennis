package com.charles.tennisresults.domain;

/**
 * Categorie ATP d'un tournoi. Determine le bareme de points par defaut
 * et, pour GRAND_SLAM et MASTERS_1000, la possibilite d'avoir un
 * {@link MandatorySlot} associe (evenement "obligatoire" pour le classement).
 */
public enum TournamentCategory {
    GRAND_SLAM,
    MASTERS_1000,
    ATP_500,
    ATP_250,
    ATP_175,
    ATP_125,
    ATP_100,
    ATP_75,
    ATP_50,
    /** Next Gen ATP Finals (meilleurs 20 ans et moins) : en poules, aucun point ATP. */
    NEXT_GEN
}
