package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.EntryType;
import com.charles.tennisresults.domain.Player;

public record EntryDto(
        Long id,
        Long tournamentId,
        Long playerId,
        String playerLastName,
        String playerFirstName,
        String playerNationality,
        Integer drawPosition,
        Integer seed,
        EntryType entryType,
        boolean bye) {

    /** null pour une case vide ; une entree sans joueur est un bye (nom affiche "BYE") ou une case a remplir. */
    public static EntryDto from(Entry e) {
        if (e == null) {
            return null;
        }
        Player p = e.getPlayer();
        if (p == null) {
            return new EntryDto(
                    e.getId(),
                    e.getTournament().getId(),
                    null,
                    e.isBye() ? "BYE" : null,
                    null,
                    null,
                    e.getDrawPosition(),
                    e.getSeed(),
                    e.getEntryType(),
                    e.isBye());
        }
        return new EntryDto(
                e.getId(),
                e.getTournament().getId(),
                p.getId(),
                p.getLastName(),
                p.getFirstName(),
                p.getNationality(),
                e.getDrawPosition(),
                e.getSeed(),
                e.getEntryType(),
                e.isBye());
    }
}
