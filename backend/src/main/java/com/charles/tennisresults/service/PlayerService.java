package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.dto.PlayerCreateDto;
import com.charles.tennisresults.dto.PlayerDto;
import com.charles.tennisresults.repository.PlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<PlayerDto> findAll() {
        return playerRepository.findAll().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(PlayerDto::lastName))
                .toList();
    }

    public PlayerDto create(PlayerCreateDto dto) {
        Player p = new Player(dto.lastName(), dto.firstName(), dto.nationality());
        return toDto(playerRepository.save(p));
    }

    public PlayerDto update(Long id, PlayerCreateDto dto) {
        Player p = playerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable: " + id));
        p.setLastName(dto.lastName());
        p.setFirstName(dto.firstName());
        p.setNationality(dto.nationality());
        return toDto(playerRepository.save(p));
    }

    private PlayerDto toDto(Player p) {
        return new PlayerDto(p.getId(), p.getLastName(), p.getFirstName(), p.getNationality(), p.getLegacySnapshotPoints());
    }
}
