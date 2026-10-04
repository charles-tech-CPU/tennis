package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.EntryType;

/**
 * Un match decide d'un joueur dans un tournoi (tableau principal ou qualifs),
 * vu de son cote. bye = passage sans adversaire (opponent et score null) ;
 * opponentSeed/opponentEntryType = tete de serie / statut d'entree de
 * l'adversaire dans ce tableau, null si non renseignes ; opponentRanking =
 * son classement fige au demarrage du tournoi, null si inconnu.
 */
public record PlayerMatchDto(
        Long matchId,
        boolean qualifying,
        Integer roundOrder,
        String roundLabel,
        boolean bye,
        PlayerDto opponent,
        Integer opponentSeed,
        EntryType opponentEntryType,
        Integer opponentRanking,
        String score,
        boolean won) {}
