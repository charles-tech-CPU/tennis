package com.charles.tennisresults.service;

import com.charles.tennisresults.domain.Entry;
import com.charles.tennisresults.domain.Player;
import com.charles.tennisresults.domain.Tournament;
import com.charles.tennisresults.dto.EntryCreateDto;
import com.charles.tennisresults.dto.EntryDto;
import com.charles.tennisresults.repository.EntryRepository;
import com.charles.tennisresults.repository.PlayerRepository;
import com.charles.tennisresults.repository.TournamentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class EntryService {

    private final EntryRepository entryRepository;
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final BracketService bracketService;

    public EntryService(EntryRepository entryRepository, TournamentRepository tournamentRepository,
                         PlayerRepository playerRepository, BracketService bracketService) {
        this.entryRepository = entryRepository;
        this.tournamentRepository = tournamentRepository;
        this.playerRepository = playerRepository;
        this.bracketService = bracketService;
    }

    @Transactional(readOnly = true)
    public List<EntryDto> findByTournament(Long tournamentId) {
        return entryRepository.findByTournamentIdOrderByDrawPositionAsc(tournamentId).stream()
                .sorted(Comparator.comparing(Entry::getDrawPosition))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EntryDto create(Long tournamentId, EntryCreateDto dto) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournoi introuvable: " + tournamentId));

        Entry entry = new Entry();
        entry.setTournament(tournament);
        entry.setDrawPosition(dto.drawPosition());
        entry.setSeed(dto.seed());
        entry.setEntryType(dto.entryType());
        entry.setBye(dto.bye());

        if (!dto.bye()) {
            if (dto.playerId() == null) {
                throw new IllegalArgumentException("playerId est requis pour une entree qui n'est pas un bye.");
            }
            Player player = playerRepository.findById(dto.playerId())
                    .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable: " + dto.playerId()));
            entry.setPlayer(player);
        }

        entry = entryRepository.save(entry);
        bracketService.syncRound1FromEntries(tournamentId);
        return toDto(entry);
    }

    @Transactional
    public void delete(Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entree introuvable: " + entryId));
        Long tournamentId = entry.getTournament().getId();
        bracketService.clearEntryFromMatches(entry);
        entryRepository.delete(entry);
        bracketService.syncRound1FromEntries(tournamentId);
    }

    private EntryDto toDto(Entry e) {
        return new EntryDto(
                e.getId(), e.getTournament().getId(),
                e.getPlayer() != null ? e.getPlayer().getId() : null,
                e.getPlayer() != null ? e.getPlayer().getLastName() : (e.isBye() ? "BYE" : null),
                e.getPlayer() != null ? e.getPlayer().getFirstName() : null,
                e.getPlayer() != null ? e.getPlayer().getNationality() : null,
                e.getDrawPosition(), e.getSeed(), e.getEntryType(), e.isBye()
        );
    }
}
