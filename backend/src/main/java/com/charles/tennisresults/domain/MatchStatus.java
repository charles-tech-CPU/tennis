package com.charles.tennisresults.domain;

public enum MatchStatus {
    /** Les deux joueurs du match ne sont pas encore connus (tour suivant pas encore atteint). */
    PENDING,
    /** Les deux joueurs sont connus, le match reste a jouer. */
    SCHEDULED,
    /** Score saisi, vainqueur connu. */
    COMPLETED,
    /** Bye automatique (un seul cote du match est rempli, l'autre est vide en permanence). */
    BYE
}
