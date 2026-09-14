package com.charles.tennisresults.domain;

/**
 * Les 14 "cases obligatoires" du classement, comme dans le fichier Excel de Charles :
 * les 4 Grand Chelem, l'ATP Finals (M8), et les 9 Masters 1000 (dont Monte-Carlo,
 * qui est le seul optionnel : voir RankingService pour la regle de remplacement).
 * Un tournoi (GRAND_SLAM ou MASTERS_1000) ne joue ce role que si on le lui assigne
 * explicitement - un meme MandatorySlot ne devrait avoir qu'un seul tournoi par saison.
 */
public enum MandatorySlot {
    AUSTRALIAN_OPEN,
    ROLAND_GARROS,
    WIMBLEDON,
    US_OPEN,
    ATP_FINALS,
    INDIAN_WELLS,
    MIAMI,
    MONTE_CARLO,
    MADRID,
    ROME,
    CANADA,
    CINCINNATI,
    SHANGHAI,
    PARIS_BERCY
}
