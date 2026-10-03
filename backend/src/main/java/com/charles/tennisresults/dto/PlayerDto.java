package com.charles.tennisresults.dto;

import com.charles.tennisresults.domain.Player;

public record PlayerDto(
        Long id,
        String lastName,
        String firstName,
        String nationality,
        Integer legacySnapshotPoints,
        boolean favorite) {

    public static PlayerDto from(Player p) {
        return new PlayerDto(
                p.getId(),
                p.getLastName(),
                p.getFirstName(),
                p.getNationality(),
                p.getLegacySnapshotPoints(),
                p.isFavorite());
    }
}
